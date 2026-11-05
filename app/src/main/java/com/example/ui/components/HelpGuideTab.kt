package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.MetalBorderHighlight
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class TutorialVisualStep(
    val stepNumber: String,
    val title: String,
    val description: String,
    val assetPath: String,
    val badgeText: String
)

private val EXPORT_TUTORIAL_STEPS = listOf(
    TutorialVisualStep(
        stepNumber = "PASSO 1",
        title = "Escolha o Slot de Destino na MK-20",
        description = "Com a pedaleira conectada ao M-EFCS (via Bluetooth no celular ou USB no PC), selecione na barra superior o número do banco/slot onde deseja carregar o timbre (ex.: [083] Abraca-me).",
        assetPath = "tutorial/passo_1.png",
        badgeText = "1. Selecionar Slot [083]"
    ),
    TutorialVisualStep(
        stepNumber = "PASSO 2",
        title = "Clique no Botão '... More'",
        description = "Logo abaixo do nome do preset, ao lado dos botões 'Save' e 'Save to', toque ou clique no botão '... More' (destacado em vermelho) para abrir o menu de importação.",
        assetPath = "tutorial/passo_2.png",
        badgeText = "2. Abrir Menu '... More'"
    ),
    TutorialVisualStep(
        stepNumber = "PASSO 3",
        title = "Selecione 'Import current preset'",
        description = "No menu suspenso que se abre, clique na 2ª opção: 'Import current preset' (Importar preset atual). Isso carregará o timbre diretamente no slot selecionado.",
        assetPath = "tutorial/passo_3.png",
        badgeText = "3. Import current preset"
    ),
    TutorialVisualStep(
        stepNumber = "PASSO 4",
        title = "Escolha o Arquivo .mk20 Exportado",
        description = "Na janela de arquivos, localize o arquivo '.mk20' que você exportou aqui no app (ex.: Abracame.mk20), verifique o filtro 'Files (*.mk20)' e clique em 'Abrir'.",
        assetPath = "tutorial/passo_4.png",
        badgeText = "4. Selecionar Arquivo .mk20"
    ),
    TutorialVisualStep(
        stepNumber = "PASSO 5",
        title = "Confirme no Botão Laranja 'Save'",
        description = "Após importar os 10 módulos para a tela, clique em 'Save' / 'Save to', confira o nome e a posição (Position [83]) e clique no botão laranja 'Save' para gravar na memória física da pedaleira.",
        assetPath = "tutorial/passo_5.png",
        badgeText = "5. Gravar na Memória (Save)"
    ),
    TutorialVisualStep(
        stepNumber = "PASSO 6",
        title = "Pronto! Preset Salvo na Pedaleira ('Saved to [083]')",
        description = "Uma faixa verde aparecerá no topo confirmando 'Saved to [083]'. O timbre já está gravado definitivamente no hardware da sua M-Vave MK-20!",
        assetPath = "tutorial/passo_6.png",
        badgeText = "6. Confirmado: Saved to [083]"
    )
)

