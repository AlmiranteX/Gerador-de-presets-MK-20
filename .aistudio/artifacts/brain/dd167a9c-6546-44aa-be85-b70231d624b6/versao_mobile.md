# Ponto de Restauração Oficial: Versão `mobile`

Data do Snapshot: **05/11/2026**  
Identificador Oficial: **`mobile`** (Versão Mobile Android — Google Auth + Cloud Sync Isolado por UID + Play Billing + 8 Matrizes Oficiais MK-20)

Sempre que você quiser retornar exatamente a esta versão mobile Android, basta pedir:
> *"Voltar para a versão mobile"*

---

## 1. O que compõe a Versão `mobile`

1. **Autenticação Google & Isolamento Total por Conta (`UID`)**
   - Login em 1 toque via Jetpack `CredentialManager` (`GoogleAuthComponents.kt` e `MainActivity.kt`).
   - Status **PRO Vitalício** e contador de cota diária (**FREE 4/4**) isolados por `UID` da Conta Google no armazenamento local (`is_pro_lifetime_<uid>`, `daily_generation_date_<uid>`, `daily_generation_count_<uid>`) e no Cloud Firestore (`/users/{uid}`).
   - Limpeza imediata da sessão em memória ao sair da conta (`onUserSignedOut()`), impedindo que o status PRO de um e-mail contamine outro e-mail no mesmo aparelho.
   - Backup e restauração automática de presets na nuvem (`/users/{uid}/presets/{presetId}`).

2. **Monetização Google Play Billing (`PlayBillingManager.kt`)**
   - Produto único vitalício `preset20_pro_vitalicio` (`R$ 24,90`) sem mensalidade.
   - Limite gratuito de 4 presets diários com renovação automática à meia-noite.

3. **Motor Sonoro M-VAVE MK-20 (`Mk20OfficialToneMatrix.kt` + `AcousticStudioGuardrail.kt`)**
   - 8 Matrizes Oficiais de Hardware (`GOSPEL_MELODIC_SOLO`, `WORSHIP_AMBIENT_CHIME`, `CLEAN_STUDIO_POP_FUNK`, `BRITISH_CRUNCH_ROCK`, `BLUES_TEXAS_OVERDRIVE`, `MODERN_ARENA_ROCK`, `EPIC_GILMOUR_SOLO`, `HIGH_GAIN_METAL`).
   - Busca em tempo real com Google Search Grounding (`GeminiService.kt`) + busca por Link do YouTube + captura acústica via Microfone (`AudioVibeRecorder.kt`) + Roadie IA (`Mk20ToneAssistantEngine.kt`).
   - Exportação binária `.mk20` autêntica de **412 bytes** com clonagem de molde oficial (`authentic_mk20_template.mk20`) compatível com o software **M-EFCS**.
