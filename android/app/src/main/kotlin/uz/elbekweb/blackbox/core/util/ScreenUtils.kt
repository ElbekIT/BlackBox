package uz.elbekweb.blackbox.core.util

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.util.TypedValue

/**
 * Universal Ekran va O'lcham Adaptatsiya Yordamchisi (Responsive Screen Utility).
 * Ilova har qanday qurilmada (kichik 4-dyuymli telefonlar, flagmanlar,
 * buklanuvchi Foldable qurilmalar, planshetlar) bir xil, buzilmasdan
 * va qirqilib qolmasdan (0 overflow) ishlashini ta'minlaydi.
 */
object ScreenUtils {

    /**
     * DP birligini joriy ekran zichligiga mos PX ga o'tkazish
     */
    fun dp(context: Context, dpValue: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * SP shrift birligini PX ga o'tkazish (Tizim shrift kattalashishini hisobga olgan holda)
     */
    fun sp(context: Context, spValue: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spValue,
            context.resources.displayMetrics
        )
    }

    /**
     * Ekran kengligini (px) olish
     */
    fun getScreenWidthPx(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    /**
     * Ekran balandligini (px) olish
     */
    fun getScreenHeightPx(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * Qurilma planshet (Tablet) ekanligini aniqlash (kengligi >= 600dp)
     */
    fun isTablet(context: Context): Boolean {
        return (context.resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    /**
     * Ekran yotiq (Landscape) holatda ekanligini aniqlash
     */
    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * Har qanday ekranda (480p dan 4K gacha, kichik va ulkan ekranlarda)
     * PIN klaviatura tugmalarining ideal kvadrat hajmini hisoblash.
     */
    fun calculateKeypadButtonSize(context: Context): Int {
        val screenWidth = getScreenWidthPx(context)
        val screenHeight = getScreenHeightPx(context)
        val landscape = isLandscape(context)

        return if (landscape) {
            // Yotiq holatda balandlik bo'yicha hisoblanadi (4 ta qator sig'ishi shart)
            val availableHeight = screenHeight - dp(context, 120)
            val calculated = (availableHeight / 4) - dp(context, 12)
            maxOf(dp(context, 48), minOf(calculated, dp(context, 72)))
        } else {
            // Tikka holatda kenglik bo'yicha hisoblanadi (3 ta ustun sig'ishi shart)
            val maxContainerWidth = if (isTablet(context)) dp(context, 380) else screenWidth - dp(context, 48)
            val calculated = (maxContainerWidth / 3) - dp(context, 16)
            maxOf(dp(context, 54), minOf(calculated, dp(context, 82)))
        }
    }
}
