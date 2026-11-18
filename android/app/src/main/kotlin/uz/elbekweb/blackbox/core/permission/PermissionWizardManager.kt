package uz.elbekweb.blackbox.core.permission

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import uz.elbekweb.blackbox.service.lock.AppLockAccessibilityService
import uz.elbekweb.blackbox.service.notification.NotificationShieldService
import uz.elbekweb.blackbox.service.receiver.BlackBoxDeviceAdminReceiver

/**
 * 8 bosqichli Permission Setup Wizard Manager.
 * Tizimning barcha chuqur ruxsatlarini tekshiradi va foydalanuvchini
 * tegishli tizim sozlamalari oynasiga yo'naltiradi.
 */
class PermissionWizardManager(private val context: Context) {

    enum class PermissionType(
        val title: String,
        val description: String,
        val isCritical: Boolean
    ) {
        ACCESSIBILITY(
            "Maxsus Imkoniyatlar (Accessibility)",
            "0-millisekundda qulflangan ilovalarni tutish va apparat tugmalarini boshqarish",
            true
        ),
        OVERLAY(
            "Boshqa ilovalar ustidan chizish (Overlay)",
            "Qulflangan ilova ochilganda parol oynasini darhol ko'rsatish",
            true
        ),
        MANAGE_STORAGE(
            "To'liq Xotira Ruxsati (All Files Access)",
            "AES-256 Seyf, Steganografiya va File Shredder to'g'ri ishlashi uchun",
            true
        ),
        DEVICE_ADMIN(
            "Qurilma Administratori (Device Admin)",
            "Ilovani begona shaxslar tomonidan ruxsatsiz o'chirilishidan himoya qilish",
            true
        ),
        NOTIFICATION_LISTENER(
            "Bildirishnomalar Nazorati (Notification Shield)",
            "Qulflangan messenjerlar va ilovalarning maxfiy xabarlarini berkitish",
            false
        ),
        CAMERA(
            "Kamera va Biometrika",
            "Xato parol terilganda o'g'rini rasmga olish (Intruder Selfie) va barmoq izi",
            false
        ),
        LOCATION_BLUETOOTH(
            "Hududiy va Smart Unlock (Wi-Fi/Bluetooth)",
            "Xavfsiz hududda (masalan, uyda) avtomatik qulfdan chiqarish",
            false
        ),
        BATTERY_OPTIMIZATION(
            "Batareya Tejamkorligidan Chiqarish",
            "Tizim fonda BlackBox himoyasini o'chirib qo'ymasligini ta'minlash",
            true
        )
    }

    data class PermissionStatus(
        val type: PermissionType,
        val isGranted: Boolean
    )

    /**
     * Barcha ruxsatlarning holatini ro'yxat qilib qaytaradi
     */
    fun checkAllPermissions(): List<PermissionStatus> {
        return PermissionType.values().map { type ->
            PermissionStatus(type, isPermissionGranted(type))
        }
    }

    /**
     * Aniq bitta ruxsat berilganligini tekshirish
     */
    fun isPermissionGranted(type: PermissionType): Boolean {
        return when (type) {
            PermissionType.ACCESSIBILITY -> isAccessibilityGranted()
            PermissionType.OVERLAY -> isOverlayGranted()
            PermissionType.MANAGE_STORAGE -> isManageStorageGranted()
            PermissionType.DEVICE_ADMIN -> isDeviceAdminGranted()
            PermissionType.NOTIFICATION_LISTENER -> isNotificationListenerGranted()
            PermissionType.CAMERA -> isCameraGranted()
            PermissionType.LOCATION_BLUETOOTH -> isLocationBluetoothGranted()
            PermissionType.BATTERY_OPTIMIZATION -> isBatteryOptimizationIgnored()
        }
    }

    // 1. Accessibility Service tekshiruvi
    fun isAccessibilityGranted(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedServiceName = ComponentName(context, AppLockAccessibilityService::class.java).flattenToString()
        return enabledServices.any {
            val component = ComponentName(it.resolveInfo.serviceInfo.packageName, it.resolveInfo.serviceInfo.name)
            component.flattenToString().equals(expectedServiceName, ignoreCase = true)
        }
    }

    // 2. SYSTEM_ALERT_WINDOW tekshiruvi
    fun isOverlayGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    // 3. MANAGE_EXTERNAL_STORAGE tekshiruvi
    fun isManageStorageGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 4. BIND_DEVICE_ADMIN tekshiruvi
    fun isDeviceAdminGranted(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val adminComponent = ComponentName(context, BlackBoxDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    // 5. BIND_NOTIFICATION_LISTENER_SERVICE tekshiruvi
    fun isNotificationListenerGranted(): Boolean {
        val packageName = context.packageName
        val listeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return listeners.contains(packageName)
    }

    // 6. CAMERA va BIOMETRIC tekshiruvi
    fun isCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // 7. LOCATION & BLUETOOTH tekshiruvi
    fun isLocationBluetoothGranted(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fineLocation && bluetooth
    }

    // 8. BATTERY OPTIMIZATION tekshiruvi
    fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    // ========================================================
    // RUXSATLARNI SO'RASH VA MOS SOZLAMALAR OYNASIGA O'TISH
    // ========================================================

    fun requestPermission(activity: Activity, type: PermissionType) {
        when (type) {
            PermissionType.ACCESSIBILITY -> {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            }
            PermissionType.OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    activity.startActivity(intent)
                }
            }
            PermissionType.MANAGE_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    activity.startActivity(intent)
                } else {
                    activity.requestPermissions(
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        101
                    )
                }
            }
            PermissionType.DEVICE_ADMIN -> {
                val adminComponent = ComponentName(context, BlackBoxDeviceAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "BlackBox ilovasini ruxsatsiz o'chirishdan himoyalash uchun Administrator huquqini faollashtiring."
                    )
                }
                activity.startActivity(intent)
            }
            PermissionType.NOTIFICATION_LISTENER -> {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            }
            PermissionType.CAMERA -> {
                activity.requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 102)
            }
            PermissionType.LOCATION_BLUETOOTH -> {
                val perms = mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    perms.add(android.Manifest.permission.BLUETOOTH_CONNECT)
                }
                activity.requestPermissions(perms.toTypedArray(), 103)
            }
            PermissionType.BATTERY_OPTIMIZATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    activity.startActivity(intent)
                }
            }
        }
    }
}
