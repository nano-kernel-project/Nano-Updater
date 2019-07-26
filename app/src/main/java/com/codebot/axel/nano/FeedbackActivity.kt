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
 * Last modified 24/7/19 10:32 PM.
 */

package com.codebot.axel.nano

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codebot.axel.nano.util.Constants
import com.codebot.axel.nano.util.Constants.Companion.FEEDBACK_FORM_URL
import com.codebot.axel.nano.util.Constants.Companion.FORM_DATA_TYPE
import com.codebot.axel.nano.util.Constants.Companion.KEY_CODENAME_DEVICE
import com.codebot.axel.nano.util.Utils
import kotlinx.android.synthetic.main.activity_feedback.*
import okhttp3.*
import java.io.IOException
import java.net.URLEncoder

class FeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        feedback_submit.setOnClickListener {
            if (Utils().isNetworkAvailable(this@FeedbackActivity))
                validateAndSubmitFeedback(feedback_name.text.toString(), feedback_telegram.text.toString(), feedback_problem.text.toString())
            else
                Utils().snackBar(this@FeedbackActivity, "No internet connection")
        }
    }

    private fun validateAndSubmitFeedback(feedbackName: String, feedbackTelegram: String, feedbackProblem: String) {
        if (TextUtils.isEmpty(feedbackName) || TextUtils.isEmpty(feedbackTelegram) || TextUtils.isEmpty(feedbackProblem))
            Toast.makeText(this@FeedbackActivity, "All fields are mandatory", Toast.LENGTH_SHORT).show()
        else {
            val postBody: String
            try {
                postBody = Constants.FEEDBACK_NAME_ENTRY_ID + "=" + URLEncoder.encode(feedbackName, "UTF-8") + "&" +
                        Constants.FEEDBACK_TELEGRAM_ENTRY_ID + "=" + URLEncoder.encode(feedbackTelegram, "UTF-8") + "&" +
                        Constants.FEEDBACK_DEVICE_ENTRY_ID + "=" + URLEncoder.encode(Utils().checkInstalledVersion(KEY_CODENAME_DEVICE), "UTF-8") + "&" +
                        Constants.FEEDBACK_PROBLEM_ENTRY_ID + "=" + URLEncoder.encode(feedbackProblem, "UTF-8")

                val client = OkHttpClient()
                val requestBody = RequestBody.create(FORM_DATA_TYPE, postBody)
                val request = Request.Builder().post(requestBody).url(FEEDBACK_FORM_URL).build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Toast.makeText(this@FeedbackActivity, "Form submission failed unexpectedly", Toast.LENGTH_SHORT).show()
                        Log.e("FeedbackActivity", "$e")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        runOnUiThread {
                            Toast.makeText(this@FeedbackActivity, "Thank you for your feedback", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            } catch (e: Exception) {
                Toast.makeText(this@FeedbackActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                Log.e("FeedbackActivity", "$e")
            }

        }
    }
}
