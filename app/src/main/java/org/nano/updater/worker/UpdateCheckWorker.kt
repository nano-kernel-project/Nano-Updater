package org.nano.updater.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nano.updater.NanoApplication
import org.nano.updater.repository.UpdateRepository
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.util.Constants
import java.text.NumberFormat
import javax.inject.Inject

class UpdateCheckWorker(private val context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var homeViewModel: HomeViewModel

    @Inject
    lateinit var updateRepository: UpdateRepository

    init {
        (context.applicationContext as NanoApplication).appComponent.inject(this)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Load latest data from server
        updateRepository.loadUpdateData(
            forceRefresh = true,
            homeViewModel = homeViewModel
        )

        val currentVersion = inputData.getFloat(Constants.KEY_CURRENT_VERSION, -1f)
        val latestVersion = homeViewModel.getUpdateData().value!!.kernel.kernelVersion
        val latestVersionFormatted = NumberFormat.getInstance().parse(latestVersion)?.toFloat()
        if (currentVersion == -1f || latestVersionFormatted == -1f)
            Result.failure(
                Data.Builder().apply {
                    putBoolean(Constants.KEY_IS_UPDATE_AVAILABLE, false)
                }.build()
            )
        else if (currentVersion < latestVersionFormatted!!) {
            WorkerUtils.notifyUpdateAvailable(context)
            Result.success(
                Data.Builder().apply {
                    putBoolean(Constants.KEY_IS_UPDATE_AVAILABLE, true)
                }.build()
            )
        } else {
            Result.success(
                Data.Builder().apply {
                    putBoolean(Constants.KEY_IS_UPDATE_AVAILABLE, false)
                }.build()
            )
        }
    }
}