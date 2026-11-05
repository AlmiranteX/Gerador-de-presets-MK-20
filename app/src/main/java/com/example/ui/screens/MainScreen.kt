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
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import android.app.Activity
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import com.example.ui.components.HelpGuideTab
import com.example.ui.components.ToneAssistantTab
import com.example.ui.components.JsonViewer
import com.example.ui.components.Mk20ModuleQuickNavBar
import com.example.ui.components.Mk20SignalChainPanel
import com.example.ui.components.PedalStompCard
import com.example.ui.components.ProUpgradeDialog
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
    userEmail: String = "",
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentPreset by viewModel.currentPreset.collectAsStateWithLifecycle()
    val songInput by viewModel.songInput.collectAsStateWithLifecycle()
    val songSuggestions by viewModel.songSuggestions.collectAsStateWithLifecycle()
    val isSearchingSongs by viewModel.isSearchingSongs.collectAsStateWithLifecycle()
    val selectedSongSuggestion by viewModel.selectedSongSuggestion.collectAsStateWithLifecycle()
    val youtubeUrl by viewModel.youtubeUrl.collectAsStateWithLifecycle()
    val inputMode by viewModel.inputMode.collectAsStateWithLifecycle()
    val youtubeVideoInfo by viewModel.youtubeVideoInfo.collectAsStateWithLifecycle()
    val isLoadingYouTubeInfo by viewModel.isLoadingYouTubeInfo.collectAsStateWithLifecycle()
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

    val isProUnlocked by viewModel.isProUnlocked.collectAsStateWithLifecycle()
    val dailyGenerationsUsed by viewModel.dailyGenerationsUsed.collectAsStateWithLifecycle()
    val proUpgradeDialogOpen by viewModel.proUpgradeDialogOpen.collectAsStateWithLifecycle()
    val proProductPrice by viewModel.proProductPrice.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val cloudBackupCount by viewModel.cloudBackupCount.collectAsStateWithLifecycle()
    val remainingDailyPresets = (PresetViewModel.MAX_FREE_DAILY_PRESETS - dailyGenerationsUsed).coerceAtLeast(0)

    val exportSizeMode by viewModel.exportSizeMode.collectAsStateWithLifecycle()
    val loadedTemplateBytes by viewModel.loadedTemplateBytes.collectAsStateWithLifecycle()
    val loadedTemplateName by viewModel.loadedTemplateName.collectAsStateWithLifecycle()

    val isRecordingAudio by viewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val audioCountdown by viewModel.audioCountdown.collectAsStateWithLifecycle()
    val audioVibeResult by viewModel.audioVibeResult.collectAsStateWithLifecycle()

    val toneAssistantMessages by viewModel.toneAssistantMessages.collectAsStateWithLifecycle()
    val isToneAssistantThinking by viewModel.isToneAssistantThinking.collectAsStateWithLifecycle()

    var isEditingPresetTitle by remember { androidx.compose.runtime.mutableStateOf(false) }
    var editingTitleText by remember { androidx.compose.runtime.mutableStateOf("") }
    var selectedPedalKey by remember { androidx.compose.runtime.mutableStateOf("AMP") }
    var showAllModulesInChain by remember { androidx.compose.runtime.mutableStateOf(false) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioCapture()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Permissão de microfone necessária para extrair a vibe musical!")
            }
        }
    }

    // File exporters and importers via SAF (Storage Access Framework)
    val exportMk20Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            try {
                val data = viewModel.getExportBytes(useAmk2Header = false)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(data)
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Arquivo '${currentPreset.title}.mk20' (${data.size} bytes) exportado com sucesso para a M-EFCS!")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao salvar arquivo .mk20: ${e.message}")
                }
            }
        }
    }

    val exportAmk20Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            try {
                val data = viewModel.getExportBytes(useAmk2Header = true)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(data)
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Arquivo '${currentPreset.title}.amk20' (${data.size} bytes) exportado com sucesso!")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao salvar arquivo .amk20: ${e.message}")
                }
            }
        }
    }

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

    val importPresetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val fileName = uri.lastPathSegment ?: "preset"
                    viewModel.importPresetFromBytes(bytes, fileName)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao abrir arquivo: ${e.message}")
                }
            }
        }
    }

    val importTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val rawName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "molde.mk20"
                    viewModel.setTemplateBytes(bytes, rawName)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao carregar molde .mk20: ${e.message}")
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
        contentWindowInsets = WindowInsets.safeDrawing,
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
                                text = "Preset20 AI",
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
                                    text = "Gerador com Gemini AI",
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
                    // PRO / FREE Daily Quota Badge
                    Surface(
                        color = when {
                            isProUnlocked -> LedAmber.copy(alpha = 0.2f)
                            remainingDailyPresets == 0 -> Color(0xFFFF5252).copy(alpha = 0.2f)
                            else -> LedEmerald.copy(alpha = 0.16f)
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isProUnlocked -> LedAmber
                                remainingDailyPresets == 0 -> Color(0xFFFF5252)
                                else -> LedEmerald.copy(alpha = 0.7f)
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { viewModel.openProUpgradeDialog() }
                            .testTag("top_bar_pro_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isProUnlocked) Icons.Default.Verified else Icons.Default.Star,
                                contentDescription = null,
                                tint = when {
                                    isProUnlocked -> LedAmber
                                    remainingDailyPresets == 0 -> Color(0xFFFF8A80)
                                    else -> LedEmerald
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isProUnlocked) "PRO" else "FREE $remainingDailyPresets/${PresetViewModel.MAX_FREE_DAILY_PRESETS}",
                                color = when {
                                    isProUnlocked -> LedAmber
                                    remainingDailyPresets == 0 -> Color(0xFFFF8A80)
                                    else -> TextPrimary
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

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
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp,
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
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pedaleira",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_pedalboard")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Assistente",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_assistant")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Binário",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_json")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Salvos (${savedPresets.size})",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_saved")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ajuda",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_help")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: GENERATOR INPUTS + VIRTUAL PEDALBOARD
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 36.dp),
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

                                    // Mode Switcher: Escolha entre Nome ou Link do YouTube
                                    Text(
                                        text = "Escolha como deseja configurar o timbre:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0F141F))
                                            .border(1.dp, MetalBorder, RoundedCornerShape(12.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Modo 1: Digitar Nome
                                        val isSearch = inputMode == PresetViewModel.InputMode.SEARCH_NAME
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.setInputMode(PresetViewModel.InputMode.SEARCH_NAME) }
                                                .testTag("mode_search_button"),
                                            color = if (isSearch) LedEmerald else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 9.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (isSearch) Color.White else TextSecondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Nome",
                                                    fontWeight = if (isSearch) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSearch) Color.White else TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        // Modo 2: Link do YouTube
                                        val isYt = inputMode == PresetViewModel.InputMode.YOUTUBE_LINK
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.setInputMode(PresetViewModel.InputMode.YOUTUBE_LINK) }
                                                .testTag("mode_youtube_button"),
                                            color = if (isYt) LedEmerald else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 9.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = null,
                                                    tint = if (isYt) Color.White else Color(0xFFFF4D4D),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "YouTube",
                                                    fontWeight = if (isYt) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isYt) Color.White else TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        // Modo 3: Reconhecer Vibe (Mic)
                                        val isAudio = inputMode == PresetViewModel.InputMode.AUDIO_VIBE
                                        Surface(
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.setInputMode(PresetViewModel.InputMode.AUDIO_VIBE) }
                                                .testTag("mode_audio_vibe_button"),
                                            color = if (isAudio) LedEmerald else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 9.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = if (isAudio) Color.White else LedCyan,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Vibe (Mic)",
                                                    fontWeight = if (isAudio) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isAudio) Color.White else TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Content based on chosen Mode
                                    when (inputMode) {
                                        PresetViewModel.InputMode.SEARCH_NAME -> {
                                            // Song / Musical Style Field with Live Search Trigger
                                            OutlinedTextField(
                                                value = songInput,
                                                onValueChange = { viewModel.setSongInput(it) },
                                                label = { Text("Pesquisar Música (Nome ou Artista)") },
                                                placeholder = { Text("Ex: I Will Not Bow, Eli Soares...") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Search, contentDescription = null, tint = LedEmerald)
                                                },
                                                trailingIcon = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (isSearchingSongs) {
                                                            CircularProgressIndicator(
                                                                color = LedEmerald,
                                                                strokeWidth = 2.dp,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                        if (songInput.isNotBlank()) {
                                                            IconButton(onClick = {
                                                                viewModel.setSongInput("")
                                                                viewModel.clearSelectedSongSuggestion()
                                                                viewModel.clearSongSuggestions()
                                                            }) {
                                                                Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = TextMuted)
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = { viewModel.searchSongs() },
                                                            modifier = Modifier.testTag("btn_trigger_song_search")
                                                        ) {
                                                            Icon(Icons.Default.Search, contentDescription = "Buscar opções", tint = LedEmerald)
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
                                                    .testTag("song_input")
                                            )

                                            // Live searching status indicator
                                            if (isSearchingSongs) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = LedEmerald,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Buscando 3 a 5 opções no catálogo / IA...",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }

                                            // Confirmed Song Badge / Card (when user picked an exact song)
                                            val confirmed = selectedSongSuggestion
                                            if (confirmed != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFF0C1B14),
                                                    border = BorderStroke(1.5.dp, LedEmerald),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp)
                                                        .testTag("confirmed_song_card")
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!confirmed.artworkUrl.isNullOrBlank()) {
                                                                AsyncImage(
                                                                    model = confirmed.artworkUrl,
                                                                    contentDescription = "Capa do Álbum",
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier
                                                                        .size(44.dp)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .border(1.dp, MetalBorder, RoundedCornerShape(8.dp))
                                                                )
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(44.dp)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(LedEmerald.copy(alpha = 0.2f))
                                                                        .border(1.dp, LedEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckCircle,
                                                                        contentDescription = null,
                                                                        tint = LedEmerald,
                                                                        modifier = Modifier.size(22.dp)
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.width(10.dp))

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckCircle,
                                                                        contentDescription = null,
                                                                        tint = LedEmerald,
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        text = "MÚSICA CONFIRMADA PARA A IA",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = LedEmerald,
                                                                        fontSize = 10.sp,
                                                                        letterSpacing = 0.5.sp
                                                                    )
                                                                }

                                                                Spacer(modifier = Modifier.height(2.dp))

                                                                Text(
                                                                    text = "🎵 ${confirmed.artist} – ${confirmed.title}",
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = TextPrimary,
                                                                    fontSize = 13.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )

                                                                Text(
                                                                    text = "Álbum: ${confirmed.album.ifBlank { "Original" }}${if (confirmed.version.isNotBlank()) " [${confirmed.version}]" else ""}${if (confirmed.genre.isNotBlank()) " • " + confirmed.genre else ""}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = TextSecondary,
                                                                    fontSize = 11.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = { viewModel.clearSelectedSongSuggestion() },
                                                                modifier = Modifier.testTag("clear_confirmed_song_button")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Clear,
                                                                    contentDescription = "Limpar confirmação",
                                                                    tint = TextSecondary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(4.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "✓ Artista + Título + Álbum confirmados",
                                                                fontSize = 10.sp,
                                                                color = LedEmerald.copy(alpha = 0.9f)
                                                            )

                                                            TextButton(
                                                                onClick = { viewModel.searchSongs() },
                                                                modifier = Modifier.testTag("btn_change_version")
                                                            ) {
                                                                Text(
                                                                    text = "Ver outras versões",
                                                                    fontSize = 11.sp,
                                                                    color = LedCyan,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Options List (3 to 5 options returned for user selection)
                                            if (selectedSongSuggestion == null && songSuggestions.isNotEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFF0F1420),
                                                    border = BorderStroke(1.dp, MetalBorder),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp)
                                                        .testTag("song_suggestions_container")
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(bottom = 8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.LibraryMusic,
                                                                contentDescription = null,
                                                                tint = LedEmerald,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Selecione a opção exata para a IA (${songSuggestions.size} opções):",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = TextPrimary,
                                                                fontSize = 11.sp
                                                            )
                                                            Spacer(modifier = Modifier.weight(1f))
                                                            TextButton(
                                                                onClick = { viewModel.clearSongSuggestions() }
                                                            ) {
                                                                Text("Fechar", fontSize = 11.sp, color = TextMuted)
                                                            }
                                                        }

                                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            songSuggestions.forEachIndexed { index, suggestion ->
                                                                Surface(
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    color = Color(0xFF141B2B),
                                                                    border = BorderStroke(1.dp, MetalBorderHighlight),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clickable { viewModel.selectSongSuggestion(suggestion) }
                                                                        .testTag("song_suggestion_$index")
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.padding(8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        if (!suggestion.artworkUrl.isNullOrBlank()) {
                                                                            AsyncImage(
                                                                                model = suggestion.artworkUrl,
                                                                                contentDescription = null,
                                                                                contentScale = ContentScale.Crop,
                                                                                modifier = Modifier
                                                                                    .size(36.dp)
                                                                                    .clip(RoundedCornerShape(6.dp))
                                                                            )
                                                                        } else {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .size(36.dp)
                                                                                    .clip(RoundedCornerShape(6.dp))
                                                                                    .background(Color(0xFF1E293B)),
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.MusicNote,
                                                                                    contentDescription = null,
                                                                                    tint = LedEmerald,
                                                                                    modifier = Modifier.size(18.dp)
                                                                                )
                                                                            }
                                                                        }

                                                                        Spacer(modifier = Modifier.width(8.dp))

                                                                        Column(modifier = Modifier.weight(1f)) {
                                                                            Text(
                                                                                text = "🎵 ${suggestion.artist} – ${suggestion.title}",
                                                                                style = MaterialTheme.typography.bodyMedium,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = TextPrimary,
                                                                                fontSize = 12.sp,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis
                                                                            )
                                                                            Spacer(modifier = Modifier.height(1.dp))
                                                                            Text(
                                                                                text = "Álbum: ${suggestion.album.ifBlank { "Original" }}${if (suggestion.version.isNotBlank()) " (${suggestion.version})" else ""}${if (suggestion.genre.isNotBlank()) " • " + suggestion.genre else ""}",
                                                                                style = MaterialTheme.typography.bodySmall,
                                                                                color = TextSecondary,
                                                                                fontSize = 10.sp,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis
                                                                            )
                                                                        }

                                                                        Spacer(modifier = Modifier.width(6.dp))

                                                                        Surface(
                                                                            color = LedEmerald.copy(alpha = 0.15f),
                                                                            shape = RoundedCornerShape(6.dp)
                                                                        ) {
                                                                            Text(
                                                                                text = "Escolher",
                                                                                color = LedEmerald,
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Explicit search button if no results yet and query has characters
                                            if (selectedSongSuggestion == null && songSuggestions.isEmpty() && songInput.trim().length >= 2 && !isSearchingSongs) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                OutlinedButton(
                                                    onClick = { viewModel.searchSongs() },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("btn_search_song_options"),
                                                    border = BorderStroke(1.dp, MetalBorder),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LedEmerald),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Buscar opções de músicas (3-5 opções com álbum)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }

                                        PresetViewModel.InputMode.YOUTUBE_LINK -> {
                                            // YOUTUBE LINK MODE
                                            OutlinedTextField(
                                                value = youtubeUrl,
                                                onValueChange = { viewModel.setYoutubeUrl(it) },
                                                label = { Text("Link do YouTube da Música") },
                                                placeholder = { Text("Cole aqui o link: https://youtu.be/...") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFFF0000))
                                                },
                                                trailingIcon = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (youtubeUrl.isNotBlank()) {
                                                            IconButton(onClick = { viewModel.setYoutubeUrl("") }) {
                                                                Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = TextMuted)
                                                            }
                                                        } else {
                                                            IconButton(
                                                                onClick = {
                                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                                    if (!clipText.isNullOrBlank()) {
                                                                        viewModel.setYoutubeUrl(clipText.trim())
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(Icons.Default.ContentPaste, contentDescription = "Colar", tint = LedEmerald)
                                                            }
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

                                            // Loading indicator when fetching YouTube video metadata
                                            if (isLoadingYouTubeInfo) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = LedEmerald,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Identificando título da música no YouTube...",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }

                                            // YouTube Live Thumbnail & Video Details Preview
                                            if (youtubeId != null) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Surface(
                                                    color = Color(0xFF0D121C),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MetalBorder),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(youtubeVideoInfo?.thumbnailUrl ?: "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg")
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = "Thumbnail do YouTube",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .size(width = 86.dp, height = 54.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = youtubeVideoInfo?.title?.ifBlank { "Vídeo do YouTube Identificado" } ?: "Vídeo do YouTube Identificado",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (!youtubeVideoInfo?.authorName.isNullOrBlank()) {
                                                                Text(
                                                                    text = "Canal: ${youtubeVideoInfo?.authorName}",
                                                                    fontSize = 11.sp,
                                                                    color = LedEmerald
                                                                )
                                                            }
                                                            Text(
                                                                text = "✓ Título extraído para o preset",
                                                                fontSize = 10.sp,
                                                                color = TextMuted
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Editable preset name field
                                                OutlinedTextField(
                                                    value = songInput,
                                                    onValueChange = { viewModel.setSongInput(it) },
                                                    label = { Text("Nome do Preset (Editável)") },
                                                    placeholder = { Text("Nome da música...") },
                                                    supportingText = {
                                                        Text(
                                                            text = "O preset gerado receberá este nome. Você pode editar à vontade.",
                                                            fontSize = 10.sp,
                                                            color = TextMuted
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Edit, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(18.dp))
                                                    },
                                                    trailingIcon = {
                                                        if (songInput.isNotBlank()) {
                                                            IconButton(onClick = { viewModel.setSongInput("") }) {
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
                                                        .testTag("preset_name_input_youtube")
                                                )
                                            }
                                        }

                                        PresetViewModel.InputMode.AUDIO_VIBE -> {
                                            // AUDIO VIBE MODE
                                            Surface(
                                                color = Color(0xFF0C101A),
                                                shape = RoundedCornerShape(14.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecordingAudio) LedEmerald else MetalBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Reconhecimento Acústico em Tempo Real",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Toque em ouvir e aproxime o microfone da guitarra ou caixa de som por 5 segundos. O app analisa dinâmica, ganho, agudos e BPM para sugerir o preset perfeito na MK-20.",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary,
                                                        lineHeight = 15.sp
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    if (isRecordingAudio) {
                                                        // Live recording visualizer
                                                        Surface(
                                                            color = Color(0xFF141A29),
                                                            shape = RoundedCornerShape(12.dp),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, LedEmerald.copy(alpha = 0.5f)),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(16.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Center
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(10.dp)
                                                                            .clip(CircleShape)
                                                                            .background(Color.Red)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Text(
                                                                        text = "Ouvindo áudio... $audioCountdown s restantes",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White,
                                                                        fontSize = 13.sp
                                                                    )
                                                                }

                                                                Spacer(modifier = Modifier.height(14.dp))

                                                                // Audio equalizer dynamic bars
                                                                Row(
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.height(48.dp)
                                                                ) {
                                                                    listOf(0.4f, 0.7f, 1.0f, 0.85f, 0.6f, 0.95f, 0.5f).forEach { scale ->
                                                                        val barHeight = (12.dp + (36.dp * (audioAmplitude * scale).coerceIn(0f, 1f)))
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .width(7.dp)
                                                                                .height(barHeight)
                                                                                .clip(RoundedCornerShape(4.dp))
                                                                                .background(LedEmerald)
                                                                        )
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.height(12.dp))

                                                                TextButton(
                                                                    onClick = { viewModel.stopAudioCapture() },
                                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                                                                ) {
                                                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("Cancelar Gravação", fontSize = 12.sp)
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        // Listening trigger button
                                                        Button(
                                                            onClick = {
                                                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF162032)
                                                            ),
                                                            shape = RoundedCornerShape(12.dp),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, LedEmerald),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(48.dp)
                                                                .testTag("btn_listen_audio_vibe")
                                                        ) {
                                                            Icon(Icons.Default.Mic, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(20.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = if (audioVibeResult != null) "Ouvir Novamente (5s)" else "Ouvir com Microfone (5s)",
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                    }

                                                    // Display results if audio vibe was captured
                                                    if (audioVibeResult != null) {
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Surface(
                                                            color = Color(0xFF111724),
                                                            shape = RoundedCornerShape(12.dp),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, LedEmerald.copy(alpha = 0.5f)),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(modifier = Modifier.padding(12.dp)) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(16.dp))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = "Vibe Detectada:",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = LedEmerald,
                                                                        fontSize = 12.sp
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = audioVibeResult?.detectedStyle.orEmpty(),
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White,
                                                                        fontSize = 12.sp,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }

                                                                Spacer(modifier = Modifier.height(8.dp))

                                                                // Badges for Ganho e BPM
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Surface(
                                                                        color = Color(0xFF1A2333),
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.Center
                                                                        ) {
                                                                            Icon(Icons.Default.Bolt, contentDescription = null, tint = LedAmber, modifier = Modifier.size(14.dp))
                                                                            Spacer(modifier = Modifier.width(4.dp))
                                                                            Text(
                                                                                text = audioVibeResult?.gainVibe.orEmpty(),
                                                                                fontSize = 10.sp,
                                                                                color = TextPrimary,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis
                                                                            )
                                                                        }
                                                                    }

                                                                    Surface(
                                                                        color = Color(0xFF1A2333),
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.Center
                                                                        ) {
                                                                            Icon(Icons.Default.Timer, contentDescription = null, tint = LedCyan, modifier = Modifier.size(14.dp))
                                                                            Spacer(modifier = Modifier.width(4.dp))
                                                                            Text(
                                                                                text = "${audioVibeResult?.estimatedBpm ?: 120} BPM",
                                                                                fontSize = 10.sp,
                                                                                color = TextPrimary
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.height(8.dp))

                                                                // Suggested models preview
                                                                Text(
                                                                    text = "Modelos Sugeridos da MK-20:",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = TextSecondary
                                                                )
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(
                                                                    text = "• AMP: ${audioVibeResult?.suggestedAmp} | CAB: ${audioVibeResult?.suggestedCab}\n• Drive: ${audioVibeResult?.suggestedDrive} | Delay: ${audioVibeResult?.suggestedDelay}\n• Mod: ${audioVibeResult?.suggestedMod} | Reverb: ${audioVibeResult?.suggestedReverb}",
                                                                    fontSize = 10.sp,
                                                                    color = TextPrimary,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    lineHeight = 14.sp
                                                                )

                                                                Spacer(modifier = Modifier.height(8.dp))

                                                                Text(
                                                                    text = audioVibeResult?.details.orEmpty(),
                                                                    fontSize = 10.sp,
                                                                    color = TextMuted,
                                                                    lineHeight = 14.sp
                                                                )

                                                                Spacer(modifier = Modifier.height(8.dp))

                                                                // Editable preset name field
                                                                OutlinedTextField(
                                                                    value = songInput,
                                                                    onValueChange = { viewModel.setSongInput(it) },
                                                                    label = { Text("Nome do Preset (Editável)") },
                                                                    placeholder = { Text("Nome da música...") },
                                                                    supportingText = {
                                                                        Text(
                                                                            text = "Você pode alterar o nome do preset antes de gerar.",
                                                                            fontSize = 10.sp,
                                                                            color = TextMuted
                                                                        )
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(Icons.Default.Edit, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(18.dp))
                                                                    },
                                                                    trailingIcon = {
                                                                        if (songInput.isNotBlank()) {
                                                                            IconButton(onClick = { viewModel.setSongInput("") }) {
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
                                                                        .testTag("preset_name_input_vibe")
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Detalhes e Preferências do Timbre
                                    OutlinedTextField(
                                        value = detailsInput,
                                        onValueChange = { viewModel.setDetailsInput(it) },
                                        label = { Text("Detalhes e Preferências do Timbre") },
                                        placeholder = { Text("Ex: Captador Single Coil, som limpo com chorus suave e delay colcheia pontuada.") },
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

                                    // Quick tags para agilizar a criação do timbre
                                    val quickToneTags = listOf(
                                        "Single Coil", "Humbucker", "Clean Cristalino", "Warm Drive",
                                        "Crunch", "Solo / Lead", "Heavy Metal", "Worship Ambient"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        quickToneTags.forEach { tag ->
                                            FilterChip(
                                                selected = detailsInput.contains(tag, ignoreCase = true),
                                                onClick = {
                                                    val current = detailsInput.trim()
                                                    if (current.isEmpty()) {
                                                        viewModel.setDetailsInput(tag)
                                                    } else if (!current.contains(tag, ignoreCase = true)) {
                                                        viewModel.setDetailsInput("$current, $tag")
                                                    }
                                                },
                                                label = { Text(tag, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LedEmerald.copy(alpha = 0.2f),
                                                    selectedLabelColor = LedEmerald
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Daily Quota / PRO Status Banner
                                    Surface(
                                        color = Color(0xFF0C101A),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isProUnlocked -> LedAmber.copy(alpha = 0.6f)
                                                remainingDailyPresets == 0 -> Color(0xFFFF5252).copy(alpha = 0.7f)
                                                else -> MetalBorder
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("daily_quota_card")
                                    ) {
                                        if (isProUnlocked) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.openProUpgradeDialog() }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Verified,
                                                        contentDescription = null,
                                                        tint = LedAmber,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Preset20 AI PRO Vitalício • Gerações Ilimitadas",
                                                        color = LedAmber,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Text(
                                                    text = "ATIVO",
                                                    color = LedEmerald,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = if (remainingDailyPresets > 0) {
                                                                "Presets Grátis Hoje: $remainingDailyPresets de ${PresetViewModel.MAX_FREE_DAILY_PRESETS} disponíveis"
                                                            } else {
                                                                "Limite Diário Atingido (0 de ${PresetViewModel.MAX_FREE_DAILY_PRESETS} restantes hoje)"
                                                            },
                                                            color = if (remainingDailyPresets == 0) Color(0xFFFF8A80) else TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                        Text(
                                                            text = "Desbloqueie tudo com pagamento único (sem mensalidade)",
                                                            color = TextMuted,
                                                            fontSize = 10.sp
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Surface(
                                                        color = LedAmber.copy(alpha = 0.18f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, LedAmber),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.openProUpgradeDialog() }
                                                            .testTag("generator_upgrade_pro_btn")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.LockOpen,
                                                                contentDescription = null,
                                                                tint = LedAmber,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Liberar PRO",
                                                                color = LedAmber,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                // 4 LED Bars for Daily Quota
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    for (i in 0 until PresetViewModel.MAX_FREE_DAILY_PRESETS) {
                                                        val available = i < remainingDailyPresets
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(6.dp)
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(
                                                                    if (available) LedEmerald else Color(0xFF252D40)
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Generate Button
                                    Button(
                                        onClick = { viewModel.generatePreset() },
                                        enabled = !isGenerating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isProUnlocked && remainingDailyPresets == 0) LedAmber else LedEmerald,
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
                                        } else if (!isProUnlocked && remainingDailyPresets == 0) {
                                            Icon(
                                                Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Liberar Presets Ilimitados (PRO Vitalício)",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
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
                                                when (inputMode) {
                                                    PresetViewModel.InputMode.AUDIO_VIBE -> "Gerar Preset da Vibe do Áudio"
                                                    PresetViewModel.InputMode.YOUTUBE_LINK -> "Gerar Preset do YouTube"
                                                    PresetViewModel.InputMode.SEARCH_NAME -> "Gerar Preset para MK-20"
                                                },
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentPreset.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${currentPreset.style} • ${currentPreset.bpm} BPM",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LedEmerald,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            editingTitleText = currentPreset.title
                                            isEditingPresetTitle = true
                                        },
                                        modifier = Modifier.testTag("edit_preset_title_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar Nome do Preset",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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

                        // Real Rig Description
                        if (currentPreset.details.isNotBlank()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F1420),
                                    border = BorderStroke(1.dp, MetalBorderHighlight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = LedEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = currentPreset.details,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Pedals Chain Header + MK-20 Hardware LCD Screen
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CADEIA DE SINAL M-VAVE MK-20",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Toque em um módulo para editar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedEmerald,
                                        fontSize = 10.sp
                                    )
                                }

                                Mk20SignalChainPanel(
                                    preset = currentPreset,
                                    selectedPedalKey = selectedPedalKey,
                                    onSelectPedal = { key -> selectedPedalKey = key },
                                    onToggleSelectedPedal = { key -> viewModel.togglePedalActive(key) }
                                )

                                Mk20ModuleQuickNavBar(
                                    selectedPedalKey = selectedPedalKey,
                                    showAllModules = showAllModulesInChain,
                                    onSelectPedal = { key -> selectedPedalKey = key },
                                    onToggleShowAll = { showAllModulesInChain = !showAllModulesInChain }
                                )
                            }
                        }

                        if (!showAllModulesInChain) {
                            // MODO VISOR MK-20 (Padrão): Exibe apenas as configurações do módulo clicado!
                            val selectedModule = when (selectedPedalKey) {
                                "FX" -> currentPreset.pedals.FX
                                "NS" -> currentPreset.pedals.activeNS
                                "DS" -> currentPreset.pedals.DS
                                "AMP" -> currentPreset.pedals.AMP
                                "CAB" -> currentPreset.pedals.CAB
                                "EQ" -> currentPreset.pedals.EQ
                                "MOD" -> currentPreset.pedals.MOD
                                "DELAY" -> currentPreset.pedals.DELAY
                                "REVERB" -> currentPreset.pedals.REVERB
                                "VOL" -> currentPreset.pedals.VOL
                                else -> currentPreset.pedals.AMP
                            }

                            selectedModule?.let { module ->
                                item(key = "focused_module_$selectedPedalKey") {
                                    PedalStompCard(
                                        pedalKey = selectedPedalKey,
                                        module = module,
                                        onToggleActive = { viewModel.togglePedalActive(selectedPedalKey) },
                                        onParamChange = { p, v -> viewModel.updatePedalParam(selectedPedalKey, p, v) },
                                        onModelChange = { m -> viewModel.updatePedalModel(selectedPedalKey, m) }
                                    )
                                }
                            }
                        } else {
                            // Modo Opcional "Ver Todos (10)" caso o usuário queira expandir a lista inteira
                            currentPreset.pedals.FX?.let { fxModule ->
                                item {
                                    PedalStompCard(
                                        pedalKey = "FX",
                                        module = fxModule,
                                        onToggleActive = { viewModel.togglePedalActive("FX") },
                                        onParamChange = { p, v -> viewModel.updatePedalParam("FX", p, v) },
                                        onModelChange = { m -> viewModel.updatePedalModel("FX", m) }
                                    )
                                }
                            }

                            currentPreset.pedals.NS?.let { nsModule ->
                                item {
                                    PedalStompCard(
                                        pedalKey = "NS",
                                        module = nsModule,
                                        onToggleActive = { viewModel.togglePedalActive("NS") },
                                        onParamChange = { p, v -> viewModel.updatePedalParam("NS", p, v) },
                                        onModelChange = { m -> viewModel.updatePedalModel("NS", m) }
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

                            currentPreset.pedals.EQ?.let { eqModule ->
                                item {
                                    PedalStompCard(
                                        pedalKey = "EQ",
                                        module = eqModule,
                                        onToggleActive = { viewModel.togglePedalActive("EQ") },
                                        onParamChange = { p, v -> viewModel.updatePedalParam("EQ", p, v) },
                                        onModelChange = { m -> viewModel.updatePedalModel("EQ", m) }
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

                            currentPreset.pedals.VOL?.let { volModule ->
                                item {
                                    PedalStompCard(
                                        pedalKey = "VOL",
                                        module = volModule,
                                        onToggleActive = { viewModel.togglePedalActive("VOL") },
                                        onParamChange = { p, v -> viewModel.updatePedalParam("VOL", p, v) },
                                        onModelChange = { m -> viewModel.updatePedalModel("VOL", m) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: ASSISTENTE DE TIMBRE (ROADIE IA)
                    ToneAssistantTab(
                        currentPreset = currentPreset,
                        messages = toneAssistantMessages,
                        isThinking = isToneAssistantThinking,
                        isProUnlocked = isProUnlocked,
                        dailyUsed = dailyGenerationsUsed,
                        maxDailyFree = PresetViewModel.MAX_FREE_DAILY_PRESETS,
                        onSendMessage = { prompt -> viewModel.sendToneAssistantMessage(prompt) },
                        onApplyToPedalboard = { preset -> viewModel.applyAssistantPresetToPedalboard(preset) },
                        onSavePreset = { preset -> viewModel.openSaveDialog(preset) },
                        onExportPreset = { preset -> viewModel.openShareDialog(preset) },
                        onClearConversation = { viewModel.clearAssistantConversation() },
                        onOpenUpgradeDialog = { viewModel.openProUpgradeDialog() }
                    )
                }

                2 -> {
                    // TAB 2: BINARY MK-20 & JSON EXPORT
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            val sanitizedName = remember(currentPreset.title) {
                                val clean = currentPreset.title
                                    .trim()
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
                                    .replace("[^a-zA-Z0-9_]".toRegex(), "_")
                                    .replace("_{2,}".toRegex(), "_")
                                    .trim('_')
                                    .lowercase()
                                clean.take(15).trim('_').ifBlank { "mk20_preset" }
                            }

                            JsonViewer(
                                preset = currentPreset,
                                exportSizeMode = exportSizeMode,
                                loadedTemplateName = loadedTemplateName,
                                loadedTemplateSize = loadedTemplateBytes?.size,
                                onSelectSizeMode = { mode -> viewModel.setExportSizeMode(mode) },
                                onCopyJson = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("MK-20 Preset JSON", currentPreset.toJsonString(indent = 2)))
                                    scope.launch { snackbarHostState.showSnackbar("JSON copiado para a área de transferência!") }
                                },
                                onShareLink = { viewModel.openShareDialog() },
                                onExportMk20 = {
                                    exportMk20Launcher.launch("${sanitizedName}.mk20")
                                },
                                onExportAmk20 = {
                                    exportAmk20Launcher.launch("${sanitizedName}.amk20")
                                },
                                onExportJson = {
                                    exportJsonLauncher.launch("${sanitizedName}.json")
                                },
                                onImportFile = {
                                    importPresetLauncher.launch(arrayOf("*/*"))
                                },
                                onImportTemplate = {
                                    importTemplateLauncher.launch(arrayOf("*/*"))
                                },
                                onClearTemplate = {
                                    viewModel.clearTemplate()
                                },
                                getExportBytes = { useAmk2 -> viewModel.getExportBytes(useAmk2) }
                            )
                        }
                    }
                }

                3 -> {
                    // TAB 3: SAVED PRESETS LIBRARY (ROOM + CLOUD FIRESTORE BACKUP)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Google Cloud Backup Banner
                        item {
                            Surface(
                                color = Color(0xFF0E1624),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = LedEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Backup na Nuvem Google Ativo",
                                                color = LedEmerald,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (userEmail.isNotBlank()) {
                                                "$userEmail • Seus presets ficam salvos na nuvem"
                                            } else {
                                                "Seus presets favoritos estão protegidos na sua Conta Google"
                                            },
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.syncPresetsWithCloudNow() },
                                        enabled = !isCloudSyncing,
                                        border = BorderStroke(1.dp, LedEmerald),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("btn_sync_cloud_presets")
                                    ) {
                                        Text(
                                            text = if (isCloudSyncing) "Sincronizando..." else "Sincronizar",
                                            color = LedEmerald,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

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

                4 -> {
                    // TAB 4: CENTRAL DE AJUDA & TUTORIAL COMPLETO
                    HelpGuideTab(
                        onOpenSettings = { viewModel.openSettingsDialog() },
                        onGoToPedalboardTab = { viewModel.setSelectedTab(0) },
                        onGoToBinaryTab = { viewModel.setSelectedTab(2) }
                    )
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

    if (proUpgradeDialogOpen) {
        ProUpgradeDialog(
            isProUnlocked = isProUnlocked,
            dailyUsed = dailyGenerationsUsed,
            maxDailyFree = PresetViewModel.MAX_FREE_DAILY_PRESETS,
            productPrice = proProductPrice,
            userEmail = userEmail,
            cloudBackupCount = cloudBackupCount,
            isCloudSyncing = isCloudSyncing,
            onBuyProClick = {
                viewModel.launchProPurchase(context as? Activity)
            },
            onRestorePurchasesClick = {
                viewModel.restoreProPurchases()
            },
            onSyncCloudClick = {
                viewModel.syncPresetsWithCloudNow()
            },
            onSignOutClick = onSignOut,
            onDebugActivatePro = {
                viewModel.debugActivateProLifetime()
            },
            onDebugResetFree = {
                viewModel.debugResetToFreePlan()
            },
            onDebugExhaustQuota = {
                viewModel.debugExhaustDailyQuota()
            },
            onDismiss = {
                viewModel.closeProUpgradeDialog()
            }
        )
    }

    if (isEditingPresetTitle) {
        AlertDialog(
            onDismissRequest = { isEditingPresetTitle = false },
            containerColor = PedalCardDark,
            title = {
                Text(
                    text = "Editar Nome do Preset",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Altere o nome deste preset como desejar:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editingTitleText,
                        onValueChange = { editingTitleText = it },
                        label = { Text("Nome do Preset") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LedEmerald,
                            unfocusedBorderColor = MetalBorder,
                            focusedLabelColor = LedEmerald
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_preset_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePresetTitle(editingTitleText)
                        isEditingPresetTitle = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingPresetTitle = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
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
