package org.nano.updater.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import org.nano.updater.R
import org.nano.updater.network.ActionReceiver
import org.nano.updater.util.Constants
import org.nano.updater.util.Constants.CHANNEL_ID
import org.nano.updater.util.Constants.FLASH_CHANNEL_ID
import org.nano.updater.util.Constants.FLASH_NOTIFICATION_CHANNEL
import org.nano.updater.util.Constants.FLASH_NOTIFICATION_CHANNEL_DESC
import org.nano.updater.util.Constants.NOTIFICATION_CHANNEL_DESC
import org.nano.updater.util.Constants.NOTIFICATION_CHANNEL_NAME
import org.nano.updater.util.Constants.NOTIFICATION_ID
import org.nano.updater.util.Constants.UPDATE_CHANNEL_ID
import org.nano.updater.util.Constants.UPDATE_NOTIFICATION_CHANNEL
import org.nano.updater.util.Constants.UPDATE_NOTIFICATION_CHANNEL_DESC
import org.nano.updater.util.Constants.UPDATE_NOTIFICATION_ID
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object WorkerUtils {
    private var prevBytesRead = 0L

    fun saveStreamToFile(inputStream: InputStream, file: File): Boolean {
        return try {
            FileOutputStream(file, true).use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            inputStream.close()
        }
    }

    fun makeStatusNotification(
        bytesRead: Long,
        contentLength: Long,
        done: Boolean,
        context: Context,
        fileName: String,
        isKernelUpdate: Boolean
    ) {
        if (bytesRead >= 100000L && bytesRead - prevBytesRead < 1000000L && !done)
            return

        // Update prevBytesRead
        prevBytesRead = bytesRead

        val message: String
        val progress: Int = if (contentLength != 0L)
            (bytesRead / 1000000).toInt()
        else
            0

        message = if (done)
            context.getString(R.string.update_ready)
        else if (!done && contentLength == 0L)
            ""
        else
            context.getString(R.string.download_progress, progress, contentLength / 1000000)

        val name = NOTIFICATION_CHANNEL_NAME
        val description = NOTIFICATION_CHANNEL_DESC
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description

        // Add the channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)

        // Create the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(fileName)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setVibrate(LongArray(0))

        if (!done) {
            val cancelIntent = Intent(context, ActionReceiver::class.java).apply {
                putExtra(
                    "action",
                    Constants.ACTION_CANCEL
                )
                putExtra("fileName", fileName)
            }
            val cancelPendingIntent = PendingIntent.getBroadcast(
                context,
                Constants.CANCEL_REQUEST_CODE,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                R.drawable.ic_close,
                context.getString(R.string.action_cancel),
                cancelPendingIntent
            )
            if (progress == 0)
                builder.setProgress(0, 0, true)
            else
                builder.setProgress((contentLength / 1000000).toInt(), progress, false)
        } else {
            builder.setProgress(0, 0, false)
            builder.setContentIntent(
                NavDeepLinkBuilder(context)
                    .setGraph(R.navigation.nav_graph)
                    .setDestination(R.id.updateFragment)
                    .setArguments(bundleOf("position" to isKernelUpdate.toInt()))
                    .createPendingIntent()
            )
        }

        // Show the notification
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    fun notifyUpdateAvailable(context: Context) {
        val name = UPDATE_NOTIFICATION_CHANNEL
        val description = UPDATE_NOTIFICATION_CHANNEL_DESC
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(UPDATE_CHANNEL_ID, name, importance)
        channel.description = description

        // Add the channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.homeFragment)
            .createPendingIntent()

        // Create the notification
        val builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("New updates available")
            .setContentText("Tap to view the updates")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVibrate(LongArray(0))

        // Show the notification
        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, builder.build())
    }

    fun makeFlashNotification(context: Context): Notification? {
        val name = FLASH_NOTIFICATION_CHANNEL
        val description = FLASH_NOTIFICATION_CHANNEL_DESC
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(FLASH_CHANNEL_ID, name, importance)
        channel.description = description

        // Add the channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)

        // Create the notification
        val builder = NotificationCompat.Builder(context, FLASH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Flashing kernel...")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setVibrate(LongArray(0))

        // Show the notification
        return builder.build()
    }
}

fun Boolean.toInt(): Int {
    return if (this)
        1
    else
        2
}
