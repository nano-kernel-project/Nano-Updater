package org.nano.updater.ui.home

import android.animation.ObjectAnimator
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentHomeBinding
import org.nano.updater.ui.MainActivity
import org.nano.updater.ui.update.UpdateViewModel
import org.nano.updater.util.Constants
import org.nano.updater.util.SnackBarUtils
import org.nano.updater.util.createMaterialElevationScale
import javax.inject.Inject

class HomeFragment : Fragment() {
    private lateinit var homeBinding: FragmentHomeBinding

    @Inject
    lateinit var homeViewModel: HomeViewModel

    @Inject
    lateinit var snackBarUtils: SnackBarUtils

    private val connectivityManager by lazy {
        requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkRequest by lazy {
        NetworkRequest.Builder().build() as NetworkRequest
    }

    private var isNetworkAvailable: Boolean = false

    private val networkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                isNetworkAvailable = false
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isNetworkAvailable = true
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                isNetworkAvailable = false
            }
        }
    }

    private val homeAdapter by lazy {
        HomeAdapter(requireActivity() as MainActivity)
    }

    private val animator by lazy {
        ObjectAnimator().apply {
            setPropertyName("rotation")
            setFloatValues(0f, 360f)
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
            duration = 1000
        }
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
        homeBinding = FragmentHomeBinding.inflate(inflater)
        return homeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        (requireActivity() as MainActivity).binding.apply {
            fab.setOnClickListener {
                refreshData()
            }
            bottomAppBar.hideOnScroll = true
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Observe for the official device status
        homeViewModel.getIsUnsupportedDevice().observe(viewLifecycleOwner, Observer {
            animator.end()
            if (it)
                Snackbar.make(
                    (requireActivity() as MainActivity).binding.coordinator,
                    getString(R.string.unsupported_device, Build.DEVICE),
                    Snackbar.LENGTH_LONG
                ).apply {
                    anchorView = (requireActivity() as MainActivity).binding.fab
                    show()
                }
        })

        // Observe for updateData changes
        homeViewModel.getUpdateData().observe(viewLifecycleOwner, Observer {
            animator.end()

            if (it == null) {
                snackBarUtils.showSnackBarWithAction(
                    requireContext(),
                    getString(R.string.cannot_retrieve_data)
                ) { refreshData() }
            }
            return@Observer
        })

        HomeStore.homeItems.observe(viewLifecycleOwner, Observer {
            animator.end()
            homeAdapter.submitList(it?.toMutableList())
        })

        homeBinding.homeRecyclerView.apply {
            setHasFixedSize(true)
            adapter = homeAdapter
        }

        // Observe for download status
        homeViewModel.getDownloadStatus().observe(viewLifecycleOwner, Observer
        {
            if (it == UpdateViewModel.DownloadStatus.CANCELLED)
                closeNotification()
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }

    private fun closeNotification() {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager!!.cancel(Constants.NOTIFICATION_ID)
    }

    private fun refreshData() {
        if (isNetworkAvailable) {
            animator.apply {
                target = (requireActivity() as MainActivity).binding.fab
                start()
            }
            homeViewModel.loadUpdateData(true)
        } else
            snackBarUtils.showSnackBarWithAction(
                requireContext(),
                getString(R.string.no_network_connection)
            ) { refreshData() }
    }

    override fun onStop() {
        super.onStop()
        animator.end()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
