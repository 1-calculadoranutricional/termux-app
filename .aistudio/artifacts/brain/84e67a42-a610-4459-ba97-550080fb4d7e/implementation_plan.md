# OpenWiki Technical Audit & Code Synchronization Plan

Auditoría técnica exhaustiva y sincronización completa de la documentación interna de OpenWiki con la implementación real del código fuente de Termux en sus 4 módulos (`:app`, `:termux-shared`, `:terminal-view`, `:terminal-emulator`), resolviendo inconsistencias y añadiendo cobertura para subsistemas no documentados.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario en Fase 1:**
> - **Idioma de la documentación**: Inglés técnico (`en-US`), manteniendo coherencia estilística, etiquetas y enlaces canónicos con el resto de la wiki del repositorio.
> - **Alcance de la auditoría**: Cobertura integral de todos los módulos y subsistemas (`:app`, `:termux-shared`, `:terminal-view`, `:terminal-emulator`).
> - **Nuevos componentes**: Creación de nuevas páginas canónicas de OpenWiki para subsistemas clave que hoy carecen de cobertura (`TermuxDocumentsProvider`, `FileReceiverActivity`, `CrashHandler` y diagnósticos).

---

### 1. Overview & Core Concept

- **Qué hace**: Realiza una actualización rigurosa de las especificaciones técnicas en OpenWiki contrastando cada afirmación contra el código fuente real (Java, C JNI, AndroidManifest, Gradle).
- **Problema que resuelve**: Elimina la deuda documental provocada por evoluciones recientes del código:
  1. La documentación del **Termux Store** describe estructuras de datos (`StorePackageItem`), comandos de acción y un flujo obsoleto de generación de scripts en disco (`~/setup_zsh.sh`), cuando en realidad el código ejecuta los comandos directamente en la sesión activa (`ACTION_EXECUTE_IN_CURRENT_SESSION`) y elimina cualquier script residual.
  2. Falta documentación sobre la resolución y guardado de contenidos enriquecidos (imágenes y capturas de Gboard) en `TermuxTerminalViewClient`.
  3. Ausencia total de documentación sobre el proveedor del Storage Access Framework (`TermuxDocumentsProvider`), recepción de archivos (`FileReceiverActivity`) y el subsistema de reporte de fallos (`CrashHandler` / `ReportActivity`).
- **Valor aportado**: Garantiza que desarrolladores, subagentes y herramientas como CodeGraph cuenten con una fuente de verdad 100% verídica, con referencias exactas a líneas de código (`repo://...`).

---

### 2. Information Architecture & Wiki Navigation

Ajustes y nuevas páginas dentro de la jerarquía de `openwiki/`:

```
openwiki/
├── quickstart.md                                (ACTUALIZAR: añadir enlaces y rutas de nuevas páginas)
├── index.md                                     (ACTUALIZAR: listar índices de sección)
├── architecture/
│   ├── index.md                                 (ACTUALIZAR: incluir crash handling & diagnostics)
│   ├── overview.md                              (ACTUALIZAR: corregir TermuxStoreActivity y SAF)
│   ├── modules-and-dependencies.md              (VERIFICAR / REVISAR dependencias y plugins)
│   └── crash-handling-and-diagnostics.md        (NUEVO: TermuxCrashUtils, CrashHandler, ReportActivity)
├── concepts/
│   ├── index.md                                 (ACTUALIZAR)
│   ├── rendering-and-view.md                    (ACTUALIZAR: documentar resolución de imágenes de portapapeles)
│   └── terminal-emulation.md                    (AUDITAR: buffers, sixel, UTF-8 y JNI)
└── workflows/
    ├── index.md                                 (ACTUALIZAR)
    ├── package-management-and-store.md          (ACTUALIZAR: corrección completa de campos, categorías y sin scripts temporales)
    ├── storage-access-and-file-sharing.md       (NUEVO: TermuxDocumentsProvider, SAF, FileReceiverActivity)
    ├── session-lifecycle.md                     (AUDITAR: ciclo de vida de sesiones y pty)
    └── command-execution.md                     (AUDITAR: RunCommandService, AmSocketServer)
```

---

### 3. Key Technical Findings & Specific Adjustments

#### 1. Termux Store (`package-management-and-store.md` y `overview.md`)
- **Discrepancia en Modelo de Datos**: La wiki afirma que `StorePackageItem` posee `name`, `category`, `iconRes`, `binaryPath`, `launchCommand`, `isInstalled`.
  - *Realidad*: Posee `id`, `title`, `categoryId`, `category`, `description`, `iconResId`, `installCommand`, `uninstallCommand`, `updateCommand`, `binaryCheckPath` (soporta rutas separadas por `:`), `rating`, `size`, `initGuide`, `helpGuide`, `quickTip` y enum `ActionType`.
- **Discrepancia en Flujo de Ejecución**: La wiki describe que Zsh genera un script `~/setup_zsh.sh` en disco.
  - *Realidad*: La generación de scripts temporales fue eliminada. `TermuxStoreActivity.executeShellCommandInTerminal()` envía todos los comandos directamente mediante `TermuxActivity.ACTION_EXECUTE_IN_CURRENT_SESSION` con `EXTRA_COMMAND`, y elimina de forma defensiva scripts residuales si existieran.
