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
 * Last modified 26/7/19 3:32 PM.
 */

package com.codebot.axel.kernel.updater

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash.*

// Timer for splash screen duration (1 sec)
private const val SPLASH_DURATION = 1000L

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splash_imageVIew.scaleType = ImageView.ScaleType.FIT_XY

        Handler().postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, SPLASH_DURATION)
    }
}
