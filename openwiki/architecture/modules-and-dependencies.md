---
type: reference
title: Modules and Dependencies
description: Detailed breakdown of the multi-module project structure, Gradle configuration, and inter-module dependencies in Termux app.
tags: [gradle, modules, dependencies, android, architecture]
sources:
  - id: openwiki-source-ba5e180102dd55751678e7a4
    resource: repo://app/build.gradle
  - id: openwiki-source-b68c69ba290f0fee793ec69b
    resource: repo://gradle.properties
  - id: openwiki-source-12eccab9eb2d86aae390f578
    resource: repo://settings.gradle
  - id: openwiki-source-6cf531d9d25ef5c9fb296b5b
    resource: repo://terminal-emulator/build.gradle
  - id: openwiki-source-42e8edc4e833393fc9058cb7
    resource: repo://terminal-view/build.gradle
  - id: openwiki-source-0bd423d182fbcfbcec59b8b7
    resource: repo://termux-shared/build.gradle
generated: { by: "openwiki/0.7.2", at: "2026-10-26T20:00:32.746Z" }
verified:
  - by: openwiki/0.7.2
    at: 2026-10-26T20:00:32.746Z
---

# Modules and Dependencies

Termux is structured as a multi-module Gradle project comprising an application module and three core library modules (`:app`, `:termux-shared`, `:terminal-emulator`, `:terminal-view`). This separation modularizes concerns across terminal emulation, rendering views, shared utilities/bootstrap logic, and the main application UI.

## Project Structure and Modules

As defined in `settings.gradle` (repo://settings.gradle#L1), the project includes four Gradle modules:

1. **`app`** (`com.termux`):
   - **Role**: The main Android application module (`com.android.application`).
   - **Responsibilities**: Provides the primary user interface (activities, fragments, drawer layout, preferences), terminal session management, settings storage, execution services, bootstrapping, and the curated package store subsystem (`com.termux.app.store`).
   - **Dependencies**: Depends on `:terminal-view` and `:termux-shared`, as well as AndroidX libraries, Material Components, Guava, and Markwon Markdown parser.

2. **`termux-shared`** (`com.termux.shared`):
   - **Role**: Shared library module (`com.android.library`).
   - **Responsibilities**: Houses shared utilities, file operations, permissions, intent handling, bootstrap package configuration (`TermuxBootstrap.PackageVariant`), and execution helpers.
   - **Dependencies**: Depends on `:terminal-view`, AndroidX libraries, `hiddenapibypass`, `commons-io` (pinned to version `2.5` to avoid runtime exceptions on Android < 8 due to missing `java.nio.file.Path`), and `termux-am-library`.

3. **`terminal-view`** (`com.termux.view`):
   - **Role**: UI library module for rendering the terminal (`com.android.library`).
   - **Responsibilities**: Provides `TerminalView` and related view classes for handling touch input, scaling, scrolling, and rendering terminal screen contents.
   - **Dependencies**: Uses `api project(":terminal-emulator")` to expose the emulator API upstream, along with AndroidX annotations.

4. **`terminal-emulator`** (`com.termux.emulator`):
   - **Role**: Pure Java/C terminal emulation library (`com.android.library`).
   - **Responsibilities**: Implements ANSI/DEC VT terminal state machine emulation, screen buffer management, key input translation, and JNI-based terminal backend processing (`Android.mk`).
   - **Dependencies**: Pure library module with no upstream module dependencies; depends only on Android annotations and JUnit for unit testing.

## Inter-Module Dependency Graph

```mermaid
graph TD
    subgraph Application
        app["app (:app)"]
    end

    subgraph Shared Utilities
        termuxShared["termux-shared (:termux-shared)"]
    end

    subgraph Terminal Rendering & Emulation
        terminalView["terminal-view (:terminal-view)"]
        terminalEmulator["terminal-emulator (:terminal-emulator)"]
    end

    app --> termuxShared
    app --> terminalView
    termuxShared --> terminalView
    terminalView -->|api| terminalEmulator
```

## Build Configuration & SDK Versions

Global project properties are centralized in `gradle.properties` (repo://gradle.properties#L18-L22) and applied across modules:

- **`minSdkVersion`**: `21` (Android 5.0 Lollipop)
- **`targetSdkVersion`**: `28` (Android 9.0 Pie)
- **`compileSdkVersion`**: `36`
- **`ndkVersion`**: `29.0.14206865` (with fallback to `JITPACK_NDK_VERSION`)

### Gradle Tooling & Plugins
- **Root Buildscript**: Uses Android Gradle Plugin `com.android.tools.build:gradle:8.13.2` with repositories configured for Google Maven and Central (repo://build.gradle#L1-L16).
- **Maven Publishing**: Library modules (`terminal-emulator`, `terminal-view`, `termux-shared`) apply the `maven-publish` plugin to produce release publications with sources and javadoc jars.
- **Java Compatibility**: Library modules configure Java 8 source and target compatibility (`JavaVersion.VERSION_1_8`), and `termux-shared` enables core library desugaring (`coreLibraryDesugaringEnabled true`) with `com.android.tools:desugar_jdk_libs:1.1.5`.
