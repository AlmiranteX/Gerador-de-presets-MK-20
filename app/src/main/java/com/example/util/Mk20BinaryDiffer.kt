package com.example.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * Ferramenta de Comparação Binária (Hex Dump & Diff) e Diagnóstico de Hardware para arquivos .mk20 (412 bytes).
 * Permite comparar lado a lado qualquer preset gerado pelo app contra arquivos autênticos exportados pelo M-EFCS
 * (como puts.mk20 e MK20_preset_teste.mk20), identificando cabeçalhos, assinaturas, offsets de parâmetros e trailers.
 */
object Mk20BinaryDiffer {

    data class ValidationItem(
        val id: String,
        val title: String,
        val offsetRange: String,
        val passed: Boolean,
        val actualValue: String,
        val expectedValue: String,
        val details: String
    )

    data class SectionComparison(
        val sectionName: String,
        val offsetHex: String,
        val startOffset: Int,
        val endOffsetExclusive: Int,
        val isStructuralHeaderOrFooter: Boolean,
        val leftSummary: String,
        val rightSummary: String,
        val leftHex: String,
        val rightHex: String,
        val diffByteCount: Int
    )

    data class BinaryDiffReport(
        val leftLabel: String,
        val rightLabel: String,
        val leftSize: Int,
        val rightSize: Int,
        val totalDiffBytes: Int,
        val structuralDiffBytes: Int,
        val parameterDiffBytes: Int,
        val allValidationsPassed: Boolean,
        val validations: List<ValidationItem>,
        val sections: List<SectionComparison>,
        val sideBySideHexDump: String
    )

