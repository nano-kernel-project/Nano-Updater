package org.nano.updater.ui.settings

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import kotlinx.coroutines.launch
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentSettingsBinding
import org.nano.updater.model.AppPreference
import org.nano.updater.util.Constants
import org.nano.updater.util.SharedPrefUtils
import org.nano.updater.util.createMaterialElevationScale
import org.nano.updater.worker.UpdateCheckWorker
import org.nano.updater.worker.WorkerUtils
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : Fragment(), AppPreferenceAdapter.AppPreferenceAdapterListener {
    private lateinit var settingsBinding: FragmentSettingsBinding

    private val sharedPrefUtils by lazy {
        SharedPrefUtils(requireContext())
    }

    private val appSettings by lazy {
        ArrayList<AppPreference>().apply {
            add(
                AppPreference(
                    0,
                    getString(R.string.display_flash_logs),
                    getString(R.string.display_flash_logs_summary),
                    sharedPrefUtils.isDisplayLogsToggled()
                )
            )

            add(
                AppPreference(
                    1,
                    getString(R.string.reboot_after_install),
                    getString(R.string.reboot_after_install_summary),
                    sharedPrefUtils.isRebootAfterFlashToggled()
                )
            )

            add(
                AppPreference(
                    2,
                    getString(R.string.check_updates_periodically),
                    getString(R.string.check_updates_periodically_summary),
                    sharedPrefUtils.isCheckUpdatesPeriodicallyToggled()
                )
            )

            add(
                AppPreference(
                    3,
                    getString(R.string.cleanup_update_dir),
                    getString(R.string.cleanup_update_dir_summary),
                    sharedPrefUtils.isCleanupDirToggled()
                )
            )
        }
    }

    private val adapter by lazy {
        AppPreferenceAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = createMaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settingsBinding = FragmentSettingsBinding.inflate(inflater)
        return settingsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Handler().postDelayed({
            settingsBinding.appPrefRecyclerView.adapter = adapter
        }, resources.getInteger(R.integer.nano_motion_duration_large).toLong())
        adapter.submitList(appSettings)

        WorkManager.getInstance(requireContext())
            .getWorkInfosByTagLiveData(Constants.TAG_UPDATE_CHECK_WORKER)
            .observe(viewLifecycleOwner, Observer {
                // We have only one work enqueued, so accessing 0th index is what we need
                if (it != null && it.isNotEmpty() && it[0].state == WorkInfo.State.ENQUEUED) {
                    val updateCheckResult = it[0].outputData
                    if (updateCheckResult.getBoolean(Constants.KEY_IS_UPDATE_AVAILABLE, false))
                        WorkerUtils.notifyUpdateAvailable(requireContext().applicationContext)
                }
            })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }

    override fun onPreferenceClick(appPreference: AppPreference) {
        appPreference.toggle = !appPreference.toggle
        adapter.notifyItemChanged(appPreference.id)

        when (appPreference.id) {
            0 -> sharedPrefUtils.toggleDisplayLogs(appPreference.toggle)
            1 -> sharedPrefUtils.toggleRebootAfterFlash(appPreference.toggle)
            2 -> {
                sharedPrefUtils.toggleCheckUpdatesPeriodically(appPreference.toggle)
                if (appPreference.toggle)
                    lifecycleScope.launch {
                        initPeriodicUpdateCheck()
                    }
                else
                    WorkManager.getInstance(requireContext())
                        .cancelAllWorkByTag(Constants.TAG_UPDATE_CHECK_WORKER)
            }
            else -> sharedPrefUtils.toggleCleanupDir(appPreference.toggle)
        }
    }

    private fun initPeriodicUpdateCheck() {
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()
        val workRequest =
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(3, TimeUnit.DAYS)
                .addTag(Constants.TAG_UPDATE_CHECK_WORKER)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .setInputData(Data.Builder().putFloat(Constants.KEY_CURRENT_VERSION, 1f).build())
                .build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            Constants.UPDATE_CHECK_WORKER,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
