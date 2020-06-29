package org.nano.updater.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.network.BotService
import org.nano.updater.util.Constants
import java.io.File
import javax.inject.Inject

/**
 * A class that helps in sending logs to the Nano Telegram channel
 */
class ReportRepository @Inject constructor(
    private val context: Context
) {

    // Used for tracking the report count
    private var reportCount: Int = 0
    private var reportSent: Int = 0

    @Inject
    lateinit var botService: BotService

    init {
        (context.applicationContext as NanoApplication).appComponent.inject(this)
        Shell.Config.setTimeout(10)
    }

    /**
     * Method that invokes logs generation
     * @param updaterLog: Whether user toggled updater log
     * @param kernelLog: Whether user toggled kernel log
     * @param reportStatusMessage: String that holds the status of log generation
     * @param isLogReported: Track whether the log report is successful
     */
    @Throws(Exception::class)
    suspend fun generateLogs(
        updaterLog: Boolean,
        kernelLog: Boolean,
        reportStatusMessage: MutableLiveData<String>,
        isLogReported: MutableLiveData<Boolean?>
    ) = withContext(Dispatchers.IO) {
        reportCount = updaterLog.toInt() + kernelLog.toInt()
        if (updaterLog) {
            updateStatus(
                reportStatusMessage,
                context.getString(R.string.report_status_generating_updater_logs)
            )
            saveLogs(
                execCommand(Constants.CMD_LOGCAT),
                isKernelLog = false,
                reportStatusMessage = reportStatusMessage,
                isLogReported = isLogReported
            )
        }
        // Check if sending of first log is success
        // By default, isLogReported is null, if the first log fails
        // then the value will be updated to false ( != null )
        // This will make sure that the next log will most probably fail and ignore sending it
        // Also fixes the SnackBar popping two times
        if (isLogReported.value != null)
            return@withContext

        if (kernelLog) {
            updateStatus(
                reportStatusMessage,
                context.getString(R.string.report_status_generating_kernel_logs)
            )
            saveLogs(
                execCommand(Constants.CMD_DMESG),
                isKernelLog = true,
                reportStatusMessage = reportStatusMessage,
                isLogReported = isLogReported
            )
        }
    }

    /**
     * Helper method to execute the [logcat, dmesg] commands for generating logs
     * @param cmd: Command that will be executed
     * @return String: Output of the the command after execution
     */
    private suspend fun execCommand(cmd: String): String = withContext(Dispatchers.IO) {
        val log: ArrayList<String> = ArrayList()
        Shell.su(cmd).to(log).exec()
        val logBuilder = StringBuilder()
        for (line in log)
            logBuilder.append(line).append("\n")
        logBuilder.toString()
    }

    /**
     * Helper method to save logs to cache dir after generation
     * @param log: Log file name (absolute path)
     * @param isKernelLog: If the log is Kernel or Updater log
     * @param reportStatusMessage: Reports the status message in the bottom sheet as "Saving logs"
     * @param isLogReported: Chaining the var to update it at the end
     */
    private suspend fun saveLogs(
        log: String,
        isKernelLog: Boolean,
        reportStatusMessage: MutableLiveData<String>,
        isLogReported: MutableLiveData<Boolean?>
    ) =
        withContext(Dispatchers.IO) {
            val logFile: String
            if (isKernelLog) {
                logFile = Constants.KERNEL_LOG
                updateStatus(
                    reportStatusMessage,
                    context.getString(R.string.report_status_saving_kernel_logs)
                )
            } else {
                logFile = Constants.UPDATER_LOG
                updateStatus(
                    reportStatusMessage,
                    context.getString(R.string.report_status_saving_updater_logs)
                )
            }

            val logFilePath = "${context.cacheDir}/$logFile"
            File(logFilePath).writeText(log)
            try {
                sendLog(isKernelLog, reportStatusMessage, isLogReported)
            } catch (e: Exception) {
                // Some error has occurred while sending logs
                isLogReported.postValue(false)
                e.printStackTrace()
            }
        }


    /**
     * Helper method to send logs to Telegram channel
     * @param isKernelLog: Whether the log type is Kernel or Updater
     * @param reportStatusMessage: Report the status as "Sending logs"
     * @param isLogReported: Whether the log is reported successfully
     */
    private suspend fun sendLog(
        isKernelLog: Boolean,
        reportStatusMessage: MutableLiveData<String>,
        isLogReported: MutableLiveData<Boolean?>
    ) = withContext(Dispatchers.IO) {
        val logFile: String
        if (isKernelLog) {
            logFile = Constants.KERNEL_LOG
            updateStatus(
                reportStatusMessage,
                context.getString(R.string.report_status_sending_kernel_logs)
            )
        } else {
            logFile = Constants.UPDATER_LOG
            updateStatus(
                reportStatusMessage,
                context.getString(R.string.report_status_sending_updater_logs)
            )
        }

        val response = botService.sendLogWithCaption(
            RequestBody.create(MediaType.parse("text/plain"), Constants.TG_MY_ID),
            MultipartBody.Part.createFormData(
                "document",
                logFile,
                RequestBody.create(
                    MediaType.parse("multipart/form-data"),
                    File(context.cacheDir, logFile)
                )
            ),
            RequestBody.create(MediaType.parse("text/plain"), Constants.TG_CAPTION),
            RequestBody.create(MediaType.parse("text/plain"), Constants.TG_PARSE_MODE),
            "${Constants.TG_CHANNEL_API_URL}${Constants.TG_METHOD_SEND_DOCUMENT}"
        ).execute()
        if (response.isSuccessful)
            if (isKernelLog)
                updateStatus(
                    reportStatusMessage,
                    context.getString(R.string.report_status_kernel_log_sent_successfully)
                )
            else
                updateStatus(
                    reportStatusMessage,
                    context.getString(R.string.report_status_updater_log_sent_successfully)
                )
        if (response.isSuccessful)
            reportSent += 1
        if (reportSent == reportCount) {
            reportSent = 0
            isLogReported.postValue(true)
            File(context.cacheDir, Constants.KERNEL_LOG).apply { if(exists()) delete() }
            File(context.cacheDir, Constants.UPDATER_LOG).apply { if(exists()) delete() }
        }
    }

    /**
     * Helper method to show the current status of logs to the user
     */
    private fun updateStatus(reportStatusMessage: MutableLiveData<String>, reportMessage: String) {
        reportStatusMessage.postValue(reportMessage)
    }

    // Extension function for converting Boolean -> Int
    private fun Boolean.toInt(): Int {
        return if (this)
            1
        else
            0
    }
}
