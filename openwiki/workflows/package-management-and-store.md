---
type: workflow
title: Termux Store & Package Management Workflow
description: Package discovery, dynamic installation inspection, guide dialogs, direct active session execution, and curated catalog management in TermuxStoreActivity.
tags: [termux-store, packages, installation, activity, workflow, direct-session-execution, ai-cli]
sources:
  - id: openwiki-source-store-activity
    resource: repo://app/src/main/java/com/termux/app/store/TermuxStoreActivity.java
  - id: openwiki-source-store-item
    resource: repo://app/src/main/java/com/termux/app/store/StorePackageItem.java
  - id: openwiki-source-store-adapter
    resource: repo://app/src/main/java/com/termux/app/store/StorePackageAdapter.java
  - id: openwiki-source-termux-activity
    resource: repo://app/src/main/java/com/termux/app/TermuxActivity.java
---

# Termux Store & Package Management Workflow

The **Termux Store** (`com.termux.app.store`) is an interactive package discovery, installation, and tools management subsystem within the `:app` module. Styled after modern application stores (Google Play Store dark theme), it provides a curated catalog enabling users to search, inspect installation state, read beginner and initialization guides, copy setup commands, and install, update, launch, or uninstall CLI tools, programming runtimes, system utilities, and AI developer CLI tools directly in their active terminal session.

---

## Architecture & Interaction Flow

`TermuxStoreActivity` interacts directly with the local Termux filesystem to determine package installation state, and communicates with `TermuxActivity` via `ACTION_EXECUTE_IN_CURRENT_SESSION` to execute commands inside the active terminal session without spawning separate background scripts or secondary sessions.

```mermaid
sequenceDiagram
    participant User as User
    participant Store as TermuxStoreActivity
    participant Adapter as StorePackageAdapter
    participant FS as Termux Filesystem ($PREFIX/bin)
    participant Act as TermuxActivity
    participant Term as TerminalSession

    User->>Store: Open Store & Browse / Filter by Category or Search Query
    Store->>Adapter: Bind package item
    Adapter->>FS: Check binaryCheckPath (split by ':' & File.exists)
    FS-->>Adapter: Presence confirmed (isInstalled: true/false)
    Adapter-->>User: Render state ("Instalar" / "Actualizar" / "Ejecutar" + optional "Desinstalar")

    alt Direct Action Click ("Instalar", "Actualizar", "Ejecutar", "Desinstalar")
        User->>Adapter: Tap Action Button
        Adapter->>Store: onActionClick(item, isInstalled) / onSecondaryActionClick(item)
        Store->>Act: Intent(ACTION_EXECUTE_IN_CURRENT_SESSION, EXTRA_COMMAND)
        Act->>Term: Write shellCommand + \r to active session
        Term-->>User: Command executes immediately in foreground terminal
    else Detail & Beginner Guide Dialog
        User->>Adapter: Tap Package Card
        Adapter->>Store: onItemClick(item)
        Store->>User: Display MaterialAlertDialog (Init Guide, Help, Quick Tip)
        User->>Store: Tap "Copiar comandos" or action button
        Store->>Act: Intent(ACTION_EXECUTE_IN_CURRENT_SESSION) or copy to ClipboardManager
    end
```
*Sequence of package inspection, guide interaction, and direct command execution from Termux Store.*

---

## Core Components & Data Model

### 1. `StorePackageItem` (`com.termux.app.store.StorePackageItem`)
Represents an individual entry in the package store catalog:
- **`id`** (`String`): Unique package identifier (e.g., `"git"`, `"openwiki"`, `"codegraph"`, `"engram"`, `"zsh"`, `"neovim"`).
- **`title`** (`String`): Human-readable package title displayed in cards and dialogs.
- **`categoryId`** (`String`): Category ID for filtering (`"system"`, `"tools"`, `"dev"`, `"google"`, `"ia"`).
- **`category`** (`String`): Human-readable category display badge (e.g., `"IA y CLI • Wiki & Arquitectura"`).
- **`description`** (`String`): Detailed overview of tool capabilities.
- **`iconResId`** (`int`): Resource ID of the package icon drawable (`R.drawable.ic_pkg_*`).
- **`installCommand`** (`String`): CLI command string executed to install (e.g., `"pkg install lazygit -y"` or `"npm install -g @colbymchenry/codegraph"`).
- **`uninstallCommand`** (`String`, nullable): Optional removal command (e.g., `"pkg uninstall lazygit -y"`).
- **`updateCommand`** (`String`, nullable): Optional update command executed when the package is already installed.
- **`binaryCheckPath`** (`String`, nullable): Colon-separated (`:`) list of candidate binary paths in `$PREFIX/bin/` or `$HOME/` used to verify installation status.
- **`rating`** (`String`): Star rating display (e.g., `"4.9 ★"`).
- **`size`** (`String`): Approximate download/install size (e.g., `"15 MB"`).
- **`initGuide`** (`String`, nullable): Step-by-step instructions for initializing the tool.
- **`helpGuide`** (`String`, nullable): Commands for accessing help and documentation.
- **`quickTip`** (`String`, nullable): Beginner tips and best practices.
- **`ActionType`** (`enum`): `INSTALL`, `UPDATE`, `UNINSTALL`, `EXECUTE`.

