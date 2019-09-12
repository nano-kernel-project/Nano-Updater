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
 * Last modified 12/9/19 1:42 PM.
 */

package com.codebot.axel.kernel.updater.util

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.codebot.axel.kernel.updater.R
import com.codebot.axel.kernel.updater.model.Nano
import kotlinx.android.synthetic.main.layout_flash_expanded.*
import kotlinx.android.synthetic.main.layout_update_card.*
import kotlinx.android.synthetic.main.package_list_item.*
import kotlinx.android.synthetic.main.update_info_layout.*
import java.io.File

/**
 *  This class holds all the utilities required for downloading kernel packages.
 */
class DownloadUtils {

    private var downloadId: Long = 0

    /**
     *  Helper method to check if the download is successful.
     *  @param context Reference from the base Activity
     *  @param id The unique value assigned to a downloading task
     *  @param downloadManager Reference from the base Activity
     *  @return Returns status message (success, failed, pending, running, paused).
     */
    fun isDownloadSuccessful(context: Context, id: Long, downloadManager: DownloadManager): Array<String> {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))

        if (cursor == null)
            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
        else {
            if (cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_PAUSED -> {
                        return arrayOf("status_paused", "true")
                        // Toast.makeText(context, "Download paused", Toast.LENGTH_SHORT).show()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        return arrayOf("failed")
                        // Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        // Toast.makeText(context, "Download successful", Toast.LENGTH_SHORT).show()
                        return arrayOf("status_successful", "true")
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        // Toast.makeText(context, "Download running", Toast.LENGTH_SHORT).show()
                        return arrayOf("status_running", "true")
                    }
                    DownloadManager.STATUS_PENDING -> {
                        return arrayOf("status_pending", "true")
                        // Toast.makeText(context, "Download pending", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        /**
         * When code execution  is here, it clearly states that the download is failed.
         * Let's hide those views that aren't needed.
         * */
        (context as Activity).runOnUiThread {
            context.updates_compact.visibility = View.VISIBLE
            context.update_info_expanded.visibility = View.GONE
            context.update_progressBar.visibility = View.GONE
            context.progress_info.visibility = View.GONE
            context.downloadButton.visibility = View.VISIBLE
            context.update_fileDownload.visibility = View.VISIBLE
        }
        return arrayOf("failed")
    }

    /**
     * Helper method to download a package using DownloadManager class.
     * @param context Reference from base Activity.
     * @param downloadManager Reference from base Activity.
     * @param nanoData Holds the information about the package to be downloaded.
     * @return Returns the unique identifier for the enqueued download.
     */
    fun downloadPackage(context: Context, downloadManager: DownloadManager, nanoData: Nano?): Long {
        val downloadUrl: String = if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_miui_check), false))
            nanoData!!.MIUI[0].url
        else
            nanoData!!.AOSP[0].url
        val downloadFileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1, downloadUrl.length)
        val downloadPath = Environment.getExternalStorageDirectory().toString() + "/kernel.updater/builds/"

        if (!File(downloadPath).exists())
            File(downloadPath).mkdirs()
        val installPackage = File("$downloadPath$downloadFileName")
        if (installPackage.exists()) {
            (context as Activity).update_fileDownload.isEnabled = true
            context.downloadButton.isEnabled = true
            Utils().setViewVisibilityAndListeners(context, installPackage, View.GONE)
        } else {
            (context as Activity).update_progressBar.visibility = View.VISIBLE
            context.progress_info.visibility = View.VISIBLE
            context.update_fileDownload.visibility = View.GONE

            val downloadRequest = DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle(downloadFileName)
                    .setDestinationInExternalPublicDir("/kernel.updater/builds/", downloadFileName)

            downloadId = downloadManager.enqueue(downloadRequest)
            context.flasherImage.setOnClickListener {
                Utils().performManualFlash(context, installPackage)
            }
            context.expanded_flasherImage.setOnClickListener {
                Utils().performManualFlash(context, installPackage)
            }
            context.autoFlasherImage.setOnClickListener {
                Utils().performAutoFlash(context, installPackage)
            }
            context.expanded_autoFlasherImage.setOnClickListener {
                Utils().performAutoFlash(context, installPackage)
            }
            Toast.makeText(context, "Downloading $downloadFileName", Toast.LENGTH_SHORT).show()
            context.downloadButton.setTextColor(ContextCompat.getColor(context, R.color.strokeColor))
            updateProgress(context, downloadId, downloadManager)
            return downloadId
        }
        return -1
    }

    /**
     * Helper method to update the progress while downloading a package.
     * @param context Reference from base Activity.
     * @param downloadId Unique identifier of enqueued download.
     * @param downloadManager Reference from base Activity.
     */
    private fun updateProgress(context: Context, downloadId: Long, downloadManager: DownloadManager) {
        Thread(Runnable {
            var isDownloading = true
            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val noOfBytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val sizeOfPackage = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        isDownloading = false
                        (context as Activity).runOnUiThread {
                            context.update_progressBar.visibility = View.GONE
                            context.progress_info.visibility = View.GONE
                            context.downloadButton.visibility = View.VISIBLE
                            context.update_fileDownload.visibility = View.VISIBLE
                        }
                    }

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_FAILED)
                        Toast.makeText(context, "Download unsuccessful", Toast.LENGTH_SHORT).show()
                    val downloadProgress = (noOfBytesDownloadedSoFar.toFloat() / sizeOfPackage.toFloat()) * 100
                    (context as Activity).runOnUiThread {
                        context.update_progressBar.progress = downloadProgress.toInt()
                        context.progress_info.progress = downloadProgress.toInt()
                    }
                }
                cursor.close()
            }
        }).start()
    }

    fun getDownloadedFileName(id: Long, context: Context): String {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
        }
        return ""
    }
}