package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Mk20Preset
import com.example.data.model.ToneAssistantMessage
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.LedEmeraldDark
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.MetalBorderHighlight
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.PedalCardSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ToneAssistantTab(
    currentPreset: Mk20Preset,
    messages: List<ToneAssistantMessage>,
    isThinking: Boolean,
    isProUnlocked: Boolean,
    dailyUsed: Int,
    maxDailyFree: Int,
    onSendMessage: (String) -> Unit,
    onApplyToPedalboard: (Mk20Preset) -> Unit,
    onSavePreset: (Mk20Preset) -> Unit,
    onExportPreset: (Mk20Preset) -> Unit,
    onClearConversation: () -> Unit,
    onOpenUpgradeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Rola para a mensagem mais recente quando novas mensagens chegam
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val remainingQuota = (maxDailyFree - dailyUsed).coerceAtLeast(0)

    val quickSuggestions = listOf(
        "🎸 Amaciar agudos (Strato/Tele)",
        "🔥 Mais ganho / Modo Solo",
        "⚡ Refrão Pesado com Alto Ganho",
        "✂️ Som mais seco (Menos Reverb)",
        "🌊 Mais ambiência (Chorus + Shimmer)",
        "🧼 Limpar graves embolados",
        "🎸 Som Clean de Arpejo Cristalino",
        "⚡ Tirar chiado (Apertar Gate)"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .imePadding()
    ) {
        // 1. Cabeçalho do Assistente de Timbre
        Surface(
            color = PedalCardDark,
            border = BorderStroke(1.dp, MetalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LedEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LedEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Assistente de Timbre",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = LedEmerald.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ROADIE IA",
                                        color = LedEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Ajusta botões e parâmetros oficiais da MK-20 em tempo real",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Botão de reiniciar conversa
                    IconButton(
                        onClick = onClearConversation,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_assistant_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpar conversa",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Indicador do Preset Base que está sendo ajustado
                Surface(
                    color = PedalCardSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MetalBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = LedCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timbre em Edição: ",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = currentPreset.title.ifBlank { "Preset MK-20" },
                            color = LedCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val ampName = currentPreset.pedals.AMP?.model ?: "AMP"
                        Text(
                            text = "[$ampName]",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 2. Chips de Sugestões Rápidas (Carrossel Horizontal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSuggestions.forEach { suggestion ->
                Surface(
                    onClick = {
                        onSendMessage(suggestion)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = PedalCardDark,
                    border = BorderStroke(1.dp, MetalBorderHighlight.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = suggestion,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3. Lista de Mensagens do Chat
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    UserMessageBubble(message = msg)
                } else {
                    AssistantMessageBubble(
                        message = msg,
                        onApplyToPedalboard = onApplyToPedalboard,
                        onSavePreset = onSavePreset,
                        onExportPreset = onExportPreset
                    )
                }
            }

            if (isThinking) {
                item {
                    AssistantThinkingBubble()
                }
            }
        }

        // 4. Rodapé: Indicador de Cota & Caixa de Entrada
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PedalCardDark)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Indicador de Cota
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isProUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LedEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PRO Vitalício Liberado • Ajustes ilimitados",
                            color = LedEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Restam $remainingQuota de $maxDailyFree ajustes grátis hoje",
                        color = if (remainingQuota > 0) TextSecondary else LedAmber,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Liberar Ilimitado",
                        color = LedEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("unlock_pro_from_chat")
                            .clip(RoundedCornerShape(4.dp))
                            .padding(2.dp),
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    )
                }
            }

            // Input TextField e Botão de Envio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ex: Amacia os agudos para a minha Strato...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_input_field"),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PedalCardSurface,
                        unfocusedContainerColor = PedalCardSurface,
                        focusedBorderColor = LedEmerald,
                        unfocusedBorderColor = MetalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank() && !isThinking) {
                            val textToSend = inputText
                            inputText = ""
                            onSendMessage(textToSend)
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isThinking) {
                            val textToSend = inputText
                            inputText = ""
                            onSendMessage(textToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isThinking) LedEmerald else MetalBorder)
                        .testTag("send_assistant_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = if (inputText.isNotBlank() && !isThinking) Color.Black else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: ToneAssistantMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = Color(0xFF1E283D),
            border = BorderStroke(1.dp, MetalBorderHighlight),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = LedCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    message: ToneAssistantMessage,
    onApplyToPedalboard: (Mk20Preset) -> Unit,
    onSavePreset: (Mk20Preset) -> Unit,
    onExportPreset: (Mk20Preset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = PedalCardDark,
                border = BorderStroke(1.dp, MetalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    // Header do Roadie
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LedEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Roadie MK-20",
                            color = LedEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Texto explicativo musical
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    // Lista de ajustes técnicos feitos
                    if (message.appliedChangesSummary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = PedalCardSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MetalBorder.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Ajustes aplicados na pedaleira:",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                message.appliedChangesSummary.forEach { change ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = LedEmerald,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = change,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card Interativo do Preset Gerado
            message.generatedPreset?.let { preset ->
                Spacer(modifier = Modifier.height(8.dp))
                AssistantPresetCard(
                    preset = preset,
                    onApplyToPedalboard = onApplyToPedalboard,
                    onSavePreset = onSavePreset,
                    onExportPreset = onExportPreset
                )
            }
        }
    }
}

@Composable
private fun AssistantPresetCard(
    preset: Mk20Preset,
    onApplyToPedalboard: (Mk20Preset) -> Unit,
    onSavePreset: (Mk20Preset) -> Unit,
    onExportPreset: (Mk20Preset) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF131822),
        border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Título do Preset
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = LedEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = preset.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = LedEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "412 BYTES",
                        color = LedEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Detalhes de Amp e Cab
            val ampName = preset.pedals.AMP?.model ?: "AMP"
            val cabName = preset.pedals.CAB?.model ?: "CAB"
            Text(
                text = "Amp: $ampName • Cab: $cabName",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Badges dos Blocos Ativos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "AMP" to (preset.pedals.AMP?.active == true),
                    "CAB" to (preset.pedals.CAB?.active == true),
                    "DS" to (preset.pedals.DS?.active == true),
                    "EQ" to (preset.pedals.EQ?.active == true),
                    "MOD" to (preset.pedals.MOD?.active == true),
                    "DLY" to (preset.pedals.DELAY?.active == true),
                    "REV" to (preset.pedals.REVERB?.active == true)
                ).forEach { (block, active) ->
                    Surface(
                        color = if (active) LedEmerald.copy(alpha = 0.2f) else MetalBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (active) LedEmerald.copy(alpha = 0.5f) else Color.Transparent)
                    ) {
                        Text(
                            text = block,
                            color = if (active) LedEmerald else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botões de Ação Direta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Aplicar na Pedaleira
                Button(
                    onClick = { onApplyToPedalboard(preset) },
                    colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(34.dp)
                        .testTag("apply_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aplicar",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Salvar
                OutlinedButton(
                    onClick = { onSavePreset(preset) },
                    border = BorderStroke(1.dp, MetalBorderHighlight),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("save_assistant_preset")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = LedCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Salvar",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                }

                // Baixar .MK20
                OutlinedButton(
                    onClick = { onExportPreset(preset) },
                    border = BorderStroke(1.dp, MetalBorderHighlight),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("export_assistant_preset")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = LedAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = ".MK20",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PedalCardDark,
            border = BorderStroke(1.dp, MetalBorder),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = LedEmerald,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "O Roadie está ajustando os parâmetros da MK-20...",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
