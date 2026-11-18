package uz.elbekweb.blackbox.presentation

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uz.elbekweb.blackbox.core.permission.PermissionWizardManager
import uz.elbekweb.blackbox.core.util.ScreenUtils
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences

/**
 * BlackBox Asosiy Kiber-Boshqaruv Markazi (Dashboard & Permission Wizard).
 * Har qanday ekran o'lchami (4" dan 12" gacha) uchun 100% universal va moslashuvchan.
 * Ishlab chiquvchi: Elbek Web (@elbekdesign_va_webdasturchi_uz)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionWizardManager
    private lateinit var contentContainer: LinearLayout
    private lateinit var scoreTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionWizardManager(this)

        val isTablet = ScreenUtils.isTablet(this)

        // AMOLED Cyber UI ni dasturiy tarzda chizish (Adaptive ScrollView)
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#000000")) // Pure AMOLED Black
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val rootWrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val horizPadding = if (isTablet) ScreenUtils.dp(this@MainActivity, 32) else ScreenUtils.dp(this@MainActivity, 16)
            val vertPadding = ScreenUtils.dp(this@MainActivity, 24)
            setPadding(horizPadding, vertPadding, horizPadding, vertPadding)
        }
        scrollView.addView(rootWrapper)

        // Markaziy chegaralangan konteyner (Planshet va katta ekranlarda cho'zilib ketmasligi uchun)
        val maxContainerWidth = if (isTablet) ScreenUtils.dp(this, 560) else ViewGroup.LayoutParams.MATCH_PARENT
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(maxContainerWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        rootWrapper.addView(contentContainer)

        setContentView(scrollView)
        buildDashboardUi()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun buildDashboardUi() {
        contentContainer.removeAllViews()

        // 1. Kiber Sarlavha
        val header = TextView(this).apply {
            text = "BLACKBOX // CYBER SHIELD"
            setTextColor(Color.parseColor("#00FF66")) // Neon Green
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, ScreenUtils.dp(this@MainActivity, 8), 0, ScreenUtils.dp(this@MainActivity, 4))
        }
        contentContainer.addView(header)

        val subHeader = TextView(this).apply {
            text = "Elbek Web • 100% Oflayn Kiber Maxfiylik"
            setTextColor(Color.parseColor("#00E5FF")) // Neon Blue
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, ScreenUtils.dp(this@MainActivity, 20))
        }
        contentContainer.addView(subHeader)

        // 2. Cyber Guardian Reyting Kartochkasi (0 - 100)
        val scoreCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val p = ScreenUtils.dp(this@MainActivity, 20)
            setPadding(p, p, p, p)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#0D1117"))
                setStroke(ScreenUtils.dp(this@MainActivity, 2), Color.parseColor("#00FF66"))
                cornerRadius = ScreenUtils.dp(this@MainActivity, 16).toFloat()
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val scoreTitle = TextView(this).apply {
            text = "CYBER GUARDIAN XAVFSIZLIK REYTINGI"
            setTextColor(Color.parseColor("#8B949E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
        }
        scoreCard.addView(scoreTitle)

        scoreTextView = TextView(this).apply {
            text = "-- / 100"
            setTextColor(Color.parseColor("#00FF66"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
            typeface = Typeface.MONOSPACE
            setPadding(0, ScreenUtils.dp(this@MainActivity, 8), 0, 0)
        }
        scoreCard.addView(scoreTextView)
        contentContainer.addView(scoreCard)

        // 3. Permission Setup Wizard Sarlavhasi
        val wizardTitle = TextView(this).apply {
            text = "TIZIM RUXSATLARI WIZARD (0 ms Himoya)"
            setTextColor(Color.parseColor("#E6EDF3"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            setPadding(
                0,
                ScreenUtils.dp(this@MainActivity, 24),
                0,
                ScreenUtils.dp(this@MainActivity, 10)
            )
        }
        contentContainer.addView(wizardTitle)

        // Ruxsatlar ro'yxati
        val permissions = permissionManager.checkAllPermissions()
        for (item in permissions) {
            val card = createPermissionCard(item)
            contentContainer.addView(card)
        }

        // 4. Kalkulyator Niqobi (Chameleon Disguise) Tugmasi
        val disguiseBtn = Button(this).apply {
            text = if (BlackBoxPreferences.isCalculatorDisguiseEnabled) {
                "KALKULYATOR NIQOBI: YONIQ (O'chirish)"
            } else {
                "KALKULYATOR NIQOBINI YOQISH (Stealth)"
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#000000"))
            typeface = Typeface.MONOSPACE

            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#00E5FF"))
                cornerRadius = ScreenUtils.dp(this@MainActivity, 12).toFloat()
            }
            background = btnBg

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ScreenUtils.dp(this@MainActivity, 48)
            ).apply {
                setMargins(
                    0,
                    ScreenUtils.dp(this@MainActivity, 24),
                    0,
                    ScreenUtils.dp(this@MainActivity, 32)
                )
            }
            layoutParams = params

            setOnClickListener {
                toggleCalculatorDisguise()
            }
        }
        contentContainer.addView(disguiseBtn)

        updateScore()
    }

    private fun createPermissionCard(item: PermissionWizardManager.PermissionStatus): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hPad = ScreenUtils.dp(this@MainActivity, 16)
            val vPad = ScreenUtils.dp(this@MainActivity, 14)
            setPadding(hPad, vPad, hPad, vPad)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#161B22"))
                setStroke(
                    ScreenUtils.dp(this@MainActivity, 1),
                    if (item.isGranted) Color.parseColor("#00FF66") else Color.parseColor("#30363D")
                )
                cornerRadius = ScreenUtils.dp(this@MainActivity, 12).toFloat()
            }
            background = bg
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, ScreenUtils.dp(this@MainActivity, 6), 0, ScreenUtils.dp(this@MainActivity, 6))
            }
            layoutParams = params
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, ScreenUtils.dp(this@MainActivity, 12), 0)
            }
            layoutParams = params
        }

        val name = TextView(this).apply {
            text = item.type.title
            setTextColor(if (item.isGranted) Color.parseColor("#00FF66") else Color.parseColor("#E6EDF3"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
        }
        textLayout.addView(name)

        val desc = TextView(this).apply {
            text = item.type.description
            setTextColor(Color.parseColor("#8B949E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, ScreenUtils.dp(this@MainActivity, 2), 0, 0)
        }
        textLayout.addView(desc)
        card.addView(textLayout)

        val actionBtn = Button(this).apply {
            text = if (item.isGranted) "AKTIV" else "BERISH"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setTextColor(if (item.isGranted) Color.parseColor("#00FF66") else Color.parseColor("#000000"))

            val btnBg = GradientDrawable().apply {
                setColor(if (item.isGranted) Color.parseColor("#002811") else Color.parseColor("#00FF66"))
                setStroke(ScreenUtils.dp(this@MainActivity, 1), Color.parseColor("#00FF66"))
                cornerRadius = ScreenUtils.dp(this@MainActivity, 8).toFloat()
            }
            background = btnBg

            val btnParams = LinearLayout.LayoutParams(
                ScreenUtils.dp(this@MainActivity, 74),
                ScreenUtils.dp(this@MainActivity, 36)
            )
            layoutParams = btnParams

            if (!item.isGranted) {
                setOnClickListener {
                    permissionManager.requestPermission(this@MainActivity, item.type)
                }
            }
        }
        card.addView(actionBtn)

        return card
    }

    private fun updateScore() {
        val permissions = permissionManager.checkAllPermissions()
        val total = permissions.size
        val granted = permissions.count { it.isGranted }
        val score = (granted * 100) / total
        scoreTextView.text = "$score / 100"
        scoreTextView.setTextColor(
            if (score > 75) Color.parseColor("#00FF66")
            else if (score > 40) Color.parseColor("#00E5FF")
            else Color.parseColor("#FF0055")
        )
    }

    private fun refreshDashboard() {
        buildDashboardUi()
    }

    private fun toggleCalculatorDisguise() {
        val newState = !BlackBoxPreferences.isCalculatorDisguiseEnabled
        BlackBoxPreferences.isCalculatorDisguiseEnabled = newState

        val pm = packageManager
        val mainComponent = ComponentName(this, MainActivity::class.java)
        val aliasComponent = ComponentName(this, "$packageName.presentation.CalculatorDisguiseAlias")

        if (newState) {
            pm.setComponentEnabledSetting(
                aliasComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                mainComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Toast.makeText(this, "Kalkulyator niqobi faollashtirildi!", Toast.LENGTH_LONG).show()
        } else {
            pm.setComponentEnabledSetting(
                mainComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                aliasComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Toast.makeText(this, "BlackBox asl ko'rinishiga qaytarildi!", Toast.LENGTH_LONG).show()
        }

        refreshDashboard()
    }
}
