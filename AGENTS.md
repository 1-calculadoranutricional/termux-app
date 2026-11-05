# Protocolo Operativo para el Agente de Google AI Studio (AGENTS.md)

Este documento define tu manual operativo, de navegación, implementación y verificación técnica como **agente de desarrollo de software de Google AI Studio** en este repositorio. Cada directiva está mapeada estrictamente a tus herramientas nativas y al entorno de ejecución en la nube.

---

<!-- OPENWIKI:START -->
## FASE 1: Contextualización Conceptual Obligatoria (OpenWiki)

Antes de inspeccionar código fuente en profundidad o utilizar herramientas de búsqueda de texto, **DEBES** fundamentar tu comprensión conceptual de la arquitectura y los subsistemas en la base documental de `openwiki/`:

1. **Enrutamiento Conceptual con `view_file`**:
   - Al inicio de cualquier tarea que involucre arquitectura, módulos o flujos, ejecuta `view_file` sobre `/openwiki/quickstart.md` para identificar el dominio exacto.
   - El proyecto se divide en 4 módulos Gradle (`:app`, `:termux-shared`, `:terminal-view`, `:terminal-emulator`) y capas C/C++ nativas, estructurados en 3 áreas canónicas:
     - **Arquitectura (`openwiki/architecture/`)**:
       - `overview.md`: Topología de componentes, procesos, servicios (`TermuxService`) y fronteras de seguridad.
       - `modules-and-dependencies.md`: Estructura multi-módulo Gradle y resolución de dependencias.
       - `service-and-background-runtime.md`: Arquitectura de `TermuxService`, ciclo de foreground service, wakelocks y notificaciones persistentes.
       - `crash-handling-and-diagnostics.md`: Intercepción de excepciones (`CrashHandler`), telemetría forense y visor Markwon (`ReportActivity`).
       - `system-event-receivers-and-intents.md`: Ciclo de vida de broadcasts (`SystemEventReceiver`), actualizaciones de plugins, `termux-open` (`ContentProvider` con protección de dotfiles) y mitigaciones de *Phantom Process Killer* en Android 12+.
     - **Conceptos (`openwiki/concepts/`)**:
       - `terminal-emulation.md`: Motor DEC VT/ANSI, buffers de pantalla (`TerminalBuffer`, `TerminalRow`), gráficos Sixel y anchos Unicode (`WcWidth`).
       - `native-jni-and-pty.md`: Capa C/C++ nativa (`termux.c`), asignación de PTY master/slave (`/dev/ptmx`), termios `IUTF8` y seguridad de sockets Unix `SO_PEERCRED`.
       - `rendering-and-view.md`: Renderizado en `Canvas` (`TerminalView`, `TerminalRenderer`), gestos táctiles, selección de texto e inyección de contenido rico Gboard (`commitContent`).
       - `virtual-keyboard-and-extra-keys.md`: Barra virtual de teclas (`ExtraKeysView`), modificadores (`SpecialButton`), gestos y matrices de `termux.properties`.
       - `settings-and-preferences.md`: Arquitectura dual de configuración (`TermuxAppSharedPreferences` vs `termux.properties` dotfile) y recarga de estilos en caliente (`ACTION_RELOAD_STYLE`).
       - `ipc-and-socket-server.md`: Servidor socket `AF_UNIX` local (`AmSocketServer`), autenticación por `SO_PEERCRED` y despacho de intents `am` de alto rendimiento.
       - `shell-environment-and-execution.md`: Síntesis jerárquica de variables de entorno (`IShellEnvironment`, `TermuxShellEnvironment`), runner no interactivo `AppShell` y control de streams sin deadlock con `StreamGobbler`.
       - `filesystem-and-posix-dispatcher.md`: Abstracción POSIX nativa (`NativeDispatcher`, syscalls `stat`/`lstat`/`fstat`), metadatos `FileAttributes`, permisos y operaciones seguras (`FileUtils`).
     - **Flujos de Trabajo (`openwiki/workflows/`)**:
       - `session-lifecycle.md`: Ciclo de vida de terminal sessions, procesos PTY y vinculación con `TermuxService`.
       - `command-execution.md`: Ejecución externa, intents de `RunCommandService` y entorno shell.
       - `package-management-and-store.md`: Catálogo de la tienda (`TermuxStoreActivity`), inyección atómica Bracketed Paste (`TerminalEmulator.paste()`) y ejecución en sesión activa (`ACTION_EXECUTE_IN_CURRENT_SESSION`).
       - `storage-access-and-file-sharing.md`: Integración SAF (`TermuxDocumentsProvider`) y receptor de compartición (`FileReceiverActivity`).
       - `bootstrap-installation.md`: Extracción de paquetes bootstrap nativos (`libtermux-bootstrap.so`), staging atómico y symlinks de almacenamiento.

