package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PedalModule
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedBlue
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.LedIndigo
import com.example.ui.theme.LedOff
import com.example.ui.theme.LedPink
import com.example.ui.theme.LedPurple
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.MetalBorderHighlight
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.PedalCardSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PedalStompCard(
    pedalKey: String,
    module: PedalModule,
    onToggleActive: () -> Unit,
    onParamChange: (String, Float) -> Unit,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = remember(pedalKey) {
        when (pedalKey) {
            "FX" -> LedPink
            "NS", "NR" -> LedPurple
            "DS" -> LedAmber
            "AMP" -> LedCyan
            "CAB" -> LedEmerald
            "EQ" -> LedCyan
            "MOD" -> LedBlue
            "DELAY" -> LedIndigo
            "REVERB" -> LedPink
            "VOL" -> LedEmerald
            else -> LedEmerald
        }
    }

    val ledColor by animateColorAsState(
        targetValue = if (module.active) themeColor else LedOff,
        label = "ledColor"
    )

    var modelMenuExpanded by remember { mutableStateOf(false) }

    val availableModels = remember(pedalKey) {
        when (pedalKey) {
            "FX" -> com.example.data.model.Mk20Catalog.FX_MODELS.map { it.name }
            "NS", "NR" -> com.example.data.model.Mk20Catalog.NS_MODELS.map { it.name }
            "DS" -> com.example.data.model.Mk20Catalog.DS_MODELS.map { it.name }
            "AMP" -> com.example.data.model.Mk20Catalog.AMP_MODELS.map { it.name }
            "CAB" -> com.example.data.model.Mk20Catalog.CAB_MODELS.map { it.name } + listOf("User IR / Bypass")
            "EQ" -> com.example.data.model.Mk20Catalog.EQ_MODELS.map { it.name }
            "MOD" -> com.example.data.model.Mk20Catalog.MOD_MODELS.map { it.name }
            "DELAY" -> com.example.data.model.Mk20Catalog.DELAY_MODELS.map { it.name }
            "REVERB" -> com.example.data.model.Mk20Catalog.REVERB_MODELS.map { it.name }
            "VOL" -> listOf("Master", "Output Boost", "Linear Vol")
            else -> listOf(module.model)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pedal_card_$pedalKey")
            .shadow(if (module.active) 8.dp else 2.dp, RoundedCornerShape(16.dp))
            .border(
                width = if (module.active) 1.5.dp else 1.dp,
                color = if (module.active) themeColor.copy(alpha = 0.6f) else MetalBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = PedalCardDark
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PedalCardDark,
                            PedalCardSurface
                        )
                    )
                )
                .padding(14.dp)
        ) {
            // Header Row: Pedal Name, LED Status Light, Bypass Stomp Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // LED indicator light with glow ring
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (module.active) {
                                    Brush.radialGradient(
                                        listOf(Color.White, ledColor)
                                    )
                                } else {
                                    Brush.radialGradient(listOf(LedOff, Color(0xFF1E2530)))
                                }
                            )
                            .border(1.dp, Color(0xFF333F50), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = pedalKey,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = themeColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Stomp switch button (Toggle bypass/on)
                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (module.active) listOf(Color(0xFF2C394B), Color(0xFF151C26))
                                else listOf(Color(0xFF1F2530), Color(0xFF0F141C))
                            )
                        )
                        .border(
                            1.5.dp,
                            if (module.active) themeColor else MetalBorderHighlight,
                            CircleShape
                        )
                        .testTag("pedal_toggle_$pedalKey")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Alternar ${module.name}",
                        tint = if (module.active) themeColor else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val currentCatalogItem = remember(pedalKey, module.model) {
                com.example.data.model.Mk20Catalog.findItem(pedalKey, module.model)
            }

            // Model Selector Dropdown Chip
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10141D))
                        .border(1.dp, MetalBorder, RoundedCornerShape(8.dp))
                        .clickable { modelMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = module.model,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (module.active) themeColor else TextMuted,
                            fontSize = 12.sp
                        )
                        if (currentCatalogItem != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${currentCatalogItem.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Selecionar Modelo",
                        tint = if (module.active) themeColor else TextMuted
                    )
                }

                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false },
                    modifier = Modifier
                        .background(Color(0xFF161B26))
                        .heightIn(max = 420.dp)
                ) {
                    availableModels.forEach { modelOption ->
                        val catalogItem = com.example.data.model.Mk20Catalog.findItem(pedalKey, modelOption)
                        val isSelected = modelOption == module.model

                        DropdownMenuItem(
                            text = {
                                if (catalogItem != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = catalogItem.name,
                                                color = if (isSelected) themeColor else TextPrimary,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = themeColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = catalogItem.type,
                                                        color = themeColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = themeColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = catalogItem.description,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = modelOption,
                                            color = if (isSelected) themeColor else TextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = themeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onModelChange(modelOption)
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Parameter Controls (Knobs)
            if (module.params.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = if (module.params.size >= 4) 4 else 3
                ) {
                    module.params.forEach { (paramName, paramVal) ->
                        val isMs = paramName.equals("Time", ignoreCase = true) ||
                            paramName.startsWith("Time ", ignoreCase = true)
                        val isSyncBpm = paramName.equals("SyncBPM", ignoreCase = true) ||
                            paramName.equals("Sync BPM", ignoreCase = true)
                        val isToggle = paramName.equals("Sync", ignoreCase = true) ||
                            paramName.equals("inputDetect", ignoreCase = true) ||
                            paramName.equals("LPtype", ignoreCase = true) ||
                            paramName.equals("boost", ignoreCase = true) ||
                            (pedalKey == "DS" && module.model.equals("MW-XEP", ignoreCase = true) && paramName.equals("bright", ignoreCase = true))
                        val isStep2 = (pedalKey == "DS" && !module.model.equals("MW-EQP", ignoreCase = true) && (paramName.equals("mode", ignoreCase = true) || paramName.equals("gate mode", ignoreCase = true))) ||
                            (pedalKey == "MOD" && paramName.equals("stage", ignoreCase = true))
                        val isStep3 = (pedalKey == "DS" && module.model.equals("MW-EQP", ignoreCase = true) && paramName.equals("mode", ignoreCase = true)) ||
                            (pedalKey == "DS" && module.model.equals("MW-B7000", ignoreCase = true) && (
                                paramName.equals("grunt", ignoreCase = true) ||
                                paramName.equals("attack", ignoreCase = true) ||
                                paramName.equals("lomid freq", ignoreCase = true) ||
                                paramName.equals("himid freq", ignoreCase = true)
                            ))
                        val isStep6 = pedalKey == "DS" && module.model.equals("MW-HRZ", ignoreCase = true) && paramName.equals("attack", ignoreCase = true)
                        val isModLfo5 = pedalKey == "MOD" && paramName.equals("lfo", ignoreCase = true)
                        val isModChorusMode6 = pedalKey == "MOD" && module.model.equals("CHORUS STEREO", ignoreCase = true) && paramName.equals("mode", ignoreCase = true)
                        val isModTremoloMode3 = pedalKey == "MOD" && module.model.equals("TREMOLO STEREO", ignoreCase = true) && paramName.equals("mode", ignoreCase = true)
                        val isFxWhammyMode3 = pedalKey == "FX" && module.model.equals("Whammy", ignoreCase = true) && paramName.equals("mode", ignoreCase = true)
                        val isModBits = pedalKey == "MOD" && paramName.equals("bits", ignoreCase = true)
                        val isFxLofiBit = pedalKey == "FX" && paramName.equals("Bit", ignoreCase = true)
                        val isFxHighPitch = pedalKey == "FX" && paramName.equals("High Pitch", ignoreCase = true)
                        val isFxLowPitch = pedalKey == "FX" && paramName.equals("Low Pitch", ignoreCase = true)
                        val isFxSemi = pedalKey == "FX" && paramName.equals("Semi", ignoreCase = true)
                        val isFxDecimal10 = pedalKey == "FX" && (
                            paramName.equals("Speed", ignoreCase = true) ||
                            paramName.equals("Sense", ignoreCase = true) ||
                            paramName.equals("Sustain", ignoreCase = true) ||
                            paramName.equals("Ratio", ignoreCase = true) ||
                            (paramName.equals("Gain", ignoreCase = true) && module.model.contains("Boost", ignoreCase = true))
                        )
                        val isModSpeed16 = pedalKey == "MOD" && paramName.equals("speed", ignoreCase = true) && (
                            module.model.equals("PHASER STEREO", ignoreCase = true) ||
                            module.model.equals("FLANGER STEREO", ignoreCase = true) ||
                            module.model.equals("VIBE STEREO", ignoreCase = true)
                        )
                        val isModSpeed10 = pedalKey == "MOD" && paramName.equals("speed", ignoreCase = true) && !isModSpeed16
                        val isEqBand = pedalKey == "EQ"
                        val isDelayPitch = pedalKey == "DELAY" && paramName.equals("Pitch", ignoreCase = true)
                        val isGateThd = (pedalKey == "NS" || pedalKey == "NR") &&
                            paramName.equals("thd", ignoreCase = true) &&
                            (module.model.contains("gate", ignoreCase = true) || paramVal < 0f)
                        val isCabLowCut = pedalKey == "CAB" && paramName.equals("Low Cut", ignoreCase = true)
                        val isCabHighCut = pedalKey == "CAB" && paramName.equals("High Cut", ignoreCase = true)

                        val paramRange = when {
                            isMs -> 100f..2000f
                            isSyncBpm -> 40f..240f
                            isToggle -> 0f..1f
                            isStep2 -> 1f..2f
                            isStep3 || isModTremoloMode3 || isFxWhammyMode3 -> 1f..3f
                            isModLfo5 -> 1f..5f
                            isStep6 || isModChorusMode6 -> 1f..6f
                            isModBits -> 4f..24f
                            isFxLofiBit -> 2f..24f
                            isFxHighPitch -> 0f..24f
                            isFxLowPitch -> -24f..0f
                            isFxSemi -> -12f..12f
                            isFxDecimal10 -> 0f..10f
                            isModSpeed16 -> 0.17f..16f
                            isModSpeed10 -> 0.1f..10f
                            isEqBand -> -12f..12f
                            isDelayPitch -> 0f..12f
                            isGateThd -> -80f..-30f
                            isCabLowCut -> 20f..300f
                            isCabHighCut -> 5000f..18000f
                            else -> 0f..100f
                        }
                        val unit = when {
                            isMs -> "ms"
                            isSyncBpm -> " BPM"
                            isToggle -> "ON/OFF"
                            isStep2 -> "/2"
                            isStep3 -> "/3"
                            isStep6 -> "/6"
                            isModLfo5 -> "LFO_WAVE"
                            isModChorusMode6 -> "CHORUS_MODE"
                            isModTremoloMode3 -> "TREM_MODE"
                            isFxWhammyMode3 -> "WHAMMY_MODE"
                            isModBits || isFxLofiBit -> "bit"
                            isFxHighPitch || isFxLowPitch || isFxSemi || isDelayPitch -> "st"
                            isFxDecimal10 || isModSpeed16 || isModSpeed10 -> "DEC10"
                            isEqBand -> "dB_EQ"
                            isGateThd -> "dB"
                            isCabLowCut || isCabHighCut -> "Hz"
                            else -> "%"
                        }

                        KnobControl(
                            label = paramName,
                            value = paramVal.coerceIn(paramRange.start, paramRange.endInclusive),
                            onValueChange = { newVal -> onParamChange(paramName, newVal) },
                            range = paramRange,
                            activeColor = if (module.active) themeColor else TextMuted,
                            unit = unit,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
