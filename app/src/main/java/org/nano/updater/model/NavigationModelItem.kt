package org.nano.updater.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import org.nano.updater.R

sealed class NavigationModelItem {
    data class NavMenuItem(
        val id: Int = 0,
        @DrawableRes val icon: Int = R.drawable.ic_home,
        @StringRes val titleRes: Int = R.string.action_home,
        var checked: Boolean = true
    ) : NavigationModelItem()

    object NavModelItemDiff : DiffUtil.ItemCallback<NavMenuItem>() {
        override fun areItemsTheSame(
            oldItem: NavMenuItem,
            newItem: NavMenuItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: NavMenuItem,
            newItem: NavMenuItem
        ): Boolean {
            return oldItem.icon == newItem.icon &&
                    oldItem.titleRes == newItem.titleRes &&
                    oldItem.checked == newItem.checked
        }
    }
}