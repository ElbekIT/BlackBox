package com.algorix.app

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
                            "isGranted" to it.isGranted
                        )
                    }
                    result.success(list)
                }

                // 2. Ruxsat so'rash
                "requestPermission" -> {
                    val typeName = call.argument<String>("type")
                    if (typeName != null) {
                        val type = PermissionWizardManager.PermissionType.valueOf(typeName)
                        val manager = PermissionWizardManager(this)
                        manager.requestPermission(this, type)
                        result.success(true)
                    } else {
                        result.error("INVALID_TYPE", "Ruxsat turi ko'rsatilmadi", null)
                    }
                }

                // 3. Ilovani qulflash / qulfdan chiqarish
                "setPackageLocked" -> {
                    val pkg = call.argument<String>("packageName") ?: ""
                    val locked = call.argument<Boolean>("locked") ?: false
                    BlackBoxPreferences.setPackageLocked(pkg, locked)
                    result.success(true)
                }

                // 4. Ilova qulflanganligini tekshirish
                "isPackageLocked" -> {
                    val pkg = call.argument<String>("packageName") ?: ""
                    result.success(BlackBoxPreferences.isPackageLocked(pkg))
                }

                // 5. File Shredder (Faylni yo'q qilish)
                "shredFile" -> {
                    val path = call.argument<String>("filePath") ?: ""
                    val file = File(path)
                    val success = FileShredder.shredFile(file)
                    result.success(success)
                }

                // 6. EXIF Scrubber (GPS va metadatalarni tozalash)
                "scrubExif" -> {
                    val path = call.argument<String>("filePath") ?: ""
                    val file = File(path)
                    val success = ExifScrubber.scrubImage(file)
                    result.success(success)
                }

                // 7. Kalkulyator niqobini yoqish/o'chirish
                "toggleCalculatorDisguise" -> {
                    val enabled = BlackBoxPreferences.isCalculatorDisguiseEnabled
                    BlackBoxPreferences.isCalculatorDisguiseEnabled = !enabled
                    result.success(!enabled)
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
