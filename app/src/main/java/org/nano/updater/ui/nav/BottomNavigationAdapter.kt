package org.nano.updater.ui.nav

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nano.updater.databinding.ItemNavMenuBinding
import org.nano.updater.model.NavigationModelItem

class BottomNavigationAdapter(private val listener: BottomNavigationAdapterListener) :
    ListAdapter<NavigationModelItem.NavMenuItem, BottomNavigationAdapter.BottomNavigationViewHolder>(NavigationModelItem.NavModelItemDiff) {

    interface BottomNavigationAdapterListener {
        fun onNavigationItemClicked(modelItem: NavigationModelItem.NavMenuItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomNavigationViewHolder {
        val binding = ItemNavMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BottomNavigationViewHolder(
            binding,
            listener
        )
    }

    override fun onBindViewHolder(holder: BottomNavigationViewHolder, position: Int) =
        holder.bind(getItem(position))


    class BottomNavigationViewHolder(
        val binding: ItemNavMenuBinding,
        val listener: BottomNavigationAdapterListener
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(navItem: NavigationModelItem.NavMenuItem) {
            binding.run {
                navMenuItem = navItem
                navListener = listener
                executePendingBindings()
            }
        }
    }
}