# Ponto de Reversão: Pré-Matrizes Oficiais Travadas & Google Search Grounding

Data do Snapshot: 29/10/2026
Nome de Referência: **`Ponto de Reversão - Pré-Matrizes Oficiais`**

Se você desejar voltar exatamente a este ponto a qualquer momento, basta pedir:
> *"Reverta para o Ponto de Reversão - Pré-Matrizes Oficiais"*

---

## 1. `app/src/main/java/com/example/data/remote/GeminiService.kt`
- Modelo: `v1beta/models/gemini-3.5-flash:generateContent`
- Payload padrão sem `tools = listOf(GoogleSearchTool())`

## 2. `app/src/main/java/com/example/util/AcousticStudioGuardrail.kt`
- Volume Master: `preset.masterVolume.coerceIn(55, 75)`
- Noise Gate: `-48f` (effectiveGain >= 80), `-52f` (effectiveGain >= 45), `-58f` (Clean)
- Bright Switch: `>= 65f`
- High Cut: `6000f..9500f` (padrão `7800f`)
- Low Cut: `70f..95f` (padrão `75f..80f`)

## 3. `app/src/main/java/com/example/data/remote/Mk20ToneEngine.kt`
- Geração via `SYSTEM_INSTRUCTION` livre com sanitização por `AcousticStudioGuardrail.sanitize()`
- `synthesizePresetLocally` com busca em `Mk20MasterSongDatabase` e 6 perfis base.

## 4. `app/src/main/java/com/example/data/remote/Mk20ToneAssistantEngine.kt`
- Assistente conversacional via `consultAndAdjust` + fallback local `adjustLocally` com 6 categorias de ajuste e `AcousticStudioGuardrail.sanitize()`.
