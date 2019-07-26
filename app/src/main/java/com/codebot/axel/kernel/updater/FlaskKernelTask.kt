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
 * Last modified 26/7/19 3:22 PM.
 */

package com.codebot.axel.kernel.updater

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import com.codebot.axel.kernel.updater.util.FlashKernel
import java.lang.ref.WeakReference

class FlashKernelTask(context: Context) : AsyncTask<Any, Void, Void>() {

    private val contextRef = WeakReference(context)
    private val progressDialog = ProgressDialog(contextRef.get())

    override fun doInBackground(vararg params: Any?): Void? {
        FlashKernel().unzipAndFlash((params[0] as Context), (params[1] as String), progressDialog)
        return null
    }

    override fun onPreExecute() {
        progressDialog.isIndeterminate = true
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Flashing Kernel")
        progressDialog.show()
        super.onPreExecute()
    }

    override fun onPostExecute(result: Void?) {
        progressDialog.cancel()
        super.onPostExecute(result)
    }
}