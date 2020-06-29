package org.nano.updater.ui.flash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.nano.updater.databinding.ItemConsoleBinding
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsoleAdapter @Inject constructor(): RecyclerView.Adapter<ConsoleAdapter.ConsoleViewHolder>() {
    private lateinit var liveFlashLog: ArrayList<String>

    fun setLiveFlashLog(liveFlashLog: ArrayList<String>) {
        this.liveFlashLog = liveFlashLog
    }

    class ConsoleViewHolder(private val binding: ItemConsoleBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(flashLog: String) {
            binding.run {
                this.flashLog = flashLog
                executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsoleViewHolder {
        val binding = ItemConsoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConsoleViewHolder(binding)
    }

    override fun getItemCount(): Int = liveFlashLog.size

    override fun onBindViewHolder(holder: ConsoleViewHolder, position: Int) = holder.bind(liveFlashLog[position])
}
