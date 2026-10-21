package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.PresetEntity
import com.example.data.model.Mk20Preset
import com.example.ui.components.JsonViewer
import com.example.ui.components.PedalStompCard
import com.example.ui.components.SavePresetDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SharePresetDialog
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.MetalBorderHighlight
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PresetViewModel
import com.example.util.DeepLinkUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PresetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentPreset by viewModel.currentPreset.collectAsStateWithLifecycle()
    val songInput by viewModel.songInput.collectAsStateWithLifecycle()
    val youtubeUrl by viewModel.youtubeUrl.collectAsStateWithLifecycle()
    val detailsInput by viewModel.detailsInput.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val savedPresets by viewModel.savedPresets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val shareDialogPreset by viewModel.shareDialogPreset.collectAsStateWithLifecycle()
    val saveDialogPreset by viewModel.saveDialogPreset.collectAsStateWithLifecycle()
    val settingsDialogOpen by viewModel.settingsDialogOpen.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()

    // File exporters and importers via SAF (Storage Access Framework)
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(currentPreset.toJsonString(indent = 2).toByteArray())
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Arquivo '${currentPreset.title}.json' salvo com sucesso!")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao salvar arquivo: ${e.message}")
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val jsonStr = stream.bufferedReader().readText()
                    viewModel.importPresetFromJson(jsonStr)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao abrir arquivo: ${e.message}")
                }
            }
        }
    }

    // React to status messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val youtubeId = remember(youtubeUrl) {
        DeepLinkUtils.extractYouTubeId(youtubeUrl)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF059669), Color(0xFF047857))
                                    )
                                )
                                .border(1.dp, LedEmerald.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "MK-20 Tone Generator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(LedEmerald)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Powered by Gemini AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = LedEmerald,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openSaveDialog() },
                        modifier = Modifier.testTag("action_save_preset")
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Salvar Preset", tint = LedEmerald)
                    }
                    IconButton(
                        onClick = { viewModel.openShareDialog() },
                        modifier = Modifier.testTag("action_share_preset")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = { viewModel.openSettingsDialog() },
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF101522),
                contentColor = LedEmerald,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = LedEmerald,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pedaleira", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_pedalboard")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JSON MK-20", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_json")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvos (${savedPresets.size})", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_saved")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: GENERATOR INPUTS + VIRTUAL PEDALBOARD
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Configuration Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = PedalCardDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CONFIGURAR PEDALEIRA COM IA",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    // Quick suggestions chips
                                    Text(
                                        text = "Exemplos rápidos:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "Worship Ambient",
                                            "Sweet Child O' Mine",
                                            "Comfortably Numb",
                                            "Texas Blues",
                                            "Modern Djent Metal",
                                            "Funk Crystal Clean"
                                        ).forEach { styleName ->
                                            FilterChip(
                                                selected = songInput.equals(styleName, ignoreCase = true),
                                                onClick = {
                                                    viewModel.setSongInput(styleName)
                                                },
                                                label = { Text(styleName, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LedEmerald.copy(alpha = 0.2f),
                                                    selectedLabelColor = LedEmerald
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Song / Musical Style Field
                                    OutlinedTextField(
                                        value = songInput,
                                        onValueChange = { viewModel.setSongInput(it) },
                                        label = { Text("Música ou Estilo Musical") },
                                        placeholder = { Text("Ex: Há Jesus, Sweet Child O' Mine, Worship...") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LedEmerald,
                                            unfocusedBorderColor = MetalBorder,
                                            focusedLabelColor = LedEmerald
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("song_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // YouTube Link Field
                                    OutlinedTextField(
                                        value = youtubeUrl,
                                        onValueChange = { viewModel.setYoutubeUrl(it) },
                                        label = { Text("Link do YouTube da Música (Opcional)") },
                                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                                        leadingIcon = {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFFF0000))
                                        },
                                        trailingIcon = {
                                            if (youtubeUrl.isNotBlank()) {
                                                IconButton(onClick = { viewModel.setYoutubeUrl("") }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = TextMuted)
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LedEmerald,
                                            unfocusedBorderColor = MetalBorder,
                                            focusedLabelColor = LedEmerald
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("youtube_url_input")
                                    )

                                    // YouTube Live Thumbnail Preview
                                    AnimatedVisibility(visible = youtubeId != null) {
                                        youtubeId?.let { id ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Surface(
                                                color = Color(0xFF0D121C),
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data("https://img.youtube.com/vi/$id/hqdefault.jpg")
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Thumbnail do YouTube",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(width = 80.dp, height = 48.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = "Vídeo do YouTube Identificado",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = LedEmerald
                                                        )
                                                        Text(
                                                            text = "Pronto para análise de timbre por IA Gemini",
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Tone details input
                                    OutlinedTextField(
                                        value = detailsInput,
                                        onValueChange = { viewModel.setDetailsInput(it) },
                                        label = { Text("Detalhes e Preferências do Timbre") },
                                        placeholder = { Text("Ex: Guitarra Stratocaster Single Coil, som limpo cristalino com delay colcheia pontuada e chorus suave.") },
                                        minLines = 2,
                                        maxLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LedEmerald,
                                            unfocusedBorderColor = MetalBorder,
                                            focusedLabelColor = LedEmerald
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("details_input")
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Generate Button
                                    Button(
                                        onClick = { viewModel.generatePreset() },
                                        enabled = !isGenerating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LedEmerald,
                                            disabledContainerColor = Color(0xFF1A3D31)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("generate_preset_button")
                                    ) {
                                        if (isGenerating) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                "Analisando com Gemini IA...",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Gerar Preset para MK-20",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Preset Title & Quick Action Bar
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentPreset.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${currentPreset.style} • ${currentPreset.bpm} BPM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LedEmerald,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.openSaveDialog() },
                                        modifier = Modifier.testTag("quick_save_button")
                                    ) {
                                        Icon(Icons.Default.Bookmark, contentDescription = "Salvar", tint = LedEmerald)
                                    }
                                    IconButton(
                                        onClick = { viewModel.openShareDialog() },
                                        modifier = Modifier.testTag("quick_share_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = TextPrimary)
                                    }
                                }
                            }
                        }

                        // Pedals Chain Header
                        item {
                            Text(
                                text = "CADEIA DE SINAL M-VAVE MK-20",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        // Virtual Stompboxes
                        currentPreset.pedals.NR?.let { nrModule ->
                            item {
                                PedalStompCard(
                                    pedalKey = "NR",
                                    module = nrModule,
                                    onToggleActive = { viewModel.togglePedalActive("NR") },
                                    onParamChange = { p, v -> viewModel.updatePedalParam("NR", p, v) },
                                    onModelChange = { m -> viewModel.updatePedalModel("NR", m) }
                                )
                            }
                        }

                        currentPreset.pedals.DS?.let { dsModule ->
                            item {
                                PedalStompCard(
                                    pedalKey = "DS",
                                    module = dsModule,
                                    onToggleActive = { viewModel.togglePedalActive("DS") },
                                    onParamChange = { p, v -> viewModel.updatePedalParam("DS", p, v) },
                                    onModelChange = { m -> viewModel.updatePedalModel("DS", m) }
                                )
                            }
                        }

                        currentPreset.pedals.AMP?.let { ampModule ->
                            item {
                                PedalStompCard(
                                    pedalKey = "AMP",
                                    module = ampModule,
                                    onToggleActive = { viewModel.togglePedalActive("AMP") },
                                    onParamChange = { p, v -> viewModel.updatePedalParam("AMP", p, v) },
                                    onModelChange = { m -> viewModel.updatePedalModel("AMP", m) }
                                )
                            }
                        }

                        currentPreset.pedals.CAB?.let { cabModule ->
                            item {
                                PedalStompCard(
                                    pedalKey = "CAB",
                                    module = cabModule,
                                    onToggleActive = { viewModel.togglePedalActive("CAB") },
                                    onParamChange = { p, v -> viewModel.updatePedalParam("CAB", p, v) },
                                    onModelChange = { m -> viewModel.updatePedalModel("CAB", m) }
                                )
                            }
                        }

                        currentPreset.pedals.MOD?.let { modModule ->
                            item {
                                PedalStompCard(
                                    pedalKey = "MOD",
                                    module = modModule,
                                    onToggleActive = { viewModel.togglePedalActive("MOD") },
                                    onParamChange = { p, v -> viewModel.updatePedalParam("MOD", p, v) },
                                    onModelChange = { m -> viewModel.updatePedalModel("MOD", m) }
                                )
                            }
                        }

                        currentPreset.pedals.DELAY?.let { delayModule ->
                            item {
                                PedalStompCard(
                                    pedalKey = "DELAY",
                                    module = delayModule,
                                    onToggleActive = { viewModel.togglePedalActive("DELAY") },
                                    onParamChange = { p, v -> viewModel.updatePedalParam("DELAY", p, v) },
                                    onModelChange = { m -> viewModel.updatePedalModel("DELAY", m) }
                                )
                            }
                        }

                        currentPreset.pedals.REVERB?.let { reverbModule ->
                            item {
                                PedalStompCard(
                                    pedalKey = "REVERB",
                                    module = reverbModule,
                                    onToggleActive = { viewModel.togglePedalActive("REVERB") },
                                    onParamChange = { p, v -> viewModel.updatePedalParam("REVERB", p, v) },
                                    onModelChange = { m -> viewModel.updatePedalModel("REVERB", m) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: JSON VIEWER & EXPORT
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            JsonViewer(
                                preset = currentPreset,
                                onCopyJson = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("MK-20 Preset JSON", currentPreset.toJsonString(indent = 2)))
                                    scope.launch { snackbarHostState.showSnackbar("JSON copiado para a área de transferência!") }
                                },
                                onShareLink = { viewModel.openShareDialog() },
                                onExportJson = {
                                    val sanitized = currentPreset.title.lowercase().replace("\\s+".toRegex(), "_")
                                    exportJsonLauncher.launch("${sanitized}_mk20.json")
                                },
                                onImportJson = {
                                    importJsonLauncher.launch(arrayOf("application/json", "text/plain"))
                                }
                            )
                        }
                    }
                }

                2 -> {
                    // TAB 2: SAVED PRESETS LIBRARY (ROOM PERSISTENCE)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Field
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Pesquisar presets salvos por nome ou estilo...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = TextMuted)
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LedEmerald,
                                    unfocusedBorderColor = MetalBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_presets_input")
                            )
                        }

                        if (savedPresets.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (searchQuery.isBlank()) "Nenhum preset salvo ainda" else "Nenhum preset encontrado para a busca",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Gere um preset com a IA e clique em 'Salvar'!",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(savedPresets, key = { it.id }) { presetEntity ->
                                SavedPresetCard(
                                    entity = presetEntity,
                                    onLoad = { viewModel.loadSavedEntity(presetEntity) },
                                    onShare = { viewModel.openShareDialog(presetEntity.toPreset()) },
                                    onDelete = { viewModel.deleteSavedPreset(presetEntity) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    saveDialogPreset?.let { presetToSave ->
        SavePresetDialog(
            preset = presetToSave,
            onDismiss = { viewModel.closeSaveDialog() },
            onSave = { name, style -> viewModel.saveCurrentPreset(name, style) }
        )
    }

    shareDialogPreset?.let { presetToShare ->
        SharePresetDialog(
            preset = presetToShare,
            onDismiss = { viewModel.closeShareDialog() },
            onCopyLink = { link ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("MK-20 Preset Link", link))
                scope.launch { snackbarHostState.showSnackbar("Link copiado para a área de transferência!") }
            },
            onShareApp = {
                DeepLinkUtils.sharePreset(context, presetToShare)
            },
            onExportJsonFile = {
                val sanitized = presetToShare.title.lowercase().replace("\\s+".toRegex(), "_")
                exportJsonLauncher.launch("${sanitized}_mk20.json")
            }
        )
    }

    if (settingsDialogOpen) {
        SettingsDialog(
            currentApiKey = customApiKey,
            onSaveKey = { key -> viewModel.saveApiKey(key) },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }
}

@Composable
fun SavedPresetCard(
    entity: PresetEntity,
    onLoad: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("saved_preset_item_${entity.id}")
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MetalBorder, RoundedCornerShape(14.dp))
            .clickable { onLoad() },
        color = PedalCardDark,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entity.style.ifBlank { "Personalizado" },
                        style = MaterialTheme.typography.labelSmall,
                        color = LedEmerald,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = " • ${entity.bpm} BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                if (entity.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entity.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLoad) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Carregar", tint = LedEmerald)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
