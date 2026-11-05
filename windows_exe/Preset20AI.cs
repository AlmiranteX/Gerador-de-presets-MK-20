using System;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;
using System.Diagnostics;
using System.Reflection;
using System.Windows.Forms;
using System.Drawing;

namespace Preset20AI
{
    public class Program
    {
        private static HttpListener listener;
        private static string appDataDir;
        private static string savedFilePath;
        private static string settingsFilePath;
        private static int port = 18920;

        [STAThread]
        public static void Main(string[] args)
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            appDataDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Preset20AI_MK20");
            if (!Directory.Exists(appDataDir)) Directory.CreateDirectory(appDataDir);
            savedFilePath = Path.Combine(appDataDir, "saved_presets_mk20.json");
            settingsFilePath = Path.Combine(appDataDir, "settings_mk20.json");

            StartLocalServer();
            OpenAppWindow("http://127.0.0.1:" + port + "/");

            Application.Run(new TrayAppContext("http://127.0.0.1:" + port + "/"));
        }

        private static void StartLocalServer()
        {
            Random rnd = new Random();
            for (int attempt = 0; attempt < 15; attempt++)
            {
                try
                {
                    listener = new HttpListener();
                    listener.Prefixes.Add("http://127.0.0.1:" + port + "/");
                    listener.Start();
                    break;
                }
                catch
                {
                    port = 18921 + rnd.Next(1, 5000);
                }
            }

            Thread serverThread = new Thread(() =>
            {
                while (listener != null && listener.IsListening)
                {
                    try
                    {
                        HttpListenerContext ctx = listener.GetContext();
                        ThreadPool.QueueUserWorkItem(_ => HandleRequest(ctx));
                    }
                    catch { }
                }
            });
            serverThread.IsBackground = true;
            serverThread.Start();
        }

