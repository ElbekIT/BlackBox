package uz.elbekweb.blackbox.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences

/**
 * Telefon o'chib-yonganida (Reboot) himoyani avtomatik tiklovchi qabul qiluvchi.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action || "android.intent.action.QUICKBOOT_POWERON" == action) {
            // Sozlamalarni tekshirish va fondagi himoyani ishga tushirish
            BlackBoxPreferences.init(context)
        }
    }
}
