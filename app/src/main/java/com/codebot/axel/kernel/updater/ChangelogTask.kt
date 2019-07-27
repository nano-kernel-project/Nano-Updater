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
 * Last modified 26/7/19 3:22 PM.
 */

package com.codebot.axel.kernel.updater

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.kernel.updater.adapter.ChangelogAdapter
import com.codebot.axel.kernel.updater.model.DataHolder
import com.codebot.axel.kernel.updater.util.Utils
import kotlinx.android.synthetic.main.activity_changelog.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.ref.WeakReference

class ChangelogTask(context: Context) : AsyncTask<Any, Void, DataHolder>() {
    private val contextWeakReference = WeakReference(context)
    override fun doInBackground(vararg params: Any?): DataHolder {
        return DataHolder(getChangelogFromUrl(params[0] as String), (params[1] as RecyclerView))
    }

    override fun onPostExecute(result: DataHolder) {
        if (result.changelogArray.size == 1 && result.changelogArray[0] == "") {
            val context = contextWeakReference.get()
            if (context is ChangelogActivity) {
                (context as Activity).changelogLayout.visibility = View.GONE
                context.changelog_networkDisconnected.visibility = View.VISIBLE
            }
        }
        result.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ChangelogAdapter(result.changelogArray)
        }

        super.onPostExecute(result)
    }

    private fun getChangelogFromUrl(url: String): Array<String> {
        val context = contextWeakReference.get()
        val inputStream: InputStream
        val changelogArray = ArrayList<String>()
        if (!Utils().isNetworkAvailable(contextWeakReference.get()!!) || PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context!!.getString(R.string.is_changelog_saved), false)) {
            val changelog = Utils().loadOfflineChangelog(contextWeakReference.get()!!)
            return changelog.toArray(Array(changelog.size) { "" })
        }
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val connection = client.newCall(request).execute().body()!!
            inputStream = connection.byteStream()
            val bufferReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            var line = bufferReader.readLine()
            var offlineChangelogData = ""
            while (line != null) {
                changelogArray.add(line)
                offlineChangelogData = if (offlineChangelogData == "")
                    line
                else
                    offlineChangelogData + "\n" + line
                line = bufferReader.readLine()
            }
            Utils().saveChangelogOffline(contextWeakReference.get()!!, offlineChangelogData)
        } catch (e: Exception) {
            if (context is ChangelogActivity) {
                (context as Activity).runOnUiThread {
                    context.changelogLayout.visibility = View.GONE
                    context.changelog_networkDisconnected.visibility = View.VISIBLE
                }
            }
            Log.e("ChangelogTask", "$e")
        }
        return changelogArray.toArray(Array(changelogArray.size) { "" })
    }
}