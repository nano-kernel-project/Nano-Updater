package org.nano.updater.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.nano.updater.databinding.ItemViewInfoBinding
import org.nano.updater.databinding.ItemViewUpdateBinding
import org.nano.updater.model.HomeModelItem

private const val VIEW_TYPE_UPDATE = 0
private const val VIEW_TYPE_INFO = 1

class HomeAdapter(private val listener: HomeAdapterListener) :
    ListAdapter<HomeModelItem, HomeViewHolder<HomeModelItem>>(HomeModelItem.HomeDiffUtil) {

    interface HomeAdapterListener {
        fun onUpdateCardClicked(cardView: View, position: Int)
        fun onInfoCardClicked(position: Int)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HomeViewHolder<HomeModelItem> {
        return if (viewType == VIEW_TYPE_UPDATE)
            HomeViewHolder.UpdateCardViewHolder(
                ItemViewUpdateBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                ), listener
            ) as HomeViewHolder<HomeModelItem>
        else
            HomeViewHolder.InfoCardViewHolder(
                ItemViewInfoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), listener
            ) as HomeViewHolder<HomeModelItem>
    }

    override fun onBindViewHolder(holder: HomeViewHolder<HomeModelItem>, position: Int) =
        holder.bind(getItem(position), position)

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 || position == 3)
            VIEW_TYPE_INFO
        else
            VIEW_TYPE_UPDATE
    }
}
