using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace Preset20AIWindows
{
    public class PedalModule
    {
        public string Key;
        public string Name;
        public bool Active;
        public string Model;
        public Dictionary<string, int> Params = new Dictionary<string, int>();

        public PedalModule Clone()
        {
            var copy = new PedalModule { Key = Key, Name = Name, Active = Active, Model = Model };
            foreach (var kv in Params) copy.Params[kv.Key] = kv.Value;
            return copy;
        }
    }

    public class Mk20PresetData
    {
        public string Title;
        public string Style;
        public int Bpm = 120;
        public int MasterVolume = 65;
        public string Details;
        public Dictionary<string, PedalModule> Pedals = new Dictionary<string, PedalModule>();

        public Mk20PresetData Clone(string customTitle = null)
        {
            var copy = new Mk20PresetData
            {
                Title = string.IsNullOrWhiteSpace(customTitle) ? Title : customTitle,
                Style = Style,
                Bpm = Bpm,
                MasterVolume = MasterVolume,
                Details = Details
            };
            foreach (var kv in Pedals) copy.Pedals[kv.Key] = kv.Value.Clone();
            return copy;
        }
    }

    public class MainForm : Form
    {
        private static readonly string OFFICIAL_412_B64 =
            "VVNFUiBQUkVTRVQgMTQ1AAAAAAD/kB4APABQAAAAAAECAwQFBgcICQAAAAAAAAAAAQAAAAAAAAAAAAAAGQAyAFAAGQAyAAAAAABQAAAAAAAAAAAAMgAyAAAAAAAAAAAAAAAAAAAAAAAAAAAAMgAyADIAMgAyAAAAMgBkAAAAAAAAAAAAMgAyADIAMgAyAAAAMgBkAAAAAAAAAAAAUAAUALQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACgAyADIAAABQAAAAAAAAAAAAAAAAAAAA9AEeADIAAABQAAAAAAAAAAAAAAAAAAAAFAAeAFAAUAAoAAAAAAAAAAAAAAAAAAAAZAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACgAAAAAAZAAAAAsAAAAAAGQAAAAMAAAAAABkAAAACQAAAAAAZAAAAAkKAAAAAAgJAAAAAAQFAAAAAABHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATkdENAAAAA==";

        private static readonly string[] MODULE_KEYS = { "FX", "NS", "DS", "AMP", "CAB", "EQ", "MOD", "DELAY", "REVERB", "VOL" };

        private byte[] templateBytes;
        private List<Mk20PresetData> officialMatrices = new List<Mk20PresetData>();
        private List<Mk20PresetData> masterSongs = new List<Mk20PresetData>();
        private Mk20PresetData currentPreset;
        private string selectedModKey = "AMP";

        // UI Controls
        private TextBox txtSong;
        private TextBox txtDetails;
        private NumericUpDown numBpm;
        private NumericUpDown numMasterVol;
        private ListBox lstMatrices;
        private ListBox lstSongs;
        private FlowLayoutPanel pnlChain;
        private Label lblActiveModHeader;
        private CheckBox chkModActive;
        private TextBox txtModModel;
        private Panel pnlSliders;
        private TextBox txtNotes;
        private Label lblStatus;
        private Label lblTemplateStatus;

        // Colors
        private readonly Color BgMain = Color.FromArgb(11, 15, 23);
        private readonly Color BgPanel = Color.FromArgb(19, 25, 36);
        private readonly Color BgCard = Color.FromArgb(26, 35, 50);
        private readonly Color Emerald = Color.FromArgb(16, 185, 129);
        private readonly Color Amber = Color.FromArgb(245, 158, 11);
        private readonly Color Cyan = Color.FromArgb(6, 182, 212);
        private readonly Color TextLight = Color.FromArgb(241, 245, 249);
        private readonly Color TextMuted = Color.FromArgb(148, 163, 184);

        public MainForm()
        {
            templateBytes = Convert.FromBase64String(OFFICIAL_412_B64);
            InitDatabase();
            currentPreset = officialMatrices[0].Clone("Eli Soares - Me Ajude a Melhorar");
            BuildGui();
            RefreshAllUi();
        }

        private struct ParamPair
        {
            public string Key;
            public int Value;
            public ParamPair(string k, int v) { Key = k; Value = v; }
        }

        private static ParamPair P(string k, int v) { return new ParamPair(k, v); }

        private static PedalModule Mod(string key, bool active, string model, params ParamPair[] prms)
        {
            var m = new PedalModule { Key = key, Name = key, Active = active, Model = model };
            foreach (var kv in prms) m.Params[kv.Key] = kv.Value;
            return m;
        }

        private void InitDatabase()
        {
            officialMatrices.Add(CreateMatrix(
                "1. Solo Melodico & Gospel Lead (UK 800 + TS808)",
                "Gospel Rock / Melodic Solo", 128, 65,
                "Marshall JCM800 (86OD_JCM800) + Caixa 1960A V30 + Tube Screamer + Compressor suave e Delay de Solo.",
                Mod("FX", true, "Compress", P("Sustain", 45), P("Attack", 48), P("Level", 68), P("Blend", 55)),
                Mod("NS", true, "soft gate", P("thd", -52), P("inputDetect", 0)),
                Mod("DS", true, "MW-808", P("gain", 20), P("tone", 56), P("level", 90)),
                Mod("AMP", true, "86OD_JCM800", P("Gain", 58), P("Level", 78), P("Bass", 54), P("Middle", 66), P("Treble", 66), P("Reso", 56), P("Pres", 62), P("Bright", 68)),
                Mod("CAB", true, "33Mar60A412A", P("Level", 85), P("Low Cut", 78), P("High Cut", 7800)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 1), P("200Hz", 0), P("400Hz", -1), P("800Hz", 2), P("1.6kHz", 3), P("3.2kHz", 2)),
                Mod("MOD", false, "CHORUS", P("speed", 10), P("depth", 38), P("mix", 28)),
                Mod("DELAY", true, "Analog", P("Time", 375), P("Fb", 28), P("Mix", 24)),
                Mod("REVERB", true, "plate", P("decay", 26), P("mix", 20), P("highPass", 80), P("lowPass", 80)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            officialMatrices.Add(CreateMatrix(
                "2. Worship & Ambient Chime (JC-120 / AC30 + Shimmer)",
                "Worship / Ambient", 110, 64,
                "Timbre cristalino Worship com Klon Centaur (MW-GOLD), Chorus Estereo, Delay pontuado e Shimmer Reverb.",
                Mod("FX", true, "Compress", P("Sustain", 48), P("Attack", 44), P("Level", 68), P("Blend", 65)),
                Mod("NS", true, "soft gate", P("thd", -58), P("inputDetect", 0)),
                Mod("DS", true, "MW-GOLD", P("gain", 20), P("tone", 54), P("level", 86)),
                Mod("AMP", true, "26CL2_JC120", P("Gain", 32), P("Level", 78), P("Bass", 50), P("Middle", 60), P("Treble", 66), P("Reso", 48), P("Pres", 56), P("Bright", 72)),
                Mod("CAB", true, "20FdTwRev212A", P("Level", 84), P("Low Cut", 75), P("High Cut", 7800)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 0), P("200Hz", 0), P("400Hz", -2), P("800Hz", 1), P("1.6kHz", 2), P("3.2kHz", 2)),
                Mod("MOD", true, "CHORUS STEREO", P("speed", 10), P("depth", 38), P("mix", 30)),
                Mod("DELAY", true, "Analog", P("Time", 440), P("Fb", 34), P("Mix", 28)),
                Mod("REVERB", true, "shimmer stereo", P("decay", 48), P("mix", 28), P("tone", 65)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            officialMatrices.Add(CreateMatrix(
                "3. Studio Clean & Pop Funk (Fender Deluxe + Twin 2x12)",
                "Studio Clean / Pop / Funk", 115, 64,
                "Fender 65 Deluxe Reverb cristalino com caixa Twin 2x12, compressor transparente e agudos abertos em 8.0kHz.",
                Mod("FX", true, "Compress Pro", P("Sustain", 46), P("Attack", 42), P("Level", 68), P("Blend", 70)),
                Mod("NS", true, "soft gate", P("thd", -58), P("inputDetect", 0)),
                Mod("DS", false, "MW-GOLD", P("gain", 15), P("tone", 52), P("level", 85)),
                Mod("AMP", true, "14CL2_FenDi", P("Gain", 34), P("Level", 78), P("Bass", 52), P("Middle", 58), P("Treble", 68), P("Reso", 48), P("Pres", 58), P("Bright", 70)),
                Mod("CAB", true, "10FdTR65R212A", P("Level", 85), P("Low Cut", 75), P("High Cut", 8000)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 0), P("200Hz", -1), P("400Hz", -2), P("800Hz", 1), P("1.6kHz", 2), P("3.2kHz", 2)),
                Mod("MOD", true, "CHORUS", P("speed", 12), P("depth", 35), P("mix", 26)),
                Mod("DELAY", false, "Analog", P("Time", 350), P("Fb", 22), P("Mix", 18)),
                Mod("REVERB", true, "plate", P("decay", 22), P("mix", 18), P("highPass", 80), P("lowPass", 80)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            officialMatrices.Add(CreateMatrix(
                "4. British Crunch & Classic Rock (Marshall 2203 + V30)",
                "Classic & Hard Rock", 125, 64,
                "Crunch britanico classico com definicao de acordes, corte de lama em 400Hz e filtro anti-abelha em 7.4kHz.",
                Mod("FX", false, "Compress", P("Sustain", 42), P("Attack", 50), P("Level", 66), P("Blend", 55)),
                Mod("NS", true, "soft gate", P("thd", -52), P("inputDetect", 0)),
                Mod("DS", true, "MW-808", P("gain", 16), P("tone", 55), P("level", 88)),
                Mod("AMP", true, "51OD1_MS2203", P("Gain", 58), P("Level", 78), P("Bass", 56), P("Middle", 64), P("Treble", 62), P("Reso", 54), P("Pres", 58), P("Bright", 55)),
                Mod("CAB", true, "33Mar60A412A", P("Level", 85), P("Low Cut", 80), P("High Cut", 7400)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 2), P("200Hz", -1), P("400Hz", -3), P("800Hz", 2), P("1.6kHz", 3), P("3.2kHz", 2)),
                Mod("MOD", false, "PHASER", P("speed", 10), P("depth", 45), P("mix", 25)),
                Mod("DELAY", true, "Analog", P("Time", 360), P("Fb", 26), P("Mix", 20)),
                Mod("REVERB", true, "plate", P("decay", 24), P("mix", 18), P("highPass", 80), P("lowPass", 80)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            officialMatrices.Add(CreateMatrix(
                "5. Texas Blues & Overdrive (Fender Tweed + TS808)",
                "Texas Blues / Mayer / SRV", 98, 64,
                "Timbre quente e dinamico de Blues com resposta sensivel ao toque da palheta e medios encorpados.",
                Mod("FX", true, "Compress", P("Sustain", 40), P("Attack", 55), P("Level", 68), P("Blend", 55)),
                Mod("NS", true, "soft gate", P("thd", -55), P("inputDetect", 0)),
                Mod("DS", true, "MW-808", P("gain", 32), P("tone", 54), P("level", 84)),
                Mod("AMP", true, "14CL2_FenDi", P("Gain", 48), P("Level", 78), P("Bass", 56), P("Middle", 64), P("Treble", 62), P("Reso", 52), P("Pres", 56), P("Bright", 60)),
                Mod("CAB", true, "10FdTR65R212A", P("Level", 85), P("Low Cut", 78), P("High Cut", 7500)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 1), P("200Hz", 0), P("400Hz", -1), P("800Hz", 2), P("1.6kHz", 3), P("3.2kHz", 1)),
                Mod("MOD", false, "CHORUS", P("speed", 10), P("depth", 35), P("mix", 25)),
                Mod("DELAY", true, "Analog", P("Time", 340), P("Fb", 22), P("Mix", 18)),
                Mod("REVERB", true, "room", P("decay", 30), P("mix", 22), P("highPass", 80), P("lowPass", 80)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            officialMatrices.Add(CreateMatrix(
                "6. Modern Arena Rock (Mesa Dual Rectifier + 4x12)",
                "Modern Rock / Post-Grunge", 135, 64,
                "Parede sonora de Arena Rock com graves secos por Tube Screamer e caixa 4x12 imponente.",
                Mod("FX", false, "Compress", P("Sustain", 42), P("Attack", 48), P("Level", 68), P("Blend", 50)),
                Mod("NS", true, "soft gate", P("thd", -50), P("inputDetect", 0)),
                Mod("DS", true, "MW-808", P("gain", 12), P("tone", 58), P("level", 92)),
                Mod("AMP", true, "48CH1_ME", P("Gain", 64), P("Level", 76), P("Bass", 56), P("Middle", 56), P("Treble", 64), P("Reso", 60), P("Pres", 58), P("Bright", 50)),
                Mod("CAB", true, "46MbOS412A", P("Level", 85), P("Low Cut", 88), P("High Cut", 7200)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 2), P("200Hz", -2), P("400Hz", -3), P("800Hz", 1), P("1.6kHz", 3), P("3.2kHz", 2)),
                Mod("MOD", false, "CHORUS", P("speed", 10), P("depth", 35), P("mix", 22)),
                Mod("DELAY", true, "Analog", P("Time", 400), P("Fb", 26), P("Mix", 20)),
                Mod("REVERB", true, "plate", P("decay", 24), P("mix", 18), P("highPass", 80), P("lowPass", 78)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            officialMatrices.Add(CreateMatrix(
                "7. Epic Gilmour Lead (Hiwatt + Big Muff Sustain)",
                "Progressive Rock / Epic Solo", 66, 65,
                "Sustain liquido e encorpado estilo David Gilmour com Delay espacial sincronizado e Chorus suave.",
                Mod("FX", true, "Compress", P("Sustain", 55), P("Attack", 45), P("Level", 70), P("Blend", 60)),
                Mod("NS", true, "soft gate", P("thd", -52), P("inputDetect", 0)),
                Mod("DS", true, "MW-808", P("gain", 52), P("tone", 54), P("level", 86)),
                Mod("AMP", true, "86OD_JCM800", P("Gain", 54), P("Level", 78), P("Bass", 56), P("Middle", 64), P("Treble", 62), P("Reso", 56), P("Pres", 58), P("Bright", 55)),
                Mod("CAB", true, "33Mar60A412A", P("Level", 85), P("Low Cut", 80), P("High Cut", 7400)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 1), P("200Hz", -1), P("400Hz", -2), P("800Hz", 3), P("1.6kHz", 3), P("3.2kHz", 2)),
                Mod("MOD", true, "CHORUS", P("speed", 12), P("depth", 42), P("mix", 28)),
                Mod("DELAY", true, "Analog", P("Time", 460), P("Fb", 38), P("Mix", 34)),
                Mod("REVERB", true, "hall", P("decay", 52), P("mix", 30), P("highPass", 80), P("lowPass", 76)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            officialMatrices.Add(CreateMatrix(
                "8. High-Gain Metal & Thrash (EVH 5150 / Mesa + Gate)",
                "Heavy Metal / Thrash / Djent", 160, 65,
                "Ganho pesado com palm-mute cirurgico, TS-808 como Clean Boost e Gate Rapido sem cortar notas.",
                Mod("FX", false, "Boost", P("Gain", 25), P("Level", 75)),
                Mod("NS", true, "soft gate", P("thd", -48), P("inputDetect", 0)),
                Mod("DS", true, "MW-808", P("gain", 5), P("tone", 60), P("level", 96)),
                Mod("AMP", true, "48CH1_ME", P("Gain", 70), P("Level", 76), P("Bass", 58), P("Middle", 44), P("Treble", 66), P("Reso", 64), P("Pres", 62), P("Bright", 50)),
                Mod("CAB", true, "46MbOS412A", P("Level", 85), P("Low Cut", 92), P("High Cut", 7100)),
                Mod("EQ", true, "Guitar EQ 6", P("100Hz", 3), P("200Hz", -2), P("400Hz", -4), P("800Hz", 0), P("1.6kHz", 3), P("3.2kHz", 3)),
                Mod("MOD", false, "CHORUS", P("speed", 10), P("depth", 30), P("mix", 20)),
                Mod("DELAY", false, "Analog", P("Time", 340), P("Fb", 22), P("Mix", 18)),
                Mod("REVERB", true, "room", P("decay", 18), P("mix", 14), P("highPass", 85), P("lowPass", 75)),
                Mod("VOL", true, "Master", P("Level", 100))
            ));

            // Master Songs
            masterSongs.Add(officialMatrices[0].Clone("Eli Soares - Me Ajude a Melhorar"));
            masterSongs.Add(officialMatrices[0].Clone("Voz da Verdade - O Escudo (Solo)"));
            masterSongs.Add(officialMatrices[1].Clone("Gabriela Rocha - Lugar Secreto"));
            masterSongs.Add(officialMatrices[3].Clone("Guns N' Roses - Sweet Child O' Mine"));
            masterSongs.Add(officialMatrices[6].Clone("Pink Floyd - Comfortably Numb"));
            masterSongs.Add(officialMatrices[4].Clone("John Mayer - Slow Dancing in a Burning Room"));
            masterSongs.Add(officialMatrices[5].Clone("Breaking Benjamin - The Diary of Jane"));
            masterSongs.Add(officialMatrices[7].Clone("Metallica - Master of Puppets"));
        }

        private Mk20PresetData CreateMatrix(
            string title, string style, int bpm, int masterVol, string details,
            params PedalModule[] mods)
        {
            var p = new Mk20PresetData { Title = title, Style = style, Bpm = bpm, MasterVolume = masterVol, Details = details };
            foreach (var m in mods)
            {
                p.Pedals[m.Key] = m;
            }
            return p;
        }

        private void BuildGui()
        {
            Text = "Preset20 AI • Windows Desktop Edition (M-VAVE MK-20 / M-EFCS 412B)";
            Size = new Size(1260, 780);
            MinimumSize = new Size(1080, 680);
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = BgMain;
            ForeColor = TextLight;
            Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);

            // Top Header Panel
            var pnlTop = new Panel { Dock = DockStyle.Top, Height = 64, BackColor = BgPanel, Padding = new Padding(16, 10, 16, 10) };
            var lblTitle = new Label
            {
                Text = "🎸 PRESET20 AI • WORKSTATION WINDOWS (M-VAVE MK-20 / M-EFCS)",
                Font = new Font("Segoe UI", 13f, FontStyle.Bold),
                ForeColor = Emerald,
                AutoSize = true,
                Location = new Point(16, 10)
            };
            lblTemplateStatus = new Label
            {
                Text = "Molde Hardware Oficial: 412 Bytes Ativo (Assinatura FF 90 1E 00 ... NGD4)",
                Font = new Font("Consolas", 9f, FontStyle.Regular),
                ForeColor = Cyan,
                AutoSize = true,
                Location = new Point(18, 36)
            };

            var btnExportMk20 = CreateStyledButton("💾 EXPORTAR .MK20 (412B P/ M-EFCS)", Emerald, Color.Black, 270, 40);
            btnExportMk20.Location = new Point(950, 12);
            btnExportMk20.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnExportMk20.Click += (s, e) => ExportMk20File();

            var btnLoadTemplate = CreateStyledButton("🧬 Clonar Molde .MK20", BgCard, Cyan, 175, 40);
            btnLoadTemplate.Location = new Point(765, 12);
            btnLoadTemplate.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnLoadTemplate.Click += (s, e) => LoadCustomTemplate();

            var btnStudioCal = CreateStyledButton("🎚️ Calibrar Estúdio", BgCard, Amber, 160, 40);
            btnStudioCal.Location = new Point(595, 12);
            btnStudioCal.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnStudioCal.Click += (s, e) => ApplyStudioCalibration();

            pnlTop.Controls.AddRange(new Control[] { lblTitle, lblTemplateStatus, btnStudioCal, btnLoadTemplate, btnExportMk20 });

            // Bottom Status Bar
            var pnlBottom = new Panel { Dock = DockStyle.Bottom, Height = 30, BackColor = Color.FromArgb(7, 10, 15) };
            lblStatus = new Label
            {
                Text = "✅ Pronto! Escolha uma música ou Matriz Oficial e clique em 'Exportar .MK20 (412B)' para abrir no M-EFCS do Windows.",
                ForeColor = Emerald,
                Font = new Font("Consolas", 9f),
                AutoSize = true,
                Location = new Point(14, 7)
            };
            pnlBottom.Controls.Add(lblStatus);

            // Main Split Container (Left Generator/Matrices | Right Signal Chain & Knobs)
            var splitMain = new SplitContainer
            {
                Dock = DockStyle.Fill,
                SplitterDistance = 410,
                BackColor = BgMain
            };

            // LEFT PANEL
            var pnlLeft = new Panel { Dock = DockStyle.Fill, BackColor = BgPanel, Padding = new Padding(14), AutoScroll = true };
            int y = 12;

            pnlLeft.Controls.Add(MakeSectionLabel("⚡ GERADOR DE TIMBRE IA (MÚSICA OU ESTILO)", Emerald, 14, y));
            y += 24;

            pnlLeft.Controls.Add(MakeFieldLabel("Nome da Música, Artista ou Estilo:", 14, y));
            y += 18;
            txtSong = new TextBox
            {
                Text = "Eli Soares - Me Ajude a Melhorar",
                Location = new Point(14, y),
                Width = 370,
                BackColor = BgMain,
                ForeColor = TextLight,
                BorderStyle = BorderStyle.FixedSingle
            };
            pnlLeft.Controls.Add(txtSong);
            y += 30;

            pnlLeft.Controls.Add(MakeFieldLabel("Detalhes (Solo, Base, Guitarra Strato / Les Paul):", 14, y));
            y += 18;
            txtDetails = new TextBox
            {
                Text = "Solo melódico encorpado sem abelha digital",
                Location = new Point(14, y),
                Width = 370,
                BackColor = BgMain,
                ForeColor = TextLight,
                BorderStyle = BorderStyle.FixedSingle
            };
            pnlLeft.Controls.Add(txtDetails);
            y += 32;

            var btnGenerate = CreateStyledButton("✨ GERAR TIMBRE MK-20 AGORA", Emerald, Color.Black, 370, 38);
            btnGenerate.Location = new Point(14, y);
            btnGenerate.Click += (s, e) => GenerateFromInput();
            pnlLeft.Controls.Add(btnGenerate);
            y += 50;

            pnlLeft.Controls.Add(MakeSectionLabel("🎛️ 8 MATRIZES OFICIAIS DE HARDWARE MK-20 (1 CLIQUE)", Amber, 14, y));
            y += 22;
            lstMatrices = new ListBox
            {
                Location = new Point(14, y),
                Size = new Size(370, 175),
                BackColor = BgCard,
                ForeColor = TextLight,
                BorderStyle = BorderStyle.FixedSingle,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold)
            };
            foreach (var m in officialMatrices) lstMatrices.Items.Add(m.Title);
            lstMatrices.SelectedIndex = 0;
            lstMatrices.SelectedIndexChanged += (s, e) =>
            {
                if (lstMatrices.SelectedIndex >= 0)
                {
                    var chosen = officialMatrices[lstMatrices.SelectedIndex];
                    currentPreset = chosen.Clone(txtSong.Text.Trim());
                    RefreshAllUi();
                    lblStatus.Text = "🎛️ Matriz Oficial aplicada: " + chosen.Title;
                }
            };
            pnlLeft.Controls.Add(lstMatrices);
            y += 185;

            pnlLeft.Controls.Add(MakeSectionLabel("🎵 TIMBRES LENDÁRIOS PRONTOS (MASTER SONGS)", Cyan, 14, y));
            y += 22;
            lstSongs = new ListBox
            {
                Location = new Point(14, y),
                Size = new Size(370, 165),
                BackColor = BgCard,
                ForeColor = TextLight,
                BorderStyle = BorderStyle.FixedSingle
            };
            foreach (var s in masterSongs) lstSongs.Items.Add(s.Title);
            lstSongs.SelectedIndexChanged += (s, e) =>
            {
                if (lstSongs.SelectedIndex >= 0)
                {
                    var songPreset = masterSongs[lstSongs.SelectedIndex];
                    txtSong.Text = songPreset.Title;
                    currentPreset = songPreset.Clone();
                    RefreshAllUi();
                    lblStatus.Text = "🎵 Timbre lendário carregado: " + songPreset.Title;
                }
            };
            pnlLeft.Controls.Add(lstSongs);

            splitMain.Panel1.Controls.Add(pnlLeft);

            // RIGHT PANEL (Signal Chain + Active Module Knobs + Roadie AI Quick Buttons)
            var pnlRight = new Panel { Dock = DockStyle.Fill, BackColor = BgPanel, Padding = new Padding(14), AutoScroll = true };

            var lblChainTitle = MakeSectionLabel("🔗 CADEIA DE SINAL M-EFCS (CLIQUE NO BLOCO PARA EDITAR)", Cyan, 14, 10);
            pnlRight.Controls.Add(lblChainTitle);

            // BPM & Master Volume Controls
            var lblBpm = MakeFieldLabel("BPM:", 500, 10);
            numBpm = new NumericUpDown
            {
                Minimum = 40, Maximum = 240, Value = 120,
                Location = new Point(540, 8), Width = 65,
                BackColor = BgMain, ForeColor = Emerald
            };
            numBpm.ValueChanged += (s, e) => currentPreset.Bpm = (int)numBpm.Value;

            var lblVol = MakeFieldLabel("MASTER VOL:", 620, 10);
            numMasterVol = new NumericUpDown
            {
                Minimum = 0, Maximum = 100, Value = 65,
                Location = new Point(710, 8), Width = 60,
                BackColor = BgMain, ForeColor = Amber
            };
            numMasterVol.ValueChanged += (s, e) => currentPreset.MasterVolume = (int)numMasterVol.Value;

            pnlRight.Controls.AddRange(new Control[] { lblBpm, numBpm, lblVol, numMasterVol });

            pnlChain = new FlowLayoutPanel
            {
                Location = new Point(14, 36),
                Size = new Size(780, 68),
                BackColor = BgMain,
                Padding = new Padding(4),
                Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right
            };
            pnlRight.Controls.Add(pnlChain);

            // Active Module Header
            lblActiveModHeader = new Label
            {
                Text = "BLOCO AMP",
                Font = new Font("Segoe UI", 12f, FontStyle.Bold),
                ForeColor = Amber,
                AutoSize = true,
                Location = new Point(14, 114)
            };
            chkModActive = new CheckBox
            {
                Text = "Bloco Ligado na Cadeia",
                ForeColor = Emerald,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                AutoSize = true,
                Location = new Point(280, 116)
            };
            chkModActive.CheckedChanged += (s, e) =>
            {
                if (currentPreset.Pedals.ContainsKey(selectedModKey))
                {
                    currentPreset.Pedals[selectedModKey].Active = chkModActive.Checked;
                    RenderSignalChain();
                }
            };

            var lblModModel = MakeFieldLabel("Modelo M-EFCS:", 480, 117);
            txtModModel = new TextBox
            {
                Location = new Point(585, 114),
                Width = 190,
                BackColor = BgMain,
                ForeColor = Cyan,
                BorderStyle = BorderStyle.FixedSingle
            };
            txtModModel.TextChanged += (s, e) =>
            {
                if (currentPreset.Pedals.ContainsKey(selectedModKey))
                {
                    currentPreset.Pedals[selectedModKey].Model = txtModModel.Text.Trim();
                    RenderSignalChain();
                }
            };

            pnlRight.Controls.AddRange(new Control[] { lblActiveModHeader, chkModActive, lblModModel, txtModModel });

            // Sliders Container
            pnlSliders = new Panel
            {
                Location = new Point(14, 148),
                Size = new Size(780, 340),
                BackColor = BgMain,
                AutoScroll = true,
                Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right
            };
            pnlRight.Controls.Add(pnlSliders);

            // Roadie Quick Adjustments
            var lblRoadie = MakeSectionLabel("🤖 AJUSTE RÁPIDO ROADIE IA:", Emerald, 14, 500);
            pnlRight.Controls.Add(lblRoadie);

            var btnAntiBee = CreateStyledButton("✂️ Cortar Agudo Ardido (Anti-Abelha)", BgCard, TextLight, 235, 34);
            btnAntiBee.Location = new Point(14, 524);
            btnAntiBee.Click += (s, e) => RoadieQuickAdjust("antibeelha");

            var btnSoloBoost = CreateStyledButton("🔥 Empurrar Solo (+Sustentação)", BgCard, Amber, 220, 34);
            btnSoloBoost.Location = new Point(256, 524);
            btnSoloBoost.Click += (s, e) => RoadieQuickAdjust("solo");

            var btnStrato = CreateStyledButton("🎸 Calibrar p/ Captador Strato", BgCard, Cyan, 215, 34);
            btnStrato.Location = new Point(482, 524);
            btnStrato.Click += (s, e) => RoadieQuickAdjust("strato");

            pnlRight.Controls.AddRange(new Control[] { btnAntiBee, btnSoloBoost, btnStrato });

            txtNotes = new TextBox
            {
                Multiline = true,
                ReadOnly = true,
                Location = new Point(14, 568),
                Size = new Size(780, 65),
                BackColor = BgCard,
                ForeColor = TextMuted,
                BorderStyle = BorderStyle.FixedSingle,
                Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right
            };
            pnlRight.Controls.Add(txtNotes);

            splitMain.Panel2.Controls.Add(pnlRight);

            Controls.Add(splitMain);
            Controls.Add(pnlBottom);
            Controls.Add(pnlTop);
        }

        private Label MakeSectionLabel(string text, Color color, int x, int y)
        {
            return new Label
            {
                Text = text,
                ForeColor = color,
                Font = new Font("Consolas", 9.5f, FontStyle.Bold),
                AutoSize = true,
                Location = new Point(x, y)
            };
        }

        private Label MakeFieldLabel(string text, int x, int y)
        {
            return new Label
            {
                Text = text,
                ForeColor = TextMuted,
                Font = new Font("Segoe UI", 9f, FontStyle.Regular),
                AutoSize = true,
                Location = new Point(x, y)
            };
        }

        private Button CreateStyledButton(string text, Color bg, Color fg, int w, int h)
        {
            var btn = new Button
            {
                Text = text,
                Size = new Size(w, h),
                BackColor = bg,
                ForeColor = fg,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9f, FontStyle.Bold),
                Cursor = Cursors.Hand
            };
            btn.FlatAppearance.BorderColor = Color.FromArgb(55, 71, 97);
            return btn;
        }

        private void RefreshAllUi()
        {
            numBpm.Value = Math.Max(40, Math.Min(240, currentPreset.Bpm));
            numMasterVol.Value = Math.Max(0, Math.Min(100, currentPreset.MasterVolume));
            txtNotes.Text = "NOTAS DE ENGENHARIA ACÚSTICA: " + currentPreset.Details;
            RenderSignalChain();
            RenderSlidersForSelectedModule();
        }

        private void RenderSignalChain()
        {
            pnlChain.Controls.Clear();
            foreach (var key in MODULE_KEYS)
            {
                var mod = currentPreset.Pedals.ContainsKey(key) ? currentPreset.Pedals[key] : null;
                bool active = mod != null && mod.Active;
                bool isSel = key == selectedModKey;

                var btn = new Button
                {
                    Text = key + "\n" + (mod != null ? mod.Model : "-"),
                    Size = new Size(72, 54),
                    BackColor = isSel ? Color.FromArgb(36, 52, 74) : BgCard,
                    ForeColor = active ? Emerald : TextMuted,
                    FlatStyle = FlatStyle.Flat,
                    Font = new Font("Consolas", 8f, FontStyle.Bold),
                    Cursor = Cursors.Hand
                };
                btn.FlatAppearance.BorderColor = isSel ? Amber : (active ? Emerald : Color.FromArgb(45, 55, 72));
                btn.FlatAppearance.BorderSize = isSel ? 2 : 1;
                string capturedKey = key;
                btn.Click += (s, e) =>
                {
                    selectedModKey = capturedKey;
                    RenderSignalChain();
                    RenderSlidersForSelectedModule();
                };
                pnlChain.Controls.Add(btn);
            }
        }

        private void RenderSlidersForSelectedModule()
        {
            pnlSliders.Controls.Clear();
            if (!currentPreset.Pedals.ContainsKey(selectedModKey)) return;
            var mod = currentPreset.Pedals[selectedModKey];

            lblActiveModHeader.Text = "BLOCO " + selectedModKey + " • " + mod.Model;
            chkModActive.Checked = mod.Active;
            txtModModel.Text = mod.Model;

            int y = 10;
            var keys = new List<string>(mod.Params.Keys);
            foreach (var pKey in keys)
            {
                int val = mod.Params[pKey];
                int min = 0, max = 100;
                if (pKey == "Low Cut") { min = 20; max = 300; }
                else if (pKey == "High Cut") { min = 5000; max = 12000; }
                else if (pKey == "Time") { min = 100; max = 2000; }
                else if (pKey == "thd") { min = -80; max = -30; }
                else if (selectedModKey == "EQ") { min = -12; max = 12; }

                var lblName = new Label
                {
                    Text = pKey.ToUpper() + ":",
                    ForeColor = TextLight,
                    Font = new Font("Consolas", 9.5f, FontStyle.Bold),
                    Location = new Point(14, y + 6),
                    Size = new Size(130, 22)
                };

                var lblVal = new Label
                {
                    Text = val.ToString(),
                    ForeColor = Emerald,
                    Font = new Font("Consolas", 10f, FontStyle.Bold),
                    Location = new Point(660, y + 6),
                    Size = new Size(80, 22)
                };

                var track = new TrackBar
                {
                    Minimum = min,
                    Maximum = max,
                    Value = Math.Max(min, Math.Min(max, val)),
                    TickStyle = TickStyle.None,
                    Location = new Point(150, y),
                    Size = new Size(500, 32)
                };

                string capturedParam = pKey;
                Label capturedValLabel = lblVal;
                track.Scroll += (s, e) =>
                {
                    mod.Params[capturedParam] = track.Value;
                    capturedValLabel.Text = track.Value.ToString();
                };

                pnlSliders.Controls.AddRange(new Control[] { lblName, track, lblVal });
                y += 38;
            }
        }

        private void GenerateFromInput()
        {
            string q = (txtSong.Text + " " + txtDetails.Text).ToLowerInvariant();
            int idx = 0;
            if (q.Contains("metal") || q.Contains("metallica") || q.Contains("megadeth") || q.Contains("slipknot") || q.Contains("5150")) idx = 7;
            else if (q.Contains("gilmour") || q.Contains("pink floyd") || q.Contains("numb") || q.Contains("shine")) idx = 6;
            else if (q.Contains("breaking") || q.Contains("creed") || q.Contains("alter bridge") || q.Contains("arena")) idx = 5;
            else if (q.Contains("blues") || q.Contains("mayer") || q.Contains("srv") || q.Contains("bb king")) idx = 4;
            else if (q.Contains("guns") || q.Contains("slash") || q.Contains("ac/dc") || q.Contains("crunch")) idx = 3;
            else if (q.Contains("clean") || q.Contains("funk") || q.Contains("pop") || q.Contains("djavan")) idx = 2;
            else if (q.Contains("worship") || q.Contains("hillsong") || q.Contains("gabriela") || q.Contains("lugar secreto") || q.Contains("shimmer")) idx = 1;

            lstMatrices.SelectedIndex = idx;
            currentPreset = officialMatrices[idx].Clone(txtSong.Text.Trim());

            if (q.Contains("solo") || q.Contains("lead"))
            {
                currentPreset.Pedals["DS"].Active = true;
                currentPreset.Pedals["DELAY"].Active = true;
                currentPreset.Pedals["EQ"].Params["1.6kHz"] = 3;
            }
            RefreshAllUi();
            lblStatus.Text = "✨ Timbre '" + currentPreset.Title + "' gerado com sucesso na Matriz Oficial! Pronto para exportar .MK20 (412B).";
        }

        private void ApplyStudioCalibration()
        {
            if (currentPreset.Pedals.ContainsKey("CAB"))
            {
                currentPreset.Pedals["CAB"].Active = true;
                currentPreset.Pedals["CAB"].Params["Low Cut"] = 82;
                currentPreset.Pedals["CAB"].Params["High Cut"] = 7400;
            }
            if (currentPreset.Pedals.ContainsKey("EQ"))
            {
                currentPreset.Pedals["EQ"].Active = true;
                currentPreset.Pedals["EQ"].Params["400Hz"] = -3;
                currentPreset.Pedals["EQ"].Params["1.6kHz"] = 3;
            }
            RefreshAllUi();
            lblStatus.Text = "🎚️ Calibração de Estúdio aplicada: Low Cut 82Hz, High Cut 7.4kHz (Anti-Abelha) e limpeza em 400Hz!";
        }

        private void RoadieQuickAdjust(string mode)
        {
            if (mode == "antibeelha")
            {
                currentPreset.Pedals["CAB"].Params["High Cut"] = 7000;
                currentPreset.Pedals["AMP"].Params["Treble"] = Math.Max(45, currentPreset.Pedals["AMP"].Params["Treble"] - 6);
                lblStatus.Text = "✂️ Roadie IA: High Cut reduzido para 7.0kHz e Treble suavizado (Zero Abelha)!";
            }
            else if (mode == "solo")
            {
                currentPreset.Pedals["DS"].Active = true;
                currentPreset.Pedals["DELAY"].Active = true;
                currentPreset.Pedals["EQ"].Params["800Hz"] = 3;
                currentPreset.Pedals["EQ"].Params["1.6kHz"] = 4;
                lblStatus.Text = "🔥 Roadie IA: Drive DS ativado, médios de solo empurrados e Delay aberto!";
            }
            else if (mode == "strato")
            {
                currentPreset.Pedals["AMP"].Params["Bass"] = Math.Min(68, currentPreset.Pedals["AMP"].Params["Bass"] + 6);
                currentPreset.Pedals["AMP"].Params["Middle"] = Math.Min(72, currentPreset.Pedals["AMP"].Params["Middle"] + 5);
                currentPreset.Pedals["CAB"].Params["High Cut"] = 7200;
                lblStatus.Text = "🎸 Roadie IA: Timbre encorpado para captadores Single-Coil (Stratocaster)!";
            }
            RefreshAllUi();
        }

        private void LoadCustomTemplate()
        {
            using (var ofd = new OpenFileDialog())
            {
                ofd.Title = "Selecione um arquivo .mk20 exportado do seu M-EFCS";
                ofd.Filter = "M-VAVE MK-20 Preset (*.mk20)|*.mk20|Todos os arquivos (*.*)|*.*";
                if (ofd.ShowDialog() == DialogResult.OK)
                {
                    var bytes = File.ReadAllBytes(ofd.FileName);
                    if (bytes.Length == 412)
                    {
                        templateBytes = bytes;
                        lblTemplateStatus.Text = "Molde Clonado Ativo: " + Path.GetFileName(ofd.FileName) + " (412 bytes)";
                        lblStatus.Text = "🧬 Molde '" + Path.GetFileName(ofd.FileName) + "' carregado com sucesso!";
                    }
                    else
                    {
                        MessageBox.Show("O arquivo selecionado possui " + bytes.Length + " bytes. O formato oficial da MK-20 exige exatos 412 bytes.", "Aviso de Tamanho", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                    }
                }
            }
        }

        private byte[] BuildAuthentic412Binary()
        {
            byte[] result = (byte[])templateBytes.Clone();

            // 1. Nome nos bytes 0..14 (bytes 15..19 = 0x00 para compatibilidade M-EFCS)
            string cleanTitle = string.IsNullOrWhiteSpace(currentPreset.Title) ? "MK20 PRESET" : currentPreset.Title;
            byte[] ascii = Encoding.ASCII.GetBytes(cleanTitle);
            for (int i = 0; i < 20; i++)
            {
                result[i] = (i < 15 && i < ascii.Length) ? ascii[i] : (byte)0;
            }

            // 2. Assinatura Hardware MK-20 (0x14..0x17 = FF 90 1E 00)
            result[0x14] = 0xFF; result[0x15] = 0x90; result[0x16] = 0x1E; result[0x17] = 0x00;

            // 3. Master Volume (0x18) e BPM (0x1A)
            WriteInt16LE(result, 0x18, (short)Math.Max(0, Math.Min(100, currentPreset.MasterVolume)));
            WriteInt16LE(result, 0x1A, (short)Math.Max(40, Math.Min(240, currentPreset.Bpm)));

            // 4. Ordem dos 10 módulos (0x1E..0x27)
            for (int i = 0; i < 10; i++) result[0x1E + i] = (byte)i;

            // 5. Flags Ativo/Bypass (0x28..0x31)
            for (int i = 0; i < MODULE_KEYS.Length; i++)
            {
                string k = MODULE_KEYS[i];
                bool act = currentPreset.Pedals.ContainsKey(k) && currentPreset.Pedals[k].Active;
                result[0x28 + i] = act ? (byte)1 : (byte)0;
            }

            // 6. AMP (0x84)
            var amp = currentPreset.Pedals["AMP"].Params;
            string[] ampKeys = { "Gain", "Level", "Bass", "Middle", "Treble", "Reso", "Pres", "Bright" };
            for (int i = 0; i < ampKeys.Length; i++)
            {
                int v = amp.ContainsKey(ampKeys[i]) ? amp[ampKeys[i]] : 50;
                WriteInt16LE(result, 0x84 + i * 2, (short)Math.Max(0, Math.Min(100, v)));
            }

            // 7. CAB (0x9C)
            var cab = currentPreset.Pedals["CAB"].Params;
            WriteInt16LE(result, 0x9C, (short)(cab.ContainsKey("Level") ? cab["Level"] : 85));
            WriteInt16LE(result, 0x9E, (short)(cab.ContainsKey("Low Cut") ? cab["Low Cut"] : 80));
            int hcRaw = cab.ContainsKey("High Cut") ? cab["High Cut"] : 7500;
            int hcScaled = hcRaw > 200 ? hcRaw / 100 : hcRaw;
            WriteInt16LE(result, 0xA0, (short)Math.Max(50, Math.Min(180, hcScaled)));

            // 8. EQ (0xB4)
            var eq = currentPreset.Pedals["EQ"].Params;
            string[] eqKeys = { "100Hz", "200Hz", "400Hz", "800Hz", "1.6kHz", "3.2kHz" };
            for (int i = 0; i < eqKeys.Length; i++)
            {
                int v = eq.ContainsKey(eqKeys[i]) ? eq[eqKeys[i]] : 0;
                WriteInt16LE(result, 0xB4 + i * 2, (short)Math.Max(-12, Math.Min(12, v)));
            }

            // 9. DELAY (0xE4)
            var dly = currentPreset.Pedals["DELAY"].Params;
            WriteInt16LE(result, 0xE4, (short)(dly.ContainsKey("Time") ? dly["Time"] : 380));
            WriteInt16LE(result, 0xE6, (short)(dly.ContainsKey("Fb") ? dly["Fb"] : 28));
            WriteInt16LE(result, 0xE8, (short)(dly.ContainsKey("Mix") ? dly["Mix"] : 22));

            // 10. REVERB (0xFC)
            var rev = currentPreset.Pedals["REVERB"].Params;
            WriteInt16LE(result, 0xFC, (short)(rev.ContainsKey("decay") ? rev["decay"] : 26));
            WriteInt16LE(result, 0xFE, (short)(rev.ContainsKey("mix") ? rev["mix"] : 20));

            // 11. Trailer NGD4 (405..408)
            result[405] = 0x4E; result[406] = 0x47; result[407] = 0x44; result[408] = 0x34;
            return result;
        }

        private void WriteInt16LE(byte[] buf, int offset, short value)
        {
            buf[offset] = (byte)(value & 0xFF);
            buf[offset + 1] = (byte)((value >> 8) & 0xFF);
        }

        private void ExportMk20File()
        {
            using (var sfd = new SaveFileDialog())
            {
                string safeName = "preset_mk20";
                if (!string.IsNullOrWhiteSpace(txtSong.Text))
                {
                    foreach (char c in txtSong.Text.Trim())
                    {
                        if (char.IsLetterOrDigit(c) || c == '_' || c == '-') safeName += c;
                        else if (c == ' ') safeName += '_';
                    }
                }
                sfd.Title = "Salvar Preset Oficial .MK20 (412 Bytes) para o M-EFCS Windows";
                sfd.Filter = "M-VAVE MK-20 Preset (*.mk20)|*.mk20";
                sfd.FileName = safeName + ".mk20";
                if (sfd.ShowDialog() == DialogResult.OK)
                {
                    byte[] data = BuildAuthentic412Binary();
                    File.WriteAllBytes(sfd.FileName, data);
                    lblStatus.Text = "💾 Arquivo '" + Path.GetFileName(sfd.FileName) + "' (412 bytes) salvo com sucesso! Importe no M-EFCS.";
                    MessageBox.Show(
                        "Preset exportado com sucesso!\n\nArquivo: " + sfd.FileName + "\nTamanho exato: " + data.Length + " bytes (Padrão Oficial M-EFCS).",
                        "Preset20 AI • Exportação Concluída",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Information
                    );
                }
            }
        }

        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }
}
