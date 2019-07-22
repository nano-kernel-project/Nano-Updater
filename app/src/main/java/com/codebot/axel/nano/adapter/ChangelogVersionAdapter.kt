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

package com.codebot.axel.nano.adapter

import android.content.Context
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.nano.ChangelogTask
import com.codebot.axel.nano.R
import com.codebot.axel.nano.model.Nano
import com.codebot.axel.nano.util.Utils
import kotlinx.android.synthetic.main.version_changelog_model.view.*

class ChangelogVersionAdapter(private val context: Context, private val nanoData: Nano) : RecyclerView.Adapter<ChangelogVersionAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.version_changelog_model, parent, false)
        return ViewHolder(inflater)
    }

    override fun getItemCount(): Int {
        return if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_miui_check), false))
            nanoData.MIUI.size
        else
            nanoData.AOSP.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.changelog_detail.visibility = View.GONE
        val nanoPackage = if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_miui_check), false))
            nanoData.MIUI
        else
            nanoData.AOSP
        holder.view.changelog_version.text = "Nano ${nanoPackage[position].release_number} • ${Utils().formatDate(nanoPackage[position].date)}"
        holder.view.changelog_layout_item.setOnClickListener {
            if (holder.view.changelog_detail.visibility == View.GONE)
                holder.view.changelog_detail.visibility = View.VISIBLE
            else
                holder.view.changelog_detail.visibility = View.GONE
        }

        ChangelogTask().execute(nanoPackage[position].changelog_url, holder.view.changelog_detailRecyclerView, context)
        setScrollListener(holder.view.changelog_detailRecyclerView)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    private fun setScrollListener(recyclerView: RecyclerView) {
        val mScrollChangeListener = object : RecyclerView.OnItemTouchListener {
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_MOVE -> {
                        rv.parent.requestDisallowInterceptTouchEvent(true)
                    }
                }
                return false
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        }
        recyclerView.addOnItemTouchListener(mScrollChangeListener)
    }
}