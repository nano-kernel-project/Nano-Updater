package org.nano.updater.ui.report

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialFadeThrough
import org.nano.updater.NanoApplication
import org.nano.updater.R
import org.nano.updater.databinding.FragmentReportBinding
import org.nano.updater.repository.ReportRepository
import org.nano.updater.ui.MainActivity
import org.nano.updater.ui.MainViewModel
import org.nano.updater.util.SnackBarUtils
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 */
class ReportFragment : Fragment() {
    private lateinit var bugReportBinding: FragmentReportBinding

    @Inject
    lateinit var mainViewModel: MainViewModel

    @Inject
    lateinit var reportRepository: ReportRepository

    @Inject
    lateinit var reportViewModel: ReportViewModel

    @Inject
    lateinit var snackBarUtils: SnackBarUtils

    private val scrollChangeListener by lazy {
        View.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (oldScrollY > scrollY)
                (requireActivity() as MainActivity).binding.bottomAppBar.performShow()
            else
                (requireActivity() as MainActivity).binding.bottomAppBar.performHide()
        }
    }

    private val disableBackAction = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Do nothing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, disableBackAction)
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.nano_motion_duration_large).toLong()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bugReportBinding = FragmentReportBinding.inflate(inflater)
        return bugReportBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity() as MainActivity
        bugReportBinding.bugReportScrollView.setOnScrollChangeListener(scrollChangeListener)

        activity.binding.fab.setOnClickListener {
            findNavController().navigate(R.id.reportCollectionFragment)
            generateLogs()
        }

        reportViewModel.getIsLogReported().observe(viewLifecycleOwner, Observer {
            disableBackAction.isEnabled = false
            if (it == null)
                return@Observer
            if (it) {
                Handler().postDelayed({
                    snackBarUtils.showSnackBar(
                        requireContext(),
                        getString(R.string.all_reports_sent)
                    )
                }, 500)
            } else {
                // An error occurred
                // Can be no root access, network error or some IO related error
                // Simply navigate to ReportFragment
                Handler().postDelayed({
                    snackBarUtils.showSnackBar(requireContext(), getString(R.string.report_an_error_occurred))
                }, 500)
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as NanoApplication).appComponent.inject(this)
    }

    private fun generateLogs() {
        if (!bugReportBinding.bugReportChipUpdater.isChecked && !bugReportBinding.bugReportChipKernel.isChecked) {
            snackBarUtils.showSnackBar(requireContext(), getString(R.string.choose_at_least_one_log_type))
            return
        }

        disableBackAction.isEnabled = true
        reportViewModel.generateLogs(
            bugReportBinding.bugReportChipUpdater.isChecked,
            bugReportBinding.bugReportChipKernel.isChecked
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reportViewModel.setIsLogReported(null)
        reportViewModel.setReportStatusMessage("")
    }
}
