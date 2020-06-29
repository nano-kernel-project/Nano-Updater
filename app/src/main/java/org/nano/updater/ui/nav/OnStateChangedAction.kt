/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nano.updater.ui.nav

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.nano.updater.R
import org.nano.updater.ui.MainViewModel

/**
 * An action to be performed when a bottom sheet's state is changed.
 */
interface OnStateChangedAction {
    fun onStateChanged(sheet: View, newState: Int)
}

/**
 * A state change action that handles showing the fab when the sheet is hidden and hiding the fab
 * when the sheet is not hidden as well as checks whether the fab needs to be hidden on specific fragments
 */
class ShowHideFabStateAction(
    private val fab: FloatingActionButton,
    private val mainViewModel: MainViewModel
) : OnStateChangedAction {

    override fun onStateChanged(sheet: View, newState: Int) {
        val destinationId = mainViewModel.getCurrentDestination().value
        if (newState == BottomSheetBehavior.STATE_HIDDEN &&
            destinationId != R.id.aboutFragment &&
            destinationId != R.id.settingsFragment
        )
            fab.show()
        else
            fab.hide()
    }
}

/**
 * A state change action that sets a view's visibility depending on whether the sheet is hidden
 * or not.
 *
 * By default, the view will be hidden when the sheet is hidden and shown when the sheet is shown
 * (not hidden). If [reverse] is set to true, the view will be shown when the sheet is hidden and
 * hidden when the sheet is shown (not hidden).
 */
class VisibilityStateAction(
    private val view: View,
    private val reverse: Boolean = false
) : OnStateChangedAction {
    override fun onStateChanged(sheet: View, newState: Int) {
        val stateHiddenVisibility = if (!reverse) View.GONE else View.VISIBLE
        val stateDefaultVisibility = if (!reverse) View.VISIBLE else View.GONE
        when (newState) {
            BottomSheetBehavior.STATE_HIDDEN -> view.visibility = stateHiddenVisibility
            else -> view.visibility = stateDefaultVisibility
        }
    }
}
