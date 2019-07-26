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
 * Last modified 22/7/19 7:33 PM.
 */

package com.codebot.axel.nano

import android.os.AsyncTask
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.nano.adapter.ChangelogAdapter
import com.codebot.axel.nano.model.DataHolder
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ChangelogTask : AsyncTask<Any, Void, DataHolder>() {
    override fun doInBackground(vararg params: Any?): DataHolder {
        return DataHolder(getChangelogFromUrl(params[0] as String), (params[1] as RecyclerView))
    }

    override fun onPostExecute(result: DataHolder) {
        result.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = ChangelogAdapter(result.changelogArray)
        }

        super.onPostExecute(result)
    }

    private fun getChangelogFromUrl(url: String): Array<String> {
        val inputStream: InputStream
        val changelogArray = ArrayList<String>()
        try {
            val connection = URL(url).openConnection() as HttpsURLConnection
            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                inputStream = connection.inputStream
                val bufferReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                var line = bufferReader.readLine()
                while (line != null) {
                    changelogArray.add(line)
                    line = bufferReader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e("ChangelogTask", "$e")
        }
        return changelogArray.toArray(Array(changelogArray.size) { "" })
    }
}