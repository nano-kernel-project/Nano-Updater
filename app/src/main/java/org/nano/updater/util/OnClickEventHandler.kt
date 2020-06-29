package org.nano.updater.util

import android.content.Intent
import android.net.Uri
import android.view.View
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnClickEventHandler @Inject constructor() {

    fun onSupportClick(view: View, supportUrl: String) {
        view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl)))
    }
}
