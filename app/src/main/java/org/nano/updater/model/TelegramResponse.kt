package org.nano.updater.model

import com.google.gson.annotations.SerializedName

data class TelegramResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: TelegramResult
)

data class TelegramResult(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("chat") val telegramChat: TelegramChat,
    @SerializedName("date") val date: Long,
    @SerializedName("document") val document: TelegramDocument,
    @SerializedName("caption") val caption: String
)

data class TelegramChat(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String
)

data class TelegramDocument(
    @SerializedName("file_name") val fileName: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_size") val fileSize: Long
)