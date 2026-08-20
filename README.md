# Minesweeper 64-bit Seed

## Custom Feature

- Seed format `<A-Z>-<1-9A-F>`
- Chord - Double Tap
- Shareable seeds (the same board can be replayed)
- Board 2x2 - 30x30
- FNV-1a 64-bit
- SplitMix64 initialization
- xorshift64* RNG
- Fisher-Yates shuffle

## Build

Required JDK 17.

If Gradle 9.5.x Available:

```bash
gradle :app:assembleDebug
```

APK:
`app/build/outputs/apk/debug/app-debug.apk`

Note:
The project deliberately uses Android Gradle Plugin 9.3.0 and Gradle 9.5.0. AGP 9.3 requires Gradle 9.5.0 or later.

