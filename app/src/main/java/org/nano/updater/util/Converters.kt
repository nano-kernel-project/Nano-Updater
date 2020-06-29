package org.nano.updater.util


object Converters {

    fun fromString(value: String, forceRefresh: Boolean = false): List<String> {
        val changelog = value.split("\n").toMutableList()
        if (forceRefresh) {
            changelog.removeAt(0)
            changelog.removeAt(0)
            changelog.removeAt(0)
            changelog.removeAt(changelog.size - 1)
            changelog.removeAt(changelog.size - 1)
        }
        return changelog
    }

    fun fromArrayList(list: List<String>): String? {
        val changelogBuilder = StringBuilder()
        for (i in list.indices)
            if (i != list.size - 1) {
                if (list[i].isNotEmpty())
                    changelogBuilder.append(list[i]).append("\n")
            } else
                changelogBuilder.append(list[i])
        return changelogBuilder.toString()
    }
}