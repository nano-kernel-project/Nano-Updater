package org.nano.updater.util

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import org.nano.updater.R
import org.nano.updater.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnackBarUtils @Inject constructor() {

    fun showSnackBarWithAction(context: Context, message: String, action: (view: View) -> Unit) {
        Snackbar.make(
            (context as MainActivity).binding.coordinator,
            message,
            Snackbar.LENGTH_LONG
        ).apply {
            anchorView = context.binding.fab
            setAction(context.getString(R.string.action_retry), action)
            setActionTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.nano_green_800
                )
            )
            show()
        }
    }

    fun showSnackBar(context: Context, message: String) {
        Snackbar.make(
            (context as MainActivity).binding.coordinator,
            message,
            Snackbar.LENGTH_LONG
        ).apply {
            anchorView = context.binding.fab
            show()
        }
    }
}