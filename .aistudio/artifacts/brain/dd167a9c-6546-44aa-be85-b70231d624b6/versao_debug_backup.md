# Registro da Versão: (Debug) — Painel de Teste do Desenvolvedor

Este arquivo registra o código exato da versão **(Debug)** do sistema de limite diário (4 presets/dia) + desbloqueio vitalício PRO, caso você precise reativar os botões de simulação (`Ativar PRO`, `Zerar (0/4)` e `Reset (4/4)`) no futuro.

---

## 1. `PresetViewModel.kt` — Funções de Simulação (Debug)
```kotlin
fun debugActivateProLifetime() {
    unlockProLifetimeInternal()
    _statusMessage.value = "👑 Preset20 AI PRO Vitalício ativado! Gerações ilimitadas liberadas."
}

fun debugResetToFreePlan() {
    _isProUnlocked.value = false
    _dailyGenerationsUsed.value = 0
    prefs.edit()
        .putBoolean(PREF_IS_PRO_LIFETIME, false)
        .putString(PREF_DAILY_DATE, getTodayDateKey())
        .putInt(PREF_DAILY_COUNT, 0)
        .apply()
    _statusMessage.value = "Plano Gratuito restaurado: 4 de 4 presets disponíveis hoje."
}

fun debugExhaustDailyQuota() {
    _isProUnlocked.value = false
    _dailyGenerationsUsed.value = MAX_FREE_DAILY_PRESETS
    prefs.edit()
        .putBoolean(PREF_IS_PRO_LIFETIME, false)
        .putString(PREF_DAILY_DATE, getTodayDateKey())
        .putInt(PREF_DAILY_COUNT, MAX_FREE_DAILY_PRESETS)
        .apply()
    _statusMessage.value = "Simulação: Limite diário atingido (4/4 usados hoje). Tente gerar um preset para testar o bloqueio!"
}
```

---

## 2. `PresetDialogs.kt` — Bloco UI do Painel de Teste no `ProUpgradeDialog`
```kotlin
// Developer Sandbox Controls (Only in Debug builds so developer can test Free/Pro states)
if (com.example.BuildConfig.DEBUG) {
    Surface(
        color = Color(0xFF141B2D),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = LedEmerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PAINEL DE TESTE DO DESENVOLVEDOR",
                    color = LedEmerald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "ID do Produto no Play Console: preset20_pro_vitalicio",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onDebugActivatePro,
                    border = BorderStroke(1.dp, LedAmber),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_debug_unlock_pro")
                ) {
                    Text("Ativar PRO", color = LedAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDebugExhaustQuota,
                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_debug_exhaust_quota")
                ) {
                    Text("Zerar (0/4)", color = Color(0xFFFF8A80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDebugResetFree,
                    border = BorderStroke(1.dp, LedEmerald),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_debug_reset_free")
                ) {
                    Text("Reset (4/4)", color = LedEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
```
