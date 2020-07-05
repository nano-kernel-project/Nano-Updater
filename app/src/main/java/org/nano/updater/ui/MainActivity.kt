package org.nano.updater.ui

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.google.android.material.bottomappbar.BottomAppBar
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.ActivityMainBinding
import org.nano.updater.model.NavigationModelItem
import org.nano.updater.ui.home.HomeAdapter
import org.nano.updater.ui.home.HomeFragmentDirections
import org.nano.updater.ui.home.HomeViewModel
import org.nano.updater.ui.nav.*
import org.nano.updater.util.FileUtils
import org.nano.updater.util.createMaterialElevationScale
import javax.inject.Inject

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener,
    BottomNavigationAdapter.BottomNavigationAdapterListener,
    HomeAdapter.HomeAdapterListener {
    @Inject
    lateinit var mainViewModel: MainViewModel

    @Inject
    lateinit var homeViewModel: HomeViewModel

    lateinit var binding: ActivityMainBinding
    private val bottomNavDrawer: BottomNavDrawerFragment by lazy(LazyThreadSafetyMode.NONE) {
        supportFragmentManager.findFragmentById(R.id.bottom_nav_drawer) as BottomNavDrawerFragment
    }

    private val currentNavigationFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager
            ?.fragments
            ?.first()

    private val navigationMap = mapOf(
        0 to R.id.homeFragment,
        1 to R.id.settingsFragment,
        2 to R.id.bugReportFragment,
        3 to R.id.aboutFragment
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Nano_Dark_NoActionBar)
        (application as NanoApplication).appComponent.inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        init()
    }

    private fun init() {
        findNavController(R.id.nav_host_fragment).addOnDestinationChangedListener(this@MainActivity)

        binding.apply {
            navMenuItem = NavigationModelItem.NavMenuItem()
            fab.apply {
                setShowMotionSpecResource(R.animator.fab_show)
                setHideMotionSpecResource(R.animator.fab_hide)
            }
        }

        binding.bottomAppBarContentContainer.setOnClickListener {
            bottomNavDrawer.toggle()
        }

        bottomNavDrawer.apply {
            addOnSlideAction(HalfClockwiseRotateSlideAction(binding.bottomAppBarChevron))
            addOnStateChangedAction(ShowHideFabStateAction(binding.fab, mainViewModel))
            addOnSlideAction(AlphaSlideAction(binding.bottomAppBarTitle, true))
            addNavigationListener(this@MainActivity)
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        mainViewModel.setCurrentDestination(findNavController(R.id.nav_host_fragment).currentDestination!!.id)
        when (destination.id) {
            R.id.homeFragment,
            R.id.bugReportFragment -> setupBottomAppBarForHomeAndReport(destination.id)

            R.id.aboutFragment,
            R.id.settingsFragment -> setupBottomAppBarForAboutAndSettings(destination.id)

            else -> setupBottomAppBarForUpdateAndFlash(destination.id)
        }
    }

    override fun onNavigationItemClicked(modelItem: NavigationModelItem.NavMenuItem) {
        currentNavigationFragment?.exitTransition = createMaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
        }

        // We need ids of only Home, Settings, Report, About screens
        if (modelItem.id <= 4)
            navigateToDestination(modelItem.id)
    }

    override fun onUpdateCardClicked(cardView: View, position: Int) {
        if (homeViewModel.getUpdateData().value == null)
            return

        val extras = FragmentNavigatorExtras(
            cardView to cardView.transitionName
        )

        val isUpdateVerified = FileUtils.getIsUpdateVerified(this, homeViewModel, position)
        if (isUpdateVerified)
            binding.fab.setImageResource(R.drawable.asl_sync_save)
        else
            binding.fab.setImageResource(R.drawable.asl_sync_get)


        currentNavigationFragment?.exitTransition = createMaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
        }

        findNavController(R.id.nav_host_fragment)
            .navigate(
                HomeFragmentDirections.actionHomeFragmentToUpdateFragment(position, isUpdateVerified),
                extras
            )
    }

    override fun onInfoCardClicked(position: Int) {
        if (position == 3)
            findNavController(R.id.nav_host_fragment).navigate(R.id.supportFragment)
    }

    private fun navigateToDestination(destinationId: Int) {
        Handler().postDelayed({
            findNavController(R.id.nav_host_fragment).navigate(
                navigationMap.getValue(destinationId),
                null,
                NavOptions.Builder().apply {
                    setPopUpTo(
                        if (destinationId != 0) navigationMap.getValue(destinationId) else -1,
                        true
                    )
                }.build()
            )
        }, resources.getInteger(R.integer.nano_motion_duration_small).toLong())
    }

    private fun updateTitle(@StringRes titleRes: Int) {
        if (getString(titleRes) != binding.bottomAppBarTitle.text)
            binding.bottomAppBarTitle.text = getString(titleRes)
    }

    private fun setupBottomAppBarForHomeAndReport(destinationId: Int) {
        binding.bottomAppBarTitle.visibility = View.VISIBLE
        binding.bottomAppBar.run {
            visibility = View.VISIBLE
            performShow()
        }
        Handler().postDelayed({
            binding.fab.show()
        }, resources.getInteger(R.integer.nano_motion_duration_large).toLong())

        if (destinationId == R.id.homeFragment) {
            binding.bottomAppBar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
            binding.fab.setImageResource(R.drawable.asl_sync_get)
            updateTitle(R.string.action_home)
            NavigationModel.setNavigationMenuItemChecked(0)
        } else {
            binding.bottomAppBar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
            binding.fab.setImageResource(R.drawable.ic_send)
            updateTitle(R.string.action_report)
            NavigationModel.setNavigationMenuItemChecked(2)
        }
        binding.fab.setImageState(intArrayOf(-android.R.attr.state_activated), true)
    }

    private fun setupBottomAppBarForAboutAndSettings(destinationId: Int) {
        binding.bottomAppBarTitle.visibility = View.VISIBLE
        binding.bottomAppBar.run {
            visibility = View.VISIBLE
            performShow()
        }
        if (destinationId == R.id.aboutFragment) {
            updateTitle(R.string.action_about)
            NavigationModel.setNavigationMenuItemChecked(3)
        } else {
            updateTitle(R.string.action_settings)
            NavigationModel.setNavigationMenuItemChecked(1)
        }
    }

    private fun setupBottomAppBarForUpdateAndFlash(destinationId: Int) {
        binding.bottomAppBarTitle.visibility = View.INVISIBLE
        binding.bottomAppBar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER

        // Set BottomAppBar title and checked item to Home
        updateTitle(R.string.action_home)
        NavigationModel.setNavigationMenuItemChecked(0)

        if (destinationId == R.id.flashFragment)
            binding.apply {
                bottomAppBar.performHide()
                fab.hide()
            }
        else
            binding.apply {
                bottomAppBar.performShow()
                fab.show()
            }
        binding.fab.setImageState(intArrayOf(android.R.attr.state_activated), true)
    }
}
