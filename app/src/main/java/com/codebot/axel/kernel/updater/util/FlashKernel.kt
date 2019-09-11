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
 * Last modified 11/9/19 7:51 PM.
 */

package com.codebot.axel.kernel.updater.util

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Environment
import android.util.Log
import com.codebot.axel.kernel.updater.FlashKernelTask
import com.codebot.axel.kernel.updater.R
import java.io.*


/**
 *  This class holds all the utilities required for flashing the kernel.
 */
class FlashKernel {

    /**
     *  Helper method to auto flash kernel package. Reboots the device to recovery mode to flash kernel.
     *  @param packageName The package name to be flashed
     */
    fun flashPackage(context: Context, absolutePath: String) {
        val installPackage = File(absolutePath)
        val packageName = absolutePath.substring(absolutePath.lastIndexOf('/') + 1, absolutePath.length)
        AlertDialog.Builder(context, R.style.DialogTheme)
                .setTitle("Flash Kernel")
                .setMessage("Your device will reboot to recovery to flash $packageName. Would you like to reboot now?")
                .setPositiveButton("Reboot") { dialog, which ->
                    try {
                        val p = Runtime.getRuntime().exec("su")
                        val dos = DataOutputStream(p.outputStream)
                        dos.writeBytes("echo 'boot-recovery ' > ${Constants.CACHE_RECOVERY_CMD}\n")
                        dos.writeBytes("echo '--update_package=$absolutePath' > ${Constants.CACHE_RECOVERY_CMD}\n")
                        dos.writeBytes("${Constants.SHUTDOWN_BROADCAST}\n")
                        dos.writeBytes("${Constants.SYNC}\n")
                        dos.writeBytes("${Constants.REBOOT_RECOVERY_CMD}\n")
                        dos.writeBytes("exit\n")
                        dos.flush()
                        dos.close()
                        p.waitFor()
                        installPackage.delete()
                    } catch (e: Exception) {
                        Log.d("flashPackage()", "$e")
                    }
                }
                .setNegativeButton("Later") { dialog, which -> dialog!!.dismiss() }
                .show()
    }