    /**
     * Executa a bateria completa de validação binária de hardware v85 em um array de bytes .mk20.
     */
    fun validateMk20Binary(bytes: ByteArray): List<ValidationItem> {
        val checks = mutableListOf<ValidationItem>()

        // 1. Tamanho Exato (412 bytes)
        val sizeOk = bytes.size == Mk20BinarySerializer.AUTHENTIC_MK20_SIZE
        checks.add(
            ValidationItem(
                id = "SIZE_412",
                title = "Tamanho Oficial do Arquivo",
                offsetRange = "0x000..0x19B",
                passed = sizeOk,
                actualValue = "${bytes.size} bytes",
                expectedValue = "412 bytes",
                details = if (sizeOk) "Tamanho exato de snapshot nativo da MK-20." else "Tamanho incorreto; o M-EFCS exige exatamente 412 bytes."
            )
        )

        if (bytes.size < 412) {
            return checks
        }

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // 2. Título ASCII + Null Terminator em 0x13
        val titleBytes = bytes.sliceArray(0 until 20)
        val firstNull = titleBytes.indexOf(0.toByte())
        val hasNullTerminator = titleBytes[19] == 0.toByte() && firstNull in 1..19
        val activeTitleSlice = if (firstNull > 0) titleBytes.sliceArray(0 until firstNull) else titleBytes
        val allAsciiPrintable = activeTitleSlice.all { (it.toInt() and 0xFF) in 0x20..0x7E }
        val paddingClean = if (firstNull >= 0) (firstNull until 20).all { titleBytes[it] == 0.toByte() } else false
        val titleOk = hasNullTerminator && allAsciiPrintable && paddingClean
        val decodedTitle = String(activeTitleSlice, StandardCharsets.US_ASCII)
        checks.add(
            ValidationItem(
                id = "ASCII_TITLE",
                title = "String de Título ASCII + Null Terminator",
                offsetRange = "0x00..0x13",
                passed = titleOk,
                actualValue = "\"$decodedTitle\" (byte[19]=0x${String.format("%02X", titleBytes[19])})",
                expectedValue = "ASCII puro (<=19 chars) + 0x00 no byte 19",
                details = if (titleOk) {
                    "String ASCII limpa e terminada em 0x00 (evita buffer overflow no DSP)."
                } else {
                    "Caracteres UTF-8 multibyte (ex: Ç, •) ou falta de 0x00 no byte 19 corrompem a leitura no firmware."
                }
            )
        )

        // 3. Assinatura de Hardware M-EFCS v85 (0x14..0x17 = FF 90 1E 00)
        val sigHex = bytes.sliceArray(0x14..0x17).joinToString(" ") { String.format("%02X", it) }
        val sigOk = bytes[0x14] == 0xFF.toByte() &&
            bytes[0x15] == 0x90.toByte() &&
            bytes[0x16] == 0x1E.toByte() &&
            bytes[0x17] == 0x00.toByte()
        checks.add(
            ValidationItem(
                id = "HW_MAGIC",
                title = "Assinatura de Hardware M-EFCS v85",
                offsetRange = "0x14..0x17",
                passed = sigOk,
                actualValue = sigHex,
                expectedValue = "FF 90 1E 00",
                details = if (sigOk) {
                    "Assinatura idêntica aos arquivos válidos puts.mk20 e MK20_preset_teste.mk20."
                } else {
                    "Assinatura divergente ($sigHex). O M-EFCS v85 rejeita cabeçalhos legados como 22 00 16 00."
                }
            )
        )

        // 4. Volume Master (0x18) & BPM (0x1A) & Reservado (0x1C)
        val masterVol = buf.getShort(0x18).toInt()
        val bpm = buf.getShort(0x1A).toInt()
        val reserved = buf.getShort(0x1C).toInt()
        val volBpmOk = masterVol in 0..100 && bpm in 40..240 && reserved == 0
        checks.add(
            ValidationItem(
                id = "VOL_BPM",
                title = "Volume Global, BPM e Bytes Reservados",
                offsetRange = "0x18..0x1D",
                passed = volBpmOk,
                actualValue = "Vol=$masterVol, BPM=$bpm, Res=$reserved",
                expectedValue = "Vol 0..100, BPM 40..240, Res=0",
                details = if (volBpmOk) "Limites globais de volume e tempo dentro da faixa oficial." else "Volume ou BPM fora da faixa suportada pelo hardware."
            )
        )

        // 5. Roteamento da Cadeia de Sinal (0x1E..0x27)
        val orderBytes = (0 until 10).map { bytes[0x1E + it].toInt() and 0xFF }
        val orderOk = orderBytes == (0..9).toList()
        checks.add(
            ValidationItem(
                id = "CHAIN_ORDER",
                title = "Ordem da Cadeia de 10 Módulos",
                offsetRange = "0x1E..0x27",
                passed = orderOk,
                actualValue = orderBytes.joinToString(" "),
                expectedValue = "0 1 2 3 4 5 6 7 8 9",
                details = if (orderOk) "Sequência oficial FX -> NS -> DS -> AMP -> CAB -> EQ -> MOD -> DLY -> REV -> VOL." else "Ordem de módulos inválida."
            )
        )

        // 6. IDs de Modelo dentro dos limites do catálogo de hardware (0x32..0x3B)
        val maxModelIds = intArrayOf(19, 7, 51, 119, 99, 2, 19, 26, 17, 0)
        val modLabels = arrayOf("FX", "NS", "DS", "AMP", "CAB", "EQ", "MOD", "DLY", "REV", "VOL")
        val modelIds = (0 until 10).map { bytes[0x32 + it].toInt() and 0xFF }
        val invalidModels = mutableListOf<String>()
        for (i in 0 until 10) {
            if (modelIds[i] !in 0..maxModelIds[i]) {
                invalidModels.add("${modLabels[i]}=${modelIds[i]} (máx ${maxModelIds[i]})")
            }
        }
        val modelsOk = invalidModels.isEmpty()
        checks.add(
            ValidationItem(
                id = "MODEL_IDS",
                title = "IDs de Modelo (0-Indexados)",
                offsetRange = "0x32..0x3B",
                passed = modelsOk,
                actualValue = modelIds.mapIndexed { i, v -> "${modLabels[i]}:$v" }.joinToString(", "),
                expectedValue = "Todos dentro dos limites [0..máx]",
                details = if (modelsOk) {
                    "Todos os 10 seletores de algoritmo apontam para slots válidos de fábrica."
                } else {
                    "IDs fora da faixa: ${invalidModels.joinToString(", ")} (ex: 255/0xFF em CAB trava a escrita)."
                }
            )
        )

        // 7. Limites de Parâmetros de Módulos (CAB High Cut / Low Cut, DLY Time, FX SyncBpm)
        val cabLevel = buf.getShort(0x9C).toInt()
        val cabLowCut = buf.getShort(0x9E).toInt()
        val cabHighCut = buf.getShort(0xA0).toInt()
        val dlyTime = buf.getShort(0xE4).toInt()
        val fxSlot7SyncBpm = buf.getShort(0x4A).toInt()
        val paramsOk = cabLevel in 0..100 &&
            cabLowCut in 20..300 &&
            cabHighCut in 50..180 &&
            dlyTime in 100..2000 &&
            fxSlot7SyncBpm in 40..240
        checks.add(
            ValidationItem(
                id = "PARAM_BOUNDS",
                title = "Escalas e Limites de Parâmetros de Hardware",
                offsetRange = "0x3C..0x115",
                passed = paramsOk,
                actualValue = "CAB[Lvl=$cabLevel, Lo=${cabLowCut}Hz, Hi=${cabHighCut / 10.0}k], DLY=${dlyTime}ms, FX_BPM=$fxSlot7SyncBpm",
                expectedValue = "CAB Lo 20..300, Hi 50..180, DLY 100..2000, FX_BPM 40..240",
                details = if (paramsOk) {
                    "Todas as escalas (High Cut 50..180, Delay 100..2000ms, SyncBpm 40..240) estão válidas."
                } else {
                    "Parâmetro fora de escala detectado (ex: FX SyncBpm=0 em 0x4A ou CAB High Cut fora de 50..180)."
                }
            )
        )

        // 8. Tabela de Expressão / Calibração (0x116..0x193) e Trailer NGD4 (0x194..0x19B)
        val refTemplate = Mk20BinarySerializer.getAuthenticTemplateBytes()
        val footerDiffs = (0x116 until 0x194).count { bytes[it] != refTemplate[it] }
        val trailerStr = String(bytes.sliceArray(0x195..0x198), StandardCharsets.US_ASCII)
        val trailerOk = bytes[0x194] == 0.toByte() &&
            trailerStr == "NGD4" &&
            bytes[0x199] == 0.toByte() &&
            bytes[0x19A] == 0.toByte() &&
            bytes[0x19B] == 0.toByte() &&
            footerDiffs == 0
        checks.add(
            ValidationItem(
                id = "FOOTER_NGD4",
                title = "Tabela EXP/CTRL & Trailer NGD4 (Sem Checksum CRC)",
                offsetRange = "0x116..0x19B",
                passed = trailerOk,
                actualValue = "Trailer=\"$trailerStr\", byte[0x166]=0x${String.format("%02X", bytes[0x166])}, diffs=$footerDiffs",
                expectedValue = "Trailer=\"NGD4\", byte[0x166]=0x00, diffs=0",
                details = if (trailerOk) {
                    "Tabela EXP/CTRL e assinatura final NGD4 100% idênticas ao puts.mk20 do M-EFCS."
                } else {
                    "Diferença encontrada na tabela de expressão (ex: 0x09 em 0x166) ou no trailer NGD4."
                }
            )
        )

        return checks
    }