- **Categorías Reales**: Las categorías del catálogo son `"system"`, `"tools"`, `"dev"`, `"google"` y la nueva categoría `"ia"` (Artificial Intelligence & CLI: OpenWiki, CodeGraph, Engram).
- **Diálogo Interactivo de Guías**: Soporte de diálogo con tabs/secciones de inicialización, ayuda y tip para principiantes, más copia de comandos al portapapeles.

#### 2. Terminal View & Portapapeles Gboard (`rendering-and-view.md`)
- Documentar el flujo end-to-end de `InputConnectionCompat.OnCommitContentListener` en `TerminalView` conectado con `TermuxTerminalViewClient`:
  - Recepción de imágenes (PNG, JPEG, WebP, GIF) desde el portapapeles de Gboard.
  - Consulta del ContentResolver para identificar la ruta en disco real (`MediaStore.MediaColumns.DATA`).
  - Mapeo a rutas canónicas de Termux (`~/storage/dcim`, `~/storage/pictures`, `~/storage/downloads`, `~/storage/shared`).
  - Si el flujo es un stream sin archivo local, guardado automático en `~/storage/dcim/Screenshots/` (o fallback en `~/clipboard/`) y escritura de la ruta absoluta entrecomillada directamente en la terminal activa.

#### 3. Nuevo Componente: Storage Access Framework & File Sharing (`storage-access-and-file-sharing.md`)
- **`TermuxDocumentsProvider`**:
  - Extensión de `DocumentsProvider` para exponer el `$HOME` (`/data/data/com.termux/files/home`) a través del Storage Access Framework (SAF) de Android.
  - Métodos implementados: `queryRoots`, `queryDocument`, `queryChildDocuments`, `openDocument`, `createDocument`, `deleteDocument`, soporte de miniaturas/iconos.
- **`FileReceiverActivity`**:
  - Recepción de intents externos (`ACTION_SEND`, `ACTION_SEND_MULTIPLE`).
  - Guardado en `~/downloads` o ejecución interactiva vía `termux-file-editor` y `termux-url-opener`.

#### 4. Nuevo Componente: Crash Handling & Diagnostics (`crash-handling-and-diagnostics.md`)
- **`TermuxCrashUtils` & `CrashHandler`**:
  - Captura global de excepciones no controladas a nivel de proceso (`Thread.setDefaultUncaughtExceptionHandler`).
  - Generación de reportes forenses con stack trace, información del dispositivo, versión de Android, arquitectura de CPU, paquetes instalados y memoria.
- **`ReportActivity` & `TextIOActivity`**:
  - Interfaz de usuario para inspeccionar y compartir volcados de depuración y crash logs de forma segura.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Termux Android App (:app)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  Activities:                                                                │
│  ┌───────────────────────┐  ┌───────────────────────┐  ┌──────────────────┐ │
│  │    TermuxActivity     │  │  TermuxStoreActivity  │  │ SettingsActivity │ │
│  │ (TerminalView Client) │  │  (Curated Catalog)    │  │ (Preferences UI) │ │
│  └───────────┬───────────┘  └───────────┬───────────┘  └──────────────────┘ │
│              │                          │                                   │
│              │   ACTION_EXECUTE_...     │                                   │
│              │◄─────────────────────────┘                                   │
│              │ (Direct Session Execution, no .sh scripts)                    │
│  ┌───────────▼───────────┐  ┌───────────────────────┐  ┌──────────────────┐ │
│  │     TermuxService     │  │ TermuxDocumentsProv.  │  │  FileReceiverAct │ │
│  │ (Foreground Sessions) │  │ (SAF Provider $HOME)  │  │ (External Share) │ │
│  └───────────┬───────────┘  └───────────────────────┘  └──────────────────┘ │
└──────────────┼──────────────────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────────────────┐
│                    Terminal Subsystems (:view, :emulator)                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  TerminalView:                                                              │
│  - InputConnectionCompat: Gboard commitContent (Images -> Termux Storage)   │
│  - IME Active Word Buffer: context tracking & solitary newline detection    │
│  - Bracketed Paste: DECSET 2004 for multiline clipboard streams             │
│                                                                             │
│  TerminalEmulator:                                                          │
│  - ANSI/VT parsing, Sixel bitmap rendering, TerminalBuffer & Rows           │
│  - JNI / PTY POSIX process management                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 5. Execution Steps

1. **Actualizar `openwiki/workflows/package-management-and-store.md`**: Corregir discrepancias de campos de `StorePackageItem`, flujo de ejecución directa sin scripts temporales y categorías del catálogo.
2. **Actualizar `openwiki/architecture/overview.md`**: Actualizar la descripción de `TermuxStoreActivity` y agregar referencias a SAF y diagnósticos.
3. **Actualizar `openwiki/concepts/rendering-and-view.md`**: Documentar en detalle el flujo de resolución de medios/imágenes de `InputConnectionCompat` en `TermuxTerminalViewClient`.
4. **Crear `openwiki/workflows/storage-access-and-file-sharing.md`**: Documentar `TermuxDocumentsProvider`, SAF y `FileReceiverActivity`.
5. **Crear `openwiki/architecture/crash-handling-and-diagnostics.md`**: Documentar `TermuxCrashUtils`, `CrashHandler`, `ReportActivity` y `TextIOActivity`.
6. **Actualizar índices y mapa de navegación**: Sincronizar `quickstart.md`, `openwiki/index.md`, `architecture/index.md`, `workflows/index.md` y `.page-manifest.json`.
7. **Verificación y Reporte**: Ejecutar validación de enlaces y generar el reporte final detallado para el usuario.
