@echo off
title Compilador Nativo - Preset20AI.exe (Windows)
echo ===================================================================
echo   Gerando executavel nativo presetai20.exe para Windows 10 / 11...
echo ===================================================================
set RES_FLAGS=
if exist "%~dp0..\app\src\main\assets\tutorial\pedaleira_fisica.jpg" (
    set RES_FLAGS=/resource:"%~dp0..\app\src\main\assets\tutorial\pedaleira_fisica.jpg",pedaleira_fisica.jpg /resource:"%~dp0..\app\src\main\assets\tutorial\passo_1.png",passo_1.png /resource:"%~dp0..\app\src\main\assets\tutorial\passo_2.png",passo_2.png /resource:"%~dp0..\app\src\main\assets\tutorial\passo_3.png",passo_3.png /resource:"%~dp0..\app\src\main\assets\tutorial\passo_4.png",passo_4.png /resource:"%~dp0..\app\src\main\assets\tutorial\passo_5.png",passo_5.png /resource:"%~dp0..\app\src\main\assets\tutorial\passo_6.png",passo_6.png
)
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /target:winexe /out:"%~dp0presetai20.exe" /r:System.Windows.Forms.dll /r:System.Drawing.dll %RES_FLAGS% "%~dp0Preset20AI.cs"
if exist "%~dp0presetai20.exe" (
    echo.
    echo [SUCESSO] O arquivo presetai20.exe foi criado nesta pasta!
    echo Iniciando presetai20.exe...
    start "" "%~dp0presetai20.exe"
) else (
    echo [ERRO] Falha ao compilar presetai20.exe.
    pause
)
