package uz.elbekweb.blackbox.service.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * BlackBox Device Admin Qabul qiluvchisi.
 * Ilovani sozlamalardan o'chirib yuborish (Uninstall) urinishlarini to'sadi
 * va xato lockscreen parollari kiritilganda datchik sifatida ishlaydi.
 */
class BlackBoxDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "BlackBox: Qurilma Administratori faollashtirildi!", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "DIQQAT! BlackBox administrator huquqini o'chirish barcha shaxsiy seyf fayllarini va ilovalarni himoyasiz qoldiradi!"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "BlackBox: Administrator himoyasi o'chirildi.", Toast.LENGTH_SHORT).show()
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        // Lockscreen paroli xato terilganda intruder datchigi
    }
}
