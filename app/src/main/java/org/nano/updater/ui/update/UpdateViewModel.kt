package org.nano.updater.ui.update

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateViewModel @Inject constructor() :
    ViewModel() {
    private val isFlashingComplete: MutableLiveData<ArrayList<Any>> = MutableLiveData()
    private val isFlashServiceRunning: MutableLiveData<Boolean> = MutableLiveData()
    private val hideView: MutableLiveData<Boolean> = MutableLiveData()
    private val flashEndStatus: MutableLiveData<String> = MutableLiveData()

    fun setIsFlashingComplete(isFlashingComplete: ArrayList<Any>) {
        this.isFlashingComplete.postValue(isFlashingComplete)
    }

    fun setHideViews(hideView: Boolean) {
        this.hideView.value = hideView
    }

    fun setFlashEndStatus(status: String) {
        flashEndStatus.value = status
    }

    fun setIsFlashServiceRunning(isFlashServiceRunning: Boolean) {
        this.isFlashServiceRunning.value = isFlashServiceRunning
    }

    fun getIsFlashServiceRunning(): LiveData<Boolean> = isFlashServiceRunning

    fun getFlashEndStatus(): LiveData<String> = flashEndStatus

    fun getHideViews(): LiveData<Boolean> = hideView

    fun getIsFlashingComplete(): LiveData<ArrayList<Any>> = isFlashingComplete

    enum class DownloadStatus {
        COMPLETED,
        RUNNING,
        CANCELLED
    }
}
