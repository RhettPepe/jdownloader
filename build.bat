@echo off
echo ============================================
echo  JDownloader 2 Bootstrapper - Build Script
echo ============================================

:: Step 1: Compile
echo [1/2] Compiling Main.java...
javac -encoding UTF-8 -d out src\Main.java 2>nul || javac -encoding UTF-8 -d out Main.java
if errorlevel 1 (
    echo ERROR: Compilation failed. Make sure Java JDK is installed.
    pause
    exit /b 1
)
echo       Done.

:: Step 2: Package into runnable JAR
echo [2/2] Packaging into JD2Downloader.jar...
if not exist out mkdir out
cd out
echo Main-Class: Main > manifest.txt
jar cfm ..\JD2Downloader.jar manifest.txt *.class
cd ..

echo.
echo Build complete!  Run with:  java -jar JD2Downloader.jar
echo.
pause
