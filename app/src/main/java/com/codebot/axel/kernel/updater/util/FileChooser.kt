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
 * Last modified 11/9/19 7:35 PM.
 */

package com.codebot.axel.kernel.updater.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.core.content.ContextCompat
import com.codebot.axel.kernel.updater.R
import com.codebot.axel.kernel.updater.model.FileModel
import java.io.File
import java.util.*

class FileChooser(private val activity: Activity) {
    private val list: ListView
    private val dialog: Dialog
    private var currentPath: File? = null

    // filter on file extension
    private var extension: String? = ".zip"

    private var fileListener: FileSelectedListener? = null

    fun setExtension(extension: String?) {
        this.extension = extension?.toLowerCase()
    }

    // file selection event handling
    interface FileSelectedListener {
        fun fileSelected(file: File)
    }

    fun setFileListener(fileListener: FileSelectedListener): FileChooser {
        this.fileListener = fileListener
        return this
    }

    init {
        dialog = Dialog(activity, R.style.ChooserTheme)
        list = ListView(activity)
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, which, _ ->
            val fileModel = list.getItemAtPosition(which) as FileModel
            val fileChosen = fileModel.fileList
            val chosenFile = getChosenFile(fileChosen)
            if (chosenFile.isDirectory) {
                try {
                    refresh(chosenFile)
                } catch (e: Exception) {
                    refresh(Environment.getExternalStorageDirectory())
                    e.printStackTrace()
                }

            } else {
                if (fileListener != null) {
                    fileListener!!.fileSelected(chosenFile)
                }
                dialog.dismiss()
            }
        }
        dialog.setContentView(list)
        dialog.window!!.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        refresh(Environment.getExternalStorageDirectory())
    }

    fun showDialog() {
        dialog.show()
    }

    /**
     * Sort, filter and display the files for the given path.
     */
    private fun refresh(path: File) {
        this.currentPath = path
        if (path.exists()) {
            val dirs = path.listFiles { file -> file.isDirectory && file.canRead() }
            val files = path.listFiles { file ->
                if (!file.isDirectory) {
                    if (!file.canRead()) {
                        false
                    } else if (extension == null) {
                        true
                    } else {
                        file.name.toLowerCase().endsWith(extension!!)
                    }
                } else {
                    false
                }
            }

            // convert to an array
            val fileModel = ArrayList<FileModel>()
            val fileList: String
            val drawable: Drawable? = null
            if (path.parentFile != null) {
                fileList = PARENT_DIR
                fileModel.add(FileModel(fileList, drawable))
            }
            Arrays.sort(dirs)
            Arrays.sort(files)
            for (dir in dirs) {
                fileModel.add(FileModel(dir.name, ContextCompat.getDrawable(activity, R.drawable.ic_folder)!!))
            }
            for (file in files) {
                fileModel.add(FileModel(file.name, ContextCompat.getDrawable(activity, R.drawable.ic_file)!!))
            }

            // refresh the user interface
            dialog.setTitle(currentPath!!.path)
            list.adapter = CustomAdapter(activity, fileModel)
            list.divider = null
        }
    }


    /**
     * Convert a relative filename into an actual File object.
     */
    private fun getChosenFile(fileChosen: String): File {
        return if (fileChosen == PARENT_DIR) {
            currentPath!!.parentFile
        } else {
            File(currentPath, fileChosen)
        }
    }

    inner class CustomAdapter(context: Context, fileModel: ArrayList<FileModel>) : ArrayAdapter<FileModel>(context, 0, fileModel) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val fileModel = getItem(position)
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.layout_list_item, parent, false)

            val text = convertView!!.findViewById<TextView>(R.id.text)
            val image = convertView.findViewById<ImageView>(R.id.image)

            text.text = Objects.requireNonNull<FileModel>(fileModel).fileList
            image.setImageDrawable(fileModel!!.drawable)
            return convertView
        }
    }

    companion object {
        private const val PARENT_DIR = ".."
    }
}