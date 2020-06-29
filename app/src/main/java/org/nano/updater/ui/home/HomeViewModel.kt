package org.nano.updater.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nano.updater.model.NanoUpdate
import org.nano.updater.repository.UpdateRepository
import org.nano.updater.ui.update.UpdateViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeViewModel @Inject constructor(private val updateRepository: UpdateRepository) :
    ViewModel() {
    private val nanoUpdate: MutableLiveData<NanoUpdate> = MutableLiveData()
    private val downloadStatus = MutableLiveData<UpdateViewModel.DownloadStatus>()
    private val isUnsupportedDevice: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        loadUpdateData(false)
    }

    fun getDownloadStatus() = downloadStatus

    fun getUpdateData(): LiveData<NanoUpdate> = nanoUpdate

    fun getIsUnsupportedDevice() = isUnsupportedDevice

    fun setIsUnsupportedDevice(newValue: Boolean) {
        isUnsupportedDevice.postValue(newValue)
    }

    fun setDownloadStatus(status: UpdateViewModel.DownloadStatus) {
        downloadStatus.postValue(status)
    }

    fun setUpdateData(updateData: NanoUpdate?) {
        nanoUpdate.postValue(updateData)
    }

    fun loadUpdateData(forceRefresh: Boolean) {
        viewModelScope.launch {
            try {
                updateRepository.loadUpdateData(forceRefresh, this@HomeViewModel)
            } catch (e: Exception) {
                Log.e("ViewModel", "$e")
                setUpdateData(getUpdateData().value)
            }
        }
    }
}
