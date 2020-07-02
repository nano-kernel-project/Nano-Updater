package org.nano.updater.util

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nano.updater.R
import org.nano.updater.ui.MainActivity

@BindingAdapter("android:drawableStart")
fun setDrawableStart(textView: TextView, @DrawableRes oldIcon: Int, @DrawableRes newIcon: Int) {
    if ((oldIcon == newIcon) || newIcon == 0)
        return
    textView.setCompoundDrawablesWithIntrinsicBounds(newIcon, 0, 0, 0)
}

@BindingAdapter("src")
fun setDrawable(imageView: ImageView, @DrawableRes icon: Int) {
    imageView.setImageResource(icon)
}

@BindingAdapter(value = ["statusColor", "position"])
fun setStatusColor(textView: TextView, isUpdateAvailable: Boolean?, position: Int) {
    if (position != 3) {
        if (isUpdateAvailable != null)
            if (isUpdateAvailable)
                textView.setTextColor(
                    ContextCompat.getColor(
                        textView.context,
                        R.color.nano_pink_300
                    )
                )
            else
                textView.setTextColor(
                    ContextCompat.getColor(
                        textView.context,
                        R.color.nano_green_300
                    )
                )
        else
            textView.setTextColor(
                ContextCompat.getColor(
                    textView.context,
                    R.color.nano_orange_300
                )
            )
    }
}

@BindingAdapter("toggle")
fun toggleSwitch(imageView: ImageView, check: Boolean) {
    if (check) {
        imageView.setImageResource(R.drawable.avd_toggle_off_on)
        (imageView.drawable as AnimatedVectorDrawable).start()
    } else {
        imageView.setImageResource(R.drawable.avd_toggle_on_off)
        (imageView.drawable as AnimatedVectorDrawable).start()
    }
}

@BindingAdapter("showOrHide")
fun hideOrShowView(view: View, hideView: Boolean) {
    val sharedPrefUtils = SharedPrefUtils(view.context)
    if (sharedPrefUtils.isDisplayLogsToggled()) {
        if (view is RecyclerView)
            view.visibility = View.VISIBLE

        if (view.id == R.id.flash_info || view.id == R.id.flash_illustration)
            view.visibility = if (hideView)
                View.GONE
            else {
                if ((view.context as MainActivity).findViewById<RecyclerView>(R.id.flash_log_recycler_view).adapter!!.itemCount != 0)
                    View.GONE
                else
                    View.VISIBLE
            }
    } else {
        if (view is RecyclerView)
            view.visibility = View.GONE

        if (view.id == R.id.flash_illustration || view.id == R.id.flash_info) {
            view.visibility = View.VISIBLE
            if (hideView) {
                if (view.id == R.id.flash_illustration)
                    ((view as ImageView).drawable as AnimatedVectorDrawable).start()
                else
                    (view as TextView).text = view.context.getString(R.string.status_flashing)
            } else {
                if (view.id == R.id.flash_illustration) {
                    ((view as ImageView).drawable as AnimatedVectorDrawable).reset()
                }
            }
        }
    }
}

@BindingAdapter("layoutFullscreen")
fun View.bindLayoutFullscreen(previousFullscreen: Boolean, fullscreen: Boolean) {
    if (previousFullscreen != fullscreen && fullscreen) {
        systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}

@BindingAdapter(
    "paddingLeftSystemWindowInsets",
    "paddingTopSystemWindowInsets",
    "paddingRightSystemWindowInsets",
    "paddingBottomSystemWindowInsets",
    requireAll = false
)
fun View.applySystemWindowInsetsPadding(
    previousApplyLeft: Boolean,
    previousApplyTop: Boolean,
    previousApplyRight: Boolean,
    previousApplyBottom: Boolean,
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    if (previousApplyLeft == applyLeft &&
        previousApplyTop == applyTop &&
        previousApplyRight == applyRight &&
        previousApplyBottom == applyBottom
    ) {
        return
    }

    doOnApplyWindowInsets { view, insets, padding, _, _ ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        view.setPadding(
            padding.left + left,
            padding.top + top,
            padding.right + right,
            padding.bottom + bottom
        )
    }
}

@BindingAdapter(
    "marginLeftSystemWindowInsets",
    "marginTopSystemWindowInsets",
    "marginRightSystemWindowInsets",
    "marginBottomSystemWindowInsets",
    requireAll = false
)
fun View.applySystemWindowInsetsMargin(
    previousApplyLeft: Boolean,
    previousApplyTop: Boolean,
    previousApplyRight: Boolean,
    previousApplyBottom: Boolean,
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    if (previousApplyLeft == applyLeft &&
        previousApplyTop == applyTop &&
        previousApplyRight == applyRight &&
        previousApplyBottom == applyBottom
    ) {
        return
    }

    doOnApplyWindowInsets { view, insets, _, margin, _ ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = margin.left + left
            topMargin = margin.top + top
            rightMargin = margin.right + right
            bottomMargin = margin.bottom + bottom
        }
    }
}

fun View.doOnApplyWindowInsets(
    block: (View, WindowInsets, InitialPadding, InitialMargin, Int) -> Unit
) {
    // Create a snapshot of the view's padding & margin states
    val initialPadding = recordInitialPaddingForView(this)
    val initialMargin = recordInitialMarginForView(this)
    val initialHeight = recordInitialHeightForView(this)
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding & margin states
    setOnApplyWindowInsetsListener { v, insets ->
        block(v, insets, initialPadding, initialMargin, initialHeight)
        // Always return the insets, so that children can also use them
        insets
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

class InitialPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

class InitialMargin(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)

private fun recordInitialMarginForView(view: View): InitialMargin {
    val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
        ?: throw IllegalArgumentException("Invalid view layout params")
    return InitialMargin(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
}

private fun recordInitialHeightForView(view: View): Int {
    return view.layoutParams.height
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}
