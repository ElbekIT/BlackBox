package uz.elbekweb.blackbox.data.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest

/**
 * Shifrlangan kiber-sozlamalar menejeri (EncryptedSharedPreferences).
 * Barcha PIN-kodlar, qulflangan paketlar va xavfsizlik konfiguratsiyasini saqlaydi.
 */
object BlackBoxPreferences {

    private const val PREFS_FILE = "blackbox_secure_vault_prefs"
    private lateinit var prefs: SharedPreferences

    // Kalitlar
    private const val KEY_MASTER_PIN_HASH = "key_master_pin_hash"
    private const val KEY_DECOY_PIN_HASH = "key_decoy_pin_hash"
    private const val KEY_LOCKED_PACKAGES = "key_locked_packages"
    private const val KEY_FAKE_CRASH_PACKAGES = "key_fake_crash_packages"
    private const val KEY_DISGUISE_CALCULATOR = "key_disguise_calculator"
    private const val KEY_TIME_PIN_ENABLED = "key_time_pin_enabled"
    private const val KEY_MORSE_CODE = "key_morse_code" // masalan "+-++"
    private const val KEY_KEYSTROKE_DYNAMICS = "key_keystroke_dynamics"
    private const val KEY_INTRUDER_SELFIE = "key_intruder_selfie"
    private const val KEY_ANTI_THEFT_CHARGER = "key_anti_theft_charger"
    private const val KEY_ANTI_THEFT_POCKET = "key_anti_theft_pocket"

    fun init(context: Context) {
        prefs = try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("BlackBox_Salt_2026_$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    var masterPin: String
        get() = prefs.getString(KEY_MASTER_PIN_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MASTER_PIN_HASH, hashPin(value)).apply()

    fun checkMasterPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_MASTER_PIN_HASH, "") ?: ""
        if (storedHash.isEmpty()) {
            // Standart dastlabki parol: 1234
            return pin == "1234"
        }
        return hashPin(pin) == storedHash
    }

    var decoyPin: String
        get() = prefs.getString(KEY_DECOY_PIN_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DECOY_PIN_HASH, hashPin(value)).apply()

    fun checkDecoyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_DECOY_PIN_HASH, "") ?: ""
        return storedHash.isNotEmpty() && hashPin(pin) == storedHash
    }

    var isTimePinEnabled: Boolean
        get() = prefs.getBoolean(KEY_TIME_PIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TIME_PIN_ENABLED, value).apply()

    var isCalculatorDisguiseEnabled: Boolean
        get() = prefs.getBoolean(KEY_DISGUISE_CALCULATOR, false)
        set(value) = prefs.edit().putBoolean(KEY_DISGUISE_CALCULATOR, value).apply()

    var isIntruderSelfieEnabled: Boolean
        get() = prefs.getBoolean(KEY_INTRUDER_SELFIE, true)
        set(value) = prefs.edit().putBoolean(KEY_INTRUDER_SELFIE, value).apply()

    var isAntiTheftChargerEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANTI_THEFT_CHARGER, false)
        set(value) = prefs.edit().putBoolean(KEY_ANTI_THEFT_CHARGER, value).apply()

    var isAntiTheftPocketEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANTI_THEFT_POCKET, false)
        set(value) = prefs.edit().putBoolean(KEY_ANTI_THEFT_POCKET, value).apply()

    var morseCodeSequence: String
        get() = prefs.getString(KEY_MORSE_CODE, "+-+") ?: "+-+"
        set(value) = prefs.edit().putString(KEY_MORSE_CODE, value).apply()

    fun getLockedPackages(): MutableSet<String> {
        return prefs.getStringSet(KEY_LOCKED_PACKAGES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    fun isPackageLocked(packageName: String): Boolean {
        val list = getLockedPackages()
        return list.contains(packageName)
    }

    fun setPackageLocked(packageName: String, locked: Boolean) {
        val list = getLockedPackages()
        if (locked) list.add(packageName) else list.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, list).apply()
    }

    fun isFakeCrashPackage(packageName: String): Boolean {
        val list = prefs.getStringSet(KEY_FAKE_CRASH_PACKAGES, emptySet()) ?: emptySet()
        return list.contains(packageName)
    }

    fun setFakeCrashPackage(packageName: String, fakeCrash: Boolean) {
        val list = prefs.getStringSet(KEY_FAKE_CRASH_PACKAGES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (fakeCrash) list.add(packageName) else list.remove(packageName)
        prefs.edit().putStringSet(KEY_FAKE_CRASH_PACKAGES, list).apply()
    }
}
