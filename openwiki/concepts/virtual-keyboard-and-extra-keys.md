---
type: concept
title: Virtual Keyboard & Extra Keys Toolbar
description: Architecture of the virtual extra keys bar, touch gestures, special modifier buttons, termux.properties matrix configuration, and terminal input dispatching.
tags: [extra-keys, keyboard, toolbar, virtual-keys, input, gestures, termux-properties]
sources:
  - id: openwiki-source-termux-extra-keys
    resource: repo://app/src/main/java/com/termux/app/terminal/io/TermuxTerminalExtraKeys.java
  - id: openwiki-source-extra-keys-view
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/extrakeys/ExtraKeysView.java
  - id: openwiki-source-special-button
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/extrakeys/SpecialButton.java
  - id: openwiki-source-extra-keys-info
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/extrakeys/ExtraKeysInfo.java
  - id: openwiki-source-toolbar-pager
    resource: repo://app/src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java
verified:
  - by: manual-audit
    at: 2026-11-04T03:00:00.000Z
---

# Virtual Keyboard & Extra Keys Toolbar

The **Virtual Keyboard & Extra Keys Toolbar** subsystem (`com.termux.app.terminal.io` and `com.termux.shared.termux.extrakeys`) provides an extensible on-screen row of control keys (such as `ESC`, `CTRL`, `ALT`, `TAB`, arrow keys, function keys, and action shortcuts) positioned above the software keyboard. It bridges touch interactions with POSIX terminal input streams, allowing full terminal navigation on mobile touchscreens without requiring a physical hardware keyboard.

---

## Architectural Topology

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       EXTRA KEYS COMPONENT TOPOLOGY                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   TermuxActivityRootView                                                    │
│         │                                                                   │
│         ├── TerminalToolbarViewPager                                        │
│         │         ├── Page 0: ExtraKeysView (Virtual Button Matrix)         │
│         │         └── Page 1: Text Input Field (Optional IME direct entry)  │
│         │                                                                   │
│         └── TerminalView (monospaced grid canvas)                           │
│                                                                             │
│   ExtraKeysView                                                             │
│         │                                                                   │
│         ├── SpecialButton / SpecialButtonState (CTRL, ALT, FN, SHIFT)       │
│         │                                                                   │
│         └── TermuxTerminalExtraKeys (Event Dispatcher)                      │
│                   ├── Custom Actions: DRAWER, KEYBOARD, PASTE, SCROLL, F, Y │
│                   └── TerminalSession.write(escapes / characters)           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Control Flow & Key Dispatching

```mermaid
sequenceDiagram
    participant User as User Touch Gesture
    participant EKV as ExtraKeysView
    participant SB as SpecialButton (CTRL / ALT)
    participant TTEK as TermuxTerminalExtraKeys
    participant View as TerminalView
    participant Session as TerminalSession

    User->>EKV: Tap / Long Press / Swipe on Key Button
    alt Special Modifier Key (e.g., CTRL, ALT)
        EKV->>SB: Toggle state (OFF -> ACTIVE -> LOCKED)
        EKV-->>User: Update visual highlight / background
    else Standard Key or Macro (e.g., ESC, TAB, UP)
        EKV->>TTEK: onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown)
        alt Action: KEYBOARD
            TTEK->>View: onToggleSoftKeyboardRequest()
        alt Action: DRAWER
            TTEK->>TTEK: Toggle DrawerLayout open/closed
        alt Action: PASTE
            TTEK->>TTEK: Paste clipboard text into session
        alt Action: SCROLL
            TTEK->>View: Toggle auto-scroll disabled
        alt Action: F
            TTEK->>TTEK: Show PopupMenu for F1-F12 function keys
        alt Action: Y
            TTEK->>Session: Write "yazi\n" (fast launch)
        else Standard Character / Escape Sequence
            TTEK->>Session: Write translated escape code (e.g. \u001b[A)
        end
        EKV->>SB: Reset non-locked modifiers
    end
```

---

## Core Components & Responsibilities

### 1. `ExtraKeysView` (`com.termux.shared.termux.extrakeys.ExtraKeysView`)
Extends `GridLayout` to generate a dynamic button matrix:
- **Button Generation**: Dynamically creates `Button` views for each matrix cell defined in `ExtraKeysInfo`.
- **Swipe Gestures**: Supports directional swiping (up, down, left, right) on individual buttons to trigger alternate secondary characters or macros without consuming screen space.
- **Auto-Repeat**: Implements repeating key presses when a key is held down (e.g., arrow navigation or backspace).
- **Haptic Feedback**: Triggers haptic feedback (`performHapticFeedback`) on press and state transitions.

### 2. `SpecialButton` & `SpecialButtonState`
Manages stateful modifier toggles:
- **State Machine**:
  - `INACTIVE (0)`: Normal inactive state.
  - `ACTIVE (1)`: Modifier applies to the next single keypress, then auto-clears.
  - `LOCKED (2)`: Double-tap locks modifier on indefinitely until tapped again.
- **Supported Modifiers**: `CTRL`, `ALT`, `SHIFT`, `FN`.

### 3. `TermuxTerminalExtraKeys` (`com.termux.app.terminal.io.TermuxTerminalExtraKeys`)
Subclasses `TerminalExtraKeys` to inject Termux-specific application behaviors:
- **`KEYBOARD`**: Triggers soft keyboard toggle via `TermuxTerminalViewClient#onToggleSoftKeyboardRequest`.
- **`DRAWER`**: Opens or closes the left sessions navigation drawer (`DrawerLayout.openDrawer`).
- **`PASTE`**: Invokes `TermuxTerminalSessionActivityClient#onPasteTextFromClipboard`.
- **`SCROLL`**: Toggles terminal auto-scroll locking (`TerminalEmulator#toggleAutoScrollDisabled`).
- **`F`**: Displays a `PopupMenu` presenting function keys `F1` through `F12`.
- **`Y`**: Injects `yazi\n` to instantaneously launch the modern terminal file manager.

### 4. Configuration via `termux.properties`
The matrix layout is entirely customizable by users in `~/.termux/termux.properties`:
```properties
# Example custom extra keys matrix
extra-keys = [ \
  ['ESC', {'key': 'CTRL', 'popup': 'F'}, 'ALT', 'TAB', 'KEYBOARD'], \
  ['DRAWER', 'LEFT', 'DOWN', 'UP', 'RIGHT', 'PASTE'] \
]
extra-keys-style = arrows-only
```
- **Display Style Mappings**: Configures whether arrows display as textual labels (`UP`, `DOWN`) or Unicode glyphs (`↑`, `↓`) via `ExtraKeysConstants.ExtraKeyDisplayMap`.

---

## Method & API Reference

| Class / Method | Signature | Responsibility |
| :--- | :--- | :--- |
| `TermuxTerminalExtraKeys#setExtraKeys` | `() -> void` | Loads `extra-keys` and style properties from `TermuxSharedProperties` and initializes `ExtraKeysInfo`. |
| `TermuxTerminalExtraKeys#onTerminalExtraKeyButtonClick` | `(View, String, boolean, boolean, boolean, boolean) -> void` | Intercepts custom action keys (`DRAWER`, `PASTE`, `SCROLL`, etc.) or delegates to terminal session. |
| `ExtraKeysView#reload` | `(ExtraKeysInfo) -> void` | Rebuilds the button grid layout from a updated `ExtraKeysInfo` configuration. |
| `SpecialButtonState#setState` | `(int) -> void` | Sets modifier button state (`INACTIVE`, `ACTIVE`, `LOCKED`). |
