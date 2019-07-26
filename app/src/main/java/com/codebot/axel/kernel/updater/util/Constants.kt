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
 * Last modified 24/7/19 10:20 PM.
 */

package com.codebot.axel.kernel.updater.util

import android.view.animation.Animation
import android.view.animation.RotateAnimation
import okhttp3.MediaType

class Constants {

    companion object {
        const val FEEDBACK_FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSf2M2N-65QKzHcwm6tNDCK5KNTyiwDRyLq5evHcK_2LsG7dkw/formResponse"
        val FORM_DATA_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8")
        const val FEEDBACK_NAME_ENTRY_ID = "entry.1174402051"
        const val FEEDBACK_TELEGRAM_ENTRY_ID = "entry.1168756939"
        const val FEEDBACK_DEVICE_ENTRY_ID = "entry.615481595"
        const val FEEDBACK_PROBLEM_ENTRY_ID = "entry.774677401"
        const val KEY_CODENAME_DEVICE = "ro.product.device"
        const val API_ENDPOINT_URL = "https://raw.githubusercontent.com/nano-kernel-project/Nano_OTA_changelogs/master/api.json"
        const val CHECK_FOR_UPDATES = "Check For Updates"
        const val DOWNLOAD = "Download"
        const val BUILD_DATE = "nano.release.date"
        const val BUILD_VERSION = "nano.version"
        val ROTATE_ANIMATION = RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
    }
}
