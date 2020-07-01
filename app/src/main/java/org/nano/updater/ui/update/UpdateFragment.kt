package org.nano.updater.ui.update

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
        updateType: ArrayList<Any>,
        isUpdateVerified: Boolean
    ) {
        binding.updateFab.setOnClickListener {
            if (!isUpdateVerified) {
                // Force user to download the update
                val downloadUrl = if (position == 1)
                    (updateType[0] as Kernel).kernelLink
                else
                    (updateType[0] as Updater).updaterLink
                DownloadTask(
                    requireContext(),
                    homeViewModel,
                    position == 1
                ).execute(
                    downloadUrl, updateType[2] as String
                )
            } else {
                if (position == 1) {
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
                    // Launch package installer for installing update APK
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "org.nano.updater.fileprovider",
                        File(
                            requireContext().getExternalFilesDir("updater"),
                            updateType[2] as String
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
        val updateTypeAndFileName = getUpdateTypeAndFileName(updateData, position == 1)

        // Verify checksum of the update package
        // If matched, let user install the update
        // Otherwise, force user to download the update
        if (FileUtils.verifyChecksum(
                updateFile = getUpdatePackage(
                    requireContext(),
                    updateTypeAndFileName[2] as String,
                    position == 1
                ),
                referenceChecksum = updateTypeAndFileName[1] as String
            )
        ) {
            setFabClickListener(updateType = updateTypeAndFileName, isUpdateVerified = true)
            binding.updateFab.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_save_alt)
            binding.updateFab.text = getString(R.string.action_install)
        } else {
            setFabClickListener(updateType = updateTypeAndFileName, isUpdateVerified = false)
            binding.updateFab.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_get_app)
            binding.updateFab.text = getString(R.string.action_download)
        }
    }

    private fun closeNotification() {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager!!.cancel(Constants.NOTIFICATION_ID)
    }

    // Helper method which returns a list holding updateData at index 0 (can be either org.nano.updater.model.Kernel or org.nano.updater.model.Updater),
    // MD5 checksum at index 1
    // fileName at index 2
    private fun getUpdateTypeAndFileName(
        updateData: NanoUpdate,
        isKernelUpdate: Boolean
    ): ArrayList<Any> {
        return if (isKernelUpdate)
            ArrayList<Any>().apply {
                add(updateData.kernel)
                add(updateData.kernel.kernelMD5)
                add(
                    updateData.kernel.kernelLink.substring(
                        updateData.kernel.kernelLink.lastIndexOf('/') + 1,
                        updateData.kernel.kernelLink.length
                    )
                )
            }
        else
            ArrayList<Any>().apply {
                add(updateData.updater)
                add(updateData.updater.updaterMD5)
                add(
                    updateData.updater.updaterLink.substring(
                        updateData.updater.updaterLink.lastIndexOf('/') + 1,
                        updateData.updater.updaterLink.length
                    )
                )
            }
    }

    private fun prepareTransitions() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            // Scope the transition to a view in the hierarchy so we know it will be added under
            // the bottom app bar
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
            interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        }
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.home_root
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
            interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        }
    }

    private fun startTransitions() {
        binding.executePendingBindings()
        startPostponedEnterTransition()
    }
}
