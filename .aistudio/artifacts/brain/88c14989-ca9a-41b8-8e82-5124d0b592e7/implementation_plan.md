# Soporte de Portapapeles Gboard (Texto e Imágenes) en Termux

Permitir pegar texto e imágenes (capturas de pantalla y elementos multimedia) directamente desde la barra de sugerencias y portapapeles de Gboard en Termux, resolviendo la ruta del archivo en el almacenamiento del dispositivo o guardando el flujo de imagen para insertar su ruta absoluta en la línea de comandos activa.

### Decisiones Críticas y Preferencias Confirmadas

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> - **Inserción de imágenes desde Gboard**: Al pulsar una captura o imagen en el portapapeles de Gboard, Termux obtendrá la ruta del archivo y la insertará directamente en la terminal activa como texto (ruta entrecomillada).
> - **Resolución de ruta**: Si la captura ya existe en el almacenamiento del dispositivo (como `/sdcard/DCIM/Screenshots/...`), se resolverá y priorizará la ruta accesible a través del almacenamiento de Termux (`/data/data/com.termux/files/home/storage/dcim/Screenshots/...` o la ruta absoluta en almacenamiento compartido). Si es un URI de contenido temporal o externo, se guardará en la carpeta de capturas o descargas y se pegará dicha ruta.
> - **Inserción de texto**: El texto pegado desde las sugerencias de Gboard se insertará de manera directa como entrada de teclado estándar en la sesión de terminal.

---

### 1. Visión General del Flujo y Concepto

Gboard en Android ofrece una barra superior con chips de sugerencias del portapapeles tanto para fragmentos de texto como para imágenes o capturas de pantalla recientes.

Actualmente, `TerminalView` no declara compatibilidad con tipos MIME de contenido enriquecido (`EditorInfoCompat.setContentMimeTypes`), por lo que Gboard no ofrece sugerencias de imágenes en su barra superior. Además, se requiere un `OnCommitContentListener` para recibir los datos de `InputContentInfoCompat` cuando el usuario pulsa un chip de imagen en Gboard.

Con esta mejora:
1. Gboard detectará que Termux admite `image/png`, `image/jpeg`, `image/gif`, `image/webp` e `image/*`.
2. Al tocar una sugerencia de captura de pantalla en Gboard, Termux recibirá el URI de contenido.
3. Se resolverá la ruta del archivo real en el dispositivo (ej. `DCIM/Screenshots/Screenshot_...jpg`) o se guardará en `~/storage/dcim/Screenshots/` si proviene de un proveedor en memoria.
4. La ruta absoluta se insertará automáticamente entrecomillada en el prompt de la terminal activa (`'/data/data/com.termux/files/home/storage/dcim/Screenshots/...'`).
5. El texto copiado en Gboard se pegará de forma inmediata e intuitiva en la sesión activa.

---

### 2. Experiencia de Usuario y Flujo de Interacción

```
  ┌──────────────────────────────────────────────────────────────┐
  │ 1. Usuario copia texto o toma una captura de pantalla        │
  │    (aparece el chip de portapapeles en Gboard)               │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 2. Usuario toca el chip de captura en Gboard                 │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 3. Termux recibe InputContentInfoCompat (URI de imagen)       │
  │    - Solicita permisos de lectura de URI si aplica           │
  │    - Resuelve la ruta física del archivo en DCIM/Screenshots │
  │    - Si es volátil/externo, guarda la copia en el storage    │
  └──────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ 4. Termux inserta la ruta en la sesión activa:               │
  │    $ '/data/data/com.termux/files/home/storage/dcim/...'     │
  └──────────────────────────────────────────────────────────────┘
```

---

### 3. Arquitectura Técnica

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Gboard (IME)                                                            │
│   ├── Suggestions Strip (Text Chips & Image Thumbnails)                │
│   └── commitContent(InputContentInfoCompat) / commitText()              │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ TerminalView (terminal-view)                                            │
│   ├── onCreateInputConnection(EditorInfo outAttrs)                      │
│   │     ├── EditorInfoCompat.setContentMimeTypes(image/*)               │
│   │     └── InputConnectionCompat.createWrapper(..., commitListener)   │
│   └── TerminalViewClient.onCommitContent(inputContentInfo)              │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ TermuxTerminalViewClient / TermuxActivity (app)                         │
│   ├── ClipboardContentHandler                                           │
│   │     ├── Resolver URI -> File Path (/storage/emulated/0/DCIM/...)    │
│   │     ├── Mapeo a ruta Termux (~/storage/dcim/Screenshots/...)        │
│   │     └── Fallback: Guardar stream a archivo local                    │
│   └── TerminalSession.write(escapedPath)                                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 4. Componentes y Modificaciones Previstas

1. **`terminal-view/src/main/java/com/termux/view/TerminalView.java`**:
   - En `onCreateInputConnection`:
     - Configurar `EditorInfoCompat.setContentMimeTypes(outAttrs, new String[]{"image/png", "image/jpeg", "image/gif", "image/webp", "image/*"})`.
     - Envolver el `BaseInputConnection` mediante `InputConnectionCompat.createWrapper()` con un `OnCommitContentListener`.
     - Validar que las opciones de `EditorInfo` permitan a Gboard mostrar la tira de sugerencias y portapapeles sin romper el comportamiento de caracteres de la terminal.

2. **`terminal-view/src/main/java/com/termux/view/TerminalViewClient.java`**:
   - Declarar el callback `boolean onCommitContent(InputContentInfoCompat inputContentInfo)`.

3. **`app/src/main/java/com/termux/app/terminal/TermuxTerminalViewClient.java`**:
   - Implementar `onCommitContent(InputContentInfoCompat inputContentInfo)`:
     - Gestionar el permiso transitorio del URI (`inputContentInfo.requestPermission()`).
     - Consultar el ContentResolver para identificar el nombre de archivo y la ruta original (mediante `MediaStore.MediaColumns.DATA` o `OpenableColumns.DISPLAY_NAME`).
     - Resolver la ruta existente en `/storage/emulated/0/...` y traducirla a la ruta equivalente de Termux si existe el enlace simbólico `~/storage/` (por ejemplo `/data/data/com.termux/files/home/storage/dcim/...`).
     - Si el URI proviene de una app externa y no tiene archivo local accesible, copiar el `InputStream` al directorio de capturas o almacenamiento de Termux.
     - Escribir la ruta entrecomillada (`'...'`) directamente en la sesión actual (`getCurrentSession().write(...)`).

4. **Compatibilidad con permisos y compilación**:
   - Asegurar que no se rompan las dependencias existentes y validar el build completo del proyecto con `compile_applet`.
