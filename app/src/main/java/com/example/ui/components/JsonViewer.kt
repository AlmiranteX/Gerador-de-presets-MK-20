package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Mk20Preset
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PresetViewModel

@Composable
fun JsonViewer(
    preset: Mk20Preset,
    exportSizeMode: PresetViewModel.ExportSizeMode,
    loadedTemplateName: String?,
    loadedTemplateSize: Int?,
    onSelectSizeMode: (PresetViewModel.ExportSizeMode) -> Unit,
    onCopyJson: () -> Unit,
    onShareLink: () -> Unit,
    onExportMk20: () -> Unit,
    onExportAmk20: () -> Unit,
    onExportJson: () -> Unit,
    onImportFile: () -> Unit,
    onImportTemplate: () -> Unit,
    onClearTemplate: () -> Unit,
    getExportBytes: (Boolean) -> ByteArray,
    modifier: Modifier = Modifier
) {
    val jsonContent = remember(preset) { preset.toJsonString(indent = 2) }
    val binaryBytes = remember(preset, exportSizeMode, loadedTemplateSize) { getExportBytes(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PedalCardDark)
            .border(1.dp, MetalBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = LedEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXPORTAÇÃO BINÁRIA MK-20",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
            }

            // Badge com tamanho exato
            Surface(
                color = if (exportSizeMode == PresetViewModel.ExportSizeMode.TEMPLATE_CLONE) Color(0xFF132A1C) else Color(0xFF0D1D16),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LedEmerald.copy(alpha = 0.6f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (exportSizeMode == PresetViewModel.ExportSizeMode.TEMPLATE_CLONE) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "${binaryBytes.size} bytes (.mk20)",
                        color = LedEmerald,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Seletor de Perfil de Tamanho de Arquivo para a M-EFCS
        Text(
            text = "Formato de Tamanho para M-EFCS / MK-20:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Opção 1: 412 bytes oficial MK-20 (Recomendado para M-EFCS)
            FilterChip(
                selected = exportSizeMode == PresetViewModel.ExportSizeMode.AUTHENTIC_412,
                onClick = { onSelectSizeMode(PresetViewModel.ExportSizeMode.AUTHENTIC_412) },
                label = { Text("412B Oficial MK-20 (Recomendado)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LedEmerald.copy(alpha = 0.25f),
                    selectedLabelColor = LedEmerald
                ),
                modifier = Modifier.testTag("size_mode_412")
            )

            // Opção 2: Molde do pedal do usuário
            FilterChip(
                selected = exportSizeMode == PresetViewModel.ExportSizeMode.TEMPLATE_CLONE,
                onClick = {
                    if (loadedTemplateName != null) {
                        onSelectSizeMode(PresetViewModel.ExportSizeMode.TEMPLATE_CLONE)
                    } else {
                        onImportTemplate()
                    }
                },
                label = {
                    val labelText = if (loadedTemplateName != null) {
                        "Molde do Pedal: $loadedTemplateName (${loadedTemplateSize ?: binaryBytes.size}B)"
                    } else {
                        "+ Usar Molde do Meu MK-20"
                    }
                    Text(labelText, fontSize = 11.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LedCyan.copy(alpha = 0.25f),
                    selectedLabelColor = LedCyan
                ),
                modifier = Modifier.testTag("size_mode_template")
            )

            // Opção 3: 512 bytes padrão alternativo
            FilterChip(
                selected = exportSizeMode == PresetViewModel.ExportSizeMode.M_EFCS_512,
                onClick = { onSelectSizeMode(PresetViewModel.ExportSizeMode.M_EFCS_512) },
                label = { Text("512B M-EFCS Alternativo", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LedAmber.copy(alpha = 0.25f),
                    selectedLabelColor = LedAmber
                ),
                modifier = Modifier.testTag("size_mode_512")
            )

            // Opção 4: 256 bytes compacto
            FilterChip(
                selected = exportSizeMode == PresetViewModel.ExportSizeMode.COMPACT_256,
                onClick = { onSelectSizeMode(PresetViewModel.ExportSizeMode.COMPACT_256) },
                label = { Text("256B Compacto", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LedAmber.copy(alpha = 0.25f),
                    selectedLabelColor = LedAmber
                ),
                modifier = Modifier.testTag("size_mode_256")
            )

            // Opção 5: 1024 bytes expandido
            FilterChip(
                selected = exportSizeMode == PresetViewModel.ExportSizeMode.EXPANDED_1024,
                onClick = { onSelectSizeMode(PresetViewModel.ExportSizeMode.EXPANDED_1024) },
                label = { Text("1024B Expandido", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TextSecondary.copy(alpha = 0.25f),
                    selectedLabelColor = TextPrimary
                ),
                modifier = Modifier.testTag("size_mode_1024")
            )
        }

        // Cartão Informativo de Molde / Compatibilidade
        if (loadedTemplateName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF0F1E19),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LedEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Molde Ativo: $loadedTemplateName",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = LedEmerald,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Garante compatibilidade exata de bytes com a versão do seu M-EFCS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onClearTemplate,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Remover", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color(0xFF141926),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = LedCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dica: Para 100% de precisão, exporte um preset pelo 'Save to' no M-EFCS e use o botão 'Carregar Molde'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row (Primary Export .mk20, Molde .mk20, Export .amk20, Import, Copy, Share)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary .mk20 binary export
            ElevatedButton(
                onClick = onExportMk20,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = LedEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("export_mk20_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar .mk20 (M-EFCS)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Botão Carregar Molde .mk20 da M-EFCS
            OutlinedButton(
                onClick = onImportTemplate,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LedCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, LedCyan.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("load_template_button")
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Carregar Molde .mk20", fontSize = 12.sp)
            }

            // Secondary .amk20 export
            OutlinedButton(
                onClick = onExportAmk20,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                modifier = Modifier.testTag("export_amk20_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar .amk20", fontSize = 12.sp)
            }

            // Universal import button (.mk20 / .amk20 / .json)
            OutlinedButton(
                onClick = onImportFile,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                modifier = Modifier.testTag("import_preset_file_button")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importar Arquivo", fontSize = 12.sp)
            }

            // Copy JSON
            ElevatedButton(
                onClick = onCopyJson,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF1F2937),
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("copy_json_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar JSON", fontSize = 12.sp)
            }

            // Share link
            ElevatedButton(
                onClick = onShareLink,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF1F2937),
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("share_preset_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartilhar Link", fontSize = 12.sp)
            }

            // Backup JSON export
            OutlinedButton(
                onClick = onExportJson,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                modifier = Modifier.testTag("export_json_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Backup .json", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visualização JSON
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = LedCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Visualização JSON",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = LedCyan,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF080C14))
                .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()

            Text(
                text = jsonContent,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = LedEmerald.copy(alpha = 0.9f),
                lineHeight = 16.sp,
                modifier = Modifier
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
            )
        }
    }
}
