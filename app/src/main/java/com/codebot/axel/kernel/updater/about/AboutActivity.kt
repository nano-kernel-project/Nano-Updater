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
 * Last modified 26/7/19 3:31 PM.
 */

package com.codebot.axel.kernel.updater.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.codebot.axel.kernel.updater.R
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        about_text.text = "App version: ${packageManager.getPackageInfo(packageName, 0).versionName}"

        about_dialog_telegram.setOnClickListener {
            val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Nano_Releases"))
            startActivity(telegramIntent)
        }

        about_dialog_xda.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://forum.xda-developers.com/redmi-5/development/kernel-nano-kernel-t3914450"))
            startActivity(browserIntent)
        }

        about_dialog_github.setOnClickListener {
            val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nano-kernel-project/Nano-Updater"))
            startActivity(githubIntent)
        }
    }
}