    /**
     * Compara dois arquivos binários .mk20 de 412 bytes (ex: Gerado pelo App vs Oficial M-EFCS puts.mk20).
     */
    fun compare(
        leftBytes: ByteArray,
        rightBytes: ByteArray = Mk20BinarySerializer.getAuthenticTemplateBytes(),
        leftLabel: String = "Gerado pelo App (.mk20)",
        rightLabel: String = "Oficial M-EFCS (puts.mk20)"
    ): BinaryDiffReport {
        val validations = validateMk20Binary(leftBytes)
        val allValid = validations.all { it.passed }

        val minLen = minOf(leftBytes.size, rightBytes.size)
        var totalDiffs = 0
        var structuralDiffs = 0
        var paramDiffs = 0

        val sectionDefs = listOf(
            Triple("01. Nome do Preset (ASCII 20B)", 0x00 to 0x14, false),
            Triple("02. Assinatura Hardware (Magic ID)", 0x14 to 0x18, true),
            Triple("03. Volume Global, BPM & Reservado", 0x18 to 0x1E, false),
            Triple("04. Ordem da Cadeia de Sinal (10B)", 0x1E to 0x28, true),
            Triple("05. Flags Ativo/Bypass (10 Módulos)", 0x28 to 0x32, false),
            Triple("06. IDs de Modelo (10 Módulos)", 0x32 to 0x3C, false),
            Triple("07. Bloco [0] FX (12x Int16)", 0x3C to 0x54, false),
            Triple("08. Bloco [1] NS (12x Int16)", 0x54 to 0x6C, false),
            Triple("09. Bloco [2] DS (12x Int16)", 0x6C to 0x84, false),
            Triple("10. Bloco [3] AMP (12x Int16)", 0x84 to 0x9C, false),
            Triple("11. Bloco [4] CAB (12x Int16)", 0x9C to 0xB4, false),
            Triple("12. Bloco [5] EQ (12x Int16)", 0xB4 to 0xCC, false),
            Triple("13. Bloco [6] MOD (12x Int16)", 0xCC to 0xE4, false),
            Triple("14. Bloco [7] DLY (12x Int16)", 0xE4 to 0xFC, false),
            Triple("15. Bloco [8] REV (12x Int16)", 0xFC to 0x114, false),
            Triple("16. Bloco [9] VOL Level (Int16)", 0x114 to 0x116, false),
            Triple("17. Tabela EXP / Footswitch CTRL", 0x116 to 0x194, true),
            Triple("18. Trailer Oficial NGD4 (8B)", 0x194 to 0x19C, true)
        )

        val sections = mutableListOf<SectionComparison>()
        for ((name, range, isStructural) in sectionDefs) {
            val (start, end) = range
            if (start >= minLen) continue
            val safeEnd = minOf(end, minLen)
            val lSlice = leftBytes.sliceArray(start until safeEnd)
            val rSlice = rightBytes.sliceArray(start until safeEnd)
            val diffCount = (0 until lSlice.size).count { lSlice[it] != rSlice[it] }
            totalDiffs += diffCount
            if (isStructural) {
                structuralDiffs += diffCount
            } else {
                paramDiffs += diffCount
            }

            sections.add(
                SectionComparison(
                    sectionName = name,
                    offsetHex = String.format("0x%03X..0x%03X", start, safeEnd - 1),
                    startOffset = start,
                    endOffsetExclusive = safeEnd,
                    isStructuralHeaderOrFooter = isStructural,
                    leftSummary = summarizeSection(start, lSlice),
                    rightSummary = summarizeSection(start, rSlice),
                    leftHex = lSlice.take(12).joinToString(" ") { String.format("%02X", it) } + if (lSlice.size > 12) " …" else "",
                    rightHex = rSlice.take(12).joinToString(" ") { String.format("%02X", it) } + if (rSlice.size > 12) " …" else "",
                    diffByteCount = diffCount
                )
            )
        }

        val sideBySide = buildSideBySideHexDump(leftBytes, rightBytes, leftLabel, rightLabel)

        return BinaryDiffReport(
            leftLabel = leftLabel,
            rightLabel = rightLabel,
            leftSize = leftBytes.size,
            rightSize = rightBytes.size,
            totalDiffBytes = totalDiffs + kotlin.math.abs(leftBytes.size - rightBytes.size),
            structuralDiffBytes = structuralDiffs,
            parameterDiffBytes = paramDiffs,
            allValidationsPassed = allValid,
            validations = validations,
            sections = sections,
            sideBySideHexDump = sideBySide
        )
    }

