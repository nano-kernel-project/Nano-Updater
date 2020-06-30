package org.nano.updater.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentSupportBinding
import org.nano.updater.util.OnClickEventHandler
import javax.inject.Inject

class SupportFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentSupportBinding

    @Inject
    lateinit var homeViewModel: HomeViewModel

    private val onClickEventHandler by lazy {
        OnClickEventHandler()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getTheme(): Int {
        return R.style.Widget_Nano_BottomSheet
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            homeViewModel = this@SupportFragment.homeViewModel
            onClickEventHandler = this@SupportFragment.onClickEventHandler
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as NanoApplication).appComponent.inject(this)
    }
}