package org.nano.updater.ui.flash

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.transition.Slide
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentFlashBinding
import org.nano.updater.model.NanoUpdate
import org.nano.updater.service.FlashService
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.ui.update.UpdateViewModel
import org.nano.updater.util.Constants
import org.nano.updater.util.FlashUtils
import java.io.File
import javax.inject.Inject


class FlashFragment : Fragment() {
    private lateinit var binding: FragmentFlashBinding

    @Inject
    lateinit var updateViewModel: UpdateViewModel

    @Inject
    lateinit var homeViewModel: HomeViewModel

    @Inject
    lateinit var flashUtils: FlashUtils

    @Inject
    lateinit var flashService: FlashService

    private val liveLog by lazy {
        ArrayList<String>()
    }

    @Inject
    lateinit var consoleAdapter: ConsoleAdapter

    private val disableBackAction: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                // Do nothing
                // Don't allow user to close the FlashFragment when flashing
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, disableBackAction)
        postponeEnterTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFlashBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.flashFab.setOnClickListener {
            flashKernel(
                homeViewModel.getUpdateData().value!!
            )
        }

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = updateViewModel
        }

        // Init flashing complete
        updateViewModel.setIsFlashingComplete(ArrayList<Any>().apply {
            add(false)
            add(getString(R.string.flash_init_info))
        })

        updateViewModel.getIsFlashingComplete().observe(viewLifecycleOwner, Observer {
            disableBackAction.isEnabled = false
            updateViewModel.setHideViews(false)

            if (it[0] == false && (it[1] as String).startsWith("No root"))
                binding.flashIllustration.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.nano_pink_300
                    )
                )

            binding.flashFab.apply {
                isEnabled = true
                if (it[0] == true && updateViewModel.getIsFlashServiceRunning().value!!) {
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_share)
                    text = getString(R.string.action_share)
                    setOnClickListener { shareLog() }
                } else {
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_flash_on)
                    text = getString(R.string.action_flash)
                    setOnClickListener { flashKernel(updateData = homeViewModel.getUpdateData().value!!) }
                }
            }
            if ((it[1] as String).contains("Unsupported", true)) {
                binding.flashIllustration.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.nano_pink_300
                    )
                )
                updateViewModel.setFlashEndStatus(getString(R.string.flashing_failed_unsupported_device))
            } else
                updateViewModel.setFlashEndStatus(it[1] as String)
        })

        updateViewModel.setHideViews(false)

        consoleAdapter.setLiveFlashLog(liveLog)
        binding.flashLogRecyclerView.adapter = consoleAdapter

        startTransitions()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateViewModel.setIsFlashingComplete(ArrayList<Any>().apply {
            add(false)
            add("")
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }

    private fun flashKernel(updateData: NanoUpdate) {
        disableBackAction.isEnabled = true
        updateViewModel.setHideViews(true)
        binding.flashFab.isEnabled = false

        // Start FlashService
        Intent(requireContext(), FlashService::class.java).also {
            it.putExtra(
                Constants.KEY_ABSOLUTE_PATH,
                requireContext().getExternalFilesDir("kernel")!!.path + "/" + updateData.kernel.kernelLink.substring(
                    updateData.kernel.kernelLink.lastIndexOf('/') + 1,
                    updateData.kernel.kernelLink.length
                )
            )
            requireContext().startForegroundService(it)
        }
    }

    private fun shareLog() {
        val logsDir = File(requireContext().getExternalFilesDir("logs")!!.path)
        val latestLogFile: File? = logsDir.listFiles()?.let { it[it.size - 1] }
        val fileUri = FileProvider.getUriForFile(
            requireContext(),
            "org.nano.updater.fileprovider",
            latestLogFile!!
        )
        val logShareIntent = Intent().apply {
            type = "text/plain"
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(logShareIntent, latestLogFile.name))
    }

    private fun startTransitions() {
        binding.executePendingBindings()
        enterTransition = MaterialContainerTransform().apply {
            startView =
                requireActivity().findViewById<ExtendedFloatingActionButton>(R.id.update_fab)
            endView = binding.flashCard
            setPathMotion(MaterialArcMotion())
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
            interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        }
        returnTransition = Slide().apply {
            duration = resources.getInteger(R.integer.nano_motion_duration_medium).toLong()
            interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
        }
        startPostponedEnterTransition()
    }
}
