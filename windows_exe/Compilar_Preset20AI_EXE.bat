@echo off
title Compilador Nativo - Preset20AI.exe (Windows)
pushd "%~dp0"
echo ===================================================================
echo   Gerando executavel nativo presetai20.exe para Windows 10 / 11...
echo ===================================================================
taskkill /f /im presetai20.exe >nul 2>&1
if exist "presetai20.exe" del /f /q "presetai20.exe"

if exist "..\app\src\main\assets\tutorial\pedaleira_fisica.jpg" goto COM_IMAGENS
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /target:winexe /out:"presetai20.exe" /r:System.Windows.Forms.dll /r:System.Drawing.dll "Preset20AI.cs"
goto VERIFICAR

:COM_IMAGENS
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /target:winexe /out:"presetai20.exe" /r:System.Windows.Forms.dll /r:System.Drawing.dll /resource:"..\app\src\main\assets\tutorial\pedaleira_fisica.jpg",pedaleira_fisica.jpg /resource:"..\app\src\main\assets\tutorial\passo_1.png",passo_1.png /resource:"..\app\src\main\assets\tutorial\passo_2.png",passo_2.png /resource:"..\app\src\main\assets\tutorial\passo_3.png",passo_3.png /resource:"..\app\src\main\assets\tutorial\passo_4.png",passo_4.png /resource:"..\app\src\main\assets\tutorial\passo_5.png",passo_5.png /resource:"..\app\src\main\assets\tutorial\passo_6.png",passo_6.png "Preset20AI.cs"

:VERIFICAR
if not exist "presetai20.exe" goto ERRO
echo.
echo [SUCESSO] O arquivo presetai20.exe foi criado nesta pasta!
echo Iniciando presetai20.exe...
start "" "presetai20.exe"
popd
exit /b 0

:ERRO
echo.
echo [ERRO] Falha ao compilar presetai20.exe. Verifique as mensagens acima.
popd
pause
