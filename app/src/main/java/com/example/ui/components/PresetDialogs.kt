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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeepLinkUtils

@Composable
fun SavePresetDialog(
    preset: Mk20Preset,
    onDismiss: () -> Unit,
    onSave: (name: String, style: String) -> Unit
) {
    var name by remember { mutableStateOf(preset.title) }
    var style by remember { mutableStateOf(preset.style) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PedalCardDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = LedEmerald)
        },
        title = {
            Text("Salvar Preset na Pedaleira", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dê um nome ao seu timbre para salvá-lo no dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Preset") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LedEmerald,
                        unfocusedBorderColor = MetalBorder,
                        focusedLabelColor = LedEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_preset_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = style,
                    onValueChange = { style = it },
                    label = { Text("Estilo / Categoria") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LedEmerald,
                        unfocusedBorderColor = MetalBorder,
                        focusedLabelColor = LedEmerald
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, style) },
                colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("Salvar Preset", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SharePresetDialog(
    preset: Mk20Preset,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onShareApp: () -> Unit,
    onExportJsonFile: () -> Unit
) {
    val shareUrl = remember(preset) { DeepLinkUtils.createShareUrl(preset, useWebScheme = true) }
    val deepLink = remember(preset) { DeepLinkUtils.createShareUrl(preset, useWebScheme = false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PedalCardDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Default.Share, contentDescription = null, tint = LedEmerald)
        },
        title = {
            Text("Compartilhar Preset MK-20", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Gere um link único para que outros guitarristas possam abrir diretamente este timbre na pedaleira:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Link Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C101A))
                        .border(1.dp, MetalBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Link Único do Preset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LedEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shareUrl,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Button(
                    onClick = { onCopyLink(shareUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_copy_link_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar Link Único", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onShareApp,
                    colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_share_system_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartilhar com Amigos / Apps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExportJsonFile,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_export_file_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar como Arquivo .json", color = TextPrimary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = LedEmerald)
            }
        }
    )
}

@Composable
fun SettingsDialog(
    currentApiKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PedalCardDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Default.Key, contentDescription = null, tint = LedEmerald)
        },
        title = {
            Text("Configurações da Gemini IA", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "A pedaleira MK-20 utiliza a IA Gemini 3.5 para analisar músicas, links do YouTube e timbres específicos. Você pode inserir sua própria chave de API aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Chave da API Gemini (Opcional)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LedEmerald,
                        unfocusedBorderColor = MetalBorder,
                        focusedLabelColor = LedEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Caso não informe uma chave, o gerador usará o banco de dados oficial de timbres da MK-20 em modo inteligente.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveKey(apiKeyInput.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                modifier = Modifier.testTag("save_api_key_button")
            ) {
                Text("Salvar Chave", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
