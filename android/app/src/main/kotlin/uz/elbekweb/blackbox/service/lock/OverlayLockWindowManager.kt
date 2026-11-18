package uz.elbekweb.blackbox.service.lock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences
import java.util.Calendar

/**
 * 0-Millisekundli WindowManager Overlay Qulf Oynasi.
 * TYPE_ACCESSIBILITY_OVERLAY yoki TYPE_APPLICATION_OVERLAY orqali
 * tizim ustidan darhol xavfsizlik interfeysini chizadi.
 * FLAG_SECURE: skrinshot va ekran yozishdan to'liq himoyalangan.
 */
class OverlayLockWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var currentLockedPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Kiritilgan PIN va Yozish dinamikasi (Keystroke dynamics)
    private val enteredPin = StringBuilder()
    private val keyPressTimestamps = mutableListOf<Long>()
    private var pinDotsView: LinearLayout? = null
    private var failedAttempts = 0

    // Vaqtinchalik ochilgan ilovalar ro'yxati (ekran o'chguncha)
    companion object {
        val temporarilyUnlockedPackages = mutableSetOf<String>()
    }

    /**
     * Parol oynasini 0 ms kechikish bilan ekranga chizish
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay(packageName: String) {
        if (overlayView != null) return // Allaqachon ko'rinib turibdi
        if (temporarilyUnlockedPackages.contains(packageName)) return

        currentLockedPackage = packageName
        enteredPin.clear()
        keyPressTimestamps.clear()

        mainHandler.post {
            // Agar ilova uchun "Soxta Xatolik" (Fake Crash) rejimi yoqilgan bo'lsa
            if (BlackBoxPreferences.isFakeCrashPackage(packageName)) {
                showFakeCrashOverlay(packageName)
                return@post
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_SECURE or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val lockView = createCyberLockUi()
            overlayView = lockView

            try {
                windowManager.addView(lockView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Parol oynasini yopish
     */
    fun hideOverlay() {
        mainHandler.post {
            overlayView?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    overlayView = null
                    currentLockedPackage = null
                    enteredPin.clear()
                }
            }
        }
    }

    /**
     * Sof kod orqali tezkor AMOLED Cyber Lock UI yaratish (0ms overhead)
     */
    private fun createCyberLockUi(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#000000")) // Pure AMOLED Black
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 80, 48, 48)
        }

        // Qulf Ikonkasi va Kiber Sarlavha
        val titleView = TextView(context).apply {
            text = "BLACKBOX // CYBER VAULT"
            setTextColor(Color.parseColor("#00FF66")) // Cyber Green
            textSize = 20f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 16)
        }
        root.addView(titleView)

        val subTitle = TextView(context).apply {
            text = "QULFLANGAN ILOVA: [${currentLockedPackage ?: "SYSTEM"}]"
            setTextColor(Color.parseColor("#00E5FF")) // Neon Blue
            textSize = 12f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        root.addView(subTitle)

        // PIN Nuqtalari ko'rsatgichi
        pinDotsView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }
        updatePinDots()
        root.addView(pinDotsView)

        // Raqamlar Klaviaturasi (Grid 3x4)
        val keypadGrid = GridLayout(context).apply {
            columnCount = 3
            rowCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }

        val buttons = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "C", "0", "⌫"
        )

        for (label in buttons) {
            val btn = Button(context).apply {
                text = label
                textSize = 22f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#E6EDF3"))

                // Kiber tugma stili
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#0D1117"))
                    setStroke(2, Color.parseColor("#1F3327"))
                    cornerRadius = 60f
                }
                background = bg

                val params = GridLayout.LayoutParams().apply {
                    width = 200
                    height = 200
                    setMargins(16, 16, 16, 16)
                }
                layoutParams = params

                setOnClickListener {
                    onKeyClicked(label)
                }
            }
            keypadGrid.addView(btn)
        }

        root.addView(keypadGrid)
        return root
    }

    private fun onKeyClicked(label: String) {
        val now = System.currentTimeMillis()
        keyPressTimestamps.add(now)

        when (label) {
            "C" -> {
                enteredPin.clear()
                keyPressTimestamps.clear()
                updatePinDots()
            }
            "⌫" -> {
                if (enteredPin.isNotEmpty()) {
                    enteredPin.deleteCharAt(enteredPin.length - 1)
                    if (keyPressTimestamps.isNotEmpty()) keyPressTimestamps.removeAt(keyPressTimestamps.size - 1)
                    updatePinDots()
                }
            }
            else -> {
                if (enteredPin.length < 8) {
                    enteredPin.append(label)
                    updatePinDots()
                    checkEnteredPin()
                }
            }
        }
    }

    private fun updatePinDots() {
        pinDotsView?.removeAllViews()
        val count = maxOf(4, enteredPin.length)
        for (i in 0 until count) {
            val dot = View(context).apply {
                val size = 28
                val params = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(12, 0, 12, 0)
                }
                layoutParams = params
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (i < enteredPin.length) {
                        setColor(Color.parseColor("#00FF66")) // To'ldirilgan (Neon Yashil)
                    } else {
                        setColor(Color.parseColor("#21262D")) // Bo'sh
                        setStroke(2, Color.parseColor("#30363D"))
                    }
                }
                background = drawable
            }
            pinDotsView?.addView(dot)
        }
    }

    /**
     * PIN tekshiruvi: Master PIN, Decoy PIN va Time-PIN
     */
    private fun checkEnteredPin() {
        val pin = enteredPin.toString()
        if (pin.length < 4) return

        // 1. Time-PIN tekshiruvi (agar yoqilgan bo'lsa, joriy soat-daqiqa HHmm masalan: 1425)
        if (BlackBoxPreferences.isTimePinEnabled) {
            val calendar = Calendar.getInstance()
            val timePin = String.format("%02d%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            if (pin == timePin) {
                onUnlockSuccess()
                return
            }
        }

        // 2. Master PIN tekshiruvi
        if (BlackBoxPreferences.checkMasterPin(pin)) {
            onUnlockSuccess()
            return
        }

        // 3. Decoy (Mehmon) PIN tekshiruvi
        if (BlackBoxPreferences.checkDecoyPin(pin)) {
            onUnlockSuccess(isDecoy = true)
            return
        }

        // 4. Xato parol holati
        if (pin.length >= 4 && (pin.length == 6 || pin.length == 8 || pin.length == 4)) {
            failedAttempts++
            pinDotsView?.let { view ->
                // Qizil qilib miltillatish
                view.setBackgroundColor(Color.parseColor("#44FF0055"))
                mainHandler.postDelayed({
                    view.setBackgroundColor(Color.TRANSPARENT)
                    enteredPin.clear()
                    updatePinDots()
                }, 300)
            }

            if (failedAttempts >= 2 && BlackBoxPreferences.isIntruderSelfieEnabled) {
                // Intruder Selfie olish mexanizmini ishga tushirish
                triggerIntruderCapture()
            }
        }
    }

    private fun onUnlockSuccess(isDecoy: Boolean = false) {
        failedAttempts = 0
        currentLockedPackage?.let {
            temporarilyUnlockedPackages.add(it)
        }
        hideOverlay()
    }

    private fun triggerIntruderCapture() {
        // Old kamera orqali maxfiy suratga olish vazifasi
    }

    /**
     * Soxta "Dastur to'xtatildi" xatolik oynasi (Fake Crash)
     */
    private fun showFakeCrashOverlay(packageName: String) {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val crashLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setPadding(40, 40, 40, 30)

                val msg = TextView(context).apply {
                    text = "Afsuski, dastur to'xtatildi."
                    setTextColor(Color.WHITE)
                    textSize = 18f
                }
                addView(msg)

                val okBtn = Button(context).apply {
                    text = "OK"
                    setTextColor(Color.parseColor("#00E5FF"))
                    setBackgroundColor(Color.TRANSPARENT)
                    gravity = Gravity.END
                    setOnClickListener {
                        hideOverlay()
                        // Bosh ekranga (Home) chiqarib yuborish
                    }
                }
                addView(okBtn)
            }
            addView(card)
        }

        overlayView = crashLayout
        try {
            windowManager.addView(crashLayout, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
