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
 * Last modified 4/9/19 9:32 PM.
 */

package com.codebot.axel.kernel.updater.util

import android.util.Log
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipManager {

    @Throws(IOException::class)
    fun unzip(zipFile: String, location: String) {
        try {
            val f = File(location)
            if (!f.isDirectory) {
                f.mkdirs()
            }
            val zin = ZipInputStream(FileInputStream(zipFile))
            try {
                var ze: ZipEntry? = zin.nextEntry
                while (ze != null) {
                    val path = location + File.separator + ze!!.name

                    if (ze.isDirectory) {
                        val unzipFile = File(path)
                        if (!unzipFile.isDirectory) {
                            unzipFile.mkdirs()
                        }
                    } else {
                        val fout = FileOutputStream(path, false)
                        val bout = BufferedOutputStream(fout)

                        try {
                            val b = ByteArray(1024)
                            var n = zin.read(b, 0, 1024)
                            while (n >= 0) {
                                bout.write(b, 0, n)
                                n = zin.read(b, 0, 1024)
                            }
                            zin.closeEntry()
                            bout.close()
                        } finally {
                            fout.close()
                        }
                    }
                    ze = zin.nextEntry
                }
            } finally {
                zin.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Decompress", "Unzip exception", e)
        }

    }
}