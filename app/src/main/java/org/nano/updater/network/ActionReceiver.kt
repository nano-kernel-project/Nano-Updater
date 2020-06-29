package org.nano.updater.network

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.nano.updater.NanoApplication
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.ui.update.UpdateViewModel
import org.nano.updater.util.Constants
import javax.inject.Inject

class ActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var homeViewModel: HomeViewModel

    override fun onReceive(context: Context?, intent: Intent?) {
        (context!!.applicationContext as NanoApplication).appComponent.inject(this)

        when (intent!!.getStringExtra("action")) {
            Constants.ACTION_CANCEL -> {
                homeViewModel.setDownloadStatus(UpdateViewModel.DownloadStatus.CANCELLED)
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                notificationManager!!.cancel(Constants.NOTIFICATION_ID)
            }
        }
    }
}