    /**
     *  Helper method to open a file chooser for manual flashing.
     *  @param context Reference from the base Activity.
     */
    fun launchFileChooser(context: Context) {
        FileChooser(context as Activity).setFileListener(object : FileChooser.FileSelectedListener {
            override fun fileSelected(file: File) {
                if (!isAnyKernelZip(file.absolutePath)) {
                    Utils().snackBar(context, "Choose a valid AnyKernel zip")
                    return
                }
                AlertDialog.Builder(context, R.style.DialogTheme)
                        .setTitle("Flash Kernel")
                        .setMessage("You're about to flash ${file.name}. Your device will reboot after the process of flashing. Would you like to flash now?")
                        .setPositiveButton("Flash", object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                FlashKernelTask(context).execute(context, file.absolutePath)
                            }
                        })
                        .setNegativeButton("Later", object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                dialog!!.dismiss()
                            }
                        })
                        .show()
            }
        }).showDialog()
    }

    /**
     * Helper method for manual flashing a kernel. Upon invokingm, it flashes kernel within the app and reboots the device.
     * @param context Reference from the base Activity.
     * @param absolutePath The absolute path of the package to be flashed.
     * @param progressDialog Used to display the Flashing Kernel message.
     */
    fun unzipAndFlash(context: Context?, absolutePath: String, progressDialog: ProgressDialog) {
        var isFlashSuccessful = false
        val updateBinaryPath = "/META-INF/com/google/android/update-binary"
        val path = absolutePath.substring(0, absolutePath.lastIndexOf("/") + 1)
        var modifiedPath = ""
        var fileName = absolutePath.substring(absolutePath.lastIndexOf("/") + 1, absolutePath.length)
        val installPackagePath = File("${Environment.getExternalStorageDirectory().path}/kernel.updater/install_package/")
        if (!isAnyKernelZip(absolutePath)) {
            (context as Activity).runOnUiThread {
                Utils().snackBar(context, "Not a valid AnyKernel zip")
            }
            return
        }
        try {
            val process = Runtime.getRuntime().exec("su")
            val dos = DataOutputStream(process.outputStream)
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            val unwantedCharsPresent = !fileName.matches("[^()\\s\\[\\]]+".toRegex())
            // Remove any white spaces, brackets in the file name
            if (unwantedCharsPresent) {
                fileName = fileName.replace("[()\\s]".toRegex()) { "" }
                modifiedPath = path + fileName

                // Rename file with modified name
                val origFile = File(absolutePath)
                val renamedFile = File(modifiedPath)
                origFile.renameTo(renamedFile)
            }
            dos.writeBytes("mount -o rw,remount /\n")
            if (!checkUnZipUtility()) {
                Log.e("FlashActivity", "unZip")
                copyAssets(context!!)
                dos.writeBytes("mount -o rw,remount /system\n")
                dos.writeBytes("mv ${Environment.getExternalStorageDirectory().path}/kernel.updater/tmp/unzip /system/bin/unzip\n")
                dos.writeBytes("chmod 755 /system/bin/unzip\n")
                dos.writeBytes("rm -rf ${Environment.getExternalStorageDirectory().path}/kernel.updater/tmp/\n")
                dos.writeBytes("mount -o ro,remount /system\n")
            }
            val tempPath = "${Environment.getExternalStorageDirectory().path}/kernel.updater/tmp"
            dos.writeBytes("mkdir $tempPath/\n")

            if (!unwantedCharsPresent) {
                dos.writeBytes("unzip $absolutePath -d $tempPath/\n")
                dos.writeBytes("sh $tempPath$updateBinaryPath dummy 1 $absolutePath\n")
            } else {
                dos.writeBytes("unzip $modifiedPath -d $tempPath/\n")
                dos.writeBytes("sh $tempPath$updateBinaryPath dummy 1 $modifiedPath\n")
            }
            dos.writeBytes("rm -rf $tempPath/\n")
            dos.writeBytes("mount -o ro,remount /\n")
            dos.writeBytes("exit\n")
            dos.flush()
            process.waitFor()
            var line = bufferedReader.readLine()
            val builder = StringBuilder()
            while (line != null) {
                if (line.contains("Unsupported device")) {
                    (context as Activity).runOnUiThread {
                        Utils().snackBar(context, "Unsupported device. Flash unsuccessful")
                    }
                }
                if (line.contains("Done!")) {
                    (context as Activity).runOnUiThread {
                        isFlashSuccessful = true
                        Utils().snackBar(context, "Manual flash successful. Rebooting your device!")
                    }
                }
                builder.append(line + "\n")
                line = bufferedReader.readLine()
            }

            if (!unwantedCharsPresent)
                Utils().writeLogToFile(builder.toString(), absolutePath)

            if (unwantedCharsPresent) {
                val origFile = File(absolutePath)
                val renamedFile = File(modifiedPath)
                renamedFile.renameTo(origFile)
            }

            installPackagePath.deleteRecursively()
            if (isFlashSuccessful)
                Utils().rebootDevice()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     *  Helper method to set up environment for Manual Flasher.
     *  @param context Reference from base Activity
     */
    private fun copyAssets(context: Context) {
        val file = File(Environment.getExternalStorageDirectory().path + "/kernel.updater/tmp/")
        if (!file.exists())
            file.mkdirs()
        val fileOutputStream = FileOutputStream("${Environment.getExternalStorageDirectory().path}/kernel.updater/tmp/unzip")
        val buffer = ByteArray(1024)
        var length: Int
        val inputStream = context.assets.open("unzip")
        length = inputStream.read(buffer)
        while (length > 0) {
            fileOutputStream.write(buffer, 0, length)
            length = inputStream.read(buffer)
        }
    }

    /**
     *  Helper method to check if unzip binary is present in the device.
     *  @return Returns true if unzip is present in /system/bin. Otherwise returns false
     */
    private fun checkUnZipUtility(): Boolean {
        val process = Runtime.getRuntime().exec("su")
        val dos = DataOutputStream(process.outputStream)
        dos.writeBytes("ls /system/bin/\n")
        dos.writeBytes("exit\n")
        dos.flush()
        process.waitFor()
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
        var line = bufferedReader.readLine()
        while (line != null) {
            if (line == "unzip") {
                return true
            }
            line = bufferedReader.readLine()
        }
        return false
    }

    /**
     * Helper method to check if the selected zip is a valid AnyKernel zip.
     * @param absolutePath is the path of file
     */
    private fun isAnyKernelZip(absolutePath: String): Boolean {
        val installPackagePath = File("${Environment.getExternalStorageDirectory().path}/kernel.updater/install_package/")
        ZipManager.unzip(absolutePath, "${installPackagePath.absolutePath}/")
        val files = installPackagePath.listFiles()
        for (file in files) {
            if (file.name == "anykernel.sh") {
                return true
            }
        }
        installPackagePath.deleteRecursively()
        return false
    }
}
