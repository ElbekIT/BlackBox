package uz.elbekweb.blackbox.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences

/**
 * Notification Shield Servisi.
 * Qulflangan ilovalardan (Telegram, WhatsApp, SMS va b.) kelgan bildirishnomalarni
 * tutib qoladi va ularning matni begona ko'zlar oldida ko'rinishining oldini oladi.
 */
class NotificationShieldService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return

        // Agar ushbu ilova BlackBox tomonidan qulflangan bo'lsa
        if (BlackBoxPreferences.isPackageLocked(pkg)) {
            // Bildirishnomani ekrandan yashirish (bekor qilish)
            try {
                cancelNotification(sbn.key)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
