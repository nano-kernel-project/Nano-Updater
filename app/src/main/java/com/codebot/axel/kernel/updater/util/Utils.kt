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
 * Last modified 12/9/19 12:23 PM.
 */

package com.codebot.axel.kernel.updater.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codebot.axel.kernel.updater.*
import com.codebot.axel.kernel.updater.model.Nano
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_changelog.*
import kotlinx.android.synthetic.main.activity_feedback.*
import kotlinx.android.synthetic.main.activity_flash.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.layout_flash_expanded.*
import kotlinx.android.synthetic.main.layout_update_card.*
import kotlinx.android.synthetic.main.package_list_item.*
import kotlinx.android.synthetic.main.update_info_layout.*
import java.io.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Utils {

    /**
     *  This method is used to check the network connectivity.
     *  @param context Receives the context from calling Activity
     *  @return Returns true if network is available otherwise false
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    /**
     *  This method is used to start the refresh animation of FAB in the MainActivity
     *  @param activity Reference from the base activity
     *  @param animation Reference of RotateAnimation from base activity
     */
    fun startRefreshAnimation(activity: Activity, animation: RotateAnimation) {
        animation.interpolator = LinearInterpolator()
        animation.repeatCount = Animation.INFINITE
        animation.duration = 1000
        activity.check_update.startAnimation(animation)
    }

    /**
     *  This method stops the rotate animation of FAB in MainActivity
     *  @param animation Reference of RotateAnimation from base activity
     */
    fun stopRefreshAnimation(animation: RotateAnimation) {
        animation.cancel()
    }

    /**
     *  This method checks if a kernel update is available
     *  @param context Receives the context from calling Activity
     *  @param nanoData Holds the data in JSON format
     *  @param buildDate Build date of the installed version (yyyyMMdd)
     *  @param animation Reference of RotateAnimation from base activity
     */
    fun isUpdateAvailable(context: Context, nanoData: Nano?, buildDate: String, animation: RotateAnimation) {
        val nanoPackage = if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_miui_check), false))
            nanoData!!.MIUI
        else
            nanoData!!.AOSP
        val isKernelInstalled = Constants.KERNEL_VERSION.contains("Nano")
        when {
            buildDate == "" && !isKernelInstalled -> {
                (context as Activity).update_notify_textView.text = "Unsupported kernel detected"
                context.update_notify_textView.setTextColor(ContextCompat.getColor(context, R.color.accentTitleColor))
                context.update_notify_textView.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accentTitleColor))
                context.update_notify_textView.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.ic_info), null, null, null)
            }
            getBuildTimeFromDate(nanoPackage[0].date) > getBuildTimeFromDate(buildDate) -> {
                (context as Activity).update_notify_textView.text = "An update is available!"
                context.update_notify_textView.setTextColor(ContextCompat.getColor(context, R.color.accentTitleColor))
                context.update_notify_textView.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accentTitleColor))
                context.update_notify_textView.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.ic_info), null, null, null)
                if (isStoragePermissionGranted(context, Constants.STORAGE_PERMISSION_CODE)) {
                    val packagesDir = File("${Environment.getExternalStorageDirectory().path}/kernel.updater/builds/")
                    if (packagesDir.exists()) {
                        try {
                            val files = packagesDir.listFiles()
                            if (files.isNotEmpty()) {
                                for (file in files) {
                                    Log.d("Utils", "Packages found in builds dir")
                                    if (file.name == nanoPackage[0].filename) {
                                        Log.d("Utils", "Update package already exist in builds dir")
                                        setViewVisibilityAndListeners(context, file, View.GONE)
                                        break
                                    } else
                                        setVisibilityForViews(context)
                                }
                            } else {
                                setVisibilityForViews(context)
                            }
                        } catch (e: Exception) {
                            Log.e("isUpdateAvailable", "$e")
                        }
                    }
                } else {
                    if (context.update_info_expanded.visibility == View.VISIBLE)
                        context.update_info_expanded.visibility = View.GONE
                    context.updates_compact.visibility = View.VISIBLE
                }
            }
            else -> {
                (context as Activity).update_notify_textView.text = "You're up to date"
                context.update_notify_textView.setTextColor(ContextCompat.getColor(context, R.color.green))
                context.update_notify_textView.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green))
                context.update_notify_textView.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.ic_done), null, null, null)
            }
        }
        stopRefreshAnimation(animation)
    }

    /**
     *  This method will get time in milliseconds from a date string of format yyyyMMdd
     *  @param date The date in the format yyyyMMdd
     *  @return Returns time in milliseconds (long)
     */
    private fun getBuildTimeFromDate(date: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
        var time = 0L
        try {
            val currentDate = simpleDateFormat.parse(date) as Date
            time = currentDate.time
        } catch (e: Exception) {
            Log.e("getBuildTimeFromDate", "$e")
        }
        return time
    }

    /**
     *  Invokes auto flashing method
     *  @param context Receives the context from calling Activity
     *  @param installPackage The package file to be flashed
     */
    fun performAutoFlash(context: Context, installPackage: File) {
        FlashKernel().flashPackage(context, installPackage.name)
    }

    /**
     *  Invokes manual flashing method
     *  @param context Receives the context from calling Activity
     *  @param installPackage The package file to be flashed
     */
    fun performManualFlash(context: Context, installPackage: File) {
        FlashKernelTask(context).execute(context, installPackage.absolutePath)
    }

    /**
     *  This method formats the date
     *  @param date Date in string format (yyyyMMdd)
     *  @return Returns string in the format MMM dd, yyyy. For instance Jul 22, 2019.
     */
    fun formatDate(date: String): String {
        return if (date.length == 8) {
            val dateFormatter = SimpleDateFormat("yyyyMMdd")
            val new_date = dateFormatter.parse(date) as Date
            val new_date_formatter = SimpleDateFormat("MMM dd, yyyy")
            new_date_formatter.format(new_date)
        } else if (date == "")
            return ""
        else {
            val date = Date(date.toLong())
            val dateFormat = SimpleDateFormat("MMM dd, yyyy")
            dateFormat.format(date)
        }
    }

    /**
     *  This method hides/shows views based on certain conditions
     *  @param context Receives the context from calling Activity
     *  @param installPackage The file to be flashed (We retrieve info of this package in this method. For instance, size, name)
     *  @param compactLayoutVisibility The visibility value (View.VISIBLE or View.GONE)
     */
    fun setViewVisibilityAndListeners(context: Context, installPackage: File, compactLayoutVisibility: Int) {
        val flashLayoutVisibility: Int = if (compactLayoutVisibility == View.GONE)
            View.VISIBLE
        else
            View.GONE

        // Check if listeners were already set. We don't need to set the listeners more than once
        if (((context as Activity).packageInfoCompact.visibility == flashLayoutVisibility) || (context.packageInfoExpanded.visibility == flashLayoutVisibility))
            return

        context.fileName.text = installPackage.name
        context.fileDate.text = formatDate(installPackage.lastModified().toString())
        context.fileSize.text = "${installPackage.length() / 1000000} MB"

        context.expanded_sizeInfoTextView.text = "${installPackage.length() / 1000000} MB"
        context.expanded_packageInfoTextView.text = installPackage.name
        context.expanded_dateInfoTextView.text = formatDate(installPackage.lastModified().toString())

        context.updates_compact.visibility = compactLayoutVisibility
        context.packageInfoCompact.visibility = flashLayoutVisibility

        context.packageInfoCompact.setOnClickListener {
            context.packageInfoCompact.visibility = compactLayoutVisibility
            context.packageInfoExpanded.visibility = flashLayoutVisibility
        }
        context.packageInfoExpanded.setOnClickListener {
            context.packageInfoExpanded.visibility = compactLayoutVisibility
            context.packageInfoCompact.visibility = flashLayoutVisibility
        }
        context.flasherImage.setOnClickListener {
            performManualFlash(context, installPackage)
        }
        context.expanded_flasherImage.setOnClickListener {
            performManualFlash(context, installPackage)
        }
        context.expanded_autoFlasherImage.setOnClickListener {
            performAutoFlash(context, installPackage)
        }
        context.autoFlasherImage.setOnClickListener {
            performAutoFlash(context, installPackage)
        }
    }

    /**
     *  Helper method for showing SnackBar whenever needed
     *  @param context Receives the context from calling Activity
     *  @param message The message to be shown in the snackbar
     */
    fun snackBar(context: Context, message: String) {
        val snackBar: Snackbar
        when (context) {
            is MainActivity -> {
                snackBar = Snackbar.make((context as Activity).check_update, message, Snackbar.LENGTH_LONG)
                snackBar.anchorView = context.check_update
            }
            is FeedbackActivity -> snackBar = Snackbar.make((context as Activity).feedback_root_layout, message, Snackbar.LENGTH_LONG)
            is ChangelogActivity -> snackBar = Snackbar.make((context as Activity).changelog_root_layout, message, Snackbar.LENGTH_LONG)
            else -> snackBar = Snackbar.make((context as Activity).flasherLayout, message, Snackbar.LENGTH_LONG)
        }
        snackBar.setBackgroundTint(ContextCompat.getColor(context, R.color.navBackground))
        snackBar.setTextColor(ContextCompat.getColor(context, R.color.navIconTint))
        snackBar.show()
    }

    /**
     *  Helper method to check the currently installed kernel version on users device.
     *  It retrieves the installed version from /system/build.prop file using propKey
     *  @param propKey The key used to retrieve the kernel version
     *  @return Returns version in the format vX.Y if supported otherwise an empty string
     */
    fun checkInstalledVersion(propKey: String): String {
        var propValue = ""
        try {
            val process = ProcessBuilder("/system/bin/getprop", propKey).redirectErrorStream(true).start()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line = bufferedReader.readLine()
            while (line != null) {
                propValue = line
                line = bufferedReader.readLine()
            }
            process.destroy()
        } catch (e: Exception) {
            Log.e("checkInstalledVersion", "$e")
        }
        return propValue
    }

    /**
     *  Helper method to write logs to a file when manual flashing is under progress.
     *  Logs are stored under /sdcard/kernel.updater/logs.
     *  @param log The text to be written in the file
     *  @param absolutePath The absolute path of the file that is currently being flashed
     */
    fun writeLogToFile(log: String, absolutePath: String) {
        try {
            val currentTimeAndDate = DateFormat.getDateTimeInstance().format(Date())
            val file = absolutePath.substring(absolutePath.lastIndexOf('/') + 1, absolutePath.length)
            val logName = "$currentTimeAndDate-$file.log"
            val logPath = "${Environment.getExternalStorageDirectory().path}/kernel.updater/logs/"
            if (!File(logPath).exists())
                File(logPath).mkdirs()
            val logFile = File("${Environment.getExternalStorageDirectory().path}/kernel.updater/logs/$logName")
            val fileWriter = FileWriter(logFile)
            fileWriter.append(log)
            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            Log.e("writeLogToFile", "$e")
        }
    }

    /**
     *  Helper method to write logs to a file when manual flashing is under progress.
     *  Logs are stored under /sdcard/kernel.updater/logs.
     *  @param context Reference of the base Activity.
     *  @param bodyOfJSON The response that's returned from the remote in JSON format.
     */
    fun saveJSONtoPreferences(context: Context, bodyOfJSON: String?) {
        if (!bodyOfJSON.equals(null)) {
            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
            preferenceManager.edit().putBoolean(context.getString(R.string.is_json_saved), true).apply()

            val saveResponseStringPref = context.getSharedPreferences(context.getString(R.string.save_json), Context.MODE_PRIVATE)
            saveResponseStringPref.edit().putString(context.getString(R.string.json_response), bodyOfJSON).apply()
        }
    }

    /**
     *  Helper method to write logs to a file when manual flashing is under progress.
     *  Logs are stored under /sdcard/kernel.updater/logs.
     *  @param context Reference of the base Activity.
     *  @return Returns the offline data in JSON format (String).
     */
    fun loadOfflineData(context: Context): String? {
        val offlineData = context.getSharedPreferences(context.getString(R.string.save_json), Context.MODE_PRIVATE)
        return offlineData.getString(context.getString(R.string.json_response), "")
    }

    /**
     *  Helper method to write logs to a file when manual flashing is under progress.
     *  Logs are stored under /sdcard/kernel.updater/logs.
     *  @param context Reference of the base Activity.
     *  @param changelogData The response that's returned from the remote in text format.
     */
    fun saveChangelogOffline(context: Context, changelogData: String) {
        if (changelogData != "") {
            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
            preferenceManager.edit().putBoolean(context.getString(R.string.is_changelog_saved), true).apply()

            val saveResponseStringPref = context.getSharedPreferences(context.getString(R.string.save_changelog), Context.MODE_PRIVATE)
            saveResponseStringPref.edit().putString(context.getString(R.string.changelog_response), changelogData).apply()
        }
    }

    /**
     *  Helper method to write logs to a file when manual flashing is under progress.
     *  Logs are stored under /sdcard/kernel.updater/logs.
     *  @param context Reference of the base Activity.
     *  @return Returns the offline changelog data in the ArrayList<String> format
     */
    fun loadOfflineChangelog(context: Context): ArrayList<String> {
        val changelogList = ArrayList<String>()
        val changelogPref = context.getSharedPreferences(context.getString(R.string.save_changelog), Context.MODE_PRIVATE)
        val changelog = changelogPref.getString(context.getString(R.string.changelog_response), "")
        if (changelog != "") {
            val changelogData = changelog!!.split("\n")
            for (log in changelogData)
                changelogList.add(log)
            return changelogList
        }
        /**
         * When the code execution is here, we know that there's no offline changelog available.
         * So appending an empty text and returning will be helpful for verifying.
         */
        changelogList.add("")
        return changelogList
    }

    /**
     *  Helper method to reboot device once auto flashing method is invoked
     */
    fun rebootDevice() {
        val process = Runtime.getRuntime().exec("su")
        val dos = DataOutputStream(process.outputStream)
        dos.writeBytes("${Constants.SHUTDOWN_BROADCAST}\n")
        dos.writeBytes("${Constants.SYNC}\n")
        dos.writeBytes("${Constants.NORMAL_REBOOT_CMD}\n")
        dos.writeBytes("exit\n")
        dos.flush()
        process.waitFor()
    }

    /**
     * Helper method to check if storage permissions are granted by the user
     * @param context Reference from calling Activity
     */
    fun isStoragePermissionGranted(context: Activity, storagePermissionCode: Int): Boolean {
        return if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            true
        else {
            ActivityCompat.requestPermissions(context, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), storagePermissionCode)
            false
        }
    }

    fun showDialog(context: Context, absolutePath: String) {
        val packageToBeFlashed = absolutePath.substring(absolutePath.lastIndexOf('/') + 1, absolutePath.length)
        AlertDialog.Builder(context, R.style.DialogTheme)
                .setTitle("Flash Kernel")
                .setMessage("You're about to flash $packageToBeFlashed within the app. Your device will reboot after the flashing is successful. Would you like to flash now?")
                .setPositiveButton("Flash", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        FlashKernelTask(context).execute(context, absolutePath)
                    }
                })
                .setNegativeButton("Later", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        dialog!!.dismiss()
                    }
                })
                .show()
    }

    private fun setVisibilityForViews(context: Activity) {
        // Set required views enabled
        if (context.update_card_stub != null)
            context.update_card_stub.inflate()
        if (context.package_list_stub != null)
            context.package_list_stub.inflate()
        if (context.update_info_stub != null)
            context.update_info_stub.inflate()
        if (context.flash_expanded_stub != null)
            context.flash_expanded_stub.inflate()
        context.packageInfoTextView.isSelected = true
        context.md5InfoTextView.isSelected = true
        context.fileName.isSelected = true
        context.expanded_packageInfoTextView.isSelected = true
        if (context.update_info_expanded.visibility == View.VISIBLE)
            context.update_info_expanded.visibility = View.GONE
        context.updates_compact.visibility = View.VISIBLE
    }
}
