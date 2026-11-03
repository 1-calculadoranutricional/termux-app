# Corrección de Autocorrector de Gboard en Termux sin Duplicación

Resolver el conflicto del autocorrector de Gboard al escribir desde el teclado táctil y presionar la barra espaciadora en Termux, eliminando la duplicación de palabras (ej. `"HlaHola Hola"`) mediante la sincronización del búfer de la palabra en `InputConnection` y la emisión precisa de caracteres de retroceso (`Backspace`) para reemplazar limpiamente la palabra antes de insertar la corrección.

### Decisiones Críticas y Preferencias Confirmadas

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> - **Comportamiento del autocorrector al pulsar espacio**: Reemplazar automáticamente la palabra mal escrita por la corrección sugerida por el teclado.
> - **Gestión de borrado**: Sincronizar el búfer de la palabra activa en `InputConnection` para borrar la palabra anterior enviando los códigos de retroceso (`\u007F`) exactos a la sesión de terminal antes de emitir el texto corregido.
> - **Preservación de funcionalidades previas**: Mantener intacto el soporte de pegado de imágenes/capturas desde el portapapeles de Gboard, el pegado de texto y el dictado por voz (micrófono).

---

### 1. Visión General y Concepto Central

- **Problema actual**: Con la configuración de `EditorInfo.inputType` enriquecida (`TYPE_CLASS_TEXT`), Gboard expone la barra completa de portapapeles, capturas de pantalla, micrófono y sugerencias de texto. Sin embargo, al escribir con el teclado e incurrir en una corrección al pulsar espacio:
  1. `commitCorrection(CorrectionInfo)` interceptaba la sugerencia e insertaba el texto nuevo sin coordinar con el evento posterior del IME.
  2. El método `commitText` de `TerminalView` borraba inmediatamente su `Editable` (`content.clear()`), provocando que `getTextBeforeCursor()` devolviera una cadena vacía.
  3. Gboard perdía la noción del texto presente antes del cursor e inyectaba la corrección y el espacio como texto adicional, generando cadenas anómalas como `"HlaHola Hola"`.
- **Solución propuesta**:
  1. Mantener un búfer sincronizado de la línea o palabra activa (`mCurrentWordBuffer`) dentro del `InputConnection` que responda adecuadamente a `getTextBeforeCursor` y `getSelectedText`.
  2. Desacoplar la inyección duplicada en `commitCorrection`: tratar `commitCorrection` de forma estándar o sincronizada para que no compita con `commitText` o `setComposingText`.
  3. Al recibir una sustitución de palabra o región en composición (autocorrección por espacio o sugerencia), calcular la longitud real de la palabra mal escrita, enviar exactamente esa cantidad de retrocesos (`sendBackspace(n)`) a la terminal PTY e insertar la palabra corregida junto al espacio.

---

### 2. Experiencia de Usuario y Flujo de Interacción

```
  ┌──────────────────────────────────────────────────────────────┐
  │ 1. Usuario escribe "Hla" en el teclado táctil                │
  │    - Se muestra "Hla" en la línea de comandos de Termux      │
  │    - El búfer interno registra "Hla" como palabra activa     │
  │    - Gboard resalta la sugerencia "Hola" en la barra         │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 2. Usuario presiona la barra "Espacio"                       │
  │    - Gboard activa la autocorrección automática              │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 3. TerminalView sincroniza la sustitución:                   │
  │    - Identifica la palabra previa ("Hla", longitud = 3)      │
  │    - Emite 3 códigos de retroceso \u007F al PTY de Termux    │
  │    - Emite la palabra corregida "Hola " a la terminal        │
  │    - Actualiza el búfer interno a "Hola "                    │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 4. Resultado visible en Termux:                              │
  │    $ Hola                                                    │
  │    (Sin duplicación, sin saltos extraños, cursor al final)   │
  └──────────────────────────────────────────────────────────────┘
```

---

### 3. Decisiones Técnicas y Compensaciones (Trade-Offs)

