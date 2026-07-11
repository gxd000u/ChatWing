@echo off
chcp 65001 >nul
echo ============================================
echo   ChatWing APK 构建脚本
echo ============================================
echo.

REM 设置环境变量
set JAVA_HOME=D:\work++\jdk
set ANDROID_HOME=D:\work++\android-sdk
set PATH=%JAVA_HOME%\bin;%PATH%

REM 检查工具链
echo [1/5] 检查 Java...
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | find "version"
if %ERRORLEVEL% NEQ 0 (
    echo [FAIL] 请先安装 JDK 17
    pause
    exit /b 1
)

echo [2/5] 检查 Flutter...
flutter --version 2>&1 | find "Flutter"
if %ERRORLEVEL% NEQ 0 (
    echo [FAIL] 请先安装 Flutter SDK (3.24.0+)
    pause
    exit /b 1
)

echo [3/5] 安装 Flutter 依赖...
cd /d "%~dp0"
flutter pub get
if %ERRORLEVEL% NEQ 0 (
    echo [FAIL] flutter pub get 失败
    pause
    exit /b 1
)
echo [OK] 依赖安装完成

echo [4/5] 接受 Android SDK 许可...
call flutter doctor --android-licenses
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] 许可接受失败，可能需要手动运行 flutter doctor --android-licenses
    echo 按任意键继续尝试构建...
    pause >nul
)

echo [5/5] 构建 APK...
flutter build apk --release
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo   构建成功！
    echo   APK 位置: build\app\outputs\flutter-apk\app-release.apk
    echo ============================================
) else (
    echo [FAIL] 构建失败
    echo 请检查错误信息后重试
)

pause
