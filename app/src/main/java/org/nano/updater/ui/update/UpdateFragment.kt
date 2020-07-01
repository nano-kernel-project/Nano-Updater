package org.nano.updater.ui.update

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentUpdateBinding
import org.nano.updater.model.Kernel
import org.nano.updater.model.NanoUpdate
import org.nano.updater.model.Updater
import org.nano.updater.network.DownloadTask
import org.nano.updater.ui.MainActivity
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.util.Constants
import org.nano.updater.util.FileUtils
import org.nano.updater.util.FileUtils.getUpdatePackage
import org.nano.updater.util.SnackBarUtils
import org.nano.updater.util.createMaterialElevationScale
import java.io.File
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 */
class UpdateFragment : Fragment() {
    private lateinit var binding: FragmentUpdateBinding
    private val args: UpdateFragmentArgs by navArgs()
    private val position: Int by lazy(LazyThreadSafetyMode.NONE) { args.position }

    @Inject
    lateinit var homeViewModel: HomeViewModel

    @Inject
    lateinit var updateViewModel: UpdateViewModel

    @Inject
    lateinit var snackBarUtils: SnackBarUtils

    private val adapter by lazy { ChangelogAdapter() }

    private val onBackPressedCallback by lazy {
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                exitTransition = createMaterialElevationScale(false).apply {
                    duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
                }
                findNavController().navigateUp()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
        onBackPressedCallback.isEnabled = true

        // Force hide BottomAppBar on this Fragment
        (requireActivity() as MainActivity).binding.bottomAppBar.hideOnScroll = false

        // Get a reference to updateData
        val updateData = homeViewModel.getUpdateData().value!!
        binding.run {
            // Hide or shrink FAB based on scroll direction
            this.updateScrollView.setOnScrollChangeListener { _, _, scrollY: Int, _, oldScrollY: Int ->
                if (scrollY > oldScrollY) {
                    binding.updateFab.shrink()
                } else {
                    binding.updateFab.extend()
                }
            }

            lifecycleOwner = this@UpdateFragment
            position = this@UpdateFragment.position
            kernel = updateData.kernel
            updater = updateData.updater

            // Update fab listener to either Install or Download
            updateFabListener(updateData)

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
                when (it) {
                    UpdateViewModel.DownloadStatus.CANCELLED,
                    UpdateViewModel.DownloadStatus.COMPLETED -> {
                        if (it == UpdateViewModel.DownloadStatus.CANCELLED)
                            closeNotification()
                        binding.updateFab.isEnabled = true
                        updateFabListener(updateData)
                    }
                    else -> {
                        binding.updateFab.isEnabled = false
                        updateFabListener(updateData)
                    }
                }
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
        isUpdateVerified: Boolean
    ) {
        binding.updateFab.setOnClickListener {
            if (!isUpdateVerified) {
                // Force user to download the update
                val downloadUrl = if (position == 1)
                    updateData.kernel.kernelLink
                else
                    updateData.updater.updaterLink
                DownloadTask(
                    requireContext(),
                    homeViewModel,
                    position == 1
                ).execute(
                    downloadUrl,
                    getUpdatePackageName(if (position == 1) updateData.kernel else updateData.updater)
                )
            } else {
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
                } else {
                    // User clicked the app updater card
                    // Launch package installer for installing update APK
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "org.nano.updater.fileprovider",
                        File(
                            requireContext().getExternalFilesDir("updater"),
                            getUpdatePackageName(if (position == 1) updateData.kernel else updateData.updater)
                        )
                    )
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    private fun updateFabListener(updateData: NanoUpdate) {
        // Verify checksum of the update package
        // If matched, let user install the update
        // Otherwise, force user to download the update
        if (FileUtils.verifyChecksum(
                updateFile = getUpdatePackage(
                    requireContext(),
                    getUpdatePackageName(if (position == 1) updateData.kernel else updateData.updater),
                    position == 1
                ),
                referenceChecksum = getUpdateMD5Checksum(if (position == 1) updateData.kernel else updateData.updater)
            )
        ) {
            setFabClickListener(updateData = updateData, isUpdateVerified = true)
            updateFABTextAndIcon(isUpdateVerified = true)
        } else {
            setFabClickListener(updateData = updateData, isUpdateVerified = false)
            updateFABTextAndIcon(isUpdateVerified = false)
        }
    }

    private fun closeNotification() {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager!!.cancel(Constants.NOTIFICATION_ID)
    }

    private fun getUpdatePackageName(updateData: Any): String {
        return if (updateData is Kernel)
            updateData.kernelLink.substring(
                updateData.kernelLink.lastIndexOf('/') + 1,
                updateData.kernelLink.length
            )
        else
            (updateData as Updater).updaterLink.substring(
                updateData.updaterLink.lastIndexOf('/') + 1,
                updateData.updaterLink.length
            )
    }

    private fun getUpdateMD5Checksum(updateData: Any): String {
        return if (updateData is Kernel)
            updateData.kernelMD5
        else
            (updateData as Updater).updaterMD5
    }

    private fun updateFABTextAndIcon(isUpdateVerified: Boolean) {
        binding.updateFab.apply {
            if (isUpdateVerified) {
                icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_save_alt)
                text = getString(R.string.action_install)
            } else {
                icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_get_app)
                text = getString(R.string.action_download)
            }
        }
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
