package org.nano.updater.ui.nav

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.nano.updater.R
import org.nano.updater.model.NavigationModelItem

object NavigationModel {

    private var navigationMenuItems = mutableListOf(
        NavigationModelItem.NavMenuItem(
            id = 0,
            icon = R.drawable.ic_home,
            titleRes = R.string.action_home,
            checked = false
        ),
        NavigationModelItem.NavMenuItem(
            id = 1,
            icon = R.drawable.ic_settings,
            titleRes = R.string.action_settings,
            checked = false
        ),
        NavigationModelItem.NavMenuItem(
            id = 2,
            icon = R.drawable.ic_bug_report,
            titleRes = R.string.action_report,
            checked = false
        ),
        NavigationModelItem.NavMenuItem(
            id = 3,
            icon = R.drawable.ic_info,
            titleRes = R.string.action_about,
            checked = false
        )
    )

    private val _navigationList: MutableLiveData<List<NavigationModelItem.NavMenuItem>> =
        MutableLiveData()
    val navigationList: LiveData<List<NavigationModelItem.NavMenuItem>>
        get() = _navigationList

    init {
        postListUpdate()
    }

    fun setNavigationMenuItemChecked(id: Int): Boolean {
        var updated = false
        navigationMenuItems.forEachIndexed { index, item ->
            val shouldCheck = item.id == id
            if (item.checked != shouldCheck) {
                navigationMenuItems[index] = item.copy(checked = shouldCheck)
                updated = true
            }
        }
        if (updated)
            postListUpdate()
        return updated
    }

    private fun postListUpdate() {
        val newList = navigationMenuItems
        _navigationList.value = newList
    }
}