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

package com.codebot.axel.nano

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.nano.adapter.ChangelogVersionAdapter
import com.codebot.axel.nano.model.Nano
import com.codebot.axel.nano.util.Utils
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_changelog.*
import okhttp3.*
import java.io.IOException

class ChangelogActivity : AppCompatActivity() {

    private var nanoJSONUrl = "https://raw.githubusercontent.com/nano-kernel-project/Nano_OTA_changelogs/master/api.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog)

        initialize()

        changelog_retryButton.setOnClickListener {
            initialize()
        }
    }

    private fun initialize() {
        if (Utils().isNetworkAvailable(this)) {
            changelogLayout.visibility = View.VISIBLE
            changelog_networkDisconnected.visibility = View.GONE
            fetchJSON()
        } else {
            if (changelog_progress_circular.visibility == View.VISIBLE) {
                changelog_progress_circular.hide()
                changelog_progress_circular.visibility = View.GONE
            }
            changelogLayout.visibility = View.GONE
            changelog_networkDisconnected.visibility = View.VISIBLE
        }
    }

    private fun fetchJSON() {
        changelog_progress_horizontal.visibility = View.VISIBLE
        changelog_progress_horizontal.show()
        if (!Utils().isNetworkAvailable(this)) {
            Utils().snackBar(this@ChangelogActivity, "No connection")
            changelog_progress_horizontal.hide()
            changelog_progress_horizontal.visibility = View.GONE
        } else {
            val client = OkHttpClient()
            val request = Request.Builder().url(nanoJSONUrl).build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call?, e: IOException?) {
                    e!!.printStackTrace()
                }

                override fun onResponse(call: Call?, response: Response?) {
                    val bodyOfJSON = response?.body()?.string()
                    val gson = GsonBuilder().create()
                    val nanoData = gson.fromJson(bodyOfJSON, Nano::class.java)
                    runOnUiThread {
                        versionChangelogRecyclerView.apply {
                            layoutManager = LinearLayoutManager(this@ChangelogActivity, RecyclerView.VERTICAL, false)
                            adapter = ChangelogVersionAdapter(this@ChangelogActivity, nanoData)
                        }
                        changelog_progress_horizontal.hide()
                        changelog_progress_horizontal.visibility = View.GONE

                    }
                }
            })
        }
    }
}
