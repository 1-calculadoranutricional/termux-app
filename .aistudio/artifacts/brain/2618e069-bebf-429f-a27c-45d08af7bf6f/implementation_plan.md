# Plan de Auditoría Integral y Sincronización Documental de OpenWiki

## 1. Diagnóstico de Cobertura y Brechas del Repositorio

Tras contrastar el árbol de fuentes (`:app`, `:termux-shared`, `:terminal-view`, `:terminal-emulator`, código C/C++ nativo) contra los archivos en `openwiki/`, se han identificado tres subsistemas nucleares con **cobertura cero (0%)**:

1. **Capa Nativa JNI y Subsistema de Pseudoterminal (PTY)**:
   - Fuentes: `terminal-emulator/src/main/jni/termux.c`, `terminal-emulator/src/main/java/com/termux/terminal/JNI.java`, `termux-shared/src/main/cpp/local-socket.cpp`, `app/src/main/cpp/termux-bootstrap.c`.
   - Estado: Sin documento en OpenWiki. Carece de documentación sobre `createSubprocess` (forkpty/openpty, ioctl `TIOCSCTTY`, `setPtyWindowSize`, `setPtyUtf8Mode`, `waitFor`), socket Unix local (`SO_PEERCRED`) y extracción bootstrap nativa.
2. **Servicio Central en Segundo Plano, Wakelocks y Ciclo de Vida (`TermuxService`)**:
   - Fuentes: `app/src/main/java/com/termux/app/TermuxService.java`, `app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionServiceClient.java`, `termux-shared/src/main/java/com/termux/shared/termux/notification/TermuxNotificationUtils.java`.
   - Estado: Mencionado tangencialmente pero sin página dedicada. Ocurre omisión del ciclo de vida de foreground service, acciones de notificación ("Exit", "Acquire/Release Wakelock"), gestión de Wi-Fi lock y despacho multi-cliente de terminal sessions.
3. **Arquitectura Dual de Preferencias y Configuración**:
   - Fuentes: `app/src/main/java/com/termux/app/activities/SettingsActivity.java`, `termux-shared/src/main/java/com/termux/shared/termux/settings/preferences/TermuxAppSharedPreferences.java`, `termux-shared/src/main/java/com/termux/shared/termux/settings/properties/TermuxAppSharedProperties.java`, fragmentos de configuración de `:app`.
   - Estado: Cobertura 0%. No documenta la coexistencia entre las `SharedPreferences` de Android (estilos de UI, fuentes, plugins API/Float/Tasker/Widget) y las propiedades en disco `~/.termux/termux.properties` (recarga en caliente vía broadcast, matrices de teclas y shortcuts).

Asimismo, existen **desviaciones técnicas en documentos existentes**:
- `openwiki/workflows/package-management-and-store.md`: Requiere incorporar el pipeline de inyección atómica con Bracketed Paste (`TerminalEmulator.paste()`), secuencias `\033[200~` y `\033[201~`, encadenamiento lógico `&&` y la activación directa con `exec zsh`.
- `openwiki/concepts/rendering-and-view.md`: Debe documentar la interceptación de contenido multimedia enriquecido del portapapeles de Gboard vía `InputContentInfoCompat` (`onCommitContent`).

---

## 2. Acciones del Plan de Implementación

### Fase 1: Creación de Documentos Nuevos (Estándar OpenWiki)
1. **`openwiki/concepts/native-jni-and-pty.md`**:
   - Frontmatter YAML canónico (`type: concept`, `title: Native JNI & Pseudoterminal (PTY) Subsystem`, `sources:`, `verified:`).
   - Diagrama de flujo de inicialización del proceso PTY (Java `TerminalSession` -> JNI -> `termux.c` -> `openpty`/`forkpty` -> `/system/bin/sh` / shell login).
   - Tabla técnica de firmas JNI y descriptores de archivo.
   - Seguridad de sockets Unix con `getPeerCred` (`SO_PEERCRED`).
2. **`openwiki/architecture/service-and-background-runtime.md`**:
   - Frontmatter YAML canónico (`type: architecture`, `title: TermuxService & Background Runtime Architecture`).
   - Diagrama de secuencia de ciclo de vida del servicio (`onCreate`, `onStartCommand`, vinculación de clientes, wakelocks y notificación persistente).
   - Tabla de intents y broadcast receivers gestionados por el servicio.
3. **`openwiki/concepts/settings-and-preferences.md`**:
   - Frontmatter YAML canónico (`type: concept`, `title: Settings & Properties Architecture`).
   - Comparativa y tabla de separación de responsabilidades: `TermuxAppSharedPreferences` vs `TermuxAppSharedProperties`.
   - Flujo de recarga dinámica de propiedades (`ACTION_RELOAD_STYLE`) y jerarquía de fragmentos de configuración.

### Fase 2: Actualización de Documentos Existentes
1. **`openwiki/workflows/package-management-and-store.md`**:
   - Consolidar la sección del pipeline de inyección atómica reflejando el uso de `TerminalEmulator.paste()` con Bracketed Paste (DECSET 2004) y terminación en retorno de carro (`\r`).
2. **`openwiki/concepts/rendering-and-view.md`**:
   - Agregar sección de integración con IMEs y portapapeles (`onCommitContent`), resolución de rutas de almacenamiento y pegado directo en el stream PTY.

### Fase 3: Sincronización de Índices de Navegación
1. **`openwiki/quickstart.md`**:
   - Integrar los 3 nuevos documentos en las tablas de arquitectura, conceptos y flujos recomendados.
2. **`openwiki/architecture/index.md`**:
   - Agregar `service-and-background-runtime.md`.
3. **`openwiki/concepts/index.md`**:
   - Agregar `native-jni-and-pty.md` y `settings-and-preferences.md`.

### Fase 4: Generación y Actualización de Claims y Manifiesto
1. **Creación/Actualización de Claims en `openwiki/.claims/`**:
   - `.claims/concepts/native-jni-and-pty.json`
   - `.claims/architecture/service-and-background-runtime.json`
   - `.claims/concepts/settings-and-preferences.json`
   - Actualizar `.claims/workflows/package-management-and-store.json` y `.claims/concepts/rendering-and-view.json`.
2. **Actualización de `openwiki/.page-manifest.json`**:
   - Registrar las 3 nuevas páginas con su hash `pageVersion`, fuente y metadatos de auditoría.
3. **Actualización de `openwiki/.last-update.json`**:
   - Actualizar fecha y estado de sincronización global.

### Fase 5: Quality Gate de Integridad
1. **Compilación Limpia**:
   - Validar con `compile_applet`.
2. **Pruebas Unitarias JVM**:
   - Ejecutar `gradle :app:testDebugUnitTest`.
3. **Sincronización de Grafo**:
   - Ejecutar `codegraph sync --no-color`.
4. **Verificación de Consistencia de OpenWiki**:
   - Comprobar que no existan enlaces rotos ni claims huérfanos.
