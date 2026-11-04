---
type: architecture
title: System Architecture Overview
description: High-level system architecture, component topology, processes, and service lifecycle of Termux-App.
tags: [architecture, overview, termux, android, service, activity, lifecycle]
sources:
  - id: openwiki-source-c8d0e1f25d0be9ba898acecd
    resource: repo://app/src/main/java/com/termux/app/TermuxActivity.java
  - id: openwiki-source-2375a4375bb82706bd6d3275
    resource: repo://app/src/main/java/com/termux/app/TermuxApplication.java
  - id: openwiki-source-84966089f535a69b6da761e1
    resource: repo://app/src/main/java/com/termux/app/TermuxService.java
generated: { by: "openwiki/0.7.2", at: "2026-10-26T20:00:32.746Z" }
verified:
  - by: openwiki/0.7.2
    at: 2026-10-26T20:00:32.746Z
---

# System Architecture Overview

Termux-App is an Android terminal emulator and Linux environment application structured around a clean separation of concerns between application presentation (`TermuxActivity`), long-running background execution and session management (`TermuxService`), application bootstrapping and global initializations (`TermuxApplication`), and plugin/API integration components (such as broadcast receivers and remote action intents).

## Architectural Topology & Component Relationships

The application runs primarily within a single Android process, managed by `TermuxApplication`. The core runtime components and their interactions are illustrated below:

```mermaid
sequenceDiagram
    participant App as TermuxApplication
    participant Store as TermuxStoreActivity
    participant Act as TermuxActivity
    participant Svc as TermuxService
    participant Shell as TermuxShellManager
    participant Term as TerminalSession

    App->>Shell: Initialize shell manager & bootstrap
    Act->>Svc: bindService(Intent) / startService()
    Svc-->>Act: LocalBinder connection established
    Act->>Shell: Create or attach TerminalSession
    Shell->>Term: Spawn terminal execution / PTY session
    Term-->>Act: Output stream / TerminalView rendering
    Note over Act,Svc: TermuxService runs as Foreground Service with persistent notification

    Store->>Act: Intent(ACTION_EXECUTE_IN_CURRENT_SESSION, EXTRA_COMMAND)
    Act->>Term: Write command + \r to active session
```

## Core Component Responsibilities

### 1. `TermuxApplication` (`com.termux.app.TermuxApplication`)
- **Role**: Application-wide entry point extending `android.app.Application`.
- **Responsibilities**:
  - Sets up global crash handling via `TermuxCrashUtils`.
  - Configures logging levels (`TermuxApplication.setLogConfig`).
  - Initializes package variant configurations (`TermuxBootstrap`), app-wide shared properties (`TermuxAppSharedProperties`), and the shell manager (`TermuxShellManager`).
  - Validates accessibility of the Termux files directory (`TermuxFileUtils.isTermuxFilesDirectoryAccessible`) and initializes the `TermuxAmSocketServer` for activity manager socket communication.
  - Generates and writes initial environment variables to file (`TermuxShellEnvironment`).

### 2. `TermuxService` (`com.termux.app.TermuxService`)
- **Role**: Foreground service responsible for maintaining terminal sessions and background execution tasks (`AppShell`) across activity lifecycles.
- **Responsibilities**:
  - Runs as a foreground service (`startForeground`) displaying a persistent status notification to prevent Android from killing active shell sessions.
  - Manages active `TermuxSession` instances and background tasks through `TermuxShellManager`.
  - Provides binder interface (`LocalBinder`) for local communication with `TermuxActivity`.
  - Handles wake locks (`ACTION_WAKE_LOCK`) and wifi locks to ensure long-running execution continuity.
  - Listens to system package update events via `SystemEventReceiver`.

### 3. `TermuxActivity` (`com.termux.app.TermuxActivity`)
- **Role**: The primary user-facing terminal emulator activity (`AppCompatActivity`).
- **Responsibilities**:
  - Binds to `TermuxService` via `ServiceConnection`.
  - Manages the UI hierarchy (`TermuxActivityRootView`), `TerminalView`, terminal session lists (`TermuxSessionsListViewController`), toolbar viewpagers, and extra keys views (`ExtraKeysView`).
  - Implements terminal view clients (`TermuxTerminalViewClient`) and activity session clients (`TermuxTerminalSessionActivityClient`) to handle user input, touch events, visual styling changes, and clipboard interactions.
  - Handles Activity lifecycle transitions (`onResume`, `onStop`, `onDestroy`) ensuring clean disconnection of activity-bound terminal session clients when UI goes out of visibility.

### 4. Auxiliary Activities, Providers, Receivers & Integration Components
- **`TermuxStoreActivity` (`com.termux.app.store.TermuxStoreActivity`)**: An interactive package and tools manager (Play Store style UI) allowing users to discover, install, launch, or configure curated CLI packages (e.g., nodejs, python, git, lazygit, yazi, neovim, openwiki, codegraph) and utilities. It checks binary presence in `$PREFIX/bin/` to display installation state dynamically and dispatches execution commands directly to the active terminal session via `TermuxActivity.ACTION_EXECUTE_IN_CURRENT_SESSION` without generating temporary scripts. See **[Termux Store & Package Management Workflow](../workflows/package-management-and-store.md)** for details.
- **`TermuxDocumentsProvider` (`com.termux.filepicker.TermuxDocumentsProvider`)**: Implements Android's Storage Access Framework (`DocumentsProvider`) to securely expose the Termux `$HOME` directory (`/data/data/com.termux/files/home`) to other Android apps and the system file picker, supporting root queries, document browsing, file reading, creation, and deletion. See **[Storage Access & File Sharing Workflow](../workflows/storage-access-and-file-sharing.md)**.
- **`FileReceiverActivity` (`com.termux.app.api.file.FileReceiverActivity`)**: Handles external file sharing intents (`ACTION_SEND`, `ACTION_SEND_MULTIPLE`) dispatched to Termux, directing files to `~/downloads`, URLs to `termux-url-opener`, or text/scripts to `termux-file-editor`.
- **`CrashHandler` & `ReportActivity` (`com.termux.shared.crash.CrashHandler`, `com.termux.shared.activities.ReportActivity`)**: Application-level crash capture mechanism intercepting uncaught exceptions and formatting forensic crash reports with system and process telemetry. See **[Crash Handling & Diagnostics Architecture](crash-handling-and-diagnostics.md)**.
- **`SettingsActivity` (`com.termux.app.activities.SettingsActivity`)**: Manages application preferences (terminal styling, font size, terminal bell, soft keyboard behaviors) and navigates to the store and report screens.
- **`HelpActivity` (`com.termux.app.activities.HelpActivity`)**: Displays embedded help documentation and user guides.
- **`SystemEventReceiver`**: Manages system broadcast registrations (e.g., package updates, boot completion).
- **`TermuxOpenReceiver`**: Handles `ACTION_OPEN` / `ACTION_VIEW` intents dispatched to open URLs, scripts, and files within Termux.
- **`TermuxAmSocketServer`**: Local socket server supporting Android activity manager integration for sub-processes.

## Lifecycle & Invariants

- **Service Longevity**: `TermuxService` outlives `TermuxActivity`. When the user closes or rotates the activity, unbinding disconnects UI callbacks without terminating active shell sessions or background tasks.
- **Foreground Persistence**: While sessions or background tasks are active, `TermuxService` maintains a foreground notification, ensuring OS resource allocators prioritize the terminal environment.
- **Process Model**: All core components run within the main application process, communicating via local binder IPC and direct state managers (`TermuxShellManager`).
