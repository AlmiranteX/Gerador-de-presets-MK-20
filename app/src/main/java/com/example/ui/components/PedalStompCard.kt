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
            "NR" -> LedPurple
            "DS" -> LedAmber
            "AMP" -> LedCyan
            "CAB" -> LedEmerald
            "MOD" -> LedBlue
            "DELAY" -> LedIndigo
            "REVERB" -> LedPink
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
            "NR" -> listOf("Noise Gate")
            "DS" -> listOf(
                "1BLUES_OD", "2TS8", "3DS1", "4DS2", "5M-VAVE_OD", "6M-VAVE_DS",
                "7M-VAVE_TS1", "8M-VAVE_TS2", "9SUPA_1", "10SUPA_2", "11RAT", "12RAT_BT",
                "13JHS_1", "14JHS_2", "15MT_1", "16MT_2", "20HIGAIN", "22TS-9",
                "23BIG-DR", "24CL_BOOST", "31BOG_DS", "36SuperOD", "37BLUES_DR", "39BIG-MUFF"
            )
            "AMP" -> listOf(
                "26CL2_JC120", "25CL1_JC120", "13CL1_FenDi", "14CL2_FenDi", "15CL3_FenDi",
                "1CL1_410H", "2CL2_410H", "3CR1_410H", "4CR2_410H", "5CR3_410H",
                "6OD1_410H", "7OD2_410H", "8OD3_410H", "34OD2_JVMcrunch", "86OD_JCM800",
                "87DS_JCM800", "51OD1_MS2203", "19CL1_J900", "22OD1_J900", "23DS1_J900",
                "27OD1_JHS", "28OD2_JHS", "30DS_JHS", "74OD_ENGL 120", "75DS_ENGL 120",
                "76CL_SoldSolo100", "77OD_SoldSolo100", "78DS_SoldSolo100", "79OD_5150III",
                "80DS_5150III", "81CL_VOSAC30", "82OD_VOSAC30", "83CL_DiezzlVH4",
                "85DS_DiezzlVH4", "88OD_Peavev6505", "89DS_Peavev6505", "18HVMetal _1",
                "48CH1_ME", "49CH2_ME", "64DS1_XC", "102ApSVT_BS", "91AgB659_BS", "94Ap50_BS"
            )
            "CAB" -> listOf(
                "20FdTwRev212A", "21FdTwRev212B", "11FdTR65R212B", "10FdTR65R212A",
                "13FdDelux112A", "16FdDTweed112", "17FdPrinc110A", "61RJ120azz212",
                "1BogUber412A", "2BogUber412B", "4BogUber412D", "8EngXXL412", "9EvStright412",
                "33Mar60A412A", "36Mar60B412A", "46MbOS412A", "57MbTradS412B", "58OgPPC412A",
                "22Fman212A", "26HesModn212A", "64VAC30TBC212", "66ZillCab212",
                "71GisonHubird", "74TaylrOcE", "80AmpgSVT810", "82Bareface110", "90GK410RB410B"
            )
            "MOD" -> listOf("Dimensional Chorus", "Vintage Flanger", "Phase 90", "Optical Tremolo", "Uni-Vibe")
            "DELAY" -> listOf("Analog Delay", "Digital Delay", "Tape Echo", "Dotted Eighth", "Ping Pong")
            "REVERB" -> listOf("Plate Reverb", "Shimmer Reverb", "Hall Reverb", "Spring 63", "Room Reverb")
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
                    Text(
                        text = module.model,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (module.active) TextPrimary else TextMuted,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Selecionar Modelo",
                        tint = if (module.active) themeColor else TextMuted
                    )
                }

                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF161B26))
                ) {
                    availableModels.forEach { modelOption ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = modelOption,
                                        color = if (modelOption == module.model) themeColor else TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = if (modelOption == module.model) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (modelOption == module.model) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
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
                    maxItemsInEachRow = 3
                ) {
                    module.params.forEach { (paramName, paramVal) ->
                        val isMs = paramName.equals("Time", ignoreCase = true)
                        val maxRange = if (isMs) 1000f else 100f
                        val unit = if (isMs) "ms" else "%"

                        KnobControl(
                            label = paramName,
                            value = paramVal,
                            onValueChange = { newVal -> onParamChange(paramName, newVal) },
                            range = 0f..maxRange,
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
