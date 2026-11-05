@echo off
title Compilador Nativo - Preset20AI.exe (Windows)
echo ===================================================================
echo   Gerando executavel nativo presetai20.exe para Windows 10 / 11...
echo ===================================================================
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /target:winexe /out:"%~dp0presetai20.exe" /r:System.Windows.Forms.dll /r:System.Drawing.dll "%~dp0Preset20AI.cs"
if exist "%~dp0presetai20.exe" (
    echo.
    echo [SUCESSO] O arquivo presetai20.exe foi criado nesta pasta!
    echo Iniciando presetai20.exe...
    start "" "%~dp0presetai20.exe"
) else (
    echo [ERRO] Falha ao compilar presetai20.exe.
    pause
)
