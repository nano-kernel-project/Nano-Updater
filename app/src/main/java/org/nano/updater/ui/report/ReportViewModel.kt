package org.nano.updater.ui.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import org.nano.updater.repository.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportViewModel @Inject constructor(private val reportRepository: ReportRepository) :
    ViewModel() {
    private val reportStatusMessage: MutableLiveData<String> = MutableLiveData()
    private var isLogReported: MutableLiveData<Boolean?> = MutableLiveData(null)

    fun generateLogs(isUpdaterLog: Boolean, isKernelLog: Boolean) {
        if (Shell.getShell().isRoot)
            viewModelScope.launch {
                reportRepository.generateLogs(
                    isUpdaterLog,
                    isKernelLog,
                    reportStatusMessage,
                    isLogReported
                )
            }
        else
            isLogReported.value = false
    }

    fun getReportStatusMessage(): LiveData<String> = reportStatusMessage

    fun getIsLogReported(): LiveData<Boolean?> = isLogReported

    fun setIsLogReported(isLogReported: Boolean?) {
        this.isLogReported.value = isLogReported
    }

    fun setReportStatusMessage(reportStatusMessage: String) {
        this.reportStatusMessage.value = reportStatusMessage
    }
}
