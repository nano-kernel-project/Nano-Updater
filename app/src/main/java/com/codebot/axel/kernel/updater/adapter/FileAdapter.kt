/*
 * Copyright (c) 2019. The Nano Kernel Project.  All rights reserved.
 * Developed by Axel <karthikgaddam4@gmail.com>.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work.
 *
 * Last modified 22/7/19 7:19 PM.
 */

package com.codebot.axel.kernel.updater.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.kernel.updater.FlashKernelTask
import com.codebot.axel.kernel.updater.R
import com.codebot.axel.kernel.updater.model.Package
import com.codebot.axel.kernel.updater.util.FlashKernel
import kotlinx.android.synthetic.main.layout_flash_expanded.view.*
import kotlinx.android.synthetic.main.package_list_item.view.*

class FileAdapter(private val packageList: ArrayList<Package>, private val context: Context) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.package_list_item, parent, false)
        return ViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return packageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.packageInfoCompact.setOnClickListener {
            holder.view.packageInfoCompact.visibility = View.GONE
            holder.view.packageInfoExpanded.visibility = View.VISIBLE
        }
        holder.view.packageInfoExpanded.setOnClickListener {
            holder.view.packageInfoCompact.visibility = View.VISIBLE
            holder.view.packageInfoExpanded.visibility = View.GONE
        }

        holder.view.autoFlasherImage.setOnClickListener {
            FlashKernel().flashPackage(packageList[position].absolutePath)
        }

        holder.view.expanded_autoFlasherImage.setOnClickListener {
            FlashKernel().flashPackage(packageList[position].absolutePath)
        }

        holder.view.flasherImage.setOnClickListener {
            FlashKernelTask(context).execute(context, packageList[position].absolutePath)
        }

        holder.view.expanded_flasherImage.setOnClickListener {
            FlashKernelTask(context).execute(context, packageList[position].absolutePath)
        }

        holder.view.fileName.isSelected = true
        holder.view.expanded_packageInfoTextView.isSelected = true
        holder.view.isSelected = true

        holder.view.fileName.text = packageList[position].fileName
        holder.view.fileDate.text = packageList[position].timestamp
        holder.view.fileSize.text = "${packageList[position].size} MB"

        holder.view.expanded_sizeInfoTextView.text = "${packageList[position].size} MB"
        holder.view.expanded_packageInfoTextView.text = packageList[position].fileName
        holder.view.expanded_dateInfoTextView.text = packageList[position].timestamp

        holder.view.packageInfoCompact.visibility = View.VISIBLE
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}