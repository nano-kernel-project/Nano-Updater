package org.nano.updater.ui.report

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.nano.updater.NanoApplication
import org.nano.updater.databinding.FragmentGenerateReportBinding
import javax.inject.Inject

class ReportCollectionFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentGenerateReportBinding

    @Inject
    lateinit var reportViewModel: ReportViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = FragmentGenerateReportBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        reportViewModel.getIsLogReported().observe(viewLifecycleOwner, Observer {
            if (it != null)
                dismiss()
        })

        reportViewModel.getReportStatusMessage().observe(viewLifecycleOwner, Observer {
            binding.reportStatus.text = it
        })
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            reportViewModel = reportViewModel
        }
        (binding.reportImage.drawable as AnimatedVectorDrawable).start()
    }

    override fun onDestroy() {
        super.onDestroy()
        reportViewModel.setIsLogReported(null)
        reportViewModel.setReportStatusMessage("")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }
}