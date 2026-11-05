# Plan de Actualización de Aliases de Zsh en Termux Store

Actualización de los comandos de configuración generados para Zsh en la tienda de paquetes de Termux (`TermuxStoreActivity.java`), incorporando los aliases abreviados tanto en minúsculas como en mayúsculas (`x`, `X`, `c`, `C`) para las operaciones más frecuentes (`exit` y `clear`).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Aliases Solicitados e Integración**:
> Se actualizarán de forma idéntica y atómica tanto el comando de instalación inicial de Zsh (`installCommand`) como el de actualización (`updateCommand`) dentro de `TermuxStoreActivity.java`:
> ```bash
> alias x="exit"
> alias X="exit"
> alias c="clear"
> alias C="clear"
> ```
> Estos comandos se integran en la sección `# Aliases` que se inyecta en el archivo `~/.zshrc` del usuario al instalar o actualizar Zsh desde la Play Store de Termux.

---

## 1. Overview & Core Concept

### What It Does
Permite a los usuarios de Termux que instalen o actualicen Zsh a través de la tienda gráfica (`TermuxStoreActivity`) disponer inmediatamente de accesos directos ultrarrápidos para limpiar la terminal (`c` o `C`) y salir de la sesión (`x` o `X`) sin necesidad de teclear los comandos completos ni configurar manualmente su dotfile `~/.zshrc`.

### Target Audience
Usuarios de terminal en dispositivos móviles donde escribir palabras completas en teclados virtuales táctiles o reducidos resulta más lento y propenso a errores tipográficos.

---

## 2. Technical Architecture & Implementation Details

```
┌────────────────────────────────────────────────────────────────────────┐
│                   PIPELINE DE DESPACHO EN TERMUX STORE                 │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   TermuxStoreActivity.java                                             │
│   ├── Package Item: "zsh"                                              │
│   │     ├── installCommand: Generación de ~/.zshrc (HereDoc EOF)       │
│   │     │     ├── ZSH_HIGHLIGHT_STYLES...                              │
│   │     │     ├── [Aliases] ──► x="exit", X="exit", c="clear", C="clear"│
│   │     │     └── [Yazi integration & keybindings]                    │
│   │     │                                                              │
│   │     └── updateCommand: Regeneración de ~/.zshrc (HereDoc EOF)      │
│   │           ├── ZSH_HIGHLIGHT_STYLES...                              │
│   │           ├── [Aliases] ──► x="exit", X="exit", c="clear", C="clear"│
│   │           └── [Yazi integration & keybindings]                    │
│   │                                                                    │
│   ▼ Intent(ACTION_EXECUTE_IN_CURRENT_SESSION)                          │
│   TerminalSession (Bracketed Paste Mode & Ejecución interactiva)       │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### Puntos Específicos de Modificación (`TermuxStoreActivity.java`)

1. **Bloque 1 (`installCommand`, Líneas 720-724)**:
   - **Contenido actual:**
     ```java
     "# Aliases\n" +
     "# ------------------------------------------------------------------------------\n" +
     "alias X=\"exit\"\n" +
     "alias C=\"clear\"\n" +
     ```
   - **Nuevo contenido:**
     ```java
     "# Aliases\n" +
     "# ------------------------------------------------------------------------------\n" +
     "alias x=\"exit\"\n" +
     "alias X=\"exit\"\n" +
     "alias c=\"clear\"\n" +
     "alias C=\"clear\"\n" +
     ```

2. **Bloque 2 (`updateCommand`, Líneas 803-807)**:
   - **Contenido actual:**
     ```java
     "# Aliases\n" +
     "# ------------------------------------------------------------------------------\n" +
     "alias X=\"exit\"\n" +
     "alias C=\"clear\"\n" +
     ```
   - **Nuevo contenido:**
     ```java
     "# Aliases\n" +
     "# ------------------------------------------------------------------------------\n" +
     "alias x=\"exit\"\n" +
     "alias X=\"exit\"\n" +
     "alias c=\"clear\"\n" +
     "alias C=\"clear\"\n" +
     ```

---

## 3. Quality Gate y Verificación

1. **Edición Atómica**: Utilizar `replace_file_content` para sustituir cada bloque contiguo en `TermuxStoreActivity.java`.
2. **Compilación Limpia**: Ejecutar `compile_applet` para verificar que la sintaxis Java y las cadenas multilínea concatenadas compilan sin errores.
3. **Pruebas Unitarias JVM**: Ejecutar `gradle :app:testDebugUnitTest`.
4. **Sincronización de Grafo**: Ejecutar `codegraph sync --no-color`.
