package org.nano.updater.ui.update

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nano.updater.databinding.ItemChangelogBinding

class ChangelogAdapter :
    ListAdapter<String, ChangelogAdapter.ChangelogViewHolder>(ChangelogDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangelogViewHolder {
        return ChangelogViewHolder(
            ItemChangelogBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ChangelogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChangelogViewHolder(val binding: ItemChangelogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(changelog: String) {
            binding.run {
                changelogItem = changelog.substring(1).trimStart()
                executePendingBindings()
            }
        }
    }

    object ChangelogDiffUtil : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}