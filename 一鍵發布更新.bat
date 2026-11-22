@echo off
chcp 65001 >nul
title C17 鎖屏流體雲優化 - 一鍵編譯與發布 (GitHub + LSPosed 倉庫)
cd /d "%~dp0"

echo ========================================================
echo   C17 鎖屏流體雲優化 - 自動化編譯與上傳腳本
echo   1. 自動檢查/編譯最新簽名 APK
echo   2. 自動提交並推送原始碼至 nicochanyo/c17-fpoverlap
echo   3. 自動建立 Release 並上傳 APK 至 LSPosed 倉庫與原始碼倉庫
echo ========================================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0publish.ps1"

echo.
pause