### 2. `StorePackageAdapter` (`com.termux.app.store.StorePackageAdapter`)
A `RecyclerView.Adapter` rendering package cards and handling user interaction:
- **Filtering**: Filters items in real time based on search query strings and active category chips.
- **Dynamic Binary Status**: Iterates through colon-separated paths in `item.getBinaryCheckPath()`. If any path exists (`new File(path).exists()`), the package is marked as installed.
- **Button State Resolution**:
  - If `binaryCheckPath == null`: Displays single **"Ejecutar"** button (for system utilities like storage permission setup or backup/restore).
  - If installed: Displays primary **"Actualizar"** button and secondary **"Desinstalar"** button (if `uninstallCommand` is present).
  - If not installed: Displays primary **"Instalar"** button.
- **Package Details Dialog**: Clicking the card triggers `onItemClick()`, presenting a Material Design dialog containing `description`, `initGuide`, `helpGuide`, `quickTip`, and a **"Copiar comandos"** button.

### 3. `TermuxStoreActivity` (`com.termux.app.store.TermuxStoreActivity`)
Main activity managing the catalog lifecycle and terminal bridging:
- **Search & Chips**: Real-time search bar with clear button (`store_search_clear`) and horizontal category chips (`"Para ti"`, `"Sistema"`, `"Herramientas"`, `"Desarrollo"`, `"Google"`, `"IA & CLI"`).
- **`executeShellCommandInTerminal()`**: Bridges package actions directly to the active terminal session.
- **Script Cleanup**: Defensively removes any legacy `setup_zsh.sh` files from `$HOME` or cache directory to prevent conflicting multi-step script executions.

---

## Direct Active Session Execution Pipeline

Unlike legacy approaches that created temporary `.sh` shell scripts or opened separate sessions:
1. `TermuxStoreActivity` packages the target shell command string.
2. Constructs an explicit `Intent` targeting `TermuxActivity`:
   ```java
   Intent termuxIntent = new Intent(this, TermuxActivity.class);
   termuxIntent.setAction(TermuxActivity.ACTION_EXECUTE_IN_CURRENT_SESSION);
   termuxIntent.putExtra(TermuxActivity.EXTRA_COMMAND, commandToSend);
   termuxIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
   startActivity(termuxIntent);
   ```
3. `FLAG_ACTIVITY_REORDER_TO_FRONT` brings the existing `TermuxActivity` directly to the foreground without recreating it.
4. `TermuxActivity.onNewIntent(Intent)` receives `ACTION_EXECUTE_IN_CURRENT_SESSION`, extracts `EXTRA_COMMAND`, and writes it followed by a carriage return (`\r`) directly into the active `TerminalSession`.
5. The command runs interactively in the current terminal prompt, allowing users to watch installation progress and respond to interactive prompts (such as `[Y/n]` confirmations).

---

## Curated Catalog Categories & Packages

The catalog is organized into five functional categories:

| Category ID | Display Label | Packages Included | Primary Commands |
|---|---|---|---|
| **`ia`** | **IA & CLI** | OpenWiki CLI, CodeGraph, Engram | `npm install -g openwiki`, `npm install -g @colbymchenry/codegraph`, `engram` setup |
| **`system`** | **Sistema** | Termux Storage (`termux-setup-storage`), Backup/Restore, Mirrors Config (`termux-change-repo`), Termux-API | `termux-setup-storage`, `pkg install termux-api -y` |
| **`tools`** | **Herramientas** | Zsh + Oh My Zsh, Lazygit, Yazi, Neovim, Tmux, Htop | Direct `pkg install` + theme setups |
| **`dev`** | **Desarrollo** | Git, GitHub CLI (`gh`), Node.js, Python, Golang, Rust | Runtime installs and CLI compilers |
| **`google`** | **Google** | Gemini CLI, Cloud SDK, Angular CLI, Antigravity | CLI developer tools for cloud and web |

---

## Invariants & Error Handling

- **Zero Temporary Scripts**: No `.sh` scripts are written to `$HOME` or cache directories during store package dispatch. All executions stream directly to the foreground terminal session.
- **Multi-Path Binary Verification**: Packages providing multiple binaries or alternative paths (e.g., `binaryCheckPath` containing multiple colon-delimited paths) are considered installed if any candidate exists.
- **Safe Session Fallback**: If `TermuxActivity` has no active session when receiving `ACTION_EXECUTE_IN_CURRENT_SESSION`, it ensures the primary session is initialized before writing the command.
- **Clipboard Utility Integration**: Users can preview initialization and help commands and copy them cleanly to the Android system clipboard without triggering terminal execution.
