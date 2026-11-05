package com.example.util

import com.example.data.model.Mk20Pedals
import com.example.data.model.Mk20Preset
import com.example.data.model.PedalModule
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

/**
 * Serializador e Deserializador Binário oficial para a pedaleira M-VAVE MK-20 e aplicativo M-EFCS.
 * Estrutura baseada no arquivo original extraído da MK-20 (412 bytes exatos).
 */
object Mk20BinarySerializer {

    // Tamanho oficial e obrigatório da MK-20 no aplicativo M-EFCS
    const val AUTHENTIC_MK20_SIZE = 412
    const val TOTAL_FILE_SIZE = AUTHENTIC_MK20_SIZE

    // Tamanhos secundários / legados
    const val SIZE_512 = 512
    const val SIZE_256 = 256
    const val SIZE_1024 = 1024

    // Magic headers legados
    val MAGIC_MK20 = byteArrayOf(0x4D.toByte(), 0x4B.toByte(), 0x32.toByte(), 0x30.toByte())
    val MAGIC_AMK2 = byteArrayOf(0x41.toByte(), 0x4D.toByte(), 0x4B.toByte(), 0x32.toByte())
    private val TRAILER_TONE = byteArrayOf(0x54.toByte(), 0x4F.toByte(), 0x4E.toByte(), 0x45.toByte()) // "TONE"
    private val TRAILER_NGD4 = byteArrayOf(0x4E.toByte(), 0x47.toByte(), 0x44.toByte(), 0x34.toByte()) // "NGD4"

    const val FILE_VERSION: Short = 1
    const val DEVICE_ID_MK20: Short = 0x0020

    // Identificadores oficiais dos 10 módulos da MK-20
    const val TYPE_FX: Byte = 0
    const val TYPE_NS: Byte = 1
    const val TYPE_NR: Byte = 1 // Alias para compatibilidade
    const val TYPE_DS: Byte = 2
    const val TYPE_AMP: Byte = 3
    const val TYPE_CAB: Byte = 4
    const val TYPE_EQ: Byte = 5
    const val TYPE_MOD: Byte = 6
    const val TYPE_DELAY: Byte = 7
    const val TYPE_REVERB: Byte = 8
    const val TYPE_VOL: Byte = 9

