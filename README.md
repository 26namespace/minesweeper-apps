# Minesweeper Apps

A Minesweeper implementation written in Kotlin with deterministic board generation.

This is merely a concept, I'm creating a more advanced complex and competitive take on the classic Minesweeper, But early APKs are available on Releases Page:
[https://github.com/26namespace/minesweeper-apps/releases/latest](https://github.com/26namespace/minesweeper-apps/releases/latest)

Preview APKs:
<h1>🎮 Minesweeper Development Preview</h1>

<p>Try the latest version directly in your browser.</p>

<iframe
    src="https://appetize.io/embed/b_o2kkhw7hrshmvdczt76wll3v7e"
    width="378"
    height="800"
    frameborder="0"
    scrolling="no">
</iframe>

<h2>🆕 Changelog</h2>

<h3>Version 0.5.0</h3>
<ul>
    <li>Added new solver</li>
    <li>Added game statistics</li>
    <li>Improved animations</li>
</ul>

<h3>Version 0.4.0</h3>
<ul>
    <li>Added seed system</li>
    <li>Improved board generation</li>
</ul>

<h2>🚧 Currently Working On</h2>

<p>Multiplayer system</p>


<table>
<tr>
<td><img src="assets/Screenshot_20260820-160426.jpg" width="180"></td>
<td><img src="assets/Screenshot_20260820-160655.jpg" width="180"></td>
</tr>
</table>

## Custom Feature

- Seed format `<A-Z>-<0-9A-F>{1,16}` (for now)
- Double tap Chord (for now)
- Long press to flag cells (for now)
- Flood fill cascading
- 2x2 to 30x30 board sizes (for now)
- First click safe zone
- Shareable seeds for reproducible board layouts (for now)

## Custom Generation

- FNV-1a 64-bit (for now)
- SplitMix64 initialization (for now)
- xorshift64* RNG (for now)
- Fisher-Yates shuffle (for now)

## Build

Required JDK 17.

If Gradle 9.5.x Available:

```bash
gradle :app:assembleDebug
```

Note:
The project deliberately uses Android Gradle Plugin 9.3.0 and Gradle 9.5.0. AGP 9.3 requires Gradle 9.5.0 or later.