- **Eliminación de inyección redundante en `commitCorrection`**:
  - *Enfoque*: En el framework de Android IME, `commitCorrection` es una notificación pasiva del teclado hacia el editor para resaltar sugerencias; la sustitución real de caracteres siempre viaja por `setComposingRegion` / `deleteSurroundingText` / `commitText`. Al suprimir la doble escritura en `commitCorrection`, evitamos que `"Hola"` se envíe dos veces.
- **Búfer de palabra activa en `InputConnection`**:
  - *Enfoque*: Implementar un `StringBuilder` o seguimiento de palabra en curso en la conexión de entrada. Cuando el IME solicita `getTextBeforeCursor(n, flags)`, devolvemos el contenido real del búfer en lugar de una cadena vacía.
  - *Beneficio*: Gboard detecta con precisión los límites del texto y puede invocar `deleteSurroundingText` o `setComposingRegion` con los índices correctos.
- **Preservación del soporte de imágenes y voz**:
  - *Enfoque*: No alterar los tipos MIME declarados en `outAttrs.contentMimeTypes` ni el wrapper de `InputConnectionCompat.createWrapper()`, asegurando que los chips de capturas y el botón del micrófono sigan operativos al 100%.

---

### 4. Arquitectura Técnica y Estrategia de Datos

```
┌──────────────────────────────────────────────────────────────────────────┐
│ Gboard (IME)                                                             │
│   ├── Suggestions & Autocorrect Engine                                   │
│   ├── Queries: getTextBeforeCursor()                                     │
│   └── Actions: deleteSurroundingText() / setComposingRegion() / commitText│
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ TerminalView.java (InputConnection)                                      │
│   ├── mActiveWordBuffer (Buffer de texto sincronizado antes del cursor)  │
│   ├── getTextBeforeCursor(n, flags) -> Retorna buffer de palabra activa  │
│   ├── deleteSurroundingText(before, after)                               │
│   │     └── sendBackspace(before) + actualiza mActiveWordBuffer          │
│   ├── commitText(text, newCursorPos)                                     │
│   │     ├── Si mComposingRegionLength > 0 -> sendBackspace(length)       │
│   │     ├── sendTextToTerminal(text)                                     │
│   │     └── actualiza mActiveWordBuffer con delimitadores (espacio/enter)│
│   └── commitCorrection(correctionInfo)                                   │
│         └── Sincronización pasiva (no duplica sendTextToTerminal)        │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ TerminalSession / TerminalEmulator (PTY)                                 │
│   ├── Recibe exactamente: \u007F\u007F\u007F ("Hla" borrado)             │
│   └── Recibe: "Hola " (Texto corregido)                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

#### Plan de Modificaciones

1. **`terminal-view/src/main/java/com/termux/view/TerminalView.java`**:
   - Ajustar el `InputConnection` creado en `onCreateInputConnection`:
     - Incorporar `mActiveWordBuffer` para registrar los caracteres que se van escribiendo o pegando en la línea actual.
     - Implementar `getTextBeforeCursor(int n, int flags)` para retornar el final de `mActiveWordBuffer`, permitiendo a Gboard conocer la palabra actual.
     - En `deleteSurroundingText(int leftLength, int rightLength)`: recortar `mActiveWordBuffer` en `leftLength` caracteres y enviar `sendBackspace(leftLength)`.
     - En `commitCorrection(CorrectionInfo correctionInfo)`: evitar la escritura manual redundante si la corrección ya está siendo gestionada por el flujo de composición/commit del IME.
     - En `commitText(CharSequence text, int newCursorPosition)`: verificar si el reemplazo sustituye la palabra previa; si es así, retroceder la longitud correspondiente y registrar el nuevo texto en el búfer. Si el carácter es un delimitador (espacio o salto de línea), resetear el búfer de la palabra activa.
     - En `sendKeyEvent`: al detectar `KEYCODE_ENTER` o `KEYCODE_DEL`, actualizar en sincronía el búfer local.

2. **Verificación y Compilación**:
   - Compilar el proyecto completo usando `compile_applet` para garantizar que la compilación de todos los módulos (`:terminal-view`, `:terminal-emulator`, `:termux-shared`, `:app`) sea exitosa.
