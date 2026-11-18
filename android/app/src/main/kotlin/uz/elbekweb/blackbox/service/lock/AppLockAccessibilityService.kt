package uz.elbekweb.blackbox.service.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences

/**
 * 0-Millisekundli AppLock va Tizim Nazorati Accessibility Servisi.
 * TYPE_WINDOW_STATE_CHANGED orqali ilovalar ishga tushgan zahoti
 * (interfeysi ko'rinmasidan oldin) tutib qoladi va qulf oynasini chaqiradi.
 * Shuningdek, Morse-kod Ovoz tugmalari (+ / -) orqali yashirin ochishni boshqaradi.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var overlayWindowManager: OverlayLockWindowManager
    private var lastForegroundPackage: String? = null

    // Morse-kod ovoz tugmalarini tutish kesh buferi
    private val morseBuffer = StringBuilder()
    private val morseResetHandler = Handler(Looper.getMainLooper())
    private val morseResetRunnable = Runnable {
        morseBuffer.clear()
    }

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayLockWindowManager(this)
    }

    /**
     * Tizim datchigi: Oyna holati o'zgarganda 0 ms da chaqiriladi
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return

                // O'z ilovamiz yoki tizim launcherlarini o'tkazib yuborish
                if (packageName == applicationContext.packageName) {
                    return
                }

                // Agar paket avvalgisidan farq qilsa
                if (packageName != lastForegroundPackage) {
                    lastForegroundPackage = packageName

                    // Qulflangan ilova ekanligini tekshirish
                    if (BlackBoxPreferences.isPackageLocked(packageName)) {
                        overlayWindowManager.showOverlay(packageName)
                    } else {
                        // Qulfsiz ilovaga o'tilganda overlayni yopish
                        overlayWindowManager.hideOverlay()
                    }
                }
            }
        }
    }

    /**
     * Apparat ovoz tugmalari orqali Morse-kod kombinatsiyasini tekshirish
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    registerMorseKey("+")
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    registerMorseKey("-")
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun registerMorseKey(symbol: String) {
        morseResetHandler.removeCallbacks(morseResetRunnable)
        morseBuffer.append(symbol)

        val targetMorse = BlackBoxPreferences.morseCodeSequence // masalan: "+-+"
        if (morseBuffer.toString() == targetMorse) {
            // Morse kod to'g'ri kiritildi! Darhol qulfni ochish
            overlayWindowManager.hideOverlay()
            morseBuffer.clear()
        } else if (morseBuffer.length >= targetMorse.length) {
            morseBuffer.clear()
        } else {
            // 2 soniyadan so'ng buferni tozalash
            morseResetHandler.postDelayed(morseResetRunnable, 2000)
        }
    }

    override fun onInterrupt() {
        // Tizim to'xtatganda
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayWindowManager.hideOverlay()
    }
}
