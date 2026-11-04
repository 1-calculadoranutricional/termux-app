---
type: concept
title: Settings & Properties Architecture
description: Dual configuration subsystem spanning Android SharedPreferences and file-based termux.properties with live reload broadcast mechanics.
tags: [settings, preferences, termux-properties, configuration, shared-preferences, reload-styling]
sources:
  - id: openwiki-source-settings-activity
    resource: repo://app/src/main/java/com/termux/app/activities/SettingsActivity.java
  - id: openwiki-source-shared-preferences
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/settings/preferences/TermuxAppSharedPreferences.java
  - id: openwiki-source-shared-properties
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/settings/properties/TermuxAppSharedProperties.java
  - id: openwiki-source-property-constants
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/settings/properties/TermuxPropertyConstants.java
verified:
  - by: manual-audit
    at: 2026-11-04T07:10:00.000Z
---

# Settings & Properties Architecture

Termux employs a **dual configuration architecture** designed to bridge standard Android platform preferences (accessible via GUI menus) with traditional Unix file-based dotfiles (`~/.termux/termux.properties`). This separation guarantees that terminal emulator behaviors, font rendering, keyboard modifiers, and plugin integrations remain consistent and customizable through both graphical and shell-scripted interfaces.

---

## Architecture: Dual-Tier Configuration Model

```mermaid
graph TD
    subgraph Tier 1: Android SharedPreferences (GUI & Framework)
        SA["SettingsActivity"]
        FRAG["PreferenceFragments (TerminalView, IO, Debugging, Plugins)"]
        TASP["TermuxAppSharedPreferences"]
        XML["XML Storage (/data/data/com.termux/shared_prefs/)"]
        SA --> FRAG
        FRAG --> TASP
        TASP --> XML
    end

    subgraph Tier 2: Unix Dotfiles (File-Based & Dynamic)
        PROP_FILE["~/.termux/termux.properties"]
        PARSER["SharedPropertiesParser"]
        TASProp["TermuxAppSharedProperties (Singleton In-Memory)"]
        PROP_FILE --> PARSER
        PARSER --> TASProp
    end

    subgraph Synchronization & Live Reload
        BROADCAST["Broadcast: ACTION_RELOAD_STYLE"]
        TR["termux-reload-settings (CLI Utility)"]
        TA["TermuxActivity Receiver"]
        TV["TerminalView & ExtraKeysView"]

        TR -->|Sends broadcast| BROADCAST
        BROADCAST --> TA
        TA -->|Reloads from disk| TASProp
        TA -->|Updates UI in place| TV
    end
```
*Dual-tier configuration model and live styling reload pipeline.*

---

## Comparison: `TermuxAppSharedPreferences` vs `TermuxAppSharedProperties`

| Feature | `TermuxAppSharedPreferences` | `TermuxAppSharedProperties` |
|---|---|---|
| **Storage Medium** | Private Android XML files (`com.termux_preferences.xml`) | Flat Unix key-value file (`$HOME/.termux/termux.properties`) |
| **Primary Interface** | `SettingsActivity` and Android Settings UI fragments | Text editors (`nano`, `vim`), CLI scripts, or dotfiles repositories |
| **Typical Settings** | Font size (`fontsize`), toolbar visibility toggle, soft keyboard enabled/disabled, key logging toggle | Extra keys matrix (`extra-keys`), bell behavior (vibrate/beep/ignore), cursor blink rate, hardware shortcuts, terminal margins |
| **Scope** | App UI layout and internal framework preferences | Terminal emulation mechanics and CLI environment defaults |
| **Access Pattern** | Injected via `Context` / Android `SharedPreferences` API | Parsed into memory via `TermuxSharedProperties` on startup and reloaded on demand |

---

## Live Reload Pipeline (`ACTION_RELOAD_STYLE`)

Users can update `~/.termux/termux.properties` directly from the command line without restarting Termux:

1. **Trigger**: Running `termux-reload-settings` broadcasts `com.termux.app.reload_style`.
2. **Reception**: `TermuxActivity.TermuxActivityBroadcastReceiver` intercepts the broadcast intent.
3. **Execution**:
   - Calls `mProperties.loadTermuxPropertiesFromDisk()`.
   - Re-evaluates night mode (`TermuxThemeUtils.setAppNightMode()`).
   - Reloads extra keys matrix via `mExtraKeysView.reload()`.
   - Updates margins, font metrics, and cursor blink rate.
   - If `recreateActivity` is set to `true`, cleanly recreates the activity preserving transcript buffers.
