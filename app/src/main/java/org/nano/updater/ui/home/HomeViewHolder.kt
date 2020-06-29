package org.nano.updater.ui.home

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.nano.updater.databinding.ItemViewInfoBinding
import org.nano.updater.databinding.ItemViewUpdateBinding
import org.nano.updater.model.HomeModelItem

sealed class HomeViewHolder<T : HomeModelItem>(view: View) : RecyclerView.ViewHolder(view) {

    abstract fun bind(homeItem: T, position: Int)

    class UpdateCardViewHolder(
        private val binding: ItemViewUpdateBinding,
        private val listener: HomeAdapter.HomeAdapterListener
    ) : HomeViewHolder<HomeModelItem.UpdateCard>(binding.root) {

        override fun bind(homeItem: HomeModelItem.UpdateCard, position: Int) {
            binding.run {
                updateCard = homeItem
                this.listener = this@UpdateCardViewHolder.listener
                executePendingBindings()
            }
        }
    }

    class InfoCardViewHolder(
        private val binding: ItemViewInfoBinding,
        private val listener: HomeAdapter.HomeAdapterListener
    ) : HomeViewHolder<HomeModelItem.InformationCard>(binding.root) {

        override fun bind(homeItem: HomeModelItem.InformationCard, position: Int) {
            binding.run {
                informationCard = homeItem
                this.listener = this@InfoCardViewHolder.listener
            }
        }
    }
}