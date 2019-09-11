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
 * Last modified 11/9/19 8:37 PM.
 */

package com.codebot.axel.kernel.updater

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Animatable2
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codebot.axel.kernel.updater.adapter.FileAdapter
import com.codebot.axel.kernel.updater.model.Package
import com.codebot.axel.kernel.updater.util.Constants.Companion.STORAGE_PERMISSION_CODE
import com.codebot.axel.kernel.updater.util.FlashKernel
import com.codebot.axel.kernel.updater.util.Utils
import kotlinx.android.synthetic.main.activity_flash.*
import kotlinx.android.synthetic.main.layout_package_info.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


class FlashActivity : AppCompatActivity() {

    private var packageList = ArrayList<Package>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash)

        selectFile.drawable.mutate().setTint(Color.WHITE)

        fileRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0)
                    selectFile.hide()
                else if (dy < 0)
                    selectFile.show()
            }
        })
        selectFile.setOnClickListener {
            if (Utils().isStoragePermissionGranted(this@FlashActivity, STORAGE_PERMISSION_CODE))
                FlashKernel().launchFileChooser(this@FlashActivity)
        }
        if (Utils().isStoragePermissionGranted(this@FlashActivity, STORAGE_PERMISSION_CODE)) {
            Thread {
                initialize()
            }.start()
        }
    }


    private fun initialize() {
        val nanoDirectory = File("${Environment.getExternalStorageDirectory().path}/kernel.updater/builds/")
        if (nanoDirectory.exists()) {
            val files = nanoDirectory.listFiles()
            noOfPackagesOnStorage.text = "Packages ready to be flashed: ${files.size}"

            for (file in files) {
                var size = (file.length() / 1000000.00).toString()
                if (size.length - size.lastIndexOf('.') <= 2)
                    size += "00"
                Log.d("Size", "length - " + size.length + " index of dot - " + size.lastIndexOf('.') + " and it's size - " + size)
                size = size.substring(0, size.lastIndexOf('.') + 3)
                val date = Date(extractDateFromFileName(file.name))
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
            runOnUiThread {
                fileRecyclerView.apply {
                    adapter = FileAdapter(packageList, this@FlashActivity)
                    layoutManager = LinearLayoutManager(this@FlashActivity, RecyclerView.VERTICAL, false)
                }
            }
        }
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

    private fun extractDateFromFileName(fileName: String): Long {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val pattern1 = Pattern.compile(".*(\\d{8})-(\\d{8}).*")
        val matcher1 = pattern1.matcher(fileName)
        if (matcher1.find())
            return dateFormat.parse(matcher1.group(1)).time
        else {
            val pattern2 = Pattern.compile(".*(\\d{8}).*")
            val matcher2 = pattern2.matcher(fileName)
            if (matcher2.find())
                return dateFormat.parse(matcher2.group(1)).time
        }
        return 0L
    }
}
