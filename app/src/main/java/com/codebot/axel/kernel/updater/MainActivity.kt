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
 * Last modified 26/7/19 10:30 PM.
 */

package com.codebot.axel.kernel.updater

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.codebot.axel.kernel.updater.about.AboutActivity
import com.codebot.axel.kernel.updater.model.Nano
import com.codebot.axel.kernel.updater.util.Constants.Companion.API_ENDPOINT_URL
import com.codebot.axel.kernel.updater.util.Constants.Companion.BUILD_DATE
import com.codebot.axel.kernel.updater.util.Constants.Companion.BUILD_VERSION
import com.codebot.axel.kernel.updater.util.Constants.Companion.CHECK_FOR_UPDATES
import com.codebot.axel.kernel.updater.util.Constants.Companion.DOWNLOAD
import com.codebot.axel.kernel.updater.util.Constants.Companion.ROTATE_ANIMATION
import com.codebot.axel.kernel.updater.util.DownloadUtils
import com.codebot.axel.kernel.updater.util.Utils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_layout.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.layout_flash_expanded.*
import kotlinx.android.synthetic.main.layout_update_card.*
import kotlinx.android.synthetic.main.package_list_item.*
import kotlinx.android.synthetic.main.update_info_layout.*
import okhttp3.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    val context = this
    private lateinit var preferenceManager: SharedPreferences
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var nanoData: Nano? = null
    private var downloadId: Long = 0
    private var buildVersion = ""
    private var buildDate = ""
    private lateinit var downloadManager: DownloadManager
    private lateinit var onDownloadComplete: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        check_update.drawable.mutate().setTint(Color.WHITE)

        packageInfoTextView.isSelected = true
        md5InfoTextView.isSelected = true

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)

        buildDate = Utils().checkInstalledVersion(BUILD_DATE)
        buildVersion = Utils().checkInstalledVersion(BUILD_VERSION)


        isStoragePermissionGranted()

        fileName.isSelected = true
        expanded_packageInfoTextView.isSelected = true

        if (buildDate != "")
            current_version_textView.text = "Nano $buildVersion • ${Utils().formatDate(buildDate)}"

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, bottomSheetBehavior.peekHeight * 2 + 16)
        update_info_expanded.layoutParams = layoutParams

        if (!preferenceManager.getBoolean(getString(R.string.key_is_root_checked), false)) {
            try {
                Runtime.getRuntime().exec("su")
                preferenceManager.edit().putBoolean(getString(R.string.key_is_root_checked), true).apply()
            } catch (e: Exception) {
                Toast.makeText(context, "Device is not rooted", Toast.LENGTH_SHORT).show()
                preferenceManager.edit().putBoolean(getString(R.string.key_is_root_checked), true).apply()
            }
        }

        // Attempt to load json data
        Utils().startRefreshAnimation(context, ROTATE_ANIMATION)
        fetchJSON(ROTATE_ANIMATION, CHECK_FOR_UPDATES)

        onDownloadComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                downloadButton.isEnabled = true
                update_fileDownload.isEnabled = true

                //Fetching the download id received with the broadcast
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)

                if (DownloadUtils().isDownloadSuccessful(context, id, downloadManager)[0] == "status_successful") {
                    Toast.makeText(context, "Build downloaded successfully", Toast.LENGTH_SHORT).show()
                    (context as Activity).updates_compact.visibility = View.GONE
                    context.update_info_expanded.visibility = View.GONE
                    context.update_progressBar.visibility = View.GONE
                    context.progress_info.visibility = View.GONE
                    context.downloadButton.visibility = View.VISIBLE
                    context.update_fileDownload.visibility = View.VISIBLE
                    val packageName = getDownloadedFileName(id)
                    val installPackage = File("${Environment.getExternalStorageDirectory().path}/kernel.updater/builds/$packageName")
                    context.fileName.text = installPackage.name
                    context.fileDate.text = Utils().formatDate(installPackage.lastModified().toString())
                    context.fileSize.text = "${installPackage.length() / 1000000} MB"

                    context.expanded_sizeInfoTextView.text = "${installPackage.length() / 1000000} MB"
                    context.expanded_packageInfoTextView.text = installPackage.name
                    context.expanded_dateInfoTextView.text = Utils().formatDate(installPackage.lastModified().toString())
                    context.packageInfoCompact.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@MainActivity, "Download failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        // Set OnClickListeners
        setListeners()

    }

    private fun setListeners() {
        update_fileDownload.setOnClickListener {
            update_fileDownload.isEnabled = false
            downloadButton.isEnabled = false
            fetchJSON(ROTATE_ANIMATION, DOWNLOAD)
        }

        downloadButton.setOnClickListener {
            update_fileDownload.isEnabled = false
            downloadButton.isEnabled = false
            fetchJSON(ROTATE_ANIMATION, DOWNLOAD)
        }

        check_update.setOnClickListener {
            Utils().startRefreshAnimation(context, ROTATE_ANIMATION)
            fetchJSON(ROTATE_ANIMATION, CHECK_FOR_UPDATES)
        }

        updates_compact.setOnClickListener {
            updates_compact.visibility = View.GONE
            update_info_expanded.visibility = View.VISIBLE
        }

        update_info_expanded.setOnClickListener {
            update_info_expanded.visibility = View.GONE
            updates_compact.visibility = View.VISIBLE
        }

        packageInfoCompact.setOnClickListener {
            packageInfoCompact.visibility = View.GONE
            packageInfoExpanded.visibility = View.VISIBLE
        }

        packageInfoExpanded.setOnClickListener {
            packageInfoExpanded.visibility = View.GONE
            packageInfoCompact.visibility = View.VISIBLE
        }

        nav_menu.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            else
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        nav_view.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_about -> {
                    startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                }
                R.id.nav_feedback -> {
                    startActivity(Intent(this@MainActivity, FeedbackActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                }
                R.id.nav_flasher -> {
                    startActivity(Intent(this@MainActivity, FlashActivity::class.java))
                }
                R.id.nav_changelog -> {
                    startActivity(Intent(this@MainActivity, ChangelogActivity::class.java))
                }
            }
            true
        }
    }

    private fun getDownloadedFileName(id: Long): String {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
        }
        return ""
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            true
        else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            false
        }
    }

    private fun fetchJSON(animation: RotateAnimation, currentTask: String) {
        if (!Utils().isNetworkAvailable(context)) {
            Utils().snackBar(this@MainActivity, "No connection. Attempting to load offline data")

            // Get offline data
            val bodyOfJSON = Utils().loadOfflineData(this@MainActivity)
            if (bodyOfJSON != "") {
                nanoData = GsonBuilder().create().fromJson(bodyOfJSON, Nano::class.java)
                loadData(nanoData)
                executeCurrentTask(currentTask)
            } else
                Utils().snackBar(this@MainActivity, "Failed to load offline data")

            Utils().stopRefreshAnimation(animation)
        } else {
            val client = OkHttpClient()
            val request = Request.Builder().url(API_ENDPOINT_URL).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    e!!.printStackTrace()
                }

                override fun onResponse(call: Call?, response: Response?) {
                    val bodyOfJSON = response?.body()?.string()
                    val gson = GsonBuilder().create()
                    nanoData = gson.fromJson(bodyOfJSON, Nano::class.java)

                    // Save an offline copy of the response string to be used further
                    Utils().saveJSONtoPreferences(context, bodyOfJSON)

                    loadData(nanoData)
                    executeCurrentTask(currentTask)
                }
            })
        }
    }

    private fun executeCurrentTask(currentTask: String) {
        when (currentTask) {
            DOWNLOAD -> {
                runOnUiThread {
                    downloadId = DownloadUtils().downloadPackage(context, downloadManager, nanoData)
                }
            }
            CHECK_FOR_UPDATES -> {
                runOnUiThread {
                    Utils().isUpdateAvailable(context, nanoData, buildDate, ROTATE_ANIMATION)
                }
            }
        }
    }

    private fun loadData(nanoData: Nano?) {
        val nanoPackage = if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_miui_check), false))
            nanoData!!.MIUI
        else
            nanoData!!.AOSP
        runOnUiThread {
            ChangelogTask(context).execute(nanoPackage[0].changelog_url, changelogView, this@MainActivity)
            latest_version_textView.text = "Nano " + nanoPackage[0].release_number + " • " + Utils().formatDate(nanoPackage[0].date)
            update_fileName.text = "Nano Kernel ${nanoPackage[0].release_number}"
            update_fileSize.text = nanoPackage[0].size
            update_timestamp.text = Utils().formatDate(nanoPackage[0].date)
            packageInfoTextView.text = nanoPackage[0].filename
            sizeInfoTextView.text = nanoPackage[0].size
            md5InfoTextView.text = nanoPackage[0].md5
        }
    }

    override fun onStart() {
        nav_view.setCheckedItem(R.id.nav_home)
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.edit().putBoolean(getString(R.string.is_json_saved), false).apply()
        preferenceManager.edit().putBoolean(getString(R.string.key_is_root_checked), false).apply()
        unregisterReceiver(onDownloadComplete)
    }
}