    private fun summarizeSection(startOffset: Int, slice: ByteArray): String {
        val buf = ByteBuffer.wrap(slice).order(ByteOrder.LITTLE_ENDIAN)
        return when (startOffset) {
            0x00 -> {
                val nul = slice.indexOf(0.toByte()).let { if (it == -1) slice.size else it }
                val str = String(slice, 0, nul, StandardCharsets.UTF_8)
                "\"$str\" (${nul}B + ${slice.size - nul} nulls)"
            }
            0x14 -> slice.joinToString(" ") { String.format("%02X", it) }
            0x18 -> {
                if (slice.size >= 6) {
                    val vol = buf.getShort(0).toInt()
                    val bpm = buf.getShort(2).toInt()
                    val res = buf.getShort(4).toInt()
                    "Vol=$vol, BPM=$bpm, Res=$res"
                } else ""
            }
            0x1E -> slice.joinToString(" ") { (it.toInt() and 0xFF).toString() }
            0x28 -> {
                val names = arrayOf("FX", "NS", "DS", "AMP", "CAB", "EQ", "MOD", "DLY", "REV", "VOL")
                slice.mapIndexed { i, b -> "${names.getOrElse(i) { "$i" }}=${if (b != 0.toByte()) "ON" else "OFF"}" }
                    .joinToString(" ")
            }
            0x32 -> {
                val names = arrayOf("FX", "NS", "DS", "AMP", "CAB", "EQ", "MOD", "DLY", "REV", "VOL")
                slice.mapIndexed { i, b -> "${names.getOrElse(i) { "$i" }}=#${(b.toInt() and 0xFF) + 1}(${b.toInt() and 0xFF})" }
                    .joinToString(" ")
            }
            in 0x3C..0xFC -> {
                val shorts = ShortArray(slice.size / 2) { buf.short }
                shorts.take(8).joinToString(", ") { it.toString() }
            }
            0x114 -> if (slice.size >= 2) "Level=${buf.getShort(0).toInt()}" else ""
            0x116 -> "EXP/CTRL (${slice.size}B, [0x166]=0x${String.format("%02X", slice.getOrElse(0x166 - 0x116) { 0 })})"
            0x194 -> {
                val ascii = slice.map { b ->
                    val c = b.toInt() and 0xFF
                    if (c in 32..126) c.toChar() else '.'
                }.joinToString("")
                "\"$ascii\" (${slice.joinToString(" ") { String.format("%02X", it) }})"
            }
            else -> slice.joinToString(" ") { String.format("%02X", it) }
        }
    }

