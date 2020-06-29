package org.nano.updater.util

import android.os.Build
import java.util.*

object Constants {
    // Device
    private val DEVICE_CODE_NAME: String = Build.DEVICE
    private val ANDROID_VERSION: String = Build.VERSION.RELEASE
    private val DEVICE_MODEL: String = Build.MODEL
    private val DEVICE_COUNTRY: String = Locale.getDefault().displayCountry

    // Log file paths
    val UPDATER_LOG = "${DEVICE_CODE_NAME}_updater.log"
    val KERNEL_LOG = "${DEVICE_CODE_NAME}_kernel.log"

    // URLs
    const val UPDATER_BASE_API_URL = "https://raw.githubusercontent.com/nano-kernel-project/OTA/"

    // Telegram
    const val TG_MY_ID = "958138341"
    const val TG_CHANNEL_ID = "-1001214984471"
    private const val TG_BOT_BASE_API_URL = "https://api.telegram.org/bot"
    private const val TG_BOT_ACCESS_TOKEN = "1231933275:AAFcn1Er3RTWF7NV2vkpafI_5w31sMr8kdw"
    const val TG_METHOD_SEND_DOCUMENT = "sendDocument"
    val TG_CAPTION =
        "#${DEVICE_CODE_NAME}\nLogs generated for device - *$DEVICE_MODEL*\n\n*Codename:* $DEVICE_CODE_NAME\n*Android:* $ANDROID_VERSION\n*Country:* $DEVICE_COUNTRY"
    const val TG_PARSE_MODE = "markdown"
    const val TG_CHANNEL_API_URL = "$TG_BOT_BASE_API_URL$TG_BOT_ACCESS_TOKEN/"

    // Shared preferences
    const val SHARED_PREF = "org.nano.updater.SHARED_PREF"

    // App settings
    const val KEY_DISPLAY_LOGS_TOGGLE = "org.nano.updater.DISPLAY_LOGS"
    const val KEY_REBOOT_AFTER_FLASH = "org.nano.updater.REBOOT_AFTER_FLASH"
    const val KEY_CHECK_UPDATES_PERIODICALLY = "org.nano.updater.CHECK_UPDATES_PERIODICALLY"
    const val KEY_CLEANUP_DIR = "org.nano.updater.CLEANUP_DIR"

    // Map keys
    const val KEY_IS_KERNEL_UPDATE = "is_kernel_update"

    // Download tag
    const val TAG_DOWNLOAD_REQUEST = "download_request"

    // Worker constants
    const val CHANNEL_ID = "DOWNLOAD_NOTIFICATION"
    const val NOTIFICATION_CHANNEL_NAME = "Download notifications"
    const val NOTIFICATION_CHANNEL_DESC = "Show notifications during downloads"
    const val NOTIFICATION_ID = 1
    const val ACTION_CANCEL = "ACTION_CANCEL"
    const val CANCEL_REQUEST_CODE = 2
    const val KEY_CURRENT_VERSION = "CURRENT_VERSION"
    const val TAG_UPDATE_CHECK_WORKER = "TAG_UPDATE_CHECK_WORKER"
    const val UPDATE_CHECK_WORKER = "UPDATE_CHECK_WORKER"
    const val KEY_IS_UPDATE_AVAILABLE = "IS_UPDATE_AVAILABLE"
    const val UPDATE_NOTIFICATION_CHANNEL = "Update notifications"
    const val UPDATE_NOTIFICATION_CHANNEL_DESC = "Notify about kernel and app updates"
    const val UPDATE_NOTIFICATION_ID = 3
    const val UPDATE_CHANNEL_ID = "UPDATE_NOTIFICATION"
    const val FLASH_CHANNEL_ID = "FLASH_CHANNEL"
    const val FLASH_NOTIFICATION_CHANNEL = "Kernel flash notifications"
    const val FLASH_NOTIFICATION_CHANNEL_DESC = "Notify during a kernel flash"
    const val FLASH_NOTIFICATION_ID = 4

    // Flash
    const val KEY_ABSOLUTE_PATH = "ABSOLUTE_PATH"

    // Commands
    const val CMD_LOGCAT = "logcat -d"
    const val CMD_DMESG = "dmesg"
    const val SHUTDOWN_BROADCAST = "am broadcast android.intent.action.ACTION_SHUTDOWN"
    const val SYNC = "sync"
    const val NORMAL_REBOOT_CMD = "svc power reboot"
}
