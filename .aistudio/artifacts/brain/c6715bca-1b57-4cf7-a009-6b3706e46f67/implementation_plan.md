# Soporte de Bracketed Paste para el Portapapeles de Gboard en Termux

Resolver la ejecución automática y la corrupción de comandos multilínea al pegar bloques de texto desde el portapapeles de Gboard en Termux, canalizando el pegado con saltos de línea a través del mecanismo de *Bracketed Paste Mode* (`mEmulator.paste()`) e insertando texto de una sola línea como texto plano sin forzar la ejecución.

### Decisiones Críticas y Preferencias Confirmadas

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> - **Pegado de comandos y texto multilínea**: Usar *Bracketed Paste Mode* (`\033[200~ ... \033[201~` mediante `mEmulator.paste()`) para evitar la ejecución automática inmediata de cada línea y evitar la repetición del prompt secundario (`cmdand>`).
> - **Texto de una sola línea**: Insertarse como texto plano en la posición actual del cursor sin enviar retorno de carro (`\r`), permitiendo al usuario revisar o completar el comando antes de presionar Enter manualmente.
> - **Preservación de funcionalidades**: Mantener intacto el pegado de imágenes/capturas de pantalla, la autocorrección sin duplicación en la barra espaciadora y el dictado por voz (micrófono).

---

### 1. Visión General y Concepto Central

- **Causa raíz del error**:
  1. Al presionar "Paste" en el menú contextual de Termux (mantener pulsada la pantalla), Termux invoca `mEmulator.paste(text)`, el cual activa el protocolo terminal estándar de *Bracketed Paste* (`\033[200~` y `\033[201~`). La shell (bash, zsh, fish) reconoce el bloque como texto pegado: no ejecuta los saltos de línea y no duplica los prompts.
  2. En cambio, al pulsar un chip de texto en el portapapeles de Gboard, el teclado envía todo el contenido a través de `InputConnection.commitText(text, 1)`.
  3. Previamente, `commitText` iteraba carácter por carácter en `sendTextToTerminal`, traduciendo cada salto de línea `\n` en un retorno de carro `\r` (tecla Enter). Esto provocaba que la shell interpretara cada línea como un comando individual ejecutado inmediatamente con máxima velocidad, rompiendo la estructura de comandos encadenados con `&& \` y generando prompts secundarios anidados (`cmdand> cmdand>`).

- **Solución propuesta**:
  1. En `TerminalView.java` (`InputConnection.commitText`): detectar cuando el texto recibido contiene saltos de línea (`\n` o `\r`) o proviene de un bloque de portapapeles.
  2. Para textos multilínea, canalizar la inserción directamente mediante `mEmulator.paste(text.toString())`, igual que lo hace el menú contextual "Paste".
  3. Para textos de una sola línea sin saltos de línea, insertarlos en el terminal como texto plano en el cursor sin presionar Enter, actualizando el búfer local.

---

### 2. Experiencia de Usuario y Flujo de Interacción

```
  ┌──────────────────────────────────────────────────────────────┐
  │ 1. Usuario copia bloque multilínea:                          │
  │    mkdir -p ~/.termux && \                                   │
  │    curl -L -o ...                                            │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 2. Usuario toca el chip de texto en el portapapeles de Gboard│
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 3. TerminalView detecta contenido multilínea o de portapapeles│
  │    - Si contiene saltos de línea (\n o \r):                   │
  │        Invoca mEmulator.paste(text) con bracketed paste      │
  │    - Si es una sola línea:                                   │
  │        Inserta texto plano en el cursor sin \r               │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 4. Resultado en pantalla de Termux:                          │
  │    mkdir -p ~/.termux && \                                   │
  │    curl -L -o ~/.termux/Iosevka.zip ... && \                 │
  │    unzip -o ~/.termux/Iosevka.zip -d ~/.termux/ && \         │
  │    cp ...                                                    │
  │    (Limpio, sin ejecutarse solo, listo para pulsar Enter)    │
  └──────────────────────────────────────────────────────────────┘
```

---

### 3. Decisiones Técnicas y Compensaciones (Trade-Offs)

- **Uso directo de `mEmulator.paste(String text)`**:
  - *Ventaja*: Es el método canónico y testeado en Termux para el pegado seguro desde el portapapeles. Limpia caracteres de escape peligrosos C1 (`0x80-0x9F`), unifica retornos de línea y emite los marcadores DECSET 2004 (`\033[200~` y `\033[201~`) reconocidos por bash/zsh/readline.
  - *Comportamiento*: Idéntico al menú táctil nativo de Termux.
- **Detección de texto multilínea vs texto continuo**:
  - *Ventaja*: El tipeo normal y el dictado de palabras individuales siguen su flujo habitual a través de `sendTextToTerminal` y `mActiveWordBuffer`, mientras que cualquier inserción con saltos de línea pasa por el mecanismo de pegado seguro.

---

### 4. Arquitectura Técnica y Estrategia de Datos

```
┌──────────────────────────────────────────────────────────────────────────┐
│ Gboard (Portapapeles de texto)                                           │
│   └── commitText(text, 1)                                                │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ TerminalView.java (commitText)                                           │
│   ├── ¿Contiene saltos de línea (\n o \r)? o ¿Bloque largo de pegado?   │
│   │     ├── SÍ -> mEmulator.paste(text.toString())                       │
│   │     │         └── Emite \033[200~ + texto + \033[201~                │
│   │     └── NO -> Inserción regular sin Enter (\r)                       │
│   │               └── sendTextToTerminal(text) + mActiveWordBuffer       │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ TerminalSession / TerminalEmulator                                       │
│   └── Shell recibe bloque seguro sin disparar ejecución prematura        │
└──────────────────────────────────────────────────────────────────────────┘
```

#### Plan de Modificaciones

1. **`terminal-view/src/main/java/com/termux/view/TerminalView.java`**:
   - En `InputConnection.commitText(CharSequence text, int newCursorPosition)`:
     - Detectar si `text` contiene saltos de línea (`text.toString().indexOf('\n') >= 0 || text.toString().indexOf('\r') >= 0`).
     - Si contiene saltos de línea y el emulador está activo, invocar `mEmulator.paste(text.toString())` y resetear el búfer de palabra activa (`mActiveWordBuffer.setLength(0)`).
     - Si es texto de una sola línea, mantener el envío estándar sin ejecutar, actualizando el búfer local.
     - Gestionar adecuadamente cualquier región en composición previa (`mComposingRegionLength`).

2. **Verificación y Compilación**:
   - Validar con `compile_applet`.
   - Ejecutar pruebas unitarias para certificar la no regresión.
