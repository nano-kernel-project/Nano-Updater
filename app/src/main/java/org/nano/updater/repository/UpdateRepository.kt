package org.nano.updater.repository

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.nano.updater.BuildConfig
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.di.module.DatabaseModule
import org.nano.updater.model.NanoUpdate
import org.nano.updater.model.asEntityModel
import org.nano.updater.model.asInformationCard
import org.nano.updater.model.asUpdateCard
import org.nano.updater.model.entity.Update
import org.nano.updater.model.entity.asDomainModel
import org.nano.updater.network.UpdateService
import org.nano.updater.ui.home.HomeStore
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.util.Converters
import org.nano.updater.util.TimeUtils
import java.net.HttpURLConnection
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A class that manages the update data
 */
@Singleton
class UpdateRepository @Inject constructor(
    databaseModule: DatabaseModule,
    private val context: Context
) {
    private val updaterDatabase = databaseModule.provideUpdaterDatabase(context)
    private val updaterDao = databaseModule.provideUpdaterDao(updaterDatabase)

    @Inject
    lateinit var updateService: UpdateService

    @Inject
    lateinit var client: OkHttpClient

    @Inject
    lateinit var timeUtils: TimeUtils

    init {
        (context.applicationContext as NanoApplication).appComponent.inject(this)
    }

    /**
     * Helper method to fetch the update data either from remote server or local database
     * @param forceRefresh: Force updater to fetch data from remote server
     * @param homeViewModel: A refernce to homeViewModel to update values
     */
    suspend fun loadUpdateData(
        forceRefresh: Boolean,
        homeViewModel: HomeViewModel
    ) = withContext(Dispatchers.IO) {
        var currentKernelVersion: String? = null
        var currentKernelBuild: String? = null
        // Update current version and build date from sample file in /sdcard/test.txt
        // test.txt:
        // version=1.2.3
        // build=yyyy-MM-dd
        try {
            val kernelDetails = Shell.su("cat /sdcard/test.txt").exec().out
            currentKernelVersion = kernelDetails[0].split("=")[1].trim()
            currentKernelBuild = kernelDetails[1].split("=")[1].trim()
        } catch (e: Exception) { }
        // Check if cache data is not null
        // In case it is not null, load data from cache
        val localData = loadRoomData()
        if (localData != null && localData.asDomainModel() != null && !forceRefresh) {
            val updateData = localData.asDomainModel()!!
            val lastCheckedString = timeUtils.formatLastCheckedString(
                LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC)
                    .toEpochMilli() - localData.lastChecked
            )
            homeViewModel.setUpdateData(updateData)
            checkUpdates(updateData, lastCheckedString, currentKernelBuild, currentKernelVersion)
            return@withContext
        }

        // Now that we are here, updater fetches the data from remote
        val response = updateService.getUpdateData().execute()

        // If status code is 404 then the device is not yet official
        // because the page isn't available yet
        if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
            homeViewModel.setIsUnsupportedDevice(true)
            return@withContext
        }

        // Hold a reference to updateData
        val updateData: NanoUpdate = response.body()!!

        // Remove any NetworkInterceptors if added by the org.nano.updater.network.DownloadTask
        // and hold the kernel and updater changelog response from the server
        val kernelChangelog = client.newBuilder().apply {
            networkInterceptors().apply { if (size > 0) removeAt(0) }
        }.build().newCall(Request.Builder().url(updateData.kernel.kernelChangelogLink).build())
            .execute()
        val updaterChangelog = client
            .newCall(Request.Builder().url(updateData.updater.updaterChangelogLink).build())
            .execute()

        // Convert the string response to a List and store them in vars
        updateData.kernelChangelog =
            Converters.fromString(kernelChangelog.body()!!.string(), forceRefresh = true)
        updateData.updaterChangelog =
            Converters.fromString(updaterChangelog.body()!!.string(), forceRefresh = true)

        // Format dates
        updateData.kernel.kernelDate = timeUtils.formatDate(updateData.kernel.kernelDate)
        updateData.updater.updaterDate = timeUtils.formatDate(updateData.updater.updaterDate)

        // Set updateData of homeViewModel
        homeViewModel.setUpdateData(updateData)

        // Calculate last checked
        val lastCheckedForUpdate =
            LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC).toEpochMilli()

        // Compare versions and notify update if available
        checkUpdates(
            updateData,
            context.getString(R.string.last_checked_moments_ago),
            currentKernelBuild,
            currentKernelVersion
        )

        // Insert update data into room database
        insertRoomData(updateData, lastCheckedForUpdate)
    }

    // Get data from room database
    private suspend fun loadRoomData(): Update? {
        return updaterDao.getUpdateData()
    }

    // Insert data into room database
    private suspend fun insertRoomData(nanoUpdate: NanoUpdate, lastChecked: Long) =
        withContext(Dispatchers.IO) {
            updaterDao.insertUpdateData(nanoUpdate.asEntityModel(lastChecked))
        }

    /**
     * Method to check for updates
     * @param updateData: The data received from server and parsed to org.nano.updater.model.NanoUpdate
     * @param lastChecked: Chaining the var to use it at the end
     * @param currentKernelBuild: Holds the current kernel build
     * @param currentKernelVersion: Holds the current kernel version
     */
    private fun checkUpdates(
        updateData: NanoUpdate,
        lastChecked: String,
        currentKernelBuild: String?,
        currentKernelVersion: String?
    ) {
        val isKernelUpdateAvailable: Boolean
        val isAppUpdateAvailable: Boolean

        // Compare versions and notify update if available
        val kernelLatestVersionFormatted =
            NumberFormat.getInstance().parse(updateData.kernel.kernelVersion)?.toFloat()
        val updaterLatestVersionFormatted =
            NumberFormat.getInstance().parse(updateData.updater.updaterVersion)?.toFloat()

        // Hardcode current version for now
        val kernelCurrentVersionFormatted =
            NumberFormat.getInstance().parse(updateData.kernel.kernelVersion)?.toFloat()
        val updaterCurrentVersionFormatted =
            NumberFormat.getInstance().parse(updateData.updater.updaterVersion)?.toFloat()

        isKernelUpdateAvailable = kernelCurrentVersionFormatted!! < kernelLatestVersionFormatted!!
        isAppUpdateAvailable = updaterCurrentVersionFormatted!! < updaterLatestVersionFormatted!!

        // Update home with latest data
        refreshHome(
            updateData,
            isKernelUpdateAvailable,
            isAppUpdateAvailable,
            lastChecked,
            currentKernelBuild,
            currentKernelVersion
        )
    }


    private fun refreshHome(
        updateData: NanoUpdate,
        isKernelUpdateAvailable: Boolean,
        isAppUpdateAvailable: Boolean,
        lastChecked: String,
        currentKernelBuild: String?,
        currentKernelVersion: String?
    ) {
        HomeStore.update(
            0, asInformationCard(
                context,
                0,
                isKernelUpdateAvailable or isAppUpdateAvailable
            )
        )
        HomeStore.update(
            1,
            updateData.asUpdateCard(
                context,
                lastChecked,
                1,
                currentKernelVersion,
                timeUtils.formatDate(currentKernelBuild),
                isKernelUpdateAvailable
            )
        )
        HomeStore.update(
            2,
            updateData.asUpdateCard(
                context,
                lastChecked,
                2,
                BuildConfig.VERSION_CODE.toFloat().toString(),
                timeUtils.formatUpdaterDate(BuildConfig.VERSION_NAME),
                isAppUpdateAvailable
            )
        )
    }
}