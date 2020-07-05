package org.nano.updater.ui.update

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentUpdateBinding
import org.nano.updater.model.NanoUpdate
import org.nano.updater.network.DownloadTask
import org.nano.updater.ui.MainActivity
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.util.Constants
import org.nano.updater.util.FileUtils
import java.io.File
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * A simple [Fragment] subclass.
 */
class UpdateFragment : Fragment() {
    private lateinit var binding: FragmentUpdateBinding
    private val args: UpdateFragmentArgs by navArgs()
    private val position: Int by lazy(LazyThreadSafetyMode.NONE) { args.position }
    private var isUpdateVerified by Delegates.notNull<Boolean>()

    @Inject
    lateinit var homeViewModel: HomeViewModel

    @Inject
    lateinit var updateViewModel: UpdateViewModel

    private val adapter by lazy { ChangelogAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isUpdateVerified = args.isUpdateVerified
        postponeEnterTransition()
        prepareTransitions()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUpdateBinding.inflate(inflater)
        if (position != 1) {
            binding.updateTitle.text = requireContext().getString(R.string.update_app)
            binding.updateDesc.text = requireContext().getString(R.string.update_app_desc)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get a reference to ActivityBinding
        val activityBinding = (requireActivity() as MainActivity).binding

        // Get a reference to updateData
        val updateData = homeViewModel.getUpdateData().value!!
        binding.run {
            lifecycleOwner = this@UpdateFragment
            position = this@UpdateFragment.position
            kernel = updateData.kernel
            updater = updateData.updater

            // Update fab listener to either Install or Download
            setFabClickListener(updateData, activityBinding.fab)

            // Setup RecyclerView adapter
            this.updateChangelogRecyclerView.apply {
                setHasFixedSize(true)
                adapter = this@UpdateFragment.adapter
            }

            if (position == 1)
                adapter.submitList(updateData.kernelChangelog)
            else
                adapter.submitList(updateData.updaterChangelog)

            // Observe for download status
            // If download is either cancelled or completed, enabled FAB
            // Otherwise, disable it (Download is running)
            homeViewModel.getDownloadStatus().observe(viewLifecycleOwner, Observer {
                if (it == null)
                    return@Observer
                when (it) {
                    UpdateViewModel.DownloadStatus.CANCELLED,
                    UpdateViewModel.DownloadStatus.COMPLETED -> {
                        if (it == UpdateViewModel.DownloadStatus.CANCELLED)
                            closeNotification()
                        else
                            // Download is complete. Verify again for checksum and change isUpdateAvailable accordingly
                            isUpdateVerified = FileUtils.getIsUpdateVerified(requireContext(), homeViewModel, position as Int)
                        activityBinding.fab.isEnabled = true
                        setFabClickListener(updateData, activityBinding.fab)
                    }
                    else -> {
                        activityBinding.fab.isEnabled = false
                        setFabClickListener(updateData, activityBinding.fab)
                    }
                }
                homeViewModel.setDownloadStatus(null)
            })
        }
        startTransitions()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }

    private fun setFabClickListener(
        updateData: NanoUpdate,
        fab: FloatingActionButton
    ) {
        if (isUpdateVerified)
            fab.setImageResource(R.drawable.ic_save_alt)
        else
            fab.setImageResource(R.drawable.ic_get_app)
        fab.setOnClickListener {
            if (!isUpdateVerified)
            // Force user to download the update
                downloadUpdatePackage(updateData)
            else {
                if (position == 1) {
                    // User clicked the kernel update card
                    // Launch FlashFragment for flashing Kernel
                    exitTransition = Hold().apply {
                        duration =
                            resources.getInteger(R.integer.nano_motion_duration_large).toLong()
                    }
                    findNavController().navigate(
                        R.id.flashFragment,
                        null,
                        NavOptions.Builder().apply {
                            setPopUpTo(R.id.flashFragment, true)
                        }.build()
                    )
                } else
                // User clicked the app updater card
                // Launch package installer for installing update APK
                    installUpdateAPK(updateData)
            }
        }
    }

    private fun downloadUpdatePackage(updateData: NanoUpdate) {
        DownloadTask(
            requireContext(),
            homeViewModel,
            position == 1
        ).execute(
            if (position == 1) updateData.kernel.kernelLink else updateData.updater.updaterLink,
            homeViewModel.getUpdatePackageName(if (position == 1) updateData.kernel else updateData.updater)
        )
    }

    private fun closeNotification() {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager!!.cancel(Constants.NOTIFICATION_ID)
    }

    private fun installUpdateAPK(updateData: NanoUpdate) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "org.nano.updater.fileprovider",
            File(
                requireContext().getExternalFilesDir("updater"),
                homeViewModel.getUpdatePackageName(if (position == 1) updateData.kernel else updateData.updater)
            )
        )
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    private fun prepareTransitions() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            // Scope the transition to a view in the hierarchy so we know it will be added under
            // the bottom app bar
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        }
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.home_root
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        }
    }

    private fun startTransitions() {
        binding.executePendingBindings()
        startPostponedEnterTransition()
    }
}
