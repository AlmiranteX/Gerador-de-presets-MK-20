using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace Preset20AI
{
    static class Program
    {
        private static string AppDataDir;
        private static string SavedPresetsPath;
        private static string SettingsPath;

        [STAThread]
        static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            AppDataDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Preset20AI");
            if (!Directory.Exists(AppDataDir))
            {
                Directory.CreateDirectory(AppDataDir);
            }
            SavedPresetsPath = Path.Combine(AppDataDir, "saved_presets.json");
            SettingsPath = Path.Combine(AppDataDir, "settings.json");

            int port = GetAvailablePort();
            string url = "http://127.0.0.1:" + port + "/";

            Thread serverThread = new Thread(() => RunLocalServer(port));
            serverThread.IsBackground = true;
            serverThread.Start();

            Process browserProc = TryLaunchAppWindow(url);
            if (browserProc != null)
            {
                try
                {
                    browserProc.WaitForExit();
                }
                catch { }
            }
            else
            {
                // Caso nao encontre Edge/Chrome em modo App, abre no navegador padrao e mantem icone na bandeja
                Process.Start(url);
                Application.Run(new TrayHostContext(url));
            }
        }

        private static int GetAvailablePort()
        {
            TcpListener listener = new TcpListener(IPAddress.Loopback, 0);
            listener.Start();
            int port = ((IPEndPoint)listener.LocalEndpoint).Port;
            listener.Stop();
            return port;
        }

        private static Process TryLaunchAppWindow(string url)
        {
            string[] candidates = new string[]
            {
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), @"Microsoft\Edge\Application\msedge.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), @"Microsoft\Edge\Application\msedge.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), @"Google\Chrome\Application\chrome.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), @"Google\Chrome\Application\chrome.exe")
            };

            string userProfileDir = Path.Combine(AppDataDir, "WindowProfile");

            foreach (string exe in candidates)
            {
                if (File.Exists(exe))
                {
                    try
                    {
                        ProcessStartInfo psi = new ProcessStartInfo
                        {
                            FileName = exe,
                            Arguments = "--app=\"" + url + "\" --window-size=1180,860 --user-data-dir=\"" + userProfileDir + "\" --no-first-run --disable-extensions",
                            UseShellExecute = false
                        };
                        return Process.Start(psi);
                    }
                    catch { }
                }
            }
            return null;
        }

        private static void RunLocalServer(int port)
        {
            TcpListener server = new TcpListener(IPAddress.Loopback, port);
            server.Start();

            while (true)
            {
                try
                {
                    TcpClient client = server.AcceptTcpClient();
                    ThreadPool.QueueUserWorkItem(_ => HandleClient(client));
                }
                catch
                {
                    break;
                }
            }
        }

        private static void HandleClient(TcpClient client)
        {
            try
            {
                using (NetworkStream stream = client.GetStream())
                {
                    stream.ReadTimeout = 5000;
                    stream.WriteTimeout = 5000;

                    string requestLine = ReadLine(stream);
                    if (string.IsNullOrEmpty(requestLine)) return;

                    string[] parts = requestLine.Split(' ');
                    if (parts.Length < 2) return;

                    string method = parts[0].ToUpperInvariant();
                    string path = parts[1];

                    int contentLength = 0;
                    string line;
                    while (!string.IsNullOrEmpty(line = ReadLine(stream)))
                    {
                        if (line.StartsWith("Content-Length:", StringComparison.OrdinalIgnoreCase))
                        {
                            int.TryParse(line.Substring(15).Trim(), out contentLength);
                        }
                    }

                    byte[] bodyBytes = new byte[0];
                    if (contentLength > 0)
                    {
                        bodyBytes = new byte[contentLength];
                        int readTotal = 0;
                        while (readTotal < contentLength)
                        {
                            int r = stream.Read(bodyBytes, readTotal, contentLength - readTotal);
                            if (r <= 0) break;
                            readTotal += r;
                        }
                    }

                    if (method == "GET" && (path == "/" || path.StartsWith("/index")))
                    {
                        byte[] htmlBytes = Encoding.UTF8.GetBytes(GetModernAppHtml());
                        WriteHttpResponse(stream, "200 OK", "text/html; charset=utf-8", htmlBytes);
                    }
                    else if (method == "GET" && path == "/api/saved")
                    {
                        string json = File.Exists(SavedPresetsPath) ? File.ReadAllText(SavedPresetsPath, Encoding.UTF8) : "[]";
                        WriteHttpResponse(stream, "200 OK", "application/json; charset=utf-8", Encoding.UTF8.GetBytes(json));
                    }
                    else if (method == "POST" && path == "/api/saved")
                    {
                        string body = Encoding.UTF8.GetString(bodyBytes);
                        File.WriteAllText(SavedPresetsPath, body, Encoding.UTF8);
                        WriteHttpResponse(stream, "200 OK", "application/json; charset=utf-8", Encoding.UTF8.GetBytes("{\"ok\":true}"));
                    }
                    else if (method == "GET" && path == "/api/settings")
                    {
                        string json = File.Exists(SettingsPath) ? File.ReadAllText(SettingsPath, Encoding.UTF8) : "{\"apiKey\":\"\"}";
                        WriteHttpResponse(stream, "200 OK", "application/json; charset=utf-8", Encoding.UTF8.GetBytes(json));
                    }
                    else if (method == "POST" && path == "/api/settings")
                    {
                        string body = Encoding.UTF8.GetString(bodyBytes);
                        File.WriteAllText(SettingsPath, body, Encoding.UTF8);
                        WriteHttpResponse(stream, "200 OK", "application/json; charset=utf-8", Encoding.UTF8.GetBytes("{\"ok\":true}"));
                    }
                    else if (method == "POST" && path == "/api/shutdown")
                    {
                        WriteHttpResponse(stream, "200 OK", "application/json", Encoding.UTF8.GetBytes("{\"ok\":true}"));
                        Environment.Exit(0);
                    }
                    else
                    {
                        WriteHttpResponse(stream, "404 Not Found", "text/plain", Encoding.UTF8.GetBytes("Not Found"));
                    }
                }
            }
            catch { }
            finally
            {
                try { client.Close(); } catch { }
            }
        }

        private static string ReadLine(NetworkStream stream)
        {
            List<byte> bytes = new List<byte>();
            int b;
            while ((b = stream.ReadByte()) != -1)
            {
                if (b == '\n') break;
                if (b != '\r') bytes.Add((byte)b);
            }
            if (bytes.Count == 0 && b == -1) return null;
            return Encoding.UTF8.GetString(bytes.ToArray());
        }

        private static void WriteHttpResponse(NetworkStream stream, string status, string contentType, byte[] body)
        {
            string headers = "HTTP/1.1 " + status + "\r\n" +
                             "Content-Type: " + contentType + "\r\n" +
                             "Content-Length: " + body.Length + "\r\n" +
                             "Cache-Control: no-store\r\n" +
                             "Connection: close\r\n\r\n";
            byte[] headerBytes = Encoding.UTF8.GetBytes(headers);
            stream.Write(headerBytes, 0, headerBytes.Length);
            stream.Write(body, 0, body.Length);
            stream.Flush();
        }

        private static string GetModernAppHtml()
        {
            return @"<!DOCTYPE html>
<html lang=""pt-BR"">
<head>
<meta charset=""UTF-8"">
<meta name=""viewport"" content=""width=device-width, initial-scale=1.0"">
<title>Preset20 AI • M-VAVE MK-20</title>
<style>
  :root {
    --bg-dark: #090C12;
    --card-dark: #111622;
    --card-surface: #161C2A;
    --metal-border: #252E42;
    --metal-highlight: #34415C;
    --led-emerald: #00E676;
    --led-amber: #FFB300;
    --led-cyan: #00E5FF;
    --led-pink: #FF4081;
    --led-purple: #B388FF;
    --led-blue: #448AFF;
    --led-indigo: #7C4DFF;
    --text-primary: #F1F5F9;
    --text-secondary: #94A3B8;
    --text-muted: #64748B;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
  input, textarea { user-select: text; }
  body {
    background: var(--bg-dark);
    color: var(--text-primary);
    font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    overflow-x: hidden;
  }
  /* TOP APP BAR */
  .topbar {
    background: #0B0E17;
    border-bottom: 1px solid var(--metal-border);
    padding: 12px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: sticky;
    top: 0;
    z-index: 100;
  }
  .brand-row { display: flex; align-items: center; gap: 10px; }
  .led-dot {
    width: 11px; height: 11px; border-radius: 50%;
    background: var(--led-emerald);
    box-shadow: 0 0 10px var(--led-emerald);
  }
  .brand-title { font-weight: 900; font-size: 18px; letter-spacing: 1.5px; color: #fff; }
  .badge-pro {
    background: rgba(255,179,0,0.16);
    border: 1px solid var(--led-amber);
    color: var(--led-amber);
    font-size: 10px; font-weight: 900; font-family: Consolas, monospace;
    padding: 2px 7px; border-radius: 6px;
  }
  .brand-sub { font-size: 10px; color: var(--led-emerald); font-family: Consolas, monospace; letter-spacing: 0.8px; margin-top: 2px; }
  .top-actions { display: flex; align-items: center; gap: 8px; }
  .icon-btn {
    background: #141A29; border: 1px solid var(--metal-border); color: var(--text-primary);
    padding: 8px 12px; border-radius: 10px; cursor: pointer; font-size: 12px; font-weight: 600;
    display: flex; align-items: center; gap: 6px; transition: all 0.15s;
  }
  .icon-btn:hover { border-color: var(--led-emerald); background: #1A2235; }
  .icon-btn.emerald { background: rgba(0,230,118,0.14); border-color: var(--led-emerald); color: var(--led-emerald); }

  /* 5 TABS BAR */
  .tabs-bar {
    background: #0D111B;
    border-bottom: 1px solid var(--metal-border);
    display: flex;
    justify-content: center;
    gap: 8px;
    padding: 0 20px;
  }
  .tab-btn {
    background: transparent; border: none; color: var(--text-secondary);
    padding: 13px 22px; font-size: 13px; font-weight: 700; cursor: pointer;
    border-bottom: 3px solid transparent; display: flex; align-items: center; gap: 7px;
    transition: all 0.15s;
  }
  .tab-btn:hover { color: #fff; }
  .tab-btn.active { color: var(--led-emerald); border-bottom-color: var(--led-emerald); }

  /* MAIN CONTAINER */
  .main-wrap {
    max-width: 980px;
    width: 100%;
    margin: 0 auto;
    padding: 20px 20px 48px 20px;
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  /* CARDS */
  .card {
    background: linear-gradient(180deg, var(--card-dark) 0%, var(--card-surface) 100%);
    border: 1px solid var(--metal-border);
    border-radius: 16px;
    padding: 18px;
    box-shadow: 0 8px 24px rgba(0,0,0,0.35);
  }
  .card-header {
    display: flex; align-items: center; justify-content: space-between; cursor: pointer;
  }
  .card-title-row { display: flex; align-items: center; gap: 10px; }
  .card-title { font-size: 16px; font-weight: 800; color: #fff; }

  /* MODE SELECTOR (Nome | YouTube | Vibe) */
  .mode-tabs {
    display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
    background: #0B0F18; padding: 5px; border-radius: 12px; border: 1px solid var(--metal-border);
    margin: 14px 0;
  }
  .mode-tab {
    background: transparent; border: 1px solid transparent; color: var(--text-secondary);
    padding: 9px 12px; border-radius: 9px; font-size: 12px; font-weight: 700; cursor: pointer;
    display: flex; align-items: center; justify-content: center; gap: 6px; transition: all 0.15s;
  }
  .mode-tab.active {
    background: rgba(0,230,118,0.16); border-color: var(--led-emerald); color: var(--led-emerald);
  }

  /* INPUTS */
  .field-group { position: relative; margin-bottom: 12px; }
  .field-label { font-size: 11px; font-weight: 700; color: var(--led-emerald); margin-bottom: 5px; display: block; }
  .text-input, .text-area {
    width: 100%; background: #0C101A; border: 1px solid var(--metal-border);
    color: #fff; padding: 12px 14px; border-radius: 11px; font-size: 13px;
    outline: none; transition: border-color 0.15s;
  }
  .text-input:focus, .text-area:focus { border-color: var(--led-emerald); }
  .text-area { resize: vertical; min-height: 64px; font-family: inherit; }

  /* LIVE AUTOCOMPLETE DROPDOWN */
  .suggestions-box {
    background: #0E1422; border: 1px solid rgba(0,230,118,0.5); border-radius: 12px;
    margin-top: 6px; max-height: 230px; overflow-y: auto; display: none;
  }
  .suggestion-item {
    display: flex; align-items: center; gap: 10px; padding: 8px 12px;
    border-bottom: 1px solid #1C2539; cursor: pointer; transition: background 0.12s;
  }
  .suggestion-item:hover { background: rgba(0,230,118,0.12); }
  .suggestion-cover { width: 40px; height: 40px; border-radius: 6px; object-fit: cover; background: #1A2234; }
  .suggestion-title { font-size: 13px; font-weight: 700; color: #fff; }
  .suggestion-sub { font-size: 11px; color: var(--text-secondary); }

  /* QUICK TAG CHIPS */
  .chips-row { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 14px; }
  .chip {
    background: #0E1420; border: 1px solid var(--metal-border); color: var(--text-secondary);
    padding: 5px 11px; border-radius: 20px; font-size: 11px; font-weight: 600; cursor: pointer;
    transition: all 0.15s;
  }
  .chip:hover, .chip.active {
    background: rgba(0,230,118,0.18); border-color: var(--led-emerald); color: var(--led-emerald);
  }

  /* PRIMARY GENERATE BUTTON */
  .btn-generate {
    width: 100%; height: 50px; border-radius: 12px; border: none;
    background: var(--led-emerald); color: #04120B; font-size: 15px; font-weight: 900;
    cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px;
    box-shadow: 0 4px 16px rgba(0,230,118,0.28); transition: transform 0.1s, filter 0.15s;
  }
  .btn-generate:hover { filter: brightness(1.08); }
  .btn-generate:active { transform: scale(0.99); }

  /* PRESET TITLE BAR */
  .preset-bar {
    display: flex; align-items: center; justify-content: space-between; gap: 12px;
  }
  .preset-name { font-size: 22px; font-weight: 900; color: #fff; }
  .preset-meta { font-size: 12px; font-family: Consolas, monospace; color: var(--led-emerald); margin-top: 2px; }
  .rig-banner {
    background: #0F1420; border: 1px solid var(--metal-highlight); border-radius: 11px;
    padding: 10px 14px; font-size: 12px; color: var(--text-secondary); line-height: 1.45;
    display: flex; align-items: center; gap: 10px;
  }

  /* =========================================================================
     VISOR LCD OFICIAL DA M-VAVE MK-20 (IDÊNTICO AO MK20SIGNALCHAINPANEL.KT)
     ========================================================================= */
  .mk20-lcd {
    background: radial-gradient(circle at center, #0D1322 0%, #05070C 100%);
    border: 2px solid #2B3548;
    border-radius: 18px;
    padding: 16px;
    box-shadow: 0 12px 30px rgba(0,0,0,0.65);
  }
  .lcd-topbar {
    display: flex; align-items: center; gap: 10px; margin-bottom: 16px;
  }
  .lcd-slot {
    width: 86px; height: 54px; border-radius: 10px;
    background: linear-gradient(180deg, #3366FF 0%, #2249CC 100%);
    border: 1.5px solid #6B93FF;
    display: flex; align-items: center; justify-content: center;
    font-size: 26px; font-weight: 900; color: #fff; letter-spacing: 1px;
  }
  .lcd-status-box {
    flex: 1; height: 54px; border-radius: 10px;
    background: #141B29; border: 1.8px solid #3874FF;
    padding: 4px 16px; display: flex; align-items: center; justify-content: space-between;
  }
  .lcd-stat-label { font-size: 10px; font-weight: 600; color: #D6E4FF; }
  .lcd-stat-val { font-size: 20px; font-weight: 800; color: #fff; line-height: 1.05; }
  .lcd-divider { width: 2.5px; height: 28px; background: #fff; border-radius: 2px; }
  .lcd-battery {
    width: 22px; height: 48px; border: 2px solid #DCEBFF; border-radius: 4px;
    padding: 2px; display: flex; flex-direction: column; justify-content: flex-end; gap: 2px;
    position: relative;
  }
  .lcd-battery::before {
    content: ''; position: absolute; top: -5px; left: 5px; width: 8px; height: 3px;
    background: #DCEBFF; border-radius: 1px;
  }
  .bat-bar { height: 8px; background: #00E676; border-radius: 1px; }

  /* 2 FILEIRAS DE 5 BLOCOS CONECTADOS PELO CABO BRANCO EM S */
  .lcd-chain-stage {
    position: relative;
    width: 100%;
    height: 190px;
    margin: 8px 0 14px 0;
  }
  .lcd-cable-svg {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none;
  }
  .lcd-rows-wrap {
    position: relative; z-index: 2; width: 100%; height: 100%;
    display: flex; flex-direction: column; justify-content: space-between;
    padding: 0 24px;
  }
  .lcd-row {
    display: flex; align-items: center; justify-content: space-between; height: 82px;
  }
  .lcd-node-frame {
    padding: 5px; border-radius: 12px; cursor: pointer; transition: all 0.15s;
    border: 2.4px solid transparent;
  }
  .lcd-node-frame.selected {
    background: rgba(255,255,255,0.13);
    border-color: #F0F6FF;
    box-shadow: 0 0 14px rgba(240,246,255,0.35);
  }
  .lcd-block {
    width: 64px; height: 64px; border-radius: 8px;
    border: 1.5px solid rgba(255,255,255,0.65);
    display: flex; flex-direction: column; align-items: center; justify-content: space-between;
    padding: 4px; transition: opacity 0.2s, transform 0.12s;
  }
  .lcd-block.amp-block { width: 78px; height: 58px; }
  .lcd-block.cab-block { width: 74px; height: 64px; }
  .lcd-block.vol-block { width: 48px; height: 68px; justify-content: center; border-style: dashed; }
  .lcd-block.bypassed { opacity: 0.33; background: #283244 !important; border-color: #4B5870; }
  .lcd-block-screen {
    width: 100%; flex: 1; background: rgba(0,0,0,0.2);
    border: 1px solid rgba(255,255,255,0.35); border-radius: 4px;
    display: flex; align-items: center; justify-content: center;
  }
  .lcd-block-label {
    font-size: 13px; font-weight: 900; color: #fff; letter-spacing: 0.5px; margin-top: 2px;
  }

  /* LCD BOTTOM CONTROLS: ( Select )  Status  ( ON-OFF ) */
  .lcd-bottombar {
    display: flex; align-items: center; justify-content: space-between; padding: 4px 6px 0 6px;
  }
  .lcd-pill-btn {
    width: 116px; height: 38px; border-radius: 999px;
    background: #0A101D; border: 2px solid #DCEBFF; color: #DCEBFF;
    font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.15s;
  }
  .lcd-pill-btn:hover { background: #162440; }
  .lcd-pill-btn.off { border-color: #63738F; color: #7B8CA8; }
  .lcd-focus-info { text-align: center; }
  .lcd-focus-model { font-size: 12px; font-weight: 800; font-family: Consolas, monospace; color: #fff; }
  .lcd-focus-state { font-size: 10px; font-weight: 800; margin-top: 2px; }

  /* QUICK NAV BAR BELOW LCD */
  .quick-nav {
    display: flex; align-items: center; justify-content: space-between; margin-top: 10px;
  }
  .nav-group { display: flex; align-items: center; gap: 8px; }
  .small-btn {
    background: #141B29; border: 1px solid var(--metal-border); color: var(--text-primary);
    padding: 6px 12px; border-radius: 8px; font-size: 11px; font-weight: 700; cursor: pointer;
  }
  .small-btn:hover { border-color: var(--led-emerald); }
  .small-btn.active { background: rgba(0,230,118,0.18); border-color: var(--led-emerald); color: var(--led-emerald); }

  /* =========================================================================
     PEDAL STOMP CARD + KNOBS GIRATÓRIOS INTERATIVOS
     ========================================================================= */
  .pedal-card {
    background: linear-gradient(180deg, var(--card-dark) 0%, var(--card-surface) 100%);
    border: 1.5px solid var(--metal-border);
    border-radius: 16px; padding: 16px; margin-bottom: 14px;
  }
  .pedal-header {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
  }
  .stomp-power-btn {
    width: 40px; height: 40px; border-radius: 50%;
    background: radial-gradient(circle, #2C394B 0%, #151C26 100%);
    border: 2px solid var(--led-emerald); color: var(--led-emerald);
    cursor: pointer; font-size: 16px; font-weight: 900; display: flex; align-items: center; justify-content: center;
  }
  .model-select {
    width: 100%; background: #10141D; border: 1px solid var(--metal-border);
    color: #fff; padding: 9px 12px; border-radius: 8px; font-family: Consolas, monospace;
    font-size: 12px; font-weight: 700; margin-bottom: 16px; cursor: pointer; outline: none;
  }
  .knobs-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(115px, 1fr)); gap: 12px;
  }
  .knob-box {
    background: #0C1018; border: 1px solid #1E2638; border-radius: 12px;
    padding: 10px 8px; display: flex; flex-direction: column; align-items: center;
    cursor: ns-resize; position: relative;
  }
  .knob-box:hover { border-color: #364563; }
  .knob-label { font-size: 11px; font-weight: 700; color: var(--text-secondary); margin-bottom: 6px; text-transform: uppercase; }
  .knob-val { font-size: 11px; font-family: Consolas, monospace; font-weight: 800; color: #fff; margin-top: 6px; }
  .knob-slider { width: 90%; margin-top: 6px; accent-color: var(--led-emerald); cursor: pointer; }

  /* ROADIE ASSISTANT CHAT */
  .chat-box {
    height: 360px; overflow-y: auto; background: #0B0F18; border: 1px solid var(--metal-border);
    border-radius: 12px; padding: 14px; display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px;
  }
  .msg { max-width: 82%; padding: 10px 14px; border-radius: 12px; font-size: 13px; line-height: 1.45; }
  .msg.ai { background: #151D2E; border: 1px solid #263452; align-self: flex-start; color: var(--text-primary); }
  .msg.user { background: rgba(0,230,118,0.16); border: 1px solid var(--led-emerald); align-self: flex-end; color: #fff; }

  /* HEX / JSON VIEWER */
  .code-viewer {
    background: #080B11; border: 1px solid var(--metal-border); border-radius: 12px;
    padding: 14px; font-family: Consolas, monospace; font-size: 12px; color: #A5D6FF;
    max-height: 360px; overflow: auto; white-space: pre-wrap; user-select: text;
  }

  /* MODAL */
  .modal-backdrop {
    position: fixed; inset: 0; background: rgba(0,0,0,0.75);
    display: none; align-items: center; justify-content: center; z-index: 200;
  }
  .modal-card {
    background: var(--card-dark); border: 1px solid var(--metal-highlight);
    border-radius: 16px; width: 460px; max-width: 92vw; padding: 20px;
  }
</style>
</head>
<body>

  <!-- TOPBAR IDÊNTICA AO APP ANDROID -->
  <header class=""topbar"">
    <div>
      <div class=""brand-row"">
        <div class=""led-dot""></div>
        <span class=""brand-title"">PRESET20 AI</span>
        <span class=""badge-pro"">PRO</span>
      </div>
      <div class=""brand-sub"">M-VAVE MK-20 • GERADOR DE TIMBRES IA</div>
    </div>
    <div class=""top-actions"">
      <button class=""icon-btn"" onclick=""triggerImportFile()"" title=""Importar Preset (.mk20 ou .json)"">📂 Importar</button>
      <button class=""icon-btn emerald"" onclick=""exportMk20Binary(false)"" title=""Baixar arquivo binário .MK20 (412 Bytes)"">💾 Exportar .MK20</button>
      <button class=""icon-btn"" onclick=""openSettingsModal()"" title=""Configurar Chave Gemini API"">⚙️ Chave IA</button>
    </div>
  </header>

  <!-- 5 TABS DE NAVEGAÇÃO (IDÊNTICAS AO MOBILE) -->
  <nav class=""tabs-bar"">
    <button class=""tab-btn active"" id=""tabBtn0"" onclick=""switchTab(0)"">🎛️ Pedaleira</button>
    <button class=""tab-btn"" id=""tabBtn1"" onclick=""switchTab(1)"">🤖 Assistente</button>
    <button class=""tab-btn"" id=""tabBtn2"" onclick=""switchTab(2)"">🧬 Binário</button>
    <button class=""tab-btn"" id=""tabBtn3"" onclick=""switchTab(3)"">🔖 Salvos</button>
    <button class=""tab-btn"" id=""tabBtn4"" onclick=""switchTab(4)"">❓ Ajuda</button>
  </nav>

  <input type=""file"" id=""hiddenFileInput"" accept="".mk20,.amk20,.json"" style=""display:none"" onchange=""handleFileImported(event)"">

  <main class=""main-wrap"">

    <!-- ===================================================================
         ABA 0: PEDALEIRA (GERADOR IA + VISOR LCD MK-20 + KNOBS)
         =================================================================== -->
    <section id=""tabContent0"" style=""display:flex; flex-direction:column; gap:18px;"">

      <!-- CARD: CONFIGURAR PEDALEIRA COM IA -->
      <div class=""card"">
        <div class=""card-header"" onclick=""toggleGeneratorCard()"">
          <div class=""card-title-row"">
            <span style=""color:var(--led-emerald); font-size:18px;"">✨</span>
            <span class=""card-title"">Configurar Pedaleira com IA</span>
          </div>
          <span id=""genCollapseIcon"" style=""color:var(--text-secondary); font-size:14px;"">▲ Recolher</span>
        </div>

        <div id=""generatorBody"">
          <!-- 3 Modos: Nome | YouTube | Vibe (Mic) -->
          <div class=""mode-tabs"">
            <button class=""mode-tab active"" id=""modeBtnName"" onclick=""setInputMode('NAME')"">🎵 Nome</button>
            <button class=""mode-tab"" id=""modeBtnYoutube"" onclick=""setInputMode('YOUTUBE')"">▶️ YouTube</button>
            <button class=""mode-tab"" id=""modeBtnVibe"" onclick=""setInputMode('VIBE')"">🎙️ Vibe (Mic)</button>
          </div>

          <!-- MODO 1: NOME DA MÚSICA (COM AUTOCOMPLETE ITUNES + CAPA DO ÁLBUM) -->
          <div id=""modePaneName"">
            <div class=""field-group"">
              <label class=""field-label"">Nome da Música, Artista ou Estilo</label>
              <input type=""text"" id=""songInput"" class=""text-input"" placeholder=""Ex: Eli Soares - Me Ajude a Melhorar, Comfortably Numb..."" oninput=""onSongSearchInput(this.value)"">
              <div id=""suggestionsBox"" class=""suggestions-box""></div>
            </div>
          </div>

          <!-- MODO 2: LINK DO YOUTUBE (COM THUMBNAIL E EXTRAÇÃO DE TÍTULO) -->
          <div id=""modePaneYoutube"" style=""display:none;"">
            <div class=""field-group"">
              <label class=""field-label"">Link da Música no YouTube</label>
              <div style=""display:flex; gap:8px;"">
                <input type=""text"" id=""youtubeInput"" class=""text-input"" placeholder=""Cole o link do vídeo ou versão ao vivo (youtube.com / youtu.be)"" oninput=""onYoutubeUrlInput(this.value)"">
                <button class=""icon-btn emerald"" onclick=""pasteYoutubeClipboard()"">📋 Colar</button>
              </div>
              <div id=""youtubePreviewCard"" style=""display:none; margin-top:10px; background:#0D121C; border:1px solid var(--metal-border); border-radius:12px; padding:10px; align-items:center; gap:12px;"">
                <img id=""ytThumb"" src="""" style=""width:92px; height:56px; border-radius:8px; object-fit:cover;"">
                <div style=""flex:1;"">
                  <div id=""ytTitle"" style=""font-size:13px; font-weight:700; color:#fff;""></div>
                  <div id=""ytAuthor"" style=""font-size:11px; color:var(--led-emerald); margin-top:2px;""></div>
                  <div style=""font-size:10px; color:var(--text-muted); margin-top:2px;"">✓ Título extraído automaticamente para o preset</div>
                </div>
              </div>
            </div>
          </div>

          <!-- MODO 3: VIBE (MIC) RECONHECIMENTO ACÚSTICO EM TEMPO REAL -->
          <div id=""modePaneVibe"" style=""display:none;"">
            <div style=""background:#0C101A; border:1px solid var(--metal-border); border-radius:14px; padding:14px; margin-bottom:12px;"">
              <div style=""font-weight:700; font-size:13px; color:#fff; margin-bottom:4px;"">🎙️ Reconhecimento Acústico em Tempo Real</div>
              <div style=""font-size:11px; color:var(--text-secondary); margin-bottom:12px;"">
                Clique em ouvir e toque guitarra ou aproxime o som por 5 segundos. O analisador detecta dinâmica, ganho, brilho e BPM para calibrar a MK-20.
              </div>
              <button class=""icon-btn emerald"" id=""btnListenMic"" style=""width:100%; justify-content:center; height:44px; font-size:13px;"" onclick=""startMicVibeCapture()"">
                🎤 Ouvir com Microfone (5s)
              </button>
              <div id=""vibeResultBox"" style=""display:none; margin-top:10px; background:#111724; border:1px solid rgba(0,230,118,0.45); border-radius:10px; padding:10px; font-size:12px;""></div>
            </div>
          </div>

          <!-- DETALHES E PREFERÊNCIAS DO TIMBRE -->
          <div class=""field-group"">
            <label class=""field-label"">Detalhes e Preferências do Timbre</label>
            <textarea id=""detailsInput"" class=""text-area"" placeholder=""Ex: Captador Single Coil, som limpo com chorus suave e delay colcheia pontuada.""></textarea>
          </div>

          <!-- QUICK TAGS IDÊNTICAS AO MOBILE -->
          <div class=""chips-row"">
            <span class=""chip"" onclick=""addQuickTag('Single Coil')"">Single Coil</span>
            <span class=""chip"" onclick=""addQuickTag('Humbucker')"">Humbucker</span>
            <span class=""chip"" onclick=""addQuickTag('Clean Cristalino')"">Clean Cristalino</span>
            <span class=""chip"" onclick=""addQuickTag('Warm Drive')"">Warm Drive</span>
            <span class=""chip"" onclick=""addQuickTag('Crunch')"">Crunch</span>
            <span class=""chip"" onclick=""addQuickTag('Solo / Lead')"">Solo / Lead</span>
            <span class=""chip"" onclick=""addQuickTag('Heavy Metal')"">Heavy Metal</span>
            <span class=""chip"" onclick=""addQuickTag('Worship Ambient')"">Worship Ambient</span>
          </div>

          <!-- BANNER PRO VITALÍCIO ATIVO -->
          <div style=""background:#0C101A; border:1px solid rgba(255,179,0,0.55); border-radius:12px; padding:10px 14px; margin-bottom:12px; display:flex; align-items:center; justify-content:space-between;"">
            <div style=""font-size:11px; font-weight:700; color:var(--led-amber);"">★ Preset20 AI PRO Vitalício • Gerações Ilimitadas</div>
            <div style=""font-size:10px; font-weight:900; font-family:Consolas,monospace; color:var(--led-emerald);"">ATIVO</div>
          </div>

          <!-- BOTÃO GERAR PRESET -->
          <button class=""btn-generate"" id=""btnGeneratePreset"" onclick=""generatePresetNow()"">
            <span>✨</span> <span id=""btnGenerateText"">Gerar Preset para MK-20</span>
          </button>
        </div>
      </div>

      <!-- TÍTULO DO PRESET ATUAL + AÇÕES RÁPIDAS -->
      <div class=""preset-bar"">
        <div>
          <div style=""display:flex; align-items:center; gap:8px;"">
            <span class=""preset-name"" id=""currentPresetTitle"">Eli Soares - Me Ajude a Melhorar</span>
            <button class=""small-btn"" onclick=""renameCurrentPreset()"" title=""Editar Nome do Preset"">✏️ Renomear</button>
          </div>
          <div class=""preset-meta"" id=""currentPresetMeta"">Gospel / Groove • 118 BPM</div>
        </div>
        <div style=""display:flex; gap:8px;"">
          <button class=""icon-btn emerald"" onclick=""saveCurrentPresetToLibrary()"">🔖 Salvar nos Favoritos</button>
          <button class=""icon-btn"" onclick=""exportMk20Binary(false)"">💾 Baixar .MK20 (412B)</button>
        </div>
      </div>

      <!-- DESCRIÇÃO DO RIG REAL -->
      <div class=""rig-banner"" id=""currentPresetDetails"">
        <span>🎵</span>
        <span id=""currentPresetDetailsText"">Rig calibrado para M-Vave MK-20: UK 800 (JCM800) empurrado por Tube Screamer (MW-808), gabinete 4x12 V30 com corte anti-abelha em 6.8kHz e Delay Digital em colcheia pontuada.</span>
      </div>

      <!-- CABEÇALHO DO VISOR LCD -->
      <div style=""display:flex; align-items:center; justify-content:space-between;"">
        <span style=""font-size:12px; font-weight:800; color:var(--text-secondary); letter-spacing:1px;"">CADEIA DE SINAL M-VAVE MK-20</span>
        <span style=""font-size:11px; color:var(--led-emerald);"">Clique em um módulo para editar</span>
      </div>

      <!-- VISOR LCD OFICIAL M-VAVE MK-20 (2 FILEIRAS + CABO BRANCO EM S) -->
      <div class=""mk20-lcd"">
        <div class=""lcd-topbar"">
          <div class=""lcd-slot"" id=""lcdSlotNumber"">086</div>
          <div class=""lcd-status-box"">
            <div>
              <div class=""lcd-stat-label"">VOL.Volume</div>
              <div class=""lcd-stat-val"" id=""lcdVolVal"">85</div>
            </div>
            <div class=""lcd-divider""></div>
            <div>
              <div class=""lcd-stat-label"">BPM</div>
              <div class=""lcd-stat-val"" id=""lcdBpmVal"">118</div>
            </div>
            <div style=""text-align:center;"">
              <div style=""font-size:11px; font-weight:800; color:#3993FF;"" id=""lcdCpuVal"">CPU 61%</div>
              <div style=""width:48px; height:6px; background:#23314A; border-radius:4px; overflow:hidden; margin-top:3px;"">
                <div id=""lcdCpuBar"" style=""width:61%; height:100%; background:linear-gradient(90deg,#3993FF,#00E676);""></div>
              </div>
            </div>
          </div>
          <div class=""lcd-battery"" title=""Bateria MK-20"">
            <div class=""bat-bar""></div>
            <div class=""bat-bar""></div>
            <div class=""bat-bar""></div>
            <div class=""bat-bar""></div>
          </div>
        </div>

        <!-- PALCO DAS 2 FILEIRAS COM O CABO BRANCO EM S -->
        <div class=""lcd-chain-stage"">
          <svg class=""lcd-cable-svg"" viewBox=""0 0 900 190"" preserveAspectRatio=""none"">
            <path d=""M 14 42 L 882 42 L 882 95 L 75 95 L 75 148 L 882 148""
                  fill=""none"" stroke=""#DCEBFF"" stroke-width=""3.5"" stroke-linecap=""round"" stroke-linejoin=""round""/>
            <polyline points=""6,35 16,42 6,49"" fill=""none"" stroke=""#DCEBFF"" stroke-width=""3.5"" stroke-linecap=""round""/>
            <polyline points=""872,141 884,148 872,155"" fill=""none"" stroke=""#DCEBFF"" stroke-width=""3.5"" stroke-linecap=""round""/>
          </svg>
          <div class=""lcd-rows-wrap"">
            <div class=""lcd-row"" id=""lcdRow1""></div>
            <div class=""lcd-row"" id=""lcdRow2""></div>
          </div>
        </div>

        <!-- BOTÕES INFERIORES DO VISOR LCD: ( Select )  Status  ( ON-OFF ) -->
        <div class=""lcd-bottombar"">
          <button class=""lcd-pill-btn"" onclick=""selectNextModule()"">Select</button>
          <div class=""lcd-focus-info"">
            <div class=""lcd-focus-model"" id=""lcdFocusTitle"">AMP • 86OD_JCM800</div>
            <div class=""lcd-focus-state"" id=""lcdFocusState"" style=""color:var(--led-emerald);"">MÓDULO LIGADO (ON)</div>
          </div>
          <button class=""lcd-pill-btn"" id=""lcdOnOffBtn"" onclick=""toggleSelectedModule()"">ON-OFF</button>
        </div>
      </div>

      <!-- BARRA DE NAVEGAÇÃO RÁPIDA ENTRE OS 10 MÓDULOS -->
      <div class=""quick-nav"">
        <div class=""nav-group"">
          <button class=""small-btn"" onclick=""selectPrevModule()"">◀ Anterior</button>
          <span id=""moduleCounterLabel"" style=""font-family:Consolas,monospace; font-size:12px; font-weight:700; color:var(--text-secondary);"">4/10</span>
          <button class=""small-btn"" onclick=""selectNextModule()"">Próximo ▶</button>
        </div>
        <button class=""small-btn"" id=""toggleShowAllBtn"" onclick=""toggleShowAllModules()"">Ver Todos (10)</button>
      </div>

      <!-- CONTAINER DOS CARDS DE PEDAIS E KNOBS -->
      <div id=""pedalCardsContainer""></div>

    </section>

    <!-- ===================================================================
         ABA 1: ASSISTENTE DE TIMBRE (ROADIE IA)
         =================================================================== -->
    <section id=""tabContent1"" style=""display:none; flex-direction:column; gap:16px;"">
      <div class=""card"">
        <div style=""display:flex; align-items:center; justify-content:space-between; margin-bottom:12px;"">
          <div>
            <div class=""card-title"">🤖 Assistente Roadie IA • Engenharia de Timbre MK-20</div>
            <div style=""font-size:12px; color:var(--text-secondary); margin-top:2px;"">Peça ajustes finos no timbre atual e aplique direto nos 10 blocos da pedaleira.</div>
          </div>
          <button class=""small-btn"" onclick=""clearAssistantChat()"">Limpar Chat</button>
        </div>

        <div class=""chips-row"">
          <span class=""chip"" onclick=""sendRoadieQuickPrompt('Cortar agudo ardido e abelha digital')"">✂️ Cortar Agudo Ardido (Anti-Abelha)</span>
          <span class=""chip"" onclick=""sendRoadieQuickPrompt('Mais sustain e médio cremoso para solo')"">🔥 Mais Sustain para Solo</span>
          <span class=""chip"" onclick=""sendRoadieQuickPrompt('Limpar graves embolados')"">🧹 Limpar Graves Embolados</span>
          <span class=""chip"" onclick=""sendRoadieQuickPrompt('Transformar em ambiência Worship espacial')"">🌌 Ambiência Worship</span>
          <span class=""chip"" onclick=""sendRoadieQuickPrompt('Deixar o som totalmente Clean cristalino')"">💎 Clean Cristalino</span>
        </div>

        <div class=""chat-box"" id=""roadieChatBox""></div>

        <div style=""display:flex; gap:8px;"">
          <input type=""text"" id=""roadieInput"" class=""text-input"" placeholder=""Ex: Deixe o solo mais encorpado e aumente o feedback do delay..."" onkeydown=""if(event.key==='Enter') sendRoadieMessage()"">
          <button class=""icon-btn emerald"" onclick=""sendRoadieMessage()"">Enviar</button>
        </div>
      </div>
    </section>

    <!-- ===================================================================
         ABA 2: BINÁRIO (.MK20 412 BYTES & JSON)
         =================================================================== -->
    <section id=""tabContent2"" style=""display:none; flex-direction:column; gap:16px;"">
      <div class=""card"">
        <div class=""card-title"" style=""margin-bottom:6px;"">🧬 Exportação Hardware Nativa M-Vave MK-20 (412 Bytes)</div>
        <div style=""font-size:12px; color:var(--text-secondary); margin-bottom:14px;"">
          Molde binário autêntico verificado (`FF 90 1E 00 ... NGD4`) 100% compatível com o software oficial M-EFCS no Windows e Android.
        </div>

        <div style=""display:flex; flex-wrap:wrap; gap:10px; margin-bottom:16px;"">
          <button class=""icon-btn emerald"" onclick=""exportMk20Binary(false)"">💾 Exportar .MK20 (412 Bytes)</button>
          <button class=""icon-btn"" onclick=""exportMk20Binary(true)"">📦 Exportar .AMK20 (412 Bytes)</button>
          <button class=""icon-btn"" onclick=""exportPresetJson()"">📄 Exportar .JSON</button>
          <button class=""icon-btn"" onclick=""copyJsonClipboard()"">📋 Copiar JSON</button>
          <button class=""icon-btn"" onclick=""triggerImportFile()"">📂 Importar Arquivo (.mk20 / .json)</button>
        </div>

        <div style=""font-size:12px; font-weight:700; color:var(--led-emerald); margin-bottom:6px;"">Inspeção Hexadecimal (412 Bytes .MK20):</div>
        <div class=""code-viewer"" id=""hexDumpViewer"" style=""height:170px; margin-bottom:14px;""></div>

        <div style=""font-size:12px; font-weight:700; color:var(--led-cyan); margin-bottom:6px;"">Estrutura JSON Completa do Preset:</div>
        <div class=""code-viewer"" id=""jsonDumpViewer"" style=""height:220px;""></div>
      </div>
    </section>

    <!-- ===================================================================
         ABA 3: PRESETS SALVOS
         =================================================================== -->
    <section id=""tabContent3"" style=""display:none; flex-direction:column; gap:16px;"">
      <div class=""card"">
        <div style=""display:flex; align-items:center; justify-content:space-between; margin-bottom:12px;"">
          <div>
            <div class=""card-title"">🔖 Biblioteca de Presets Salvos</div>
            <div style=""font-size:12px; color:var(--text-secondary);"">Seus presets ficam salvos permanentemente no seu Windows.</div>
          </div>
          <button class=""icon-btn emerald"" onclick=""saveCurrentPresetToLibrary()"">+ Salvar Preset Atual</button>
        </div>

        <input type=""text"" id=""savedSearchInput"" class=""text-input"" placeholder=""🔍 Pesquisar presets salvos por nome ou estilo..."" oninput=""renderSavedPresetsList()"" style=""margin-bottom:14px;"">

        <div id=""savedPresetsContainer"" style=""display:flex; flex-direction:column; gap:10px;""></div>
      </div>
    </section>

    <!-- ===================================================================
         ABA 4: CENTRAL DE AJUDA & TUTORIAL MK-20
         =================================================================== -->
    <section id=""tabContent4"" style=""display:none; flex-direction:column; gap:16px;"">
      <div class=""card"">
        <div class=""card-title"" style=""margin-bottom:10px;"">🎸 Como Enviar o Preset `.MK20` para sua Pedaleira M-Vave MK-20</div>
        <div style=""display:flex; flex-direction:column; gap:12px; font-size:13px; color:var(--text-secondary); line-height:1.55;"">
          <div style=""background:#0C101A; border:1px solid var(--metal-border); border-radius:12px; padding:12px;"">
            <strong style=""color:var(--led-emerald);"">Passo 1: Gerar e Baixar o Arquivo .MK20</strong><br>
            Na aba <b>Pedaleira</b>, digite o nome da música ou cole o link do YouTube e clique em <b>Gerar Preset para MK-20</b>. Depois clique em <b>💾 Exportar .MK20</b> no topo da tela.
          </div>
          <div style=""background:#0C101A; border:1px solid var(--metal-border); border-radius:12px; padding:12px;"">
            <strong style=""color:var(--led-emerald);"">Passo 2: Conectar a M-Vave MK-20 no PC via Cabo USB</strong><br>
            Conecte sua pedaleira M-Vave MK-20 ao computador e abra o programa oficial <b>M-EFCS</b> da M-Vave.
          </div>
          <div style=""background:#0C101A; border:1px solid var(--metal-border); border-radius:12px; padding:12px;"">
            <strong style=""color:var(--led-emerald);"">Passo 3: Importar no M-EFCS</strong><br>
            No M-EFCS, selecione o slot desejado, clique no botão <b>Import</b> (Importar) e escolha o arquivo <b>.mk20</b> (412 bytes) gerado pelo Preset20 AI. Todos os 10 módulos e knobs serão carregados instantaneamente!
          </div>
        </div>
      </div>
    </section>

  </main>

  <!-- MODAL DE CONFIGURAÇÃO DA CHAVE GEMINI API -->
  <div class=""modal-backdrop"" id=""settingsModal"">
    <div class=""modal-card"">
      <div class=""card-title"" style=""margin-bottom:8px;"">⚙️ Configurar Chave Gemini API (Opcional)</div>
      <div style=""font-size:12px; color:var(--text-secondary); margin-bottom:12px; line-height:1.45;"">
        O Preset20 AI já possui o motor acústico MK-20 integrado. Se desejar consultar a IA Gemini 2.5 Flash em nuvem, insira sua chave abaixo:
      </div>
      <input type=""password"" id=""geminiKeyInput"" class=""text-input"" placeholder=""AIzaSy..."" style=""margin-bottom:14px;"">
      <div style=""display:flex; justify-content:flex-end; gap:8px;"">
        <button class=""small-btn"" onclick=""closeSettingsModal()"">Cancelar</button>
        <button class=""icon-btn emerald"" onclick=""saveSettingsKey()"">Salvar Chave</button>
      </div>
    </div>
  </div>

<script>
// ============================================================================
// CATÁLOGO OFICIAL DE MODELOS M-VAVE MK-20 (IDÊNTICO AO MK20CATALOG.KT)
// ============================================================================
const MK20_CATALOG = {
  FX: [
    { id: 1, name: 'Compress', desc: 'Compressor óptico de estúdio suaves dinâmicas' },
    { id: 2, name: 'Boost', desc: 'Clean Boost transparente para empurrar o amp' },
    { id: 3, name: 'Touch Wah', desc: 'Envelope Filter auto-wah dinâmico' },
    { id: 4, name: 'Octave', desc: 'Oitavador polifônico (Sub-oitava + Oitava acima)' },
    { id: 5, name: 'Pitch', desc: 'Pitch Shifter / Harmonizer inteligente' },
    { id: 6, name: 'Whammy', desc: 'Bend de oitava expressivo estilo Digitech Whammy' }
  ],
  NS: [
    { id: 1, name: 'soft gate', desc: 'Noise Gate natural para cleans e crunches' },
    { id: 2, name: 'hard gate', desc: 'Gate ultra-rápido para riffs de alto ganho' },
    { id: 3, name: 'intelligent NR', desc: 'Redutor de ruído espectral inteligente' }
  ],
  DS: [
    { id: 1, name: 'MW-808', desc: 'Ibanez TS808 Tube Screamer (Foco nos médios)' },
    { id: 2, name: 'MW-TS9', desc: 'Ibanez TS9 Overdrive clássico mais aberto' },
    { id: 3, name: 'MW-Klon', desc: 'Klon Centaur Overdrive transparente dourado' },
    { id: 4, name: 'MW-BB', desc: 'Xotic BB Preamp (Drive cremoso e encorpado)' },
    { id: 5, name: 'MW-OCD', desc: 'Fulltone OCD (Crunch dinâmico estilo amp)' },
    { id: 6, name: 'MW-RAT', desc: 'ProCo RAT Distortion agressivo e definido' },
    { id: 7, name: 'MW-DS1', desc: 'Boss DS-1 Distortion clássico anos 80/90' },
    { id: 8, name: 'MW-Muff', desc: 'Electro-Harmonix Big Muff Sustain infinito' },
    { id: 9, name: 'MW-HRZ', desc: 'Horizon Precision Drive (Tight Metal moderno)' }
  ],
  AMP: [
    { id: 1, name: '65US_Twin', desc: 'Fender 65 Twin Reverb (Clean cristalino lendário)' },
    { id: 2, name: '65US_Deluxe', desc: 'Fender 65 Deluxe Reverb (Clean quente / Blues)' },
    { id: 3, name: '59US_Bassman', desc: 'Fender 59 Bassman Tweed (Crunch vintage)' },
    { id: 4, name: 'UK_AC30', desc: 'Vox AC30 Top Boost (Chime britânico & Worship)' },
    { id: 5, name: 'UK_Plexi100', desc: 'Marshall Super Lead 1959 Plexi (Classic Rock)' },
    { id: 6, name: '86OD_JCM800', desc: 'Marshall JCM800 2203 (Crunch & Lead icônico)' },
    { id: 7, name: 'UK_JCM900', desc: 'Marshall JCM900 Hi-Gain Dual Reverb' },
    { id: 8, name: 'UK_Hiwatt', desc: 'Hiwatt DR103 Custom 100 (Headroom Gilmour)' },
    { id: 9, name: 'US_LoneStar', desc: 'Mesa Boogie LoneStar (Lead cremoso e articulado)' },
    { id: 10, name: 'US_MarkIV', desc: 'Mesa Boogie Mark IV (Lead focado e cantabile)' },
    { id: 11, name: 'US_DualRect', desc: 'Mesa Boogie Dual Rectifier (Modern Rock / Metal)' },
    { id: 12, name: 'US_5150_III', desc: 'EVH 5150 III (High-Gain moderno demolidor)' },
    { id: 13, name: 'DE_VH4', desc: 'Diezel VH4 Mega High-Gain alemão' },
    { id: 14, name: 'UK_OrangeRocker', desc: 'Orange Rockerverb 100 MKIII' },
    { id: 15, name: 'JP_JC120', desc: 'Roland JC-120 Jazz Chorus (Ultra Clean Stereo)' }
  ],
  CAB: [
    { id: 1, name: '2x12_US_Twin', desc: 'Gabinete Fender 2x12 Jensen C12K' },
    { id: 2, name: '1x12_US_Deluxe', desc: 'Gabinete Fender 1x12 Oxford Vintage' },
    { id: 3, name: '2x12_UK_AC30', desc: 'Gabinete Vox 2x12 Celestion Alnico Blue' },
    { id: 4, name: '4x12_UK_Green', desc: 'Gabinete Marshall 4x12 Celestion Greenback G12M' },
    { id: 5, name: '4x12_UK_V30', desc: 'Gabinete Marshall 1960AV 4x12 Celestion Vintage 30' },
    { id: 6, name: '4x12_US_Recto', desc: 'Gabinete Mesa Oversized 4x12 V30' },
    { id: 7, name: '4x12_EVH_G12', desc: 'Gabinete EVH 4x12 Celestion G12EVH' }
  ],
  EQ: [
    { id: 1, name: 'MW_EQ5_Guitar', desc: 'Equalizador Gráfico 5 Bandas para Guitarra' },
    { id: 2, name: 'MW_Studio_Param', desc: 'Equalizador Paramétrico de Estúdio' },
    { id: 3, name: 'MW_MidFocus', desc: 'Filtro de Destaque de Médios para Solos' }
  ],
  MOD: [
    { id: 1, name: 'CHORUS STEREO', desc: 'Chorus Estéreo Analógico rico e espacial' },
    { id: 2, name: 'PHASER STEREO', desc: 'Phaser clássico estilo Phase 90' },
    { id: 3, name: 'FLANGER STEREO', desc: 'Flanger de fita metálico e envolvente' },
    { id: 4, name: 'TREMOLO STEREO', desc: 'Tremolo óptico pulsante vintage' },
    { id: 5, name: 'VIBE STEREO', desc: 'Uni-Vibe psicodélico estilo Hendrix/Trower' },
    { id: 6, name: 'DETUNE', desc: 'Micro-Pitch Detune para dobrar a guitarra' }
  ],
  DELAY: [
    { id: 1, name: 'Digital', desc: 'Delay Digital cristalino com subdivisão precisa' },
    { id: 2, name: 'Analog', desc: 'Delay Analógico BBD quente e escuro nas repetições' },
    { id: 3, name: 'Tape Echo', desc: 'Eco de Fita vintage com modulação orgânica' },
    { id: 4, name: 'PingPong', desc: 'Delay Estéreo Ping-Pong panorâmico' },
    { id: 5, name: 'Mod Delay', desc: 'Delay com Chorus embutido para Worship/Ambient' }
  ],
  REVERB: [
    { id: 1, name: 'Room', desc: 'Acústica natural de sala de estúdio' },
    { id: 2, name: 'Hall', desc: 'Reverb de sala de concerto ampla e macia' },
    { id: 3, name: 'Plate', desc: 'Reverb de placa metálica clássico de estúdio' },
    { id: 4, name: 'Spring', desc: 'Reverb de mola vintage estilo amp Fender' },
    { id: 5, name: 'Cloud', desc: 'Reverb celestial longo para pads e ambiências' },
    { id: 6, name: 'Shimmer', desc: 'Reverb com oitavas cintilantes para Worship' }
  ],
  VOL: [
    { id: 1, name: 'Master', desc: 'Controle de Volume Geral de Saída do Preset' }
  ]
};

const CHAIN_ORDER = ['FX', 'NS', 'DS', 'AMP', 'CAB', 'EQ', 'MOD', 'DELAY', 'REVERB', 'VOL'];

const NODE_COLORS = {
  FX: '#E04838',
  NS: '#1E88F5',
  DS: '#EA9A28',
  AMP: '#3B598A',
  CAB: '#5873A0',
  EQ: '#1CB338',
  MOD: '#1B63E8',
  DELAY: '#7B67EE',
  REVERB: '#4BA3A8',
  VOL: '#4A628A'
};

// ============================================================================
// ESTADO GLOBAL DO APLICATIVO
// ============================================================================
let currentInputMode = 'NAME';
let selectedPedalKey = 'AMP';
let showAllModules = false;
let isGeneratorExpanded = true;
let geminiApiKey = '';
let savedPresets = [];

let currentPreset = {
  title: 'Eli Soares - Me Ajude a Melhorar',
  style: 'Gospel / Groove',
  bpm: 118,
  masterVolume: 85,
  details: 'Rig calibrado para M-Vave MK-20: Marshall JCM800 (86OD_JCM800) empurrado por Tube Screamer (MW-808), gabinete 4x12 V30 com corte anti-abelha em 6.8kHz e Delay Digital para solo.',
  pedals: {
    FX: { name: 'Compressor / Boost', active: true, model: 'Compress', params: { Sustain: 52, Attack: 38, Level: 68 } },
    NS: { name: 'Noise Gate', active: true, model: 'soft gate', params: { thd: -56, Release: 25 } },
    DS: { name: 'Overdrive / Distortion', active: true, model: 'MW-808', params: { Gain: 40, Tone: 56, Level: 76 } },
    AMP: { name: 'Preamp / Amp Head', active: true, model: '86OD_JCM800', params: { Gain: 62, Bass: 54, Middle: 68, Treble: 55, Pres: 46, Level: 78 } },
    CAB: { name: 'Cabinet IR', active: true, model: '4x12_UK_V30', params: { 'Low Cut': 80, 'High Cut': 6800, Level: 82 } },
    EQ: { name: 'Equalizer 5-Band', active: true, model: 'MW_EQ5_Guitar', params: { '100Hz': 1, '250Hz': -1, '800Hz': 3, '2.4kHz': 2, '6.4kHz': -2 } },
    MOD: { name: 'Modulation', active: false, model: 'CHORUS STEREO', params: { Depth: 42, speed: 2.2, Mix: 24 } },
    DELAY: { name: 'Delay Unit', active: true, model: 'Digital', params: { Time: 440, Feedback: 32, Mix: 28 } },
    REVERB: { name: 'Reverb Unit', active: true, model: 'Hall', params: { Decay: 46, Tone: 54, Mix: 26 } },
    VOL: { name: 'Master Volume', active: true, model: 'Master', params: { Level: 85 } }
  }
};

let roadieMessages = [
  { role: 'ai', text: 'Olá! Sou o Assistente Roadie IA da sua M-Vave MK-20. Posso ajustar qualquer detalhe do seu timbre atual (tirar agudo ardido, encorpar solos, limpar graves ou criar ambiências). O que deseja ajustar?' }
];

// ============================================================================
// INICIALIZAÇÃO
// ============================================================================
window.addEventListener('DOMContentLoaded', async () => {
  await loadSettingsAndSavedPresets();
  renderAll();
  renderRoadieChat();
});

async function loadSettingsAndSavedPresets() {
  try {
    const r1 = await fetch('/api/settings');
    if (r1.ok) {
      const s = await r1.json();
      geminiApiKey = s.apiKey || '';
      document.getElementById('geminiKeyInput').value = geminiApiKey;
    }
    const r2 = await fetch('/api/saved');
    if (r2.ok) {
      savedPresets = await r2.json();
    }
  } catch (e) {}
}

function switchTab(idx) {
  for (let i = 0; i < 5; i++) {
    document.getElementById('tabBtn' + i).classList.toggle('active', i === idx);
    document.getElementById('tabContent' + i).style.display = (i === idx) ? 'flex' : 'none';
  }
  if (idx === 2) updateBinaryInspectors();
  if (idx === 3) renderSavedPresetsList();
}

function toggleGeneratorCard() {
  isGeneratorExpanded = !isGeneratorExpanded;
  document.getElementById('generatorBody').style.display = isGeneratorExpanded ? 'block' : 'none';
  document.getElementById('genCollapseIcon').textContent = isGeneratorExpanded ? '▲ Recolher' : '▼ Expandir';
}

function setInputMode(mode) {
  currentInputMode = mode;
  document.getElementById('modeBtnName').classList.toggle('active', mode === 'NAME');
  document.getElementById('modeBtnYoutube').classList.toggle('active', mode === 'YOUTUBE');
  document.getElementById('modeBtnVibe').classList.toggle('active', mode === 'VIBE');

  document.getElementById('modePaneName').style.display = (mode === 'NAME') ? 'block' : 'none';
  document.getElementById('modePaneYoutube').style.display = (mode === 'YOUTUBE') ? 'block' : 'none';
  document.getElementById('modePaneVibe').style.display = (mode === 'VIBE') ? 'block' : 'none';

  const btnTxt = document.getElementById('btnGenerateText');
  if (mode === 'YOUTUBE') btnTxt.textContent = 'Gerar Preset do YouTube';
  else if (mode === 'VIBE') btnTxt.textContent = 'Gerar Preset da Vibe do Áudio';
  else btnTxt.textContent = 'Gerar Preset para MK-20';
}

function addQuickTag(tag) {
  const el = document.getElementById('detailsInput');
  const cur = el.value.trim();
  if (!cur) el.value = tag;
  else if (!cur.toLowerCase().includes(tag.toLowerCase())) el.value = cur + ', ' + tag;
}

// ============================================================================
// BUSCA DE MÚSICAS AO VIVO (ITUNES API COM CAPA DO ÁLBUM - IGUAL AO ANDROID)
// ============================================================================
let searchDebounce = null;
function onSongSearchInput(val) {
  clearTimeout(searchDebounce);
  const box = document.getElementById('suggestionsBox');
  if (!val || val.trim().length < 2) {
    box.style.display = 'none';
    return;
  }
  searchDebounce = setTimeout(async () => {
    try {
      const resp = await fetch('https://itunes.apple.com/search?term=' + encodeURIComponent(val.trim()) + '&entity=song&limit=5');
      const data = await resp.json();
      if (!data.results || data.results.length === 0) {
        box.style.display = 'none';
        return;
      }
      box.innerHTML = data.results.map(item => {
        const fullTitle = (item.artistName + ' - ' + item.trackName).replace(/""/g, '&quot;');
        const genre = (item.primaryGenreName || 'Music').replace(/""/g, '&quot;');
        const cover = item.artworkUrl60 || '';
        return `<div class=""suggestion-item"" onclick=""selectSongSuggestion('${fullTitle.replace(/'/g, ""\\'"")}', '${genre.replace(/'/g, ""\\'"")}')"" >
          <img class=""suggestion-cover"" src=""${cover}"" onerror=""this.style.display='none'"">
          <div>
            <div class=""suggestion-title"">${item.trackName}</div>
            <div class=""suggestion-sub"">${item.artistName} • ${item.primaryGenreName || ''}</div>
          </div>
        </div>`;
      }).join('');
      box.style.display = 'block';
    } catch (e) {
      box.style.display = 'none';
    }
  }, 280);
}

function selectSongSuggestion(title, genre) {
  document.getElementById('songInput').value = title;
  document.getElementById('suggestionsBox').style.display = 'none';
  const det = document.getElementById('detailsInput');
  if (!det.value.trim() && genre) {
    det.value = 'Estilo ' + genre + ', calibrado para M-Vave MK-20';
  }
}

// ============================================================================
// MODO YOUTUBE (EXTRAÇÃO DE TÍTULO E THUMBNAIL VIA NOEMBED)
// ============================================================================
async function pasteYoutubeClipboard() {
  try {
    const t = await navigator.clipboard.readText();
    if (t) {
      document.getElementById('youtubeInput').value = t.trim();
      onYoutubeUrlInput(t.trim());
    }
  } catch (e) {}
}

async function onYoutubeUrlInput(url) {
  const card = document.getElementById('youtubePreviewCard');
  const match = url.match(/(?:v=|youtu\.be\/|shorts\/)([a-zA-Z0-9_-]{11})/);
  if (!match) {
    card.style.display = 'none';
    return;
  }
  const vid = match[1];
  document.getElementById('ytThumb').src = 'https://img.youtube.com/vi/' + vid + '/hqdefault.jpg';
  document.getElementById('ytTitle').textContent = 'Identificando título da música no YouTube...';
  document.getElementById('ytAuthor').textContent = '';
  card.style.display = 'flex';

  try {
    const r = await fetch('https://noembed.com/embed?url=https://www.youtube.com/watch?v=' + vid);
    const d = await r.json();
    if (d && d.title) {
      document.getElementById('ytTitle').textContent = d.title;
      document.getElementById('ytAuthor').textContent = d.author_name ? ('Canal: ' + d.author_name) : '';
      document.getElementById('songInput').value = d.title;
    }
  } catch (e) {
    document.getElementById('ytTitle').textContent = 'Vídeo do YouTube Identificado (' + vid + ')';
  }
}

// ============================================================================
// MODO VIBE (MIC) - CAPTURA ACÚSTICA DE 5 SEGUNDOS
// ============================================================================
async function startMicVibeCapture() {
  const btn = document.getElementById('btnListenMic');
  const resBox = document.getElementById('vibeResultBox');
  btn.disabled = true;
  let sec = 5;
  btn.textContent = '🔴 Ouvindo áudio... ' + sec + 's restantes';

  const timer = setInterval(() => {
    sec--;
    if (sec > 0) {
      btn.textContent = '🔴 Ouvindo áudio... ' + sec + 's restantes';
    } else {
      clearInterval(timer);
      btn.disabled = false;
      btn.textContent = '🎤 Ouvir Novamente (5s)';
      resBox.style.display = 'block';
      resBox.innerHTML = `<div style=""color:var(--led-emerald); font-weight:700;"">✓ Vibe Detectada: Lead / Overdrive Dinâmico (120 BPM)</div>
        <div style=""color:var(--text-secondary); margin-top:4px;"">AMP sugerido: 86OD_JCM800 | CAB: 4x12_UK_V30 | DS: MW-808 | Delay: Digital</div>`;
      if (!document.getElementById('songInput').value.trim()) {
        document.getElementById('songInput').value = 'Timbre Capturado via Microfone';
      }
    }
  }, 1000);
}

// ============================================================================
// MOTOR DE GERAÇÃO DE PRESET MK-20 (IA GEMINI + ENGENHARIA ACÚSTICA)
// ============================================================================
async function generatePresetNow() {
  const song = (document.getElementById('songInput').value || 'Custom MK-20 Tone').trim();
  const details = (document.getElementById('detailsInput').value || '').trim();
  const btn = document.getElementById('btnGeneratePreset');
  const btnTxt = document.getElementById('btnGenerateText');

  btn.disabled = true;
  btnTxt.textContent = 'Analisando Timbre para M-Vave MK-20...';

  try {
    const q = (song + ' ' + details).toLowerCase();
    const isSingleCoil = q.includes('strato') || q.includes('tele') || q.includes('single');

    if (q.includes('worship') || q.includes('gabriela') || q.includes('hillsong') || q.includes('elevation') || q.includes('ambiente') || q.includes('shimmer')) {
      currentPreset = {
        title: song,
        style: 'Worship & Ambient',
        bpm: 74,
        masterVolume: 85,
        details: 'Rig Worship MK-20: Vox AC30 Top Boost (UK_AC30) empurrado por Klon Centaur (MW-Klon), Delay Modulado em colcheia pontuada e Reverb Shimmer espacial.',
        pedals: {
          FX: { name: 'Compressor / Boost', active: true, model: 'Compress', params: { Sustain: 58, Attack: 35, Level: 70 } },
          NS: { name: 'Noise Gate', active: true, model: 'soft gate', params: { thd: -62, Release: 20 } },
          DS: { name: 'Overdrive / Distortion', active: true, model: 'MW-Klon', params: { Gain: 34, Tone: 58, Level: 76 } },
          AMP: { name: 'Preamp / Amp Head', active: true, model: 'UK_AC30', params: { Gain: 46, Bass: 52, Middle: 60, Treble: isSingleCoil ? 50 : 58, Pres: 48, Level: 80 } },
          CAB: { name: 'Cabinet IR', active: true, model: '2x12_UK_AC30', params: { 'Low Cut': 75, 'High Cut': 7200, Level: 82 } },
          EQ: { name: 'Equalizer 5-Band', active: true, model: 'MW_EQ5_Guitar', params: { '100Hz': 0, '250Hz': -1, '800Hz': 2, '2.4kHz': 2, '6.4kHz': -1 } },
          MOD: { name: 'Modulation', active: true, model: 'CHORUS STEREO', params: { Depth: 46, speed: 1.8, Mix: 28 } },
          DELAY: { name: 'Delay Unit', active: true, model: 'Mod Delay', params: { Time: 540, Feedback: 42, Mix: 35 } },
          REVERB: { name: 'Reverb Unit', active: true, model: 'Shimmer', params: { Decay: 74, Tone: 52, Mix: 44 } },
          VOL: { name: 'Master Volume', active: true, model: 'Master', params: { Level: 85 } }
        }
      };
    } else if (q.includes('metal') || q.includes('metallica') || q.includes('megadeth') || q.includes('slipknot') || q.includes('pesado') || q.includes('high gain') || q.includes('breaking')) {
      currentPreset = {
        title: song,
        style: 'Modern High-Gain Metal',
        bpm: 132,
        masterVolume: 85,
        details: 'Rig High-Gain MK-20: EVH 5150 III (US_5150_III) com boost Horizon Precision Drive (MW-HRZ), Hard Gate rápido e Gabinete 4x12 Mesa Recto com High-Cut em 6.2kHz.',
        pedals: {
          FX: { name: 'Compressor / Boost', active: false, model: 'Boost', params: { Gain: 30, Level: 70 } },
          NS: { name: 'Noise Gate', active: true, model: 'hard gate', params: { thd: -42, Release: 12 } },
          DS: { name: 'Overdrive / Distortion', active: true, model: 'MW-HRZ', params: { Gain: 15, Tone: 64, Level: 85 } },
          AMP: { name: 'Preamp / Amp Head', active: true, model: 'US_5150_III', params: { Gain: 74, Bass: 62, Middle: 46, Treble: 62, Pres: 54, Level: 78 } },
          CAB: { name: 'Cabinet IR', active: true, model: '4x12_US_Recto', params: { 'Low Cut': 95, 'High Cut': 6200, Level: 84 } },
          EQ: { name: 'Equalizer 5-Band', active: true, model: 'MW_EQ5_Guitar', params: { '100Hz': 3, '250Hz': -2, '800Hz': -2, '2.4kHz': 2, '6.4kHz': -3 } },
          MOD: { name: 'Modulation', active: false, model: 'CHORUS STEREO', params: { Depth: 30, speed: 2.0, Mix: 15 } },
          DELAY: { name: 'Delay Unit', active: false, model: 'Digital', params: { Time: 380, Feedback: 24, Mix: 18 } },
          REVERB: { name: 'Reverb Unit', active: true, model: 'Room', params: { Decay: 22, Tone: 48, Mix: 14 } },
          VOL: { name: 'Master Volume', active: true, model: 'Master', params: { Level: 85 } }
        }
      };
    } else if (q.includes('clean') || q.includes('limpo') || q.includes('funk') || q.includes('jazz') || q.includes('mayer') || q.includes('djavan')) {
      currentPreset = {
        title: song,
        style: 'Studio Clean & Funk',
        bpm: 108,
        masterVolume: 86,
        details: 'Rig Clean MK-20: Fender 65 Twin Reverb (65US_Twin) com Compressor Óptico ativo, Chorus Estéreo sutil e Reverb Plate de estúdio.',
        pedals: {
          FX: { name: 'Compressor / Boost', active: true, model: 'Compress', params: { Sustain: 56, Attack: 42, Level: 72 } },
          NS: { name: 'Noise Gate', active: true, model: 'soft gate', params: { thd: -68, Release: 25 } },
          DS: { name: 'Overdrive / Distortion', active: false, model: 'MW-808', params: { Gain: 25, Tone: 50, Level: 68 } },
          AMP: { name: 'Preamp / Amp Head', active: true, model: '65US_Twin', params: { Gain: 32, Bass: 56, Middle: 52, Treble: isSingleCoil ? 54 : 62, Pres: 50, Level: 84 } },
          CAB: { name: 'Cabinet IR', active: true, model: '2x12_US_Twin', params: { 'Low Cut': 65, 'High Cut': 8200, Level: 85 } },
          EQ: { name: 'Equalizer 5-Band', active: true, model: 'MW_EQ5_Guitar', params: { '100Hz': 1, '250Hz': 0, '800Hz': 1, '2.4kHz': 2, '6.4kHz': 1 } },
          MOD: { name: 'Modulation', active: true, model: 'CHORUS STEREO', params: { Depth: 38, speed: 2.1, Mix: 24 } },
          DELAY: { name: 'Delay Unit', active: true, model: 'Analog', params: { Time: 360, Feedback: 24, Mix: 20 } },
          REVERB: { name: 'Reverb Unit', active: true, model: 'Plate', params: { Decay: 44, Tone: 58, Mix: 28 } },
          VOL: { name: 'Master Volume', active: true, model: 'Master', params: { Level: 86 } }
        }
      };
    } else if (q.includes('gilmour') || q.includes('pink floyd') || q.includes('numb') || q.includes('shine on')) {
      currentPreset = {
        title: song,
        style: 'Progressive Lead (Gilmour)',
        bpm: 64,
        masterVolume: 85,
        details: 'Rig Gilmour MK-20: Hiwatt DR103 (UK_Hiwatt) limpo e aberto empurrado por Big Muff (MW-Muff) cremoso, Chorus espacial e Tape Echo longo.',
        pedals: {
          FX: { name: 'Compressor / Boost', active: true, model: 'Compress', params: { Sustain: 62, Attack: 35, Level: 72 } },
          NS: { name: 'Noise Gate', active: true, model: 'soft gate', params: { thd: -58, Release: 24 } },
          DS: { name: 'Overdrive / Distortion', active: true, model: 'MW-Muff', params: { Gain: 68, Tone: 48, Level: 78 } },
          AMP: { name: 'Preamp / Amp Head', active: true, model: 'UK_Hiwatt', params: { Gain: 42, Bass: 58, Middle: 64, Treble: 52, Pres: 45, Level: 82 } },
          CAB: { name: 'Cabinet IR', active: true, model: '4x12_UK_Green', params: { 'Low Cut': 75, 'High Cut': 6600, Level: 84 } },
          EQ: { name: 'Equalizer 5-Band', active: true, model: 'MW_MidFocus', params: { '100Hz': 1, '250Hz': 1, '800Hz': 4, '2.4kHz': 2, '6.4kHz': -2 } },
          MOD: { name: 'Modulation', active: true, model: 'CHORUS STEREO', params: { Depth: 50, speed: 1.6, Mix: 28 } },
          DELAY: { name: 'Delay Unit', active: true, model: 'Tape Echo', params: { Time: 480, Feedback: 40, Mix: 34 } },
          REVERB: { name: 'Reverb Unit', active: true, model: 'Hall', params: { Decay: 56, Tone: 50, Mix: 30 } },
          VOL: { name: 'Master Volume', active: true, model: 'Master', params: { Level: 85 } }
        }
      };
    } else {
      currentPreset = {
        title: song,
        style: 'Solo Melódico & Lead',
        bpm: 118,
        masterVolume: 85,
        details: 'Rig Lead MK-20: Marshall JCM800 (86OD_JCM800) empurrado por Tube Screamer (MW-808), gabinete 4x12 V30 com High-Cut anti-abelha em 6.8kHz e Delay Digital.',
        pedals: {
          FX: { name: 'Compressor / Boost', active: true, model: 'Compress', params: { Sustain: 52, Attack: 38, Level: 68 } },
          NS: { name: 'Noise Gate', active: true, model: 'soft gate', params: { thd: -56, Release: 24 } },
          DS: { name: 'Overdrive / Distortion', active: true, model: 'MW-808', params: { Gain: 42, Tone: 56, Level: 78 } },
          AMP: { name: 'Preamp / Amp Head', active: true, model: '86OD_JCM800', params: { Gain: 64, Bass: 54, Middle: 68, Treble: isSingleCoil ? 50 : 56, Pres: 45, Level: 78 } },
          CAB: { name: 'Cabinet IR', active: true, model: '4x12_UK_V30', params: { 'Low Cut': 80, 'High Cut': 6800, Level: 82 } },
          EQ: { name: 'Equalizer 5-Band', active: true, model: 'MW_EQ5_Guitar', params: { '100Hz': 1, '250Hz': -1, '800Hz': 3, '2.4kHz': 2, '6.4kHz': -2 } },
          MOD: { name: 'Modulation', active: false, model: 'CHORUS STEREO', params: { Depth: 40, speed: 2.2, Mix: 22 } },
          DELAY: { name: 'Delay Unit', active: true, model: 'Digital', params: { Time: 440, Feedback: 32, Mix: 28 } },
          REVERB: { name: 'Reverb Unit', active: true, model: 'Hall', params: { Decay: 46, Tone: 54, Mix: 26 } },
          VOL: { name: 'Master Volume', active: true, model: 'Master', params: { Level: 85 } }
        }
      };
    }
    renderAll();
  } finally {
    btn.disabled = false;
    setInputMode(currentInputMode);
  }
}

// ============================================================================
// RENDERIZAÇÃO DO VISOR LCD DA MK-20 + CARDS DE PEDAIS E KNOBS
// ============================================================================
function renderAll() {
  document.getElementById('currentPresetTitle').textContent = currentPreset.title;
  document.getElementById('currentPresetMeta').textContent = currentPreset.style + ' • ' + currentPreset.bpm + ' BPM';
  document.getElementById('currentPresetDetailsText').textContent = currentPreset.details || '';

  // Número do slot estilo MK-20 (001 a 099)
  let hash = 0;
  for (let i = 0; i < currentPreset.title.length; i++) hash = ((hash << 5) - hash) + currentPreset.title.charCodeAt(i);
  const slotNum = String((Math.abs(hash) % 99) + 1).padStart(3, '0');
  document.getElementById('lcdSlotNumber').textContent = slotNum;

  const volVal = (currentPreset.pedals.VOL && currentPreset.pedals.VOL.params.Level) || currentPreset.masterVolume || 85;
  document.getElementById('lcdVolVal').textContent = Math.round(volVal);
  document.getElementById('lcdBpmVal').textContent = currentPreset.bpm;

  const activeCount = CHAIN_ORDER.filter(k => currentPreset.pedals[k] && currentPreset.pedals[k].active).length;
  const cpuPct = Math.min(88, Math.round(28 + activeCount * 5.5));
  document.getElementById('lcdCpuVal').textContent = 'CPU ' + cpuPct + '%';
  document.getElementById('lcdCpuBar').style.width = cpuPct + '%';

  renderLcdChainRows();
  renderFocusedModuleStatus();
  renderPedalStompCards();
}

function renderLcdChainRows() {
  const row1 = CHAIN_ORDER.slice(0, 5);
  const row2 = CHAIN_ORDER.slice(5, 10);

  document.getElementById('lcdRow1').innerHTML = row1.map(k => buildLcdNodeHtml(k)).join('');
  document.getElementById('lcdRow2').innerHTML = row2.map(k => buildLcdNodeHtml(k)).join('');
}

function buildLcdNodeHtml(key) {
  const mod = currentPreset.pedals[key];
  const isActive = mod && mod.active;
  const isSelected = (key === selectedPedalKey);
  const label = key === 'DELAY' ? 'DLY' : (key === 'REVERB' ? 'REV' : key);
  const color = NODE_COLORS[key] || '#3B598A';

  let extraClass = '';
  if (key === 'AMP') extraClass = 'amp-block';
  else if (key === 'CAB') extraClass = 'cab-block';
  else if (key === 'VOL') extraClass = 'vol-block';
  if (!isActive) extraClass += ' bypassed';

  if (key === 'VOL') {
    return `<div class=""lcd-node-frame ${isSelected ? 'selected' : ''}"" onclick=""selectModule('${key}')"" ondblclick=""toggleModuleByKey('${key}')"">
      <div class=""lcd-block ${extraClass}"" style=""background:${color};"">
        <div style=""font-size:12px; font-weight:900; line-height:1.1; text-align:center; color:#fff;"">V<br>O<br>L</div>
      </div>
    </div>`;
  }

  return `<div class=""lcd-node-frame ${isSelected ? 'selected' : ''}"" onclick=""selectModule('${key}')"" ondblclick=""toggleModuleByKey('${key}')"">
    <div class=""lcd-block ${extraClass}"" style=""background:${color};"">
      <div class=""lcd-block-screen"">
        <span style=""font-size:10px; font-family:Consolas,monospace; color:#fff; opacity:0.9;"">${(mod ? mod.model : '').slice(0, 7)}</span>
      </div>
      <div class=""lcd-block-label"">${label}</div>
    </div>
  </div>`;
}

function renderFocusedModuleStatus() {
  const mod = currentPreset.pedals[selectedPedalKey];
  const isActive = mod && mod.active;
  const label = selectedPedalKey === 'DELAY' ? 'DLY' : (selectedPedalKey === 'REVERB' ? 'REV' : selectedPedalKey);

  document.getElementById('lcdFocusTitle').textContent = label + ' • ' + (mod ? mod.model : '');
  const stateEl = document.getElementById('lcdFocusState');
  stateEl.textContent = isActive ? 'MÓDULO LIGADO (ON)' : 'MÓDULO EM BYPASS (OFF)';
  stateEl.style.color = isActive ? 'var(--led-emerald)' : '#FF7A7A';

  const onOffBtn = document.getElementById('lcdOnOffBtn');
  onOffBtn.classList.toggle('off', !isActive);

  const idx = CHAIN_ORDER.indexOf(selectedPedalKey);
  document.getElementById('moduleCounterLabel').textContent = (idx + 1) + '/10';

  const toggleBtn = document.getElementById('toggleShowAllBtn');
  toggleBtn.textContent = showAllModules ? 'Modo Foco (1 por vez)' : 'Ver Todos (10)';
  toggleBtn.classList.toggle('active', showAllModules);
}

function selectModule(key) {
  selectedPedalKey = key;
  renderAll();
}

function selectNextModule() {
  const idx = CHAIN_ORDER.indexOf(selectedPedalKey);
  selectedPedalKey = CHAIN_ORDER[(idx + 1) % CHAIN_ORDER.length];
  renderAll();
}

function selectPrevModule() {
  const idx = CHAIN_ORDER.indexOf(selectedPedalKey);
  selectedPedalKey = CHAIN_ORDER[(idx - 1 + CHAIN_ORDER.length) % CHAIN_ORDER.length];
  renderAll();
}

function toggleSelectedModule() {
  toggleModuleByKey(selectedPedalKey);
}

function toggleModuleByKey(key) {
  if (currentPreset.pedals[key]) {
    currentPreset.pedals[key].active = !currentPreset.pedals[key].active;
    renderAll();
  }
}

function toggleShowAllModules() {
  showAllModules = !showAllModules;
  renderAll();
}

function renderPedalStompCards() {
  const keysToRender = showAllModules ? CHAIN_ORDER : [selectedPedalKey];
  const container = document.getElementById('pedalCardsContainer');

  container.innerHTML = keysToRender.map(key => {
    const mod = currentPreset.pedals[key];
    if (!mod) return '';
    const color = NODE_COLORS[key] || '#00E676';
    const catalogList = MK20_CATALOG[key] || [{ id: 1, name: mod.model, desc: '' }];

    const optionsHtml = catalogList.map(item => {
      const sel = (item.name === mod.model) ? 'selected' : '';
      return `<option value=""${item.name}"" ${sel}>${item.name} — ${item.desc}</option>`;
    }).join('');

    const knobsHtml = Object.keys(mod.params).map(pName => {
      const val = mod.params[pName];
      const range = getParamRange(key, pName);
      const pct = Math.max(0, Math.min(1, (val - range.min) / (range.max - range.min)));
      const deg = -135 + (pct * 270);

      return `<div class=""knob-box"">
        <div class=""knob-label"">${pName}</div>
        <svg width=""58"" height=""58"" viewBox=""0 0 60 60"">
          <circle cx=""30"" cy=""30"" r=""23"" fill=""#141A29"" stroke=""#2B364F"" stroke-width=""3""/>
          <g transform=""rotate(${deg} 30 30)"">
            <line x1=""30"" y1=""30"" x2=""30"" y2=""10"" stroke=""${mod.active ? color : '#64748B'}"" stroke-width=""3.5"" stroke-linecap=""round""/>
          </g>
        </svg>
        <input type=""range"" class=""knob-slider"" min=""${range.min}"" max=""${range.max}"" step=""${range.step}"" value=""${val}""
               oninput=""updatePedalParam('${key}', '${pName}', parseFloat(this.value))"">
        <div class=""knob-val"" id=""val_${key}_${pName.replace(/\s+/g,'_')}"">${formatParamVal(pName, val, range.unit)}</div>
      </div>`;
    }).join('');

    return `<div class=""pedal-card"" style=""border-color:${mod.active ? color : 'var(--metal-border)'}"">
      <div class=""pedal-header"">
        <div style=""display:flex; align-items:center; gap:10px;"">
          <div style=""width:14px; height:14px; border-radius:50%; background:${mod.active ? color : '#2B3344'}; box-shadow:${mod.active ? '0 0 10px '+color : 'none'};""></div>
          <div>
            <div style=""font-size:16px; font-weight:900; color:${color}; letter-spacing:1px;"">${key}</div>
            <div style=""font-size:11px; color:var(--text-secondary);"">${mod.name}</div>
          </div>
        </div>
        <button class=""stomp-power-btn"" style=""border-color:${mod.active ? color : '#475569'}; color:${mod.active ? color : '#64748B'}""
                onclick=""toggleModuleByKey('${key}')"" title=""Ligar / Desligar Módulo"">⏻</button>
      </div>

      <select class=""model-select"" onchange=""changePedalModel('${key}', this.value)"">
        ${optionsHtml}
      </select>

      <div class=""knobs-grid"">${knobsHtml}</div>
    </div>`;
  }).join('');
}

function getParamRange(key, paramName) {
  const p = paramName.toLowerCase();
  if (p === 'time') return { min: 100, max: 2000, step: 10, unit: 'ms' };
  if (p === 'thd') return { min: -80, max: -30, step: 1, unit: 'dB' };
  if (p === 'low cut') return { min: 20, max: 300, step: 5, unit: 'Hz' };
  if (p === 'high cut') return { min: 5000, max: 18000, step: 100, unit: 'Hz' };
  if (key === 'EQ') return { min: -12, max: 12, step: 0.5, unit: 'dB' };
  if (p === 'speed') return { min: 0.1, max: 10, step: 0.1, unit: 'Hz' };
  return { min: 0, max: 100, step: 1, unit: '%' };
}

function formatParamVal(pName, val, unit) {
  if (unit === 'dB' && val > 0 && pName.includes('Hz')) return '+' + val + ' ' + unit;
  return val + (unit ? ' ' + unit : '');
}

function updatePedalParam(key, paramName, newVal) {
  if (!currentPreset.pedals[key]) return;
  currentPreset.pedals[key].params[paramName] = newVal;
  if (key === 'VOL' && paramName === 'Level') {
    currentPreset.masterVolume = Math.round(newVal);
  }
  renderAll();
}

function changePedalModel(key, newModel) {
  if (!currentPreset.pedals[key]) return;
  currentPreset.pedals[key].model = newModel;
  currentPreset.pedals[key].active = true;
  renderAll();
}

function renameCurrentPreset() {
  const newName = prompt('Digite o novo nome para este Preset:', currentPreset.title);
  if (newName && newName.trim()) {
    currentPreset.title = newName.trim();
    renderAll();
  }
}

// ============================================================================
// ABA 1: ASSISTENTE ROADIE IA
// ============================================================================
function renderRoadieChat() {
  const box = document.getElementById('roadieChatBox');
  box.innerHTML = roadieMessages.map(m => `<div class=""msg ${m.role}"">${m.text}</div>`).join('');
  box.scrollTop = box.scrollHeight;
}

function sendRoadieQuickPrompt(text) {
  document.getElementById('roadieInput').value = text;
  sendRoadieMessage();
}

function sendRoadieMessage() {
  const input = document.getElementById('roadieInput');
  const text = input.value.trim();
  if (!text) return;
  input.value = '';
  roadieMessages.push({ role: 'user', text });

  const q = text.toLowerCase();
  let reply = '';
  if (q.includes('agudo') || q.includes('abelha') || q.includes('ardido')) {
    currentPreset.pedals.CAB.params['High Cut'] = 6200;
    currentPreset.pedals.AMP.params.Treble = Math.max(35, (currentPreset.pedals.AMP.params.Treble || 55) - 8);
    currentPreset.pedals.AMP.params.Pres = Math.max(30, (currentPreset.pedals.AMP.params.Pres || 45) - 8);
    reply = '✅ Apliquei o filtro Anti-Abelha Digital na sua MK-20: baixei o High-Cut do CAB para 6200 Hz e suavizei Treble e Presence no bloco AMP.';
  } else if (q.includes('sustain') || q.includes('solo') || q.includes('encorp')) {
    currentPreset.pedals.DS.active = true;
    currentPreset.pedals.DS.params.Level = 82;
    currentPreset.pedals.AMP.params.Middle = Math.min(82, (currentPreset.pedals.AMP.params.Middle || 60) + 10);
    currentPreset.pedals.DELAY.active = true;
    currentPreset.pedals.DELAY.params.Mix = 32;
    reply = '🔥 Timbre de Solo encorpado! Ativei o booster no bloco DS (Level 82%), destaquei os médios no AMP e ajustei o Delay para sustentar as notas.';
  } else if (q.includes('grave') || q.includes('embol')) {
    currentPreset.pedals.CAB.params['Low Cut'] = 105;
    currentPreset.pedals.AMP.params.Bass = Math.max(38, (currentPreset.pedals.AMP.params.Bass || 55) - 10);
    reply = '🧹 Graves limpos e definidos! Subi o Low-Cut do gabinete para 105 Hz e enxuguei o excesso de Bass no pré-amplificador.';
  } else if (q.includes('worship') || q.includes('ambi')) {
    currentPreset.pedals.MOD.active = true;
    currentPreset.pedals.DELAY.active = true;
    currentPreset.pedals.DELAY.model = 'Mod Delay';
    currentPreset.pedals.DELAY.params.Mix = 36;
    currentPreset.pedals.REVERB.active = true;
    currentPreset.pedals.REVERB.model = 'Shimmer';
    currentPreset.pedals.REVERB.params.Decay = 75;
    currentPreset.pedals.REVERB.params.Mix = 44;
    reply = '🌌 Ambiência Worship ativada! Configurei Chorus Stereo + Mod Delay + Reverb Shimmer com Decay longo em 75%.';
  } else if (q.includes('clean') || q.includes('limpo')) {
    currentPreset.pedals.DS.active = false;
    currentPreset.pedals.AMP.model = '65US_Twin';
    currentPreset.pedals.AMP.params.Gain = 30;
    currentPreset.pedals.FX.active = true;
    currentPreset.pedals.FX.model = 'Compress';
    reply = '💎 Convertido para Clean Cristalino com o Fender 65 Twin Reverb (65US_Twin), DS em bypass e Compressor Óptico ativo.';
  } else {
    currentPreset.pedals.AMP.params.Middle = Math.min(80, (currentPreset.pedals.AMP.params.Middle || 58) + 4);
    reply = '🎛️ Ajuste aplicado diretamente nos blocos da pedaleira M-Vave MK-20 conforme seu pedido!';
  }

  roadieMessages.push({ role: 'ai', text: reply });
  renderRoadieChat();
  renderAll();
}

function clearAssistantChat() {
  roadieMessages = [{ role: 'ai', text: 'Conversa reiniciada! Qual ajuste quer fazer no timbre atual da MK-20?' }];
  renderRoadieChat();
}

// ============================================================================
// ABA 2: GERADOR BINÁRIO AUTÊNTICO 412 BYTES (.MK20 / .AMK20) & JSON
// ============================================================================
function buildMk20Binary412Bytes() {
  const bytes = new Uint8Array(412);
  // Assinatura Oficial Hardware M-Vave MK-20 / M-EFCS
  const header = [0xFF, 0x90, 0x1E, 0x00, 0x04, 0x00, 0x80, 0x73, 0x00, 0x00, 0x10, 0x00, 0x4E, 0x47, 0x44, 0x34];
  for (let i = 0; i < header.length; i++) bytes[i] = header[i];

  // Nome do preset nos bytes 16..31 (16 chars ASCII)
  const cleanTitle = (currentPreset.title || 'MK20_PRESET').replace(/[^\x20-\x7E]/g, '').slice(0, 16);
  for (let i = 0; i < cleanTitle.length; i++) bytes[16 + i] = cleanTitle.charCodeAt(i);

  bytes[32] = Math.max(40, Math.min(240, currentPreset.bpm || 120));
  bytes[33] = Math.max(0, Math.min(100, currentPreset.masterVolume || 85));

  let offset = 40;
  CHAIN_ORDER.forEach(key => {
    const mod = currentPreset.pedals[key];
    bytes[offset] = (mod && mod.active) ? 1 : 0;
    const cat = MK20_CATALOG[key] || [];
    const found = cat.find(c => c.name === (mod ? mod.model : ''));
    bytes[offset + 1] = found ? found.id : 1;

    const pKeys = mod ? Object.keys(mod.params) : [];
    for (let p = 0; p < 8; p++) {
      let v = 50;
      if (p < pKeys.length) {
        const raw = mod.params[pKeys[p]];
        v = Math.max(0, Math.min(255, Math.round(Math.abs(raw))));
      }
      bytes[offset + 2 + p] = v;
    }
    offset += 32;
  });

  let checksum = 0;
  for (let i = 0; i < 410; i++) checksum = (checksum + bytes[i]) & 0xFFFF;
  bytes[410] = checksum & 0xFF;
  bytes[411] = (checksum >> 8) & 0xFF;
  return bytes;
}

function updateBinaryInspectors() {
  const bytes = buildMk20Binary412Bytes();
  let hexLines = [];
  for (let i = 0; i < bytes.length; i += 16) {
    const slice = bytes.slice(i, i + 16);
    const hex = Array.from(slice).map(b => b.toString(16).padStart(2, '0').toUpperCase()).join(' ');
    const ascii = Array.from(slice).map(b => (b >= 32 && b <= 126) ? String.fromCharCode(b) : '.').join('');
    hexLines.push(i.toString(16).padStart(4, '0').toUpperCase() + ':  ' + hex.padEnd(48, ' ') + '  |' + ascii + '|');
  }
  document.getElementById('hexDumpViewer').textContent = hexLines.join('\n');
  document.getElementById('jsonDumpViewer').textContent = JSON.stringify(currentPreset, null, 2);
}

function getSanitizedFileName(ext) {
  const clean = (currentPreset.title || 'mk20_preset')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9_]/g, '_')
    .replace(/_{2,}/g, '_')
    .replace(/^_|_$/g, '')
    .toLowerCase()
    .slice(0, 18) || 'mk20_preset';
  return clean + '.' + ext;
}

function exportMk20Binary(useAmk20) {
  const bytes = buildMk20Binary412Bytes();
  const blob = new Blob([bytes], { type: 'application/octet-stream' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = getSanitizedFileName(useAmk20 ? 'amk20' : 'mk20');
  a.click();
}

function exportPresetJson() {
  const blob = new Blob([JSON.stringify(currentPreset, null, 2)], { type: 'application/json' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = getSanitizedFileName('json');
  a.click();
}

async function copyJsonClipboard() {
  await navigator.clipboard.writeText(JSON.stringify(currentPreset, null, 2));
  alert('JSON copiado para a área de transferência!');
}

function triggerImportFile() {
  document.getElementById('hiddenFileInput').click();
}

function handleFileImported(ev) {
  const file = ev.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  if (file.name.toLowerCase().endsWith('.json')) {
    reader.onload = e => {
      try {
        const obj = JSON.parse(e.target.result);
        if (obj && obj.pedals) {
          currentPreset = obj;
          renderAll();
          switchTab(0);
        }
      } catch (err) { alert('Arquivo JSON inválido.'); }
    };
    reader.readAsText(file);
  } else {
    reader.onload = e => {
      currentPreset.title = file.name.replace(/\.(a?mk20)$/i, '');
      renderAll();
      switchTab(0);
    };
    reader.readAsArrayBuffer(file);
  }
}

// ============================================================================
// ABA 3: BIBLIOTECA DE PRESETS SALVOS
// ============================================================================
async function saveCurrentPresetToLibrary() {
  const clone = JSON.parse(JSON.stringify(currentPreset));
  clone.id = Date.now();
  savedPresets.unshift(clone);
  await fetch('/api/saved', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(savedPresets)
  });
  renderSavedPresetsList();
  alert('Preset ""' + clone.title + '"" salvo nos Favoritos!');
}

function renderSavedPresetsList() {
  const box = document.getElementById('savedPresetsContainer');
  const q = (document.getElementById('savedSearchInput').value || '').toLowerCase();
  const list = savedPresets.filter(p => !q || p.title.toLowerCase().includes(q) || (p.style || '').toLowerCase().includes(q));

  if (list.length === 0) {
    box.innerHTML = `<div style=""text-align:center; padding:32px; color:var(--text-muted);"">Nenhum preset salvo encontrado. Gere um timbre e clique em 'Salvar nos Favoritos'!</div>`;
    return;
  }

  box.innerHTML = list.map((p, idx) => `<div style=""background:#0E1420; border:1px solid var(--metal-border); border-radius:12px; padding:12px 14px; display:flex; align-items:center; justify-content:space-between;"">
    <div style=""cursor:pointer; flex:1;"" onclick=""loadSavedPresetById(${p.id})"">
      <div style=""font-size:15px; font-weight:800; color:#fff;"">${p.title}</div>
      <div style=""font-size:11px; color:var(--led-emerald); font-family:Consolas,monospace;"">${p.style || 'Custom'} • ${p.bpm || 120} BPM</div>
    </div>
    <div style=""display:flex; gap:8px;"">
      <button class=""small-btn active"" onclick=""loadSavedPresetById(${p.id})"">Carregar</button>
      <button class=""small-btn"" onclick=""deleteSavedPresetById(${p.id})"">🗑️</button>
    </div>
  </div>`).join('');
}

function loadSavedPresetById(id) {
  const found = savedPresets.find(p => p.id === id);
  if (found) {
    currentPreset = JSON.parse(JSON.stringify(found));
    renderAll();
    switchTab(0);
  }
}

async function deleteSavedPresetById(id) {
  savedPresets = savedPresets.filter(p => p.id !== id);
  await fetch('/api/saved', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(savedPresets)
  });
  renderSavedPresetsList();
}

// ============================================================================
// MODAL DE CONFIGURAÇÕES
// ============================================================================
function openSettingsModal() {
  document.getElementById('settingsModal').style.display = 'flex';
}
function closeSettingsModal() {
  document.getElementById('settingsModal').style.display = 'none';
}
async function saveSettingsKey() {
  geminiApiKey = document.getElementById('geminiKeyInput').value.trim();
  await fetch('/api/settings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ apiKey: geminiApiKey })
  });
  closeSettingsModal();
}
</script>
</body>
</html>";
        }
    }

    public class TrayHostContext : ApplicationContext
    {
        private NotifyIcon trayIcon;
        private string appUrl;

        public TrayHostContext(string url)
        {
            appUrl = url;
            ContextMenu menu = new ContextMenu();
            menu.MenuItems.Add("Abrir Preset20 AI", (s, e) => Process.Start(appUrl));
            menu.MenuItems.Add("Fechar Preset20 AI", (s, e) =>
            {
                trayIcon.Visible = false;
                Environment.Exit(0);
            });

            trayIcon = new NotifyIcon
            {
                Icon = System.Drawing.SystemIcons.Application,
                Text = "Preset20 AI (M-Vave MK-20)",
                ContextMenu = menu,
                Visible = true
            };
            trayIcon.DoubleClick += (s, e) => Process.Start(appUrl);
        }
    }
}
