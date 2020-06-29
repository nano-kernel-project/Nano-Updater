package org.nano.updater.util

import android.content.Context
import android.content.Intent
import android.os.Handler
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.service.FlashService
import org.nano.updater.ui.flash.ConsoleAdapter
import org.nano.updater.ui.update.UpdateViewModel
import org.nano.updater.worker.WorkerUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class FlashUtils @Inject constructor(context: Context) {
    private var isUnsupportedDevice = false

    init {
        (context.applicationContext as NanoApplication).appComponent.inject(this)
    }

    @Inject
    lateinit var updateViewModel: UpdateViewModel

    suspend fun flashKernel(
        context: Context,
        absolutePath: String,
        liveLog: ArrayList<String>,
        consoleAdapter: ConsoleAdapter
    ) =
        withContext(Dispatchers.IO) {
            // Verify if root permissions are granted
            if (!Shell.getShell().isRoot) {
                displayLogToConsole(context, liveLog, consoleAdapter, "No root access :(")
                updateViewModel.setIsFlashingComplete(
                    arrayListOf(
                        false,
                        context.getString(R.string.not_enough_permissions)
                    )
                )
                // Stop FlashService
                context.stopService(Intent(context, FlashService::class.java))
                return@withContext
            }

            // Hold a reference to update-binary path
            val updateBinaryPath = "/META-INF/com/google/android/update-binary"

            // Extract parent dir from absolutePath
            val parentPath = absolutePath.substring(0, absolutePath.lastIndexOf("/") + 1)

            // Required to rename the file if unwanted chars are present
            var modifiedPath = ""

            // Extract the file name from absolutePath
            var fileName =
                absolutePath.substring(absolutePath.lastIndexOf("/") + 1, absolutePath.length)

            // Remove any white spaces, brackets in the file name
            val unwantedCharsPresent = !fileName.matches("[^()\\s\\[\\]]+".toRegex())
            if (unwantedCharsPresent) {
                fileName = fileName.replace("[()\\s]".toRegex()) { "" }
                modifiedPath = parentPath + fileName

                // Rename file with modified name
                val origFile = File(absolutePath)
                val renamedFile = File(modifiedPath)
                origFile.renameTo(renamedFile)
            }

            // Mount / as RW
            execAndDisplayLog(context, "mount -o rw,remount /", liveLog, consoleAdapter)

            // Create a tmp path for unzipAndCheck operations
            val tempPath = "${context.getExternalFilesDir(null)!!.path}/tmp"

            // Delete tmp path if exists
            if (File(tempPath).exists())
                File(tempPath).deleteRecursively()

            displayLogToConsole(
                context,
                liveLog,
                consoleAdapter,
                "Setting up flashing environment...\n"
            )

            File(tempPath).mkdirs()

            // If file name doesn't contain any special chars, unzip original file
            // Otherwise unzip modified file
            // Start executing the updater-binary that in turn invokes AnyKernel flashing script
            if (!unwantedCharsPresent) {
                unzip(context, liveLog, consoleAdapter, absolutePath, tempPath)
                displayLogToConsole(context, liveLog, consoleAdapter, "Flashing $fileName\n")
                execAndDisplayLog(
                    context,
                    "sh $tempPath$updateBinaryPath dummy 1 $absolutePath",
                    liveLog,
                    consoleAdapter
                )
            } else {
                unzip(context, liveLog, consoleAdapter, modifiedPath, tempPath)
                displayLogToConsole(context, liveLog, consoleAdapter, "Flashing $fileName...\n")
                execAndDisplayLog(
                    context,
                    "sh $tempPath$updateBinaryPath dummy 1 $modifiedPath",
                    liveLog,
                    consoleAdapter
                )
            }

            displayLogToConsole(context, liveLog, consoleAdapter, "\nCleaning up...")

            // Clean up the tmp dir
            File(tempPath).deleteRecursively()

            // Mount / as RO
            execAndDisplayLog(context, "mount -o ro,remount /", liveLog, consoleAdapter)

            // Exit
            execAndDisplayLog(context, "exit", liveLog, consoleAdapter)

            Shell.getShell().close()

            // Stop FlashService
            context.stopService(Intent(context, FlashService::class.java))
        }

    private fun getStreamFromList(flashOutput: ArrayList<String>): ByteArrayInputStream {
        val logBuilder = StringBuilder()
        for (line in flashOutput)
            logBuilder.append(line).append("\n")

        return logBuilder.toString().byteInputStream(Charsets.UTF_8)
    }

    private fun execAndDisplayLog(
        context: Context,
        cmd: String,
        liveLog: ArrayList<String>,
        consoleAdapter: ConsoleAdapter
    ) {
        val logs = object : CallbackList<String>() {
            override fun onAddElement(log: String) {
                var tempLog: String = log
                if (tempLog.contains("ui_print", true) && tempLog.trim() != "ui_print")
                // substring(1) so as to remove the white space at index 0
                // trim() might mess with the logo
                    tempLog = tempLog.replace("ui_print", "").substring(1)

                if (!tempLog.contains("progress") &&
                    !tempLog.contains("Archive: ", true) &&
                    !tempLog.contains("Inflating: ", true) &&
                    tempLog.trim() != "ui_print"
                ) {
                    if (tempLog.contains("Unsupported"))
                        isUnsupportedDevice = true
                    displayLogToConsole(context, liveLog, consoleAdapter, tempLog)
                }
            }
        }

        Shell.su(cmd).to(logs).exec()

        // If cmd is exit, we're done with executing all the commands
        // Save the flash log to Android/data/org.nano.updater/logs/
        if (cmd == "exit") {
            val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
            val now = Date()
            val logFile: String = "log_" + formatter.format(now).toString() + ".txt"

            // Clean up directories if toggled by user
            val sharedPrefUtils = SharedPrefUtils(context)
            if (sharedPrefUtils.isCleanupDirToggled()) {
                displayLogToConsole(
                    context,
                    liveLog,
                    consoleAdapter,
                    "Deleting kernel update dir (toggled in settings)"
                )
                File(context.getExternalFilesDir("kernel")!!.absolutePath).deleteRecursively()
            }

            displayLogToConsole(context, liveLog, consoleAdapter, "\nWriting logs to ${context.getExternalFilesDir("logs")}/$logFile...")

            // Save logs to file in /sdcard/Android/data/org.nano.updater/files/logs/
            WorkerUtils.saveStreamToFile(
                getStreamFromList(liveLog),
                File(context.getExternalFilesDir("logs"), logFile)
            )

            displayLogToConsole(context, liveLog, consoleAdapter, "\nDone")

            updateViewModel.setIsFlashingComplete(
                if (!isUnsupportedDevice)
                    arrayListOf(
                        true,
                        "Flashing successful. You may share flash logs now for debugging."
                    )
                else
                    arrayListOf(
                        true,
                        "Unsupported device. Flashing aborted"
                    )
            )

            // Check if user toggled reboot after flash
            if (sharedPrefUtils.isRebootAfterFlashToggled()) {
                displayLogToConsole(
                    context,
                    liveLog,
                    consoleAdapter,
                    "\nReboot is toggled in settings."
                )
                displayLogToConsole(context, liveLog, consoleAdapter, "\nRebooting in a moment...")
                Shell.su(Constants.SHUTDOWN_BROADCAST).exec()
                Shell.su(Constants.SYNC).exec()
                Shell.su(Constants.NORMAL_REBOOT_CMD).exec()
            }
        }
    }

    private fun displayLogToConsole(
        context: Context,
        liveLog: ArrayList<String>,
        consoleAdapter: ConsoleAdapter,
        log: String
    ) {
        liveLog.add(log)
        Handler(context.mainLooper).post { consoleAdapter.notifyItemChanged(liveLog.size - 1) }
    }

    private suspend fun unzip(
        context: Context,
        liveLog: ArrayList<String>,
        consoleAdapter: ConsoleAdapter,
        absolutePath: String,
        tempPath: String
    ) = withContext(Dispatchers.IO) {
        try {
            FileUtils.unzip(absolutePath, tempPath)
        } catch (e: Exception) {
            displayLogToConsole(context, liveLog, consoleAdapter, "\nUnzip error!")
            updateViewModel.setIsFlashingComplete(
                arrayListOf(
                    false,
                    context.getString(R.string.not_enough_permissions)
                )
            )
            return@withContext
        }
    }
}