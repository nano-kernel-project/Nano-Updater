package org.nano.updater.model

import android.content.Context
import androidx.room.Embedded
import com.google.gson.annotations.SerializedName
import org.nano.updater.R
import org.nano.updater.model.entity.Update
import org.nano.updater.util.Converters

data class NanoUpdate(
    @SerializedName("kernel") val kernel: Kernel,
    @SerializedName("updater") val updater: Updater,
    var kernelChangelog: List<String>,
    var updaterChangelog: List<String>
)

data class Kernel(
    @SerializedName("version") val kernelVersion: String,
    @SerializedName("link") val kernelLink: String,
    @SerializedName("changelog") val kernelChangelogLink: String,
    @SerializedName("date") var kernelDate: String,
    @SerializedName("md5") val kernelMD5: String,
    @SerializedName("support") @Embedded val kernelSupport: Support,
    @SerializedName("size") val kernelSize: String
)

data class Support(
    @SerializedName("telegram") val telegram: String = "",
    @SerializedName("xda") val xda: String = ""
)

data class Updater(
    @SerializedName("latest") val updaterVersion: String,
    @SerializedName("link") val updaterLink: String,
    @SerializedName("changelog") val updaterChangelogLink: String,
    @SerializedName("date") var updaterDate: String,
    @SerializedName("md5") val updaterMD5: String,
    @SerializedName("size") val updaterSize: String
)

fun NanoUpdate.asEntityModel(lastChecked: Long): Update {
    return Update(
        kernel = kernel,
        updater = updater,
        kernelChangelog = Converters.fromArrayList(kernelChangelog)!!,
        updaterChangelog = Converters.fromArrayList(updaterChangelog)!!,
        lastChecked = lastChecked
    )
}

fun NanoUpdate.asUpdateCard(
    context: Context,
    lastChecked: String,
    position: Int,
    currentVersion: String?,
    currentBuild: String?,
    isUpdateAvailable: Boolean?
): HomeModelItem.UpdateCard {
    return HomeModelItem.UpdateCard(
        System.nanoTime(),
        position,
        lastChecked,
        if (position == 1) R.drawable.ic_memory else R.drawable.ic_system_update,
        if (position == 1) context.getString(R.string.card_title_kernel) else context.getString(R.string.card_title_updater),
        currentVersion,
        if (position == 1) this.kernel.kernelVersion else this.updater.updaterVersion,
        currentBuild,
        when {
            isUpdateAvailable == null -> context.getString(R.string.unknown)
            isUpdateAvailable -> context.getString(R.string.outdated)
            else -> context.getString(R.string.updated)
        },
        isUpdateAvailable
    )
}

fun asInformationCard(
    context: Context,
    position: Int,
    isUpdateAvailable: Boolean?
): HomeModelItem.InformationCard {
    return HomeModelItem.InformationCard(
        System.nanoTime(),
        position,
        when {
            isUpdateAvailable == null -> context.getString(R.string.status_caption_unknown)
            isUpdateAvailable -> context.getString(R.string.status_caption_outdated)
            else -> context.getString(R.string.status_caption_updated)
        },
        if (position == 3) R.drawable.ic_contact_support
        else {
            when {
                isUpdateAvailable == null -> R.drawable.ic_help
                isUpdateAvailable -> R.drawable.ic_error
                else -> R.drawable.ic_check_circle
            }
        },
        if (position == 0)
            context.getString(R.string.card_title_status)
        else
            context.getString(R.string.card_title_support),
        if (position == 3)
            context.getString(R.string.support_desc)
        else {
            when {
                isUpdateAvailable == null -> context.getString(R.string.status_unknown_desc)
                isUpdateAvailable -> context.getString(R.string.status_outdated_desc)
                else -> context.getString(R.string.status_updated_desc)
            }
        },
        isUpdateAvailable
    )
}
