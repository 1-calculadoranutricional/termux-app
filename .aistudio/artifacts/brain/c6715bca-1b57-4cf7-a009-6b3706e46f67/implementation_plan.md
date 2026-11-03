# Corrección de Ejecución con la Tecla Enter vs Pegado Multilínea en Termux

Permitir que la tecla Enter del teclado virtual (Gboard) ejecute los comandos inmediatamente enviando un retorno de carro (`\r`), diferenciando las pulsaciones de Enter aisladas (`\n`) de los bloques multilínea pegados desde el portapapeles (los cuales conservan el *Bracketed Paste Mode* para no autoejecutarse ni romper el prompt).

### Decisiones Críticas y Preferencias Confirmadas

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> - **Comportamiento de la tecla Enter**: Al presionar la tecla Enter en el teclado en pantalla, enviar un retorno de carro (`\r`) para ejecutar de inmediato el comando escrito (por ejemplo, `ls` + Enter).
> - **Distinción entre Enter y bloque pegado**: Si el texto recibido es exclusivamente un salto de línea (`\n`, `\r` o `\r\n`), se trata como la tecla Enter y se ejecuta el comando; si contiene texto además de saltos de línea (un bloque multilínea de comandos o código), se aplica *Bracketed Paste* (`mEmulator.paste()`).
> - **Preservación total**: Mantener intacto el pegado de imágenes/capturas desde el portapapeles, la autocorrección sin duplicación en la barra espaciadora y el dictado por voz (micrófono).

---

### 1. Visión General y Concepto Central

- **Causa del problema**:
  En teclados como Gboard configurados para campos de texto con soporte de portapapeles enriquecido, la tecla Enter emite `commitText("\n", 1)`. Al verificar genéricamente si el texto contenía `\n`, el salto de línea solitario de la tecla Enter se derivaba a `mEmulator.paste("\n")` dentro de secuencias de bracketed paste (`\033[200~\r\033[201~`). En este modo de pegado protegido, la shell (bash/zsh) interpreta el carácter como una nueva línea visual en la línea de edición en lugar de ordenar la ejecución del comando.

- **Solución propuesta**:
  1. En `commitText`: evaluar si el texto recibido es exclusivamente un salto de línea (`"\n"`, `"\r"`, `"\r\n"`).
  2. Si es un salto de línea solitario (tecla Enter):
     - Limpiar el búfer de la palabra activa (`mActiveWordBuffer.setLength(0)`).
     - Enviar el retorno de carro (`\r`) a la sesión activa mediante `sendTextToTerminal("\r")` para ejecutar el comando.
  3. Si el texto contiene saltos de línea y además contiene texto de comandos (bloque multilínea pegado):
     - Mantener la canalización con `mEmulator.paste(str)` para no autoejecutar el bloque copiado ni romper los prompts `cmdand>`.

---

### 2. Experiencia de Usuario y Flujo de Interacción

```
  ┌──────────────────────────────────────────────────────────────┐
  │ 1. Usuario escribe "ls" y presiona la tecla Enter en Gboard  │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 2. TerminalView recibe commitText("\n")                      │
  │    - Detecta que es un salto solitario (tecla Enter)         │
  │    - Emite retorno de carro estándar (\r) a la terminal      │
  │    - Limpia mActiveWordBuffer                                │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 3. Resultado en pantalla:                                    │
  │    $ ls                                                      │
  │    archivo1  archivo2  directorio/                           │
  │    $ █                                                       │
  │    (Comando ejecutado de forma inmediata y esperada)         │
  └──────────────────────────────────────────────────────────────┘
```

---

### 3. Decisiones Técnicas y Compensaciones (Trade-Offs)

- **Identificación precisa de tecla Enter**:
  - *Condición*: `str.equals("\n") || str.equals("\r") || str.equals("\r\n")`.
  - *Efecto*: Garantiza que el comportamiento de la tecla Enter virtual sea 100% idéntico al de un teclado físico o al botón de retorno de la terminal, sin interferir con el pegado de bloques completos que contienen texto y saltos intercalados.
- **Limpieza de búfer**:
  - Al pulsar Enter, el búfer de autocompletado y autocorrección se resetea a cero para iniciar una nueva línea de comando sin arrastrar estados previos.

---

### 4. Arquitectura Técnica y Estrategia de Datos

```
┌──────────────────────────────────────────────────────────────────────────┐
│ Gboard (Evento de entrada)                                               │
│   └── commitText(text, 1)                                                │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ TerminalView.java (commitText)                                           │
│   ├── ¿Es salto solitario ("\n", "\r", "\r\n")?                          │
│   │     ├── SÍ -> Tecla Enter:                                           │
│   │     │         ├── mActiveWordBuffer.setLength(0)                     │
│   │     │         └── sendTextToTerminal("\r") [EJECUTA COMANDO]         │
│   │     └── NO -> ¿Contiene saltos de línea (multilínea pegada)?         │
│   │               ├── SÍ -> mEmulator.paste(str) [BRACKETED PASTE]       │
│   │               └── NO -> Inserción regular en cursor                  │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ TerminalSession / TerminalEmulator                                       │
│   ├── Si es Enter: ejecuta comando en shell                              │
│   └── Si es bloque pegado: inserta comandos sin ejecutarlos              │
└──────────────────────────────────────────────────────────────────────────┘
```

#### Plan de Modificaciones

1. **`terminal-view/src/main/java/com/termux/view/TerminalView.java`**:
   - Actualizar `commitText` en `InputConnection`:
     - Si `text` es únicamente un salto de línea (`"\n"` / `"\r"` / `"\r\n"`), despachar `sendTextToTerminal("\r")` y reiniciar el búfer local.
     - Si `text` contiene saltos de línea intercalados con texto de contenido, canalizarlo hacia `mEmulator.paste(str)`.
   - Ajustar `finishComposingText` bajo el mismo criterio.

2. **Verificación y Compilación**:
   - Validar con `compile_applet`.
   - Ejecutar pruebas unitarias para certificar la no regresión.
