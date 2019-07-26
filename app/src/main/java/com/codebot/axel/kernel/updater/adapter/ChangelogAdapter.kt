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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.kernel.updater.R
import kotlinx.android.synthetic.main.changelog_model.view.*

class ChangelogAdapter(private val sampleData: Array<String>) : RecyclerView.Adapter<ChangelogAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.changelog_model, parent, false)
        return ViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return sampleData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.changelogTextView.text = "${sampleData[position]}"
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}