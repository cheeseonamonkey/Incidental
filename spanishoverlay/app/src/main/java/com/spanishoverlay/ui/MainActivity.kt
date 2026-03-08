package com.spanishoverlay.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import com.spanishoverlay.R
import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.CountMode
import com.spanishoverlay.data.OverlayConfig
import com.spanishoverlay.data.PoS
import com.spanishoverlay.service.SpanishOverlayService
import com.spanishoverlay.util.isAccessibilityServiceEnabled

class MainActivity : AppCompatActivity() {
    private val config by lazy { ConfigRepository.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindAllSections()
        setupComposeIslands()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateBatteryWarning()
    }

    private fun updateServiceStatus() {
        val on = isAccessibilityServiceEnabled(SpanishOverlayService::class.java)
        val tv = findViewById<TextView>(R.id.status_text)
        tv.text = getString(if (on) R.string.service_status_on else R.string.service_status_off)
        tv.setTextColor(ContextCompat.getColor(this, if (on) R.color.status_on else R.color.status_off))
        findViewById<View>(R.id.btn_open_a11y).apply {
            visibility = if (on) View.GONE else View.VISIBLE
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
    }

    private fun updateBatteryWarning() {
        val card = findViewById<View>(R.id.battery_warning_card)
        if (Build.VERSION.SDK_INT < 23) { card.visibility = View.GONE; return }
        val pm = getSystemService(PowerManager::class.java)
        card.visibility = if (pm.isIgnoringBatteryOptimizations(packageName)) View.GONE else View.VISIBLE
        card.setOnClickListener {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun setupComposeIslands() {
        findViewById<ComposeView>(R.id.compose_stop_words).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    StopWordsScreen(
                        initialWords = config.snapshot().stopWords.sorted(),
                        onWordsChanged = { config.update { copy(stopWords = it.toSet()) } }
                    )
                }
            }
        }
        findViewById<ComposeView>(R.id.compose_excluded_apps).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ExcludedAppsScreen(
                        initialPackages = config.snapshot().excludePackages.toList(),
                        packageManager = this@MainActivity.packageManager,
                        onPackagesChanged = { config.update { copy(excludePackages = it.toSet()) } }
                    )
                }
            }
        }
    }

    private fun bindAllSections() {
        val cfg = config.snapshot()

        // Count mode
        val rbFraction = findViewById<RadioButton>(R.id.rb_fraction)
        val rbFixed = findViewById<RadioButton>(R.id.rb_fixed)
        if (cfg.replaceCountMode == CountMode.FRACTION) rbFraction.isChecked = true else rbFixed.isChecked = true
        findViewById<RadioGroup>(R.id.rg_count_mode).setOnCheckedChangeListener { _, id ->
            config.update { copy(replaceCountMode = if (id == R.id.rb_fraction) CountMode.FRACTION else CountMode.FIXED) }
        }

        // Every N
        val labelN = findViewById<TextView>(R.id.label_every_n)
        val seekN = findViewById<SeekBar>(R.id.seekbar_replace_n)
        seekN.progress = cfg.replaceEveryN - 2
        labelN.text = getString(R.string.label_every_n, cfg.replaceEveryN)
        seekN.onChange(
            drag = { p -> labelN.text = getString(R.string.label_every_n, p + 2) },
            commit = { p -> config.update { copy(replaceEveryN = p + 2) } }
        )

        // Fixed count
        val labelFixed = findViewById<TextView>(R.id.label_fixed_count)
        val seekFixed = findViewById<SeekBar>(R.id.seekbar_fixed_count)
        seekFixed.progress = cfg.replaceFixedCount - 1
        labelFixed.text = getString(R.string.label_fixed_count, cfg.replaceFixedCount)
        seekFixed.onChange(
            drag = { p -> labelFixed.text = getString(R.string.label_fixed_count, p + 1) },
            commit = { p -> config.update { copy(replaceFixedCount = p + 1) } }
        )

        // Min word length
        val labelLen = findViewById<TextView>(R.id.label_word_length)
        val seekLen = findViewById<SeekBar>(R.id.seekbar_min_length)
        seekLen.progress = cfg.minWordLength - 1
        labelLen.text = getString(R.string.label_word_length, cfg.minWordLength, cfg.maxWordLength)
        seekLen.onChange(
            drag = { p -> labelLen.text = getString(R.string.label_word_length, p + 1, config.snapshot().maxWordLength) },
            commit = { p -> config.update { copy(minWordLength = p + 1) } }
        )

        // Complexity
        val labelComp = findViewById<TextView>(R.id.label_complexity)
        val seekComp = findViewById<SeekBar>(R.id.seekbar_complexity_max)
        seekComp.progress = cfg.complexityMax
        labelComp.text = getString(R.string.label_complexity, cfg.complexityMin, cfg.complexityMax)
        seekComp.onChange(
            drag = { p -> labelComp.text = getString(R.string.label_complexity, 0, p) },
            commit = { p -> config.update { copy(complexityMax = p) } }
        )

        // PoS checkboxes
        fun bindPos(id: Int, pos: PoS) {
            findViewById<CheckBox>(id).apply {
                isChecked = pos in cfg.enabledPos
                setOnCheckedChangeListener { _, checked ->
                    config.update {
                        val next = if (checked) enabledPos + pos else enabledPos - pos
                        copy(enabledPos = next.ifEmpty { setOf(PoS.NOUN) })
                    }
                }
            }
        }
        bindPos(R.id.cb_noun, PoS.NOUN); bindPos(R.id.cb_verb, PoS.VERB)
        bindPos(R.id.cb_adj, PoS.ADJECTIVE); bindPos(R.id.cb_adv, PoS.ADVERB)

        // Alpha
        val labelAlpha = findViewById<TextView>(R.id.label_alpha)
        val seekAlpha = findViewById<SeekBar>(R.id.seekbar_alpha)
        seekAlpha.progress = (cfg.overlayAlpha * 100).toInt()
        labelAlpha.text = getString(R.string.label_alpha, (cfg.overlayAlpha * 100).toInt())
        seekAlpha.onChange(
            drag = { p -> labelAlpha.text = getString(R.string.label_alpha, p) },
            commit = { p -> config.update { copy(overlayAlpha = p / 100f) } }
        )

        // TTL
        val labelTtl = findViewById<TextView>(R.id.label_ttl)
        val seekTtl = findViewById<SeekBar>(R.id.seekbar_ttl)
        seekTtl.progress = (cfg.overlayTtlMs - 1000) / 500
        labelTtl.text = getString(R.string.label_ttl, cfg.overlayTtlMs / 1000f)
        seekTtl.onChange(
            drag = { p -> labelTtl.text = getString(R.string.label_ttl, (1000 + p * 500) / 1000f) },
            commit = { p -> config.update { copy(overlayTtlMs = 1000 + p * 500) } }
        )

        // Debounce
        val labelDb = findViewById<TextView>(R.id.label_debounce)
        val seekDb = findViewById<SeekBar>(R.id.seekbar_debounce)
        seekDb.progress = (cfg.debounceMs - 100) / 50
        labelDb.text = getString(R.string.label_debounce, cfg.debounceMs)
        seekDb.onChange(
            drag = { p -> labelDb.text = getString(R.string.label_debounce, 100 + p * 50) },
            commit = { p -> config.update { copy(debounceMs = 100 + p * 50) } }
        )

        // Reset
        findViewById<View>(R.id.btn_reset).setOnClickListener {
            config.update { OverlayConfig.DEFAULT }
            recreate()
        }
    }

    /** Live label updates during drag, config commit only on finger-up. */
    private fun SeekBar.onChange(drag: (Int) -> Unit, commit: (Int) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) drag(p) }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { commit(sb.progress) }
        })
    }
}
