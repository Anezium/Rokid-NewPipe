# Rokid NewPipe

Rokid NewPipe is based on TeamNewPipe/NewPipe and keeps the upstream Java/Kotlin package layout so future merges stay practical. The installed Android package is `com.anezium.rokid.newpipe`.

## Glasses UX Baseline

- Primary target: Rokid glasses at 480 x 640 portrait.
- Rendering: black root, high-contrast text, outline controls, minimal filled surfaces.
- Input: one swipe axis plus tap/back. Directional aliases are debounced so `DPAD_RIGHT` followed by `DPAD_DOWN` counts as one move.
- Search: the Rokid keyboard is a one-axis carousel. Swipe cycles keys, tap inserts/activates, back closes it.
- Player: swipe left/right seeks backward/forward by the configured NewPipe seek duration. Tap shows or activates playback controls.

## Local Dev Loop

```powershell
.\scripts\rokid-dev-loop.ps1
```

With a connected device:

```powershell
.\scripts\rokid-dev-loop.ps1 -Install -SmokeKeys
```

With an explicit ADB serial:

```powershell
.\scripts\rokid-dev-loop.ps1 -Serial <serial> -Install -SmokeKeys
```

## GitHub Workflow

`.github/workflows/rokid-glasses.yml` builds the debug APK, runs JVM tests, and uploads the APK artifact. A manual dispatch can run connected Android checks when the runner has a device attached.
