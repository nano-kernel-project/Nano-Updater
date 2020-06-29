package org.nano.updater.util

import android.content.Context
import org.nano.updater.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeUtils @Inject constructor(private val context: Context) {

    fun formatLastCheckedString(milliSeconds: Long): String {
        // In case user never checked for update, return R.string.last_checked_never
        if (milliSeconds == -1L)
            return context.getString(R.string.last_checked_never)

        if (milliSeconds == 0L)
            return context.getString(
                R.string.last_checked_moments_ago
            )

        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliSeconds)
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        when {
            seconds < 60 -> return context.getString(
                R.string.last_checked,
                seconds.toString(),
                context.getString(R.string.seconds)
            )
            minutes < 60 -> return context.getString(
                R.string.last_checked,
                minutes.toString(),
                context.getString(R.string.minutes)
            )
            hours < 24 -> return if (hours == 1L) context.getString(
                R.string.last_checked,
                hours.toString(),
                context.getString(R.string.hour)
            )
            else
                context.getString(
                    R.string.last_checked,
                    hours.toString(),
                    context.getString(R.string.hours)
                )
            else -> return if (days == 1L) context.getString(
                R.string.last_checked,
                days.toString(),
                context.getString(R.string.day)
            )
            else
                context.getString(
                    R.string.last_checked,
                    hours.toString(),
                    context.getString(R.string.days)
                )
        }
    }

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
