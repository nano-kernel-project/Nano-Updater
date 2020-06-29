package org.nano.updater.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nano.updater.NanoApplication
import org.nano.updater.ui.flash.ConsoleAdapter
import org.nano.updater.ui.update.UpdateViewModel
import org.nano.updater.util.Constants
import org.nano.updater.util.FlashUtils
import org.nano.updater.worker.WorkerUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A class that initiates the Flashing process in the background thread
 */
@Singleton
class FlashService @Inject constructor() : Service() {

    @Inject
    lateinit var updateViewModel: UpdateViewModel

    @Inject
    lateinit var flashUtils: FlashUtils

    @Inject
    lateinit var consoleAdapter: ConsoleAdapter

    private val liveLog by lazy {
        ArrayList<String>()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            Constants.FLASH_NOTIFICATION_ID,
            WorkerUtils.makeFlashNotification(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateViewModel.setIsFlashServiceRunning(true)
        val fileAbsolutePath = intent?.getStringExtra(Constants.KEY_ABSOLUTE_PATH)
        consoleAdapter.setLiveFlashLog(liveLog)
        CoroutineScope(Dispatchers.IO).launch {
            flashUtils.flashKernel(
                applicationContext,
                fileAbsolutePath!!,
                liveLog,
                consoleAdapter
            )
        }
        return START_NOT_STICKY
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        (base?.applicationContext as NanoApplication).appComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateViewModel.setIsFlashServiceRunning(false)
    }
}