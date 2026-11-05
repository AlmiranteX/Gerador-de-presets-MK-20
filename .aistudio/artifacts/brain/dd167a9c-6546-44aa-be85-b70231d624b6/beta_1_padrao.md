# Versão Padrão de Partida: `beta-1`

Data do Snapshot: **29/10/2026**  
Identificador Oficial: **`beta-1`** (Versão Padrão — Matrizes Oficiais + Search Grounding)

Sempre que você quiser retornar exatamente a este ponto de partida padrão (pronto para loja/produção ou para iniciar novas evoluções), basta pedir:
> *"Voltar para a versão padrão beta-1"*

---

## 1. Arquitetura Consolidada na `beta-1`

1. **8 Matrizes Oficiais de Hardware M-VAVE MK-20 (`Mk20OfficialToneMatrix.kt`)**
   - `GOSPEL_MELODIC_SOLO`: `UK 800` + `UK 4x12 (V30)` + `T-Scream` + `Comp` | `Master Vol = 65` | `AMP Level = 78` | `CAB Level = 85` | `High Cut = 7500Hz` | `NS thd = -52dB`
   - `WORSHIP_AMBIENT_CHIME`: `AC 30` + `AC 2x12 (Blue)` | `Master Vol = 64` | `High Cut = 7800Hz` | `NS thd = -58dB`
   - `CLEAN_STUDIO_POP_FUNK`: `US Deluxe` + `US 1x12` | `Master Vol = 64` | `High Cut = 8000Hz` | `NS thd = -58dB`
   - `BRITISH_CRUNCH_ROCK`: `UK 800` + `UK 4x12 (V30)` | `Master Vol = 64` | `High Cut = 7400Hz` | `NS thd = -52dB`
   - `BLUES_TEXAS_OVERDRIVE`: `US Tweed` + `US 2x12` | `Master Vol = 64` | `High Cut = 7500Hz` | `NS thd = -54dB`
   - `MODERN_ARENA_ROCK`: `Cali Recto` + `Recto 4x12` | `Master Vol = 64` | `High Cut = 7200Hz` | `NS thd = -50dB`
   - `EPIC_GILMOUR_SOLO`: `Hi-Power` + `UK 4x12 (Green)` | `Master Vol = 65` | `High Cut = 7400Hz` | `NS thd = -52dB`
   - `HIGH_GAIN_METAL`: `EV 5150` + `Recto 4x12` | `Master Vol = 65` | `High Cut = 7200Hz` | `NS thd = -48dB`
   - Travamento de Matriz (`lockToOfficialMatrix`) com margem de micro-ajuste segura (`±6` pontos).

2. **Google Search Grounding na Gemini API (`GeminiService.kt`, `Mk20ToneEngine.kt`, `Mk20ToneAssistantEngine.kt`)**
   - Consulta web ativa (`GoogleSearchTool`) com fallback automático transparente para identificar versão da música, guitarrista, BPM e equipamentos reais antes de travar a matriz.

3. **Guardrail Acústico Calibrado (`AcousticStudioGuardrail.kt`)**
   - Volume Master padrão `64` (faixa `55..80`).
   - Noise Gate transparente (`-58dB` Clean, `-52dB` Crunch/Solo Melódico, `-48dB` High-Gain Metal) sem cortar o sustain das notas.
   - Abertura de brilho (`CAB High Cut` até `8500Hz`, `AMP Treble` até `72`, `Pres` até `66`, `EQ 1.6kHz / 3.2kHz` até `+4dB / +3dB`).

4. **Modo de Cota Diária / PRO (`PresetViewModel.kt` & `PresetDialogs.kt`)**
   - Na `beta-1` pura (produção), o `ProUpgradeDialog` exibe apenas os controles de compra única e restauração da Google Play (`preset20_pro_vitalicio`), sem o painel de simulação/reset.
