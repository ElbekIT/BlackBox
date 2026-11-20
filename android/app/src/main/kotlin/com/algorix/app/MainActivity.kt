package com.algorix.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import uz.elbekweb.blackbox.core.crypto.AesEncryptionEngine
import uz.elbekweb.blackbox.core.crypto.ExifScrubber
import uz.elbekweb.blackbox.core.crypto.FileShredder
import uz.elbekweb.blackbox.core.permission.PermissionWizardManager
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences
import java.io.File

/**
 * FlutterActivity v2 Embedding asosi.
 * Flutter interfeysi va BlackBox Native Kiber-Xavfsizlik tizimi o'rtasida
 * MethodChannel orqali 0 ms aloqani ta'minlaydi.
 */
class MainActivity : FlutterActivity() {

    private val CHANNEL = "uz.elbekweb.blackbox/security"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                // 1. Ruxsatlarni tekshirish
                "checkPermissions" -> {
                    val manager = PermissionWizardManager(this)
                    val list = manager.checkAllPermissions().map {
                        mapOf(
                            "type" to it.type.name,
                            "title" to it.type.title,
                            "description" to it.type.description,
                            "isCritical" to it.type.isCritical,
                            "isGranted" to it.isGranted
                        )
                    }
                    result.success(list)
                }

                // 2. Ruxsat so'rash
                "requestPermission" -> {
                    val typeName = call.argument<String>("type")
                    if (typeName != null) {
                        try {
                            val type = PermissionWizardManager.PermissionType.valueOf(typeName)
                            val manager = PermissionWizardManager(this)
                            manager.requestPermission(this, type)
                            result.success(true)
                        } catch (e: Exception) {
                            result.error("ERROR", e.localizedMessage, null)
                        }
                    } else {
                        result.error("INVALID_TYPE", "Ruxsat turi ko'rsatilmadi", null)
                    }
                }

                // 3. Android 13+ Cheklangan Sozlamani Yechish: App Info (Ilova ma'lumotlari) oynasiga o'tish
                "openAppDetailsSettings" -> {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("APP_SETTINGS_ERROR", e.localizedMessage, null)
                    }
                }

                // 4. Maxsus Imkoniyatlar (Accessibility) oynasini bevosita ochish
                "openAccessibilitySettings" -> {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("ACCESSIBILITY_ERROR", e.localizedMessage, null)
                    }
                }

                // 5. O'rnatilgan ilovalar ro'yxatini olish (0-ms AppLock uchun)
                "getInstalledApps" -> {
                    try {
                        val pm = packageManager
                        val intent = Intent(Intent.ACTION_MAIN, null).apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                        }
                        val resolveInfoList = pm.queryIntentActivities(intent, 0)
                        val appList = mutableListOf<Map<String, Any>>()
                        val seen = mutableSetOf<String>()

                        for (resolveInfo in resolveInfoList) {
                            val pkg = resolveInfo.activityInfo.packageName
                            if (pkg == packageName || seen.contains(pkg)) continue
                            seen.add(pkg)
                            val name = resolveInfo.loadLabel(pm).toString()
                            val isLocked = BlackBoxPreferences.isPackageLocked(pkg)
                            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                            appList.add(
                                mapOf(
                                    "packageName" to pkg,
                                    "appName" to name,
                                    "isLocked" to isLocked,
                                    "isSystem" to isSystem
                                )
                            )
                        }
                        // Saralash: Qulflanganlar birinchi, so'ng alifbo tartibida
                        appList.sortWith(
                            compareByDescending<Map<String, Any>> { it["isLocked"] as Boolean }
                                .thenBy { (it["appName"] as String).lowercase() }
                        )
                        result.success(appList)
                    } catch (e: Exception) {
                        result.error("APP_LIST_ERROR", e.localizedMessage, null)
                    }
                }

                // 6. Ilovani qulflash / qulfdan chiqarish
                "setPackageLocked" -> {
                    val pkg = call.argument<String>("packageName") ?: ""
                    val locked = call.argument<Boolean>("locked") ?: false
                    BlackBoxPreferences.setPackageLocked(pkg, locked)
                    result.success(true)
                }

                // 7. Ilova qulflanganligini tekshirish
                "isPackageLocked" -> {
                    val pkg = call.argument<String>("packageName") ?: ""
                    result.success(BlackBoxPreferences.isPackageLocked(pkg))
                }

                // 8. File Shredder (Faylni yo'q qilish)
                "shredFile" -> {
                    val path = call.argument<String>("filePath") ?: ""
                    val file = File(path)
                    val success = FileShredder.shredFile(file)
                    result.success(success)
                }

                // 9. EXIF Scrubber (GPS va metadatalarni tozalash)
                "scrubExif" -> {
                    val path = call.argument<String>("filePath") ?: ""
                    val file = File(path)
                    val success = ExifScrubber.scrubImage(file)
                    result.success(success)
                }

                // 10. Kalkulyator niqobini holatini tekshirish
                "isDisguiseEnabled" -> {
                    result.success(BlackBoxPreferences.isCalculatorDisguiseEnabled)
                }

                // 11. Kalkulyator niqobini yoqish/o'chirish
                "toggleCalculatorDisguise" -> {
                    val enabled = BlackBoxPreferences.isCalculatorDisguiseEnabled
                    BlackBoxPreferences.isCalculatorDisguiseEnabled = !enabled
                    result.success(!enabled)
                }

                // 12. Asosiy PIN-kodni olish / o'rnatish
                "getMasterPin" -> {
                    result.success(BlackBoxPreferences.masterPin)
                }
                "setMasterPin" -> {
                    val newPin = call.argument<String>("pin") ?: "7777"
                    BlackBoxPreferences.masterPin = newPin
                    result.success(true)
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
