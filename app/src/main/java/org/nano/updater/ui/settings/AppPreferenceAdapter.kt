package org.nano.updater.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nano.updater.databinding.ItemSettingsBinding
import org.nano.updater.model.AppPreference

class AppPreferenceAdapter(
    private val preferenceListener: AppPreferenceAdapterListener
) : ListAdapter<AppPreference, AppPreferenceAdapter.AppPreferenceViewHolder>(PreferenceDiffUtil) {

    interface AppPreferenceAdapterListener {
        fun onPreferenceClick(appPreference: AppPreference)
    }

    class AppPreferenceViewHolder(
        private val binding: ItemSettingsBinding,
        private val preferenceListener: AppPreferenceAdapterListener
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appPreference: AppPreference) {
            binding.run {
                preference = appPreference
                listener = preferenceListener
                executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppPreferenceViewHolder {
        val binding =
            ItemSettingsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppPreferenceViewHolder(binding, preferenceListener)
    }

    override fun onBindViewHolder(holder: AppPreferenceViewHolder, position: Int) =
        holder.bind(getItem(position))

    object PreferenceDiffUtil : DiffUtil.ItemCallback<AppPreference>() {
        override fun areItemsTheSame(oldItem: AppPreference, newItem: AppPreference): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppPreference, newItem: AppPreference): Boolean {
            return oldItem.title == newItem.title && oldItem.summary == newItem.summary
        }
    }
}