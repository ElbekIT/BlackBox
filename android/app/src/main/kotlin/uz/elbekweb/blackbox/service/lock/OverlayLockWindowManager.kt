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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import uz.elbekweb.blackbox.core.util.ScreenUtils
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences
import java.util.Calendar

/**
 * 0-Millisekundli Universal Responsive WindowManager Overlay Qulf Oynasi.
 * Har qanday ekran o'lchami (4-dyuymdan planshetlargacha, Foldable va Landscape)
 * uchun to'liq moslashadi. Hech qanday ekranda elementlar qirqilmaydi va oqib ketmaydi.
 * FLAG_SECURE: skrinshot va ekran yozishdan to'liq himoyalangan.
 */
class OverlayLockWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var currentLockedPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val enteredPin = StringBuilder()
    private val keyPressTimestamps = mutableListOf<Long>()
    private var pinDotsView: LinearLayout? = null
    private var failedAttempts = 0

    companion object {
        val temporarilyUnlockedPackages = mutableSetOf<String>()
    }

    /**
     * Parol oynasini 0 ms kechikish bilan ekranga chizish
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay(packageName: String) {
        if (overlayView != null) return
        if (temporarilyUnlockedPackages.contains(packageName)) return

        currentLockedPackage = packageName
        enteredPin.clear()
        keyPressTimestamps.clear()

        mainHandler.post {
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
                // Zamonaviy ekran tirqishlari (Notch/Punch-hole) bilan to'g'ri ishlash
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            val lockView = createUniversalCyberLockUi()
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
     * Har qanday ekran o'lchami va orientatsiyada (Landscape / Portrait / Tablet / Foldable)
     * mukammal moslashuvchi (Responsive) AMOLED Cyber Lock UI
     */
    private fun createUniversalCyberLockUi(): View {
        val isLandscape = ScreenUtils.isLandscape(context)
        val isTablet = ScreenUtils.isTablet(context)
        val btnSize = ScreenUtils.calculateKeypadButtonSize(context)

        // 1. Tashqi ScrollView - balandlik yetmay qolsa (masalan, yotiq rejimda) ekran aylanishi uchun
        val scrollView = ScrollView(context).apply {
            setBackgroundColor(Color.parseColor("#000000")) // Pure AMOLED Black
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        // 2. Markazlashtiruvchi tashqi konteyner
        val outerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val horizPadding = if (isTablet) ScreenUtils.dp(context, 48) else ScreenUtils.dp(context, 16)
            val vertPadding = ScreenUtils.dp(context, 24)
            setPadding(horizPadding, vertPadding, horizPadding, vertPadding)
        }
        scrollView.addView(outerContainer)

        // 3. Markaziy karta (Planshetlarda ulkan bo'lib ketmasligi uchun maksimal kenglik chegaralangan)
        val centerCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val maxCardWidth = if (isTablet) ScreenUtils.dp(context, 460) else ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams = LinearLayout.LayoutParams(maxCardWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        outerContainer.addView(centerCard)

        // Sarlavha
        val titleView = TextView(context).apply {
            text = "BLACKBOX // CYBER VAULT"
            setTextColor(Color.parseColor("#00FF66")) // Cyber Green
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isLandscape) 16f else 20f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, ScreenUtils.dp(context, 8), 0, ScreenUtils.dp(context, 4))
        }
        centerCard.addView(titleView)

        // Qulflangan ilova nomi
        val subTitle = TextView(context).apply {
            text = "QULFLANGAN: [${currentLockedPackage ?: "SYSTEM"}]"
            setTextColor(Color.parseColor("#00E5FF")) // Neon Blue
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isLandscape) 10f else 12f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, ScreenUtils.dp(context, if (isLandscape) 12 else 24))
        }
        centerCard.addView(subTitle)

        // PIN Nuqtalari (Dots)
        pinDotsView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, ScreenUtils.dp(context, if (isLandscape) 12 else 28))
        }
        updatePinDots()
        centerCard.addView(pinDotsView)

        // Raqamlar Klaviaturasi (3x4 Grid)
        val keypadGrid = GridLayout(context).apply {
            columnCount = 3
            rowCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            val gridParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            layoutParams = gridParams
        }

        val buttons = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "C", "0", "⌫"
        )

        val margin = ScreenUtils.dp(context, if (isLandscape) 6 else 10)
        val cornerRadius = btnSize / 2f // Tugma to'liq yumaloq kiber shaklga keladi

        for (label in buttons) {
            val btn = Button(context).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (btnSize < ScreenUtils.dp(context, 60)) 18f else 22f)
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#E6EDF3"))

                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#0D1117"))
                    setStroke(ScreenUtils.dp(context, 1), Color.parseColor("#1F3327"))
                    this.cornerRadius = cornerRadius
                }
                background = bg

                val params = GridLayout.LayoutParams().apply {
                    width = btnSize
                    height = btnSize
                    setMargins(margin, margin, margin, margin)
                }
                layoutParams = params

                setOnClickListener {
                    onKeyClicked(label)
                }
            }
            keypadGrid.addView(btn)
        }

        centerCard.addView(keypadGrid)
        return scrollView
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
        val dotSize = ScreenUtils.dp(context, 14)
        val dotMargin = ScreenUtils.dp(context, 8)

        for (i in 0 until count) {
            val dot = View(context).apply {
                val params = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    setMargins(dotMargin, 0, dotMargin, 0)
                }
                layoutParams = params
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (i < enteredPin.length) {
                        setColor(Color.parseColor("#00FF66")) // Neon Yashil
                    } else {
                        setColor(Color.parseColor("#21262D"))
                        setStroke(ScreenUtils.dp(context, 1), Color.parseColor("#30363D"))
                    }
                }
                background = drawable
            }
            pinDotsView?.addView(dot)
        }
    }

    private fun checkEnteredPin() {
        val pin = enteredPin.toString()
        if (pin.length < 4) return

        if (BlackBoxPreferences.isTimePinEnabled) {
            val calendar = Calendar.getInstance()
            val timePin = String.format("%02d%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            if (pin == timePin) {
                onUnlockSuccess()
                return
            }
        }

        if (BlackBoxPreferences.checkMasterPin(pin)) {
            onUnlockSuccess()
            return
        }

        if (BlackBoxPreferences.checkDecoyPin(pin)) {
            onUnlockSuccess(isDecoy = true)
            return
        }

        if (pin.length >= 4 && (pin.length == 6 || pin.length == 8 || pin.length == 4)) {
            failedAttempts++
            pinDotsView?.let { view ->
                view.setBackgroundColor(Color.parseColor("#44FF0055"))
                mainHandler.postDelayed({
                    view.setBackgroundColor(Color.TRANSPARENT)
                    enteredPin.clear()
                    updatePinDots()
                }, 300)
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

    /**
     * Universal Responsive Soxta Xatolik (Fake Crash) oynasi
     */
    private fun showFakeCrashOverlay(packageName: String) {
        val isTablet = ScreenUtils.isTablet(context)

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
            val p = ScreenUtils.dp(context, 24)
            setPadding(p, p, p, p)

            val maxCardWidth = if (isTablet) ScreenUtils.dp(context, 420) else ViewGroup.LayoutParams.MATCH_PARENT
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val cardBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E1E1E"))
                    cornerRadius = ScreenUtils.dp(context, 16).toFloat()
                }
                background = cardBg
                val cp = ScreenUtils.dp(context, 24)
                setPadding(cp, cp, cp, ScreenUtils.dp(context, 16))
                layoutParams = LinearLayout.LayoutParams(maxCardWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

                val msg = TextView(context).apply {
                    text = "Afsuski, dastur to'xtatildi."
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                }
                addView(msg)

                val okBtn = Button(context).apply {
                    text = "OK"
                    setTextColor(Color.parseColor("#00E5FF"))
                    setBackgroundColor(Color.TRANSPARENT)
                    gravity = Gravity.END
                    setOnClickListener {
                        hideOverlay()
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