2. **Lectura Focalizada y Extracción de Símbolos Ancla**:
   - Utiliza `view_file` para leer **ÚNICAMENTE** la subpágina identificada desde `quickstart.md` aplicable a la tarea para no saturar tu ventana de contexto.
   - **Extrae Símbolos Ancla**: Identifica las clases e interfaces listadas en el bloque YAML `sources:` (ej. `TerminalEmulator`, `RunCommandService`, `TermuxSession`, `TermuxStoreActivity`, `TermuxService`, `JNI`, `NativeDispatcher`, `CrashHandler`). Estos símbolos son tus puntos de entrada obligatorios para la Fase 2.

3. **Invariante**:
   - No saltes directamente a lecturas masivas de código sin contextualización previa. Prohibido editar páginas de OpenWiki a menos que la tarea implique documentar o actualizar la arquitectura del proyecto.
<!-- OPENWIKI:END -->

---

<!-- CODEGRAPH:START -->
## FASE 2: Navegación Estructural e Inspección Relacional (CodeGraph)

CodeGraph constituye tu motor obligatorio de navegación estructural y análisis relacional de llamadas. **DEBES** ejecutarlo siempre en modo estrictamente no interactivo mediante `run_command`.

1. **Auto-recuperación e Inicialización Resiliente**:
   - Si `run_command` retorna error 127 (`codegraph: not found`), instálalo de inmediato en el contenedor:
     ```bash
     npm install -g @colbymchenry/codegraph
     ```
   - Si el comando reporta `CodeGraph not initialized`, inicialízalo de forma no interactiva:
     ```bash
     codegraph init -y --no-color
     ```

2. **Ejecución Estrictamente No Interactiva**:
   - En cada invocación a `run_command`, incluye siempre `--no-color`:
     - `codegraph explore --no-color "<símbolo>"`: Inspecciona código fuente y rutas de llamadas ascendentes/descendentes.
     - `codegraph node --no-color <símbolo>`: Consulta jerarquía de invocadores (*callers*), invocados (*callees*) y definiciones.
     - `codegraph query --no-color "<término>"`: Búsqueda rápida de símbolos indexados.
   - Para métodos JNI o nativos identificados en el grafo, inspecciona las fuentes C/C++ correspondientes bajo `app/src/main/cpp/`, `terminal-emulator/src/main/jni/` o `termux-shared/src/main/cpp/`.
   - Prohibido reemplazar CodeGraph por búsquedas ciegas de texto con `grep` cuando `.codegraph/` esté presente.

3. **Sincronización de Grafo Obligatoria**:
   - Ejecuta `codegraph sync --no-color` inmediatamente tras crear, modificar o eliminar archivos fuente para mantener el índice al día.
<!-- CODEGRAPH:END -->

---

## FASE 3: Implementación Quirúrgica con Herramientas Nativas

Una vez ubicado el punto exacto de intervención mediante las Fases 1 y 2, procede a implementar la solución utilizando las herramientas nativas del agente:

1. **Uso de Herramientas Nativas de Edición**:
   - **`replace_file_content` (Prioridad 1)**: Para sustituciones atómicas de un bloque contiguo de código. Evita reescribir código no afectado.
   - **`multi_replace_file_content`**: Para aplicar varias modificaciones dispersas y no contiguas en un mismo archivo de forma atómica.
   - **`write_to_file`**: Reservado exclusivamente para la creación de archivos nuevos o cuando la reescritura estructural completa del módulo sea indispensable.

2. **Regla de Lectura Previa (*Read-Before-Write*)**:
   - Utiliza `view_file` antes de editar si desconoces el estado exacto o la indentación del bloque objetivo.
   - Respeta estrictamente la indentación existente (espacios vs tabs) y el estándar del módulo (Java 8 / Kotlin).

