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
 * Last modified 26/7/19 10:20 PM.
 */

package com.codebot.axel.kernel.updater

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Animatable2
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.kernel.updater.adapter.FileAdapter
import com.codebot.axel.kernel.updater.model.Package
import com.codebot.axel.kernel.updater.util.Constants.Companion.FILE_CHOOSER_INT
import com.codebot.axel.kernel.updater.util.Constants.Companion.STORAGE_PERMISSION_CODE
import com.codebot.axel.kernel.updater.util.FlashKernel
import com.codebot.axel.kernel.updater.util.Utils
import kotlinx.android.synthetic.main.activity_flash.*
import kotlinx.android.synthetic.main.layout_package_info.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FlashActivity : AppCompatActivity() {

    private var packageList = ArrayList<Package>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash)

        selectFile.drawable.mutate().setTint(Color.WHITE)

        selectFile.setOnClickListener {
            if (Utils().isStoragePermissionGranted(this@FlashActivity, STORAGE_PERMISSION_CODE))
                FlashKernel().launchFileChooser(this@FlashActivity, FILE_CHOOSER_INT)
        }
        if (Utils().isStoragePermissionGranted(this@FlashActivity, STORAGE_PERMISSION_CODE)) {
            initialize()
        }
    }

    private fun initialize() {
        val nanoDirectory = File("${Environment.getExternalStorageDirectory().path}/kernel.updater/builds/")
        if (nanoDirectory.exists()) {
            val files = nanoDirectory.listFiles()
            noOfPackagesOnStorage.text = "Packages ready to be flashed: ${files.size}"

            for (file in files) {
                var size = (file.length() / 1000000.00).toString()
                size = size.substring(0, 5)
                val date = Date(file.lastModified())
                val dateFormat = SimpleDateFormat("MMM dd, yyyy")
                packageList.add(Package(file.name, size, dateFormat.format(date), file.absolutePath))
            }
        }

        if (packageList.size == 0) {
            fileRecyclerView.visibility = View.GONE
            subtitle1.visibility = View.GONE
            noOfPackagesOnStorage.visibility = View.GONE
            empty_view.visibility = View.VISIBLE
            (empty_view_image.drawable as Animatable2).start()
        } else {
            fileRecyclerView.visibility = View.VISIBLE
            empty_view.visibility = View.GONE
            fileRecyclerView.apply {
                adapter = FileAdapter(packageList, this@FlashActivity)
                layoutManager = LinearLayoutManager(this@FlashActivity, RecyclerView.VERTICAL, false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_INT) {
            if (resultCode == Activity.RESULT_OK) {
                val dataUri = data!!.data
                val pathContainsColon = getPath(dataUri).contains(":")
                if (!pathContainsColon) {
                    val absolutePath = getPath(dataUri)
                    if (isZipFile(absolutePath))
                        FlashKernelTask(this@FlashActivity).execute(this@FlashActivity, absolutePath)
                    else
                        Utils().snackBar(this@FlashActivity, "Selected file is not a zip")
                } else {
                    val filePath = getPath(dataUri).split(":")
                    val isRaw = filePath[0] == "/document/raw"
                    val absolutePath: String
                    if (!isRaw) {
                        val zipPath = filePath[1]
                        if (isZipFile(zipPath)) {
                            absolutePath = "${Environment.getExternalStorageDirectory().path}/$zipPath"
                            FlashKernelTask(this@FlashActivity).execute(this@FlashActivity, absolutePath)
                        } else
                            Utils().snackBar(this@FlashActivity, "Selected file is not a zip")
                    } else {
                        absolutePath = filePath[1]
                        if (isZipFile(absolutePath)) {
                            FlashKernelTask(this@FlashActivity).execute(this@FlashActivity, absolutePath)
                        } else
                            Utils().snackBar(this@FlashActivity, "Selected file is not a zip")
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // User granted permission
                    initialize()
                } else {
                    fileRecyclerView.visibility = View.GONE
                    subtitle1.visibility = View.GONE
                    noOfPackagesOnStorage.visibility = View.GONE
                    empty_view.visibility = View.VISIBLE
                    (empty_view_image.drawable as Animatable2).start()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getPath(uri: Uri): String {
        val path: String?
        val projection: Array<String> = Array(MediaStore.Files.FileColumns.DATA.length) { MediaStore.Files.FileColumns.DATA }
        val cursor = contentResolver.query(uri, projection, null, null, null)

        if (cursor == null) {
            path = uri.path
        } else {
            cursor.moveToFirst()
            val column_index = cursor.getColumnIndexOrThrow(projection[0])
            path = cursor.getString(column_index)
            cursor.close()
        }
        return if (path == null || path.isEmpty())
            uri.path!!
        else
            path
    }

    private fun isZipFile(absolutePath: String): Boolean {
        return absolutePath.substring(absolutePath.lastIndexOf('.') + 1, absolutePath.length) == "zip"
    }
}
