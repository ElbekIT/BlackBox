package uz.elbekweb.blackbox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import uz.elbekweb.blackbox.core.crypto.KeyStoreManager
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences

/**
 * BlackBox Application Entry Point.
 * 100% Oflayn kiber-xavfsizlik va shaxsiy maxfiylik ilovasi.
 * Muallif: Elbek Web (@elbekdesign_va_webdasturchi_uz)
 */
class BlackBoxApp : Application() {

    companion object {
        const val CHANNEL_SECURITY_ID = "blackbox_security_alerts"
        const val CHANNEL_SERVICE_ID = "blackbox_guard_service"
        lateinit var instance: BlackBoxApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Android Keystore Master Key initsializatsiyasi
        KeyStoreManager.initMasterKey()

        // 2. Shifrlangan sozlamalarni yuklash
        BlackBoxPreferences.init(this)

        // 3. Xavfsizlik bildirishnoma kanallarini yaratish
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val guardChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "BlackBox Xavfsizlik Qo'riqchisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BlackBox tizim darajasidagi faol himoya servislari"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_SECURITY_ID,
                "BlackBox Kiber Ogohlantirishlar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "O'g'rilik, soxta urinishlar va xavfsizlik hodisalari signallari"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(guardChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
