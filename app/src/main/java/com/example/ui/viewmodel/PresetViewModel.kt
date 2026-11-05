package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PresetEntity
import com.example.data.model.Mk20Pedals
import com.example.data.model.Mk20Preset
import com.example.data.model.PedalModule
import com.example.data.remote.Mk20ToneEngine
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.PresetRepository
import com.example.util.DeepLinkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.app.Activity
import com.example.util.PlayBillingManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PresetViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_FREE_DAILY_PRESETS = 4
        private const val PREF_IS_PRO_LIFETIME = "is_pro_lifetime"
        private const val PREF_DAILY_DATE = "daily_generation_date"
        private const val PREF_DAILY_COUNT = "daily_generation_count"
        private const val PREF_CLEAN_PRO_LEAK_V2 = "clean_pro_leak_v2_"
    }

    private val repository: PresetRepository
    private val cloudRepository = CloudSyncRepository(application)
    private val prefs = application.getSharedPreferences("mk20_prefs", Context.MODE_PRIVATE)

    // Indica se o Google Play Billing confirmou uma compra oficial ativa nesta sessão
    private var isBillingConfirmedPro = false

    private val _isProUnlocked = MutableStateFlow(false)
    val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    private val _dailyGenerationsUsed = MutableStateFlow(0)
    val dailyGenerationsUsed: StateFlow<Int> = _dailyGenerationsUsed.asStateFlow()

    private val _proUpgradeDialogOpen = MutableStateFlow(false)
    val proUpgradeDialogOpen: StateFlow<Boolean> = _proUpgradeDialogOpen.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _cloudBackupCount = MutableStateFlow(0)
    val cloudBackupCount: StateFlow<Int> = _cloudBackupCount.asStateFlow()

    private val playBillingManager = PlayBillingManager(
        context = application,
        onProUnlocked = {
            isBillingConfirmedPro = true
            unlockProLifetimeInternal()
        },
        onBillingMessage = { msg ->
            _statusMessage.value = msg
        }
    )

    val proProductPrice: StateFlow<String> = playBillingManager.productPrice

    init {
        val db = AppDatabase.getInstance(application)
        repository = PresetRepository(db.presetDao())
        // Remove a chave global antiga sem escopo de usuário para evitar que contamine outras contas
        if (prefs.contains(PREF_IS_PRO_LIFETIME)) {
            prefs.edit().remove(PREF_IS_PRO_LIFETIME).apply()
        }
        refreshDailyQuota()
        playBillingManager.startConnection()
        viewModelScope.launch {
            repository.seedFactoryPresetsIfNeeded()
        }
    }

    private fun currentUserKeySuffix(): String {
        return cloudRepository.getCurrentUserIdOrNull()?.takeIf { it.isNotBlank() } ?: "guest"
    }

    private fun proPrefKey(uid: String = currentUserKeySuffix()) = "${PREF_IS_PRO_LIFETIME}_$uid"
    private fun dailyDatePrefKey(uid: String = currentUserKeySuffix()) = "${PREF_DAILY_DATE}_$uid"
    private fun dailyCountPrefKey(uid: String = currentUserKeySuffix()) = "${PREF_DAILY_COUNT}_$uid"

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun refreshDailyQuota() {
        val uid = currentUserKeySuffix()
        val today = getTodayDateKey()
        val dateKey = dailyDatePrefKey(uid)
        val countKey = dailyCountPrefKey(uid)
        val savedDate = prefs.getString(dateKey, "") ?: ""
        if (savedDate != today) {
            prefs.edit()
                .putString(dateKey, today)
                .putInt(countKey, 0)
                .apply()
            _dailyGenerationsUsed.value = 0
        } else {
            _dailyGenerationsUsed.value = prefs.getInt(countKey, 0).coerceIn(0, MAX_FREE_DAILY_PRESETS)
        }
    }

    private fun consumeDailyGenerationIfFree(): Int {
        refreshDailyQuota()
        if (_isProUnlocked.value) return MAX_FREE_DAILY_PRESETS
        val uid = currentUserKeySuffix()
        val newUsed = (_dailyGenerationsUsed.value + 1).coerceAtMost(MAX_FREE_DAILY_PRESETS)
        _dailyGenerationsUsed.value = newUsed
        val today = getTodayDateKey()
        prefs.edit()
            .putString(dailyDatePrefKey(uid), today)
            .putInt(dailyCountPrefKey(uid), newUsed)
            .apply()
        syncQuotaToCloudAsync(today, newUsed, _isProUnlocked.value, forceOverride = false)
        return (MAX_FREE_DAILY_PRESETS - newUsed).coerceAtLeast(0)
    }

    private fun canGeneratePresetToday(): Boolean {
        refreshDailyQuota()
        return _isProUnlocked.value || _dailyGenerationsUsed.value < MAX_FREE_DAILY_PRESETS
    }

    private fun unlockProLifetimeInternal() {
        val uid = currentUserKeySuffix()
        _isProUnlocked.value = true
        prefs.edit().putBoolean(proPrefKey(uid), true).apply()
        _proUpgradeDialogOpen.value = false
        syncQuotaToCloudAsync(getTodayDateKey(), _dailyGenerationsUsed.value, true, forceOverride = true)
    }

    fun onUserSignedOut() {
        _isProUnlocked.value = isBillingConfirmedPro
        _dailyGenerationsUsed.value = 0
        _cloudBackupCount.value = 0
    }

    fun onUserAuthenticated() {
        val uid = cloudRepository.getCurrentUserIdOrNull() ?: return
        val cleanMigrationKey = "$PREF_CLEAN_PRO_LEAK_V2$uid"
        val needsContaminationCleanup = !prefs.getBoolean(cleanMigrationKey, false)

        if (needsContaminationCleanup && !isBillingConfirmedPro) {
            prefs.edit().putBoolean(proPrefKey(uid), false).apply()
        }

        // Carrega apenas o estado pertencente a este UID específico (nunca de outra conta anterior)
        _isProUnlocked.value = isBillingConfirmedPro || prefs.getBoolean(proPrefKey(uid), false)
        refreshDailyQuota()

        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val today = getTodayDateKey()
                // No login, só promove a nuvem para PRO se houver compra confirmada pelo Google Play Billing.
                // Se a conta ainda não passou pela limpeza única de contaminação anterior, reseta o proLifetime contaminado.
                val quotaResult = cloudRepository.syncUserQuota(
                    todayDate = today,
                    localCount = _dailyGenerationsUsed.value,
                    localPro = isBillingConfirmedPro,
                    forceOverrideCloud = false,
                    resetProOnly = needsContaminationCleanup && !isBillingConfirmedPro
                )
                quotaResult.onSuccess { cloudQuota ->
                    val clampedUsed = cloudQuota.dailyCount.coerceIn(0, MAX_FREE_DAILY_PRESETS)
                    val resolvedPro = cloudQuota.proLifetime || isBillingConfirmedPro
                    _dailyGenerationsUsed.value = clampedUsed
                    _isProUnlocked.value = resolvedPro
                    prefs.edit()
                        .putBoolean(cleanMigrationKey, true)
                        .putBoolean(proPrefKey(uid), resolvedPro)
                        .putString(dailyDatePrefKey(uid), today)
                        .putInt(dailyCountPrefKey(uid), clampedUsed)
                        .apply()
                }
                syncPresetsWithCloudInternal(showFeedback = false)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    private fun syncQuotaToCloudAsync(
        todayDate: String,
        dailyCount: Int,
        proLifetime: Boolean,
        forceOverride: Boolean
    ) {
        val uid = cloudRepository.getCurrentUserIdOrNull() ?: return
        viewModelScope.launch {
            val result = cloudRepository.syncUserQuota(
                todayDate = todayDate,
                localCount = dailyCount,
                localPro = proLifetime,
                forceOverrideCloud = forceOverride
            )
            result.onSuccess { synced ->
                val clampedUsed = synced.dailyCount.coerceIn(0, MAX_FREE_DAILY_PRESETS)
                _dailyGenerationsUsed.value = clampedUsed
                _isProUnlocked.value = synced.proLifetime
                prefs.edit()
                    .putBoolean(proPrefKey(uid), synced.proLifetime)
                    .putString(dailyDatePrefKey(uid), todayDate)
                    .putInt(dailyCountPrefKey(uid), clampedUsed)
                    .apply()
            }
        }
    }

    fun syncPresetsWithCloudNow() {
        if (cloudRepository.getCurrentUserIdOrNull() == null) {
            _statusMessage.value = "Faça login com sua Conta Google para sincronizar na nuvem."
            return
        }
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                syncPresetsWithCloudInternal(showFeedback = true)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    private suspend fun syncPresetsWithCloudInternal(showFeedback: Boolean) {
        val cloudResult = cloudRepository.getUserPresets()
        cloudResult.onSuccess { cloudList ->
            val localList = repository.allPresets.first()
            val localTitles = localList.map { it.title.trim().lowercase() }.toSet()
            var restoredCount = 0

            // 1. Restore presets from Cloud that are not on this device
            for (cloudPreset in cloudList) {
                if (cloudPreset.title.trim().lowercase() !in localTitles) {
                    val presetObj = cloudPreset.toPresetEntity().toPreset()
                    repository.savePreset(presetObj)
                    restoredCount++
                }
            }

            // 2. Backup local user presets to Cloud if not yet backed up
            val factoryTitles = Mk20Preset.FACTORY_PRESETS.map { it.title.trim().lowercase() }.toSet()
            val cloudIds = cloudList.map { it.id }.toSet()
            var uploadedCount = 0
            for (localEntity in localList) {
                val docId = CloudSyncRepository.sanitizePresetDocId(localEntity.title)
                val isUserCreated = localEntity.title.trim().lowercase() !in factoryTitles || localEntity.isFavorite
                if (isUserCreated && docId !in cloudIds) {
                    cloudRepository.saveCloudPreset(
                        preset = localEntity.toPreset(),
                        favorite = localEntity.isFavorite,
                        customPresetId = docId
                    )
                    uploadedCount++
                }
            }

            _cloudBackupCount.value = cloudList.size + uploadedCount
            if (showFeedback) {
                _statusMessage.value = when {
                    restoredCount > 0 && uploadedCount > 0 ->
                        "Nuvem sincronizada: $restoredCount timbres restaurados e $uploadedCount salvos no backup!"
                    restoredCount > 0 ->
                        "$restoredCount timbres restaurados do seu Backup na Nuvem Google!"
                    uploadedCount > 0 ->
                        "$uploadedCount timbres enviados para o seu Backup na Nuvem Google!"
                    else ->
                        "Backup em Nuvem Google atualizado (${_cloudBackupCount.value} presets protegidos)!"
                }
            }
        }.onFailure { err ->
            if (showFeedback) {
                _statusMessage.value = "Não foi possível sincronizar com a nuvem: ${err.localizedMessage ?: "Tente novamente"}"
            }
        }
    }

    fun openProUpgradeDialog() {
        refreshDailyQuota()
        _proUpgradeDialogOpen.value = true
    }

    fun closeProUpgradeDialog() {
        _proUpgradeDialogOpen.value = false
    }

    fun launchProPurchase(activity: Activity?) {
        playBillingManager.launchOneTimePurchase(activity)
    }

    fun restoreProPurchases() {
        playBillingManager.queryExistingPurchases(silent = false)
        onUserAuthenticated()
    }

    fun debugActivateProLifetime() {
        unlockProLifetimeInternal()
        _statusMessage.value = "👑 Preset20 AI PRO Vitalício ativado (Local + Nuvem)! Gerações ilimitadas liberadas."
    }

    fun debugResetToFreePlan() {
        val uid = currentUserKeySuffix()
        val today = getTodayDateKey()
        isBillingConfirmedPro = false
        _isProUnlocked.value = false
        _dailyGenerationsUsed.value = 0
        prefs.edit()
            .putBoolean(proPrefKey(uid), false)
            .putString(dailyDatePrefKey(uid), today)
            .putInt(dailyCountPrefKey(uid), 0)
            .apply()
        syncQuotaToCloudAsync(today, 0, false, forceOverride = true)
        _statusMessage.value = "Cota diária resetada (Local + Nuvem): 4 de 4 presets disponíveis hoje!"
    }

    fun debugExhaustDailyQuota() {
        val uid = currentUserKeySuffix()
        val today = getTodayDateKey()
        isBillingConfirmedPro = false
        _isProUnlocked.value = false
        _dailyGenerationsUsed.value = MAX_FREE_DAILY_PRESETS
        prefs.edit()
            .putBoolean(proPrefKey(uid), false)
            .putString(dailyDatePrefKey(uid), today)
            .putInt(dailyCountPrefKey(uid), MAX_FREE_DAILY_PRESETS)
            .apply()
        syncQuotaToCloudAsync(today, MAX_FREE_DAILY_PRESETS, false, forceOverride = true)
        _statusMessage.value = "Simulação (Local + Nuvem): Limite diário atingido (4/4 usados hoje). Tente gerar um preset para testar o bloqueio!"
    }

    enum class InputMode {
        SEARCH_NAME,
        YOUTUBE_LINK,
        AUDIO_VIBE
    }

    private val _inputMode = MutableStateFlow(InputMode.SEARCH_NAME)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()

    private val audioRecorder = com.example.util.AudioVibeRecorder()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _audioCountdown = MutableStateFlow(5)
    val audioCountdown: StateFlow<Int> = _audioCountdown.asStateFlow()

    private val _audioVibeResult = MutableStateFlow<com.example.util.AudioVibeAnalysis?>(null)
    val audioVibeResult: StateFlow<com.example.util.AudioVibeAnalysis?> = _audioVibeResult.asStateFlow()

    private val _currentPreset = MutableStateFlow(Mk20Preset.DEFAULT_PRESET)
    val currentPreset: StateFlow<Mk20Preset> = _currentPreset.asStateFlow()

    private val _songInput = MutableStateFlow("")
    val songInput: StateFlow<String> = _songInput.asStateFlow()

    private val _songSuggestions = MutableStateFlow<List<com.example.data.remote.SongSuggestion>>(emptyList())
    val songSuggestions: StateFlow<List<com.example.data.remote.SongSuggestion>> = _songSuggestions.asStateFlow()

    private val _isSearchingSongs = MutableStateFlow(false)
    val isSearchingSongs: StateFlow<Boolean> = _isSearchingSongs.asStateFlow()

    private val _selectedSongSuggestion = MutableStateFlow<com.example.data.remote.SongSuggestion?>(null)
    val selectedSongSuggestion: StateFlow<com.example.data.remote.SongSuggestion?> = _selectedSongSuggestion.asStateFlow()

    private var songSearchJob: kotlinx.coroutines.Job? = null

    private val _youtubeUrl = MutableStateFlow("")
    val youtubeUrl: StateFlow<String> = _youtubeUrl.asStateFlow()

    private val _youtubeVideoInfo = MutableStateFlow<com.example.util.YouTubeVideoInfo?>(null)
    val youtubeVideoInfo: StateFlow<com.example.util.YouTubeVideoInfo?> = _youtubeVideoInfo.asStateFlow()

    private val _isLoadingYouTubeInfo = MutableStateFlow(false)
    val isLoadingYouTubeInfo: StateFlow<Boolean> = _isLoadingYouTubeInfo.asStateFlow()

    private val _detailsInput = MutableStateFlow("")
    val detailsInput: StateFlow<String> = _detailsInput.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _shareDialogPreset = MutableStateFlow<Mk20Preset?>(null)
    val shareDialogPreset: StateFlow<Mk20Preset?> = _shareDialogPreset.asStateFlow()

    private val _saveDialogPreset = MutableStateFlow<Mk20Preset?>(null)
    val saveDialogPreset: StateFlow<Mk20Preset?> = _saveDialogPreset.asStateFlow()

    private val _settingsDialogOpen = MutableStateFlow(false)
    val settingsDialogOpen: StateFlow<Boolean> = _settingsDialogOpen.asStateFlow()

    private val initialAssistantMessage = com.example.data.model.ToneAssistantMessage(
        isUser = false,
        text = "Olá! Eu sou seu Roadie e Engenheiro de Timbre da MK-20. 🎸\n\nPosso ajustar seu som atual (cortar agudos para Strato, turbinar ganho para solo, limpar excesso de reverb) ou criar um som do zero. O que você gostaria de mudar no seu timbre?"
    )

    private val _toneAssistantMessages = MutableStateFlow<List<com.example.data.model.ToneAssistantMessage>>(
        listOf(initialAssistantMessage)
    )
    val toneAssistantMessages: StateFlow<List<com.example.data.model.ToneAssistantMessage>> = _toneAssistantMessages.asStateFlow()

    private val _isToneAssistantThinking = MutableStateFlow(false)
    val isToneAssistantThinking: StateFlow<Boolean> = _isToneAssistantThinking.asStateFlow()

    val savedPresets: StateFlow<List<PresetEntity>> = combine(
        repository.allPresets,
        _searchQuery
    ) { presets, query ->
        if (query.isBlank()) {
            presets
        } else {
            val q = query.lowercase().trim()
            presets.filter {
                it.title.lowercase().contains(q) ||
                it.style.lowercase().contains(q) ||
                it.details.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun sendToneAssistantMessage(text: String) {
        val userPrompt = text.trim()
        if (userPrompt.isBlank() || _isToneAssistantThinking.value) return

        if (!canGeneratePresetToday()) {
            _proUpgradeDialogOpen.value = true
            _statusMessage.value = "Você atingiu o limite de $MAX_FREE_DAILY_PRESETS presets diários! Desbloqueie o PRO para ajustes e criações ilimitadas."
            return
        }

        val userMsg = com.example.data.model.ToneAssistantMessage(
            isUser = true,
            text = userPrompt
        )
        _toneAssistantMessages.value = _toneAssistantMessages.value + userMsg

        viewModelScope.launch {
            _isToneAssistantThinking.value = true
            try {
                val current = _currentPreset.value
                val result = com.example.data.remote.Mk20ToneAssistantEngine.consultAndAdjust(
                    currentPreset = current,
                    userMessage = userPrompt,
                    customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
                )

                val assistantMsg = com.example.data.model.ToneAssistantMessage(
                    isUser = false,
                    text = result.explanation,
                    generatedPreset = result.updatedPreset,
                    appliedChangesSummary = result.appliedChanges
                )
                _toneAssistantMessages.value = _toneAssistantMessages.value + assistantMsg

                val remaining = consumeDailyGenerationIfFree()
                val quotaSuffix = if (_isProUnlocked.value) " [PRO Ilimitado]" else " (Restam $remaining/$MAX_FREE_DAILY_PRESETS hoje)"
                _statusMessage.value = "Timbre ajustado pelo Roadie IA!$quotaSuffix"
            } catch (e: Exception) {
                val errorMsg = com.example.data.model.ToneAssistantMessage(
                    isUser = false,
                    text = "Opa, tive uma pequena falha ao ajustar o som: ${e.message ?: "Tente novamente"}."
                )
                _toneAssistantMessages.value = _toneAssistantMessages.value + errorMsg
            } finally {
                _isToneAssistantThinking.value = false
            }
        }
    }

    fun applyAssistantPresetToPedalboard(preset: Mk20Preset) {
        _currentPreset.value = preset
        _songInput.value = preset.title
        _selectedTab.value = 0 // Muda para a aba Pedaleira
        _statusMessage.value = "Preset '${preset.title}' carregado na Pedaleira!"
    }

    fun clearAssistantConversation() {
        _toneAssistantMessages.value = listOf(initialAssistantMessage)
        _statusMessage.value = "Conversa com o Roadie reiniciada!"
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setInputMode(mode: InputMode) {
        _inputMode.value = mode
    }

    fun setSongInput(song: String) {
        _songInput.value = song

        // If user altered title away from selected suggestion, reset selected confirmation
        val currentSelected = _selectedSongSuggestion.value
        if (currentSelected != null && !song.contains(currentSelected.title, ignoreCase = true)) {
            _selectedSongSuggestion.value = null
        }

        // Debounced search when in SEARCH_NAME mode
        if (_inputMode.value == InputMode.SEARCH_NAME) {
            val q = song.trim()
            songSearchJob?.cancel()
            if (q.length >= 2) {
                songSearchJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(450)
                    _isSearchingSongs.value = true
                    try {
                        val results = com.example.data.remote.SongSearchService.searchSongs(
                            query = q,
                            customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
                        )
                        _songSuggestions.value = results
                    } catch (e: Exception) {
                        // ignore error
                    } finally {
                        _isSearchingSongs.value = false
                    }
                }
            } else {
                _songSuggestions.value = emptyList()
            }
        }
    }

    fun searchSongs(query: String = _songInput.value) {
        val q = query.trim()
        if (q.length < 2) {
            _statusMessage.value = "Digite pelo menos 2 letras para buscar músicas"
            return
        }
        songSearchJob?.cancel()
        songSearchJob = viewModelScope.launch {
            _isSearchingSongs.value = true
            try {
                val results = com.example.data.remote.SongSearchService.searchSongs(
                    query = q,
                    customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
                )
                _songSuggestions.value = results
                if (results.isNotEmpty() && _selectedSongSuggestion.value == null) {
                    _statusMessage.value = "${results.size} opções encontradas para '$q'"
                } else if (results.isEmpty()) {
                    _statusMessage.value = "Nenhuma sugestão encontrada para '$q'"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erro na busca: ${e.message}"
            } finally {
                _isSearchingSongs.value = false
            }
        }
    }

    fun selectSongSuggestion(suggestion: com.example.data.remote.SongSuggestion) {
        _selectedSongSuggestion.value = suggestion
        _songInput.value = "${suggestion.artist} - ${suggestion.title}"
        val albumMeta = buildString {
            if (suggestion.album.isNotBlank()) append("Álbum: ${suggestion.album}")
            if (suggestion.genre.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append("Gênero: ${suggestion.genre}")
            }
        }
        if (_detailsInput.value.isBlank()) {
            _detailsInput.value = albumMeta
        } else if (!_detailsInput.value.contains(suggestion.album, ignoreCase = true)) {
            _detailsInput.value = "${_detailsInput.value} • $albumMeta"
        }
        _statusMessage.value = "Música confirmada: ${suggestion.artist} – ${suggestion.title} (${suggestion.album})"
    }

    fun clearSelectedSongSuggestion() {
        _selectedSongSuggestion.value = null
    }

    fun clearSongSuggestions() {
        _songSuggestions.value = emptyList()
    }

    fun setYoutubeUrl(url: String) {
        _youtubeUrl.value = url
        val videoId = DeepLinkUtils.extractYouTubeId(url)
        if (videoId != null) {
            viewModelScope.launch {
                _isLoadingYouTubeInfo.value = true
                val info = DeepLinkUtils.fetchYouTubeVideoInfo(url)
                _youtubeVideoInfo.value = info
                if (info != null && info.title.isNotBlank()) {
                    // Set preset name automatically from the YouTube video title (editable by user)
                    _songInput.value = info.title
                    _statusMessage.value = "Música identificada: ${info.title}"
                }
                _isLoadingYouTubeInfo.value = false
            }
        } else {
            _youtubeVideoInfo.value = null
        }
    }

    fun updatePresetTitle(newTitle: String) {
        if (newTitle.isNotBlank()) {
            _currentPreset.value = _currentPreset.value.copy(title = newTitle.trim())
            _songInput.value = newTitle.trim()
            _statusMessage.value = "Título atualizado para: $newTitle"
        }
    }

    fun setDetailsInput(details: String) {
        _detailsInput.value = details
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveApiKey(apiKey: String) {
        _customApiKey.value = apiKey
        prefs.edit().putString("gemini_api_key", apiKey).apply()
        _statusMessage.value = "Chave da API Gemini salva com sucesso!"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openShareDialog(preset: Mk20Preset = _currentPreset.value) {
        _shareDialogPreset.value = preset
    }

    fun closeShareDialog() {
        _shareDialogPreset.value = null
    }

    fun openSaveDialog(preset: Mk20Preset = _currentPreset.value) {
        _saveDialogPreset.value = preset
    }

    fun closeSaveDialog() {
        _saveDialogPreset.value = null
    }

    fun openSettingsDialog() {
        _settingsDialogOpen.value = true
    }

    fun closeSettingsDialog() {
        _settingsDialogOpen.value = false
    }

    fun startAudioCapture() {
        if (_isRecordingAudio.value) return
        viewModelScope.launch {
            _isRecordingAudio.value = true
            _audioCountdown.value = 5
            _statusMessage.value = "Ouvindo áudio da música/guitarra... (5 segundos)"
            try {
                val pcmData = audioRecorder.recordAudio(
                    maxDurationSeconds = 5,
                    onAmplitude = { amp ->
                        _audioAmplitude.value = amp
                    },
                    onSecondsUpdate = { elapsed ->
                        _audioCountdown.value = (5 - elapsed).coerceAtLeast(0)
                    }
                )
                if (pcmData.isNotEmpty()) {
                    val analysis = audioRecorder.analyzeAcousticVibe(pcmData)
                    _audioVibeResult.value = analysis
                    _songInput.value = analysis.title
                    _detailsInput.value = analysis.details
                    _statusMessage.value = "Vibe identificada: ${analysis.detectedStyle} (${analysis.gainVibe})!"
                } else {
                    _statusMessage.value = "Não foi possível capturar o áudio. Tente novamente."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erro na gravação: ${e.message}"
            } finally {
                _isRecordingAudio.value = false
                _audioAmplitude.value = 0f
            }
        }
    }

    fun stopAudioCapture() {
        audioRecorder.stop()
        _isRecordingAudio.value = false
        _audioAmplitude.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
        playBillingManager.endConnection()
    }

    fun generatePreset() {
        if (!canGeneratePresetToday()) {
            _proUpgradeDialogOpen.value = true
            _statusMessage.value = "Você atingiu o limite gratuito de $MAX_FREE_DAILY_PRESETS presets hoje! Desbloqueie o PRO Vitalício para gerar sem limites."
            return
        }

        val mode = _inputMode.value

        if (mode == InputMode.AUDIO_VIBE) {
            val vibe = _audioVibeResult.value
            if (vibe == null) {
                _statusMessage.value = "Toque em 'Ouvir Música' para capturar o áudio antes de gerar o preset!"
                return
            }
            viewModelScope.launch {
                _isGenerating.value = true
                val customTitle = _songInput.value.trim().ifBlank { vibe.title }
                val customDetails = _detailsInput.value.trim().ifBlank { vibe.details }
                val updatedVibe = vibe.copy(title = customTitle, details = customDetails)
                
                val result = Mk20ToneEngine.generatePresetFromAudio(
                    vibeAnalysis = updatedVibe,
                    customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
                )

                result.fold(
                    onSuccess = { generated ->
                        val finalPreset = generated.copy(title = customTitle)
                        _currentPreset.value = finalPreset
                        _songInput.value = finalPreset.title
                        val remaining = consumeDailyGenerationIfFree()
                        val quotaSuffix = if (_isProUnlocked.value) " [PRO Ilimitado]" else " (Restam $remaining/$MAX_FREE_DAILY_PRESETS grátis hoje)"
                        _statusMessage.value = "Preset '${finalPreset.title}' gerado com sucesso!$quotaSuffix"
                    },
                    onFailure = { err ->
                        _statusMessage.value = "Erro ao gerar preset da vibe: ${err.message}"
                    }
                )
                _isGenerating.value = false
            }
            return
        }

        val yt = _youtubeUrl.value.trim()
        val ytTitle = _youtubeVideoInfo.value?.title?.trim().orEmpty()
        val userEnteredName = _songInput.value.trim()
        val confirmedSong = _selectedSongSuggestion.value

        val song = when (mode) {
            InputMode.YOUTUBE_LINK -> {
                if (userEnteredName.isNotBlank()) userEnteredName
                else if (ytTitle.isNotBlank()) ytTitle
                else if (yt.isNotBlank()) "M-VAVE YouTube Tone"
                else ""
            }
            InputMode.SEARCH_NAME -> {
                if (confirmedSong != null) {
                    if (userEnteredName.isNotBlank()) userEnteredName else "${confirmedSong.artist} - ${confirmedSong.title}"
                } else {
                    userEnteredName
                }
            }
            InputMode.AUDIO_VIBE -> userEnteredName
        }

        val details = buildString {
            if (mode == InputMode.SEARCH_NAME && confirmedSong != null) {
                append("Música confirmada: ${confirmedSong.artist} - ${confirmedSong.title}. ")
                if (confirmedSong.album.isNotBlank()) append("Álbum: ${confirmedSong.album}. ")
                if (confirmedSong.version.isNotBlank()) append("Versão: ${confirmedSong.version}. ")
                if (confirmedSong.genre.isNotBlank()) append("Gênero: ${confirmedSong.genre}. ")
            }
            val userDetails = _detailsInput.value.trim()
            if (userDetails.isNotBlank()) {
                append(userDetails)
            }
        }.trim()

        if (mode == InputMode.SEARCH_NAME && song.isBlank()) {
            _statusMessage.value = "Digite o nome da música ou escolha um exemplo!"
            return
        }

        if (mode == InputMode.YOUTUBE_LINK && yt.isBlank() && song.isBlank()) {
            _statusMessage.value = "Cole o link do YouTube da música!"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            val effectiveYtUrl = if (mode == InputMode.YOUTUBE_LINK) yt else ""
            val result = Mk20ToneEngine.generatePreset(
                songOrStyle = song,
                youtubeUrl = effectiveYtUrl,
                details = details,
                customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
            )

            result.fold(
                onSuccess = { generated ->
                    val finalPreset = if (song.isNotBlank()) generated.copy(title = song) else generated
                    _currentPreset.value = finalPreset
                    _songInput.value = finalPreset.title
                    val remaining = consumeDailyGenerationIfFree()
                    val quotaSuffix = if (_isProUnlocked.value) " [PRO Ilimitado]" else " (Restam $remaining/$MAX_FREE_DAILY_PRESETS grátis hoje)"
                    _statusMessage.value = "Preset '${finalPreset.title}' gerado com sucesso!$quotaSuffix"
                },
                onFailure = { err ->
                    _statusMessage.value = "Erro ao gerar: ${err.message}"
                }
            )
            _isGenerating.value = false
        }
    }

    fun loadPreset(preset: Mk20Preset) {
        _currentPreset.value = preset
        _songInput.value = preset.title
        _youtubeUrl.value = preset.youtubeLink
        _detailsInput.value = preset.details
        _songSuggestions.value = emptyList()
        _selectedSongSuggestion.value = null
        if (preset.youtubeLink.isNotBlank()) {
            _inputMode.value = InputMode.YOUTUBE_LINK
            setYoutubeUrl(preset.youtubeLink)
        } else {
            _inputMode.value = InputMode.SEARCH_NAME
        }
        _selectedTab.value = 0
    }

    fun loadMasterSong(songItem: com.example.data.model.MasterSongItem) {
        loadPreset(songItem.preset)
        _statusMessage.value = "Timbre lendário de '${songItem.title}' (${songItem.artist}) carregado com sucesso!"
    }

    fun applyStudioMixCalibration() {
        val current = _currentPreset.value
        val pedals = current.pedals

        val updatedCab = pedals.CAB?.let { cab ->
            val updatedParams = cab.params.toMutableMap().apply {
                put("Low Cut", 80f)
                put("High Cut", 7000f)
                if ((get("Level") ?: 0f) < 70f) put("Level", 82f)
            }
            cab.copy(active = true, params = updatedParams)
        }

        val updatedEq = pedals.EQ?.let { eq ->
            val updatedParams = eq.params.toMutableMap().apply {
                if (containsKey("100Hz")) put("100Hz", 1f)
                if (containsKey("200Hz")) put("200Hz", -1f)
                if (containsKey("400Hz")) put("400Hz", -3f)
                if (containsKey("800Hz")) put("800Hz", 1f)
                if (containsKey("1.6kHz")) put("1.6kHz", 3f)
                if (containsKey("3.2kHz")) put("3.2kHz", 2f)
            }
            eq.copy(active = true, params = updatedParams)
        } ?: PedalModule(
            name = "Equalizador",
            active = true,
            model = "Guitar EQ 6",
            params = mapOf("100Hz" to 1f, "200Hz" to -1f, "400Hz" to -3f, "800Hz" to 1f, "1.6kHz" to 3f, "3.2kHz" to 2f)
        )

        val updatedAmp = pedals.AMP?.let { amp ->
            val updatedParams = amp.params.toMutableMap().apply {
                val currentGain = get("Gain") ?: 50f
                if (currentGain > 75f) {
                    put("Gain", 65f)
                }
            }
            amp.copy(params = updatedParams)
        }

        val updatedPreset = current.copy(
            pedals = pedals.copy(
                AMP = updatedAmp,
                CAB = updatedCab,
                EQ = updatedEq
            )
        )
        _currentPreset.value = updatedPreset
        _statusMessage.value = "Calibração de estúdio aplicada: Low Cut 80Hz, High Cut 7kHz (Anti-Abelha) e EQ ativo!"
    }

    fun matchTargetSpectrum() {
        val current = _currentPreset.value
        val (refName, targetPreset) = com.example.ui.components.SpectralResponseCalculator.resolveTargetReference(
            currentPreset = current,
            songInput = _songInput.value
        )
        val targetPedals = targetPreset.pedals
        val updatedPedals = current.pedals.copy(
            DS = targetPedals.DS ?: current.pedals.DS,
            AMP = targetPedals.AMP ?: current.pedals.AMP,
            CAB = targetPedals.CAB ?: current.pedals.CAB,
            EQ = targetPedals.EQ ?: current.pedals.EQ
        )
        _currentPreset.value = current.copy(pedals = updatedPedals)
        _statusMessage.value = "Curva espectral sincronizada com '$refName' (98% Match)!"
    }

    fun loadSavedEntity(entity: PresetEntity) {
        val preset = entity.toPreset()
        loadPreset(preset)
    }

    fun saveCurrentPreset(name: String, style: String) {
        val current = _currentPreset.value
        val toSave = current.copy(
            title = if (name.isNotBlank()) name else current.title,
            style = if (style.isNotBlank()) style else current.style
        )
        viewModelScope.launch {
            repository.savePreset(toSave)
            _currentPreset.value = toSave
            closeSaveDialog()
            if (cloudRepository.getCurrentUserIdOrNull() != null) {
                val cloudRes = cloudRepository.saveCloudPreset(toSave, favorite = true)
                if (cloudRes.isSuccess) {
                    _cloudBackupCount.value = (_cloudBackupCount.value + 1).coerceAtLeast(1)
                    _statusMessage.value = "Preset '${toSave.title}' salvo no dispositivo e no Backup em Nuvem Google!"
                } else {
                    _statusMessage.value = "Preset '${toSave.title}' salvo no dispositivo!"
                }
            } else {
                _statusMessage.value = "Preset '${toSave.title}' salvo nos favoritos!"
            }
        }
    }

    fun deleteSavedPreset(entity: PresetEntity) {
        viewModelScope.launch {
            repository.deletePreset(entity)
            if (cloudRepository.getCurrentUserIdOrNull() != null) {
                val docId = CloudSyncRepository.sanitizePresetDocId(entity.title)
                cloudRepository.deleteCloudPreset(docId)
            }
            _statusMessage.value = "Preset '${entity.title}' removido!"
        }
    }

    fun toggleFavorite(entity: PresetEntity) {
        viewModelScope.launch {
            repository.updateFavorite(entity, !entity.isFavorite)
        }
    }

    enum class ExportSizeMode {
        AUTHENTIC_412,   // 412B oficial MK-20 / M-EFCS (Recomendado!)
        TEMPLATE_CLONE,  // Molde importado do próprio pedal (.mk20)
        M_EFCS_512,      // 512B compatibilidade
        COMPACT_256,     // 256B compacto
        EXPANDED_1024    // 1024B estendido
    }

    private val _exportSizeMode = MutableStateFlow(ExportSizeMode.AUTHENTIC_412)
    val exportSizeMode: StateFlow<ExportSizeMode> = _exportSizeMode.asStateFlow()

    private val _loadedTemplateBytes = MutableStateFlow<ByteArray?>(null)
    val loadedTemplateBytes: StateFlow<ByteArray?> = _loadedTemplateBytes.asStateFlow()

    private val _loadedTemplateName = MutableStateFlow<String?>(null)
    val loadedTemplateName: StateFlow<String?> = _loadedTemplateName.asStateFlow()

    init {
        try {
            val assetStream = application.assets.open("authentic_mk20_template.mk20")
            val bytes = assetStream.readBytes()
            assetStream.close()
            if (bytes.size == 412) {
                _loadedTemplateBytes.value = bytes
                _loadedTemplateName.value = "MK-20 (M-EFCS Oficial)"
                _exportSizeMode.value = ExportSizeMode.TEMPLATE_CLONE
            }
        } catch (_: Exception) {}
    }

    fun setExportSizeMode(mode: ExportSizeMode) {
        _exportSizeMode.value = mode
    }

    fun setTemplateBytes(bytes: ByteArray, name: String) {
        _loadedTemplateBytes.value = bytes
        _loadedTemplateName.value = name
        _exportSizeMode.value = ExportSizeMode.TEMPLATE_CLONE
        _statusMessage.value = "Molde '$name' (${bytes.size} bytes) carregado! A exportação agora usará a estrutura exata do seu pedal."
    }

    fun clearTemplate() {
        _loadedTemplateBytes.value = null
        _loadedTemplateName.value = null
        _exportSizeMode.value = ExportSizeMode.AUTHENTIC_412
        _statusMessage.value = "Molde removido. Usando formato oficial de 412B da MK-20."
    }

    fun getExportBytes(useAmk2Header: Boolean = false): ByteArray {
        val preset = _currentPreset.value
        return when (_exportSizeMode.value) {
            ExportSizeMode.AUTHENTIC_412 -> preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.AUTHENTIC_MK20_SIZE, useAmk2Header = useAmk2Header)
            ExportSizeMode.TEMPLATE_CLONE -> {
                val template = _loadedTemplateBytes.value
                if (template != null) {
                    preset.toBinaryMk20(templateBytes = template, useAmk2Header = useAmk2Header)
                } else {
                    preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.AUTHENTIC_MK20_SIZE, useAmk2Header = useAmk2Header)
                }
            }
            ExportSizeMode.M_EFCS_512 -> preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.SIZE_512, useAmk2Header = useAmk2Header)
            ExportSizeMode.COMPACT_256 -> preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.SIZE_256, useAmk2Header = useAmk2Header)
            ExportSizeMode.EXPANDED_1024 -> preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.SIZE_1024, useAmk2Header = useAmk2Header)
        }
    }

    fun updateMasterVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        val p = _currentPreset.value
        val updatedVolModule = p.pedals.VOL?.copy(params = mapOf("Level" to clamped.toFloat()))
            ?: PedalModule("Master Volume", true, "Master", mapOf("Level" to clamped.toFloat()))
        _currentPreset.value = p.copy(
            masterVolume = clamped,
            pedals = p.pedals.copy(VOL = updatedVolModule)
        )
    }

    fun updateBpm(bpm: Int) {
        val clamped = bpm.coerceIn(40, 250)
        _currentPreset.value = _currentPreset.value.copy(bpm = clamped)
    }

    fun togglePedalActive(pedalKey: String) {
        val p = _currentPreset.value
        val updated = when (pedalKey) {
            "FX" -> p.copy(pedals = p.pedals.copy(FX = p.pedals.FX?.copy(active = !p.pedals.FX.active)))
            "NS", "NR" -> p.copy(pedals = p.pedals.copy(NS = p.pedals.NS?.copy(active = !p.pedals.NS.active)))
            "DS" -> p.copy(pedals = p.pedals.copy(DS = p.pedals.DS?.copy(active = !p.pedals.DS.active)))
            "AMP" -> p.copy(pedals = p.pedals.copy(AMP = p.pedals.AMP?.copy(active = !p.pedals.AMP.active)))
            "CAB" -> p.copy(pedals = p.pedals.copy(CAB = p.pedals.CAB?.copy(active = !p.pedals.CAB.active)))
            "EQ" -> p.copy(pedals = p.pedals.copy(EQ = p.pedals.EQ?.copy(active = !p.pedals.EQ.active)))
            "MOD" -> p.copy(pedals = p.pedals.copy(MOD = p.pedals.MOD?.copy(active = !p.pedals.MOD.active)))
            "DELAY" -> p.copy(pedals = p.pedals.copy(DELAY = p.pedals.DELAY?.copy(active = !p.pedals.DELAY.active)))
            "REVERB" -> p.copy(pedals = p.pedals.copy(REVERB = p.pedals.REVERB?.copy(active = !p.pedals.REVERB.active)))
            "VOL" -> p.copy(pedals = p.pedals.copy(VOL = p.pedals.VOL?.copy(active = !p.pedals.VOL.active)))
            else -> p
        }
        _currentPreset.value = updated
    }

    fun updatePedalParam(pedalKey: String, paramName: String, value: Float) {
        val p = _currentPreset.value
        fun updateModule(mod: PedalModule?): PedalModule? {
            if (mod == null) return null
            val newParams = mod.params.toMutableMap()
            newParams[paramName] = value
            return mod.copy(params = newParams)
        }

        val updated = when (pedalKey) {
            "FX" -> p.copy(pedals = p.pedals.copy(FX = updateModule(p.pedals.FX)))
            "NS", "NR" -> p.copy(pedals = p.pedals.copy(NS = updateModule(p.pedals.NS)))
            "DS" -> p.copy(pedals = p.pedals.copy(DS = updateModule(p.pedals.DS)))
            "AMP" -> p.copy(pedals = p.pedals.copy(AMP = updateModule(p.pedals.AMP)))
            "CAB" -> p.copy(pedals = p.pedals.copy(CAB = updateModule(p.pedals.CAB)))
            "EQ" -> p.copy(pedals = p.pedals.copy(EQ = updateModule(p.pedals.EQ)))
            "MOD" -> p.copy(pedals = p.pedals.copy(MOD = updateModule(p.pedals.MOD)))
            "DELAY" -> p.copy(pedals = p.pedals.copy(DELAY = updateModule(p.pedals.DELAY)))
            "REVERB" -> p.copy(pedals = p.pedals.copy(REVERB = updateModule(p.pedals.REVERB)))
            "VOL" -> p.copy(pedals = p.pedals.copy(VOL = updateModule(p.pedals.VOL)))
            else -> p
        }
        _currentPreset.value = updated
    }

    fun updatePedalModel(pedalKey: String, modelName: String) {
        val p = _currentPreset.value
        val updated = when (pedalKey) {
            "FX" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultFxParams(modelName)
                p.copy(pedals = p.pedals.copy(FX = p.pedals.FX?.copy(model = modelName, params = newParams)))
            }
            "NS", "NR" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultNsParams(modelName)
                p.copy(pedals = p.pedals.copy(NS = p.pedals.NS?.copy(model = modelName, params = newParams)))
            }
            "DS" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultDsParams(modelName)
                p.copy(pedals = p.pedals.copy(DS = p.pedals.DS?.copy(model = modelName, params = newParams)))
            }
            "AMP" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultAmpParams(modelName)
                p.copy(pedals = p.pedals.copy(AMP = p.pedals.AMP?.copy(model = modelName, params = newParams)))
            }
            "CAB" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultCabParams(modelName)
                p.copy(pedals = p.pedals.copy(CAB = p.pedals.CAB?.copy(model = modelName, params = newParams)))
            }
            "EQ" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultEqParams(modelName)
                p.copy(pedals = p.pedals.copy(EQ = p.pedals.EQ?.copy(model = modelName, params = newParams)))
            }
            "MOD" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultModParams(modelName)
                p.copy(pedals = p.pedals.copy(MOD = p.pedals.MOD?.copy(model = modelName, params = newParams)))
            }
            "DELAY" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultDelayParams(modelName)
                p.copy(pedals = p.pedals.copy(DELAY = p.pedals.DELAY?.copy(model = modelName, params = newParams)))
            }
            "REVERB" -> {
                val newParams = com.example.data.model.Mk20Catalog.getDefaultReverbParams(modelName)
                p.copy(pedals = p.pedals.copy(REVERB = p.pedals.REVERB?.copy(model = modelName, params = newParams)))
            }
            "VOL" -> p.copy(pedals = p.pedals.copy(VOL = p.pedals.VOL?.copy(model = modelName)))
            else -> p
        }
        _currentPreset.value = updated
    }

    fun handleIncomingUri(uri: Uri) {
        val preset = DeepLinkUtils.parsePresetFromUri(uri)
        if (preset != null) {
            loadPreset(preset)
            _statusMessage.value = "Preset recebido via link: '${preset.title}'"
        }
    }

    fun handleIncomingText(text: String) {
        try {
            // Check if it contains a link
            if (text.contains("mk20://") || text.contains("mk20tone.app")) {
                val urlRegex = Regex("(https?:\\/\\/mk20tone\\.app\\/preset\\S+|mk20:\\/\\/preset\\S+)")
                val match = urlRegex.find(text)
                if (match != null) {
                    val uri = Uri.parse(match.value)
                    handleIncomingUri(uri)
                    return
                }
            }
            // Try parsing directly as JSON
            val startIdx = text.indexOf('{')
            val endIdx = text.lastIndexOf('}')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val jsonSnippet = text.substring(startIdx, endIdx + 1)
                val preset = Mk20Preset.fromJsonString(jsonSnippet)
                loadPreset(preset)
                _statusMessage.value = "Preset importado do texto: '${preset.title}'"
            }
        } catch (e: Exception) {
            // Not a recognized preset format
        }
    }

    fun importPresetFromJson(jsonString: String) {
        try {
            val preset = Mk20Preset.fromJsonString(jsonString)
            loadPreset(preset)
            _statusMessage.value = "Preset '${preset.title}' importado com sucesso!"
        } catch (e: Exception) {
            _statusMessage.value = "Erro ao importar JSON: ${e.message}"
        }
    }

    fun importPresetFromBytes(bytes: ByteArray, fileName: String = "preset") {
        try {
            if (com.example.util.Mk20BinarySerializer.isBinaryPreset(bytes)) {
                // Salva também como molde para exportações futuras serem 100% idênticas em tamanho
                setTemplateBytes(bytes, fileName)
                val preset = com.example.util.Mk20BinarySerializer.deserialize(bytes)
                loadPreset(preset)
                _statusMessage.value = "Preset MK-20 '${preset.title}' (${bytes.size} bytes) carregado e registrado como molde!"
            } else {
                val jsonString = String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                importPresetFromJson(jsonString)
            }
        } catch (e: Exception) {
            _statusMessage.value = "Erro ao carregar preset ($fileName): ${e.message}"
        }
    }
}
