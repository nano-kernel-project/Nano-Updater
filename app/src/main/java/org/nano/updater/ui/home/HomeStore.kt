package org.nano.updater.ui.home

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.nano.updater.R
import org.nano.updater.model.HomeModelItem

object HomeStore {
    private val homeItemsList = mutableListOf(
        HomeModelItem.InformationCard(
            0L,
            0,
            "Your status - Unknown",
            R.drawable.ic_help,
            "Status",
            "Cannot retrieve data. Try refreshing. Ensure you have a proper network connection.",
            null
        ),
        HomeModelItem.UpdateCard(
            1L,
            1,
            "Last checked - Never",
            R.drawable.ic_memory,
            "Kernel",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            null
        ),
        HomeModelItem.UpdateCard(
            2L,
            2,
            "Last checked - Never",
            R.drawable.ic_system_update,
            "Updater",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            null
        ),
        HomeModelItem.InformationCard(
            3L,
            3,
            "Your device - ${Build.MODEL}",
            R.drawable.ic_contact_support,
            "Support",
            "Get help from developers or other users. We support Telegram and XDA at the moment.",
            null
        )
    )

    private val _homeItems: MutableLiveData<List<HomeModelItem>> = MutableLiveData()
    val homeItems: LiveData<List<HomeModelItem>>
        get() = _homeItems

    init {
        _homeItems.value =
            homeItemsList
    }

    fun update(position: Int, homeModelItem: HomeModelItem) {
        homeItemsList[position] = homeModelItem
        _homeItems.postValue(
            homeItemsList
        )
    }
}