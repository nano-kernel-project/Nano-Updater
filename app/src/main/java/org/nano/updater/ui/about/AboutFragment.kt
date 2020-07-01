package org.nano.updater.ui.about

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentAboutBinding
import org.nano.updater.ui.MainActivity
import org.nano.updater.util.OnClickEventHandler
import org.nano.updater.util.createMaterialElevationScale

class AboutFragment : Fragment() {
    private lateinit var aboutBinding: FragmentAboutBinding
    private val onClickEventHandler by lazy {
        OnClickEventHandler()
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
        aboutBinding = FragmentAboutBinding.inflate(inflater)
        return aboutBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        aboutBinding.apply {
            lifecycleOwner = this@AboutFragment
            onClickEventHandler = this@AboutFragment.onClickEventHandler
            aboutScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (oldScrollY > scrollY)
                    (requireActivity() as MainActivity).binding.bottomAppBar.performShow()
                else
                    (requireActivity() as MainActivity).binding.bottomAppBar.performHide()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }
}
