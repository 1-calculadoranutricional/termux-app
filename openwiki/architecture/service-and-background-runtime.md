---
type: architecture
title: TermuxService & Background Runtime Architecture
description: Foreground service management, session container lifecycle, notification action dispatching, and power/wake locks in TermuxService.
tags: [architecture, service, termux-service, foreground, notification, wakelock, session-manager]
sources:
  - id: openwiki-source-termux-service
    resource: repo://app/src/main/java/com/termux/app/TermuxService.java
  - id: openwiki-source-session-service-client
    resource: repo://app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionServiceClient.java
  - id: openwiki-source-notification-utils
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/notification/TermuxNotificationUtils.java
  - id: openwiki-source-shell-manager
    resource: repo://termux-shared/src/main/java/com/termux/shared/termux/shell/TermuxShellManager.java
verified:
  - by: manual-audit
    at: 2026-11-04T07:10:00.000Z
---

# TermuxService & Background Runtime Architecture

`TermuxService` (`com.termux.app.TermuxService`) is the core engine and execution backbone of Termux. While `TermuxActivity` represents the transient UI layer that can be paused, stopped, or recreated during configuration changes (screen rotations, Android theme switches, process trim), `TermuxService` is a **Foreground Service** (`android.app.Service`) that hosts the container of active terminal sessions and background execution tasks, keeping child PTY shell processes alive.

---

## Architectural Topology & Client Decoupling

`TermuxService` enforces strict separation of concerns between headless session lifecycle management and UI activity rendering:

```mermaid
graph TD
    subgraph UI Layer
        TA["TermuxActivity"]
        TTSAC["TermuxTerminalSessionActivityClient"]
    end

    subgraph Service Layer (Background Runtime)
        TS["TermuxService (Foreground Service)"]
        TTSSC["TermuxTerminalSessionServiceClient (Headless Client)"]
        SM["TermuxShellManager"]
        NOTIF["Notification & Actions Dispatcher"]
        LOCK["WakeLock & WifiLock Controller"]
    end

    subgraph Session & Engine Core
        SESS["List<TerminalSession>"]
        TASKS["List<AppShell>"]
        PTY["PTY Child Processes (bash, zsh, node, etc.)"]
    end

    TA <-->|LocalBinder (Same Process)| TS
    TA -->|Registers when visible| TTSAC
    TTSAC -->|Attached to| SESS

    TS -->|Holds references| TTSSC
    TS -->|Fallback client when Activity dies| TTSSC
    TS -->|Manages| SM
    TS -->|Displays| NOTIF
    TS -->|Controls| LOCK

    SM --> SESS
    SM --> TASKS
    SESS --> PTY
    TASKS --> PTY
```
*Decoupling between TermuxActivity and TermuxService with multi-client session handling.*

---

## Service Lifecycle & Dual Client Architecture

1. **`LocalBinder` Connection**:
   `TermuxService` only operates within the same Android application process (`com.termux`). It does not expose AIDL or cross-process IPC; `TermuxActivity` connects via a private `LocalBinder`.
2. **Dual `TerminalSessionClient` Strategy**:
   - **`TermuxTerminalSessionActivityClient`**: Active when `TermuxActivity` is in the foreground. Handles UI-specific operations (displaying bell vibrations/toasts, updating transcripts in `TerminalView`, managing drawer session lists, clipboard operations).
   - **`TermuxTerminalSessionServiceClient`**: Attached to `TerminalSession` instances when `TermuxActivity` is destroyed or unbound (`unsetTermuxTerminalSessionClient()`). Ensures that background sessions do not leak activity contexts while continuing to run headless.

---

## Foreground Notification & Wakelock Management

Under modern Android background execution limits, any service without a persistent foreground notification would be quickly terminated by the system LMK (Low Memory Killer) or Phantom Process Killer.

### Notification Channels & Priority
- Channel ID: `termux_service_notification_channel` (`R.string.channel_service_notification_id`).
- Display Priority: `PRIORITY_LOW` (or `IMPORTANCE_LOW` on Android 8.0+) to minimize intrusive popups while maintaining foreground priority.

### Interactive Notification Actions
`TermuxService.buildNotification()` configures pending intents allowing direct control from the Android notification shade:

| Action Intent | Identifier | Behavior |
|---|---|---|
| `ACTION_STOP_SERVICE` | `com.termux.service_stop` | Terminates all active terminal sessions, finishes all background tasks, releases all power/wifi locks, and calls `stopSelf()`. |
| `ACTION_WAKE_LOCK` | `com.termux.service_wake_lock` | Acquires a `PowerManager.PARTIAL_WAKE_LOCK` (`TermuxService:WakeLock`) and `WifiManager.WIFI_MODE_FULL_HIGH_PERF` lock to prevent device deep sleep and Wi-Fi suspension. |
| `ACTION_WAKE_UNLOCK` | `com.termux.service_wake_unlock` | Releases acquired wake locks and high-performance Wi-Fi locks to allow standard Android power-saving sleep. |

---

## Methods & Public APIs

| Method | Access | Description |
|---|---|---|
| `runStartForeground()` | `public` | Calls `Service.startForeground()` with the dynamically constructed `Notification` to grant the process foreground exemption. |
| `createTermuxSession(...)` | `public` | Instantiates a new `TermuxSession`, registers it into `TermuxShellManager`, and triggers a notification update showing the active session count. |
| `removeTermuxSession(...)` | `public` | Cleans up session resources. If no running sessions or background tasks remain and `mWantsToStop` is set, stops the service. |
| `setTermuxTerminalSessionClient(...)` | `public` | Switches active terminal sessions from headless service client to UI activity client. |
| `unsetTermuxTerminalSessionClient()` | `public` | Reverts all sessions to headless service client to allow garbage collection of the finishing `TermuxActivity`. |
| `acquireWakeLock()` / `releaseWakeLock()` | `public` | Thread-safe acquisition and release of `PowerManager.WakeLock` and `WifiManager.WifiLock`. |
