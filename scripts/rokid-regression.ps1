param(
    [string]$GlassesSerial = "",
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$RunPlayer
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Artifacts = Join-Path $Root "qa\artifacts"
$Package = "com.anezium.rokid.newpipe"
$Apk = Join-Path $Root "app\build\outputs\apk\debug\app-debug.apk"
New-Item -ItemType Directory -Force -Path $Artifacts | Out-Null

function Invoke-GlassesAdb {
    param([string[]]$Arguments)
    $adbArgs = @()
    if ($GlassesSerial -ne "") {
        $adbArgs += @("-s", $GlassesSerial)
    }
    $adbArgs += $Arguments
    & adb @adbArgs
}

function Save-DeviceScreenshot {
    param([string]$Name)
    $remote = "/sdcard/$Name.png"
    $local = Join-Path $Artifacts "$Name.png"
    Invoke-GlassesAdb -Arguments @("shell", "screencap", "-p", $remote) | Out-Null
    Invoke-GlassesAdb -Arguments @("pull", $remote, $local) | Out-Null
    Invoke-GlassesAdb -Arguments @("shell", "rm", $remote) | Out-Null
    Write-Host "Saved screenshot: $local"
}

function Save-WindowState {
    param([string]$Name)
    $local = Join-Path $Artifacts "$Name-window.txt"
    Invoke-GlassesAdb -Arguments @("shell", "dumpsys", "window", "windows") |
            Out-File -Encoding utf8 $local
    Write-Host "Saved window state: $local"
}

function Save-UiDump {
    param([string]$Name)
    $remote = "/sdcard/$Name.xml"
    $local = Join-Path $Artifacts "$Name-uiautomator.xml"
    Invoke-GlassesAdb -Arguments @("shell", "uiautomator", "dump", $remote) | Out-Null
    Invoke-GlassesAdb -Arguments @("pull", $remote, $local) | Out-Null
    Invoke-GlassesAdb -Arguments @("shell", "rm", $remote) | Out-Null
    Write-Host "Saved UI dump: $local"
}

function Send-Key {
    param([int]$KeyCode)
    Invoke-GlassesAdb -Arguments @("shell", "input", "keyevent", "$KeyCode") | Out-Null
    Start-Sleep -Milliseconds 500
}

function Invoke-RailAction {
    param([int]$RightSwipes)
    Start-Sleep -Seconds 9
    Send-Key 23
    for ($index = 0; $index -lt $RightSwipes; $index++) {
        Send-Key 22
    }
    Send-Key 23
    Start-Sleep -Seconds 1
}

Write-Host "Rokid NewPipe regression smoke"
Write-Host "Package: $Package"
Write-Host "Scenarios:"
Write-Host " - app-launch-smoke"
Write-Host " - search-keyboard-swipe"
Write-Host " - offline-wifi-recovery"
Write-Host " - r08-accessibility-tree"
if ($RunPlayer) {
    Write-Host " - player-rail-actions"
}

if (-not $SkipBuild) {
    Push-Location $Root
    try {
        & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --stacktrace -DskipFormatKtlint
    } finally {
        Pop-Location
    }
}

if (-not $SkipInstall) {
    Invoke-GlassesAdb -Arguments @("install", "-r", $Apk)
}

Invoke-GlassesAdb -Arguments @("logcat", "-c") | Out-Null
Invoke-GlassesAdb -Arguments @("shell", "am", "force-stop", $Package) | Out-Null
Invoke-GlassesAdb -Arguments @(
        "shell", "monkey", "-p", $Package, "-c", "android.intent.category.LAUNCHER", "1"
) | Out-Null
Start-Sleep -Seconds 10
Save-DeviceScreenshot "01-launch"
Save-WindowState "01-launch"

Send-Key 21
Send-Key 23
Start-Sleep -Seconds 1
Save-DeviceScreenshot "02-search-keyboard"
Save-UiDump "02-search-keyboard"

Invoke-GlassesAdb -Arguments @("shell", "input", "keyevent", "4") | Out-Null
Start-Sleep -Milliseconds 800
Send-Key 22
Send-Key 23
Start-Sleep -Seconds 1
Save-WindowState "03-wifi-recovery"
Save-DeviceScreenshot "03-wifi-recovery"

if ($RunPlayer) {
    Invoke-GlassesAdb -Arguments @(
            "shell",
            "am",
            "start",
            "-a",
            "android.intent.action.VIEW",
            "-d",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "-p",
            $Package
    ) | Out-Null
    Start-Sleep -Seconds 3
    Invoke-GlassesAdb -Arguments @("shell", "input", "tap", "210", "224") | Out-Null
    Start-Sleep -Milliseconds 600
    Invoke-GlassesAdb -Arguments @("shell", "input", "tap", "180", "574") | Out-Null
    Start-Sleep -Seconds 8
    Send-Key 23
    Send-Key 22
    Send-Key 22
    Send-Key 23
    Start-Sleep -Seconds 1
    Save-DeviceScreenshot "04-player-rail"
    Save-UiDump "04-player-rail"

    Start-Sleep -Seconds 12
    Save-DeviceScreenshot "04-player-after-hide"

    Invoke-RailAction 3
    Save-DeviceScreenshot "05-player-quality-menu"
    Save-UiDump "05-player-quality-menu"
    Send-Key 4

    Invoke-RailAction 4
    Save-DeviceScreenshot "06-player-speed-menu"
    Save-UiDump "06-player-speed-menu"
    Send-Key 4

    Invoke-RailAction 5
    Save-DeviceScreenshot "07-player-subs-menu"
    Save-UiDump "07-player-subs-menu"
    Send-Key 4

    Invoke-RailAction 2
    Save-DeviceScreenshot "08-player-fullscreen"
    Save-UiDump "08-player-fullscreen"
}

Invoke-GlassesAdb -Arguments @("logcat", "-d", "-v", "time") |
        Out-File -Encoding utf8 (Join-Path $Artifacts "glasses-logcat.txt")
Write-Host "Done. Artifacts: $Artifacts"
