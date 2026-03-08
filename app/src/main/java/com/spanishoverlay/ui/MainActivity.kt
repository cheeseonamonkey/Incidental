package com.spanishoverlay.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.CountMode
import com.spanishoverlay.data.DisplayMode
import com.spanishoverlay.data.DictionaryEntry
import com.spanishoverlay.data.OverlayConfig
import com.spanishoverlay.data.OverlayPosition
import com.spanishoverlay.data.PoS
import com.spanishoverlay.service.SpanishOverlayService
import com.spanishoverlay.util.isAccessibilityServiceEnabled

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = ConfigRepository.getInstance(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(repo)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(repo: ConfigRepository) {
    val ctx = LocalContext.current
    val cfg by repo.configFlow.collectAsState()
    var serviceOn by remember { mutableStateOf(false) }

    // Refresh service status on each recomposition trigger
    LaunchedEffect(Unit) {
        serviceOn = ctx.isAccessibilityServiceEnabled(SpanishOverlayService::class.java)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Status
        item {
            StatusCard(serviceOn) {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        // Battery warning
        item { BatteryWarningCard() }

        // Overlay permission warning (API 23+)
        item {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(ctx)) {
                WarningCard("Draw over apps permission required") {
                    runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")))
                    }
                }
            }
        }

        // Presets
        item { SectionHeader("Quick Presets") }
        item {
            PresetsRow { preset ->
                repo.update { preset.copy(stopWords = cfg.stopWords, excludePackages = cfg.excludePackages) }
            }
        }

        // Frequency
        item { SectionHeader("Replacement Frequency") }
        item {
            SegmentedControl(
                options = listOf("Every N Words", "Fixed Count"),
                selected = if (cfg.replaceCountMode == CountMode.FRACTION) 0 else 1
            ) { repo.update { copy(replaceCountMode = if (it == 0) CountMode.FRACTION else CountMode.FIXED) } }
        }
        item {
            if (cfg.replaceCountMode == CountMode.FRACTION) {
                LabeledSlider("Every ${cfg.replaceEveryN} words", cfg.replaceEveryN.toFloat(), 2f, 20f) {
                    repo.update { copy(replaceEveryN = it.toInt()) }
                }
            } else {
                LabeledSlider("Replace ${cfg.replaceFixedCount} per screen", cfg.replaceFixedCount.toFloat(), 1f, 15f) {
                    repo.update { copy(replaceFixedCount = it.toInt()) }
                }
            }
        }

        // Filters
        item { SectionHeader("Word Filters") }
        item {
            LabeledSlider("Min length: ${cfg.minWordLength}", cfg.minWordLength.toFloat(), 2f, 12f) {
                repo.update { copy(minWordLength = it.toInt().coerceAtMost(maxWordLength)) }
            }
        }
        item {
            val labels = listOf("Essential", "Common", "Intermediate", "Advanced")
            LabeledSlider("Max complexity: ${labels.getOrElse(cfg.complexityMax) { "Advanced" }}",
                cfg.complexityMax.toFloat(), 0f, 3f) {
                repo.update { copy(complexityMax = it.toInt().coerceAtLeast(complexityMin)) }
            }
        }
        item { PosCheckboxRow(cfg) { repo.update(it) } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = cfg.phrasesEnabled, onCheckedChange = { repo.update { copy(phrasesEnabled = it) } })
                Text("Show phrases (buenos días, etc.)", modifier = Modifier.padding(start = 4.dp))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = cfg.stopWordsEnabled, onCheckedChange = { repo.update { copy(stopWordsEnabled = it) } })
                Text("Skip common words (a, the, is…)", modifier = Modifier.padding(start = 4.dp))
            }
        }

        // Appearance
        item { SectionHeader("Appearance") }
        item {
            SegmentedControl(
                options = listOf("Spanish", "EN→ES", "Stacked"),
                selected = cfg.displayMode.ordinal
            ) { repo.update { copy(displayMode = DisplayMode.entries[it]) } }
        }
        item {
            SegmentedControl(
                options = listOf("Above", "Inline", "Below"),
                selected = cfg.overlayPosition.ordinal
            ) { repo.update { copy(overlayPosition = OverlayPosition.entries[it]) } }
        }
        item {
            LabeledSlider("Opacity: ${(cfg.overlayAlpha * 100).toInt()}%", cfg.overlayAlpha, 0.2f, 1.0f) {
                repo.update { copy(overlayAlpha = it) }
            }
        }
        item {
            LabeledSlider("Font scale: ${"%.1f".format(cfg.fontScale)}×", cfg.fontScale, 0.7f, 2.0f) {
                repo.update { copy(fontScale = it) }
            }
        }
        item {
            LabeledSlider("Vertical offset: ${cfg.verticalOffsetDp}dp", cfg.verticalOffsetDp.toFloat(), -30f, 30f) {
                repo.update { copy(verticalOffsetDp = it.toInt()) }
            }
        }
        item {
            LabeledSlider("Max overlays: ${cfg.maxOverlays}", cfg.maxOverlays.toFloat(), 1f, 40f) {
                repo.update { copy(maxOverlays = it.toInt()) }
            }
        }
        item {
            Text("Text color", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            ColorPickerRow(OverlayConfig.TEXT_COLORS, cfg.overlayTextColor) {
                repo.update { copy(overlayTextColor = it) }
            }
        }
        item {
            Text("Background color", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            ColorPickerRow(OverlayConfig.BG_COLORS, cfg.overlayBgColor) {
                repo.update { copy(overlayBgColor = it) }
            }
        }

        // Timing
        item { SectionHeader("Timing") }
        item {
            LabeledSlider("Display time: ${"%.1f".format(cfg.overlayTtlMs / 1000f)}s", cfg.overlayTtlMs.toFloat(), 1000f, 15000f) {
                repo.update { copy(overlayTtlMs = it.toInt()) }
            }
        }
        item {
            LabeledSlider("Fade in: ${cfg.fadeInMs}ms", cfg.fadeInMs.toFloat(), 0f, 800f) {
                repo.update { copy(fadeInMs = it.toInt()) }
            }
        }
        item {
            LabeledSlider("Fade out: ${cfg.fadeOutMs}ms", cfg.fadeOutMs.toFloat(), 0f, 1200f) {
                repo.update { copy(fadeOutMs = it.toInt()) }
            }
        }
        item {
            LabeledSlider("Show delay jitter: 0–${cfg.showDelayMaxMs}ms", cfg.showDelayMaxMs.toFloat(), 0f, 1000f) {
                repo.update { copy(showDelayMaxMs = it.toInt()) }
            }
        }
        item {
            LabeledSlider("Debounce: ${cfg.debounceMs}ms", cfg.debounceMs.toFloat(), 50f, 2000f) {
                repo.update { copy(debounceMs = it.toInt()) }
            }
        }

        // Stop words
        item { SectionHeader("Stop Words") }
        item { StopWordsEditor(cfg.stopWords) { repo.update { copy(stopWords = it) } } }

        // Excluded apps
        item { SectionHeader("Excluded Apps") }
        item { ExcludedAppsEditor(cfg.excludePackages) { repo.update { copy(excludePackages = it) } } }

        // Reset
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { repo.update { OverlayConfig.DEFAULT } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reset to Defaults") }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatusCard(serviceOn: Boolean, onEnable: () -> Unit) {
    val ctx = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = if (serviceOn) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer
    )) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (serviceOn) "Service is running" else "Service not enabled",
                    fontWeight = FontWeight.Bold,
                    color = if (serviceOn) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer
                )
                if (!serviceOn) Text(
                    "Tap to open Accessibility Settings",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (!serviceOn) {
                TextButton(onClick = onEnable) { Text("Enable") }
            }
        }
    }
}

