---
type: concept
title: Quickstart & Wiki Navigation
description: Central routing and task-oriented overview for the Termux App code wiki.
tags: [quickstart, navigation, routing, architecture, terminal]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-28T02:32:52.279Z
sources:
  - id: openwiki-source-23775c3de52f3ab95a13cb8b
    resource: repo://README.md
  - id: openwiki-source-12eccab9eb2d86aae390f578
    resource: repo://settings.gradle
generated: { by: "openwiki/0.7.2", at: "2026-10-26T20:00:32.746Z" }
---

# Quickstart & Wiki Navigation

Welcome to the **Termux App Code Wiki**, an in-depth developer documentation reference covering the architecture, internal modules, terminal emulation engine, view rendering, workflows, and execution semantics of the Termux Android application (`termux-app`).

## Wiki Routing Map

Use this guide to navigate across the major domains of the Termux App codebase. Each page explores a critical subsystem with architectural overviews, control flows, state lifecycles, and verification points.

### Architecture & Modules
- **[System Architecture Overview](architecture/overview.md)**: High-level system architecture, component topology, process/service structure, and security boundaries.
- **[Modules and Dependencies](architecture/modules-and-dependencies.md)**: Breakdown of Gradle modules (`:app`, `:terminal-emulator`, `:terminal-view`, `:termux-shared`), inter-module dependencies, and library integration.
- **[Crash Handling & Diagnostics](architecture/crash-handling-and-diagnostics.md)**: Uncaught exception handling (`CrashHandler`), forensic telemetry (`TermuxCrashUtils`), notification alerts, and Markwon-powered markdown reports (`ReportActivity`).

### Core Emulation & UI Concepts
- **[Terminal Emulation Engine](concepts/terminal-emulation.md)**: DEC VT/ANSI escape sequence processing, screen buffer management (`TerminalBuffer`, `TerminalRow`), color schemes, and screen text handling.
- **[Terminal View & Rendering](concepts/rendering-and-view.md)**: UI rendering pipeline in `TerminalView`, touch gesture translation, scrolling, font metrics, text selection, and rich clipboard content resolution.

### Workflows & Execution
- **[Terminal Session Lifecycle](workflows/session-lifecycle.md)**: End-to-end flow of terminal session creation (`TerminalSession`), process execution via `TermuxTerminalSessionClient`, background execution, and destruction.
- **[Command Execution Workflow](workflows/command-execution.md)**: External command execution, `RunCommandService` intent handling, broadcast receivers, and shell integration.
- **[Termux Store & Package Management](workflows/package-management-and-store.md)**: Curated package catalog discovery, dynamic binary presence inspection, guide dialogs, and direct active session execution (`ACTION_EXECUTE_IN_CURRENT_SESSION`).
- **[Storage Access & File Sharing](workflows/storage-access-and-file-sharing.md)**: Storage Access Framework integration (`TermuxDocumentsProvider`) exposing `$HOME`, and external share intent handling (`FileReceiverActivity`) for downloads, URLs, and scripts.

---

## Development Quickstart

### Project Structure
Termux is structured as a multi-module Android Gradle project (`settings.gradle`):
1. **`:app`**: The primary Android application module containing UI activities (`TermuxActivity`, `TermuxStoreActivity`, `SettingsActivity`), services (`TermuxService`, `RunCommandService`), receivers (`TermuxOpenReceiver`, `SystemEventReceiver`), and configuration handlers.
2. **`:terminal-emulator`**: Pure Java core terminal emulation logic, VT100/ANSI parser, grid buffers, sixel graphics support, and terminal state machine.
3. **`:terminal-view`**: Android View implementation (`TerminalView`) responsible for rendering terminal grids, handling input gestures, and text selection.
4. **`:termux-shared`**: Shared utility library containing file permissions, intents, notification helpers, and common utilities used across Termux apps and plugins.

### Building & Testing
- **Build**: `./gradlew assembleDebug`
- **Unit Tests**: `./gradlew testDebugUnitTest`

---

## Contribution & Maintenance Standards
When exploring or updating the wiki:
- Keep cross-references canonical and relative to the current page directory.
- Ground architectural claims in repository files (`repo://<path>#L...`).
- Provide Mermaid sequence or flowchart diagrams for complex control flows and lifecycles.
