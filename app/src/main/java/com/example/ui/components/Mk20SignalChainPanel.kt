package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Mk20Preset
import com.example.data.model.PedalModule
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

data class Mk20ChainNode(
    val key: String,
    val displayLabel: String,
    val color: Color,
    val module: PedalModule?
)

val MK20_CHAIN_ORDER = listOf(
    "FX", "NS", "DS", "AMP", "CAB",
    "EQ", "MOD", "DELAY", "REVERB", "VOL"
)

/**
 * Painel interativo idêntico ao visor LCD da pedaleira M-Vave MK-20.
 * Exibe o cabeçalho (Slot, VOL.Volume, BPM, CPU, Bateria), a cadeia de 10 blocos em 2 linhas
 * conectadas pelo cabo branco em "S", a moldura branca de seleção no módulo atual,
 * e os botões Select / ON-OFF.
 */
@Composable
fun Mk20SignalChainPanel(
    preset: Mk20Preset,
    selectedPedalKey: String,
    onSelectPedal: (String) -> Unit,
    onToggleSelectedPedal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodes = remember(preset) {
        listOf(
            Mk20ChainNode("FX", "FX", Color(0xFFE04838), preset.pedals.FX),
            Mk20ChainNode("NS", "NS", Color(0xFF1E88F5), preset.pedals.activeNS),
            Mk20ChainNode("DS", "DS", Color(0xFFEA9A28), preset.pedals.DS),
            Mk20ChainNode("AMP", "AMP", Color(0xFF3B598A), preset.pedals.AMP),
            Mk20ChainNode("CAB", "CAB", Color(0xFF5873A0), preset.pedals.CAB),
            Mk20ChainNode("EQ", "EQ", Color(0xFF1CB338), preset.pedals.EQ),
            Mk20ChainNode("MOD", "MOD", Color(0xFF1B63E8), preset.pedals.MOD),
            Mk20ChainNode("DELAY", "DLY", Color(0xFF7B67EE), preset.pedals.DELAY),
            Mk20ChainNode("REVERB", "REV", Color(0xFF4BA3A8), preset.pedals.REVERB),
            Mk20ChainNode("VOL", "VOL", Color(0xFF4A628A), preset.pedals.VOL)
        )
    }

    val row1Nodes = nodes.subList(0, 5)
    val row2Nodes = nodes.subList(5, 10)

    val activeCount = remember(nodes) {
        nodes.count { it.module?.active == true }
    }

    // Cálculo realista de CPU baseado nos módulos ativos
    val cpuLoadFraction = remember(activeCount) {
        (0.28f + (activeCount * 0.055f)).coerceIn(0.25f, 0.88f)
    }

    // Número do slot de 3 dígitos estilo MK-20 (ex: 086)
    val presetSlotNumber = remember(preset.title) {
        val hash = (preset.title.hashCode() and 0x7FFFFFFF) % 99 + 1
        String.format("%03d", hash)
    }

    val volDisplay = remember(preset) {
        preset.pedals.VOL?.params?.get("Level")?.toInt() ?: preset.masterVolume
    }

    val selectedNode = nodes.find { it.key == selectedPedalKey } ?: nodes.first()
    val selectedIndex = nodes.indexOfFirst { it.key == selectedPedalKey }.coerceAtLeast(0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2B3548), Color(0xFF121722))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("mk20_lcd_signal_chain_panel"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF06080E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0D1322), Color(0xFF05070C)),
                        radius = 700f
                    )
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            // =========================================================================
            // 1. TOP BAR DO VISOR MK-20: [086]  [VOL.Volume 85 | BPM 120 | CPU]  [BAT]
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Slot Azul (ex: 086)
                Box(
                    modifier = Modifier
                        .height(54.dp)
                        .width(82.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3366FF), Color(0xFF2249CC))
                            )
                        )
                        .border(1.5.dp, Color(0xFF6B93FF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = presetSlotNumber,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp
                    )
                }

                // Caixa Central (VOL.Volume | BPM | CPU)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141B29))
                        .border(1.8.dp, Color(0xFF3874FF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // VOL.Volume
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "VOL.Volume",
                            color = Color(0xFFD6E4FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "$volDisplay",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                    }

                    // Barra divisória branca vertical
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )

                    // BPM
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "BPM",
                            color = Color(0xFFD6E4FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${preset.bpm}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                    }

                    // Medidor CPU
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Mk20CpuGauge(
                            loadFraction = cpuLoadFraction,
                            modifier = Modifier.size(width = 38.dp, height = 22.dp)
                        )
                        Text(
                            text = "CPU",
                            color = Color(0xFFB8C7E0),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp
                        )
                    }
                }

                // Ícone de Bateria Vertical à Direita
                Mk20BatteryIndicator(
                    modifier = Modifier
                        .width(20.dp)
                        .height(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 2. CADEIA DE SINAL EM 2 LINHAS COM ROTEAMENTO EM "S" IDÊNTICO À MK-20
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
            ) {
                // Canvas traseiro que desenha o cabo branco conectando Linha 1 -> Linha 2
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.2.dp.toPx()
                    val cableColor = Color(0xFFDCEBFF)
                    val row1Y = size.height * 0.23f
                    val midY = size.height * 0.50f
                    val row2Y = size.height * 0.77f

                    val leftMargin = 12.dp.toPx()
                    val rightMargin = size.width - 10.dp.toPx()
                    val eqCenterX = size.width * 0.11f

                    // Seta de entrada '>' na esquerda da Linha 1
                    val arrowSize = 6.dp.toPx()
                    drawLine(
                        color = cableColor,
                        start = Offset(2.dp.toPx(), row1Y - arrowSize),
                        end = Offset(leftMargin, row1Y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = cableColor,
                        start = Offset(2.dp.toPx(), row1Y + arrowSize),
                        end = Offset(leftMargin, row1Y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Linha 1 horizontal (FX -> NS -> DS -> AMP -> CAB -> borda direita)
                    val snakePath = Path().apply {
                        moveTo(leftMargin, row1Y)
                        lineTo(rightMargin, row1Y)
                        // Desce da direita do CAB até o meio entre as duas fileiras
                        lineTo(rightMargin, midY)
                        // Atravessa da direita para a esquerda até a direção do EQ
                        lineTo(eqCenterX, midY)
                        // Desce para entrar no EQ na Linha 2
                        lineTo(eqCenterX, row2Y)
                        // Segue pela Linha 2 (EQ -> MOD -> DLY -> REV -> VOL -> saída à direita)
                        lineTo(rightMargin - 2.dp.toPx(), row2Y)
                    }

                    drawPath(
                        path = snakePath,
                        color = cableColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Seta de saída '-->' na direita da Linha 2
                    val outTipX = rightMargin
                    drawLine(
                        color = cableColor,
                        start = Offset(outTipX - arrowSize * 1.2f, row2Y - arrowSize),
                        end = Offset(outTipX, row2Y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = cableColor,
                        start = Offset(outTipX - arrowSize * 1.2f, row2Y + arrowSize),
                        end = Offset(outTipX, row2Y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Os 10 blocos distribuídos nas 2 fileiras sobre o cabo branco
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Fileira 1: FX, NS, DS, AMP, CAB
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row1Nodes.forEach { node ->
                            Mk20LcdBlockItem(
                                node = node,
                                isSelected = node.key == selectedPedalKey,
                                onClick = { onSelectPedal(node.key) }
                            )
                        }
                    }

                    // Fileira 2: EQ, MOD, DLY, REV, VOL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row2Nodes.forEach { node ->
                            Mk20LcdBlockItem(
                                node = node,
                                isSelected = node.key == selectedPedalKey,
                                onClick = { onSelectPedal(node.key) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 3. BOTÕES INFERIORES DO VISOR MK-20: ( Select )   ( ON-OFF )
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão ( Select ) -> Avança para o próximo módulo na cadeia
                Box(
                    modifier = Modifier
                        .width(106.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0A101D))
                        .border(2.dp, Color(0xFFDCEBFF), RoundedCornerShape(50))
                        .clickable {
                            val nextIndex = (selectedIndex + 1) % nodes.size
                            onSelectPedal(nodes[nextIndex].key)
                        }
                        .testTag("mk20_lcd_select_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select",
                        color = Color(0xFFDCEBFF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Indicador central do módulo focado
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isSelectedActive = selectedNode.module?.active == true
                    Text(
                        text = "${selectedNode.displayLabel} • ${selectedNode.module?.model ?: "Padrão"}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isSelectedActive) "MÓDULO LIGADO (ON)" else "MÓDULO EM BYPASS (OFF)",
                        color = if (isSelectedActive) LedEmerald else Color(0xFFFF7A7A),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Botão ( ON-OFF ) -> Liga/Desliga o módulo atualmente selecionado
                val isCurrentOn = selectedNode.module?.active == true
                Box(
                    modifier = Modifier
                        .width(106.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isCurrentOn) Color(0xFF10243A) else Color(0xFF0A101D)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isCurrentOn) Color(0xFFDCEBFF) else Color(0xFF63738F),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable {
                            onToggleSelectedPedal(selectedNode.key)
                        }
                        .testTag("mk20_lcd_onoff_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ON-OFF",
                        color = if (isCurrentOn) Color(0xFFDCEBFF) else Color(0xFF7B8CA8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Barra de navegação rápida abaixo do visor para alternar entre os 10 módulos ou expandir/recolher.
 */
@Composable
fun Mk20ModuleQuickNavBar(
    selectedPedalKey: String,
    showAllModules: Boolean,
    onSelectPedal: (String) -> Unit,
    onToggleShowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = MK20_CHAIN_ORDER.indexOf(selectedPedalKey).coerceAtLeast(0)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Setas < Anterior | Próximo >
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF141B29),
                border = BorderStroke(1.dp, MetalBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val prevIdx = if (currentIndex - 1 < 0) MK20_CHAIN_ORDER.lastIndex else currentIndex - 1
                        onSelectPedal(MK20_CHAIN_ORDER[prevIdx])
                    }
                    .testTag("mk20_prev_module_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Módulo Anterior",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Anterior",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "${currentIndex + 1}/10",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF141B29),
                border = BorderStroke(1.dp, MetalBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val nextIdx = (currentIndex + 1) % MK20_CHAIN_ORDER.size
                        onSelectPedal(MK20_CHAIN_ORDER[nextIdx])
                    }
                    .testTag("mk20_next_module_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Próximo",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Próximo Módulo",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Botão para alternar entre "1 por vez (Limpo)" e "Ver Todos"
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (showAllModules) LedEmerald.copy(alpha = 0.18f) else Color(0xFF141B29),
            border = BorderStroke(1.dp, if (showAllModules) LedEmerald else MetalBorder),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleShowAll() }
                .testTag("mk20_toggle_view_mode_btn")
        ) {
            Text(
                text = if (showAllModules) "Modo Foco (1 por vez)" else "Ver Todos (10)",
                color = if (showAllModules) LedEmerald else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Renderiza cada um dos 10 blocos da cadeia de sinal exatamente com o formato, cor e ícone
 * do visor original da M-Vave MK-20, mais a moldura branca quando selecionado.
 */
@Composable
private fun Mk20LcdBlockItem(
    node: Mk20ChainNode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isActive = node.module?.active == true
    val blockAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.34f,
        animationSpec = tween(200),
        label = "blockAlpha"
    )

    val blockWidth = when (node.key) {
        "AMP" -> 68.dp
        "CAB" -> 66.dp
        "VOL" -> 42.dp
        else -> 52.dp
    }
    val blockHeight = when (node.key) {
        "AMP" -> 54.dp
        "CAB" -> 64.dp
        "VOL" -> 68.dp
        else -> 62.dp
    }

    // Container externo que desenha a moldura branca de seleção [ ] igual à foto da MK-20
    Box(
        modifier = Modifier
            .size(width = blockWidth + 10.dp, height = blockHeight + 10.dp)
            .clip(RoundedCornerShape(11.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(Color(0x22FFFFFF))
                        .border(2.4.dp, Color(0xFFF0F6FF), RoundedCornerShape(11.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag("mk20_chain_node_${node.key}"),
        contentAlignment = Alignment.Center
    ) {
        when (node.key) {
            "AMP" -> Mk20AmpHeadIcon(
                color = node.color,
                isActive = isActive,
                modifier = Modifier
                    .size(width = blockWidth, height = blockHeight)
                    .alpha(blockAlpha)
            )
            "CAB" -> Mk20CabSpeakerIcon(
                color = node.color,
                isActive = isActive,
                modifier = Modifier
                    .size(width = blockWidth, height = blockHeight)
                    .alpha(blockAlpha)
            )
            "VOL" -> Mk20VolPedalIcon(
                color = node.color,
                isActive = isActive,
                modifier = Modifier
                    .size(width = blockWidth, height = blockHeight)
                    .alpha(blockAlpha)
            )
            else -> Mk20StandardStompIcon(
                key = node.key,
                label = node.displayLabel,
                color = node.color,
                isActive = isActive,
                modifier = Modifier
                    .size(width = blockWidth, height = blockHeight)
                    .alpha(blockAlpha)
            )
        }
    }
}

/**
 * Pedal compacto padrão da MK-20 (FX, NS, DS, EQ, MOD, DLY, REV)
 * Parte superior com ícone gráfico do efeito + linha divisória + sigla embaixo.
 */
@Composable
private fun Mk20StandardStompIcon(
    key: String,
    label: String,
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) color else Color(0xFF283244)
    val borderColor = if (isActive) Color.White.copy(alpha = 0.65f) else Color(0xFF4B5870)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .border(1.3.dp, borderColor, RoundedCornerShape(7.dp))
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Visor gráfico superior do pedal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.14f))
                .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                drawStompSymbol(key)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Sigla inferior (FX, NS, DS, EQ, MOD, DLY, REV)
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            lineHeight = 14.sp
        )
    }
}

private fun DrawScope.drawStompSymbol(key: String) {
    val fg = Color.White.copy(alpha = 0.85f)
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    when (key) {
        "FX" -> {
            // Anel circular pontilhado igual ao pedal FX vermelho da foto
            drawCircle(
                color = fg,
                radius = minOf(w, h) * 0.38f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        "NS" -> {
            // Eixos + curva de gate igual ao pedal NS azul claro
            drawLine(
                color = fg.copy(alpha = 0.45f),
                start = Offset(w * 0.15f, cy),
                end = Offset(w * 0.85f, cy),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = fg.copy(alpha = 0.45f),
                start = Offset(cx, h * 0.15f),
                end = Offset(cx, h * 0.85f),
                strokeWidth = 1.dp.toPx()
            )
            val path = Path().apply {
                moveTo(w * 0.15f, h * 0.7f)
                quadraticTo(w * 0.35f, h * 0.15f, cx, cy)
                quadraticTo(w * 0.65f, h * 0.85f, w * 0.85f, h * 0.3f)
            }
            drawPath(path, color = fg, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
        }
        "DS" -> {
            // 6 pontos (2 linhas x 3 colunas) igual ao pedal DS laranja da foto
            val cols = listOf(w * 0.24f, w * 0.50f, w * 0.76f)
            val rows = listOf(h * 0.33f, h * 0.68f)
            rows.forEach { ry ->
                cols.forEach { cxDot ->
                    drawCircle(
                        color = fg,
                        radius = 2.8.dp.toPx(),
                        center = Offset(cxDot, ry)
                    )
                }
            }
        }
        "EQ" -> {
            // 3 sliders verticais igual ao pedal EQ verde da foto
            val xs = listOf(w * 0.26f, w * 0.50f, w * 0.74f)
            val knobYs = listOf(h * 0.35f, h * 0.62f, h * 0.42f)
            xs.forEachIndexed { idx, xPos ->
                drawLine(
                    color = fg.copy(alpha = 0.7f),
                    start = Offset(xPos, h * 0.15f),
                    end = Offset(xPos, h * 0.85f),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.4.dp.toPx(),
                    center = Offset(xPos, knobYs[idx])
                )
            }
        }
        "MOD" -> {
            // Círculos duplos de Chorus igual ao pedal MOD azul
            val r = minOf(w, h) * 0.34f
            drawCircle(
                color = fg,
                radius = r,
                center = Offset(cx - 2.dp.toPx(), cy),
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawCircle(
                color = fg.copy(alpha = 0.75f),
                radius = r,
                center = Offset(cx + 2.dp.toPx(), cy),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
        "DELAY" -> {
            // Círculos de eco decrescentes da esquerda para a direita igual ao DLY roxo
            drawCircle(
                color = fg,
                radius = minOf(w, h) * 0.30f,
                center = Offset(w * 0.30f, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = fg.copy(alpha = 0.8f),
                radius = minOf(w, h) * 0.21f,
                center = Offset(w * 0.56f, cy),
                style = Stroke(width = 1.3.dp.toPx())
            )
            drawCircle(
                color = fg.copy(alpha = 0.65f),
                radius = minOf(w, h) * 0.13f,
                center = Offset(w * 0.78f, cy),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
        "REVERB" -> {
            // Anéis concêntricos igual ao pedal REV verde-água
            val maxR = minOf(w, h) * 0.38f
            drawCircle(
                color = fg,
                radius = maxR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.4.dp.toPx())
            )
            drawCircle(
                color = fg.copy(alpha = 0.8f),
                radius = maxR * 0.64f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.3.dp.toPx())
            )
            drawCircle(
                color = fg.copy(alpha = 0.65f),
                radius = maxR * 0.30f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * Ícone do cabeçote AMP idêntico ao visor da MK-20:
 * Alça superior, painel com emblema vermelho à esquerda + 3 knobs brancos, e "AMP" embaixo.
 */
@Composable
private fun Mk20AmpHeadIcon(
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val bodyColor = if (isActive) color else Color(0xFF283244)
    val borderColor = if (isActive) Color(0xFFB5CBE8) else Color(0xFF4B5870)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Alça superior do cabeçote
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(borderColor)
        )

        // Gabinete do cabeçote
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(7.dp))
                .background(bodyColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(7.dp))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Painel frontal com gota vermelha + 3 knobs brancos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(21.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF233757))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Emblema/gota vermelha à esquerda
                Box(
                    modifier = Modifier
                        .size(width = 9.dp, height = 13.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color(0xFFE53935) else Color(0xFF757575))
                )
                // 3 Knobs brancos
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEAF2FF))
                    )
                }
            }

            Text(
                text = "AMP",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * Ícone da caixa acústica CAB idêntico ao visor da MK-20:
 * Alça no topo, tela treliçada (diamond grill cloth) na metade superior e "CAB" na base.
 */
@Composable
private fun Mk20CabSpeakerIcon(
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val bodyColor = if (isActive) color else Color(0xFF283244)
    val borderColor = if (isActive) Color(0xFFC2D5F0) else Color(0xFF4B5870)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Alça superior do gabinete
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(borderColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(7.dp))
                .background(bodyColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(7.dp))
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Treliça diagonal estilo caixa 4x12
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF324669))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 7.dp.toPx()
                    val meshColor = Color.White.copy(alpha = 0.65f)
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = meshColor,
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = 1.1.dp.toPx()
                        )
                        drawLine(
                            color = meshColor,
                            start = Offset(x + size.height, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.1.dp.toPx()
                        )
                        x += step
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "CAB",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * Ícone do pedal de expressão/volume VOL idêntico ao visor da MK-20:
 * Formato retangular vertical com borda pontilhada e texto "V / O / L" na vertical.
 */
@Composable
private fun Mk20VolPedalIcon(
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val bodyColor = if (isActive) color else Color(0xFF283244)
    val borderColor = if (isActive) Color(0xFFE0B6B1) else Color(0xFF4B5870)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bodyColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        // Borda interna pontilhada do pedal de volume
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.65f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(
                    width = 1.3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "V",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp
            )
            Text(
                text = "O",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp
            )
            Text(
                text = "L",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp
            )
        }
    }
}

/**
 * Medidor semicircular de CPU idêntico ao topo direito do visor da MK-20.
 */
@Composable
private fun Mk20CpuGauge(
    loadFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, (size.height - strokeWidth / 2f) * 1.7f)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        // Faixa Azul (0% a 72%)
        drawArc(
            color = Color(0xFF3993FF),
            startAngle = 180f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Faixa Laranja/Vermelha (72% a 100%)
        drawArc(
            color = Color(0xFFE86B32),
            startAngle = 310f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Ponteiro branco
        val angleDeg = 180f + (loadFraction.coerceIn(0.1f, 0.95f) * 180f)
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val center = Offset(size.width / 2f, size.height * 0.92f)
        val needleLen = size.width * 0.36f
        val tip = Offset(
            x = center.x + (cos(angleRad) * needleLen).toFloat(),
            y = center.y + (sin(angleRad) * needleLen).toFloat()
        )
        drawLine(
            color = Color.White,
            start = center,
            end = tip,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Ícone de bateria vertical no canto superior direito do visor da MK-20.
 */
@Composable
private fun Mk20BatteryIndicator(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val capWidth = w * 0.46f
        val capHeight = 3.5.dp.toPx()
        val bodyTop = capHeight + 1.dp.toPx()
        val bodyHeight = h - bodyTop

        // Polo positivo superior
        drawRoundRect(
            color = Color(0xFFDCEBFF),
            topLeft = Offset((w - capWidth) / 2f, 0f),
            size = Size(capWidth, capHeight),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )

        // Corpo externo da bateria
        drawRoundRect(
            color = Color(0xFFDCEBFF),
            topLeft = Offset(1.dp.toPx(), bodyTop),
            size = Size(w - 2.dp.toPx(), bodyHeight),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Preenchimento interno da bateria
        val pad = 4.dp.toPx()
        drawRoundRect(
            color = Color(0xFFC8E0FF),
            topLeft = Offset(pad, bodyTop + pad),
            size = Size(w - pad * 2f, bodyHeight - pad * 2f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }
}
