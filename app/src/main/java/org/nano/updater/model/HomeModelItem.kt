package org.nano.updater.model

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil

sealed class HomeModelItem {
    data class InformationCard(
        val id: Long,
        val position: Int,
        val caption: String,
        @DrawableRes val icon: Int,
        val title: String,
        val description: String,
        val updateAvailable: Boolean?
    ) : HomeModelItem()

    data class UpdateCard(
        val id: Long,
        val position: Int,
        val lastChecked: String,
        @DrawableRes val icon: Int,
        val title: String,
        val currentVersion: String?,
        val latestVersion: String,
        val currentBuild: String?,
        val status: String,
        val updateAvailable: Boolean?
    ) : HomeModelItem()

    object HomeDiffUtil : DiffUtil.ItemCallback<HomeModelItem>() {
        override fun areItemsTheSame(oldItem: HomeModelItem, newItem: HomeModelItem): Boolean {
            return if (oldItem is UpdateCard && newItem is UpdateCard)
                oldItem.id == newItem.id
            else if (oldItem is InformationCard && newItem is InformationCard)
                oldItem.id == newItem.id
            else
                false
        }

        override fun areContentsTheSame(oldItem: HomeModelItem, newItem: HomeModelItem): Boolean {
            return if (oldItem is UpdateCard && newItem is UpdateCard) {
                (oldItem.currentBuild == newItem.currentBuild &&
                        oldItem.currentVersion == newItem.currentVersion &&
                        oldItem.icon == newItem.icon &&
                        oldItem.lastChecked == newItem.lastChecked &&
                        oldItem.latestVersion == newItem.latestVersion &&
                        oldItem.status == newItem.status &&
                        oldItem.title == newItem.title &&
                        oldItem.updateAvailable == newItem.updateAvailable)
            } else if (oldItem is InformationCard && newItem is InformationCard) {
                (oldItem.caption == newItem.caption &&
                        oldItem.description == newItem.description &&
                        oldItem.icon == newItem.icon &&
                        oldItem.title == newItem.title &&
                        oldItem.updateAvailable == newItem.updateAvailable)
            } else
                false
        }
    }
}