@Composable
fun BatteryWarningCard() {
    val ctx = LocalContext.current
    if (Build.VERSION.SDK_INT < 23) return
    val pm = ctx.getSystemService(PowerManager::class.java) ?: return
    if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) return
    WarningCard("Battery optimization may stop the service") {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .apply { data = Uri.parse("package:${ctx.packageName}") })
        }
    }
}

@Composable
fun WarningCard(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Text(text, modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 13.sp)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun PresetsRow(onSelect: (OverlayConfig) -> Unit) {
    val presets = listOf(
        "Beginner" to OverlayConfig.PRESET_BEGINNER,
        "Casual" to OverlayConfig.PRESET_CASUAL,
        "Learner" to OverlayConfig.PRESET_LEARNER,
        "Scholar" to OverlayConfig.PRESET_SCHOLAR
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (label, preset) ->
            OutlinedButton(onClick = { onSelect(preset) }, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { idx, label ->
            val isSelected = idx == selected
            Surface(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(idx) },
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(label, fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, modifier = Modifier.padding(bottom = 2.dp))
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max)
    }
}

@Composable
fun PosCheckboxRow(cfg: OverlayConfig, onUpdate: (OverlayConfig.() -> OverlayConfig) -> Unit) {
    val posOptions = listOf(PoS.NOUN to "Nouns", PoS.VERB to "Verbs",
        PoS.ADJECTIVE to "Adj", PoS.ADVERB to "Adv")
    Row(modifier = Modifier.fillMaxWidth()) {
        posOptions.forEach { (pos, label) ->
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = pos in cfg.enabledPos,
                    onCheckedChange = { checked ->
                        onUpdate {
                            val next = if (checked) enabledPos + pos else enabledPos - pos
                            copy(enabledPos = next.ifEmpty { setOf(PoS.NOUN) })
                        }
                    }
                )
                Text(label, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ColorPickerRow(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        items(colors) { c ->
            val color = Color(c)
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(color)
                    .then(if (c == selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                    .clickable { onSelect(c) }
            )
        }
    }
}

@Composable
fun StopWordsEditor(words: Set<String>, onUpdate: (Set<String>) -> Unit) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val sorted = remember(words) { words.sorted() }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Add stop word") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = {
                val w = input.text.trim().lowercase()
                if (w.isNotBlank()) { onUpdate(words + w); input = TextFieldValue("") }
            }) { Icon(Icons.Default.Add, "Add") }
        }
        Spacer(Modifier.height(4.dp))
        sorted.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 2.dp)) {
                row.forEach { word ->
                    InputChip(
                        selected = false, onClick = { onUpdate(words - word) },
                        label = { Text(word, fontSize = 11.sp) },
                        trailingIcon = { Icon(Icons.Default.Delete, "Remove", modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExcludedAppsEditor(packages: Set<String>, onUpdate: (Set<String>) -> Unit) {
    var input by remember { mutableStateOf(TextFieldValue("")) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Package name (e.g. com.app.name)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = {
                val pkg = input.text.trim()
                if (pkg.isNotBlank()) { onUpdate(packages + pkg); input = TextFieldValue("") }
            }) { Icon(Icons.Default.Add, "Add") }
        }
        packages.sorted().forEach { pkg ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(pkg, modifier = Modifier.weight(1f).padding(vertical = 4.dp), fontSize = 12.sp)
                IconButton(onClick = { onUpdate(packages - pkg) }) {
                    Icon(Icons.Default.Delete, "Remove")
                }
            }
        }
    }
}