@Composable
fun HelpGuideTab(
    onOpenSettings: () -> Unit,
    onGoToPedalboardTab: () -> Unit,
    onGoToBinaryTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 0 = Todos, 1 = Como Usar, 2 = Enviar p/ Pedaleira (BT/USB), 3 = Chave API
    var selectedFilter by remember { mutableIntStateOf(0) }
    var zoomedImageStep by remember { mutableStateOf<TutorialVisualStep?>(null) }

    // Modal de Zoom em tela cheia ao tocar em qualquer imagem do tutorial
    zoomedImageStep?.let { step ->
        TutorialImageZoomDialog(
            step = step,
            onDismiss = { zoomedImageStep = null }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("help_guide_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HERO HEADER: O QUE É E COMO FUNCIONA O APP
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PedalCardDark,
                border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0A221B),
                                    PedalCardDark
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LedEmerald.copy(alpha = 0.18f))
                                .border(1.dp, LedEmerald, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = LedEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CENTRAL DE AJUDA & TUTORIAL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Guia oficial do Preset20 AI (Firmware v85)",
                                fontSize = 11.sp,
                                color = LedEmerald,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "O Preset20 AI analisa músicas, links do YouTube ou amostras de áudio com Inteligência Artificial e monta automaticamente os 10 módulos da pedaleira física M-Vave MK-20 (368 modelos oficiais de fábrica), gerando o arquivo binário autêntico de 412 bytes (.mk20) pronto para importar via Bluetooth ou USB.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resumo técnico rápido em chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecBadge(
                            label = "368 Modelos",
                            sub = "Catálogo Oficial",
                            color = LedEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        SpecBadge(
                            label = "412 Bytes",
                            sub = "Binário .mk20",
                            color = LedCyan,
                            modifier = Modifier.weight(1f)
                        )
                        SpecBadge(
                            label = "3 Instrumentos",
                            sub = "Violão/Gtr/Baixo",
                            color = LedAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // FILTRO RÁPIDOS DE TÓPICOS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipButton(
                    text = "Todos os Guias",
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    tag = "help_filter_all"
                )
                FilterChipButton(
                    text = "1. Como Usar o App",
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    tag = "help_filter_usage"
                )
                FilterChipButton(
                    text = "2. Carregar na MK-20 (BT / USB)",
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    tag = "help_filter_transfer"
                )
                FilterChipButton(
                    text = "3. Chave API Gemini",
                    selected = selectedFilter == 3,
                    onClick = { selectedFilter = 3 },
                    tag = "help_filter_apikey"
                )
                FilterChipButton(
                    text = "4. Contato & Suporte",
                    selected = selectedFilter == 4,
                    onClick = { selectedFilter = 4 },
                    tag = "help_filter_contact"
                )
            }
        }

        // =========================================================================
        // SEÇÃO 1: COMO USAR O APP E COMO ELE FUNCIONA
        // =========================================================================
        if (selectedFilter == 0 || selectedFilter == 1) {
            item {
                HelpSectionCard(
                    stepNumber = "01",
                    title = "COMO USAR O APP E GERAR TIMBRES",
                    subtitle = "Do nome da música ao preset pronto em segundos",
                    accentColor = LedEmerald,
                    icon = Icons.Default.Tune
                ) {
                    HelpStepItem(
                        number = "1",
                        title = "Escolha o Modo de Entrada",
                        description = "Na aba 'Pedaleira', escolha uma das 3 formas para criar seu timbre:\n" +
                            "• 🎵 Nome / Busca: Digite o nome da música ou artista (ex.: 'Metallica - Master of Puppets', 'Coldplay - Yellow') e selecione a sugestão.\n" +
                            "• 📺 Link YouTube: Cole o link de qualquer vídeo ou cover do YouTube. O app identifica o título e analisa a sonoridade da faixa.\n" +
                            "• 🎙️ Ouvir Áudio (5s): Toque no botão do microfone e aproxime o celular da caixa de som por 5 segundos para o app detectar o ganho, BPM e estilo automaticamente.",
                        accentColor = LedEmerald
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HelpStepItem(
                        number = "2",
                        title = "Selecione seu Instrumento (Violão, Guitarra ou Baixo)",
                        description = "Toque no instrumento que você vai plugar na MK-20:\n" +
                            "• 🪕 Violão: Seleciona os IRs acústicos oficiais (71 ao 75: Gibson, Taylor, Takamine, Martin, Yamaha), pré limpo e EQ anti-microfonia.\n" +
                            "• 🎸 Guitarra: Seleciona entre os 90 amplificadores de guitarra, 70 gabinetes (1x12, 2x12, 4x12), overdrives clássicos e Guitar EQ 6.\n" +
                            "• 🎸 Baixo: Seleciona exclusivamente os cabeçotes de baixo '_BS' (91 ao 120: Ampeg SVT, Gallien-Krueger, Darkglass), gabinetes 8x10/4x10 (76 ao 100), drive MW-B7000 e Bass EQ 7.",
                        accentColor = LedEmerald
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HelpStepItem(
                        number = "3",
                        title = "Escolha a Função na Música (Base, Solo ou Arranjo)",
                        description = "Uma mesma música possui diferentes timbres. Você pode alternar a função antes ou depois de gerar o preset:\n" +
                            "• 🎸 Base (Rítmica): Som encorpado e definido, sem embolar os acordes ou riffs.\n" +
                            "• ⚡ Solo (Lead): Empurra os médios, adiciona boost de ganho/volume e ativa Delay/Reverb para sustentação.\n" +
                            "• ✨ Arranjo (Ambient): Ganho controlado com Chorus/Modulação, Delay estéreo e Reverb espacial para dedilhados.",
                        accentColor = LedEmerald
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HelpStepItem(
                        number = "4",
                        title = "Gere, Edite os 10 Pedais e Salve",
                        description = "Toque em 'Gerar Preset para MK-20'. O app montará toda a cadeia de sinal (FX → NS → DS → AMP → CAB → EQ → MOD → DELAY → REVERB → VOL). Você pode ligar/desligar qualquer módulo, trocar o modelo ou ajustar os sliders manualmente, e clicar no ícone de favorito (Salvar) no topo para guardar na aba 'Salvos'.",
                        accentColor = LedEmerald
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onGoToPedalboardTab,
                        border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("help_go_to_pedalboard_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = LedEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ir para o Gerador de Pedaleira", color = LedEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 2: COMO CARREGAR O PRESET NA PEDALEIRA FÍSICA (COM IMAGENS PASSO A PASSO)
        // =========================================================================
        if (selectedFilter == 0 || selectedFilter == 2) {
            item {
                HelpSectionCard(
                    stepNumber = "02",
                    title = "COMO CARREGAR O PRESET NA PEDALEIRA FÍSICA",
                    subtitle = "Tutorial ilustrado passo a passo no M-EFCS (Bluetooth & USB)",
                    accentColor = LedCyan,
                    icon = Icons.Default.Memory
                ) {
                    // FOTO REAL DA PEDALEIRA FÍSICA M-VAVE MK-20
                    val pedalStep = remember {
                        TutorialVisualStep(
                            stepNumber = "HARDWARE M-VAVE MK-20",
                            title = "Pedaleira Física M-Vave MK-20 (Firmware v85)",
                            description = "Cadeia de 10 blocos simultâneos (FX → NS → DS → AMP → CAB → EQ → MOD → DLY → REV → VOL) com conexão Bluetooth (App M-EFCS Mobile) e USB Tipo-C (M-EFCS PC).",
                            assetPath = "tutorial/pedaleira_fisica.jpg",
                            badgeText = "Hardware Oficial MK-20"
                        )
                    }

                    TutorialHardwareBannerCard(
                        step = pedalStep,
                        onClickZoom = { zoomedImageStep = pedalStep }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sub-card A: Bluetooth pelo Celular
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0C121E),
                        border = BorderStroke(1.dp, LedCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = LedCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "CONEXÃO A: PELO CELULAR (BLUETOOTH)",
                                        fontWeight = FontWeight.Bold,
                                        color = LedCyan,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Usando o aplicativo oficial M-EFCS no Android ou iOS",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            HelpBulletStep(
                                step = "1.",
                                text = "Na aba 'Binário (.mk20)' deste app, toque em 'Exportar .mk20 (M-EFCS)' para salvar o arquivo no celular.",
                                color = LedCyan
                            )
                            HelpBulletStep(
                                step = "2.",
                                text = "Ligue a MK-20, ative o Bluetooth/Localização do celular, abra o app 'M-EFCS' e conecte na pedaleira.",
                                color = LedCyan
                            )
                            HelpBulletStep(
                                step = "3.",
                                text = "Siga os 6 passos ilustrados abaixo (a interface do M-EFCS no celular e no PC usa exatamente os mesmos botões: Slot → '... More' → 'Import current preset' → 'Save').",
                                color = LedCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sub-card B: Cabo USB pelo PC
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0C121E),
                        border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = null,
                                    tint = LedEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "CONEXÃO B: PELO COMPUTADOR (CABO USB)",
                                        fontWeight = FontWeight.Bold,
                                        color = LedEmerald,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Usando o programa M-EFCS para Windows ou Mac",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            HelpBulletStep(
                                step = "1.",
                                text = "Exporte o arquivo '.mk20' na aba 'Binário (.mk20)' e transfira para o PC (WhatsApp, Drive ou Cabo).",
                                color = LedEmerald
                            )
                            HelpBulletStep(
                                step = "2.",
                                text = "Conecte a MK-20 via cabo USB-C ao PC, abra o programa M-EFCS e siga o passo a passo visual abaixo:",
                                color = LedEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // TÍTULO DA GALERIA PASSO A PASSO COM AS 6 IMAGENS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "PASSO A PASSO VISUAL NO M-EFCS (6 ETAPAS)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = LedCyan
                            )
                            Text(
                                text = "Toque em qualquer imagem para ampliar em tela cheia",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Ampliar imagens",
                            tint = LedCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // LISTA DOS 6 PASSOS COM SUAS RESPECTIVAS IMAGENS (passo_1.png a passo_6.png)
                    EXPORT_TUTORIAL_STEPS.forEachIndexed { index, step ->
                        TutorialStepImageCard(
                            step = step,
                            stepIndex = index + 1,
                            onClickImage = { zoomedImageStep = step }
                        )
                        if (index < EXPORT_TUTORIAL_STEPS.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dica importante sobre 412 bytes e Molde
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF161D2B),
                        border = BorderStroke(1.dp, MetalBorderHighlight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 Dica de Compatibilidade (412 Bytes Autêntico):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = LedAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mantenha o seletor de tamanho na aba 'Binário (.mk20)' na opção padrão '412B Oficial MK-20'. Ele já inclui a assinatura de hardware oficial do Firmware v85 e o rodapé NGD4 exigidos pelo M-EFCS.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onGoToBinaryTab,
                        colors = ButtonDefaults.buttonColors(containerColor = LedCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("help_go_to_binary_btn")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF06101E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ir para a Aba Binário (.mk20) para Exportar",
                            color = Color(0xFF06101E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 3: COMO OBTER A CHAVE API GEMINI E COLOCAR NO APP
        // =========================================================================
        if (selectedFilter == 0 || selectedFilter == 3) {
            item {
                HelpSectionCard(
                    stepNumber = "03",
                    title = "COMO OBTER A CHAVE API E COLOCAR NO APP",
                    subtitle = "Passo a passo gratuito pelo Google AI Studio",
                    accentColor = LedAmber,
                    icon = Icons.Default.Key
                ) {
                    Text(
                        text = "O aplicativo já funciona sem chave usando o motor inteligente local da MK-20. Porém, ao adicionar sua Chave de API gratuita do Google Gemini, a Inteligência Artificial pesquisa em tempo real os equipamentos reais usados pelo artista em qualquer música!",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HelpStepItem(
                        number = "1",
                        title = "Acesse o Google AI Studio",
                        description = "Toque no botão abaixo para abrir a página oficial do Google AI Studio (aistudio.google.com/apikey) e faça login com sua conta Google.",
                        accentColor = LedAmber
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HelpStepItem(
                        number = "2",
                        title = "Crie sua Chave Gratuita",
                        description = "Toque no botão azul 'Create API Key' (Criar chave de API). Escolha criar em um novo projeto ('Create API key in new project') e aguarde alguns segundos.",
                        accentColor = LedAmber
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HelpStepItem(
                        number = "3",
                        title = "Copie o Código da Chave",
                        description = "O Google exibirá um código que começa com as letras 'AIzaSy...'. Toque em 'Copy' (Copiar).",
                        accentColor = LedAmber
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HelpStepItem(
                        number = "4",
                        title = "Cole nas Configurações do App",
                        description = "Volte aqui no aplicativo, toque no ícone de Engrenagem (⚙️) no canto superior direito da tela (ou no botão abaixo), cole sua chave no campo 'Chave da API Gemini' e toque em 'Salvar Chave'.",
                        accentColor = LedAmber
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                    context.startActivity(intent)
                                }
                            },
                            border = BorderStroke(1.dp, LedAmber),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("help_open_aistudio_btn")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = LedAmber, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gerar Chave (Site)",
                                color = LedAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = LedAmber),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("help_open_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF1A1200), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Colar Chave no App",
                                color = Color(0xFF1A1200),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 4: CONTATO DO DESENVOLVEDOR, SUGESTÕES E RECLAMAÇÕES
        // =========================================================================
        if (selectedFilter == 0 || selectedFilter == 4) {
            item {
                HelpSectionCard(
                    stepNumber = "04",
                    title = "CONTATO, SUGESTÕES E RECLAMAÇÕES",
                    subtitle = "Fale diretamente com o desenvolvedor do Preset20 AI",
                    accentColor = LedEmerald,
                    icon = Icons.Default.Email
                ) {
                    Text(
                        text = "Tem alguma sugestão de melhoria, encontrou algum problema nos timbres ou quer enviar uma crítica/reclamação? Entre em contato diretamente com o desenvolvedor pelo e-mail oficial abaixo:",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A1914),
                        border = BorderStroke(1.dp, LedEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = LedEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "E-MAIL DO DESENVOLVEDOR",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LedEmerald,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "StudioOlidin@gmail.com",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("E-mail do Desenvolvedor", "StudioOlidin@gmail.com")
                                )
                                Toast.makeText(
                                    context,
                                    "E-mail copiado: StudioOlidin@gmail.com",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            border = BorderStroke(1.dp, LedEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("help_copy_email_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = LedEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copiar E-mail",
                                color = LedEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:StudioOlidin@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Preset20 AI - Sugestão / Contato")
                                    }
                                    context.startActivity(intent)
                                }.onFailure {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("E-mail do Desenvolvedor", "StudioOlidin@gmail.com")
                                    )
                                    Toast.makeText(
                                        context,
                                        "E-mail copiado: StudioOlidin@gmail.com",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LedEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("help_send_email_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFF041F15),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enviar E-mail",
                                color = Color(0xFF041F15),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberAssetImageBitmap(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

@Composable
private fun TutorialHardwareBannerCard(
    step: TutorialVisualStep,
    onClickZoom: () -> Unit
) {
    val bitmap = rememberAssetImageBitmap(step.assetPath)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0A101B),
        border = BorderStroke(1.dp, LedCyan.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClickZoom() }
            .testTag("tutorial_hardware_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LedCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, LedCyan)
                    ) {
                        Text(
                            text = step.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LedCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = step.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Ampliar foto da pedaleira",
                    tint = LedCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = step.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MetalBorderHighlight, RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = step.description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun TutorialStepImageCard(
    step: TutorialVisualStep,
    stepIndex: Int,
    onClickImage: () -> Unit
) {
    val bitmap = rememberAssetImageBitmap(step.assetPath)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0D1320),
        border = BorderStroke(1.dp, LedCyan.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClickImage() }
            .testTag("tutorial_step_card_$stepIndex")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(LedCyan.copy(alpha = 0.22f))
                            .border(1.dp, LedCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepIndex.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LedCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = step.stepNumber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LedCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = step.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF152033),
                    border = BorderStroke(1.dp, MetalBorderHighlight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Ampliar",
                            tint = LedCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ampliar",
                            fontSize = 10.sp,
                            color = LedCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = step.description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF070A10))
                        .border(1.dp, LedCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "${step.stepNumber}: ${step.title}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialImageZoomDialog(
    step: TutorialVisualStep,
    onDismiss: () -> Unit
) {
    val bitmap = rememberAssetImageBitmap(step.assetPath)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(16.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.stepNumber,
                            color = LedCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = step.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("close_zoom_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar visualização",
                            tint = Color.White
                        )
                    }
                }

                // Imagem centralizada
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = step.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, LedCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        )
                    }
                }

                // Descrição inferior
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.dp, LedCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = step.description,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = LedCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Fechar Visualização",
                                color = Color(0xFF06101E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecBadge(
    label: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0E1420),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = color,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = sub,
                fontSize = 9.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun FilterChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) LedEmerald.copy(alpha = 0.2f) else Color(0xFF111724),
        border = BorderStroke(1.dp, if (selected) LedEmerald else MetalBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) LedEmerald else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun HelpSectionCard(
    stepNumber: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PedalCardDark,
        border = BorderStroke(1.dp, MetalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GUIA $stepNumber",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Recolher" else "Expandir",
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun HelpStepItem(
    number: String,
    title: String,
    description: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0E131F),
        border = BorderStroke(1.dp, MetalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HelpBulletStep(
    step: String,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = step,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextPrimary,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
