package uz.elbekweb.blackbox.presentation

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uz.elbekweb.blackbox.core.permission.PermissionWizardManager
import uz.elbekweb.blackbox.data.preference.BlackBoxPreferences

/**
 * BlackBox Asosiy Kiber-Boshqaruv Markazi (Dashboard & Permission Wizard).
 * Ishlab chiquvchi: Elbek Web (@elbekdesign_va_webdasturchi_uz)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionWizardManager
    private lateinit var contentContainer: LinearLayout
    private lateinit var scoreTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionWizardManager(this)

        // AMOLED Cyber UI ni dasturiy tarzda chizish
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#000000")) // Pure AMOLED Black
            isFillViewport = true
        }

        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 60)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        scrollView.addView(contentContainer)
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
            textSize = 22f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 8)
        }
        contentContainer.addView(header)

        val subHeader = TextView(this).apply {
            text = "Elbek Web • 100% Oflayn Kiber Maxfiylik"
            setTextColor(Color.parseColor("#00E5FF")) // Neon Blue
            textSize = 12f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        contentContainer.addView(subHeader)

        // 2. Cyber Guardian Reyting Kartochkasi (0 - 100)
        val scoreCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#0D1117"))
                setStroke(3, Color.parseColor("#00FF66"))
                cornerRadius = 24f
            }
            background = bg
        }

        val scoreTitle = TextView(this).apply {
            text = "CYBER GUARDIAN XAVFSIZLIK REYTINGI"
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        scoreCard.addView(scoreTitle)

        scoreTextView = TextView(this).apply {
            text = "-- / 100"
            setTextColor(Color.parseColor("#00FF66"))
            textSize = 36f
            typeface = Typeface.MONOSPACE
            setPadding(0, 16, 0, 0)
        }
        scoreCard.addView(scoreTextView)
        contentContainer.addView(scoreCard)

        // 3. Permission Setup Wizard Sarlavhasi
        val wizardTitle = TextView(this).apply {
            text = "TIZIM RUXSATLARI WIZARD (0 ms Himoya)"
            setTextColor(Color.parseColor("#E6EDF3"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setPadding(0, 48, 0, 16)
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
            setTextColor(Color.parseColor("#000000"))
            setBackgroundColor(Color.parseColor("#00E5FF"))
            typeface = Typeface.MONOSPACE
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 40, 0, 20)
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
            setPadding(32, 28, 32, 28)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#161B22"))
                setStroke(1, if (item.isGranted) Color.parseColor("#00FF66") else Color.parseColor("#30363D"))
                cornerRadius = 16f
            }
            background = bg
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 12)
            }
            layoutParams = params
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = params
        }

        val name = TextView(this).apply {
            text = item.type.title
            setTextColor(if (item.isGranted) Color.parseColor("#00FF66") else Color.parseColor("#E6EDF3"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        textLayout.addView(name)

        val desc = TextView(this).apply {
            text = item.type.description
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 10f
            setPadding(0, 4, 0, 0)
        }
        textLayout.addView(desc)
        card.addView(textLayout)

        val actionBtn = Button(this).apply {
            text = if (item.isGranted) "AKTIV" else "BERISH"
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(if (item.isGranted) Color.parseColor("#00FF66") else Color.parseColor("#000000"))

            val btnBg = GradientDrawable().apply {
                setColor(if (item.isGranted) Color.parseColor("#002811") else Color.parseColor("#00FF66"))
                setStroke(1, Color.parseColor("#00FF66"))
                cornerRadius = 12f
            }
            background = btnBg

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

    /**
     * Kalkulyator niqobini yoqish/o'chirish (activity-alias almashtirish)
     */
    private fun toggleCalculatorDisguise() {
        val newState = !BlackBoxPreferences.isCalculatorDisguiseEnabled
        BlackBoxPreferences.isCalculatorDisguiseEnabled = newState

        val pm = packageManager
        val mainComponent = ComponentName(this, MainActivity::class.java)
        val aliasComponent = ComponentName(this, "$packageName.presentation.CalculatorDisguiseAlias")

        if (newState) {
            // Kalkulyator niqobini faollashtirish
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
            // BlackBox asl holatiga qaytarish
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
