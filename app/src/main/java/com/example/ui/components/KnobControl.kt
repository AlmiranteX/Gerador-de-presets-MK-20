package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.MetalBorderHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun KnobControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF131822), shape = MaterialTheme.shapes.small)
            .border(1.dp, MetalBorder, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            val formattedValue = when {
                unit == "ON/OFF" ||
                    label.equals("Sync", ignoreCase = true) ||
                    label.equals("inputDetect", ignoreCase = true) ||
                    label.equals("LPtype", ignoreCase = true) ||
                    label.equals("boost", ignoreCase = true) -> if (value.toInt() == 0) "OFF" else "ON"
                unit == "LFO_WAVE" -> when (value.toInt()) {
                    1 -> "sin"
                    2 -> "sqw"
                    3 -> "saw"
                    4 -> "tri"
                    5 -> "wav"
                    else -> "sin"
                }
                unit == "CHORUS_MODE" -> when (value.toInt()) {
                    1 -> "harm"
                    2 -> "sin"
                    3 -> "sqw"
                    4 -> "saw"
                    5 -> "tri"
                    6 -> "wav"
                    else -> "harm"
                }
                unit == "TREM_MODE" -> when (value.toInt()) {
                    1 -> "sin"
                    2 -> "tube"
                    3 -> "phtn"
                    else -> "phtn"
                }
                unit == "WHAMMY_MODE" -> when (value.toInt()) {
                    1 -> "mbody"
                    2 -> "oct1"
                    3 -> "oct2"
                    else -> "oct1"
                }
                unit == "DEC10" -> String.format(java.util.Locale.US, "%.1f", value)
                unit == "dB_EQ" -> {
                    val sign = if (value > 0.05f) "+" else ""
                    String.format(java.util.Locale.US, "%s%.1fdB", sign, value)
                }
                unit == "ms" -> String.format(java.util.Locale.US, "%.3fs", value / 1000f)
                else -> "${value.toInt()}$unit"
            }
            Text(
                text = formattedValue,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = activeColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Tactile mini knob indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF333D4F), Color(0xFF181F2C))
                    )
                )
                .border(1.5.dp, MetalBorderHighlight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = Color(0xFF232A38)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )
    }
}