    private fun buildSideBySideHexDump(
        left: ByteArray,
        right: ByteArray,
        leftLabel: String,
        rightLabel: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("// ==========================================================================================")
        sb.appendLine("// COMPARADOR HEXADECIMAL LADO A LADO (.mk20 412 BYTES)")
        sb.appendLine("// ESQUERDA (A): $leftLabel (${left.size} bytes)")
        sb.appendLine("// DIREITA  (B): $rightLabel (${right.size} bytes)")
        sb.appendLine("// Legenda: [==] Linha idêntica | [!=] Diferença de parâmetros/bytes (marcada com * no byte)")
        sb.appendLine("// ==========================================================================================")
        sb.appendLine("OFFSET | ESQUERDA (A - 8 BYTES)      | DIREITA (B - 8 BYTES)       | STATUS | SEÇÃO")
        sb.appendLine("-------+-----------------------------+-----------------------------+--------+----------------")

        val maxLen = maxOf(left.size, right.size)
        for (offset in 0 until maxLen step 8) {
            val lHex = StringBuilder()
            val rHex = StringBuilder()
            var rowDiffs = 0

            for (j in 0 until 8) {
                val idx = offset + j
                val lb = if (idx < left.size) left[idx] else null
                val rb = if (idx < right.size) right[idx] else null
                val isDiff = lb != rb
                if (isDiff) rowDiffs++

                if (lb != null) {
                    lHex.append(String.format("%02X%s", lb, if (isDiff) "*" else " "))
                } else {
                    lHex.append("   ")
                }

                if (rb != null) {
                    rHex.append(String.format("%02X%s", rb, if (isDiff) "*" else " "))
                } else {
                    rHex.append("   ")
                }
            }

            val status = if (rowDiffs == 0) " [==] " else String.format(" [Δ%d] ", rowDiffs)
            val sectionTag = sectionNameForOffset(offset)
            sb.appendLine(String.format("0x%03X  | %-27s | %-27s | %s | %s", offset, lHex.toString().trimEnd(), rHex.toString().trimEnd(), status, sectionTag))
        }
        return sb.toString()
    }

    private fun sectionNameForOffset(offset: Int): String = when (offset) {
        in 0x00..0x0F -> "Título ASCII"
        in 0x10..0x17 -> "Título + Magic ID (FF 90 1E 00)"
        in 0x18..0x1F -> "Vol / BPM / Res + Ordem"
        in 0x20..0x27 -> "Ordem da Cadeia"
        in 0x28..0x2F -> "Flags ON/OFF"
        in 0x30..0x37 -> "Flags + IDs de Modelo"
        in 0x38..0x3B -> "IDs de Modelo + [0] FX"
        in 0x3C..0x53 -> "[0] FX Params"
        in 0x54..0x6B -> "[1] NS Params"
        in 0x6C..0x83 -> "[2] DS Params"
        in 0x84..0x9B -> "[3] AMP Params"
        in 0x9C..0xB3 -> "[4] CAB Params"
        in 0xB4..0xCB -> "[5] EQ Params"
        in 0xCC..0xE3 -> "[6] MOD Params"
        in 0xE4..0xFB -> "[7] DLY Params"
        in 0xFC..0x113 -> "[8] REV Params"
        in 0x114..0x115 -> "[9] VOL Level"
        in 0x116..0x18F -> "Tabela EXP/CTRL"
        in 0x190..0x19B -> "EXP + Trailer NGD4"
        else -> "Extra"
    }
}
