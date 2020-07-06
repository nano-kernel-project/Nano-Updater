package org.nano.updater.util

import android.content.Context
import android.text.format.DateUtils
import org.nano.updater.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeUtils @Inject constructor(private val context: Context) {

    fun formatLastCheckedString(milliSeconds: Long): String = context.getString(R.string.last_checked_formatted, DateUtils.getRelativeTimeSpanString(milliSeconds, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString())

    fun formatDate(dateTime: String?): String {
        return try {
            val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = formatter.parse(dateTime) as Date
            SimpleDateFormat("d MMM, Y", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun formatUpdaterDate(dateTime: String): String {
        return try {
            val formatter: DateFormat = SimpleDateFormat("yyyy.M.d.hms", Locale.getDefault())
            val date = formatter.parse(dateTime) as Date
            SimpleDateFormat("d MMM, Y", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