    // Template binário autêntico oficial de 412 bytes extraído diretamente do M-EFCS (puts.mk20 - verificado no pedal)
    private val AUTHENTIC_MK20_TEMPLATE = byteArrayOf(
        (0x55).toByte(), (0x53).toByte(), (0x45).toByte(), (0x52).toByte(), (0x20).toByte(), (0x50).toByte(), (0x52).toByte(), (0x45).toByte(), (0x53).toByte(), (0x45).toByte(), (0x54).toByte(), (0x20).toByte(), (0x31).toByte(), (0x34).toByte(), (0x35).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0xff).toByte(), (0x90).toByte(), (0x1e).toByte(), (0x00).toByte(), (0x3c).toByte(), (0x00).toByte(), (0x50).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x01).toByte(),
        (0x02).toByte(), (0x03).toByte(), (0x04).toByte(), (0x05).toByte(), (0x06).toByte(), (0x07).toByte(), (0x08).toByte(), (0x09).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x01).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x19).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(),
        (0x50).toByte(), (0x00).toByte(), (0x19).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x50).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(),
        (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x64).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x32).toByte(), (0x00).toByte(), (0x64).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x50).toByte(), (0x00).toByte(), (0x14).toByte(), (0x00).toByte(),
        (0xb4).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x0a).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(),
        (0x32).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x50).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0xf4).toByte(), (0x01).toByte(), (0x1e).toByte(), (0x00).toByte(), (0x32).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x50).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x14).toByte(), (0x00).toByte(), (0x1e).toByte(), (0x00).toByte(),
        (0x50).toByte(), (0x00).toByte(), (0x50).toByte(), (0x00).toByte(), (0x28).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x64).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x0a).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x64).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x0b).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x64).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x0c).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x64).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x09).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x64).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x09).toByte(), (0x0a).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x08).toByte(), (0x09).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x04).toByte(), (0x05).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x47).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(),
        (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte(), (0x4e).toByte(), (0x47).toByte(), (0x44).toByte(), (0x34).toByte(), (0x00).toByte(), (0x00).toByte(), (0x00).toByte()
    )

    /**
     * Verifica se os bytes fornecidos correspondem a um preset binário da MK-20 (.mk20 ou .amk20).
     */
    fun isBinaryPreset(bytes: ByteArray): Boolean {
        if (bytes.size == AUTHENTIC_MK20_SIZE) return true
        if (bytes.size < 8) return false
        val isMk20 = bytes[0] == MAGIC_MK20[0] && bytes[1] == MAGIC_MK20[1] && bytes[2] == MAGIC_MK20[2] && bytes[3] == MAGIC_MK20[3]
        val isAmk2 = bytes[0] == MAGIC_AMK2[0] && bytes[1] == MAGIC_AMK2[1] && bytes[2] == MAGIC_AMK2[2] && bytes[3] == MAGIC_AMK2[3]
        val isHardwareMk20 = bytes.size >= 24 && bytes[20] == (0xff).toByte() && bytes[21] == (0x90).toByte() && bytes[22] == (0x1e).toByte() && bytes[23] == (0x00).toByte()
        val hasNgd4 = bytes.size >= 412 && bytes[405] == 'N'.code.toByte() && bytes[406] == 'G'.code.toByte()
        return isMk20 || isAmk2 || isHardwareMk20 || hasNgd4
    }

    /**
     * Serializa o Mk20Preset no formato binário oficial exato de 412 bytes aceito pelo M-EFCS.
     */
    fun serialize(
        preset: Mk20Preset,
        targetSize: Int = AUTHENTIC_MK20_SIZE,
        useAmk2Header: Boolean = false,
        templateBytes: ByteArray? = null
    ): ByteArray {
        if (targetSize == AUTHENTIC_MK20_SIZE || (templateBytes != null && templateBytes.size == AUTHENTIC_MK20_SIZE)) {
            return serializeAuthentic412(
                preset = preset,
                baseTemplate = templateBytes ?: AUTHENTIC_MK20_TEMPLATE,
                preserveTemplateHeader = (templateBytes != null)
            )
        }

        // Se o usuário forneceu um molde personalizado de outro tamanho:
        if (templateBytes != null && templateBytes.isNotEmpty()) {
            return applyToTemplate(templateBytes, preset)
        }

        return when (targetSize) {
            SIZE_256 -> serializeCompact256(preset, useAmk2Header)
            SIZE_1024 -> serializeFull(preset, SIZE_1024, useAmk2Header)
            else -> serializeFull(preset, SIZE_512, useAmk2Header)
        }
    }

    private fun isFxTenScaleParam(fxModelId: Int, key: String): Boolean {
        return when (fxModelId) {
            1 -> key.equals("Speed", ignoreCase = true)
            3 -> key.equals("Sense", ignoreCase = true)
            4, 5, 6, 7, 8 -> key.equals("Gain", ignoreCase = true)
            9 -> key.equals("Sustain", ignoreCase = true)
            10, 11 -> key.equals("Ratio", ignoreCase = true)
            else -> false
        }
    }

    /**
     * Gera o arquivo binário oficial de 412 bytes compatível 100% com o M-EFCS (PC USB e Mobile BLE) da MK-20.
     */
    private fun serializeAuthentic412(
        preset: Mk20Preset,
        baseTemplate: ByteArray,
        preserveTemplateHeader: Boolean = false
    ): ByteArray {
        val result = baseTemplate.clone()
        // Garante que a tabela de expressão/calibração (0x116..0x19b) e os slots base dos módulos
        // partam do template oficial verificado do M-EFCS (puts.mk20), evitando bytes legados como 0x09 em 0x166.
        System.arraycopy(AUTHENTIC_MK20_TEMPLATE, 0x3c, result, 0x3c, 0x19c - 0x3c)
        val buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)

        // 1. Nome do Preset (Bytes 0 a 19):
        // No modo Clonar Molde (TEMPLATE_CLONE), preserva os 20 bytes exatos do cabeçalho do molde oficial
        // ("USER PRESET 145\0\0\0\0\0" com bytes 0x0F..0x13 = 0x00), exigido pelo M-EFCS Mobile via Bluetooth BLE.
        if (preserveTemplateHeader && baseTemplate.size >= 20 && baseTemplate[19] == 0.toByte()) {
            System.arraycopy(baseTemplate, 0, result, 0, 20)
            for (i in 15..19) {
                result[i] = 0.toByte()
            }
        } else {
            writePaddedAsciiString(buffer, preset.title, 20)
        }

        // 2. Assinatura de Hardware MK-20 (Offsets 0x14 a 0x17: 0x001E90FF -> ff 90 1e 00)
        buffer.position(0x14)
        buffer.put((0xff).toByte())
        buffer.put((0x90).toByte())
        buffer.put((0x1e).toByte())
        buffer.put((0x00).toByte())

        // 3. Preset Vol (Offset 0x18) e BPM (Offset 0x1a - 16-bit little-endian)
        val presetVol = preset.masterVolume.coerceIn(0, 100).toShort()
        val bpmVal = preset.bpm.coerceIn(40, 240).toShort()
        buffer.position(0x18)
        buffer.putShort(presetVol)
        buffer.position(0x1a)
        buffer.putShort(bpmVal)

        // 4. Offset 0x1c (2 bytes reservados = 0x00, 0x00)
        buffer.position(0x1c)
        buffer.putShort(0.toShort())

        // 5. Ordem dos Módulos (Offsets 0x1e a 0x27)
        buffer.position(0x1e)
        for (i in 0 until 10) {
            buffer.put(i.toByte())
        }

        // 4. Flags Ativo / Bypass de cada um dos 10 blocos (Offsets 0x28 a 0x31)
        val nsMod = preset.pedals.activeNS ?: preset.pedals.NS ?: preset.pedals.NR
        buffer.position(0x28)
        buffer.put(if (preset.pedals.FX?.active == true) 1.toByte() else 0.toByte())      // [0] FX
        buffer.put(if (nsMod?.active == true) 1.toByte() else 0.toByte())                 // [1] NS
        buffer.put(if (preset.pedals.DS?.active == true) 1.toByte() else 0.toByte())      // [2] DS
        buffer.put(if (preset.pedals.AMP?.active == true) 1.toByte() else 0.toByte())     // [3] AMP
        buffer.put(if (preset.pedals.CAB?.active == true) 1.toByte() else 0.toByte())     // [4] CAB
        buffer.put(if (preset.pedals.EQ?.active == true) 1.toByte() else 0.toByte())      // [5] EQ
        buffer.put(if (preset.pedals.MOD?.active == true) 1.toByte() else 0.toByte())     // [6] MOD
        buffer.put(if (preset.pedals.DELAY?.active == true) 1.toByte() else 0.toByte())   // [7] DLY
        buffer.put(if (preset.pedals.REVERB?.active == true) 1.toByte() else 0.toByte())  // [8] REV
        buffer.put(if (preset.pedals.VOL?.active == true) 1.toByte() else 0.toByte())     // [9] VOL

        // 5. IDs de Modelo dos 10 Módulos (Offsets 0x32 a 0x3b - 0-indexed byte)
        buffer.position(0x32)
        buffer.put(extractFxModelByte(preset.pedals.FX?.model))                 // [0] FX (20 Modelos: 0=Wah-Wah .. 8=Compress .. 19=Whammy)
        buffer.put(extractNsModelByte(nsMod?.model))                           // [1] NS (8 Modelos)
        buffer.put(extractDsModelByte(preset.pedals.DS?.model))                // [2] DS (52 Modelos)
        buffer.put(extractAmpModelByte(preset.pedals.AMP?.model))              // [3] AMP (120 Modelos)
        buffer.put(extractCabModelByte(preset.pedals.CAB?.model))              // [4] CAB (100 Modelos: 0..99)
        buffer.put(extractEqModelByte(preset.pedals.EQ?.model))                // [5] EQ (3 Modelos)
        buffer.put(extractModModelByte(preset.pedals.MOD?.model))              // [6] MOD (20 Modelos)
        buffer.put(extractDelayModelByte(preset.pedals.DELAY?.model))          // [7] DLY (27 Modelos)
        buffer.put(extractReverbModelByte(preset.pedals.REVERB?.model))        // [8] REV (18 Modelos)
        buffer.put(extractModelByte(preset.pedals.VOL?.model, 0.toByte()))     // [9] VOL

        // 6. Parâmetros Específicos nos offsets 16-bit de cada um dos 10 blocos:
        // [0] FX (Offset 0x3c) - 20 Modelos oficiais MK-20
        val fxModel = preset.pedals.FX?.model ?: "Compress"
        val fxParams = preset.pedals.FX?.params ?: emptyMap()
        val fxItem = com.example.data.model.Mk20Catalog.findFx(fxModel) ?: com.example.data.model.Mk20Catalog.findFx("Compress")
        val fxId = fxItem?.id ?: 9
        val fxKeys = fxItem?.defaultParams?.keys?.toList() ?: listOf("Sustain", "Attack", "Level", "Blend")
        buffer.position(0x3c)
        if (fxId == 1) {
            // 1: Wah-Wah tem layout especial no hardware:
            // [0]=Speed(1..100), [1]=Q, [2]=Mix, [3]=Width, [4]=Level, [5]=Sync, [6]=0, [7]=SyncBpm(40..240)
            val speedRaw = fxParams["Speed"] ?: fxParams["speed"] ?: 2.5f
            val speedBin = (if (speedRaw <= 10.0f) kotlin.math.round(speedRaw * 10f).toInt() else speedRaw.toInt()).coerceIn(1, 100).toShort()
            val qBin = (fxParams["Q"] ?: fxParams["q"] ?: 50f).toInt().coerceIn(0, 100).toShort()
            val mixBin = (fxParams["Mix"] ?: fxParams["mix"] ?: 80f).toInt().coerceIn(0, 100).toShort()
            val widthBin = (fxParams["Width"] ?: fxParams["width"] ?: 25f).toInt().coerceIn(0, 100).toShort()
            val levelBin = (fxParams["Level"] ?: fxParams["level"] ?: 50f).toInt().coerceIn(0, 100).toShort()
            val syncBin = (if ((fxParams["Sync"] ?: fxParams["sync"] ?: 0f) != 0f) 1 else 0).toShort()
            val syncBpmBin = (fxParams["SyncBpm"] ?: fxParams["syncBpm"] ?: fxParams["SyncBPM"] ?: fxParams["Sync BPM"] ?: preset.bpm.toFloat()).toInt().coerceIn(40, 240).toShort()
            buffer.putShort(speedBin)
            buffer.putShort(qBin)
            buffer.putShort(mixBin)
            buffer.putShort(widthBin)
            buffer.putShort(levelBin)
            buffer.putShort(syncBin)
            buffer.putShort(0.toShort()) // Slot 6 reservado em 0x48
            buffer.putShort(syncBpmBin)  // Slot 7 em 0x4A
            for (i in 8 until 12) buffer.putShort(0.toShort())
        } else {
            for (i in 0 until 12) {
                val key = fxKeys.getOrNull(i)
                val v = if (key != null) {
                    val fallbackAlias = when (key.lowercase()) {
                        "mode" -> fxParams["Mode"] ?: fxParams["mode"]
                        "bend" -> fxParams["Bend"] ?: fxParams["bend"]
                        "mix" -> fxParams["Mix"] ?: fxParams["mix"]
                        "filter" -> fxParams["Filter"] ?: fxParams["filter"]
                        "width" -> fxParams["Width"] ?: fxParams["width"]
                        "gain" -> fxParams["Gain"] ?: fxParams["gain"]
                        "level" -> fxParams["Level"] ?: fxParams["level"] ?: fxParams["Wet Level"]
                        else -> null
                    }
                    val raw = fxParams[key] ?: fallbackAlias ?: fxItem?.defaultParams?.get(key) ?: 50f
                    val coerced = when {
                        key.equals("Sync", ignoreCase = true) -> if (raw != 0f) 1 else 0
                        key.equals("SyncBpm", ignoreCase = true) -> raw.toInt().coerceIn(40, 240)
                        key.equals("Bit", ignoreCase = true) -> raw.toInt().coerceIn(2, 24)
                        key.equals("High Pitch", ignoreCase = true) -> raw.toInt().coerceIn(0, 24)
                        key.equals("Low Pitch", ignoreCase = true) -> raw.toInt().coerceIn(-24, 0)
                        key.equals("Semi", ignoreCase = true) -> raw.toInt().coerceIn(-12, 12)
                        key.equals("mode", ignoreCase = true) -> raw.toInt().coerceIn(1, 3)
                        isFxTenScaleParam(fxId, key) -> {
                            val scaled = if (raw <= 10.0f) kotlin.math.round(raw * 10f).toInt() else raw.toInt()
                            scaled.coerceIn(0, 100)
                        }
                        else -> raw.toInt().coerceIn(0, 100)
                    }
                    coerced.toShort()
                } else if (i == 7) {
                    // Preserva SyncBpm válido (80) no slot 7 do FX como nos arquivos autênticos do M-EFCS
                    80.toShort()
                } else {
                    0.toShort()
                }
                buffer.putShort(v)
            }
        }

        // [1] NS (Offset 0x54) - 8 Modelos oficiais MK-20 Noise Gate & Dynamics
        val nsModelName = nsMod?.model ?: "soft gate"
        val nsParams = nsMod?.params ?: emptyMap()
        val nsItem = com.example.data.model.Mk20Catalog.findNs(nsModelName) ?: com.example.data.model.Mk20Catalog.NS_MODELS[2]
        val nsKeys = nsItem.defaultParams.keys.toList()
        buffer.position(0x54)
        for (i in 0 until 12) {
            val key = nsKeys.getOrNull(i)
            val v = if (key != null) {
                val fallbackAlias = when (key) {
                    "gate" -> nsParams["Gate"]
                    "bias" -> nsParams["Bias"]
                    "inputDetect" -> nsParams["InputDetect"] ?: nsParams["Detect"] ?: nsParams["Input Detect"]
                    "thd" -> nsParams["Thd"] ?: nsParams["Thresh"] ?: nsParams["Threshold"]
                    "att" -> nsParams["Att"] ?: nsParams["Attack"]
                    "rel" -> nsParams["Rel"] ?: nsParams["Release"]
                    "kw" -> nsParams["Kw"]
                    "ratio" -> nsParams["Ratio"]
                    "sustain" -> nsParams["Sustain"]
                    "attack" -> nsParams["Attack"] ?: nsParams["Att"]
                    "level" -> nsParams["Level"]
                    "blend" -> nsParams["Blend"]
                    "gain" -> nsParams["Gain"]
                    "knee" -> nsParams["Knee"]
                    "tone" -> nsParams["Tone"]
                    else -> null
                }
                val raw = nsParams[key] ?: fallbackAlias ?: nsItem.defaultParams[key] ?: 0f
                val coerced = when {
                    key.equals("inputDetect", ignoreCase = true) -> if (raw != 0f) 1 else 0
                    key.equals("thd", ignoreCase = true) && nsItem.id in 3..5 -> {
                        val negVal = if (raw > 0f) -raw else raw
                        negVal.toInt().coerceIn(-80, -30)
                    }
                    else -> raw.toInt().coerceIn(0, 100)
                }
                coerced.toShort()
            } else {
                0.toShort()
            }
            buffer.putShort(v)
        }

        // [2] DS (Offset 0x6c) - 52 Modelos oficiais MK-20
        // Nos arquivos autênticos do M-EFCS (puts.mk20 / preset_teste.mk20), os 8 slots base do DS
        // partem de [50, 50, 50, 50, 50, 0, 50, 100, 0, 0, 0, 0] e apenas as chaves do modelo são sobrescritas.
        val dsDefaultSlots = shortArrayOf(50, 50, 50, 50, 50, 0, 50, 100, 0, 0, 0, 0)
        val dsModelName = preset.pedals.DS?.model ?: "MW-808"
        val dsParams = preset.pedals.DS?.params ?: emptyMap()
        val dsItem = com.example.data.model.Mk20Catalog.findDs(dsModelName) ?: com.example.data.model.Mk20Catalog.DS_MODELS.first()
        val dsKeys = dsItem.defaultParams.keys.toList()
        buffer.position(0x6c)
        for (i in 0 until 12) {
            val key = dsKeys.getOrNull(i)
            val v = if (key != null) {
                val fallbackAlias = when (key) {
                    "gain" -> dsParams["Gain"]
                    "level" -> dsParams["Level"]
                    "bass" -> dsParams["Bass"]
                    "middle" -> dsParams["Middle"] ?: dsParams["Mid"]
                    "treble" -> dsParams["Treble"] ?: dsParams["Tone"] ?: dsParams["tone"]
                    "reso" -> dsParams["Reso"]
                    "pres" -> dsParams["Pres"] ?: dsParams["Presence"]
                    "bright" -> dsParams["Bright"]
                    "tone" -> dsParams["Tone"] ?: dsParams["Treble"] ?: dsParams["treble"]
                    "filter" -> dsParams["Filter"]
                    "fuzz" -> dsParams["Fuzz"]
                    "sustain" -> dsParams["Sustain"]
                    "blend" -> dsParams["Blend"]
                    "grunt" -> dsParams["Grunt"]
                    "attack" -> dsParams["Attack"]
                    "master" -> dsParams["Master"]
                    "loMidFreq" -> dsParams["LoMid Freq"] ?: dsParams["LoMidFreq"]
                    "loMid" -> dsParams["LoMid"]
                    "hiMidFreq" -> dsParams["HiMid Freq"] ?: dsParams["HiMidFreq"]
                    "hiMid" -> dsParams["HiMid"]
                    "LPtype" -> dsParams["Type"] ?: dsParams["lptype"] ?: dsParams["LPType"]
                    "mode" -> dsParams["Mode"]
                    "boost" -> dsParams["Boost"]
                    "gate" -> dsParams["Gate"]
                    "gate mode" -> dsParams["Gate Mode"] ?: dsParams["gateMode"]
                    else -> null
                }
                val rawVal = dsParams[key] ?: fallbackAlias ?: dsItem.defaultParams[key] ?: 50f
                rawVal.toInt().coerceIn(0, 100).toShort()
            } else {
                dsDefaultSlots[i]
            }
            buffer.putShort(v)
        }

        // [3] AMP (Offset 0x84) - 120 Modelos oficiais MK-20 (8 Parâmetros Globais)
        val ampModelName = preset.pedals.AMP?.model ?: "26CL2_JC120"
        val ampParams = preset.pedals.AMP?.params ?: emptyMap()
        val ampItem = com.example.data.model.Mk20Catalog.findAmp(ampModelName)
        val ampKeys = listOf("Gain", "Level", "Bass", "Middle", "Treble", "Reso", "Pres", "Bright")
        buffer.position(0x84)
        for (i in 0 until 12) {
            val key = ampKeys.getOrNull(i)
            val v = if (key != null) {
                val fallbackAlias = when (key) {
                    "Level" -> ampParams["Volume"]
                    "Middle" -> ampParams["Mid"]
                    "Pres" -> ampParams["Presence"]
                    else -> null
                }
                val rawVal = ampParams[key] ?: fallbackAlias ?: ampItem?.defaultParams?.get(key) ?: 50f
                rawVal.toInt().coerceIn(0, 100).toShort()
            } else {
                0.toShort()
            }
            buffer.putShort(v)
        }

        // [4] CAB (Offset 0x9c) - 100 Gabinetes / IRs oficiais (3 Parâmetros: Level 0..100, Low Cut 20..300, High Cut 50..180 [5.0K..18.0K])
        val cabModelName = preset.pedals.CAB?.model ?: "20FdTwRev212A"
        val cabParams = preset.pedals.CAB?.params ?: emptyMap()
        val cabItem = com.example.data.model.Mk20Catalog.findCab(cabModelName)
        val cabLevel = (cabParams["Level"] ?: cabParams["Volume"] ?: cabItem?.defaultParams?.get("Level") ?: 80f).toInt().coerceIn(0, 100).toShort()
        val cabLowCut = (cabParams["Low Cut"] ?: cabItem?.defaultParams?.get("Low Cut") ?: 80f).toInt().coerceIn(20, 300).toShort()
        val cabHighCutRaw = cabParams["High Cut"] ?: cabItem?.defaultParams?.get("High Cut") ?: 7000f
        val cabHighCut = when {
            cabHighCutRaw <= 20f -> kotlin.math.round(cabHighCutRaw * 10f).toInt()
            cabHighCutRaw > 200f -> kotlin.math.round(cabHighCutRaw / 100f).toInt()
            else -> kotlin.math.round(cabHighCutRaw).toInt()
        }.coerceIn(50, 180).toShort()
        buffer.position(0x9c)
        buffer.putShort(cabLevel)
        buffer.putShort(cabLowCut)
        buffer.putShort(cabHighCut)
        for (i in 3 until 12) {
            buffer.putShort(0.toShort())
        }

        // [5] EQ (Offset 0xb4) - 3 Modelos de Equalizador (Guitar EQ 6, Bass EQ 7, Normal EQ 10: Min=-12.0, Max=+12.0 dB, 0=Flat)
        val eqModelName = preset.pedals.EQ?.model ?: "Guitar EQ 6"
        val eqParams = preset.pedals.EQ?.params ?: emptyMap()
        val eqItem = com.example.data.model.Mk20Catalog.findEq(eqModelName) ?: com.example.data.model.Mk20Catalog.EQ_MODELS.first()
        val eqKeys = eqItem.defaultParams.keys.toList()
        buffer.position(0xb4)
        for (i in 0 until 12) {
            val key = eqKeys.getOrNull(i)
            val v = if (key != null) {
                val rawVal = eqParams[key] ?: eqItem.defaultParams[key] ?: 0f
                val dbVal = if (rawVal > 12f) (rawVal - 50f) * 0.24f else rawVal
                kotlin.math.round(dbVal).toInt().coerceIn(-12, 12).toShort()
            } else {
                0.toShort()
            }
            buffer.putShort(v)
        }

        // [6] MOD (Offset 0xcc) - 20 Modelos oficiais de Modulação
        val modModelName = preset.pedals.MOD?.model ?: "CHORUS"
        val modParams = preset.pedals.MOD?.params ?: emptyMap()
        val modItem = com.example.data.model.Mk20Catalog.findMod(modModelName) ?: com.example.data.model.Mk20Catalog.MOD_MODELS.first()
        val modKeys = modItem.defaultParams.keys.toList()
        buffer.position(0xcc)
        for (i in 0 until 12) {
            val key = modKeys.getOrNull(i)
            val v = if (key != null) {
                val fallbackAlias = when (key) {
                    "speed" -> modParams["Speed"]
                    "depth" -> modParams["Depth"]
                    "mix" -> modParams["Mix"]
                    "fb" -> modParams["Fb"] ?: modParams["Feedback"]
                    "level" -> modParams["Level"]
                    "midcut" -> modParams["MidCut"] ?: modParams["Midcut"]
                    "reso" -> modParams["Reso"]
                    "min" -> modParams["Min"]
                    "max" -> modParams["Max"]
                    "stage" -> modParams["Stage"]
                    "regen" -> modParams["Regen"]
                    "lfo" -> modParams["Lfo"] ?: modParams["LFO"]
                    "jet" -> modParams["Jet"]
                    "mode" -> modParams["Mode"] ?: modParams["Mod"] ?: modParams["Harm"] ?: modParams["harm"]
                    "bits" -> modParams["Bits"]
                    "sync" -> modParams["Sync"]
                    "syncBpm" -> modParams["SyncBPM"] ?: modParams["Sync BPM"] ?: modParams["syncBPM"]
                    else -> null
                }
                val rawVal = modParams[key] ?: fallbackAlias ?: modItem.defaultParams[key] ?: 50f
                val coerced = when {
                    key.equals("speed", ignoreCase = true) -> {
                        // Na MK-20, speed (0.1..10.0 ou 0.17..16.0) é salvo multiplicado por 10 (ex: 1.0 -> 10)
                        val scaled = if (rawVal <= 16.0f) kotlin.math.round(rawVal * 10f).toInt() else rawVal.toInt()
                        scaled.coerceIn(1, 160)
                    }
                    key.equals("sync", ignoreCase = true) -> if (rawVal != 0f) 1 else 0
                    key.equals("syncBpm", ignoreCase = true) -> rawVal.toInt().coerceIn(40, 240)
                    else -> rawVal.toInt().coerceIn(0, 100)
                }
                coerced.toShort()
            } else {
                0.toShort()
            }
            buffer.putShort(v)
        }

        // [7] DELAY (Offset 0xe4) - 27 Modelos oficiais de Delay da MK-20 (Time: 0.100..2.000s -> 100..2000ms)
        val dlyModelName = preset.pedals.DELAY?.model ?: "Analog"
        val dlyParams = preset.pedals.DELAY?.params ?: emptyMap()
        val dlyItem = com.example.data.model.Mk20Catalog.findDelay(dlyModelName) ?: com.example.data.model.Mk20Catalog.DELAY_MODELS[3]
        val dlyKeys = dlyItem.defaultParams.keys.toList()
        buffer.position(0xe4)
        for (i in 0 until 12) {
            val key = dlyKeys.getOrNull(i)
            val v = if (key != null) {
                val fallbackAlias = when (key) {
                    "Fb", "Feedback" -> dlyParams["Fb"] ?: dlyParams["Feedback"] ?: dlyParams["Repeats"]
                    "Time" -> dlyParams["Time"] ?: dlyParams["Time L"] ?: dlyParams["Time 1"]
                    "Dive", "TremobDepth" -> dlyParams["Dive"] ?: dlyParams["TremobDepth"] ?: dlyParams["Tremolo Depth"]
                    "SyncBPM" -> dlyParams["SyncBPM"] ?: dlyParams["Sync BPM"] ?: dlyParams["syncBpm"]
                    else -> null
                }
                val rawVal = dlyParams[key] ?: fallbackAlias ?: dlyItem.defaultParams[key] ?: 50f
                val coerced = when {
                    key.startsWith("Time", ignoreCase = true) -> {
                        val msVal = if (rawVal <= 2.0f) kotlin.math.round(rawVal * 1000f).toInt() else rawVal.toInt()
                        msVal.coerceIn(100, 2000)
                    }
                    key.equals("Sync", ignoreCase = true) -> if (rawVal != 0f) 1 else 0
                    key.equals("SyncBPM", ignoreCase = true) -> rawVal.toInt().coerceIn(40, 240)
                    key.equals("Pitch", ignoreCase = true) -> rawVal.toInt().coerceIn(0, 12)
                    key.equals("Pattern", ignoreCase = true) -> rawVal.toInt().coerceIn(0, 19)
                    else -> rawVal.toInt().coerceIn(0, 100)
                }
                coerced.toShort()
            } else {
                0.toShort()
            }
            buffer.putShort(v)
        }

        // [8] REVERB (Offset 0xfc) - 18 Modelos oficiais de Reverb
        val revModelName = preset.pedals.REVERB?.model ?: "plate"
        val revParams = preset.pedals.REVERB?.params ?: emptyMap()
        val revItem = com.example.data.model.Mk20Catalog.findReverb(revModelName) ?: com.example.data.model.Mk20Catalog.REVERB_MODELS[2]
        val revKeys = revItem.defaultParams.keys.toList()
        buffer.position(0xfc)
        for (i in 0 until 12) {
            val key = revKeys.getOrNull(i)
            val v = if (key != null) {
                val fallbackAlias = when (key) {
                    "decay" -> revParams["Decay"]
                    "mix" -> revParams["Mix"]
                    "highPass" -> revParams["High Pass"] ?: revParams["HighPass"]
                    "lowPass" -> revParams["Low Pass"] ?: revParams["LowPass"]
                    "modDepth" -> revParams["Mod Depth"] ?: revParams["ModDepth"]
                    "combs" -> revParams["Combs"]
                    "tone" -> revParams["Tone"]
                    "pitch" -> revParams["Pitch"]
                    "amount" -> revParams["Amount"]
                    "lend" -> revParams["Lend"] ?: revParams["Lens"]
                    "length" -> revParams["Length"]
                    "diff" -> revParams["Diff"] ?: revParams["Drift"]
                    "sampleRete" -> revParams["SampleRete"] ?: revParams["Sample Rate"] ?: revParams["sampleRate"]
                    "noiseLevel" -> revParams["NoiseLevel"] ?: revParams["Noise Level"]
                    "riseTime" -> revParams["RiseTime"] ?: revParams["Rise Time"]
                    else -> null
                }
                val rawVal = revParams[key] ?: fallbackAlias ?: revItem.defaultParams[key] ?: 20f
                rawVal.toInt().coerceIn(0, 100).toShort()
            } else {
                0.toShort()
            }
            buffer.putShort(v)
        }

        // [9] VOL (Offset 0x114) - Volume do módulo VOL (0..100, padrão 100 na MK-20)
        val volModuleLevel = (preset.pedals.VOL?.params?.get("Level") ?: 100f).toInt().coerceIn(0, 100).toShort()
        buffer.position(0x114)
        buffer.putShort(volModuleLevel)

        return result
    }

    /**
     * Deserializa arquivos binários (.mk20 / .amk20) suportando o formato oficial de 412 bytes
     * e os formatos de compatibilidade (256B, 332B, 512B, 1024B).
     */
    fun deserialize(bytes: ByteArray): Mk20Preset {
        require(bytes.size >= 80) { "Tamanho insuficiente para preset MK-20 binário (${bytes.size} bytes)" }

        if (bytes.size == AUTHENTIC_MK20_SIZE) {
            return deserializeAuthentic412(bytes)
        }

        return deserializeGeneric(bytes)
    }

    private fun deserializeAuthentic412(bytes: ByteArray): Mk20Preset {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // 1. Título (0..19)
        val title = readPaddedString(buffer, 20).ifBlank { "Preset MK-20" }

        // 2. Preset Volume (0x18) e BPM (0x1a)
        buffer.position(0x18)
        val masterVol = buffer.short.toInt().coerceIn(0, 100)
        buffer.position(0x1a)
        val bpm = buffer.short.toInt().coerceIn(40, 240)

        // 3. Flags ativos dos 10 blocos (0x28..0x31)
        buffer.position(0x28)
        val fxActive = buffer.get() != 0.toByte()
        val nsActive = buffer.get() != 0.toByte()
        val dsActive = buffer.get() != 0.toByte()
        val ampActive = buffer.get() != 0.toByte()
        val cabActive = buffer.get() != 0.toByte()
        val eqActive = buffer.get() != 0.toByte()
        val modActive = buffer.get() != 0.toByte()
        val dlyActive = buffer.get() != 0.toByte()
        val revActive = buffer.get() != 0.toByte()
        val volActive = buffer.get() != 0.toByte()

        // 4. IDs de Modelo dos 10 blocos (0x32..0x3b)
        buffer.position(0x32)
        val fxModelId = (buffer.get().toInt() and 0xFF) + 1
        val nsModelId = (buffer.get().toInt() and 0xFF) + 1
        val dsModelId = (buffer.get().toInt() and 0xFF) + 1
        val ampModelId = (buffer.get().toInt() and 0xFF) + 1
        val cabModelRaw = buffer.get().toInt() and 0xFF
        val eqModelId = (buffer.get().toInt() and 0xFF) + 1
        val modModelId = (buffer.get().toInt() and 0xFF) + 1
        val dlyModelId = (buffer.get().toInt() and 0xFF) + 1
        val revModelId = (buffer.get().toInt() and 0xFF) + 1
        val volModelId = (buffer.get().toInt() and 0xFF) + 1

        // [0] FX (0x3c) - 20 Modelos oficiais MK-20
        val fxItem = com.example.data.model.Mk20Catalog.findFx(fxModelId) ?: com.example.data.model.Mk20Catalog.findFx("Compress")
        val fxModelName = fxItem?.name ?: "Compress"
        val fxKeys = fxItem?.defaultParams?.keys?.toList() ?: listOf("Sustain", "Attack", "Level", "Blend")
        buffer.position(0x3c)
        val fxShorts = ShortArray(12) { buffer.short }
        val fxParamsMap = mutableMapOf<String, Float>()
        if (fxModelId == 1) {
            // 1: Wah-Wah: [0]=Speed(1..100 -> 0.1..10.0), [1]=Q, [2]=Mix, [3]=Width, [4]=Level, [5]=Sync, [7]=SyncBpm
            fxParamsMap["Speed"] = (fxShorts[0].toFloat() / 10f).coerceIn(0.1f, 10.0f)
            fxParamsMap["Q"] = fxShorts[1].toFloat().coerceIn(0f, 100f)
            fxParamsMap["Mix"] = fxShorts[2].toFloat().coerceIn(0f, 100f)
            fxParamsMap["Width"] = fxShorts[3].toFloat().coerceIn(0f, 100f)
            fxParamsMap["Level"] = fxShorts[4].toFloat().coerceIn(0f, 100f)
            fxParamsMap["Sync"] = if (fxShorts[5].toInt() != 0) 1f else 0f
            val syncBpmRaw = if (fxShorts[7].toInt() in 40..240) fxShorts[7].toFloat() else if (fxShorts[6].toInt() in 40..240) fxShorts[6].toFloat() else 80f
            fxParamsMap["SyncBpm"] = syncBpmRaw
        } else {
            for (i in fxKeys.indices) {
                val key = fxKeys[i]
                val rawVal = fxShorts.getOrElse(i) { 0 }.toFloat()
                fxParamsMap[key] = if (isFxTenScaleParam(fxModelId, key) && rawVal > 10f) {
                    (rawVal / 10f).coerceIn(0f, 10f)
                } else {
                    rawVal
                }
            }
        }
        fxItem?.defaultParams?.forEach { (k, def) ->
            if (!fxParamsMap.containsKey(k)) fxParamsMap[k] = def
        }

        // [1] NS (0x54) - 8 Modelos oficiais MK-20
        val nsItem = com.example.data.model.Mk20Catalog.findNs(nsModelId) ?: com.example.data.model.Mk20Catalog.NS_MODELS[2]
        val nsModelName = nsItem.name
        val nsKeys = nsItem.defaultParams.keys.toList()
        buffer.position(0x54)
        val nsParamsMap = mutableMapOf<String, Float>()
        for (i in 0 until 12) {
            val v = buffer.short.toFloat()
            val key = nsKeys.getOrNull(i)
            if (key != null) {
                nsParamsMap[key] = v
            }
        }
        nsItem.defaultParams.forEach { (k, def) ->
            if (!nsParamsMap.containsKey(k)) nsParamsMap[k] = def
        }

        // [2] DS (0x6c) - 52 Modelos oficiais MK-20
        val dsItem = com.example.data.model.Mk20Catalog.findDs(dsModelId) ?: com.example.data.model.Mk20Catalog.DS_MODELS.first()
        val dsModelName = dsItem.name
        val dsKeys = dsItem.defaultParams.keys.toList()
        buffer.position(0x6c)
        val dsParamsMap = mutableMapOf<String, Float>()
        for (i in 0 until 12) {
            val v = buffer.short.toFloat()
            val key = dsKeys.getOrNull(i)
            if (key != null) {
                dsParamsMap[key] = v
            }
        }
        dsItem.defaultParams.forEach { (k, def) ->
            if (!dsParamsMap.containsKey(k)) dsParamsMap[k] = def
        }

        // [3] AMP (0x84) - 120 Modelos oficiais MK-20 (8 Parâmetros Globais)
        val ampItem = com.example.data.model.Mk20Catalog.findAmp(ampModelId)
        val ampModelName = ampItem?.name ?: "${ampModelId}AMP_MK20"
        val ampKeys = listOf("Gain", "Level", "Bass", "Middle", "Treble", "Reso", "Pres", "Bright")
        buffer.position(0x84)
        val ampParamsMap = mutableMapOf<String, Float>()
        for (i in 0 until 12) {
            val v = buffer.short.toFloat()
            val key = ampKeys.getOrNull(i)
            if (key != null) {
                ampParamsMap[key] = v
            }
        }
        ampItem?.defaultParams?.forEach { (k, def) ->
            if (!ampParamsMap.containsKey(k)) ampParamsMap[k] = def
        }

        // [4] CAB (0x9c) - 100 Gabinetes / IRs (Level 0..100, Low Cut 20..300, High Cut 5000..18000)
        val cabModelName = if (cabModelRaw == 0xFF) {
            "User IR / Bypass"
        } else {
            com.example.data.model.Mk20Catalog.findCab(cabModelRaw + 1)?.name
                ?: "${cabModelRaw + 1}Cab_MK20"
        }
        buffer.position(0x9c)
        val cabLevel = buffer.short.toFloat().coerceIn(0f, 100f)
        val cabLowCutRaw = buffer.short.toInt()
        val cabLowCut = (if (cabLowCutRaw in 20..300) cabLowCutRaw.toFloat() else 80f)
        val cabHighCutRaw = buffer.short.toInt()
        val cabHighCut = when {
            cabHighCutRaw in 50..180 -> (cabHighCutRaw * 100).toFloat()
            cabHighCutRaw in 5000..18000 -> cabHighCutRaw.toFloat()
            else -> 7000f
        }
        val cabParamsMap = mapOf(
            "Level" to cabLevel,
            "Low Cut" to cabLowCut,
            "High Cut" to cabHighCut
        )

        // [5] EQ (0xb4) - 3 Modelos (Guitar EQ 6, Bass EQ 7, Normal EQ 10: -12.0 a +12.0 dB)
        val eqItem = com.example.data.model.Mk20Catalog.findEq(eqModelId) ?: com.example.data.model.Mk20Catalog.EQ_MODELS.first()
        val eqModelName = eqItem.name
        val eqKeys = eqItem.defaultParams.keys.toList()
        buffer.position(0xb4)
        val eqParamsMap = mutableMapOf<String, Float>()
        for (i in 0 until 12) {
            val rawShort = buffer.short.toInt()
            val key = eqKeys.getOrNull(i)
            if (key != null) {
                val decoded = when {
                    rawShort in -12..12 -> rawShort.toFloat()
                    rawShort in -120..120 -> (rawShort / 10f).coerceIn(-12f, 12f)
                    else -> 0f
                }
                eqParamsMap[key] = decoded
            }
        }
        eqItem.defaultParams.forEach { (k, def) ->
            if (!eqParamsMap.containsKey(k)) eqParamsMap[k] = def
        }

        // [6] MOD (0xcc) - 20 Modelos oficiais de Modulação
        val modItem = com.example.data.model.Mk20Catalog.findMod(modModelId) ?: com.example.data.model.Mk20Catalog.MOD_MODELS.first()
        val modModelName = modItem.name
        val modKeys = modItem.defaultParams.keys.toList()
        buffer.position(0xcc)
        val modParamsMap = mutableMapOf<String, Float>()
        for (i in 0 until 12) {
            val v = buffer.short.toFloat()
            val key = modKeys.getOrNull(i)
            if (key != null) {
                modParamsMap[key] = if (key.equals("speed", ignoreCase = true) && v >= 1f) {
                    (v / 10f).coerceIn(0.1f, 16.0f)
                } else {
                    v
                }
            }
        }
        modItem.defaultParams.forEach { (k, def) ->
            if (!modParamsMap.containsKey(k)) modParamsMap[k] = def
        }

        // [7] DELAY (0xe4) - 27 Modelos oficiais de Delay da MK-20
        val dlyItem = com.example.data.model.Mk20Catalog.findDelay(dlyModelId) ?: com.example.data.model.Mk20Catalog.DELAY_MODELS[3]
        val dlyModelName = dlyItem.name
        val dlyKeys = dlyItem.defaultParams.keys.toList()
        buffer.position(0xe4)
        val dlyParamsMap = mutableMapOf<String, Float>()
        for (i in 0 until 12) {
            val v = buffer.short.toFloat()
            val key = dlyKeys.getOrNull(i)
            if (key != null) {
                dlyParamsMap[key] = if (key.startsWith("Time", ignoreCase = true)) {
                    v.coerceIn(100f, 2000f)
                } else {
                    v
                }
            }
        }
        dlyItem.defaultParams.forEach { (k, def) ->
            if (!dlyParamsMap.containsKey(k)) dlyParamsMap[k] = def
        }

        // [8] REVERB (0xfc) - 18 Modelos oficiais de Reverb
        val revItem = com.example.data.model.Mk20Catalog.findReverb(revModelId) ?: com.example.data.model.Mk20Catalog.REVERB_MODELS[2]
        val revModelName = revItem.name
        val revKeys = revItem.defaultParams.keys.toList()
        buffer.position(0xfc)
        val revParamsMap = mutableMapOf<String, Float>()
        for (i in 0 until 12) {
            val v = buffer.short.toFloat()
            val key = revKeys.getOrNull(i)
            if (key != null) {
                revParamsMap[key] = v
            }
        }
        revItem.defaultParams.forEach { (k, def) ->
            if (!revParamsMap.containsKey(k)) revParamsMap[k] = def
        }

        // [9] VOL (0x114)
        buffer.position(0x114)
        val volLevel = buffer.short.toInt().coerceIn(0, 100)

        return Mk20Preset(
            title = title,
            style = "MK-20 Preset",
            bpm = bpm,
            masterVolume = masterVol,
            youtubeLink = "",
            details = "Preset MK-20 autêntico de 412 bytes ($title)",
            pedals = Mk20Pedals(
                FX = PedalModule("Pré-Efeito / Dinâmica", fxActive, fxModelName, fxParamsMap),
                NS = PedalModule("Noise Suppressor", nsActive, nsModelName, nsParamsMap),
                DS = PedalModule("Drive / Boost", dsActive, dsModelName, dsParamsMap),
                AMP = PedalModule("Amplificador", ampActive, ampModelName, ampParamsMap),
                CAB = PedalModule("Caixa / IR", cabActive, cabModelName, cabParamsMap),
                EQ = PedalModule("Equalizador", eqActive, eqModelName, eqParamsMap),
                MOD = PedalModule("Modulação", modActive, modModelName, modParamsMap),
                DELAY = PedalModule("Delay", dlyActive, dlyModelName, dlyParamsMap),
                REVERB = PedalModule("Reverb", revActive, revModelName, revParamsMap),
                VOL = PedalModule("Master Volume", volActive, "Master", mapOf("Level" to volLevel.toFloat()))
            )
        )
    }

    private fun deserializeGeneric(bytes: ByteArray): Mk20Preset {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Se tiver cabeçalho MK20 ou AMK2
        val m0 = buffer.get()
        val m1 = buffer.get()
        val m2 = buffer.get()
        val m3 = buffer.get()
        val isMk20 = m0 == MAGIC_MK20[0] && m1 == MAGIC_MK20[1] && m2 == MAGIC_MK20[2] && m3 == MAGIC_MK20[3]
        val isAmk2 = m0 == MAGIC_AMK2[0] && m1 == MAGIC_AMK2[1] && m2 == MAGIC_AMK2[2] && m3 == MAGIC_AMK2[3]

        if (!isMk20 && !isAmk2) {
            // Tenta tratar como string ou formato plano
            val titleStr = String(bytes.take(20).toByteArray(), StandardCharsets.UTF_8).trim()
            return Mk20Preset.DEFAULT_PRESET.copy(title = titleStr.ifBlank { "Preset MK-20" })
        }

        buffer.short // version
        buffer.short // deviceId

        val bpm = buffer.short.toInt().coerceIn(40, 250)
        val masterVol = (buffer.get().toInt() and 0xFF).coerceIn(0, 100).takeIf { it > 0 } ?: 40
        buffer.get() // Reserved
        buffer.short // Bitmask

        val title = readPaddedString(buffer, 32).ifBlank { "Preset MK-20" }
        val style = readPaddedString(buffer, 26).ifBlank { "Personalizado" }

        return Mk20Preset.DEFAULT_PRESET.copy(
            title = title,
            style = style,
            bpm = bpm,
            masterVolume = masterVol
        )
    }

    fun applyToTemplate(templateBytes: ByteArray, preset: Mk20Preset): ByteArray {
        if (templateBytes.size == AUTHENTIC_MK20_SIZE) {
            return serializeAuthentic412(preset, templateBytes)
        }
        val result = templateBytes.clone()
        val buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)

        if (result.size >= 72) {
            buffer.position(8)
            buffer.putShort(preset.bpm.coerceIn(40, 250).toShort())
            buffer.put(preset.masterVolume.coerceIn(0, 100).toByte())
            buffer.position(12)
            writePaddedString(buffer, preset.title, 32)
        }

        if (result.size >= 16) {
            val trailerStart = result.size - 8
            val crc = CRC32()
            crc.update(result, 0, trailerStart)
            buffer.position(trailerStart)
            buffer.putInt(crc.value.toInt())
        }

        return result
    }

    private fun serializeFull(preset: Mk20Preset, targetSize: Int, useAmk2Header: Boolean): ByteArray {
        val buffer = ByteBuffer.allocate(targetSize).order(ByteOrder.LITTLE_ENDIAN)
        val magic = if (useAmk2Header) MAGIC_AMK2 else MAGIC_MK20
        buffer.put(magic)
        buffer.putShort(FILE_VERSION)
        buffer.putShort(DEVICE_ID_MK20)
        buffer.putShort(preset.bpm.coerceIn(40, 250).toShort())
        buffer.put(preset.masterVolume.coerceIn(0, 100).toByte())
        buffer.put(0.toByte())
        buffer.putShort(0.toShort())
        writePaddedString(buffer, preset.title, 32)
        writePaddedString(buffer, preset.style, 26)

        val trailerStart = targetSize - 8
        while (buffer.position() < trailerStart) {
            buffer.put(0.toByte())
        }
        val crc = CRC32()
        crc.update(buffer.array(), 0, trailerStart)
        buffer.putInt(crc.value.toInt())
        buffer.put(TRAILER_TONE)
        return buffer.array()
    }

    private fun serializeCompact256(preset: Mk20Preset, useAmk2Header: Boolean): ByteArray {
        val buffer = ByteBuffer.allocate(SIZE_256).order(ByteOrder.LITTLE_ENDIAN)
        val magic = if (useAmk2Header) MAGIC_AMK2 else MAGIC_MK20
        buffer.put(magic)
        buffer.putShort(FILE_VERSION)
        buffer.putShort(DEVICE_ID_MK20)
        buffer.putShort(preset.bpm.coerceIn(40, 250).toShort())
        buffer.put(preset.masterVolume.coerceIn(0, 100).toByte())
        buffer.put(0.toByte())
        writePaddedString(buffer, preset.title, 20)
        writePaddedString(buffer, preset.style, 8)
        while (buffer.position() < 248) {
            buffer.put(0.toByte())
        }
        val crc = CRC32()
        crc.update(buffer.array(), 0, 248)
        buffer.putInt(crc.value.toInt())
        buffer.put(TRAILER_TONE)
        return buffer.array()
    }

    /**
     * Escreve uma string sanitizada em ASCII com garantia de terminação nula (0x00).
     */
    fun writePaddedAsciiString(buffer: ByteBuffer, str: String, length: Int) {
        val normalized = str
            .replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a")
            .replace("é", "e").replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o").replace("õ", "o").replace("ô", "o")
            .replace("ú", "u")
            .replace("ç", "c")
            .replace("Á", "A").replace("À", "A").replace("Ã", "A").replace("Â", "A")
            .replace("É", "E").replace("Ê", "E")
            .replace("Í", "I")
            .replace("Ó", "O").replace("Õ", "O").replace("Ô", "O")
            .replace("Ú", "U")
            .replace("Ç", "C")
            .replace("[^\\x20-\\x7E]".toRegex(), "")
            .trim()
            .ifBlank { "Preset" }

        val clean = normalized
            .replace("[^a-zA-Z0-9 _\\-]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .take(19)
            .ifBlank { "USER PRESET" }

        val bytes = clean.toByteArray(StandardCharsets.US_ASCII)
        val copyLen = bytes.size.coerceAtMost(length - 1)
        buffer.put(bytes, 0, copyLen)
        for (i in copyLen until length) {
            buffer.put(0.toByte())
        }
    }

    private fun writePaddedString(buffer: ByteBuffer, str: String, length: Int) {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        val copyLen = bytes.size.coerceAtMost(length)
        buffer.put(bytes, 0, copyLen)
        for (i in copyLen until length) {
            buffer.put(0.toByte())
        }
    }

    private fun readPaddedString(buffer: ByteBuffer, length: Int): String {
        val bytes = ByteArray(length)
        buffer.get(bytes)
        var nullIndex = bytes.indexOf(0.toByte())
        if (nullIndex == -1) nullIndex = length
        return String(bytes, 0, nullIndex, StandardCharsets.UTF_8).trim()
    }

    private fun extractModelByte(modelName: String?, defaultVal: Byte): Byte {
        if (modelName.isNullOrBlank()) return defaultVal
        if (modelName.contains("User IR", ignoreCase = true) || modelName.contains("Bypass", ignoreCase = true)) {
            return 0xFF.toByte()
        }
        val digits = modelName.trim().takeWhile { it.isDigit() }
        if (digits.isNotBlank()) {
            val num = digits.toIntOrNull()
            if (num != null && num > 0) {
                // No protocolo oficial da MK-20, os IDs de modelo em 0x32..0x3B são 0-indexados:
                // Modelo #1 ("1BLUES_OD") -> 0x00
                // Modelo #13 ("13CL1_FenDi") -> 0x0C (12 decimal)
                // Modelo #80 ("80DS_5150III") -> 0x4F (79 decimal)
                return (num - 1).coerceIn(0, 255).toByte()
            }
        }
        return defaultVal
    }

    private fun extractFxModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 8.toByte() // 8 = Compress (Model 9)
        val item = com.example.data.model.Mk20Catalog.findFx(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 19).toByte()
        }
        val digits = modelName.trim().takeWhile { it.isDigit() }
        val num = digits.toIntOrNull()
        if (num != null && num in 1..20) {
            return (num - 1).toByte()
        }
        return 8.toByte()
    }

    private fun extractNsModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 2.toByte() // 2 = Soft Gate (Model 3)
        val item = com.example.data.model.Mk20Catalog.findNs(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 7).toByte()
        }
        val digits = modelName.trim().takeWhile { it.isDigit() }
        val num = digits.toIntOrNull()
        if (num != null && num in 1..8) {
            return (num - 1).toByte()
        }
        return 2.toByte()
    }

    private fun extractDsModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 0.toByte() // 0 = MW-808 (Model 1)
        val item = com.example.data.model.Mk20Catalog.findDs(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 51).toByte()
        }
        val digits = modelName.trim().takeWhile { it.isDigit() }
        val num = digits.toIntOrNull()
        if (num != null && num in 1..52) {
            return (num - 1).toByte()
        }
        return 0.toByte()
    }

    private fun extractAmpModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 12.toByte() // 12 = 13CL1_FenDi (Model 13)
        val item = com.example.data.model.Mk20Catalog.findAmp(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 119).toByte()
        }
        return extractModelByte(modelName, 12.toByte())
    }

    private fun extractCabModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 19.toByte() // 19 = 20FdTwRev212A (Model 20)
        val item = com.example.data.model.Mk20Catalog.findCab(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 99).toByte()
        }
        val extracted = extractModelByte(modelName, 19.toByte()).toInt() and 0xFF
        return if (extracted in 0..99) extracted.toByte() else 19.toByte()
    }

    fun getAuthenticTemplateBytes(): ByteArray = AUTHENTIC_MK20_TEMPLATE.clone()

    private fun extractEqModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 0.toByte() // 0 = Guitar EQ 6
        val item = com.example.data.model.Mk20Catalog.findEq(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 2).toByte()
        }
        return (extractModelByte(modelName, 0.toByte()).toInt() and 0xFF).coerceIn(0, 2).toByte()
    }

    private fun extractModModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 0.toByte() // 0 = CHORUS (Model 1)
        val item = com.example.data.model.Mk20Catalog.findMod(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 19).toByte()
        }
        return (extractModelByte(modelName, 0.toByte()).toInt() and 0xFF).coerceIn(0, 19).toByte()
    }

    private fun extractDelayModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 3.toByte() // 3 = Analog (Model 4)
        val item = com.example.data.model.Mk20Catalog.findDelay(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 26).toByte()
        }
        return (extractModelByte(modelName, 3.toByte()).toInt() and 0xFF).coerceIn(0, 26).toByte()
    }

    private fun extractReverbModelByte(modelName: String?): Byte {
        if (modelName.isNullOrBlank()) return 2.toByte() // 2 = plate (Model 3)
        val item = com.example.data.model.Mk20Catalog.findReverb(modelName)
        if (item != null) {
            return (item.id - 1).coerceIn(0, 17).toByte()
        }
        return (extractModelByte(modelName, 2.toByte()).toInt() and 0xFF).coerceIn(0, 17).toByte()
    }
}
