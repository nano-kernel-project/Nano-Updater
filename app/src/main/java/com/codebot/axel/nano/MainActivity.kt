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

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.codebot.axel.nano.about.AboutActivity
import com.codebot.axel.nano.model.Nano
import com.codebot.axel.nano.util.DownloadUtils
import com.codebot.axel.nano.util.Utils
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

private const val API_ENDPOINT_URL = "https://raw.githubusercontent.com/nano-kernel-project/Nano_OTA_changelogs/master/api.json"
private const val CHECK_FOR_UPDATES = "Check For Updates"
private const val DOWNLOAD = "Download"
private const val BUILD_DATE = "nano.release.date"
private const val BUILD_VERSION = "nano.version"

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
    private val animation = RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("MainActivity", "onCreate")
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

        if (Utils().isNetworkAvailable(context)) {
            Utils().startRefreshAnimation(context, animation)
            fetchJSON(animation, CHECK_FOR_UPDATES)
        }

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
                    val installPackage = File("${Environment.getExternalStorageDirectory().path}/Nano/$packageName")
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
        // Toast.makeText(this, context.packageManager.getPackageInfo(context.packageName, 0).versionName, Toast.LENGTH_SHORT).show()
        if (!Utils().isNetworkAvailable(context))
            Utils().snackBar(context, "No connection")

        // Set OnClickListeners
        setListeners()

    }

    private fun setListeners() {
        update_fileDownload.setOnClickListener {
            update_fileDownload.isEnabled = false
            downloadButton.isEnabled = false
            fetchJSON(animation, DOWNLOAD)
        }

        downloadButton.setOnClickListener {
            update_fileDownload.isEnabled = false
            downloadButton.isEnabled = false
            fetchJSON(animation, DOWNLOAD)
        }

        check_update.setOnClickListener {
            Utils().startRefreshAnimation(context, animation)
            fetchJSON(animation, CHECK_FOR_UPDATES)
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
            Utils().snackBar(context, "No connection")
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

                    runOnUiThread {
                        if (preferenceManager.getBoolean(getString(R.string.key_miui_check), false)) {
                            ChangelogTask().execute(nanoData!!.MIUI[0].changelog_url, changelogView, this@MainActivity)
                            latest_version_textView.text = "Nano " + nanoData!!.MIUI[0].release_number + " • " + Utils().formatDate(nanoData!!.MIUI[0].date)
                            update_fileName.text = "Nano Kernel ${nanoData!!.MIUI[0].release_number}"
                            update_fileSize.text = nanoData!!.MIUI[0].size
                            update_timestamp.text = Utils().formatDate(nanoData!!.MIUI[0].date)
                            packageInfoTextView.text = nanoData!!.MIUI[0].filename
                            sizeInfoTextView.text = nanoData!!.MIUI[0].size
                            md5InfoTextView.text = nanoData!!.MIUI[0].md5
                        } else {
                            ChangelogTask().execute(nanoData!!.AOSP[0].changelog_url, changelogView, this@MainActivity)
                            latest_version_textView.text = "Nano " + nanoData!!.AOSP[0].release_number + " • " + Utils().formatDate(nanoData!!.AOSP[0].date)
                            update_fileName.text = "Nano Kernel ${nanoData!!.AOSP[0].release_number}"
                            update_fileSize.text = nanoData!!.AOSP[0].size
                            update_timestamp.text = Utils().formatDate(nanoData!!.AOSP[0].date)
                            packageInfoTextView.text = nanoData!!.AOSP[0].filename
                            sizeInfoTextView.text = nanoData!!.AOSP[0].size
                            md5InfoTextView.text = nanoData!!.AOSP[0].md5
                        }
                    }

                    when (currentTask) {
                        DOWNLOAD -> {
                            runOnUiThread {
                                downloadId = DownloadUtils().downloadPackage(context, downloadManager, nanoData, "MainActivity")
                            }
                        }
                        CHECK_FOR_UPDATES -> {
                            runOnUiThread {
                                checkForUpdates(nanoData)
                            }
                        }
                    }
                }
            })
        }
    }

    fun checkForUpdates(nanoData: Nano?) {
        if (Utils().isNetworkAvailable(context))
            Utils().isUpdateAvailable(context, nanoData, buildDate, animation)
        else
            Utils().snackBar(context, "No connection")
    }


    override fun onStart() {
        nav_view.setCheckedItem(R.id.nav_home)
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.edit().putBoolean(getString(R.string.key_is_root_checked), false).apply()
        unregisterReceiver(onDownloadComplete)
    }
}