3. **Prohibición Total de Código Simulado (*No Mocks*)**:
   - No implementes datos simulados estáticos, componentes ficticios ni botones sin listeners reales conectados al backend de la aplicación.
   - Todo flujo expuesto al usuario en la interfaz debe tener su lógica funcional y trazable con el sistema.

4. **Blindaje de Archivos Críticos**:
   - **NUNCA** modifiques `debug.keystore`, `debug.keystore.base64`, `testkey_untrusted.jks` ni las configuraciones de firma (`signingConfigs`, `signingConfig`) en los archivos `build.gradle`.
   - **NUNCA** almacenes claves de API o secretos en texto plano en el repositorio. Utiliza el panel de Secrets de AI Studio y `BuildConfig`.

---

## FASE 4: Quality Gate y Cierre de Integridad (*Definition of Done*)

Ninguna tarea se considera terminada hasta haber superado secuencialmente la siguiente compuerta de verificación:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    QUALITY GATE DE CIERRE DE INTEGRIDAD                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [PASO 1: COMPILACIÓN LIMPIA]      ──► compile_applet                       │
│            │                                                                │
│            ├── [Fallo] ──► Análisis de traza de compilador + Corrección     │
│            ▼                                                                │
│  [PASO 2: PRUEBAS UNITARIAS / JVM] ──► run_command("gradle :app:testDebugUnitTest")
│            │                                                                │
│            ├── [Fallo] ──► Depuración de aserciones + Corrección            │
│            ▼                                                                │
│  [PASO 3: SINCRONIZACIÓN DE GRAFO] ──► run_command("codegraph sync --no-color")
│            │                                                                │
│            ├── [Fallo] ──► codegraph init -y --no-color && sync             │
│            ▼                                                                │
│  [PASO 4: CONSISTENCIA OPENWIKI]   ──► Verificar .page-manifest.json y .claims/
│            │                                                                │
│            ▼                                                                │
│  [TAREA FINALIZADA EXITOSAMENTE (SUCCESS)]                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

1. **Paso 1 - Compilación Limpia (`compile_applet`)**:
   - Invoca la herramienta `compile_applet` para garantizar que no existan errores de sintaxis, tipos, referencias rotas o desajustes de recursos XML. Tienes un límite de 3 iteraciones de corrección ante cualquier fallo.
2. **Paso 2 - Pruebas Unitarias JVM (`run_command`)**:
   - Ejecuta `gradle :app:testDebugUnitTest`. No se permiten regresiones en las pruebas existentes de emulador, terminal view o providers.
3. **Paso 3 - Sincronización del Índice (`run_command`)**:
   - Ejecuta `codegraph sync --no-color` para dejar el repositorio completamente indexado para el siguiente turno.
4. **Paso 4 - Consistencia de OpenWiki**:
   - Si creaste o modificaste componentes documentados en OpenWiki, actualiza la página y los archivos correspondientes en `.page-manifest.json`, `.claims/` y `.last-update.json`.

---

## REGLAS CRÍTICAS DEL ENTORNO DE GOOGLE AI STUDIO

Para operar con máxima estabilidad dentro de este contenedor en la nube:

1. **Uso de Gradle en Terminal**:
   - **Usa SIEMPRE `gradle`**, NUNCA `./gradlew` ni `gradlew`. El Gradle Wrapper no está soportado en este contenedor y fallará.
   - **Evita `gradle clean`**: Ralentiza drásticamente las compilaciones posteriores al invalidar la caché; solo úsalo en caso de corrupción irrecuperable.
2. **Sin Emulador Local ni ADB**:
   - Este entorno **no dispone de emulador local ni de comando `adb`**.
   - No intentes ejecutar pruebas instrumentadas (`androidTest` / Espresso). Todas las pruebas deben ser pruebas unitarias JVM locales o Robolectric.
3. **Sincronización de Metadatos de Plataforma (`metadata.json`)**:
   - El nombre en `metadata.json` debe coincidir exactamente con el valor de `app_name` en `res/values/strings.xml`.
   - **NUNCA** elimines `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` del array `majorCapabilities` en `metadata.json`.
4. **Gestión de Secretos y API Keys**:
   - No crees ni utilices `local.properties`. Los secretos deben ser configurados en el panel de **Secrets de AI Studio** y accedidos en el código mediante `BuildConfig`.
