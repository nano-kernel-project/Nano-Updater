package org.nano.updater.ui.nav

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentBottomNavDrawerBinding
import org.nano.updater.model.NavigationModelItem
import org.nano.updater.ui.MainViewModel
import javax.inject.Inject

class BottomNavDrawerFragment : Fragment(),
    BottomNavigationAdapter.BottomNavigationAdapterListener {
    private lateinit var binding: FragmentBottomNavDrawerBinding
    private val behavior: BottomSheetBehavior<FrameLayout> by lazy(LazyThreadSafetyMode.NONE) {
        BottomSheetBehavior.from(binding.foregroundContainer)
    }

    @Inject
    lateinit var mainViewModel: MainViewModel

    private val bottomSheetCallback: BottomNavigationDrawerCallback by lazy {
        BottomNavigationDrawerCallback()
    }

    private val navigationListeners = mutableListOf<BottomNavigationAdapter.BottomNavigationAdapterListener>()

    private val navigationMap = mapOf(
        0 to R.id.homeFragment,
        1 to R.id.settingsFragment,
        2 to R.id.bugReportFragment,
        3 to R.id.aboutFragment
    )

    val closeDrawerOnBackPressed: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                // If sheet is open, close it
                close()
            }
        }

    private val foregroundShapeDrawable: MaterialShapeDrawable by lazy(LazyThreadSafetyMode.NONE) {
        val foregroundContext = binding.foregroundContainer.context
        MaterialShapeDrawable(
            foregroundContext,
            null,
            R.attr.bottomSheetStyle,
            0
        ).apply {
            fillColor = ColorStateList.valueOf(foregroundContext.getColor(R.color.nano_grey_900))
            elevation = resources.getDimension(R.dimen.plane_16)
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_NEVER
            initializeElevationOverlay(requireContext())
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setTopLeftCorner(
                    CornerFamily.ROUNDED,
                    foregroundContext.resources.getDimension(R.dimen.nano_large_component_corner_radius)
                )
                .setTopRightCorner(
                    CornerFamily.ROUNDED,
                    foregroundContext.resources.getDimension(R.dimen.nano_large_component_corner_radius)
                )
                .build()
        }
    }

    fun toggle() {
        when (behavior.state) {
            BottomSheetBehavior.STATE_HIDDEN -> open()
            BottomSheetBehavior.STATE_HALF_EXPANDED,
            BottomSheetBehavior.STATE_EXPANDED,
            BottomSheetBehavior.STATE_COLLAPSED -> close()
        }
    }

    private fun open() {
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    fun close() {
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, closeDrawerOnBackPressed)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBottomNavDrawerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            foregroundContainer.background = foregroundShapeDrawable
        }

        binding.scrimView.setOnClickListener { close() }

        bottomSheetCallback.apply {
            // Scrim view transforms
            addOnSlideAction(AlphaSlideAction(binding.scrimView))
            addOnStateChangedAction(VisibilityStateAction(binding.scrimView))

            // If the drawer is open, pressing the system back button should close the drawer.
            addOnStateChangedAction(object : OnStateChangedAction {
                override fun onStateChanged(sheet: View, newState: Int) {
                    closeDrawerOnBackPressed.isEnabled =
                        newState != BottomSheetBehavior.STATE_HIDDEN
                }
            })
        }

        behavior.addBottomSheetCallback(bottomSheetCallback)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        val adapter = BottomNavigationAdapter(
            this@BottomNavDrawerFragment
        )
        binding.navRecyclerView.adapter = adapter

        NavigationModel.navigationList.observe(
            viewLifecycleOwner,
            Observer {
                adapter.submitList(it?.toMutableList())
            })

        NavigationModel.setNavigationMenuItemChecked(0)
    }

    override fun onNavigationItemClicked(modelItem: NavigationModelItem.NavMenuItem) {
        // Do nothing if item is reselected
        if (mainViewModel.getCurrentDestination().value == navigationMap.getValue(modelItem.id))
            return

        close()

        navigationListeners.forEach { it.onNavigationItemClicked(modelItem) }
    }

    fun addNavigationListener(listener: BottomNavigationAdapter.BottomNavigationAdapterListener) {
        navigationListeners.add(listener)
    }

    fun addOnSlideAction(action: OnSlideAction) {
        bottomSheetCallback.addOnSlideAction(action)
    }

    fun addOnStateChangedAction(action: OnStateChangedAction) {
        bottomSheetCallback.addOnStateChangedAction(action)
    }
}