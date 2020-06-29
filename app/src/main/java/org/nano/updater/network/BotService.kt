package org.nano.updater.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.nano.updater.model.TelegramResponse
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface BotService {

    @POST
    @Multipart
    fun sendLogWithCaption(
        @Part("chat_id") chatId: RequestBody,
        @Part logFile: MultipartBody.Part,
        @Part("caption") caption: RequestBody,
        @Part("parse_mode") parseMode: RequestBody,
        @Url apiUrl: String
    ): Call<TelegramResponse>
}