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
import com.example.data.repository.PresetRepository
import com.example.util.DeepLinkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PresetRepository
    private val prefs = application.getSharedPreferences("mk20_prefs", Context.MODE_PRIVATE)

    init {
        val db = AppDatabase.getInstance(application)
        repository = PresetRepository(db.presetDao())
        viewModelScope.launch {
            repository.seedFactoryPresetsIfNeeded()
        }
    }

    private val _currentPreset = MutableStateFlow(Mk20Preset.DEFAULT_PRESET)
    val currentPreset: StateFlow<Mk20Preset> = _currentPreset.asStateFlow()

    private val _songInput = MutableStateFlow("")
    val songInput: StateFlow<String> = _songInput.asStateFlow()

    private val _youtubeUrl = MutableStateFlow("")
    val youtubeUrl: StateFlow<String> = _youtubeUrl.asStateFlow()

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

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSongInput(song: String) {
        _songInput.value = song
    }

    fun setYoutubeUrl(url: String) {
        _youtubeUrl.value = url
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

    fun generatePreset() {
        val song = _songInput.value.trim()
        val yt = _youtubeUrl.value.trim()
        val details = _detailsInput.value.trim()

        if (song.isBlank() && yt.isBlank()) {
            _statusMessage.value = "Informe a música ou o link do YouTube!"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            val result = Mk20ToneEngine.generatePreset(
                songOrStyle = song,
                youtubeUrl = yt,
                details = details,
                customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
            )

            result.fold(
                onSuccess = { generated ->
                    _currentPreset.value = generated
                    _statusMessage.value = "Preset '${generated.title}' gerado com sucesso!"
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
        _selectedTab.value = 0
        _statusMessage.value = "Preset '${preset.title}' carregado!"
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
            _statusMessage.value = "Preset '${toSave.title}' salvo nos favoritos!"
            closeSaveDialog()
        }
    }

    fun deleteSavedPreset(entity: PresetEntity) {
        viewModelScope.launch {
            repository.deletePreset(entity)
            _statusMessage.value = "Preset '${entity.title}' removido!"
        }
    }

    fun toggleFavorite(entity: PresetEntity) {
        viewModelScope.launch {
            repository.updateFavorite(entity, !entity.isFavorite)
        }
    }

    fun togglePedalActive(pedalKey: String) {
        val p = _currentPreset.value
        val updated = when (pedalKey) {
            "NR" -> p.copy(pedals = p.pedals.copy(NR = p.pedals.NR?.copy(active = !p.pedals.NR.active)))
            "DS" -> p.copy(pedals = p.pedals.copy(DS = p.pedals.DS?.copy(active = !p.pedals.DS.active)))
            "AMP" -> p.copy(pedals = p.pedals.copy(AMP = p.pedals.AMP?.copy(active = !p.pedals.AMP.active)))
            "CAB" -> p.copy(pedals = p.pedals.copy(CAB = p.pedals.CAB?.copy(active = !p.pedals.CAB.active)))
            "MOD" -> p.copy(pedals = p.pedals.copy(MOD = p.pedals.MOD?.copy(active = !p.pedals.MOD.active)))
            "DELAY" -> p.copy(pedals = p.pedals.copy(DELAY = p.pedals.DELAY?.copy(active = !p.pedals.DELAY.active)))
            "REVERB" -> p.copy(pedals = p.pedals.copy(REVERB = p.pedals.REVERB?.copy(active = !p.pedals.REVERB.active)))
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
            "NR" -> p.copy(pedals = p.pedals.copy(NR = updateModule(p.pedals.NR)))
            "DS" -> p.copy(pedals = p.pedals.copy(DS = updateModule(p.pedals.DS)))
            "AMP" -> p.copy(pedals = p.pedals.copy(AMP = updateModule(p.pedals.AMP)))
            "CAB" -> p.copy(pedals = p.pedals.copy(CAB = updateModule(p.pedals.CAB)))
            "MOD" -> p.copy(pedals = p.pedals.copy(MOD = updateModule(p.pedals.MOD)))
            "DELAY" -> p.copy(pedals = p.pedals.copy(DELAY = updateModule(p.pedals.DELAY)))
            "REVERB" -> p.copy(pedals = p.pedals.copy(REVERB = updateModule(p.pedals.REVERB)))
            else -> p
        }
        _currentPreset.value = updated
    }

    fun updatePedalModel(pedalKey: String, modelName: String) {
        val p = _currentPreset.value
        val updated = when (pedalKey) {
            "NR" -> p.copy(pedals = p.pedals.copy(NR = p.pedals.NR?.copy(model = modelName)))
            "DS" -> p.copy(pedals = p.pedals.copy(DS = p.pedals.DS?.copy(model = modelName)))
            "AMP" -> p.copy(pedals = p.pedals.copy(AMP = p.pedals.AMP?.copy(model = modelName)))
            "CAB" -> p.copy(pedals = p.pedals.copy(CAB = p.pedals.CAB?.copy(model = modelName)))
            "MOD" -> p.copy(pedals = p.pedals.copy(MOD = p.pedals.MOD?.copy(model = modelName)))
            "DELAY" -> p.copy(pedals = p.pedals.copy(DELAY = p.pedals.DELAY?.copy(model = modelName)))
            "REVERB" -> p.copy(pedals = p.pedals.copy(REVERB = p.pedals.REVERB?.copy(model = modelName)))
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
}
