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
 * Last modified 2/10/19 7:12 PM.
 */

package com.codebot.axel.kernel.updater

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import com.codebot.axel.kernel.updater.util.FlashKernel
import com.codebot.axel.kernel.updater.util.Utils
import java.lang.ref.WeakReference

class FlashKernelTask(context: Context) : AsyncTask<Any, String, Boolean>() {

    private val contextRef = WeakReference(context)
    private val progressDialog = ProgressDialog(contextRef.get())

    override fun doInBackground(vararg params: Any?): Boolean {
        val context = (params[0] as Context)
        val absolutePath = params[1] as String
        if (!FlashKernel().isAnyKernelZip(context, absolutePath))
            return false
        publishProgress("Flashing kernel")
        FlashKernel().unzipAndFlash(context, absolutePath)
        return true
    }

    override fun onPreExecute() {
        progressDialog.isIndeterminate = true
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Verifying zip")
        progressDialog.show()
        super.onPreExecute()
    }

    override fun onPostExecute(result: Boolean) {
        progressDialog.cancel()
        if (!result)
            Utils().snackBar(contextRef.get()!!, "Choose a valid AnyKernel zip")
        super.onPostExecute(result)
    }
}