        private static byte[] LoadTutorialAsset(string fileName)
        {
            try
            {
                Assembly asm = Assembly.GetExecutingAssembly();
                using (Stream s = asm.GetManifestResourceStream(fileName))
                {
                    if (s != null)
                    {
                        using (MemoryStream ms = new MemoryStream())
                        {
                            s.CopyTo(ms);
                            return ms.ToArray();
                        }
                    }
                }
            }
            catch { }

            try
            {
                string[] searchPaths = new string[] {
                    Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "tutorial", fileName),
                    Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "app", "src", "main", "assets", "tutorial", fileName),
                    Path.Combine("app", "src", "main", "assets", "tutorial", fileName)
                };
                foreach (string p in searchPaths)
                {
                    if (File.Exists(p)) return File.ReadAllBytes(p);
                }
            }
            catch { }
            return null;
        }

        private static void HandleRequest(HttpListenerContext ctx)
        {
            try
            {
                string path = ctx.Request.Url.AbsolutePath;
                string method = ctx.Request.HttpMethod;

                if (path.StartsWith("/tutorial/"))
                {
                    string fileName = Path.GetFileName(path);
                    byte[] imgBytes = LoadTutorialAsset(fileName);
                    if (imgBytes != null)
                    {
                        ctx.Response.ContentType = fileName.EndsWith(".jpg") ? "image/jpeg" : "image/png";
                        ctx.Response.ContentLength64 = imgBytes.Length;
                        ctx.Response.OutputStream.Write(imgBytes, 0, imgBytes.Length);
                        ctx.Response.OutputStream.Close();
                        return;
                    }
                }

                if (path == "/api/saved" && method == "GET")
                {
                    string json = File.Exists(savedFilePath) ? File.ReadAllText(savedFilePath, Encoding.UTF8) : "[]";
                    SendJson(ctx, json);
                    return;
                }
                if (path == "/api/saved" && method == "POST")
                {
                    using (StreamReader sr = new StreamReader(ctx.Request.InputStream, Encoding.UTF8))
                    {
                        string body = sr.ReadToEnd();
                        File.WriteAllText(savedFilePath, body, Encoding.UTF8);
                    }
                    SendJson(ctx, "{\"ok\":true}");
                    return;
                }
                if (path == "/api/settings" && method == "GET")
                {
                    string json = File.Exists(settingsFilePath) ? File.ReadAllText(settingsFilePath, Encoding.UTF8) : "{}";
                    SendJson(ctx, json);
                    return;
                }
                if (path == "/api/settings" && method == "POST")
                {
                    using (StreamReader sr = new StreamReader(ctx.Request.InputStream, Encoding.UTF8))
                    {
                        string body = sr.ReadToEnd();
                        File.WriteAllText(settingsFilePath, body, Encoding.UTF8);
                    }
                    SendJson(ctx, "{\"ok\":true}");
                    return;
                }

                byte[] htmlBytes = Encoding.UTF8.GetBytes(AppHtml.GetHtml());
                ctx.Response.ContentType = "text/html; charset=utf-8";
                ctx.Response.ContentLength64 = htmlBytes.Length;
                ctx.Response.OutputStream.Write(htmlBytes, 0, htmlBytes.Length);
                ctx.Response.OutputStream.Close();
            }
            catch { }
        }

        private static void SendJson(HttpListenerContext ctx, string json)
        {
            byte[] buf = Encoding.UTF8.GetBytes(json);
            ctx.Response.ContentType = "application/json; charset=utf-8";
            ctx.Response.ContentLength64 = buf.Length;
            ctx.Response.OutputStream.Write(buf, 0, buf.Length);
            ctx.Response.OutputStream.Close();
        }

        public static void OpenAppWindow(string url)
        {
            string[] edgePaths = new string[]
            {
                @"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
                @"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
                @"C:\Program Files\Google\Chrome\Application\chrome.exe",
                @"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
            };

            foreach (string browserPath in edgePaths)
            {
                if (File.Exists(browserPath))
                {
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = browserPath,
                        Arguments = "--app=\"" + url + "\" --window-size=490,940",
                        UseShellExecute = true
                    });
                    return;
                }
            }
            Process.Start(new ProcessStartInfo { FileName = url, UseShellExecute = true });
        }
    }

    public class TrayAppContext : ApplicationContext
    {
        private NotifyIcon trayIcon;
        private string appUrl;

        public TrayAppContext(string url)
        {
            appUrl = url;
            ContextMenu menu = new ContextMenu(new MenuItem[]
            {
                new MenuItem("Abrir Preset20 AI (Mobile Fiel)", (s, e) => Program.OpenAppWindow(appUrl)),
                new MenuItem("-"),
                new MenuItem("Sair do Preset20 AI", (s, e) => { trayIcon.Visible = false; Environment.Exit(0); })
            });

            trayIcon = new NotifyIcon()
            {
                Icon = SystemIcons.Application,
                ContextMenu = menu,
                Text = "Preset20 AI - M-VAVE MK-20 (Fiel ao Android)",
                Visible = true
            };
            trayIcon.DoubleClick += (s, e) => Program.OpenAppWindow(appUrl);
        }
    }

    public static class AppHtml
    {
        public static string GetHtml()
        {
            return @"<!DOCTYPE html>
<html lang=""pt-BR"">
<head>
<meta charset=""UTF-8"">
<meta name=""viewport"" content=""width=device-width, initial-scale=1.0"">
<title>Preset20 AI • M-VAVE MK-20 (Versão Fiel ao Android)</title>
<style>
:root {
  --bg: #0D1117;
  --surface: #161B22;
  --surface-variant: #1F2631;
  --border: #2D3545;
  --text: #F0F6FC;
  --text-muted: #8B949E;
  --primary: #10B981;
  --primary-dark: #059669;
  --amber: #F59E0B;
  --amber-bg: #2B1F06;
  --danger: #EF4444;
  --lcd-bg: #11181F;
  --lcd-bar: #1E2A38;
  --lcd-cyan: #5CE1E6;
  --lcd-orange: #E55A2B;
}
* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; }
body { background: var(--bg); color: var(--text); display: flex; flex-direction: column; height: 100vh; overflow: hidden; user-select: none; }

/* TOP APP BAR */
.top-bar {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.brand-row { display: flex; align-items: center; gap: 10px; }
.brand-logo {
  width: 34px; height: 34px; border-radius: 9px;
  background: rgba(16, 185, 129, 0.16);
  border: 1px solid rgba(16, 185, 129, 0.35);
  display: flex; align-items: center; justify-content: center;
  color: var(--primary); font-size: 18px; font-weight: 900;
}
.brand-title { font-size: 16px; font-weight: 800; display: flex; align-items: center; gap: 6px; }
.badge-mk20 {
  font-size: 10px; font-weight: 800; padding: 2px 6px; border-radius: 5px;
  background: rgba(16, 185, 129, 0.16); color: var(--primary);
}
.brand-sub { font-size: 11px; color: var(--text-muted); margin-top: 1px; }
.top-actions { display: flex; align-items: center; gap: 5px; }
.btn-pro {
  padding: 5px 10px; border-radius: 8px; font-size: 11px; font-weight: 900; cursor: pointer;
  border: 1px solid var(--amber); background: var(--amber-bg); color: var(--amber);
  display: flex; align-items: center; gap: 4px;
}
.btn-pro.active { background: rgba(16, 185, 129, 0.18); border-color: var(--primary); color: var(--primary); }
.icon-btn {
  width: 34px; height: 34px; border-radius: 8px; border: 1px solid transparent;
  background: transparent; color: var(--text); cursor: pointer;
  display: flex; align-items: center; justify-content: center; font-size: 16px;
}
.icon-btn:hover { background: var(--surface-variant); border-color: var(--border); }

/* MAIN CONTENT SCROLL */
.main-scroll { flex: 1; overflow-y: auto; padding: 12px 14px 28px 14px; }
.container { max-width: 520px; margin: 0 auto; display: flex; flex-direction: column; gap: 12px; }

/* CARDS */
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 14px;
}
.card-title-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.card-title { font-size: 14px; font-weight: 800; display: flex; align-items: center; gap: 6px; }

/* SEGMENTED BUTTONS */
.seg-row {
  display: flex; gap: 6px; background: var(--bg); padding: 4px;
  border-radius: 11px; border: 1px solid var(--border); margin-bottom: 10px;
}
.seg-btn {
  flex: 1; padding: 7px 6px; border-radius: 8px; border: none;
  background: transparent; color: var(--text-muted); font-size: 12px; font-weight: 700;
  cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 5px;
}
.seg-btn.active { background: var(--primary); color: #051B11; font-weight: 800; }

/* INPUTS */
.field-label { font-size: 11px; font-weight: 700; color: var(--text-muted); margin-bottom: 4px; display: block; }
.input-wrap { position: relative; margin-bottom: 8px; }
.text-input {
  width: 100%; background: var(--bg); border: 1px solid var(--border);
  color: var(--text); padding: 10px 34px 10px 12px; border-radius: 12px; font-size: 13px;
  outline: none;
}
.text-input:focus { border-color: var(--primary); }
.clear-btn {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 14px;
}

/* CHIPS */
.chips-scroll { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px; margin-bottom: 8px; }
.chips-scroll::-webkit-scrollbar { height: 4px; }
.chips-scroll::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
.chip {
  white-space: nowrap; padding: 5px 10px; border-radius: 999px;
  border: 1px solid var(--border); background: var(--surface-variant);
  color: var(--text); font-size: 11px; font-weight: 600; cursor: pointer;
}
.chip:hover, .chip.active { border-color: var(--primary); background: rgba(16, 185, 129, 0.16); color: var(--primary); }

/* QUOTA BANNER */
.quota-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; border-radius: 10px; margin-bottom: 10px;
  background: var(--surface-variant); border: 1px solid var(--border); font-size: 11px;
}
.quota-bar.pro { background: rgba(16, 185, 129, 0.12); border-color: rgba(16, 185, 129, 0.35); color: var(--primary); }

/* PRIMARY BUTTON */
.btn-generate {
  width: 100%; padding: 12px; border-radius: 12px; border: none;
  background: var(--primary); color: #041F14; font-size: 14px; font-weight: 800;
  cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px;
}
.btn-generate:hover { background: #14CE90; }
.btn-generate:disabled { opacity: 0.5; cursor: not-allowed; }

/* SONG CONFIRMATION OPTIONS */
.song-option-card {
  display: flex; align-items: center; gap: 10px; padding: 9px 10px;
  border-radius: 12px; background: var(--surface-variant); border: 1px solid var(--border);
  margin-bottom: 6px; cursor: pointer;
}
.song-option-card:hover { border-color: var(--primary); }
.song-cover {
  width: 44px; height: 44px; border-radius: 8px; background: #1E293B;
  display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0;
  overflow: hidden;
}
.song-cover img { width: 100%; height: 100%; object-fit: cover; }
.confirmed-song-card {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  border-radius: 12px; background: rgba(16, 185, 129, 0.14);
  border: 1.5px solid var(--primary); margin-bottom: 10px;
}

/* PRESET SUMMARY HEADER */
.preset-header-card {
  background: rgba(16, 185, 129, 0.11);
  border: 1px solid rgba(16, 185, 129, 0.35);
  border-radius: 16px; padding: 14px;
}
.preset-title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.preset-name { font-size: 18px; font-weight: 800; color: var(--text); }
.badge-row { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0; }
.pill {
  font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 6px;
  background: var(--surface); border: 1px solid var(--border); color: var(--text);
}
.pill.green { background: rgba(16, 185, 129, 0.2); border-color: var(--primary); color: var(--primary); }
.preset-desc { font-size: 12px; color: var(--text-muted); line-height: 1.45; }

/* MK-20 HARDWARE LCD SIGNAL CHAIN PANEL */
.lcd-panel {
  background: var(--lcd-bg);
  border: 2px solid #2A3543;
  border-radius: 14px;
  overflow: hidden;
}
.lcd-top-bar {
  background: var(--lcd-bar);
  padding: 6px 10px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #2A3644;
}
.lcd-top-left { display: flex; align-items: center; gap: 6px; }
.lcd-top-right { display: flex; align-items: center; gap: 10px; font-size: 11px; font-weight: 800; }
.lcd-vol-pill {
  display: flex; align-items: center; gap: 4px; cursor: pointer;
  padding: 2px 6px; border-radius: 5px; background: rgba(255,255,255,0.06);
}
.lcd-canvas-wrap { position: relative; padding: 14px 10px 10px 10px; }
.lcd-row {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px;
  position: relative; z-index: 2;
}
.lcd-row.bottom { margin-top: 14px; }
.lcd-slot {
  display: flex; flex-direction: column; align-items: center; cursor: pointer;
  padding: 3px 2px; border-radius: 6px; position: relative;
}
.lcd-slot.selected {
  outline: 1.5px solid var(--lcd-cyan);
  background: rgba(92, 225, 230, 0.08);
}
.lcd-slot.selected::before {
  content: '▼'; color: var(--lcd-orange); font-size: 9px;
  position: absolute; top: -10px; left: 50%; transform: translateX(-50%);
}
.lcd-slot-icon {
  width: 44px; height: 48px; border-radius: 6px;
  display: flex; flex-direction: column; align-items: center; justify-content: space-between;
  padding: 4px 3px; position: relative; transition: transform 0.12s;
  border: 1.5px solid rgba(255,255,255,0.22);
}
.lcd-slot.off .lcd-slot-icon { opacity: 0.33; filter: grayscale(75%); }
.lcd-slot-label {
  font-size: 10px; font-weight: 800; color: #FFFFFF; margin-top: 4px;
  letter-spacing: 0.3px;
}
.lcd-slot.selected .lcd-slot-label { color: var(--lcd-cyan); }
.lcd-slot.off .lcd-slot-label { color: #566370; }

.lcd-footer {
  padding: 8px 10px 10px 10px;
  border-top: 1px solid #202B37;
  display: flex; flex-direction: column; gap: 8px;
}
.lcd-hw-btns { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.btn-hw-select {
  padding: 8px; border-radius: 8px; border: 1px solid var(--lcd-cyan);
  background: rgba(92, 225, 230, 0.12); color: var(--lcd-cyan);
  font-size: 12px; font-weight: 800; cursor: pointer;
}
.btn-hw-onoff {
  padding: 8px; border-radius: 8px; border: 1px solid var(--border);
  background: #26313F; color: #9BA7B4;
  font-size: 12px; font-weight: 800; cursor: pointer;
}
.btn-hw-onoff.on { background: var(--lcd-orange); border-color: #FF7847; color: #FFFFFF; }
.lcd-nav-row { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: var(--text-muted); }
.btn-lcd-step {
  background: transparent; border: 1px solid var(--border); color: var(--text);
  padding: 5px 10px; border-radius: 7px; font-size: 11px; font-weight: 700; cursor: pointer;
}

/* STOMP MODULE EDITOR CARD */
.stomp-card {
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: 16px;
  padding: 14px;
}
.stomp-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.stomp-badge {
  padding: 5px 10px; border-radius: 8px; font-size: 13px; font-weight: 900; color: #FFF;
}

/* CUSTOM RICH MODEL DROPDOWN */
.custom-dropdown-btn {
  width: 100%; background: var(--bg); color: var(--text);
  border: 1px solid var(--border); border-radius: 12px; padding: 10px 12px;
  display: flex; align-items: center; justify-content: space-between;
  cursor: pointer; margin-bottom: 8px; text-align: left;
}
.custom-dropdown-btn:hover { border-color: var(--primary); }
.dropdown-menu-list {
  max-height: 290px; overflow-y: auto; background: #11161D;
  border: 1.5px solid var(--primary); border-radius: 12px;
  margin-bottom: 10px; padding: 6px;
}
.dropdown-item {
  padding: 8px 10px; border-radius: 8px; cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.dropdown-item:hover, .dropdown-item.selected { background: rgba(16, 185, 129, 0.16); }
.model-desc-box {
  background: var(--bg); border: 1px solid var(--border);
  border-radius: 10px; padding: 8px 10px; font-size: 11px; color: var(--text-muted);
  margin-bottom: 12px; line-height: 1.4;
}

/* KNOB GRID */
.knobs-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.knob-card {
  background: var(--bg); border: 1px solid var(--border); border-radius: 12px;
  padding: 8px 6px; display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.knob-val { font-size: 12px; font-weight: 800; color: var(--text); }
.knob-name { font-size: 10px; font-weight: 700; color: var(--text-muted); text-align: center; }
.knob-slider { width: 100%; accent-color: var(--primary); cursor: pointer; height: 5px; }

/* CHAT / ROADIE IA */
.chat-box { display: flex; flex-direction: column; gap: 10px; margin-bottom: 10px; }
.msg {
  max-width: 92%; padding: 11px 13px; border-radius: 14px; font-size: 12.5px; line-height: 1.48;
}
.msg.ai { background: var(--surface); border: 1px solid var(--border); align-self: flex-start; }
.msg.user { background: var(--primary); color: #041F14; font-weight: 600; align-self: flex-end; }
.ai-preset-card {
  margin-top: 10px; background: var(--bg); border: 1px solid var(--primary);
  border-radius: 12px; padding: 10px;
}

/* BOTTOM NAVIGATION BAR */
.bottom-nav {
  background: var(--surface);
  border-top: 1px solid var(--border);
  display: grid; grid-template-columns: repeat(5, 1fr);
  padding: 6px 4px; flex-shrink: 0;
}
.nav-item {
  background: transparent; border: none; color: var(--text-muted);
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  padding: 6px 2px; border-radius: 10px; cursor: pointer; font-size: 10px; font-weight: 700;
}
.nav-item.active { color: var(--primary); background: rgba(16, 185, 129, 0.12); }
.nav-icon { font-size: 17px; }

/* MODALS */
.modal-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,0.78);
  display: none; align-items: center; justify-content: center; z-index: 100; padding: 14px;
}
.modal-backdrop.open { display: flex; }
.modal-card {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: 18px; width: 100%; max-width: 440px; padding: 18px;
  max-height: 90vh; overflow-y: auto;
}
.modal-title { font-size: 16px; font-weight: 800; margin-bottom: 8px; }
.btn-row { display: flex; gap: 8px; margin-top: 12px; }
.btn-sec {
  flex: 1; padding: 10px; border-radius: 10px; border: 1px solid var(--border);
  background: var(--surface-variant); color: var(--text); font-size: 12px; font-weight: 700; cursor: pointer;
}
.btn-pri {
  flex: 1; padding: 10px; border-radius: 10px; border: none;
  background: var(--primary); color: #041F14; font-size: 12px; font-weight: 800; cursor: pointer;
}
.code-pre {
  background: #090C10; border: 1px solid var(--border); border-radius: 10px;
  padding: 10px; font-family: Consolas, monospace; font-size: 11px; color: #A5D6FF;
  max-height: 240px; overflow: auto; white-space: pre-wrap; user-select: text;
}
.tutorial-img {
  width: 100%; border-radius: 10px; border: 1px solid var(--border);
  cursor: zoom-in; margin-top: 8px; display: block;
}
</style>
</head>
<body>

<!-- TOP APP BAR -->
<header class=""top-bar"">
  <div class=""brand-row"">
    <div class=""brand-logo"">🎸</div>
    <div>
      <div class=""brand-title"">
        Preset20 AI <span class=""badge-mk20"">MK-20</span>
      </div>
      <div class=""brand-sub"">Gerador de Timbres IA • 412B M-EFCS</div>
    </div>
  </div>
  <div class=""top-actions"">
    <button id=""btnProTop"" class=""btn-pro"" onclick=""openModal('modalPro')"">👑 PRO</button>
    <button class=""icon-btn"" title=""Salvar Preset"" onclick=""openSaveModal()"">💾</button>
    <button class=""icon-btn"" title=""Compartilhar / Exportar"" onclick=""openModal('modalShare')"">📤</button>
    <button class=""icon-btn"" title=""Configurações e Chave Gemini IA"" onclick=""openSettingsModal()"">⚙️</button>
  </div>
</header>

<!-- SCROLLABLE CONTENT -->
<main class=""main-scroll"">
  <div class=""container"">

    <!-- TAB 0: PEDALEIRA -->
    <div id=""tab-0"" class=""tab-pane"">
      <div class=""card"">
        <div class=""card-title-row"">
          <div class=""card-title"">✨ Criar Timbre para MK-20</div>
          <span class=""badge-mk20"">368 Modelos</span>
        </div>

        <div class=""seg-row"">
          <button id=""mode-SONG"" class=""seg-btn active"" onclick=""setInputMode('SONG')"">🎵 Nome</button>
          <button id=""mode-YOUTUBE"" class=""seg-btn"" onclick=""setInputMode('YOUTUBE')"">▶️ YouTube</button>
          <button id=""mode-AUDIO"" class=""seg-btn"" onclick=""setInputMode('AUDIO')"">🎙️ Vibe (Mic)</button>
        </div>

        <!-- SONG MODE -->
        <div id=""pane-SONG"">
          <div class=""input-wrap"">
            <input id=""songInput"" class=""text-input"" placeholder=""Artista, música ou estilo (ex: Eli Soares - Me Ajude a Melhorar)..."" value=""Eli Soares - Me Ajude a Melhorar"" oninput=""onSongInputChanged()"">
            <button class=""clear-btn"" onclick=""clearSongInput()"">✕</button>
          </div>
          <!-- Confirmed Song or Options -->
          <div id=""songConfirmArea""></div>

          <div class=""chips-scroll"">
            <button class=""chip"" onclick=""pickQuickTag('David Gilmour - Comfortably Numb Solo')"">Comfortably Numb Solo</button>
            <button class=""chip"" onclick=""pickQuickTag('Worship Ambient Swell Shimmer')"">Worship Swell</button>
            <button class=""chip"" onclick=""pickQuickTag('Metallica - Master of Puppets High Gain')"">Master of Puppets</button>
            <button class=""chip"" onclick=""pickQuickTag('John Mayer - Slow Dancing Blues Clean')"">Blues Clean</button>
            <button class=""chip"" onclick=""pickQuickTag('Slash - Sweet Child O Mine Lead')"">Slash Lead</button>
            <button class=""chip"" onclick=""pickQuickTag('Contrabaixo Ampeg SVT Slap Moderno')"">Bass Ampeg Slap</button>
            <button class=""chip"" onclick=""pickQuickTag('Violão Acústico Taylor Fingerstyle')"">Violão Taylor</button>
            <button class=""chip"" onclick=""pickQuickTag('Eli Soares - Me Ajude a Melhorar')"">Eli Soares</button>
          </div>
        </div>

        <!-- YOUTUBE MODE -->
        <div id=""pane-YOUTUBE"" style=""display:none;"">
          <div class=""input-wrap"">
            <input id=""ytUrlInput"" class=""text-input"" placeholder=""Cole o link do YouTube (ex: https://youtu.be/...)"" oninput=""onYoutubeInputChanged()"">
          </div>
          <div id=""ytPreviewArea""></div>
          <div class=""input-wrap"">
            <input id=""ytPresetNameInput"" class=""text-input"" placeholder=""Nome da Música / Timbre do Vídeo (ex: Solo Worship)..."" oninput=""syncConfirmedFromYt()"">
          </div>
        </div>

        <!-- AUDIO MIC VIBE MODE -->
        <div id=""pane-AUDIO"" style=""display:none;"">
          <div style=""background:var(--bg); border:1px solid var(--border); border-radius:12px; padding:12px; text-align:center; margin-bottom:8px;"">
            <div style=""font-size:12px; font-weight:800; margin-bottom:6px;"" id=""micStatusText"">Grave 5 a 10 segundos do timbre ou música para detectar a Vibe</div>
            <div id=""micBars"" style=""display:flex; justify-content:center; align-items:flex-end; gap:5px; height:36px; margin:8px 0;"">
              <div class=""mbar"" style=""width:8px; height:8px; background:var(--primary); border-radius:4px;""></div>
              <div class=""mbar"" style=""width:8px; height:12px; background:var(--primary); border-radius:4px;""></div>
              <div class=""mbar"" style=""width:8px; height:8px; background:var(--primary); border-radius:4px;""></div>
              <div class=""mbar"" style=""width:8px; height:16px; background:var(--primary); border-radius:4px;""></div>
              <div class=""mbar"" style=""width:8px; height:10px; background:var(--primary); border-radius:4px;""></div>
              <div class=""mbar"" style=""width:8px; height:14px; background:var(--primary); border-radius:4px;""></div>
              <div class=""mbar"" style=""width:8px; height:8px; background:var(--primary); border-radius:4px;""></div>
            </div>
            <button id=""btnMicRecord"" class=""btn-sec"" style=""width:100%;"" onclick=""toggleMicRecording()"">🎙️ Capturar Vibe pelo Microfone</button>
          </div>
          <div id=""vibeDetectedArea""></div>
        </div>

        <!-- ADDITIONAL NOTES -->
        <div class=""input-wrap"">
          <input id=""notesInput"" class=""text-input"" placeholder=""Observações (ex: Guitarra Strato / Les Paul, Solo, Base, Sem abelha)..."" value=""Solo melódico encorpado sem abelha digital"">
        </div>

        <!-- QUOTA / PRO STATUS -->
        <div id=""quotaBanner"" class=""quota-bar"">
          <span id=""quotaText"">⚡ Cota gratuita: 2/2 gerações disponíveis hoje</span>
          <span style=""color:var(--amber); font-weight:800; cursor:pointer;"" onclick=""openModal('modalPro')"">Assinar PRO →</span>
        </div>

        <button id=""btnGenerateMain"" class=""btn-generate"" onclick=""generateAiPreset()"">
          ⚡ Gerar Preset MK-20
        </button>
      </div>

      <!-- PRESET SUMMARY HEADER -->
      <div class=""preset-header-card"">
        <div class=""preset-title-row"">
          <div class=""preset-name"" id=""lblPresetTitle"">Eli Soares Lead</div>
          <button class=""icon-btn"" title=""Renomear Preset"" onclick=""renameCurrentPreset()"">✏️</button>
        </div>
        <div class=""badge-row"">
          <span class=""pill green"" id=""lblPresetStyle"">Gospel / Neo-Soul Lead</span>
          <span class=""pill"" id=""lblPresetGuitar"">🎸 Strato / Humbucker</span>
          <span class=""pill"" id=""lblPresetBpm"">⏱ 118 BPM</span>
          <span class=""pill"" id=""lblPresetPickup"">Captador Ponte</span>
        </div>
        <div class=""preset-desc"" id=""lblPresetDesc"">Timbre calibrado com engenharia acústica real para a M-VAVE MK-20 (412 bytes M-EFCS).</div>
      </div>

      <!-- MK-20 LCD HARDWARE SIGNAL CHAIN PANEL -->
      <div class=""lcd-panel"">
        <div class=""lcd-top-bar"">
          <div class=""lcd-top-left"">
            <span style=""font-size:11px; font-weight:900; color:#FFF;"">Visor LCD MK-20</span>
            <span style=""font-size:9px; padding:1px 5px; border-radius:4px; background:#0F1722; color:var(--lcd-cyan); font-weight:800;"">M-EFCS</span>
          </div>
          <div class=""lcd-top-right"">
            <div class=""lcd-vol-pill"" onclick=""adjustMasterVolPrompt()"" title=""Clique para ajustar Preset Vol"">
              🔊 <span style=""color:#9BA7B4;"">Preset Vol :</span> <span id=""lcdMasterVol"" style=""color:#FFF;"">60</span>
            </div>
            <div style=""color:#9BA7B4;"">BPM:<span id=""lcdBpmVal"" style=""color:var(--lcd-cyan);"">80</span></div>
            <div style=""display:flex; align-items:center; gap:3px; color:#5CE1E6;"">
              <span id=""lcdCpuVal"">48%</span>
            </div>
            <div style=""color:#10B981;"">🔋</div>
          </div>
        </div>

        <div class=""lcd-canvas-wrap"">
          <div class=""lcd-row"" id=""lcdRowTop""></div>
          <div class=""lcd-row bottom"" id=""lcdRowBottom""></div>
        </div>

        <div class=""lcd-footer"">
          <div class=""lcd-hw-btns"">
            <button class=""btn-hw-select"" onclick=""focusModelSelector()"">Select (Trocar Modelo)</button>
            <button id=""btnHwOnOff"" class=""btn-hw-onoff on"" onclick=""toggleSelectedBlockActive()"">ON - OFF</button>
          </div>
          <div class=""lcd-nav-row"">
            <button class=""btn-lcd-step"" onclick=""stepBlock(-1)"">&lt; Anterior</button>
            <span id=""lblBlockStep"" style=""font-weight:800; color:var(--text);"">4 / 10 • AMP</span>
            <button class=""btn-lcd-step"" onclick=""stepBlock(1)"">Próximo &gt;</button>
          </div>
          <button class=""btn-sec"" style=""width:100%; padding:7px;"" onclick=""toggleShowAllPedals()"">
            <span id=""lblToggleAllBtn"">📋 Ver Todos os 10 Módulos Empilhados</span>
          </button>
        </div>
      </div>

      <!-- SELECTED PEDAL STOMP CARD (OR ALL 10) -->
      <div id=""stompEditorArea""></div>
    </div>

    <!-- TAB 1: ASSISTENTE (ROADIE IA) -->
    <div id=""tab-1"" class=""tab-pane"" style=""display:none;"">
      <div class=""card"">
        <div class=""card-title-row"">
          <div>
            <div class=""card-title"">🎧 Roadie IA • Engenheiro de Timbre MK-20</div>
            <div style=""font-size:11px; color:var(--text-muted); margin-top:2px;"">Peça ajustes diretos ou novos timbres e aplique em 1 clique</div>
          </div>
          <button class=""icon-btn"" title=""Limpar conversa"" onclick=""resetAssistantChat()"">🔄</button>
        </div>
        <div style=""background:var(--bg); border:1px solid var(--border); border-radius:10px; padding:8px 10px; margin-bottom:10px; display:flex; align-items:center; justify-content:space-between;"">
          <span style=""font-size:11px; color:var(--text-muted);"">Timbre em Edição: <strong id=""roadieCurrentTitle"" style=""color:var(--text);"">Eli Soares Lead</strong></span>
          <span id=""roadieCurrentAmp"" class=""badge-mk20"">86OD_JCM800</span>
        </div>

        <div class=""chips-scroll"">
          <button class=""chip"" onclick=""sendRoadieQuick('Tirar som abafado e abrir clareza na mix')"">Tirar som abafado</button>
          <button class=""chip"" onclick=""sendRoadieQuick('Cortar agudo ardido / abelha digital')"">Cortar agudo ardido</button>
          <button class=""chip"" onclick=""sendRoadieQuick('Mais sustain e destaque para solo')"">Mais sustain p/ solo</button>
          <button class=""chip"" onclick=""sendRoadieQuick('Deixar mais limpo e cristalino')"">Deixar mais limpo</button>
          <button class=""chip"" onclick=""sendRoadieQuick('Timbre Worship Swell com Shimmer Stereo')"">Worship Swell</button>
          <button class=""chip"" onclick=""sendRoadieQuick('Mais peso de Metal High-Gain com Gate firme')"">Mais peso (Metal)</button>
          <button class=""chip"" onclick=""sendRoadieQuick('Timbre de Contrabaixo Ampeg SVT encorpado')"">Timbre de Baixo</button>
          <button class=""chip"" onclick=""sendRoadieQuick('Simulação de Violão Acústico natural')"">Simular Violão</button>
        </div>

        <div id=""chatMessages"" class=""chat-box""></div>

        <div style=""display:flex; gap:8px;"">
          <input id=""roadieInput"" class=""text-input"" style=""margin-bottom:0;"" placeholder=""Ex: Tire a abelha, suba os médios ou crie um timbre de Gilmour..."" onkeydown=""if(event.key==='Enter') sendRoadieMessage()"">
          <button class=""btn-pri"" style=""flex:0 0 88px;"" onclick=""sendRoadieMessage()"">Enviar ➤</button>
        </div>
      </div>
    </div>

    <!-- TAB 2: BINÁRIO (.MK20 / M-EFCS) -->
    <div id=""tab-2"" class=""tab-pane"" style=""display:none;"">
      <div class=""card"">
        <div class=""card-title-row"">
          <div class=""card-title"">💾 Exportação Oficial M-EFCS (.mk20)</div>
          <span id=""binSizeBadge"" class=""badge-mk20"">412 BYTES</span>
        </div>
        <div style=""font-size:11.5px; color:var(--text-muted); margin-bottom:10px;"">
          Gera o arquivo binário autêntico de hardware (assinatura <code>FF 90 1E 00</code> e trailer <code>NGD4</code>) pronto para importar no aplicativo M-EFCS de Windows, Android ou iOS.
        </div>

        <div class=""field-label"">Modo de Tamanho Binário:</div>
        <div class=""chips-scroll"" id=""binSizeChips"">
          <button class=""chip active"" onclick=""setBinaryMode('AUTHENTIC_412', this)"">412B Oficial MK-20 (Recomendado)</button>
          <button class=""chip"" onclick=""setBinaryMode('TEMPLATE_CLONE', this)"">+ Usar Molde do Meu MK-20</button>
          <button class=""chip"" onclick=""setBinaryMode('STANDARD_512', this)"">512B M-EFCS Alternativo</button>
          <button class=""chip"" onclick=""setBinaryMode('COMPACT_256', this)"">256B Compacto</button>
          <button class=""chip"" onclick=""setBinaryMode('EXTENDED_1024', this)"">1024B Expandido</button>
        </div>

        <div id=""templateStatusBanner"" style=""display:none; background:rgba(16,185,129,0.14); border:1px solid var(--primary); border-radius:10px; padding:8px 10px; margin-bottom:10px; font-size:11px;""></div>

        <div style=""display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-bottom:10px;"">
          <button class=""btn-generate"" style=""grid-column:span 2;"" onclick=""exportMk20File(false)"">⬇ Exportar .mk20 (M-EFCS Oficial)</button>
          <button class=""btn-sec"" onclick=""document.getElementById('fileTemplateInput').click()"">📂 Carregar Molde .mk20</button>
          <button class=""btn-sec"" onclick=""exportMk20File(true)"">⬇ Exportar .amk20</button>
          <button class=""btn-sec"" onclick=""document.getElementById('fileImportInput').click()"">📥 Importar Arquivo (.mk20/.json)</button>
          <button class=""btn-sec"" onclick=""copyPresetJson()"">📋 Copiar JSON</button>
          <button class=""btn-sec"" onclick=""exportBackupJson()"">💾 Backup .json</button>
          <button class=""btn-sec"" onclick=""openModal('modalShare')"">🔗 Compartilhar Link</button>
        </div>
        <input type=""file"" id=""fileTemplateInput"" accept="".mk20,.amk20,.bin"" style=""display:none"" onchange=""onTemplateFileLoaded(event)"">
        <input type=""file"" id=""fileImportInput"" accept="".mk20,.amk20,.json"" style=""display:none"" onchange=""onImportFileLoaded(event)"">

        <div class=""field-label"">Inspeção Hexadecimal Binária (412 Bytes M-EFCS):</div>
        <pre id=""hexPreviewBox"" class=""code-pre"" style=""margin-bottom:10px; max-height:150px;""></pre>

        <div class=""field-label"">Estrutura JSON Completa do Preset:</div>
        <pre id=""jsonPreviewBox"" class=""code-pre""></pre>
      </div>
    </div>

    <!-- TAB 3: SALVOS -->
    <div id=""tab-3"" class=""tab-pane"" style=""display:none;"">
      <div class=""card"">
        <div class=""card-title-row"">
          <div class=""card-title"">⭐ Meus Presets Salvos</div>
          <span id=""savedCountPill"" class=""badge-mk20"">0 salvos</span>
        </div>
        <div style=""background:var(--surface-variant); border:1px solid var(--border); border-radius:10px; padding:9px 11px; margin-bottom:10px; font-size:11px; display:flex; align-items:center; justify-content:space-between;"">
          <span>☁️ Sincronização Automática Local &amp; Backup JSON ativa</span>
          <button class=""chip"" onclick=""saveCurrentPresetQuick()"">+ Salvar Atual</button>
        </div>
        <div class=""input-wrap"">
          <input id=""savedSearchInput"" class=""text-input"" placeholder=""Buscar por nome, estilo ou guitarra..."" oninput=""renderSavedPresets()"">
        </div>
        <div id=""savedListArea""></div>
      </div>
    </div>

    <!-- TAB 4: AJUDA & MANUAL COM IMAGENS -->
    <div id=""tab-4"" class=""tab-pane"" style=""display:none;"">
      <div class=""card"" style=""background:rgba(16,185,129,0.12); border-color:rgba(16,185,129,0.4);"">
        <div style=""font-size:16px; font-weight:900; margin-bottom:4px;"">📖 Manual Oficial Preset20 AI &amp; M-VAVE MK-20</div>
        <div style=""font-size:12px; color:var(--text-muted); margin-bottom:10px;"">Guia completo de timbres, importação no M-EFCS e configuração da Chave Gemini IA.</div>
        <div style=""display:flex; gap:6px; flex-wrap:wrap;"">
          <span class=""pill green"">368 Modelos Oficiais</span>
          <span class=""pill green"">412 Bytes M-EFCS</span>
          <span class=""pill green"">3 Instrumentos (Guitarra, Baixo, Violão)</span>
        </div>
      </div>

      <div class=""chips-scroll"">
        <button class=""chip active"" onclick=""filterHelpSection('ALL', this)"">Todos</button>
        <button class=""chip"" onclick=""filterHelpSection('CREATE', this)"">1. Como Criar</button>
        <button class=""chip"" onclick=""filterHelpSection('BLOCKS', this)"">2. 10 Módulos</button>
        <button class=""chip"" onclick=""filterHelpSection('IMPORT', this)"">3. Importar no M-EFCS</button>
        <button class=""chip"" onclick=""filterHelpSection('APIKEY', this)"">4. Chave Gemini IA</button>
      </div>

      <div id=""help-CREATE"" class=""card help-sec"">
        <div class=""card-title"" style=""margin-bottom:8px;"">1. Como Criar Timbres Perfeitos na MK-20</div>
        <p style=""font-size:12px; color:var(--text-muted); line-height:1.5;"">
          • <strong>Modo Nome:</strong> Digite o nome da música ou artista, confirme a versão exata na lista e clique em <em>Gerar Preset MK-20</em>.<br>
          • <strong>Modo YouTube:</strong> Cole o link do vídeo para identificar a música automaticamente.<br>
          • <strong>Modo Vibe (Mic):</strong> Use o microfone para analisar o ganho, BPM e estilo acústico em tempo real.<br>
          • <strong>Hardware Oficial M-VAVE MK-20:</strong>
        </p>
        <img src=""/tutorial/pedaleira_fisica.jpg"" class=""tutorial-img"" alt=""Pedaleira Física M-VAVE MK-20"" onclick=""openZoomModal(this.src, 'Pedaleira Física M-VAVE MK-20')"">
      </div>

      <div id=""help-BLOCKS"" class=""card help-sec"">
        <div class=""card-title"" style=""margin-bottom:8px;"">2. Os 10 Blocos Oficiais da Cadeia MK-20</div>
        <p style=""font-size:12px; color:var(--text-muted); line-height:1.55;"">
          1. <strong>FX (20 modelos):</strong> Wah-Wah, Sense-Wah, Boosters, Compress, Pitch, Whammy.<br>
          2. <strong>NS (8 modelos):</strong> AI Gate, AI MS Gate, Soft/Hard/Pro Gate e Compressores.<br>
          3. <strong>DS (52 modelos):</strong> MW-808, MW-GOLD, MW-OCD, MW-RAT, MW-MUFF, MW-B7000 e 40 Drives.<br>
          4. <strong>AMP (120 modelos):</strong> 90 Amplificadores de Guitarra + 30 Pre-Amps de Contrabaixo.<br>
          5. <strong>CAB (100 modelos):</strong> 70 Caixas de Guitarra + 5 IRs de Violão Acústico + 25 Caixas de Baixo.<br>
          6. <strong>EQ (3 modelos):</strong> Guitar EQ 6, Bass EQ 7 e Normal EQ 10 (-12dB a +12dB).<br>
          7. <strong>MOD (20 modelos):</strong> Chorus, Flanger, Tremolo, Phaser, Vibrato, Univibe Mono/Stereo.<br>
          8. <strong>DELAY (27 modelos):</strong> Clean, Analog, Dtype Tape, PingPong Stereo, Ice, Reverse.<br>
          9. <strong>REVERB (18 modelos):</strong> Room, Hall, Plate, Spring, Shimmer, Cloud, Bloom, Swell.<br>
          10. <strong>VOL:</strong> Volume do módulo na cadeia + Preset Vol global.
        </p>
      </div>

      <div id=""help-IMPORT"" class=""card help-sec"">
        <div class=""card-title"" style=""margin-bottom:8px;"">3. Passo a Passo Visual: Importar .mk20 no App M-EFCS</div>
        <p style=""font-size:12px; color:var(--text-muted); margin-bottom:8px;"">Clique em qualquer imagem abaixo para ampliar em tela cheia:</p>
        <div style=""font-size:12px; font-weight:700; margin-top:6px;"">Passo 1: Exporte o arquivo .mk20 (412 Bytes) na aba Binário</div>
        <img src=""/tutorial/passo_1.png"" class=""tutorial-img"" onclick=""openZoomModal(this.src, 'Passo 1')"">
        <div style=""font-size:12px; font-weight:700; margin-top:10px;"">Passo 2: Abra o app M-EFCS e conecte sua pedaleira MK-20</div>
        <img src=""/tutorial/passo_2.png"" class=""tutorial-img"" onclick=""openZoomModal(this.src, 'Passo 2')"">
        <div style=""font-size:12px; font-weight:700; margin-top:10px;"">Passo 3: Selecione um slot vazio ou de usuário na MK-20</div>
        <img src=""/tutorial/passo_3.png"" class=""tutorial-img"" onclick=""openZoomModal(this.src, 'Passo 3')"">
        <div style=""font-size:12px; font-weight:700; margin-top:10px;"">Passo 4: Clique no ícone de Importar Preset no M-EFCS</div>
        <img src=""/tutorial/passo_4.png"" class=""tutorial-img"" onclick=""openZoomModal(this.src, 'Passo 4')"">
        <div style=""font-size:12px; font-weight:700; margin-top:10px;"">Passo 5: Escolha o arquivo .mk20 gerado pelo Preset20 AI</div>
        <img src=""/tutorial/passo_5.png"" class=""tutorial-img"" onclick=""openZoomModal(this.src, 'Passo 5')"">
        <div style=""font-size:12px; font-weight:700; margin-top:10px;"">Passo 6: Clique em Save na pedaleira para gravar na memória</div>
        <img src=""/tutorial/passo_6.png"" class=""tutorial-img"" onclick=""openZoomModal(this.src, 'Passo 6')"">
      </div>

      <div id=""help-APIKEY"" class=""card help-sec"">
        <div class=""card-title"" style=""margin-bottom:8px;"">4. Como Obter sua Chave Gratuita do Google Gemini IA</div>
        <p style=""font-size:12px; color:var(--text-muted); line-height:1.5; margin-bottom:10px;"">
          1. Acesse <strong>aistudio.google.com/app/apikey</strong>.<br>
          2. Clique em <strong>Create API Key</strong> e copie a chave gerada (começa com <code>AIza...</code>).<br>
          3. Clique no ícone ⚙️ no topo deste aplicativo e cole sua chave para ativar o motor <strong>Gemini 2.5 Flash</strong> em tempo real!
        </p>
        <div style=""display:flex; gap:8px;"">
          <button class=""btn-pri"" onclick=""window.open('https://aistudio.google.com/app/apikey','_blank')"">🔑 Abrir Google AI Studio</button>
          <button class=""btn-sec"" onclick=""openSettingsModal()"">⚙️ Colar Minha Chave</button>
        </div>
      </div>

      <div class=""card"">
        <div class=""card-title"" style=""margin-bottom:6px;"">📩 Suporte &amp; Contato do Desenvolvedor</div>
        <div style=""font-size:12px; color:var(--text-muted); margin-bottom:8px;"">Dúvidas, sugestões ou suporte técnico: <strong>StudioOlidin@gmail.com</strong></div>
        <button class=""btn-sec"" style=""width:100%;"" onclick=""navigator.clipboard.writeText('StudioOlidin@gmail.com'); alert('E-mail StudioOlidin@gmail.com copiado!');"">📋 Copiar E-mail StudioOlidin@gmail.com</button>
      </div>
    </div>

  </div>
</main>

<!-- BOTTOM NAVIGATION BAR -->
<nav class=""bottom-nav"">
  <button id=""nav-0"" class=""nav-item active"" onclick=""selectTab(0)"">
    <span class=""nav-icon"">🎛️</span><span>Pedaleira</span>
  </button>
  <button id=""nav-1"" class=""nav-item"" onclick=""selectTab(1)"">
    <span class=""nav-icon"">🎧</span><span>Assistente</span>
  </button>
  <button id=""nav-2"" class=""nav-item"" onclick=""selectTab(2)"">
    <span class=""nav-icon"">💾</span><span>Binário</span>
  </button>
  <button id=""nav-3"" class=""nav-item"" onclick=""selectTab(3)"">
    <span class=""nav-icon"">⭐</span><span id=""navSavedLabel"">Salvos</span>
  </button>
  <button id=""nav-4"" class=""nav-item"" onclick=""selectTab(4)"">
    <span class=""nav-icon"">📖</span><span>Ajuda</span>
  </button>
</nav>

<!-- MODAL: SAVE PRESET -->
<div id=""modalSave"" class=""modal-backdrop"">
  <div class=""modal-card"">
    <div class=""modal-title"">💾 Salvar Preset na Biblioteca</div>
    <label class=""field-label"">Nome do Preset:</label>
    <input id=""saveModalTitleInput"" class=""text-input"" style=""margin-bottom:10px;"">
    <div class=""btn-row"">
      <button class=""btn-sec"" onclick=""closeModal('modalSave')"">Cancelar</button>
      <button class=""btn-pri"" onclick=""confirmSavePreset()"">Salvar Preset</button>
    </div>
  </div>
</div>

<!-- MODAL: SHARE / EXPORT -->
<div id=""modalShare"" class=""modal-backdrop"">
  <div class=""modal-card"">
    <div class=""modal-title"">📤 Exportar e Compartilhar Timbre</div>
    <div style=""display:flex; flex-direction:column; gap:8px;"">
      <button class=""btn-generate"" onclick=""exportMk20File(false); closeModal('modalShare');"">⬇ Baixar Arquivo .mk20 (412B M-EFCS)</button>
      <button class=""btn-sec"" onclick=""exportMk20File(true); closeModal('modalShare');"">⬇ Baixar Arquivo .amk20</button>
      <button class=""btn-sec"" onclick=""copyShareText(); closeModal('modalShare');"">📋 Copiar Resumo do Timbre</button>
      <button class=""btn-sec"" onclick=""copyPresetJson(); closeModal('modalShare');"">📋 Copiar JSON Completo</button>
    </div>
    <div class=""btn-row"">
      <button class=""btn-sec"" onclick=""closeModal('modalShare')"">Fechar</button>
    </div>
  </div>
</div>

<!-- MODAL: SETTINGS & API KEY -->
<div id=""modalSettings"" class=""modal-backdrop"">
  <div class=""modal-card"">
    <div class=""modal-title"">⚙️ Configurações &amp; Chave Gemini IA</div>
    <label class=""field-label"">Chave da API Google Gemini (AIza...):</label>
    <input id=""settingsApiKeyInput"" type=""password"" class=""text-input"" placeholder=""Cole sua chave Gemini API aqui..."">
    <div style=""font-size:11px; color:var(--text-muted); margin-bottom:10px;"">
      Com sua chave gratuita do Google AI Studio, o Preset20 AI consulta o modelo <strong>Gemini 2.5 Flash</strong> diretamente para criar timbres sob medida. Sem chave, o motor acústico local de 368 modelos funciona 100% offline.
    </div>
    <div class=""btn-row"">
      <button class=""btn-sec"" onclick=""closeModal('modalSettings')"">Cancelar</button>
      <button class=""btn-pri"" onclick=""saveSettingsModal()"">Salvar Configuração</button>
    </div>
  </div>
</div>

<!-- MODAL: PRO UPGRADE -->
<div id=""modalPro"" class=""modal-backdrop"">
  <div class=""modal-card"">
    <div class=""modal-title"" style=""color:var(--amber);"">👑 Preset20 AI PRO • Ilimitado</div>
    <p style=""font-size:12px; color:var(--text-muted); line-height:1.5; margin-bottom:12px;"">
      • Gerações de timbres IA <strong>ilimitadas</strong> todos os dias<br>
      • Roadie IA sem limite de ajustes por conversa<br>
      • Exportação binária 412B M-EFCS e clonagem de molde ilimitadas<br>
      • Biblioteca de presets salvos ilimitada
    </p>
    <div id=""proModalActions"" style=""display:flex; flex-direction:column; gap:8px;"">
      <button class=""btn-generate"" style=""background:var(--amber); color:#1F1400;"" onclick=""toggleProVersion(true)"">✨ Ativar Modo PRO nesta Conta / PC</button>
      <button class=""btn-sec"" onclick=""toggleProVersion(false)"">Voltar para Modo Gratuito (2/dia)</button>
    </div>
    <div class=""btn-row"">
      <button class=""btn-sec"" onclick=""closeModal('modalPro')"">Fechar</button>
    </div>
  </div>
</div>

<!-- MODAL: IMAGE ZOOM -->
<div id=""modalZoom"" class=""modal-backdrop"" onclick=""closeModal('modalZoom')"">
  <div class=""modal-card"" style=""max-width:720px; text-align:center;"">
    <div id=""zoomTitle"" class=""modal-title"">Visualização</div>
    <img id=""zoomImg"" src="""" style=""max-width:100%; max-height:75vh; border-radius:10px;"">
  </div>
</div>

<script>
// ============================================================================
// 1. CATÁLOGO OFICIAL COMPLETO DE 368 MODELOS DA M-VAVE MK-20 (FIRMWARE V85)
// ============================================================================
function ampDef(type, g, l, b, m, t, r, p, br) {
  return {
    Gain: g !== undefined ? g : (type==='Clean'?28:type==='Overdrive'?55:type==='Distortion'?72:type==='Bass'?35:50),
    Level: l !== undefined ? l : (type==='Clean'?75:type==='Bass'?78:68),
    Bass: b !== undefined ? b : (type==='Clean'?48:type==='Bass'?65:55),
    Middle: m !== undefined ? m : (type==='Distortion'?48:type==='Overdrive'?60:52),
    Treble: t !== undefined ? t : (type==='Clean'?58:type==='Bass'?52:60),
    Reso: r !== undefined ? r : (type==='Clean'?45:type==='Bass'?70:type==='Distortion'?65:55),
    Pres: p !== undefined ? p : (type==='Distortion'?65:type==='Bass'?55:52),
    Bright: br !== undefined ? br : (type==='Clean'?65:type==='Overdrive'?45:0)
  };
}
function cabDef(cfg) {
  if (cfg.toLowerCase().indexOf('bass') === 0) return { Level: 85, 'Low Cut': 35, 'High Cut': 5200 };
  if (cfg.toLowerCase() === 'acoustic') return { Level: 82, 'Low Cut': 60, 'High Cut': 12000 };
  return { Level: 82, 'Low Cut': 80, 'High Cut': 7000 };
}
function ds8Def() { return { gain:50, level:50, bass:50, middle:50, treble:50, reso:0, pres:50, bright:100 }; }

const MK20_CATALOG = {
  FX: [
    {id:1, name:'Wah-Wah', type:'AUTO / LFO WAH', desc:'Speed (0.1..10.0), Q (0..100), Mix (0..100), Width (0..100), Level (0..100), Sync, SyncBpm (40..240) — Auto-Wah cíclico por LFO', params:{Speed:2.5, Q:50, Mix:80, Width:25, Level:50, Sync:0, SyncBpm:80}},
    {id:2, name:'Lofi', type:'BITCRUSHER', desc:'Bit (2..24), Level (0..100), Filter (0..100) — Degradação vintage com redução de bits e filtro', params:{Bit:12, Level:50, Filter:50}},
    {id:3, name:'Sense-Wah', type:'ENVELOPE FILTER', desc:'Sense (0..10.0), Attack, Q, fPeak, Mix, Width, Level — Wah dinâmico sensível ao ataque da palhetada', params:{Sense:5.0, Attack:40, Q:60, fPeak:50, Mix:60, Width:65, Level:70}},
    {id:4, name:'Boost', type:'CLEAN BOOST', desc:'Gain (0..10.0), Level (0..100) — Elevador limpo de ganho e volume puro', params:{Gain:4.5, Level:75}},
    {id:5, name:'A Boost', type:'TYPE A HIGH BOOST', desc:'Gain (0..10.0), Bass, Mid, Treble, Level — Pré-amp booster tipo A com foco em agudos e presença', params:{Gain:4.0, Bass:50, Mid:55, Treble:55, Level:70}},
    {id:6, name:'E Boost', type:'TYPE E MID BOOST', desc:'Gain (0..10.0), Bass, Mid, Treble, Level — Pré-amp booster tipo E com foco em médios', params:{Gain:5.0, Bass:50, Mid:65, Treble:55, Level:70}},
    {id:7, name:'B Boost', type:'TYPE B LOW BOOST', desc:'Gain (0..10.0), Bass, Mid, Treble, Level — Pré-amp booster tipo B encorpado estilo BB Preamp', params:{Gain:5.0, Bass:58, Mid:52, Treble:50, Level:70}},
    {id:8, name:'Boost ED', type:'TYPE ED GAIN BOOST', desc:'Gain (0..10.0), Grit (0..100), Level (0..100) — Booster com saturação harmônica Grit', params:{Gain:4.5, Grit:35, Level:72}},
    {id:9, name:'Compress', type:'COMPRESSOR', desc:'Sustain (0..10.0), Attack (0..100), Level (0..100), Blend (0..100) — Compressor clássico para nivelamento e sustain', params:{Sustain:5.0, Attack:50, Level:70, Blend:60}},
    {id:10, name:'Compress Pro', type:'STUDIO COMP', desc:'Ratio (0..10.0), Gain, Knee, Thd, Attack, Level, Blend — Compressor de estúdio de 7 parâmetros', params:{Ratio:4.0, Gain:50, Knee:30, Thd:50, Attack:40, Level:70, Blend:80}},
    {id:11, name:'F Compress', type:'OPTICAL / TONE COMP', desc:'Ratio (0..10.0), Gain, Knee, Thd, Attack, Tone, Level, Blend — Compressor óptico de 8 parâmetros com Tone', params:{Ratio:3.5, Gain:50, Knee:30, Thd:50, Attack:40, Tone:60, Level:70, Blend:75}},
    {id:12, name:'Pitch', type:'DUAL HARMONIZER', desc:'High Pitch (0..24), Low Pitch (-24..0), High Level, Low Level, Dry Level — Harmonizador duplo', params:{'High Pitch':12, 'Low Pitch':-12, 'High Level':50, 'Low Level':50, 'Dry Level':80}},
    {id:13, name:'Pitch shifter', type:'CHROMATIC SHIFT', desc:'Semi (-12..12) — Transpositor de afinação em semitons', params:{Semi:0}},
    {id:14, name:'Octave', type:'CLASSIC OCTAVER', desc:'High Level (0..100), Low Level (0..100), Dry Level (0..100) — Gerador de oitava', params:{'High Level':40, 'Low Level':60, 'Dry Level':80}},
    {id:15, name:'Ring', type:'RING MODULATOR', desc:'Freq (0..100), Mix (0..100) — Modulador em anel metálico', params:{Freq:45, Mix:50}},
    {id:16, name:'X-Wah', type:'MODERN WAH', desc:'Value (0..100), Gain (0..100), Level (0..100) — Wah transparente de graves a médios', params:{Value:50, Gain:55, Level:70}},
    {id:17, name:'Funk-Wah', type:'HIGH-FREQ WAH', desc:'Value (0..100), Gain (0..100), Level (0..100) — Wah brilhante focado em agudos e funk', params:{Value:50, Gain:55, Level:70}},
    {id:18, name:'Slide-Wah', type:'HIGH-GAIN WAH', desc:'Value (0..100), Gain (0..100), Level (0..100) — Wah penetrante para distorção', params:{Value:50, Gain:50, Level:70}},
    {id:19, name:'Cry-Wah', type:'CLASSIC WAH', desc:'Value (0..100), Gain (0..100), Level (0..100) — Wah clássico vocal estilo Cry Baby', params:{Value:50, Gain:50, Level:70}},
    {id:20, name:'Whammy', type:'PITCH BEND EXP', desc:'mode (1=Mbody, 2=Oct1, 3=Oct2), bend (0..100), Mix (0..100) — Pedal Pitch Whammy', params:{mode:2, bend:0, Mix:100}}
  ],
  NS: [
    {id:1, name:'ai gate', type:'ADAPTIVE NEURAL', desc:'gate (0..100), bias (0..100), inputDetect (0=Input, 1=PreGate) — Noise Gate IA', params:{gate:50, bias:50, inputDetect:0}},
    {id:2, name:'ai ms gate', type:'FAST STACCATO', desc:'gate (0..100), bias (0..100), inputDetect (0=Input, 1=PreGate) — Gate IA Gen2 ultra-rápido', params:{gate:50, bias:50, inputDetect:0}},
    {id:3, name:'soft gate', type:'SMOOTH DECAY', desc:'thd (-80..-30), inputDetect (0=Input, 1=PreGate) — Noise gate suave', params:{thd:-60, inputDetect:0}},
    {id:4, name:'hard gate', type:'HIGH-GAIN CUT', desc:'thd (-80..-30), inputDetect (0=Input, 1=PreGate) — Corte rigoroso de ruído', params:{thd:-60, inputDetect:0}},
    {id:5, name:'pro gate', type:'STUDIO MULTI-PARAMETRIC', desc:'att, rel, thd (-80..-30), kw, ratio, inputDetect — Noise gate multiparamétrico', params:{att:50, rel:50, thd:-55, kw:50, ratio:50, inputDetect:0}},
    {id:6, name:'compress', type:'STOMP COMP', desc:'sustain, attack, level, blend, inputDetect — Compressor no bloco NS', params:{sustain:50, attack:50, level:50, blend:100, inputDetect:0}},
    {id:7, name:'compress pro', type:'STUDIO COMP', desc:'ratio, gain, knee, thd, attack, level, blend — Compressor Pro de 7 parâmetros no bloco NS', params:{ratio:50, gain:50, knee:50, thd:50, attack:50, level:50, blend:100}},
    {id:8, name:'f compress', type:'OPTICAL / TONE COMP', desc:'ratio, gain, knee, thd, attack, tone, level, blend, inputDetect — Compressor F de 9 parâmetros no bloco NS', params:{ratio:50, gain:50, knee:50, thd:50, attack:50, tone:60, level:50, blend:100, inputDetect:0}}
  ],
  DS: [
    {id:1, name:'MW-808', type:'Tube Screamer TS-808', desc:'Emulação fiel do icônico Ibanez TS808 com médios focados', params:{gain:0, tone:50, level:100}},
    {id:2, name:'MW-GOLD', type:'Klon Centaur', desc:'Overdrive transparente de boutique com articulação de acordes', params:{gain:50, tone:50, level:70}},
    {id:3, name:'MW-OCD', type:'Fulltone OCD', desc:'Overdrive/Distortion dinâmico touch-sensitive', params:{gain:0, tone:50, LPtype:0, mode:2, level:100}},
    {id:4, name:'MW-SD1', type:'Super Overdrive', desc:'Inspirado no lendário Boss SD-1 com médios destacados', params:{gain:0, tone:50, level:100}},
    {id:5, name:'MW-DS1', type:'Distortion Clássica', desc:'Emulação do clássico Boss DS-1 áspero, agressivo e cortante', params:{gain:0, tone:50, level:100}},
    {id:6, name:'MW-RAT', type:'Distorção Encorpada', desc:'Inspirado no ProCo Rat com caráter fuzzy e médios densos', params:{gain:0, filter:50, level:100}},
    {id:7, name:'MW-XEP', type:'Xotic EP Booster', desc:'Emulação do pré Echoplex com graves encorpados e brilho', params:{gain:0, boost:0, bright:0}},
    {id:8, name:'MW-FACE', type:'Fuzz Face Vintage', desc:'Fuzz clássico de silício/germânico orgânico e dinâmico', params:{fuzz:90, level:100}},
    {id:9, name:'MW-HRZ', type:'High-Gain com Gate', desc:'Distorção high-gain com Gate integrado', params:{gain:20, attack:1, bright:100, gate:0, 'gate mode':1, level:100}},
    {id:10, name:'MW-EQP', type:'Overdrive Paramétrico', desc:'Overdrive flexível com 3 modos de corte', params:{gain:0, tone:50, level:100, mode:1}},
    {id:11, name:'MW-MUFF', type:'Big Muff Pi Fuzz', desc:'Fuzz encorpado com sustain massivo e textura espessa', params:{sustain:80, tone:50, level:100}},
    {id:12, name:'MW-B7000', type:'Darkglass B7K Preamp', desc:'Pré de baixo de 12 parâmetros', params:{gain:50, level:50, blend:50, grunt:2, attack:2, master:50, bass:50, loMidFreq:2, loMid:50, hiMidFreq:2, hiMid:50, treble:50}},
    {id:13, name:'1._ADAM_RAFOW', type:'Adam Rafow High-Gain', desc:'Stompbox high-gain de 8 parâmetros', params:ds8Def()},
    {id:14, name:'2TS8', type:'Tube Screamer Mod v2', desc:'TS808 com EQ de 8 parâmetros', params:ds8Def()},
    {id:15, name:'3DS1', type:'DS-1 Mod Alto Ganho', desc:'DS-1 Mod com EQ de 8 parâmetros', params:ds8Def()},
    {id:16, name:'4DS2', type:'DS-2 Turbo Emulation', desc:'Boss DS-2 Turbo Distortion', params:ds8Def()},
    {id:17, name:'5M-VAVE_OD', type:'5M-VAVE Custom OD', desc:'Overdrive customizado M-VAVE', params:ds8Def()},
    {id:18, name:'6M-VAVE_DS', type:'6M-VAVE Custom DS', desc:'Distorção customizada M-VAVE', params:ds8Def()},
    {id:19, name:'7M-VAVE_TS1', type:'Custom TS1 Overdrive', desc:'Circuito customizado de TS v1', params:ds8Def()},
    {id:20, name:'8M-VAVE_TS2', type:'Custom TS2 Overdrive', desc:'Circuito customizado de TS v2', params:ds8Def()},
    {id:21, name:'9SUPA_1', type:'Super Overdrive v1', desc:'Circuito de overdrive encorpado v1', params:ds8Def()},
    {id:22, name:'10SUPA_2', type:'Super Overdrive v2', desc:'Circuito de overdrive encorpado v2', params:ds8Def()},
    {id:23, name:'11RAT', type:'ProCo Rat Custom', desc:'Emulação aprimorada do Rat', params:ds8Def()},
    {id:24, name:'12RAT_BT', type:'ProCo Rat Fat/Turbo', desc:'Modificação do Rat Fat/Turbo', params:ds8Def()},
    {id:25, name:'13JHS_1', type:'JHS Modded OD v1', desc:'Overdrive boutique JHS v1', params:ds8Def()},
    {id:26, name:'14JHS_2', type:'JHS Modded OD v2', desc:'Overdrive boutique JHS v2', params:ds8Def()},
    {id:27, name:'15MT_1', type:'Metal Distortion v1', desc:'Distorção high-gain metal v1', params:ds8Def()},
    {id:28, name:'16MT_2', type:'Metal Distortion v2', desc:'Distorção high-gain metal v2', params:ds8Def()},
    {id:29, name:'17TDS', type:'TDS High-Gain', desc:'Distorção moderna TDS', params:ds8Def()},
    {id:30, name:'18XC_DS', type:'18X Custom Distortion', desc:'Distorção boutique de alta definição', params:ds8Def()},
    {id:31, name:'19QC_DS', type:'QC Custom Distortion', desc:'Distorção customizada QC', params:ds8Def()},
    {id:32, name:'20HIGAIN', type:'High-Gain Monster', desc:'Distorção extrema para solos e metal', params:ds8Def()},
    {id:33, name:'21M-BOOSTER', type:'M-Booster High-Headroom', desc:'Booster de alta margem dinâmica', params:ds8Def()},
    {id:34, name:'22TS-9', type:'Ibanez TS9 Tube Screamer', desc:'Emulação clássica do Ibanez TS9', params:ds8Def()},
    {id:35, name:'23BIG-DR', type:'Big Drive Distortion', desc:'Distorção robusta Big Drive', params:ds8Def()},
    {id:36, name:'24CL_BOOST', type:'Clean Booster Transparent', desc:'Boost limpo linear', params:ds8Def()},
    {id:37, name:'25BD', type:'Bass Driver DI Overdrive', desc:'Emulação de pré/overdrive BD', params:ds8Def()},
    {id:38, name:'26M90S', type:'M90S Modern Overdrive', desc:'Overdrive contemporâneo M90S', params:ds8Def()},
    {id:39, name:'27M2000', type:'27M2000 Distortion', desc:'Distorção 27M2000', params:ds8Def()},
    {id:40, name:'28DS800', type:'DS-800 High-Output', desc:'Stompbox de alta saída DS800', params:ds8Def()},
    {id:41, name:'29DS900', type:'DS-900 Heavy Metal', desc:'Distorção pesada DS900', params:ds8Def()},
    {id:42, name:'30MAR-DS', type:'Marshall-Style Distortion', desc:'Distorção britânica estilo Marshall', params:ds8Def()},
    {id:43, name:'31BOG_DS', type:'Bogu-Style High Gain', desc:'Distorção valvulada estilo Bogner', params:ds8Def()},
    {id:44, name:'32SONDO', type:'Sondo Custom Distortion', desc:'Distorção customizada Sondo', params:ds8Def()},
    {id:45, name:'33MID-BOST', type:'Mid-Boost Lead Pedal', desc:'Pedal de destaque de médios para solos', params:ds8Def()},
    {id:46, name:'34RED_DS', type:'Red Distortion Stomp', desc:'Distorção vermelha de alta voltagem', params:ds8Def()},
    {id:47, name:'35MODEN_DS', type:'Modern High-Gain', desc:'Distorção moderna cristalina', params:ds8Def()},
    {id:48, name:'36SUPEROD', type:'Super Overdrive Custom', desc:'Circuito otimizado Super Overdrive', params:ds8Def()},
    {id:49, name:'37BLUES_DR', type:'Blues Driver Dinâmico', desc:'Overdrive sensível ao toque Blues Driver', params:ds8Def()},
    {id:50, name:'38BLACK-BOX', type:'Black Box Distortion', desc:'Distorção boutique Black Box', params:ds8Def()},
    {id:51, name:'39BIG-MUFF', type:'Big Muff Pi Fuzz Sustaining', desc:'Fuzz Big Muff com EQ de 8 parâmetros', params:ds8Def()},
    {id:52, name:'SLASH_USE_YOU', type:'Slash Hard Rock Tone', desc:'Modelagem dedicada Slash / Guns N Roses', params:ds8Def()}
  ],
  AMP: [
    {id:1, name:'1CL1_410H', type:'Clean', desc:'Marshall JVM410H • Clean britânico clássico com dinâmica natural e médios encorpados', params:ampDef('Clean')},
    {id:2, name:'2CL2_410H', type:'Clean', desc:'Marshall JVM410H • Clean brilhante e transparente com riqueza harmônica para dedilhados', params:ampDef('Clean',28,75,48,52,58,45,52,75)},
    {id:3, name:'3CR1_410H', type:'Overdrive', desc:'Marshall JVM410H • Overdrive leve, quente e macio com dinâmica responsiva ao toque', params:ampDef('Overdrive',42)},
    {id:4, name:'4CR2_410H', type:'Overdrive', desc:'Marshall JVM410H • Crunch de médios cheios, firme e potente para rock clássico', params:ampDef('Overdrive',52,68,55,64)},
    {id:5, name:'5CR3_410H', type:'Overdrive', desc:'Marshall JVM410H • Crunch de alta saturação com forte impacto para power chords e hard rock', params:ampDef('Overdrive',62)},
    {id:6, name:'6OD1_410H', type:'Overdrive', desc:'Marshall JVM410H • Overdrive britânico tradicional com sustain longo para solos expressivos', params:ampDef('Overdrive',65)},
    {id:7, name:'7OD2_410H', type:'Overdrive', desc:'Marshall JVM410H • Overdrive moderno de alto ganho com agudos cortantes para rock e metal', params:ampDef('Overdrive',70,68,55,60,65)},
    {id:8, name:'8OD3_410H', type:'Overdrive', desc:'Marshall JVM410H • High-gain com médios densos e som encorpado para banda ao vivo', params:ampDef('Overdrive',74,68,55,65)},
    {id:9, name:'9OD_AXE1', type:'Overdrive', desc:'Danelectro Wasabi AX-1 • Overdrive estilo vintage fuzz com textura arenosa de garage rock', params:ampDef('Overdrive',58)},
    {id:10, name:'10OD1_BLUE', type:'Overdrive', desc:'BOSS BD-2 Blues Driver • Overdrive clássico de blues com graves quentes e médios redondos', params:ampDef('Overdrive',45,68,58)},
    {id:11, name:'11OD2_BLUE', type:'Overdrive', desc:'BOSS BD-2 Blues Driver • Variação com médios destacados e corte afiado na mix', params:ampDef('Overdrive',50,68,55,66)},
    {id:12, name:'12DARK_MATTER1', type:'Overdrive', desc:'TC Electronic Dark Matter • Overdrive de alto ganho com timbre escuro para doom e stoner', params:ampDef('Overdrive',68,68,64,60,48)},
    {id:13, name:'13CL1_FenDi', type:'Clean', desc:'Fender 65 Deluxe Reverb • Clean Blackface clássico, doce e aveludado para surf e country', params:ampDef('Clean',30,75,52,52,64)},
    {id:14, name:'14CL2_FenDi', type:'Clean', desc:'Fender 65 Deluxe Reverb • Clean brilhante e aberto com separação nítida para fingerstyle', params:ampDef('Clean',26,75,48,52,68,45,52,80)},
    {id:15, name:'15CL3_FenDi', type:'Clean', desc:'Fender 65 Deluxe Reverb • Clean vintage macio com palco sonoro relaxado para folk e indie', params:ampDef('Clean',32,75,48,52,58,45,52,55)},
    {id:16, name:'16HORIZON_1', type:'Overdrive', desc:'Horizon Devices • Overdrive de ganho moderno compacto com graves contidos para djent', params:ampDef('Overdrive',68,68,45,60,60,55,65)},
    {id:17, name:'17HORIZON_2', type:'Overdrive', desc:'Horizon Devices • Overdrive de ritmo pesado com médios agressivos para metalcore', params:ampDef('Overdrive',72,68,55,64,60,55,65)},
    {id:18, name:'18HVMetal_1', type:'Distortion', desc:'BOSS Metal Zone • Distorção heavy metal com graves profundos e agudos cortantes', params:ampDef('Distortion',78)},
    {id:19, name:'19CL1_J900', type:'Clean', desc:'Marshall JCM900 • Clean britânico firme com compressão natural e ataque definido', params:ampDef('Clean',30)},
    {id:20, name:'20CL2_J900', type:'Clean', desc:'Marshall JCM900 • Clean quente e redondo para bases melódicas e arpejos', params:ampDef('Clean',32,75,54)},
    {id:21, name:'21CL3_J900', type:'Clean', desc:'Marshall JCM900 • Clean aberto e dinâmico com excelente resposta a pedais', params:ampDef('Clean',28)},
    {id:22, name:'22OD1_J900', type:'Overdrive', desc:'Marshall JCM900 • Overdrive hard rock anos 80/90 com médios rasgados e presença', params:ampDef('Overdrive',62)},
    {id:23, name:'23DS1_J900', type:'Distortion', desc:'Marshall JCM900 • Distorção de alto ganho para glam metal e solos virtuosos', params:ampDef('Distortion',72)},
    {id:24, name:'24DS2_J900', type:'Distortion', desc:'Marshall JCM900 • Distorção pesada moderna com sustentação prolongada', params:ampDef('Distortion',76)},
    {id:25, name:'25CL1_JC120', type:'Clean', desc:'Roland JC-120 Jazz Chorus • Clean transistorizado estéreo ultralímpido para jazz e fusion', params:ampDef('Clean',22,78,48,52,58,45,52,75)},
    {id:26, name:'26CL2_JC120', type:'Clean', desc:'Roland JC-120 Jazz Chorus • Clean clássico com modulação espacial brilhante para pop e worship', params:ampDef('Clean',25,78,48,52,58,45,52,80)},
    {id:27, name:'27OD1_JHS', type:'Overdrive', desc:'JHS Loud Is More Good • Overdrive transparente boutique que preserva a dinâmica original', params:ampDef('Overdrive',45)},
    {id:28, name:'28OD2_JHS', type:'Overdrive', desc:'JHS Loud Is More Good • Overdrive com reforço de médios que destaca a guitarra rítmica', params:ampDef('Overdrive',50,68,55,65)},
    {id:29, name:'29OD3_JHS', type:'Overdrive', desc:'JHS Loud Is More Good • Overdrive quente e suave de alta resposta para solos de blues e fusion', params:ampDef('Overdrive',55)},
    {id:30, name:'30DS_JHS', type:'Distortion', desc:'JHS Loud Is More Good • Distorção de alto ganho com ataque nítido para rock contemporâneo', params:ampDef('Distortion',68)},
    {id:31, name:'31OD1_JOYO', type:'Overdrive', desc:'JOYO JF-01 Vintage OD • Circuito clássico estilo TS com médios doces e transição suave', params:ampDef('Overdrive',48,68,55,62)},
    {id:32, name:'32DS1_JR7W', type:'Distortion', desc:'Boss KTN-MINI-W • Distorção compacta simulando mini-amp para estudos e gravação', params:ampDef('Distortion',65)},
    {id:33, name:'33OD1_JVMcrunch', type:'Overdrive', desc:'Marshall JVM410H Crunch • Crunch comprimido para palhetadas rápidas no thrash metal', params:ampDef('Overdrive',64,68,55,60,60,62)},
    {id:34, name:'34OD2_JVMcrunch', type:'Overdrive', desc:'Marshall JVM410H Crunch • Crunch de alto impacto com graves vigorosos para bases pesadas', params:ampDef('Overdrive',68,68,60,60,60,65)},
    {id:35, name:'35DS1_Kohle', type:'Distortion', desc:'Kohle HM2 Singing Mids • Timbre clássico sueco de death metal estilo serra-elétrica', params:ampDef('Distortion',80,68,55,70,68)},
    {id:36, name:'36DS2_Kohle', type:'Distortion', desc:'Kohle HM2 Singing Mids • Variação com médios controlados e graves firmes para death metal', params:ampDef('Distortion',78,68,62,55)},
    {id:37, name:'37KohleHM', type:'Distortion', desc:'Kohle HM2 Singing Mids • Ganho extremo e agressivo para grindcore e black metal', params:ampDef('Distortion',85,68,55,48,70)},
    {id:38, name:'38DS1_M-VAVE', type:'Distortion', desc:'M-VAVE Custom DS • Distorção moderna neutra e equilibrada versátil para metal', params:ampDef('Distortion',70)},
    {id:39, name:'39OD1_M-VAVE', type:'Overdrive', desc:'M-VAVE Custom DS • Overdrive digital transparente de resposta rápida para pop e rock', params:ampDef('Overdrive',46)},
    {id:40, name:'40DS2_M-VAVE', type:'Distortion', desc:'M-VAVE Custom DS • Distorção pesada com subgraves profundos para breakdowns de metalcore', params:ampDef('Distortion',76,68,64,48,60,70)},
    {id:41, name:'41CL1_M-VAVE', type:'Clean', desc:'M-VAVE Custom OD • Sinal limpo bufferizado de alta fidelidade para cadeia de efeitos', params:ampDef('Clean',20)},
    {id:42, name:'42OD2_M-VAVE', type:'Overdrive', desc:'M-VAVE Custom OD • Overdrive vintage macio e aveludado para baladas e solos retrô', params:ampDef('Overdrive',50)},
    {id:43, name:'43CL2_M-VAVE', type:'Clean', desc:'M-VAVE Custom OD • Clean ligeiramente quebrado (edge-of-breakup) para country e pop', params:ampDef('Clean',36)},
    {id:44, name:'44OD3_M-VAVE', type:'Overdrive', desc:'M-VAVE Custom OD • Overdrive de alto ganho com médios cheios próximo de distorção', params:ampDef('Overdrive',62,68,55,62)},
    {id:45, name:'45CL3_M-VAVE', type:'Clean', desc:'M-VAVE Custom OD • Clean brilhante de agudos proeminentes para captadores single-coil', params:ampDef('Clean',25,75,48,52,66,45,52,75)},
    {id:46, name:'46OD3_M-VAVE', type:'Overdrive', desc:'M-VAVE Custom OD • Overdrive de alta definição com graves contidos e separação harmônica', params:ampDef('Overdrive',58,68,45)},
    {id:47, name:'47CL4_M-VAVE', type:'Clean', desc:'M-VAVE Custom OD • Clean encorpado com ênfase no grave para passagens contrastantes', params:ampDef('Clean',28,75,62)},
    {id:48, name:'48CH1_ME', type:'Overdrive', desc:'Mesa Boogie JP-2C • Timbre clássico Mark com ataque imediato e médios refinados', params:ampDef('Overdrive',60,68,55,58)},
    {id:49, name:'49CH2_ME', type:'Overdrive', desc:'Mesa Boogie JP-2C • Canal de alto ganho com ultra-clareza para solos de prog metal', params:ampDef('Overdrive',72,68,55,62,64)},
    {id:50, name:'50DS1_metal', type:'Distortion', desc:'Blackstar HT Metal 60 • Distorção valvulada moderna de alto ganho e compressão firme', params:ampDef('Distortion',75)},
    {id:51, name:'51OD1_MS2203', type:'Overdrive', desc:'Marshall JCM800 2203 • O lendário Brown Sound explosivo do hard rock e thrash clássico', params:ampDef('Overdrive',65,68,55,64,62)},
    {id:52, name:'52OD1_NUX', type:'Overdrive', desc:'NUX Overdrive • Overdrive macio e perdoador estilo TS para blues e rock', params:ampDef('Overdrive',48)},
    {id:53, name:'53OD2_NUX', type:'Overdrive', desc:'NUX Overdrive • Overdrive de saída elevada e agudos abertos para presença ao vivo', params:ampDef('Overdrive',56,68,55,60,64)},
    {id:54, name:'54CL1_SUPA', type:'Clean', desc:'Supro 1696RT Black Magick • Clean vintage norte-americano escuro e quente para blues', params:ampDef('Clean',34,75,56,52,50)},
    {id:55, name:'55CL1_orange', type:'Clean', desc:'Orange Rockerverb 100 • Clean britânico brilhante e estalado cheio de chime', params:ampDef('Clean',30,75,48,58,62)},
    {id:56, name:'56OD1_Piranha', type:'Overdrive', desc:'Pro Co RAT Custom • Overdrive agressivo de alto ganho com corte cru para grunge e punk', params:ampDef('Overdrive',64)},
    {id:57, name:'57OD1_RAT', type:'Overdrive', desc:'Pro Co RAT Clássico • Ceifamento característico do RAT com médios destacados para garage', params:ampDef('Overdrive',58,68,55,64)},
    {id:58, name:'58OD2_RAT', type:'Overdrive', desc:'Pro Co RAT Clássico • Variação com saturação expressiva e dinâmica controlável', params:ampDef('Overdrive',60)},
    {id:59, name:'59DS1_RAT', type:'Distortion', desc:'Pro Co RAT Distortion • Distorção extrema pesada com alta compressão para hardcore punk', params:ampDef('Distortion',76)},
    {id:60, name:'60DS1_ROOM', type:'Distortion', desc:'MPF Sounds Room • Distorção com reverberação espacial natural para pós-rock', params:ampDef('Distortion',66)},
    {id:61, name:'61DS2_ROOM', type:'Distortion', desc:'MPF Sounds Room • Médios densos que mantêm peso uniforme em baixos volumes', params:ampDef('Distortion',68,68,55,62)},
    {id:62, name:'62DS3_ROOM', type:'Distortion', desc:'MPF Sounds Room • Timbre encorpado de ambiente com sensação de amp no quarto', params:ampDef('Distortion',70,68,60)},
    {id:63, name:'63CL_WANG', type:'Clean', desc:'Wangs VT-18 • Clean valvulado de baixa potência britânico, dinâmico e quente', params:ampDef('Clean',32)},
    {id:64, name:'64DS1_XC', type:'Distortion', desc:'Bogner XTC Ecstasy • Distorção high-gain tridimensional para metal progressivo e melódico', params:ampDef('Distortion',74,68,55,58,60,65,65)},
    {id:65, name:'65DS2_XC', type:'Distortion', desc:'Bogner XTC Ecstasy • Distorção ultra-pesada com graves contidos e foco no ritmo', params:ampDef('Distortion',78,68,48,48,60,65,68)},
    {id:66, name:'66CL1_yuimer', type:'Clean', desc:'YUIMER OD Clean • Sinal limpo transparente sem perdas para base de modulações', params:ampDef('Clean',24)},
    {id:67, name:'67CL2_yuimer', type:'Clean', desc:'YUIMER OD Clean • Clean quente e macio para melodias de pop e rock acústico/elétrico', params:ampDef('Clean',28,75,54)},
    {id:68, name:'68OD1_yuimer', type:'Overdrive', desc:'YUIMER OD Overdrive • Overdrive aveludado e musical com médios redondos para solos', params:ampDef('Overdrive',50,68,55,60)},
    {id:69, name:'69OD1_meas', type:'Overdrive', desc:'Mesa Boogie Triple Rectifier • High-gain americano com ataque percussivo e graves poderosos', params:ampDef('Overdrive',70,68,62,60,60,68)},
    {id:70, name:'70OD2_meas', type:'Overdrive', desc:'Mesa Boogie Triple Rectifier • Overdrive de alta compressão projetado para palm mutes', params:ampDef('Overdrive',74,68,60,60,60,70)},
    {id:71, name:'71CL1_meas', type:'Clean', desc:'Mesa Boogie Triple Rectifier • Clean sólido americano de alta dinâmica com breakup natural', params:ampDef('Clean',30)},
    {id:72, name:'72CL2_meas', type:'Clean', desc:'Mesa Boogie Triple Rectifier • Clean brilhante de agudos estendidos para melodias nítidas', params:ampDef('Clean',26,75,48,52,65,45,52,75)},
    {id:73, name:'73CR1_meas', type:'Overdrive', desc:'Mesa Boogie Triple Rectifier • Crunch clássico americano na transição entre clean e high-gain', params:ampDef('Overdrive',54)},
    {id:74, name:'74OD_ENGL_120', type:'Overdrive', desc:'ENGL Savage 120 • Overdrive alemão de corte cirúrgico e rico em harmônicos para speed metal', params:ampDef('Overdrive',68,68,55,62,60,55,66)},
    {id:75, name:'75DS_ENGL_120', type:'Distortion', desc:'ENGL Savage 120 • Distorção alemã extrema de alto ganho para metal moderno e death metal', params:ampDef('Distortion',78,68,55,52,65,68,68)},
    {id:76, name:'76CL_SoldSolo100', type:'Clean', desc:'Soldano SLO-100 • Clean boutique americano cristalino e dinâmico para pedais', params:ampDef('Clean',28,75,48,52,58,45,52,70)},
    {id:77, name:'77OD_SoldSolo100', type:'Overdrive', desc:'Soldano SLO-100 • O famoso crunch dourado do SLO, aveludado e rico para AOR e hard rock', params:ampDef('Overdrive',58,68,55,62)},
    {id:78, name:'78DS_SoldSolo100', type:'Distortion', desc:'Soldano SLO-100 • A referência mundial de alto ganho com definição impecável corda a corda', params:ampDef('Distortion',74,68,55,58,64,65,65)},
    {id:79, name:'79OD_5150III', type:'Overdrive', desc:'EVH 5150 III • Assinatura explosiva de Eddie Van Halen com resposta rápida para tapping', params:ampDef('Overdrive',66,68,55,60,64,55,65)},
    {id:80, name:'80DS_5150III', type:'Distortion', desc:'EVH 5150 III • Padrão do metalcore moderno com graves secos e agudos articulados', params:ampDef('Distortion',76,68,56,48,65,68,68)},
    {id:81, name:'81CL_VOSAC30', type:'Clean', desc:'VOX AC30 Top Boost • Circuito britânico Classe A com harmônicos cintilantes (chime)', params:ampDef('Clean',32,75,48,52,66,45,52,80)},
    {id:82, name:'82OD_VOSAC30', type:'Overdrive', desc:'VOX AC30 Top Boost • Overdrive orgânico doce com médios cortantes do britpop e indie', params:ampDef('Overdrive',54,68,55,62,64,55,52,60)},
    {id:83, name:'83CL_DiezzIVH4', type:'Clean', desc:'Diezel VH4 • Clean alemão de precisão, tridimensional e hi-fi para gravações técnicas', params:ampDef('Clean',26)},
    {id:84, name:'84OD_DiezzIVH4', type:'Overdrive', desc:'Diezel VH4 • Overdrive firme com médios consistentes e agudos polidos para rock progressivo', params:ampDef('Overdrive',62,68,55,60)},
    {id:85, name:'85DS_DiezzIVH4', type:'Distortion', desc:'Diezel VH4 (Canal 3/4) • O suprassumo do metal pesado alemão com foco em afinações graves e djent', params:ampDef('Distortion',78,68,58,48,60,72,68)},
    {id:86, name:'86OD_JCM800', type:'Overdrive', desc:'Marshall JCM800 • Crunch britânico direto e focado, padrão de ouro do rock clássico e heavy metal', params:ampDef('Overdrive',64,68,55,66,62,58,60)},
    {id:87, name:'87DS_JCM800', type:'Distortion', desc:'Marshall JCM800 Modded • Alto ganho britânico modificado com médios agressivos para thrash', params:ampDef('Distortion',75,68,55,64,64)},
    {id:88, name:'88OD_Peavey6505', type:'Overdrive', desc:'Peavey 6505 Rhythm • Overdrive com graves musculosos e ataque percussivo para metalcore', params:ampDef('Overdrive',66,68,60,60,60,65)},
    {id:89, name:'89DS_Peavey6505', type:'Distortion', desc:'Peavey 6505 Lead • Distorção padrão da cena de metal moderno com altíssima sustentação', params:ampDef('Distortion',78,68,58,48,60,68,68)},
    {id:90, name:'90DS1_TONE', type:'Distortion', desc:'IK Multimedia TONEX • Modelagem de alta fidelidade neutra cobrindo timbres vintage a modernos', params:ampDef('Distortion',68)},
    {id:91, name:'91AgB659_BS', type:'Bass', desc:'Aguilar DB659 • Pré-amp clássico de contrabaixo americano valvulado, quente e sedoso', params:ampDef('Bass',32,78,64)},
    {id:92, name:'92Ag500_BS', type:'Bass', desc:'Aguilar AG500 • Contrabaixo moderno e veloz com graves limpos e médios destacados para slap', params:ampDef('Bass',30,78,66,58,60)},
    {id:93, name:'93Alm1X_BS', type:'Bass', desc:'Alembic F-1X • Pré-amplificador hi-end cristalino e ultra-detalhado para estúdio e jazz', params:ampDef('Bass',26,78,65,52,52,70,55,60)},
    {id:94, name:'94Ap50_BS', type:'Bass', desc:'Darkglass Alpha Omega 500 • Distorção moderna pesada de baixo com subgraves viscerais para djent', params:ampDef('Bass',58,78,68,52,62,72)},
    {id:95, name:'95Mar74_BS', type:'Bass', desc:'Marshall DBS7400 • Baixo de rock britânico com médios projetados e pegada agressiva', params:ampDef('Bass',42,78,65,62)},
    {id:96, name:'96PeBC1_Bs', type:'Bass', desc:'Pearce BC1 • Timbre vintage americano de contrabaixo, elástico e quente para soul e R&B', params:ampDef('Bass',34,78,64)},
    {id:97, name:'97Th21B_BS', type:'Bass', desc:'Tech21 Bass Driver DI • Clássica caixa de injeção direta de baixo com overdrive pronto para palco', params:ampDef('Bass',45,78,65,52,58)},
    {id:98, name:'98Th21VT_BS', type:'Bass', desc:'Tech21 VT Bass Deluxe • Simulação de amp valvulado de baixo estilo vintage americano', params:ampDef('Bass',44,78,65,58)},
    {id:99, name:'99Wal12_BS', type:'Bass', desc:'Walter Woods Ultra 1200 • Amplificador hi-fi ultralímpido para baixos acústicos e fretless', params:ampDef('Bass',25,78,65,52,52,70,55,55)},
    {id:100, name:'100WwL10_BS', type:'Bass', desc:'Warwick LWA 1000 • Contrabaixo moderno alemão com graves sólidos e articulados', params:ampDef('Bass',32,78,66)},
    {id:101, name:'101AgDb750_BS', type:'Bass', desc:'Aguilar DB750 • Amplificador híbrido potente com graves profundos e médios aveludados', params:ampDef('Bass',35,78,68)},
    {id:102, name:'102ApSVT_BS', type:'Bass', desc:'AMPEG SVT-400T • O som clássico valvulado de baixo do Ampeg, gordo e com pressão', params:ampDef('Bass',40,78,68,58,52,72)},
    {id:103, name:'103DgM900_BS', type:'Bass', desc:'Darkglass Microtubes 900 • Distorção pesada de contrabaixo com definição cirúrgica para metal', params:ampDef('Bass',56,78,66,52,62)},
    {id:104, name:'104FenRum_BS', type:'Bass', desc:'Fender Rumble 100 • Clean tradicional americano para contrabaixo, aberto e equilibrado', params:ampDef('Bass',30,78,62)},
    {id:105, name:'105GkF550_BS', type:'Bass', desc:'Gallien-Krueger Fusion 550 • Timbre versátil moderno com agudos pontiagudos para slap', params:ampDef('Bass',34,78,65,52,64,70,62)},
    {id:106, name:'106HkeHd50_BS', type:'Bass', desc:'Hartke HD50 • Timbre brilhante e focado com cone de alumínio para pop, slap e estúdio', params:ampDef('Bass',32,78,65,52,62,70,55,60)},
    {id:107, name:'107MarkLm_BS', type:'Bass', desc:'Markbass Little Mark 250 • Timbre italiano leve e articulado com graves secos e médios presentes', params:ampDef('Bass',32,78,65,60)},
    {id:108, name:'108OrgAd_BS', type:'Bass', desc:'Orange AD200B • Baixo valvulado britânico vintage, rugoso e encorpado para stoner e grunge', params:ampDef('Bass',48,78,66,62)},
    {id:109, name:'109PjBuddy_BS', type:'Bass', desc:'Phil Jones Bass Buddy • Hi-Fi puro e delicado para gravações acústicas e baixos de 6 cordas', params:ampDef('Bass',26)},
    {id:110, name:'110RolDb_BS', type:'Bass', desc:'Roland D-Bass 210 • Tecnologia digital neutra e equilibrada com alta margem dinâmica', params:ampDef('Bass',30)},
    {id:111, name:'111Mb400C1_BS', type:'Bass', desc:'Mesa Boogie Bass 400+ • Timbre valvulado americano de alta fidelidade, caloroso e espesso', params:ampDef('Bass',36,78,66)},
    {id:112, name:'112Mb400C2_BS', type:'Bass', desc:'Mesa Boogie Bass 400+ • Canal com agudos afiados e médios definidos para metal progressivo', params:ampDef('Bass',46,78,65,62,62)},
    {id:113, name:'113DgXu_BS', type:'Bass', desc:'Darkglass X Ultra • Distorção multibanda para baixo separando graves limpos de médios distorcidos', params:ampDef('Bass',58,78,68,52,64)},
    {id:114, name:'114ApSp_BS', type:'Bass', desc:'Ampeg BA-210SP • Clássico timbre Ampeg de ensaio, encorpado e musical para pop e funk', params:ampDef('Bass',34,78,64)},
    {id:115, name:'115Mar50_BS', type:'Bass', desc:'Marshall SuperBass • Baixo de rock clássico britânico direto e agressivo dos anos 70', params:ampDef('Bass',45,78,65,62)},
    {id:116, name:'116Mark500_BS', type:'Bass', desc:'Markbass TTE500 • Válvula clássica combinada com resposta rápida transistorizada', params:ampDef('Bass',36,78,64)},
    {id:117, name:'117PjbCub_BS', type:'Bass', desc:'Phil Jones Bass Cub 100 • Ultra-portátil de alta fidelidade para ensaios acústicos', params:ampDef('Bass',28)},
    {id:118, name:'118Tc21Vt_BS', type:'Bass', desc:'Tech21 VT Bass 500 • Overdrive de baixo simulando válvulas com elasticidade para vintage rock', params:ampDef('Bass',44,78,64,60)},
    {id:119, name:'119WatMod_BS', type:'Bass', desc:'Hiwatt DR103 Mod • Contrabaixo britânico vintage de headroom colossal, firme e limpo', params:ampDef('Bass',32,78,64,52,52,70)},
    {id:120, name:'120GKL800_BS', type:'Bass', desc:'Gallien Krueger 800RB • Amplificador de baixo dos anos 80/90 com médios cortantes e ataque rápido', params:ampDef('Bass',38,78,65,64,62)}
  ],
  CAB: [
    {id:1, name:'1BogUber412A', type:'4x12', desc:'Bogner Ubercab 412 • Shure SM57 (Dinâmico clássico)', params:cabDef('4x12')},
    {id:2, name:'2BogUber412B', type:'4x12', desc:'Bogner Ubercab 412 • Sennheiser MD421 (Dinâmico encorpado)', params:cabDef('4x12')},
    {id:3, name:'3BogUber412C', type:'4x12', desc:'Bogner Ubercab 412 • Beyerdynamic M160 (Fita quente)', params:cabDef('4x12')},
    {id:4, name:'4BogUber412D', type:'4x12', desc:'Bogner Ubercab 412 • Sennheiser MD421 (Posição alternativa)', params:cabDef('4x12')},
    {id:5, name:'5DizFL412A', type:'4x12', desc:'Diezel Front Loaded 412 • Shure SM57 (Ataque rápido)', params:cabDef('4x12')},
    {id:6, name:'6DizFL412B', type:'4x12', desc:'Diezel Front Loaded 412 • Sennheiser MD421 (Graves secos)', params:cabDef('4x12')},
    {id:7, name:'7DizFL412C', type:'4x12', desc:'Diezel Front Loaded 412 • Beyerdynamic M160 (Médios suaves)', params:cabDef('4x12')},
    {id:8, name:'8EngXXL412', type:'4x12', desc:'ENGL V30 412 XXL • Sennheiser MD421 (Punch pesado)', params:cabDef('4x12')},
    {id:9, name:'9EvStright412', type:'4x12', desc:'EVH 5150 Straight 412 • Neumann U87 (Condensador Hi-End)', params:cabDef('4x12')},
    {id:10, name:'10FdTR65R212A', type:'2x12', desc:'Fender Twin Reverb 65 Reissue • Neumann U87 (Limpo cristalino)', params:cabDef('2x12')},
    {id:11, name:'11FdTR65R212B', type:'2x12', desc:'Fender Twin Reverb 65 Reissue • Shure SM57 (Estalado clássico)', params:cabDef('2x12')},
    {id:12, name:'12FdBmanTw410', type:'4x10', desc:'Fender Bassman Tweed 410 • Shure SM57 (Médios vintage)', params:cabDef('4x10')},
    {id:13, name:'13FdDelux112A', type:'2x12', desc:'Fender Deluxe 112/212 • Shure SM57 (Dinâmica de blues)', params:cabDef('2x12')},
    {id:14, name:'14FdDelux112B', type:'1x12', desc:'Fender Deluxe 112 • Sennheiser MD421 (Caloroso)', params:cabDef('1x12')},
    {id:15, name:'15FdDBface112', type:'1x12', desc:'Fender Deluxe Blackface 112 • Neumann U87 (Palco sonoro aberto)', params:cabDef('1x12')},
    {id:16, name:'16FdDTweed112', type:'1x12', desc:'Fender Deluxe Tweed 112 • Shure SM57 (Compressão de cone)', params:cabDef('1x12')},
    {id:17, name:'17FdPrinc110A', type:'1x10', desc:'Fender Princeton 110 • Shure SM57 (Gravação compacta)', params:cabDef('1x10')},
    {id:18, name:'18FdPrinc110B', type:'1x10', desc:'Fender Princeton 110 • Neumann U87 (Rico em harmônicos)', params:cabDef('1x10')},
    {id:19, name:'19FdTremol210', type:'2x10', desc:'Fender Tremolux 210 • Sennheiser MD421 (Articulação ágil)', params:cabDef('2x10')},
    {id:20, name:'20FdTwRev212A', type:'2x12', desc:'Fender Twin Reverb 212 • Shure SM57 (Padrão de palco)', params:cabDef('2x12')},
    {id:21, name:'21FdTwRev212B', type:'2x12', desc:'Fender Twin Reverb 212 • Neumann U87 (Equilíbrio estéreo)', params:cabDef('2x12')},
    {id:22, name:'22Fman212A', type:'2x12', desc:'Friedman 212 Vintage • Shure SM57 (Focado em rock pesado)', params:cabDef('2x12')},
    {id:23, name:'23Fman212B', type:'2x12', desc:'Friedman 212 Vintage • Sennheiser MD421 (Graves estendidos)', params:cabDef('2x12')},
    {id:24, name:'24GojaCabA', type:'Custom', desc:'GojaCab Custom Metal A • Multi-Mic (Timbre pesado para Djent)', params:cabDef('Custom')},
    {id:25, name:'25GojaCabB', type:'Custom', desc:'GojaCab Custom Metal B • Multi-Mic (Corte agressivo na mix)', params:cabDef('Custom')},
    {id:26, name:'26HesModn212A', type:'2x12', desc:'Hesu Modern M212 Demon • Shure SM57 (Modern Metalcore)', params:cabDef('2x12')},
    {id:27, name:'27HesModn212B', type:'2x12', desc:'Hesu Modern M212 Demon • Neumann U87 (Alta definição e punch)', params:cabDef('2x12')},
    {id:28, name:'28HiwSE412A', type:'4x12', desc:'Hiwatt SE4123F (Fane) • Sennheiser MD421 (Clean britânico imponente)', params:cabDef('4x12')},
    {id:29, name:'29HiwSE412B', type:'4x12', desc:'Hiwatt SE4123F (Fane) • Shure SM57 (Médios penetrantes)', params:cabDef('4x12')},
    {id:30, name:'30HiwSE412C', type:'4x12', desc:'Hiwatt SE4123F (Fane) • Beyerdynamic M160 (Clássico The Who / Gilmour)', params:cabDef('4x12')},
    {id:31, name:'31HughKe4x12A', type:'4x12', desc:'Hughes & Kettner TC 412 • Shure SM57 (Alta resposta dinâmica)', params:cabDef('4x12')},
    {id:32, name:'32HughKe4x12B', type:'4x12', desc:'Hughes & Kettner TC 412 • Sennheiser MD421 (Graves articulados)', params:cabDef('4x12')},
    {id:33, name:'33Mar60A412A', type:'4x12', desc:'Marshall 1960A Slanted (G12T75) • Sennheiser MD421 (Padrão Hard Rock)', params:cabDef('4x12')},
    {id:34, name:'34Mar60A412B', type:'4x12', desc:'Marshall 1960A Slanted (G12T75) • Sennheiser MD421 (Angulação alternativa)', params:cabDef('4x12')},
    {id:35, name:'35Mar60A412C', type:'4x12', desc:'Marshall 1960A Slanted (G12T75) • Beyerdynamic M160 (Sedoso e encorpado)', params:cabDef('4x12')},
    {id:36, name:'36Mar60B412A', type:'4x12', desc:'Marshall 1960B Straight • Neumann U87 (Graves profundos firmes)', params:cabDef('4x12')},
    {id:37, name:'37Mar60B412B', type:'4x12', desc:'Marshall 1960B Straight • Shure SM57 (Mordida clássica)', params:cabDef('4x12')},
    {id:38, name:'38Mar60B412C', type:'4x12', desc:'Marshall 1960B Straight • Sennheiser MD421 (Equilibrado para bases)', params:cabDef('4x12')},
    {id:39, name:'39Mar35Bac412', type:'4x12', desc:'Marshall 1935B Alnico Cream • Beyerdynamic M160 (Doçura vintage única)', params:cabDef('4x12')},
    {id:40, name:'40Mar60TV412', type:'4x12', desc:'Marshall 1960 TV (Tall Vintage) • Shure SM57 (Ressonância ampla)', params:cabDef('4x12')},
    {id:41, name:'41Mar2551B412', type:'4x12', desc:'Marshall 2551BV Silver Jubilee • Neumann U87 (Vintage 30 britânico puro)', params:cabDef('4x12')},
    {id:42, name:'42MatCSig212A', type:'2x12', desc:'Matchless Chieftain 212 • Shure SM57 (Boutique chime definido)', params:cabDef('2x12')},
    {id:43, name:'43MatCSig212B', type:'2x12', desc:'Matchless Chieftain 212 • Neumann U87 (Espacialidade e ar)', params:cabDef('2x12')},
    {id:44, name:'44Mat30DC212A', type:'2x12', desc:'Matchless DC30 212 • Shure SM57 (Articulação de classe A)', params:cabDef('2x12')},
    {id:45, name:'45Mat30DC212B', type:'2x12', desc:'Matchless DC30 212 • Sennheiser MD421 (Médios doces e densos)', params:cabDef('2x12')},
    {id:46, name:'46MbOS412A', type:'4x12', desc:'Mesa Boogie Recto Oversized 412 • Shure SM57 (Timbre definitivo de Metal)', params:cabDef('4x12')},
    {id:47, name:'47MbOS412B', type:'4x12', desc:'Mesa Boogie Recto Oversized 412 • Sennheiser MD421 (Graves gigantes e firmes)', params:cabDef('4x12')},
    {id:48, name:'48MbSD212A', type:'2x12', desc:'Mesa Boogie Standard 212 • Shure SM57 (Compacto e versátil)', params:cabDef('2x12')},
    {id:49, name:'49MbSD212B', type:'2x12', desc:'Mesa Boogie Standard 212 • Neumann U87 (Equilíbrio para estúdio)', params:cabDef('2x12')},
    {id:50, name:'50MbOsAngl412', type:'4x12', desc:'Mesa Boogie Oversize Angled 2003 • Sennheiser MD421 (Referência Nu-Metal)', params:cabDef('4x12')},
    {id:51, name:'51MbRecLed212', type:'2x12', desc:'Mesa Boogie Rectifier Legend 212 • Shure SM57 (Ataque cirúrgico)', params:cabDef('2x12')},
    {id:52, name:'52MbRecVin212', type:'2x12', desc:'Mesa Boogie Rectifier 212 (V30) • Neumann U87 (Gravação rica em detalhes)', params:cabDef('2x12')},
    {id:53, name:'53MbSOSAn412', type:'4x12', desc:'Mesa Boogie Standard OS Angled • Neumann U87 (Projeção angular massiva)', params:cabDef('4x12')},
    {id:54, name:'54MbSOSS4t412', type:'4x12', desc:'Mesa Boogie Standard OS Straight • Shure SM57 (Graves de alta pressão)', params:cabDef('4x12')},
    {id:55, name:'55MbTradAn412', type:'4x12', desc:'Mesa Boogie Traditional Angled • Sennheiser MD421 (Médios focados e rápidos)', params:cabDef('4x12')},
    {id:56, name:'56MbTradS412A', type:'4x12', desc:'Mesa Boogie Traditional Straight • Shure SM57 (Resposta precisa de palheta)', params:cabDef('4x12')},
    {id:57, name:'57MbTradS412B', type:'4x12', desc:'Mesa Boogie Traditional Straight • Sennheiser MD421 (Encorpado para bases)', params:cabDef('4x12')},
    {id:58, name:'58OgPPC412A', type:'4x12', desc:'Orange PPC 412 (Madeira densa) • Shure SM57 (Médios encorpados britânicos)', params:cabDef('4x12')},
    {id:59, name:'59OgPPC412B', type:'4x12', desc:'Orange PPC 412 (Madeira densa) • Sennheiser MD421 (Graves quentes de stoner rock)', params:cabDef('4x12')},
    {id:60, name:'60PinilCab', type:'Custom', desc:'PinilCab Custom Studio • Multi-Mic (Otimização moderna)', params:cabDef('Custom')},
    {id:61, name:'61RJ120azz212', type:'2x12', desc:'Roland JC-120 Factory 212 • Neumann U87 (Clean plano de alta precisão)', params:cabDef('2x12')},
    {id:62, name:'62SurCab212', type:'2x12', desc:'Suhr PT15 Pete Thorn 212 • Shure SM57 (Equilíbrio boutique supremo)', params:cabDef('2x12')},
    {id:63, name:'63VAC15ox112', type:'1x12', desc:'Vox AC15 112 (Celestion Blue) • Shure SM57 (Brilho harmônico doce)', params:cabDef('1x12')},
    {id:64, name:'64VAC30TBC212', type:'2x12', desc:'Vox AC30 TopBoost 212 (Alnico) • Sennheiser MD421 (Chime clássico britânico)', params:cabDef('2x12')},
    {id:65, name:'65VAC30ox212', type:'2x12', desc:'Vox AC30 212 (Celestion Blue) • Shure SM57 (Mordida cortante para solos)', params:cabDef('2x12')},
    {id:66, name:'66ZillCab212', type:'2x12', desc:'Zilla Cabs Custom 212 • Shure SM57 (Favorito de gravações modernas)', params:cabDef('2x12')},
    {id:67, name:'67ZillCust412', type:'4x12', desc:'Zilla Custom 412 • Sennheiser MD421 (Potência de turnê)', params:cabDef('4x12')},
    {id:68, name:'68ZillFboy212', type:'2x12', desc:'Zilla Fatboy 2002 212 • Neumann U87 (Caixa profunda super encorpada)', params:cabDef('2x12')},
    {id:69, name:'69ZillMiMo112', type:'1x12', desc:'Zilla Mini Modern 112 • Shure SM57 (Foco e agilidade de estúdio)', params:cabDef('1x12')},
    {id:70, name:'70ZillOpen212', type:'2x12', desc:'Zilla Open Back 212 • Beyerdynamic M160 (Traseira aberta surround)', params:cabDef('2x12')},
    {id:71, name:'71GisonHubird', type:'Acoustic', desc:'Gibson Hummingbird Dreadnought • Neumann U87 (Simulação acústica quente)', params:cabDef('Acoustic')},
    {id:72, name:'72LakewdDCP', type:'Acoustic', desc:'Lakewood D-18CP Grand Concert • Neumann U87 (Clareza de fingerstyle europeu)', params:cabDef('Acoustic')},
    {id:73, name:'73TakmiENPU', type:'Acoustic', desc:'Takamine EN10 Electro-Acoustic • Neumann U87 (Resposta percussiva para palco)', params:cabDef('Acoustic')},
    {id:74, name:'74TaylrOcE', type:'Acoustic', desc:'Taylor CE414 Grand Auditorium • Shure SM57 (Brilho moderno articulado)', params:cabDef('Acoustic')},
    {id:75, name:'75YamhaFGX', type:'Acoustic', desc:'Yamaha FGX412 Folk Acoustic • Shure SM57 (Corpo sólido e presença acústica)', params:cabDef('Acoustic')},
    {id:76, name:'76Acous215', type:'Bass 2x15', desc:'Acoustic 215 Vintage Bass Cab • Sennheiser MD421 (Subgraves massivos clássicos)', params:cabDef('Bass 2x15')},
    {id:77, name:'77AmpgSVT115', type:'Bass 1x15', desc:'Ampeg SVT-115E Bass Enclosure • Neumann U87 (Calibre aveludado e fundo musical)', params:cabDef('Bass 1x15')},
    {id:78, name:'78AmpgSVT410', type:'Bass 4x10', desc:'Ampeg SVT-410HLF (com corneta) • Sennheiser MD421 (Punch rápido e estalo na mix)', params:cabDef('Bass 4x10')},
    {id:79, name:'79AmpgSVT412', type:'Bass 4x12', desc:'Ampeg SVT-412HE • Neumann U87 (Médios viscerais de rock n roll)', params:cabDef('Bass 4x12')},
    {id:80, name:'80AmpgSVT810', type:'Bass 8x10', desc:'Ampeg SVT-810E (A famosa Geladeira) • Sennheiser MD421 (Lenda de palcos)', params:cabDef('Bass 8x10')},
    {id:81, name:'81AshdowRM212', type:'Bass 2x12', desc:'Ashdown Rootmaster EVO II 212 • Beyerdynamic M160 (Médios britânicos redondos)', params:cabDef('Bass 2x12')},
    {id:82, name:'82Bareface110', type:'Bass 1x10', desc:'Barefaced One 10T 110 • Sennheiser MD421 (Hi-Fi ultraleve boutique)', params:cabDef('Bass 1x10')},
    {id:83, name:'83DgD210C210', type:'Bass 2x10', desc:'Darkglass DG210C 210 • Shure SM57 (Corte de distorção moderna)', params:cabDef('Bass 2x10')},
    {id:84, name:'84DgDG212N212', type:'Bass 2x12', desc:'Darkglass DG212N Neodymium 212 • Sennheiser MD421 (Graves rápidos para Drop)', params:cabDef('Bass 2x12')},
    {id:85, name:'85DavEendD410', type:'Bass 4x10', desc:'David Eden D410XLT • Neumann U87 (Agudos cristalinos para slap)', params:cabDef('Bass 4x10')},
    {id:86, name:'86FdBmanSf210', type:'Bass 2x10', desc:'Fender Bassman Silverface 210 • Shure SM57 (Vintage soul e motown)', params:cabDef('Bass 2x10')},
    {id:87, name:'87FdBmanNo410', type:'Bass 4x10', desc:'Fender Bassman 410 PRO • Sennheiser MD421 (Firmeza equilibrada)', params:cabDef('Bass 4x10')},
    {id:88, name:'88FdRumble210', type:'Bass 2x10', desc:'Fender Rumble 210 V3 • Shure SM57 (Resposta linear para pop/rock)', params:cabDef('Bass 2x10')},
    {id:89, name:'89GK410RB410A', type:'Bass 4x10', desc:'Gallien-Krueger 410RBH (Horn ativo) • Shure SM57 (Clássico ataque funk/rock)', params:cabDef('Bass 4x10')},
    {id:90, name:'90GK410RB410B', type:'Bass 4x10', desc:'Gallien-Krueger 410RBH • Neumann U87 (Graves cheios e naturais)', params:cabDef('Bass 4x10')},
    {id:91, name:'91Har410xl410', type:'Bass 4x10', desc:'Hartke 410XL (Cone de Alumínio) • Sennheiser MD421 (Ataque metálico inconfundível)', params:cabDef('Bass 4x10')},
    {id:92, name:'92Mar1935A412', type:'Bass 4x12', desc:'Marshall 1935A Bass 412 • Shure SM57 (Rock setentista e hard rock)', params:cabDef('Bass 4x12')},
    {id:93, name:'93MbSubway210', type:'Bass 2x10', desc:'Mesa Boogie Subway Ultra-Lite 210 • Sennheiser MD421 (Potente e veloz de neodímio)', params:cabDef('Bass 2x10')},
    {id:94, name:'94MbPhouse210', type:'Bass 2x10', desc:'Mesa Boogie PowerHouse 210 • Neumann U87 (Paredão sonoro maciço)', params:cabDef('Bass 2x10')},
    {id:95, name:'95OgV30PPC212', type:'Bass 2x12', desc:'Orange PPC 212 Bass/Guitar Hybrid • Shure SM57 (Distorção de baixo pesada)', params:cabDef('Bass 2x12')},
    {id:96, name:'96Pey115BX115', type:'Bass 1x15', desc:'Peavey 115BX Black Widow 115 • Shure SM57 (Clássico indestrutível de palco)', params:cabDef('Bass 1x15')},
    {id:97, name:'97Sun19L215', type:'Bass 2x15', desc:'Sunn Vintage 1970s 215 (JBL) • Sennheiser MD421 (Doom, fuzz e vibração física)', params:cabDef('Bass 2x15')},
    {id:98, name:'98SWorkman410', type:'Bass 4x10', desc:'SWR Workingman 410 • Shure SM57 (Médios abertos e clareza para slap)', params:cabDef('Bass 4x10')},
    {id:99, name:'99TaceEll412', type:'Bass 4x12', desc:'Trace Elliot 412 Bass Cab • Sennheiser MD421 (Definição rápida britânica)', params:cabDef('Bass 4x12')},
    {id:100, name:'100TaceEll118', type:'Bass 1x18', desc:'Trace Elliot 1818T 118 Subwoofer • Neumann U87 (Subgraves monumentais de 18 pol)', params:cabDef('Bass 1x18')}
  ],
  EQ: [
    {id:1, name:'Guitar EQ 6', type:'6 Bandas (-12 a +12 dB)', desc:'Equalizador de 6 bandas para guitarra (100Hz a 3.2kHz, Min=-12.0, Max=+12.0 dB)', params:{'100Hz':0, '200Hz':0, '400Hz':0, '800Hz':0, '1.6kHz':0, '3.2kHz':0}},
    {id:2, name:'Bass EQ 7', type:'7 Bandas (-12 a +12 dB)', desc:'Equalizador de 7 bandas para contrabaixo (50Hz a 10k, Min=-12.0, Max=+12.0 dB)', params:{'50Hz':0, '120Hz':0, '400Hz':0, '500Hz':0, '800Hz':0, '4.5kHz':0, '10k':0}},
    {id:3, name:'Normal EQ 10', type:'10 Bandas (-12 a +12 dB)', desc:'Equalizador gráfico de 10 bandas (31.25Hz a 16kHz, Min=-12.0, Max=+12.0 dB)', params:{'31.25Hz':0, '62.5Hz':0, '125Hz':0, '250Hz':0, '500Hz':0, '1kHz':0, '2kHz':0, '4kHz':0, '8kHz':0, '16kHz':0}}
  ],
  MOD: [
    {id:1, name:'CHORUS', type:'Espacial', desc:'speed (0.1..10.0), depth (0..100), mix (0..100), sync, syncBpm — Chorus clássico', params:{speed:1.5, depth:45, mix:35, sync:0, syncBpm:80}},
    {id:2, name:'TRI CHORUS', type:'Espacial', desc:'speed (0.1..10.0), depth (0..100), mix (0..100), sync, syncBpm — Triple Chorus tridimensional', params:{speed:1.2, depth:50, mix:40, sync:0, syncBpm:80}},
    {id:3, name:'FLANGER', type:'Modulação', desc:'speed (0.1..10.0), depth, fb, mix, sync, syncBpm — Flanger clássico', params:{speed:1.5, depth:50, fb:45, mix:40, sync:0, syncBpm:80}},
    {id:4, name:'TRI FLANGER', type:'Modulação', desc:'speed (0.1..10.0), depth, fb, mix, sync, syncBpm — Triple Flanger', params:{speed:1.5, depth:50, fb:45, mix:40, sync:0, syncBpm:80}},
    {id:5, name:'TREMOLO', type:'Dinâmica', desc:'speed (0.1..10.0), depth, level, sync, syncBpm — Tremolo clássico', params:{speed:3.0, depth:50, level:70, sync:0, syncBpm:80}},
    {id:6, name:'TRI TREMOLO', type:'Dinâmica', desc:'speed (0.1..10.0), depth, level, sync, syncBpm — Triple Tremolo', params:{speed:3.0, depth:50, level:70, sync:0, syncBpm:80}},
    {id:7, name:'OPTO TREMOLO', type:'Dinâmica', desc:'speed (0.1..10.0), depth, level, sync, syncBpm — Tremolo óptico vintage', params:{speed:3.0, depth:50, level:70, sync:0, syncBpm:80}},
    {id:8, name:'PHASER', type:'Fase', desc:'speed (0.1..10.0), midcut, reso, fb, sync, syncBpm — Phaser analógico', params:{speed:1.2, midcut:50, reso:50, fb:40, sync:0, syncBpm:80}},
    {id:9, name:'VIBRATO', type:'Modulação', desc:'speed (0.1..10.0), depth, sync, syncBpm — Vibrato de afinação', params:{speed:3.0, depth:40, sync:0, syncBpm:80}},
    {id:10, name:'TRI VIBRATO', type:'Modulação', desc:'speed (0.1..10.0), depth, sync, syncBpm — Triple Vibrato', params:{speed:3.0, depth:40, sync:0, syncBpm:80}},
    {id:11, name:'OPTO VIBRATO', type:'Modulação', desc:'speed (0.1..10.0), depth, sync, syncBpm — Vibrato óptico', params:{speed:3.0, depth:40, sync:0, syncBpm:80}},
    {id:12, name:'UNIVIBE', type:'Psicodélico', desc:'speed (0.1..10.0), depth, mix, sync, syncBpm — Univibe rotativo clássico', params:{speed:2.0, depth:55, mix:50, sync:0, syncBpm:80}},
    {id:13, name:'TRI UNIVIBE', type:'Psicodélico', desc:'speed (0.1..10.0), depth, mix, sync, syncBpm — Triple Univibe', params:{speed:2.0, depth:55, mix:50, sync:0, syncBpm:80}},
    {id:14, name:'AUTOFILTER', type:'Filtro', desc:'speed (0.1..10.0), min, max, mix, fb, sync, syncBpm — Auto-filtro dinâmico', params:{speed:2.0, min:40, max:60, mix:50, fb:50, sync:0, syncBpm:80}},
    {id:15, name:'PHASER STEREO', type:'Estéreo', desc:'speed (0.17..16.0), stage (A/B), regen, mix, lfo, sync, syncBpm — Phaser Estéreo', params:{speed:1.5, stage:1, regen:50, mix:50, lfo:1, sync:0, syncBpm:80}},
    {id:16, name:'FLANGER STEREO', type:'Estéreo', desc:'speed (0.17..16.0), depth, regen, mix, jet, lfo, sync, syncBpm — Flanger Estéreo', params:{speed:1.2, depth:45, regen:60, mix:35, jet:20, lfo:1, sync:0, syncBpm:80}},
    {id:17, name:'VIBE STEREO', type:'Estéreo', desc:'speed (0.17..16.0), stage (A/B), regen, mix, lfo, sync, syncBpm — Vibe Estéreo', params:{speed:2.0, stage:1, regen:50, mix:50, lfo:5, sync:0, syncBpm:80}},
    {id:18, name:'CHORUS STEREO', type:'Estéreo', desc:'speed (0.1..10.0), depth, mode (1..6), mix, sync, syncBpm — Chorus Estéreo', params:{speed:1.5, depth:45, mode:1, mix:35, sync:0, syncBpm:80}},
    {id:19, name:'TREMOLO STEREO', type:'Estéreo', desc:'speed (0.1..10.0), depth, level, mode (1..3), sync, syncBpm — Tremolo Estéreo', params:{speed:3.0, depth:50, level:70, mode:1, sync:0, syncBpm:80}},
    {id:20, name:'VIBRATO STEREO', type:'Estéreo', desc:'speed (0.1..10.0), depth (4..24), bits (0..100), mix (0..100), sync, syncBpm — Vibrato Estéreo', params:{speed:2.5, depth:12, bits:80, mix:50, sync:0, syncBpm:80}}
  ],
  DELAY: [
    {id:1, name:'Clean', type:'Mono', desc:'Time (0.100..2.000s), Fb, Mix, Sync, SyncBPM — Delay digital puro', params:{Time:500, Fb:30, Mix:25, Sync:0, SyncBPM:80}},
    {id:2, name:'Modern', type:'Mono', desc:'Time, Fb, Mix, Phaser, Mod, Sync, SyncBPM — Delay moderno com Phaser', params:{Time:500, Fb:30, Mix:25, Phaser:0, Mod:0, Sync:0, SyncBPM:80}},
    {id:3, name:'Echo', type:'Mono', desc:'Time, Fb, Mix, Sync, SyncBPM — Eco acústico natural', params:{Time:500, Fb:30, Mix:25, Sync:0, SyncBPM:80}},
    {id:4, name:'Analog', type:'Mono', desc:'Time, Fb, Mix, Sync, SyncBPM — Delay analógico BBD quente', params:{Time:380, Fb:28, Mix:22, Sync:0, SyncBPM:80}},
    {id:5, name:'Duck', type:'Mono', desc:'Time, Fb, Mix, Release, Speed, Depth, Sync, SyncBPM — Ducking Delay dinâmico', params:{Time:500, Fb:30, Mix:28, Release:50, Speed:20, Depth:20, Sync:0, SyncBPM:80}},
    {id:6, name:'Dtype', type:'Mono', desc:'Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Tape Delay de fita', params:{Time:470, Fb:35, Mix:28, Grit:20, Speed:12, Depth:12, Sync:0, SyncBPM:80}},
    {id:7, name:'Tremolo', type:'Mono', desc:'Time, Fb, Mix, Pattern, Speed, Depth, Sync, SyncBPM — Tremolo Delay', params:{Time:500, Fb:30, Mix:28, Pattern:1, Speed:30, Depth:40, Sync:0, SyncBPM:80}},
    {id:8, name:'Filter', type:'Mono', desc:'Time, Fb, Mix, Filter, Speed, Depth, Sync, SyncBPM — Filter Delay', params:{Time:500, Fb:30, Mix:28, Filter:50, Speed:30, Depth:15, Sync:0, SyncBPM:80}},
    {id:9, name:'Dual', type:'Mono', desc:'Time, Fb, Mix, T-Mode, Speed, Depth, Sync, SyncBPM — Dual Delay', params:{Time:420, Fb:32, Mix:28, 'T-Mode':8, Speed:15, Depth:15, Sync:0, SyncBPM:80}},
    {id:10, name:'Lofi', type:'Mono', desc:'Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Lo-Fi Delay', params:{Time:500, Fb:30, Mix:28, Grit:25, Speed:15, Depth:15, Sync:0, SyncBPM:80}},
    {id:11, name:'Pattern', type:'Mono', desc:'Time, Fb, Mix, Pattern (0..15), Speed, Depth, Sync, SyncBPM — Pattern Delay', params:{Time:500, Fb:30, Mix:28, Pattern:8, Speed:15, Depth:15, Sync:0, SyncBPM:80}},
    {id:12, name:'Ice', type:'Mono', desc:'Time, Fb, Mix, Pitch (0..12), Mod, Sync, SyncBPM — Ice Pitch Delay', params:{Time:500, Fb:30, Mix:28, Pitch:6, Mod:30, Sync:0, SyncBPM:80}},
    {id:13, name:'Reverse', type:'Mono', desc:'Time, Fb, Mix, Phaser, Mod, Sync, SyncBPM — Reverse Delay', params:{Time:500, Fb:30, Mix:28, Phaser:10, Mod:15, Sync:0, SyncBPM:80}},
    {id:14, name:'PingPong Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Sync, SyncBPM — Ping-Pong Stereo Delay', params:{Time:500, Fb:30, Mix:28, Sync:0, SyncBPM:80}},
    {id:15, name:'Clean Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Sync, SyncBPM — Clean Stereo Delay', params:{Time:500, Fb:30, Mix:28, Sync:0, SyncBPM:80}},
    {id:16, name:'Modern Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Phaser, Mod, Sync, SyncBPM — Modern Stereo Delay', params:{Time:500, Fb:30, Mix:28, Phaser:10, Mod:15, Sync:0, SyncBPM:80}},
    {id:17, name:'Echo Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Sync, SyncBPM — Echo Stereo Delay', params:{Time:500, Fb:30, Mix:28, Sync:0, SyncBPM:80}},
    {id:18, name:'Analog Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Sync, SyncBPM — Analog Stereo Delay', params:{Time:400, Fb:30, Mix:26, Sync:0, SyncBPM:80}},
    {id:19, name:'Duck Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Release, Speed, Depth, Sync, SyncBPM — Duck Stereo Delay', params:{Time:500, Fb:30, Mix:28, Release:50, Speed:20, Depth:20, Sync:0, SyncBPM:80}},
    {id:20, name:'Dtype Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Dtype Stereo Tape Delay', params:{Time:470, Fb:35, Mix:28, Grit:20, Speed:12, Depth:12, Sync:0, SyncBPM:80}},
    {id:21, name:'Tremolo Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Pattern, Dive, Speed, Depth, Sync, SyncBPM — Tremolo Stereo Delay', params:{Time:500, Fb:30, Mix:28, Pattern:1, Dive:30, Speed:15, Depth:40, Sync:0, SyncBPM:80}},
    {id:22, name:'Filter Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Filter, Speed, Depth, Sync, SyncBPM — Filter Stereo Delay', params:{Time:500, Fb:30, Mix:28, Filter:50, Speed:30, Depth:15, Sync:0, SyncBPM:80}},
    {id:23, name:'Dual Stereo', type:'Estéreo', desc:'Time, Fb, Mix, T-Mode, Speed, Depth, Sync, SyncBPM — Dual Stereo Delay', params:{Time:420, Fb:32, Mix:28, 'T-Mode':8, Speed:15, Depth:15, Sync:0, SyncBPM:80}},
    {id:24, name:'Lofi Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Lofi Stereo Delay', params:{Time:500, Fb:30, Mix:28, Grit:25, Speed:15, Depth:15, Sync:0, SyncBPM:80}},
    {id:25, name:'Pattern Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Pattern (0..15), Speed, Depth, Sync, SyncBPM — Pattern Stereo Delay', params:{Time:500, Fb:30, Mix:28, Pattern:8, Speed:15, Depth:15, Sync:0, SyncBPM:80}},
    {id:26, name:'Ice Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Pitch (0..12), Mod, Sync, SyncBPM — Ice Stereo Delay', params:{Time:500, Fb:30, Mix:28, Pitch:6, Mod:30, Sync:0, SyncBPM:80}},
    {id:27, name:'Reverse Stereo', type:'Estéreo', desc:'Time, Fb, Mix, Phaser, Mod, Sync, SyncBPM — Reverse Stereo Delay', params:{Time:500, Fb:30, Mix:28, Phaser:10, Mod:15, Sync:0, SyncBPM:80}}
  ],
  REVERB: [
    {id:1, name:'room', type:'Mono', desc:'decay, mix, highPass, lowPass, modDepth — Simulação de sala acústica natural', params:{decay:20, mix:18, highPass:80, lowPass:80, modDepth:8}},
    {id:2, name:'hall', type:'Mono', desc:'decay, mix, highPass, lowPass, modDepth — Grande salão de concerto', params:{decay:28, mix:20, highPass:80, lowPass:80, modDepth:10}},
    {id:3, name:'plate', type:'Mono', desc:'decay, mix, highPass, lowPass, modDepth — Reverberação de placa metálica de estúdio', params:{decay:24, mix:18, highPass:80, lowPass:80, modDepth:10}},
    {id:4, name:'spring', type:'Mono', desc:'decay, mix, highPass, lowPass, combs — Tanque de molas vintage', params:{decay:25, mix:22, highPass:80, lowPass:80, combs:50}},
    {id:5, name:'shimmer', type:'Mono', desc:'decay, mix, tone, pitch, amount — Reverb cristalino com oitavas', params:{decay:50, mix:28, tone:70, pitch:40, amount:30}},
    {id:6, name:'bloom', type:'Mono', desc:'decay, mix, tone, lend, length — Reverb Bloom', params:{decay:35, mix:24, tone:75, lend:35, length:30}},
    {id:7, name:'cloud', type:'Mono', desc:'decay, mix, highPass, lowPass, diff — Cloud Reverb', params:{decay:45, mix:26, highPass:80, lowPass:80, diff:20}},
    {id:8, name:'lofi', type:'Mono', desc:'decay, mix, sampleRete, noiseLevel, modDepth — Lofi Reverb', params:{decay:25, mix:22, sampleRete:80, noiseLevel:25, modDepth:15}},
    {id:9, name:'swell', type:'Mono', desc:'decay, mix, tone, lend, riseTime — Swell Reverb', params:{decay:40, mix:26, tone:75, lend:35, riseTime:30}},
    {id:10, name:'room stereo', type:'Estéreo', desc:'decay, mix, highPass, lowPass, modDepth — Sala de estúdio em estéreo', params:{decay:20, mix:18, highPass:80, lowPass:80, modDepth:8}},
    {id:11, name:'hall stereo', type:'Estéreo', desc:'decay, mix, highPass, lowPass, modDepth — Hall em estéreo', params:{decay:32, mix:22, highPass:80, lowPass:80, modDepth:10}},
    {id:12, name:'plate stereo', type:'Estéreo', desc:'decay, mix, highPass, lowPass, modDepth — Plate em estéreo', params:{decay:26, mix:20, highPass:80, lowPass:80, modDepth:10}},
    {id:13, name:'spring stereo', type:'Estéreo', desc:'decay, mix, highPass, lowPass, combs — Spring em estéreo', params:{decay:26, mix:22, highPass:80, lowPass:80, combs:50}},
    {id:14, name:'shimmer stereo', type:'Estéreo', desc:'decay, mix, tone, pitch, amount — Shimmer em estéreo', params:{decay:60, mix:32, tone:72, pitch:40, amount:32}},
    {id:15, name:'bloom stereo', type:'Estéreo', desc:'decay, mix, tone, lend, length — Bloom em estéreo', params:{decay:38, mix:25, tone:75, lend:35, length:30}},
    {id:16, name:'cloud stereo', type:'Estéreo', desc:'decay, mix, highPass, lowPass, diff — Cloud em estéreo', params:{decay:50, mix:28, highPass:80, lowPass:80, diff:20}},
    {id:17, name:'lofi stereo', type:'Estéreo', desc:'decay, mix, sampleRete, noiseLevel, modDepth — Lofi em estéreo', params:{decay:25, mix:22, sampleRete:80, noiseLevel:25, modDepth:15}},
    {id:18, name:'swell streo', type:'Estéreo', desc:'decay, mix, tone, lend, riseTime — Swell em estéreo', params:{decay:42, mix:28, tone:75, lend:35, riseTime:30}}
  ],
  VOL: [
    {id:1, name:'Volume Pedal', type:'Master / Expression', desc:'Level (0..100) — Pedal de volume na cadeia da MK-20', params:{Level:100}}
  ]
};

const BLOCK_KEYS = ['FX','NS','DS','AMP','CAB','EQ','MOD','DELAY','REVERB','VOL'];
const BLOCK_COLORS = {
  FX:'#14A3C7', NS:'#3B82F6', DS:'#E55A2B', AMP:'#C6362C', CAB:'#2A7B9B',
  EQ:'#64748B', MOD:'#8B5CF6', DELAY:'#0D9488', REVERB:'#D97706', VOL:'#475569'
};

// ============================================================================
// 2. TEMPLATE BINÁRIO AUTÊNTICO DE 412 BYTES DA MK-20 (puts.mk20 M-EFCS)
// ============================================================================
const AUTHENTIC_MK20_TEMPLATE = [
  0x55,0x53,0x45,0x52,0x20,0x50,0x52,0x45,0x53,0x45,0x54,0x20,0x31,0x34,0x35,0x00,
  0x00,0x00,0x00,0x00,0xff,0x90,0x1e,0x00,0x3c,0x00,0x50,0x00,0x00,0x00,0x00,0x01,
  0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x19,0x00,0x32,0x00,
  0x50,0x00,0x19,0x00,0x32,0x00,0x00,0x00,0x00,0x00,0x50,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x32,0x00,0x32,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x32,0x00,0x32,0x00,
  0x32,0x00,0x32,0x00,0x32,0x00,0x00,0x00,0x32,0x00,0x64,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x32,0x00,0x32,0x00,0x32,0x00,0x32,0x00,0x32,0x00,0x00,0x00,
  0x32,0x00,0x64,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x50,0x00,0x14,0x00,
  0xb4,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x0a,0x00,0x32,0x00,
  0x32,0x00,0x00,0x00,0x50,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0xf4,0x01,0x1e,0x00,0x32,0x00,0x00,0x00,0x50,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x14,0x00,0x1e,0x00,
  0x50,0x00,0x50,0x00,0x28,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x64,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x0a,0x00,0x00,0x00,
  0x00,0x00,0x64,0x00,0x00,0x00,0x0b,0x00,0x00,0x00,0x00,0x00,0x64,0x00,0x00,0x00,
  0x0c,0x00,0x00,0x00,0x00,0x00,0x64,0x00,0x00,0x00,0x09,0x00,0x00,0x00,0x00,0x00,
  0x64,0x00,0x00,0x00,0x09,0x0a,0x00,0x00,0x00,0x00,0x08,0x09,0x00,0x00,0x00,0x00,
  0x04,0x05,0x00,0x00,0x00,0x00,0x00,0x47,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x00,0x4e,0x47,0x44,0x34,0x00,0x00,0x00
];

// ============================================================================
// 3. ESTADO GLOBAL DO APLICATIVO
// ============================================================================
let currentTab = 0;
let inputMode = 'SONG';
let selectedBlock = 'AMP';
let showAllPedals = false;
let dropdownOpenFor = null;
let binaryMode = 'AUTHENTIC_412';
let customTemplateBytes = null;
let customTemplateName = null;
let confirmedSong = { artist:'Eli Soares', title:'Me Ajude a Melhorar', album:'Memórias', genre:'Gospel / Neo-Soul' };
let savedPresets = [];
let appSettings = { geminiApiKey: '', isPro: false, dailyUsed: 0, lastDate: '' };
let micRecording = false;
let micInterval = null;

let currentPreset = createPresetFromSong('Eli Soares - Me Ajude a Melhorar', 'Solo melódico encorpado sem abelha digital');

function findCatalogItem(block, nameOrId) {
  const list = MK20_CATALOG[block] || [];
  if (!nameOrId) return list[0];
  if (typeof nameOrId === 'number') return list.find(x => x.id === nameOrId) || list[0];
  const norm = String(nameOrId).toLowerCase().replace(/[_\-\s\.]/g, '');
  return list.find(x => x.name.toLowerCase().replace(/[_\-\s\.]/g, '') === norm) || list[0];
}

function cloneParams(obj) {
  return JSON.parse(JSON.stringify(obj || {}));
}

// ============================================================================
// 4. BANCO DE MÚSICAS MASTER & MOTOR ACÚSTICO INTELIGENTE MK-20
// ============================================================================
function getSongCandidates(query) {
  const q = (query || '').trim();
  if (q.length < 2) return [];
  const lower = q.toLowerCase();
  const db = [
    {artist:'Eli Soares', title:'Me Ajude a Melhorar', album:'Casa de Deus (Ao Vivo)', genre:'Gospel / Soul Lead'},
    {artist:'Eli Soares', title:'Aonde Está Deus', album:'Memórias', genre:'Gospel / Groove Clean'},
    {artist:'Voz da Verdade', title:'O Escudo (Solo)', album:'O Escudo', genre:'Gospel Rock Lead'},
    {artist:'Gabriela Rocha', title:'Lugar Secreto', album:'Céu', genre:'Worship Ambient Swell'},
    {artist:'Pink Floyd', title:'Comfortably Numb (Solo)', album:'The Wall', genre:'Progressive Rock / Fuzz Lead'},
    {artist:'Pink Floyd', title:'Shine On You Crazy Diamond', album:'Wish You Were Here', genre:'Gilmour Clean / Sustain'},
    {artist:'Guns N\' Roses', title:'Sweet Child O\' Mine', album:'Appetite for Destruction', genre:'Hard Rock / Slash JCM800'},
    {artist:'Metallica', title:'Master of Puppets', album:'Master of Puppets', genre:'Thrash Metal High-Gain'},
    {artist:'John Mayer', title:'Slow Dancing in a Burning Room', album:'Continuum', genre:'Blues Clean / Blackface'},
    {artist:'Breaking Benjamin', title:'The Diary of Jane', album:'Phobia', genre:'Modern Drop Metal'}
  ];
  const matches = db.filter(x => (x.artist + ' ' + x.title + ' ' + x.genre).toLowerCase().indexOf(lower) >= 0);
  if (matches.length > 0) return matches.slice(0, 4);
  const parts = q.split('-');
  const art = parts.length > 1 ? parts[0].trim() : 'Artista Principal';
  const tit = parts.length > 1 ? parts.slice(1).join('-').trim() : q;
  return [
    {artist: art, title: tit + ' (Versão Estúdio Original)', album: 'Single / Álbum Oficial', genre: 'Timbre Fiel da Gravação'},
    {artist: art, title: tit + ' (Versão Ao Vivo / Lead Solo)', album: 'Live Session', genre: 'Mais Sustain e Presença'},
    {artist: art, title: tit + ' (Base Rítmica / Acordes)', album: 'Arranjo Base', genre: 'Dinâmica Equilibrada'}
  ];
}

function createPresetFromSong(query, notes) {
  const text = ((query || '') + ' ' + (notes || '')).toLowerCase();
  const isBass = /baixo|bass|ampeg|slap|darkglass|svt/.test(text);
  const isAcoustic = /violao|violão|acoustic|acustico|taylor|takamine|fingerstyle/.test(text);
  const isWorship = /worship|shimmer|swell|lugar secreto|ambient|igreja|hillsong/.test(text);
  const isMetal = /metal|metallica|puppets|djent|5150|rectifier|pesado|high gain|diary of jane|drop/.test(text);
  const isGilmour = /gilmour|pink floyd|comfortably numb|muff|shine on/.test(text);
  const isClean = /clean|limpo|mayer|funk|jazz|blues clean|fender/.test(text) && !/solo/.test(text);

  let title = (query || 'MK20 Custom').split('-').pop().trim().substring(0, 18) || 'MK20 Tone';
  let style = 'Rock / Lead Custom';
  let guitar = 'Strato / Humbucker';
  let pickup = 'Captador Ponte (Bridge)';
  let bpm = 120;
  let desc = 'Timbre calibrado na arquitetura real de 412 bytes da M-VAVE MK-20 com corte de aspereza digital (High Cut 6.8kHz).';

  let fx = { active: true, model: 'Compress', params: cloneParams(findCatalogItem('FX','Compress').params) };
  let ns = { active: true, model: 'soft gate', params: cloneParams(findCatalogItem('NS','soft gate').params) };
  let ds = { active: true, model: 'MW-808', params: {gain:25, tone:54, level:85} };
  let amp = { active: true, model: '86OD_JCM800', params: {Gain:62, Level:72, Bass:56, Middle:66, Treble:58, Reso:60, Pres:56, Bright:0} };
  let cab = { active: true, model: '41Mar2551B412', params: {Level:84, 'Low Cut':85, 'High Cut':6800} };
  let eq = { active: true, model: 'Guitar EQ 6', params: {'100Hz':1, '200Hz':1, '400Hz':0, '800Hz':3, '1.6kHz':2, '3.2kHz':-1} };
  let mod = { active: false, model: 'CHORUS', params: cloneParams(findCatalogItem('MOD','CHORUS').params) };
  let dly = { active: true, model: 'Analog', params: {Time:380, Fb:32, Mix:26, Sync:0, SyncBPM:120} };
  let rev = { active: true, model: 'plate', params: {decay:28, mix:22, highPass:80, lowPass:80, modDepth:10} };
  let vol = { active: true, model: 'Volume Pedal', params: {Level:100} };

  if (isBass) {
    style = 'Contrabaixo Pro / Groove';
    guitar = 'Contrabaixo 4/5 Cordas';
    pickup = 'Captador JJ / P-Bass';
    bpm = 110;
    desc = 'Cadeia dedicada de Contrabaixo usando pré AMPEG SVT-400T + Geladeira SVT-810E e Bass EQ de 7 bandas.';
    ds = { active: false, model: 'MW-B7000', params: cloneParams(findCatalogItem('DS','MW-B7000').params) };
    amp = { active: true, model: '102ApSVT_BS', params: {Gain:42, Level:80, Bass:68, Middle:60, Treble:56, Reso:72, Pres:55, Bright:0} };
    cab = { active: true, model: '80AmpgSVT810', params: {Level:86, 'Low Cut':35, 'High Cut':5200} };
    eq = { active: true, model: 'Bass EQ 7', params: {'50Hz':3, '120Hz':2, '400Hz':-1, '500Hz':1, '800Hz':2, '4.5kHz':2, '10k':1} };
    dly.active = false;
    rev = { active: true, model: 'room', params: {decay:15, mix:10, highPass:80, lowPass:80, modDepth:5} };
  } else if (isAcoustic) {
    style = 'Simulador de Violão Acústico';
    guitar = 'Guitarra / Violão Piezo';
    pickup = 'Captador Braço (Neck)';
    bpm = 96;
    desc = 'Simulação acústica cristalina combinando Roland JC-120 + Impulse Response Taylor CE414 Grand Auditorium.';
    ds.active = false;
    amp = { active: true, model: '25CL1_JC120', params: {Gain:22, Level:78, Bass:50, Middle:48, Treble:64, Reso:45, Pres:58, Bright:75} };
    cab = { active: true, model: '74TaylrOcE', params: {Level:84, 'Low Cut':60, 'High Cut':12000} };
    eq = { active: true, model: 'Guitar EQ 6', params: {'100Hz':2, '200Hz':-2, '400Hz':-1, '800Hz':0, '1.6kHz':2, '3.2kHz':3} };
    mod = { active: true, model: 'CHORUS STEREO', params: {speed:1.2, depth:40, mode:1, mix:25, sync:0, syncBpm:96} };
    rev = { active: true, model: 'hall stereo', params: {decay:34, mix:24, highPass:80, lowPass:80, modDepth:12} };
  } else if (isWorship) {
    style = 'Worship Ambient / Shimmer';
    guitar = 'Strato / Tele / Gretsch';
    pickup = 'Posição Intermediária';
    bpm = 74;
    desc = 'Ambiência tridimensional Worship com VOX AC30 Top Boost, Delay PingPong Stereo em colcheia pontuada e Shimmer Stereo.';
    ds = { active: true, model: 'MW-GOLD', params: {gain:38, tone:56, level:78} };
    amp = { active: true, model: '81CL_VOSAC30', params: {Gain:38, Level:76, Bass:52, Middle:56, Treble:64, Reso:50, Pres:58, Bright:70} };
    cab = { active: true, model: '64VAC30TBC212', params: {Level:84, 'Low Cut':80, 'High Cut':7200} };
    mod = { active: true, model: 'CHORUS STEREO', params: {speed:1.1, depth:48, mode:1, mix:30, sync:0, syncBpm:74} };
    dly = { active: true, model: 'PingPong Stereo', params: {Time:540, Fb:42, Mix:34, Sync:1, SyncBPM:74} };
    rev = { active: true, model: 'shimmer stereo', params: {decay:68, mix:36, tone:70, pitch:40, amount:34} };
  } else if (isMetal) {
    style = 'Modern High-Gain Metal';
    guitar = 'Humbucker Ativo / Passivo';
    pickup = 'Captador Ponte (Bridge)';
    bpm = 140;
    desc = 'Parede sonora de alto ganho com EVH 5150 III / Mesa Oversized 4x12, TS-808 como booster de precisão e AI Gate rápido.';
    fx.active = false;
    ns = { active: true, model: 'ai ms gate', params: {gate:68, bias:55, inputDetect:0} };
    ds = { active: true, model: 'MW-808', params: {gain:10, tone:62, level:92} };
    amp = { active: true, model: '80DS_5150III', params: {Gain:76, Level:70, Bass:58, Middle:48, Treble:64, Reso:68, Pres:64, Bright:0} };
    cab = { active: true, model: '46MbOS412A', params: {Level:85, 'Low Cut':90, 'High Cut':6500} };
    eq = { active: true, model: 'Guitar EQ 6', params: {'100Hz':2, '200Hz':-2, '400Hz':-1, '800Hz':2, '1.6kHz':3, '3.2kHz':1} };
    dly = { active: false, model: 'Analog', params: {Time:340, Fb:22, Mix:18, Sync:0, SyncBPM:140} };
    rev = { active: true, model: 'room', params: {decay:16, mix:12, highPass:80, lowPass:80, modDepth:6} };
  } else if (isGilmour) {
    style = 'Progressive Lead / Gilmour';
    guitar = 'Stratocaster Single-Coil';
    pickup = 'Captador Ponte / Braço';
    bpm = 64;
    desc = 'Sustain infinito e cremoso inspirado em David Gilmour: Big Muff (MW-MUFF) + Hiwatt Fane 4x12 + Delay Tape Dtype.';
    ds = { active: true, model: 'MW-MUFF', params: {sustain:78, tone:54, level:84} };
    amp = { active: true, model: '13CL1_FenDi', params: {Gain:34, Level:76, Bass:56, Middle:58, Treble:58, Reso:55, Pres:54, Bright:50} };
    cab = { active: true, model: '30HiwSE412C', params: {Level:85, 'Low Cut':80, 'High Cut':6600} };
    mod = { active: true, model: 'CHORUS STEREO', params: {speed:1.3, depth:46, mode:1, mix:32, sync:0, syncBpm:64} };
    dly = { active: true, model: 'Dtype Stereo', params: {Time:480, Fb:38, Mix:30, Grit:18, Speed:12, Depth:14, Sync:0, SyncBPM:64} };
    rev = { active: true, model: 'hall stereo', params: {decay:38, mix:24, highPass:80, lowPass:80, modDepth:10} };
  } else if (isClean) {
    style = 'Boutique Clean & Blues';
    guitar = 'Stratocaster / Telecaster';
    pickup = 'Braço / Meio (Neck)';
    bpm = 98;
    desc = 'Clean cristalino e encorpado baseado no Fender 65 Deluxe Reverb com Klon Centaur leve e Spring Reverb.';
    ds = { active: true, model: 'MW-GOLD', params: {gain:22, tone:54, level:74} };
    amp = { active: true, model: '13CL1_FenDi', params: {Gain:30, Level:78, Bass:54, Middle:56, Treble:62, Reso:48, Pres:54, Bright:65} };
    cab = { active: true, model: '15FdDBface112', params: {Level:84, 'Low Cut':75, 'High Cut':7500} };
    dly = { active: true, model: 'Analog', params: {Time:340, Fb:24, Mix:18, Sync:0, SyncBPM:98} };
    rev = { active: true, model: 'spring stereo', params: {decay:28, mix:24, highPass:80, lowPass:80, combs:50} };
  }

  return {
    title: title,
    style: style,
    guitarType: guitar,
    pickupPosition: pickup,
    bpm: bpm,
    masterVolume: 60,
    description: desc,
    pedals: { FX:fx, NS:ns, DS:ds, AMP:amp, CAB:cab, EQ:eq, MOD:mod, DELAY:dly, REVERB:rev, VOL:vol }
  };
}

// ============================================================================
// 5. SERIALIZADOR BINÁRIO 412 BYTES FIEL AO MK20BINARYSERIALIZER.KT
// ============================================================================
function isFxTenScale(fxId, key) {
  const k = key.toLowerCase();
  if (fxId === 1) return k === 'speed';
  if (fxId === 3) return k === 'sense';
  if (fxId >= 4 && fxId <= 8) return k === 'gain';
  if (fxId === 9) return k === 'sustain';
  if (fxId === 10 || fxId === 11) return k === 'ratio';
  return false;
}

function serializePreset412(preset, baseTemplate, preserveHeader) {
  const tpl = (baseTemplate && baseTemplate.length === 412) ? baseTemplate.slice() : AUTHENTIC_MK20_TEMPLATE.slice();
  const bytes = new Uint8Array(412);
  for (let i = 0; i < 412; i++) bytes[i] = tpl[i];
  for (let i = 0x3c; i < 0x19c; i++) bytes[i] = AUTHENTIC_MK20_TEMPLATE[i];

  const view = new DataView(bytes.buffer);

  // 1. Nome (0..19)
  if (preserveHeader && baseTemplate && baseTemplate.length >= 20 && baseTemplate[19] === 0) {
    for (let i = 0; i < 15; i++) bytes[i] = baseTemplate[i];
    for (let i = 15; i < 20; i++) bytes[i] = 0;
  } else {
    const cleanName = (preset.title || 'PRESET MK20').replace(/[^\x20-\x7E]/g, '').substring(0, 15);
    for (let i = 0; i < 20; i++) bytes[i] = i < cleanName.length ? cleanName.charCodeAt(i) : 0;
  }

  // 2. Hardware signature 0x14..0x17: FF 90 1E 00
  bytes[0x14] = 0xff; bytes[0x15] = 0x90; bytes[0x16] = 0x1e; bytes[0x17] = 0x00;

  // 3. Master Vol (0x18) & BPM (0x1a)
  view.setInt16(0x18, Math.max(0, Math.min(100, Math.round(preset.masterVolume || 60))), true);
  view.setInt16(0x1a, Math.max(40, Math.min(240, Math.round(preset.bpm || 80))), true);
  view.setInt16(0x1c, 0, true);

  // 4. Ordem dos módulos (0x1e..0x27)
  for (let i = 0; i < 10; i++) bytes[0x1e + i] = i;

  // 5. Flags ativo/bypass (0x28..0x31)
  BLOCK_KEYS.forEach((k, idx) => {
    bytes[0x28 + idx] = (preset.pedals[k] && preset.pedals[k].active) ? 1 : 0;
  });

  // 6. IDs de modelo 0-indexed (0x32..0x3b)
  BLOCK_KEYS.forEach((k, idx) => {
    const item = findCatalogItem(k, preset.pedals[k] ? preset.pedals[k].model : null);
    bytes[0x32 + idx] = Math.max(0, (item ? item.id : 1) - 1);
  });

  // [0] FX (0x3c)
  const fxMod = preset.pedals.FX || {model:'Compress', params:{}};
  const fxItem = findCatalogItem('FX', fxMod.model);
  const fxKeys = Object.keys(fxItem.params);
  if (fxItem.id === 1) {
    const sp = fxMod.params.Speed !== undefined ? fxMod.params.Speed : 2.5;
    view.setInt16(0x3c, Math.max(1, Math.min(100, Math.round(sp <= 10 ? sp * 10 : sp))), true);
    view.setInt16(0x3e, Math.max(0, Math.min(100, Math.round(fxMod.params.Q ?? 50))), true);
    view.setInt16(0x40, Math.max(0, Math.min(100, Math.round(fxMod.params.Mix ?? 80))), true);
    view.setInt16(0x42, Math.max(0, Math.min(100, Math.round(fxMod.params.Width ?? 25))), true);
    view.setInt16(0x44, Math.max(0, Math.min(100, Math.round(fxMod.params.Level ?? 50))), true);
    view.setInt16(0x46, (fxMod.params.Sync ? 1 : 0), true);
    view.setInt16(0x48, 0, true);
    view.setInt16(0x4a, Math.max(40, Math.min(240, Math.round(fxMod.params.SyncBpm ?? preset.bpm ?? 80))), true);
  } else {
    for (let i = 0; i < 12; i++) {
      const k = fxKeys[i];
      let val = 0;
      if (k) {
        const raw = fxMod.params[k] !== undefined ? fxMod.params[k] : fxItem.params[k];
        if (k.toLowerCase() === 'sync') val = raw ? 1 : 0;
        else if (k.toLowerCase() === 'syncbpm') val = Math.max(40, Math.min(240, Math.round(raw)));
        else if (isFxTenScale(fxItem.id, k)) val = Math.max(0, Math.min(100, Math.round(raw <= 10 ? raw * 10 : raw)));
        else val = Math.max(-24, Math.min(100, Math.round(raw)));
      } else if (i === 7) val = 80;
      view.setInt16(0x3c + i * 2, val, true);
    }
  }

  // [1] NS (0x54)
  const nsMod = preset.pedals.NS || {model:'soft gate', params:{}};
  const nsItem = findCatalogItem('NS', nsMod.model);
  const nsKeys = Object.keys(nsItem.params);
  for (let i = 0; i < 12; i++) {
    const k = nsKeys[i];
    let val = 0;
    if (k) {
      let raw = nsMod.params[k] !== undefined ? nsMod.params[k] : nsItem.params[k];
      if (k === 'inputDetect') val = raw ? 1 : 0;
      else if (k === 'thd' && nsItem.id >= 3 && nsItem.id <= 5) val = Math.max(-80, Math.min(-30, Math.round(raw > 0 ? -raw : raw)));
      else val = Math.max(0, Math.min(100, Math.round(raw)));
    }
    view.setInt16(0x54 + i * 2, val, true);
  }

  // [2] DS (0x6c)
  const dsSlots = [50, 50, 50, 50, 50, 0, 50, 100, 0, 0, 0, 0];
  const dsMod = preset.pedals.DS || {model:'MW-808', params:{}};
  const dsItem = findCatalogItem('DS', dsMod.model);
  const dsKeys = Object.keys(dsItem.params);
  for (let i = 0; i < 12; i++) {
    const k = dsKeys[i];
    const val = k ? Math.max(0, Math.min(100, Math.round(dsMod.params[k] ?? dsItem.params[k] ?? 50))) : dsSlots[i];
    view.setInt16(0x6c + i * 2, val, true);
  }

  // [3] AMP (0x84)
  const ampMod = preset.pedals.AMP || {model:'86OD_JCM800', params:{}};
  const ampItem = findCatalogItem('AMP', ampMod.model);
  const ampKeys = ['Gain','Level','Bass','Middle','Treble','Reso','Pres','Bright'];
  for (let i = 0; i < 12; i++) {
    const k = ampKeys[i];
    const val = k ? Math.max(0, Math.min(100, Math.round(ampMod.params[k] ?? ampItem.params[k] ?? 50))) : 0;
    view.setInt16(0x84 + i * 2, val, true);
  }

  // [4] CAB (0x9c)
  const cabMod = preset.pedals.CAB || {model:'41Mar2551B412', params:{}};
  const cabLv = Math.max(0, Math.min(100, Math.round(cabMod.params.Level ?? 82)));
  const cabLc = Math.max(20, Math.min(300, Math.round(cabMod.params['Low Cut'] ?? 80)));
  const hcRaw = cabMod.params['High Cut'] ?? 7000;
  const cabHc = Math.max(50, Math.min(180, Math.round(hcRaw > 200 ? hcRaw / 100 : (hcRaw <= 20 ? hcRaw * 10 : hcRaw))));
  view.setInt16(0x9c, cabLv, true);
  view.setInt16(0x9e, cabLc, true);
  view.setInt16(0xa0, cabHc, true);
  for (let i = 3; i < 12; i++) view.setInt16(0x9c + i * 2, 0, true);

  // [5] EQ (0xb4)
  const eqMod = preset.pedals.EQ || {model:'Guitar EQ 6', params:{}};
  const eqItem = findCatalogItem('EQ', eqMod.model);
  const eqKeys = Object.keys(eqItem.params);
  for (let i = 0; i < 12; i++) {
    const k = eqKeys[i];
    let val = 0;
    if (k) {
      const raw = eqMod.params[k] ?? 0;
      val = Math.max(-12, Math.min(12, Math.round(raw > 12 ? (raw - 50) * 0.24 : raw)));
    }
    view.setInt16(0xb4 + i * 2, val, true);
  }

  // [6] MOD (0xcc)
  const modMod = preset.pedals.MOD || {model:'CHORUS', params:{}};
  const modItem = findCatalogItem('MOD', modMod.model);
  const modKeys = Object.keys(modItem.params);
  for (let i = 0; i < 12; i++) {
    const k = modKeys[i];
    let val = 0;
    if (k) {
      const raw = modMod.params[k] ?? modItem.params[k] ?? 50;
      if (k.toLowerCase() === 'speed') val = Math.max(1, Math.min(160, Math.round(raw <= 16 ? raw * 10 : raw)));
      else if (k.toLowerCase() === 'sync') val = raw ? 1 : 0;
      else if (k.toLowerCase() === 'syncbpm') val = Math.max(40, Math.min(240, Math.round(raw)));
      else val = Math.max(0, Math.min(100, Math.round(raw)));
    }
    view.setInt16(0xcc + i * 2, val, true);
  }

  // [7] DELAY (0xe4)
  const dlyMod = preset.pedals.DELAY || {model:'Analog', params:{}};
  const dlyItem = findCatalogItem('DELAY', dlyMod.model);
  const dlyKeys = Object.keys(dlyItem.params);
  for (let i = 0; i < 12; i++) {
    const k = dlyKeys[i];
    let val = 0;
    if (k) {
      const raw = dlyMod.params[k] ?? dlyItem.params[k] ?? 50;
      if (k.toLowerCase() === 'time') val = Math.max(100, Math.min(2000, Math.round(raw <= 2 ? raw * 1000 : raw)));
      else if (k.toLowerCase() === 'sync') val = raw ? 1 : 0;
      else if (k.toLowerCase() === 'syncbpm') val = Math.max(40, Math.min(240, Math.round(raw)));
      else val = Math.max(0, Math.min(100, Math.round(raw)));
    }
    view.setInt16(0xe4 + i * 2, val, true);
  }

  // [8] REVERB (0xfc)
  const revMod = preset.pedals.REVERB || {model:'plate', params:{}};
  const revItem = findCatalogItem('REVERB', revMod.model);
  const revKeys = Object.keys(revItem.params);
  for (let i = 0; i < 12; i++) {
    const k = revKeys[i];
    const val = k ? Math.max(0, Math.min(100, Math.round(revMod.params[k] ?? revItem.params[k] ?? 20))) : 0;
    view.setInt16(0xfc + i * 2, val, true);
  }

  // [9] VOL (0x114)
  const volLv = Math.max(0, Math.min(100, Math.round((preset.pedals.VOL && preset.pedals.VOL.params.Level) ?? 100)));
  view.setInt16(0x114, volLv, true);

  return bytes;
}

// ============================================================================
// 6. RENDERIZAÇÃO DO VISOR LCD MK-20 E MÓDULOS DE PEDAIS
// ============================================================================
function getPedalSvgSymbol(block) {
  switch(block) {
    case 'FX': return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><path d=""M2 8 Q7 1, 13 8 T24 8"" stroke=""#FFF"" stroke-width=""2"" fill=""none""/></svg>';
    case 'NS': return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><path d=""M3 13 L11 13 L19 3 L24 3"" stroke=""#FFF"" stroke-width=""2"" fill=""none""/></svg>';
    case 'DS': return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><path d=""M2 13 L6 4 L11 12 L16 3 L21 12 L24 6"" stroke=""#FFF"" stroke-width=""2"" fill=""none""/></svg>';
    case 'AMP': return '<div style=""display:flex;gap:3px;align-items:center;""><span style=""width:6px;height:6px;border-radius:50%;background:#FFF;""></span><span style=""width:5px;height:5px;border-radius:50%;background:#EF4444;""></span><span style=""width:6px;height:6px;border-radius:50%;background:#FFF;""></span></div>';
    case 'CAB': return '<div style=""width:24px;height:16px;border:1px solid rgba(255,255,255,0.7);background:repeating-linear-gradient(45deg,transparent,transparent 2px,rgba(255,255,255,0.25) 2px,rgba(255,255,255,0.25) 4px);""></div>';
    case 'EQ': return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><line x1=""5"" y1=""14"" x2=""5"" y2=""5"" stroke=""#FFF"" stroke-width=""2""/><line x1=""13"" y1=""14"" x2=""13"" y2=""2"" stroke=""#FFF"" stroke-width=""2""/><line x1=""21"" y1=""14"" x2=""21"" y2=""7"" stroke=""#FFF"" stroke-width=""2""/></svg>';
    case 'MOD': return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><circle cx=""10"" cy=""8"" r=""5"" stroke=""#FFF"" stroke-width=""1.6"" fill=""none""/><circle cx=""16"" cy=""8"" r=""5"" stroke=""#FFF"" stroke-width=""1.6"" fill=""none""/></svg>';
    case 'DELAY': return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><line x1=""5"" y1=""14"" x2=""5"" y2=""2"" stroke=""#FFF"" stroke-width=""2.2""/><line x1=""13"" y1=""14"" x2=""13"" y2=""6"" stroke=""#FFF"" stroke-width=""2""/><line x1=""21"" y1=""14"" x2=""21"" y2=""10"" stroke=""#FFF"" stroke-width=""1.8""/></svg>';
    case 'REVERB': return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><circle cx=""13"" cy=""8"" r=""2"" fill=""#FFF""/><circle cx=""13"" cy=""8"" r=""6"" stroke=""#FFF"" stroke-width=""1.4"" fill=""none""/></svg>';
    default: return '<svg width=""26"" height=""16"" viewBox=""0 0 26 16""><rect x=""9"" y=""2"" width=""8"" height=""12"" rx=""2"" stroke=""#FFF"" stroke-width=""1.6"" fill=""none""/></svg>';
  }
}

function renderLcdPanel() {
  document.getElementById('lblPresetTitle').textContent = currentPreset.title;
  document.getElementById('lblPresetStyle').textContent = currentPreset.style;
  document.getElementById('lblPresetGuitar').textContent = '🎸 ' + currentPreset.guitarType;
  document.getElementById('lblPresetBpm').textContent = '⏱ ' + currentPreset.bpm + ' BPM';
  document.getElementById('lblPresetPickup').textContent = currentPreset.pickupPosition;
  document.getElementById('lblPresetDesc').textContent = currentPreset.description;

  document.getElementById('lcdMasterVol').textContent = currentPreset.masterVolume;
  document.getElementById('lcdBpmVal').textContent = currentPreset.bpm;

  let activeCount = 0;
  BLOCK_KEYS.forEach(k => { if (currentPreset.pedals[k] && currentPreset.pedals[k].active) activeCount++; });
  document.getElementById('lcdCpuVal').textContent = (22 + activeCount * 5) + '%';

  const topRow = document.getElementById('lcdRowTop');
  const botRow = document.getElementById('lcdRowBottom');
  topRow.innerHTML = '';
  botRow.innerHTML = '';

  BLOCK_KEYS.forEach((k, idx) => {
    const mod = currentPreset.pedals[k] || {active:false, model:''};
    const isSel = selectedBlock === k;
    const div = document.createElement('div');
    div.className = 'lcd-slot' + (isSel ? ' selected' : '') + (!mod.active ? ' off' : '');
    div.onclick = () => { selectedBlock = k; renderAll(); };
    div.ondblclick = () => { mod.active = !mod.active; renderAll(); };
    const lbl = k === 'DELAY' ? 'DLY' : (k === 'REVERB' ? 'REV' : k);
    div.innerHTML = `
      <div class=""lcd-slot-icon"" style=""background:${BLOCK_COLORS[k]};"">
        <div style=""width:6px;height:6px;border-radius:50%;background:${mod.active?'#5CE1E6':'#1E293B'};""></div>
        ${getPedalSvgSymbol(k)}
        <div style=""width:10px;height:4px;border-radius:2px;background:rgba(255,255,255,0.65);""></div>
      </div>
      <div class=""lcd-slot-label"">${lbl}</div>
    `;
    if (idx < 5) topRow.appendChild(div);
    else botRow.appendChild(div);
  });

  const selMod = currentPreset.pedals[selectedBlock];
  const btnOnOff = document.getElementById('btnHwOnOff');
  if (selMod && selMod.active) {
    btnOnOff.className = 'btn-hw-onoff on';
    btnOnOff.textContent = 'ON - OFF (LIGADO)';
  } else {
    btnOnOff.className = 'btn-hw-onoff';
    btnOnOff.textContent = 'ON - OFF (BYPASS)';
  }
  const idx = BLOCK_KEYS.indexOf(selectedBlock) + 1;
  document.getElementById('lblBlockStep').textContent = idx + ' / 10 • ' + selectedBlock;
}

function formatParamValue(block, modelName, key, val) {
  const k = key.toLowerCase();
  if (k === 'sync' || k === 'lptype' || k === 'boost') return val ? 'ON' : 'OFF';
  if (k === 'inputdetect') return val ? 'PreGate' : 'Input';
  if (block === 'FX' && modelName === 'Whammy' && k === 'mode') {
    return val === 1 ? 'mbody' : (val === 3 ? 'oct2' : 'oct1');
  }
  if (block === 'MOD' && k === 'lfo') {
    const waves = ['sin','sqw','saw','tri','wav'];
    return waves[Math.max(0, Math.min(4, Math.round(val)-1))] || 'sin';
  }
  if (block === 'MOD' && modelName === 'CHORUS STEREO' && k === 'mode') {
    const modes = ['harm','sin','sqw','saw','tri','wav'];
    return modes[Math.max(0, Math.min(5, Math.round(val)-1))] || 'harm';
  }
  if (block === 'EQ') return (val > 0 ? '+' : '') + Math.round(val) + 'dB';
  if (block === 'CAB' && k === 'low cut') return Math.round(val) + 'Hz';
  if (block === 'CAB' && k === 'high cut') return Math.round(val > 200 ? val : val * 100) + 'Hz';
  if (block === 'DELAY' && k === 'time') {
    const ms = val <= 2 ? Math.round(val * 1000) : Math.round(val);
    return (ms / 1000).toFixed(3) + 's';
  }
  if (k === 'speed' || k === 'sense' || k === 'ratio') return Number(val).toFixed(1);
  return Math.round(val);
}

function getParamLimits(block, modelName, key) {
  const k = key.toLowerCase();
  if (k === 'sync' || k === 'lptype' || k === 'boost' || k === 'inputdetect') return {min:0, max:1, step:1};
  if (k === 'syncbpm') return {min:40, max:240, step:1};
  if (block === 'EQ') return {min:-12, max:12, step:1};
  if (block === 'CAB' && k === 'low cut') return {min:20, max:300, step:5};
  if (block === 'CAB' && k === 'high cut') return {min:5000, max:18000, step:100};
  if (block === 'DELAY' && k === 'time') return {min:100, max:2000, step:10};
  if (block === 'NS' && k === 'thd' && modelName.indexOf('gate') >= 0) return {min:-80, max:-30, step:1};
  if (k === 'speed') return {min:0.1, max:10.0, step:0.1};
  if (block === 'FX' && (k === 'sense' || k === 'ratio')) return {min:0.1, max:10.0, step:0.1};
  if (k === 'semi') return {min:-12, max:12, step:1};
  if (k === 'high pitch') return {min:0, max:24, step:1};
  if (k === 'low pitch') return {min:-24, max:0, step:1};
  return {min:0, max:100, step:1};
}

function renderStompEditor() {
  const area = document.getElementById('stompEditorArea');
  area.innerHTML = '';
  const blocksToRender = showAllPedals ? BLOCK_KEYS : [selectedBlock];

  blocksToRender.forEach(block => {
    const mod = currentPreset.pedals[block];
    const catalogList = MK20_CATALOG[block] || [];
    const curItem = findCatalogItem(block, mod.model);
    const isDropOpen = dropdownOpenFor === block;

    const card = document.createElement('div');
    card.className = 'stomp-card';
    card.style.marginBottom = '10px';

    let dropdownHtml = '';
    if (isDropOpen) {
      dropdownHtml = `<div class=""dropdown-menu-list"">` +
        catalogList.map(item => `
          <div class=""dropdown-item ${item.name===curItem.name?'selected':''}"" onclick=""selectCatalogModel('${block}', '${item.name.replace(/'/g,""\\'"")}')"" >
            <div style=""display:flex;justify-content:space-between;align-items:center;"">
              <strong style=""font-size:12.5px;color:var(--text);"">#${item.id} • ${item.name}</strong>
              <span class=""badge-mk20"">${item.type}</span>
            </div>
            <div style=""font-size:11px;color:var(--text-muted);margin-top:2px;"">${item.desc}</div>
          </div>
        `).join('') + `</div>`;
    }

    const paramKeys = Object.keys(curItem.params);
    const knobsHtml = paramKeys.map(k => {
      let val = mod.params[k] !== undefined ? mod.params[k] : curItem.params[k];
      if (block === 'CAB' && k === 'High Cut' && val <= 200) val = val * 100;
      const lim = getParamLimits(block, curItem.name, k);
      const disp = formatParamValue(block, curItem.name, k, val);
      return `
        <div class=""knob-card"">
          <div class=""knob-val"" id=""kval-${block}-${k.replace(/\s+/g,'_')}"">${disp}</div>
          <input type=""range"" class=""knob-slider"" min=""${lim.min}"" max=""${lim.max}"" step=""${lim.step}"" value=""${val}""
            oninput=""onKnobChange('${block}', '${k}', this.value)"">
          <div class=""knob-name"">${k}</div>
        </div>
      `;
    }).join('');

    card.innerHTML = `
      <div class=""stomp-header"">
        <div style=""display:flex;align-items:center;gap:8px;"">
          <span class=""stomp-badge"" style=""background:${BLOCK_COLORS[block]};"">${block}</span>
          <div>
            <div style=""font-size:13px;font-weight:800;"">${curItem.name}</div>
            <div style=""font-size:11px;color:var(--text-muted);"">${curItem.type} • ${catalogList.length} modelos disponíveis</div>
          </div>
        </div>
        <button class=""btn-hw-onoff ${mod.active?'on':''}"" style=""padding:5px 10px;"" onclick=""toggleBlockActive('${block}')"">
          ${mod.active ? 'ATIVO' : 'BYPASS'}
        </button>
      </div>

      <button class=""custom-dropdown-btn"" onclick=""toggleModelDropdown('${block}')"">
        <div>
          <div style=""font-size:10px;color:var(--text-muted);"">Modelo Oficial MK-20 Selecionado:</div>
          <div style=""font-size:13px;font-weight:800;color:var(--primary);"">#${curItem.id} • ${curItem.name} (${curItem.type})</div>
        </div>
        <span>${isDropOpen ? '▲' : '▼'}</span>
      </button>
      ${dropdownHtml}

      <div class=""model-desc-box"">ℹ️ ${curItem.desc}</div>

      <div class=""knobs-grid"">${knobsHtml}</div>
    `;
    area.appendChild(card);
  });
}

function toggleModelDropdown(block) {
  dropdownOpenFor = (dropdownOpenFor === block) ? null : block;
  renderStompEditor();
}

function focusModelSelector() {
  dropdownOpenFor = selectedBlock;
  renderStompEditor();
}

function selectCatalogModel(block, modelName) {
  const item = findCatalogItem(block, modelName);
  currentPreset.pedals[block].model = item.name;
  currentPreset.pedals[block].params = cloneParams(item.params);
  dropdownOpenFor = null;
  renderAll();
}

function onKnobChange(block, key, rawVal) {
  const num = parseFloat(rawVal);
  currentPreset.pedals[block].params[key] = num;
  const curItem = findCatalogItem(block, currentPreset.pedals[block].model);
  const el = document.getElementById('kval-' + block + '-' + key.replace(/\s+/g,'_'));
  if (el) el.textContent = formatParamValue(block, curItem.name, key, num);
  updateBinaryTabPreview();
}

function toggleBlockActive(block) {
  currentPreset.pedals[block].active = !currentPreset.pedals[block].active;
  renderAll();
}

function toggleSelectedBlockActive() {
  toggleBlockActive(selectedBlock);
}

function stepBlock(dir) {
  const i = BLOCK_KEYS.indexOf(selectedBlock);
  const next = (i + dir + BLOCK_KEYS.length) % BLOCK_KEYS.length;
  selectedBlock = BLOCK_KEYS[next];
  renderAll();
}

function toggleShowAllPedals() {
  showAllPedals = !showAllPedals;
  document.getElementById('lblToggleAllBtn').textContent = showAllPedals
    ? '🔼 Mostrar Apenas o Módulo Selecionado'
    : '📋 Ver Todos os 10 Módulos Empilhados';
  renderStompEditor();
}

function adjustMasterVolPrompt() {
  const v = prompt('Volume Geral do Preset (Preset Vol 0 a 100):', currentPreset.masterVolume);
  if (v !== null && !isNaN(parseInt(v))) {
    currentPreset.masterVolume = Math.max(0, Math.min(100, parseInt(v)));
    renderAll();
  }
}

function renameCurrentPreset() {
  const n = prompt('Novo nome do Preset (máx 15 caracteres para visor MK-20):', currentPreset.title);
  if (n && n.trim()) {
    currentPreset.title = n.trim();
    renderAll();
  }
}

// ============================================================================
// 7. MODOS DE ENTRADA (NOME COM CONFIRMAÇÃO, YOUTUBE, VIBE MIC) & GEMINI IA
// ============================================================================
function setInputMode(mode) {
  inputMode = mode;
  ['SONG','YOUTUBE','AUDIO'].forEach(m => {
    document.getElementById('mode-' + m).className = 'seg-btn' + (m === mode ? ' active' : '');
    document.getElementById('pane-' + m).style.display = (m === mode ? 'block' : 'none');
  });
}

function renderSongConfirmArea() {
  const area = document.getElementById('songConfirmArea');
  const q = document.getElementById('songInput').value.trim();
  if (!q) { area.innerHTML = ''; return; }

  if (confirmedSong) {
    area.innerHTML = `
      <div class=""confirmed-song-card"">
        <div class=""song-cover"">🎵</div>
        <div style=""flex:1;"">
          <div style=""font-size:10px;font-weight:900;color:var(--primary);"">✓ MÚSICA CONFIRMADA PARA A IA</div>
          <div style=""font-size:13px;font-weight:800;"">${confirmedSong.artist} – ${confirmedSong.title}</div>
          <div style=""font-size:11px;color:var(--text-muted);"">${confirmedSong.album} • ${confirmedSong.genre}</div>
        </div>
        <button class=""chip"" onclick=""confirmedSong=null; renderSongConfirmArea();"">Ver outras versões</button>
      </div>
    `;
    return;
  }

  const cands = getSongCandidates(q);
  area.innerHTML = `
    <div style=""font-size:11px;font-weight:700;color:var(--text-muted);margin-bottom:6px;"">
      Selecione a opção exata para a IA (${cands.length} opções):
    </div>
  ` + cands.map((c, idx) => `
    <div class=""song-option-card"" onclick=""confirmCandidate(${idx})"">
      <div class=""song-cover"">💿</div>
      <div style=""flex:1;"">
        <div style=""font-size:12.5px;font-weight:800;"">${c.artist} – ${c.title}</div>
        <div style=""font-size:11px;color:var(--text-muted);"">${c.album} • ${c.genre}</div>
      </div>
      <span class=""badge-mk20"">Escolher</span>
    </div>
  `).join('');
}

function confirmCandidate(idx) {
  const q = document.getElementById('songInput').value.trim();
  const cands = getSongCandidates(q);
  if (cands[idx]) {
    confirmedSong = cands[idx];
    document.getElementById('songInput').value = confirmedSong.artist + ' - ' + confirmedSong.title;
    renderSongConfirmArea();
  }
}

function onSongInputChanged() {
  confirmedSong = null;
  renderSongConfirmArea();
}

function clearSongInput() {
  document.getElementById('songInput').value = '';
  confirmedSong = null;
  renderSongConfirmArea();
}

function pickQuickTag(tag) {
  document.getElementById('songInput').value = tag;
  const cands = getSongCandidates(tag);
  confirmedSong = cands[0] || {artist:'Preset20 AI', title:tag, album:'Catálogo Oficial MK-20', genre:'Custom Tone'};
  renderSongConfirmArea();
}

function onYoutubeInputChanged() {
  const url = document.getElementById('ytUrlInput').value.trim();
  const area = document.getElementById('ytPreviewArea');
  const m = url.match(/(?:v=|youtu\.be\/|shorts\/)([A-Za-z0-9_-]{11})/);
  if (m) {
    const vid = m[1];
    area.innerHTML = `
      <div class=""song-option-card"" style=""cursor:default;"">
        <div class=""song-cover""><img src=""https://img.youtube.com/vi/${vid}/hqdefault.jpg""></div>
        <div style=""flex:1;"">
          <div style=""font-size:11px;color:var(--primary);font-weight:800;"">✓ Vídeo do YouTube Identificado</div>
          <div style=""font-size:11px;color:var(--text-muted);"">ID: ${vid} • Preencha ou edite o nome da música abaixo</div>
        </div>
      </div>
    `;
  } else {
    area.innerHTML = '';
  }
}

function syncConfirmedFromYt() {
  const name = document.getElementById('ytPresetNameInput').value.trim();
  if (name) confirmedSong = {artist:'YouTube Reference', title:name, album:'YouTube Video', genre:'Video Tone Match'};
}

function toggleMicRecording() {
  const btn = document.getElementById('btnMicRecord');
  const status = document.getElementById('micStatusText');
  const bars = document.querySelectorAll('.mbar');
  if (!micRecording) {
    micRecording = true;
    btn.textContent = '⏹ Parar e Analisar Vibe Agora';
    btn.style.borderColor = 'var(--danger)';
    status.textContent = 'Ouvindo frequências, ganho e ataques rítmicos...';
    micInterval = setInterval(() => {
      bars.forEach(b => { b.style.height = (6 + Math.floor(Math.random() * 28)) + 'px'; });
    }, 120);
  } else {
    micRecording = false;
    clearInterval(micInterval);
    bars.forEach(b => { b.style.height = '10px'; });
    btn.textContent = '🎙️ Capturar Vibe pelo Microfone';
    btn.style.borderColor = 'var(--border)';
    status.textContent = 'Análise acústica concluída com sucesso!';
    document.getElementById('vibeDetectedArea').innerHTML = `
      <div class=""confirmed-song-card"">
        <div class=""song-cover"">🎙️</div>
        <div style=""flex:1;"">
          <div style=""font-size:10px;font-weight:900;color:var(--primary);"">✓ VIBE DETECTADA PELO MICROFONE</div>
          <div style=""font-size:12.5px;font-weight:800;"">Ganho Médio-Alto • 118 BPM • Médios Quentes</div>
          <div style=""font-size:11px;color:var(--text-muted);"">Sugestão MK-20: 86OD_JCM800 + MW-808 + 41Mar2551B412</div>
        </div>
      </div>
    `;
  }
}

async function generateAiPreset() {
  if (!appSettings.isPro && appSettings.dailyUsed >= 2) {
    openModal('modalPro');
    return;
  }

  let query = document.getElementById('songInput').value.trim();
  if (inputMode === 'YOUTUBE') query = document.getElementById('ytPresetNameInput').value.trim() || 'YouTube Tone Match';
  if (inputMode === 'AUDIO') query = 'Vibe Microfone Lead 86OD_JCM800';
  const notes = document.getElementById('notesInput').value.trim();

  const btn = document.getElementById('btnGenerateMain');
  btn.disabled = true;
  btn.textContent = '⏳ Gerando Engenharia Acústica MK-20...';

  try {
    if (appSettings.geminiApiKey && appSettings.geminiApiKey.trim().length > 10) {
      const aiPreset = await callGeminiForPreset(query, notes);
      if (aiPreset) currentPreset = aiPreset;
      else currentPreset = createPresetFromSong(query, notes);
    } else {
      await new Promise(r => setTimeout(r, 350));
      currentPreset = createPresetFromSong(query, notes);
    }

    if (!appSettings.isPro) {
      appSettings.dailyUsed = (appSettings.dailyUsed || 0) + 1;
      saveSettingsToServer();
    }
    renderAll();
  } finally {
    btn.disabled = false;
    btn.textContent = '⚡ Gerar Preset MK-20';
  }
}

async function callGeminiForPreset(query, notes) {
  try {
    const base = createPresetFromSong(query, notes);
    const prompt = `Você é o engenheiro de timbres oficial da pedaleira M-VAVE MK-20. Crie um timbre para: ""${query}"" (${notes}). Responda APENAS um JSON com {"title":"...","style":"...","guitarType":"...","pickupPosition":"...","bpm":120,"description":"..."} em português.`;
    const res = await fetch('https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=' + encodeURIComponent(appSettings.geminiApiKey.trim()), {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] })
    });
    if (!res.ok) return base;
    const data = await res.json();
    const txt = data.candidates[0].content.parts[0].text;
    const m = txt.match(/\{[\s\S]*\}/);
    if (m) {
      const parsed = JSON.parse(m[0]);
      if (parsed.title) base.title = String(parsed.title).substring(0, 18);
      if (parsed.style) base.style = parsed.style;
      if (parsed.guitarType) base.guitarType = parsed.guitarType;
      if (parsed.pickupPosition) base.pickupPosition = parsed.pickupPosition;
      if (parsed.bpm) base.bpm = Number(parsed.bpm) || 120;
      if (parsed.description) base.description = parsed.description;
    }
    return base;
  } catch(e) {
    return null;
  }
}

// ============================================================================
// 8. ASSISTENTE ROADIE IA (ABA 1)
// ============================================================================
let chatHistory = [];

function resetAssistantChat() {
  chatHistory = [{
    role: 'ai',
    text: 'Olá! Sou o **Roadie IA da M-VAVE MK-20**. Posso ajustar o timbre atual na hora (tirar som abafado, cortar agudo ardido/abelha, dar mais sustain) ou criar um timbre novo completo de 412 bytes. O que deseja ajustar?',
    changes: [],
    preset: null
  }];
  renderAssistantChat();
}

function sendRoadieQuick(text) {
  document.getElementById('roadieInput').value = text;
  sendRoadieMessage();
}

function sendRoadieMessage() {
  const inp = document.getElementById('roadieInput');
  const text = inp.value.trim();
  if (!text) return;
  inp.value = '';

  chatHistory.push({ role: 'user', text: text });
  const lower = text.toLowerCase();
  const nextPreset = JSON.parse(JSON.stringify(currentPreset));
  const changes = [];
  let reply = '';

  if (/abelha|ardido|agudo|estridente/.test(lower)) {
    nextPreset.pedals.CAB.params['High Cut'] = 6400;
    nextPreset.pedals.AMP.params.Treble = Math.max(35, (nextPreset.pedals.AMP.params.Treble || 58) - 8);
    nextPreset.pedals.AMP.params.Pres = Math.max(35, (nextPreset.pedals.AMP.params.Pres || 55) - 10);
    changes.push('CAB High Cut reduzido para 6400Hz (filtro anti-abelha digital)');
    changes.push('AMP Treble e Pres suavizados em -8% / -10%');
    reply = 'Apliquei o corte cirúrgico anti-abelha no gabinete IR e suavizei a presença do amplificador para deixar o timbre aveludado e pronto para linha.';
  } else if (/abafado|escuro|clareza|abrir|brilho/.test(lower)) {
    nextPreset.pedals.CAB.params['Low Cut'] = 95;
    nextPreset.pedals.CAB.params['High Cut'] = 7600;
    nextPreset.pedals.AMP.params.Middle = Math.min(85, (nextPreset.pedals.AMP.params.Middle || 55) + 8);
    nextPreset.pedals.AMP.params.Treble = Math.min(85, (nextPreset.pedals.AMP.params.Treble || 55) + 7);
    changes.push('CAB Low Cut em 95Hz para limpar subgraves embolados');
    changes.push('AMP Middle (+8) e Treble (+7) abertos com High Cut em 7600Hz');
    reply = 'Limpei a região de subgraves do gabinete e abri os médios-agudos do pré-amplificador para o instrumento cortar a mix com nitidez.';
  } else if (/sustain|solo|lead|destaque/.test(lower)) {
    nextPreset.pedals.DS.active = true;
    nextPreset.pedals.DS.model = 'MW-808';
    nextPreset.pedals.DS.params = {gain:34, tone:56, level:88};
    nextPreset.pedals.DELAY.active = true;
    nextPreset.pedals.DELAY.params.Mix = 30;
    nextPreset.pedals.DELAY.params.Fb = 36;
    changes.push('DS MW-808 ativado como booster de sustain (Level 88)');
    changes.push('DELAY ajustado com Mix 30% e Feedback 36% para cauda de solo');
    reply = 'Empurrei o amplificador com o Tube Screamer MW-808 e encorpei a cauda do Delay para sustentar as notas nos bends e solos.';
  } else {
    const custom = createPresetFromSong(text, '');
    Object.assign(nextPreset, custom);
    changes.push('AMP configurado para ' + nextPreset.pedals.AMP.model);
    changes.push('CAB configurado para ' + nextPreset.pedals.CAB.model);
    changes.push('Calibragem 412B M-EFCS aplicada');
    reply = 'Configurei uma nova cadeia completa na MK-20 para o estilo solicitado. Confira o cartão interativo abaixo:';
  }

  currentPreset = nextPreset;
  chatHistory.push({
    role: 'ai',
    text: reply,
    changes: changes,
    preset: JSON.parse(JSON.stringify(nextPreset))
  });
  renderAll();
}

function renderAssistantChat() {
  document.getElementById('roadieCurrentTitle').textContent = currentPreset.title;
  document.getElementById('roadieCurrentAmp').textContent = currentPreset.pedals.AMP.model;
  const box = document.getElementById('chatMessages');
  box.innerHTML = chatHistory.map((m, idx) => {
    if (m.role === 'user') return `<div class=""msg user"">${m.text}</div>`;
    let chHtml = '';
    if (m.changes && m.changes.length) {
      chHtml = `<div style=""margin-top:8px;padding:8px;border-radius:8px;background:rgba(16,185,129,0.1);border:1px solid rgba(16,185,129,0.3);font-size:11px;"">
        <div style=""font-weight:800;color:var(--primary);margin-bottom:4px;"">🔧 Ajustes aplicados na pedaleira:</div>
        ${m.changes.map(c => `<div>✓ ${c}</div>`).join('')}
      </div>`;
    }
    let cardHtml = '';
    if (m.preset) {
      cardHtml = `
        <div class=""ai-preset-card"">
          <div style=""display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;"">
            <strong>🎸 ${m.preset.title}</strong>
            <span class=""badge-mk20"">412 BYTES</span>
          </div>
          <div style=""font-size:11px;color:var(--text-muted);margin-bottom:8px;"">
            AMP: ${m.preset.pedals.AMP.model} • CAB: ${m.preset.pedals.CAB.model}
          </div>
          <div style=""display:flex;gap:6px;"">
            <button class=""btn-pri"" style=""padding:6px;font-size:11px;"" onclick=""applyChatPreset(${idx})"">✓ Aplicar</button>
            <button class=""btn-sec"" style=""padding:6px;font-size:11px;"" onclick=""applyChatPreset(${idx}); saveCurrentPresetQuick();"">💾 Salvar</button>
            <button class=""btn-sec"" style=""padding:6px;font-size:11px;"" onclick=""applyChatPreset(${idx}); exportMk20File(false);"">⬇ .MK20</button>
          </div>
        </div>
      `;
    }
    return `<div class=""msg ai""><div>${m.text}</div>${chHtml}${cardHtml}</div>`;
  }).join('');
}

function applyChatPreset(idx) {
  if (chatHistory[idx] && chatHistory[idx].preset) {
    currentPreset = JSON.parse(JSON.stringify(chatHistory[idx].preset));
    renderAll();
    selectTab(0);
  }
}

// ============================================================================
// 9. ABA BINÁRIO (.MK20 / M-EFCS), IMPORTAÇÃO E EXPORTAÇÃO
// ============================================================================
function setBinaryMode(mode, btnEl) {
  binaryMode = mode;
  document.querySelectorAll('#binSizeChips .chip').forEach(c => c.classList.remove('active'));
  if (btnEl) btnEl.classList.add('active');
  if (mode === 'TEMPLATE_CLONE' && !customTemplateBytes) {
    document.getElementById('fileTemplateInput').click();
  }
  updateBinaryTabPreview();
}

function updateBinaryTabPreview() {
  const bytes412 = serializePreset412(currentPreset, customTemplateBytes, binaryMode === 'TEMPLATE_CLONE');
  const hexLines = [];
  for (let offset = 0; offset < bytes412.length; offset += 16) {
    const slice = bytes412.slice(offset, offset + 16);
    const hex = Array.from(slice).map(b => b.toString(16).padStart(2,'0').toUpperCase()).join(' ');
    const ascii = Array.from(slice).map(b => (b >= 32 && b <= 126) ? String.fromCharCode(b) : '.').join('');
    hexLines.push(offset.toString(16).padStart(4,'0').toUpperCase() + ': ' + hex.padEnd(48,' ') + ' | ' + ascii);
  }
  document.getElementById('hexPreviewBox').textContent = hexLines.join('\n');
  document.getElementById('jsonPreviewBox').textContent = JSON.stringify(currentPreset, null, 2);
}

function exportMk20File(asAmk20) {
  const bytes = serializePreset412(currentPreset, customTemplateBytes, binaryMode === 'TEMPLATE_CLONE');
  const blob = new Blob([bytes], {type: 'application/octet-stream'});
  const ext = asAmk20 ? '.amk20' : '.mk20';
  const safe = (currentPreset.title || 'preset_mk20').replace(/[^a-zA-Z0-9_-]/g, '_');
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = safe + ext;
  a.click();
}

function onTemplateFileLoaded(ev) {
  const f = ev.target.files[0];
  if (!f) return;
  const reader = new FileReader();
  reader.onload = () => {
    customTemplateBytes = new Uint8Array(reader.result);
    customTemplateName = f.name;
    const banner = document.getElementById('templateStatusBanner');
    banner.style.display = 'block';
    banner.innerHTML = `✓ Molde Oficial Carregado: <strong>${f.name}</strong> (${customTemplateBytes.length} bytes)`;
    updateBinaryTabPreview();
  };
  reader.readAsArrayBuffer(f);
}

function onImportFileLoaded(ev) {
  const f = ev.target.files[0];
  if (!f) return;
  if (f.name.endsWith('.json')) {
    const r = new FileReader();
    r.onload = () => {
      try {
        const parsed = JSON.parse(r.result);
        if (Array.isArray(parsed)) {
          savedPresets = parsed;
          savePresetsToServer();
          renderSavedPresets();
          alert('Backup com ' + parsed.length + ' presets importado com sucesso!');
        } else if (parsed.pedals) {
          currentPreset = parsed;
          renderAll();
          selectTab(0);
        }
      } catch(e) { alert('Erro ao ler arquivo JSON.'); }
    };
    r.readAsText(f);
  } else {
    const r = new FileReader();
    r.onload = () => {
      const b = new Uint8Array(r.result);
      if (b.length >= 80) {
        let name = '';
        for (let i = 0; i < 15; i++) if (b[i] >= 32 && b[i] <= 126) name += String.fromCharCode(b[i]);
        currentPreset.title = name.trim() || f.name.replace(/\..+$/,'');
        renderAll();
        selectTab(0);
        alert('Preset binário ' + f.name + ' (' + b.length + ' bytes) carregado na pedaleira!');
      }
    };
    r.readAsArrayBuffer(f);
  }
}

function copyPresetJson() {
  navigator.clipboard.writeText(JSON.stringify(currentPreset, null, 2));
  alert('JSON copiado para a área de transferência!');
}

function copyShareText() {
  const t = `🎸 Preset20 AI (M-VAVE MK-20 412B)\nTimbre: ${currentPreset.title}\nEstilo: ${currentPreset.style}\nAMP: ${currentPreset.pedals.AMP.model}\nCAB: ${currentPreset.pedals.CAB.model}`;
  navigator.clipboard.writeText(t);
  alert('Resumo do timbre copiado!');
}

function exportBackupJson() {
  const blob = new Blob([JSON.stringify(savedPresets, null, 2)], {type:'application/json'});
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'backup_presets_mk20.json';
  a.click();
}

// ============================================================================
// 10. SALVOS, CONFIGURAÇÕES E MODAIS
// ============================================================================
function openModal(id) { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

function openSaveModal() {
  document.getElementById('saveModalTitleInput').value = currentPreset.title;
  openModal('modalSave');
}

function confirmSavePreset() {
  const t = document.getElementById('saveModalTitleInput').value.trim();
  if (t) currentPreset.title = t;
  saveCurrentPresetQuick();
  closeModal('modalSave');
}

function saveCurrentPresetQuick() {
  const copy = JSON.parse(JSON.stringify(currentPreset));
  copy.savedAt = new Date().toLocaleDateString('pt-BR');
  savedPresets.unshift(copy);
  savePresetsToServer();
  renderAll();
}

function renderSavedPresets() {
  const q = (document.getElementById('savedSearchInput').value || '').toLowerCase();
  const list = savedPresets.filter(p => (p.title + ' ' + p.style + ' ' + p.guitarType).toLowerCase().indexOf(q) >= 0);
  document.getElementById('savedCountPill').textContent = savedPresets.length + ' salvos';
  document.getElementById('navSavedLabel').textContent = savedPresets.length > 0 ? `Salvos (${savedPresets.length})` : 'Salvos';

  const area = document.getElementById('savedListArea');
  if (list.length === 0) {
    area.innerHTML = `<div style=""text-align:center;padding:24px;color:var(--text-muted);font-size:12px;"">Nenhum preset salvo ainda. Clique em 💾 no topo para salvar seu timbre!</div>`;
    return;
  }
  area.innerHTML = list.map((p, idx) => `
    <div class=""song-option-card"" style=""cursor:default;"">
      <div class=""song-cover"">⭐</div>
      <div style=""flex:1;"">
        <div style=""font-size:13px;font-weight:800;"">${p.title}</div>
        <div style=""font-size:11px;color:var(--text-muted);"">${p.style} • AMP: ${p.pedals.AMP.model}</div>
      </div>
      <button class=""chip"" onclick=""loadSavedPreset(${idx})"">Carregar</button>
      <button class=""chip"" onclick=""deleteSavedPreset(${idx})"">🗑</button>
    </div>
  `).join('');
}

function loadSavedPreset(idx) {
  if (savedPresets[idx]) {
    currentPreset = JSON.parse(JSON.stringify(savedPresets[idx]));
    renderAll();
    selectTab(0);
  }
}

function deleteSavedPreset(idx) {
  savedPresets.splice(idx, 1);
  savePresetsToServer();
  renderSavedPresets();
}

function openSettingsModal() {
  document.getElementById('settingsApiKeyInput').value = appSettings.geminiApiKey || '';
  openModal('modalSettings');
}

function saveSettingsModal() {
  appSettings.geminiApiKey = document.getElementById('settingsApiKeyInput').value.trim();
  saveSettingsToServer();
  closeModal('modalSettings');
  renderAll();
}

function toggleProVersion(val) {
  appSettings.isPro = !!val;
  saveSettingsToServer();
  closeModal('modalPro');
  renderAll();
}

function openZoomModal(src, title) {
  document.getElementById('zoomImg').src = src;
  document.getElementById('zoomTitle').textContent = title || 'Visualização';
  openModal('modalZoom');
}

function filterHelpSection(sec, btnEl) {
  document.querySelectorAll('#tab-4 .chip').forEach(c => c.classList.remove('active'));
  if (btnEl) btnEl.classList.add('active');
  document.querySelectorAll('.help-sec').forEach(el => {
    el.style.display = (sec === 'ALL' || el.id === 'help-' + sec) ? 'block' : 'none';
  });
}

function selectTab(idx) {
  currentTab = idx;
  for (let i = 0; i < 5; i++) {
    document.getElementById('tab-' + i).style.display = (i === idx ? 'block' : 'none');
    document.getElementById('nav-' + i).className = 'nav-item' + (i === idx ? ' active' : '');
  }
  if (idx === 2) updateBinaryTabPreview();
}

function renderQuotaAndPro() {
  const btnPro = document.getElementById('btnProTop');
  const qBar = document.getElementById('quotaBanner');
  const qTxt = document.getElementById('quotaText');
  if (appSettings.isPro) {
    btnPro.className = 'btn-pro active';
    btnPro.innerHTML = '👑 PRO ATIVO';
    qBar.className = 'quota-bar pro';
    qTxt.textContent = '👑 Versão PRO Ativa • Gerações de timbres IA e exportações 412B ilimitadas';
  } else {
    btnPro.className = 'btn-pro';
    btnPro.innerHTML = '👑 PRO';
    qBar.className = 'quota-bar';
    const rem = Math.max(0, 2 - (appSettings.dailyUsed || 0));
    qTxt.textContent = `⚡ Cota gratuita: ${rem}/2 gerações disponíveis hoje`;
  }
}

function renderAll() {
  renderQuotaAndPro();
  renderSongConfirmArea();
  renderLcdPanel();
  renderStompEditor();
  renderAssistantChat();
  renderSavedPresets();
  updateBinaryTabPreview();
}

async function initFromServer() {
  try {
    const r1 = await fetch('/api/saved');
    if (r1.ok) savedPresets = await r1.json();
    const r2 = await fetch('/api/settings');
    if (r2.ok) {
      const s = await r2.json();
      if (s) appSettings = Object.assign(appSettings, s);
    }
  } catch(e) {}
  resetAssistantChat();
  renderAll();
}

function savePresetsToServer() {
  fetch('/api/saved', {method:'POST', body:JSON.stringify(savedPresets)}).catch(()=>{});
}

function saveSettingsToServer() {
  fetch('/api/settings', {method:'POST', body:JSON.stringify(appSettings)}).catch(()=>{});
}

window.addEventListener('DOMContentLoaded', initFromServer);
</script>
</body>
</html>";
        }
    }
}
