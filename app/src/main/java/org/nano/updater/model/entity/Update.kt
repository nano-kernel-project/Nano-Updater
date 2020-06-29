package org.nano.updater.model.entity

import androidx.room.Embedded
import androidx.room.Entity
import org.nano.updater.model.Kernel
import org.nano.updater.model.NanoUpdate
import org.nano.updater.model.Updater
import org.nano.updater.util.Converters

@Entity(primaryKeys = ["kernelLink", "updaterLink"])
data class Update(
    @Embedded
    val kernel: Kernel,
    @Embedded
    val updater: Updater,
    val kernelChangelog: String,
    val updaterChangelog: String,
    val lastChecked: Long
)

fun Update.asDomainModel(): NanoUpdate? {
    return NanoUpdate(
        kernel, updater, Converters.fromString(kernelChangelog), Converters.fromString(updaterChangelog)
    )
}