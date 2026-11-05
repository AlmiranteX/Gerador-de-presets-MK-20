package com.example.data.model

import java.util.UUID

/**
 * Representa uma mensagem dentro do chat com o Assistente de Timbre (Roadie Virtual MK-20).
 */
data class ToneAssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val generatedPreset: Mk20Preset? = null,
    val appliedChangesSummary: List<String> = emptyList()
)
