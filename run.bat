@echo off
cd /d "%~dp0"
echo Compiling project...
javac -cp ".;*" *.java
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)
echo Launching Inventory Management System...
java -cp ".;*" Main
