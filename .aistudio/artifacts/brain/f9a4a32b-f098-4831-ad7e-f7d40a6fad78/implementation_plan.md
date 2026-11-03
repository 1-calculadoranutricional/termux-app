# Ejecución Directa de Comandos en la Sesión Activa desde Termux Play Store

Simplificar el flujo de instalación y ejecución de herramientas de la Play Store de Termux enviando los comandos directamente a la sesión activa de la terminal, eliminando la generación de scripts `.sh` temporales y la proliferación de múltiples sesiones secundarias.

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones clave acordadas:**
> - **Sesión actual única:** Todo comando de instalación, actualización, desinstalación o ejecución iniciado desde la Play Store se ejecutará en la sesión que el usuario ya tenga abierta en primer plano.
> - **Sin scripts intermediarios:** Se elimina por completo la escritura de scripts `.sh` en almacenamiento temporal y la llamada a `ACTION_SERVICE_EXECUTE` que forzaba nuevas pestañas/sesiones.
> - **Transición fluida:** Al pulsar una acción, la app regresa inmediatamente a la terminal (`TermuxActivity`) enfocando la consola donde el comando ya estará ejecutándose.
> - **Garantía ante sesión inexistente:** Si por algún motivo no hubiese ninguna sesión abierta (o estuviese finalizada), se levantará la sesión principal y se inyectará el comando sin crear duplicados.

---

### 1. Overview & Core Concept

- **Qué hace:** Al pulsar "Instalar", "Actualizar", "Ejecutar" o "Desinstalar" en cualquier paquete o herramienta de la Play Store de Termux, la aplicación toma el comando original del paquete (por ejemplo `pkg install -y python`), cambia el foco de la pantalla a la terminal activa e inyecta la instrucción con un retorno de carro (`\r`), ejecutándolo de inmediato en la misma sesión.
- **Audiencia / Persona:** Usuarios de Termux que buscan una experiencia ágil y limpia sin que el menú lateral de sesiones se llene de pestañas residuales cada vez que instalan un paquete.
- **Valor Principal:** Simplicidad, inmediatez y orden. Cero archivos `.sh` temporales que limpiar y cero sesiones huérfanas que cerrar manualmente con `exit`.

---

### 2. User Experience & Visual Design

- **Flujo de Usuario:**
  1. El usuario navega por las categorías ("Para ti", "Google", "Top Tools", "Dev", "System", "AI CLI") o busca un paquete en la Play Store.
  2. Pulsa el botón de acción ("Instalar", "Actualizar", "Ejecutar") en la tarjeta del paquete o dentro del diálogo de detalles.
  3. La Play Store muestra un aviso breve (Toast) informando el comando que se está ejecutando.
  4. La pantalla vuelve de inmediato a la terminal principal (`TermuxActivity`).
  5. En la misma sesión activa, el comando ya se encuentra ejecutándose en vivo, mostrando el progreso de instalación estándar de `apt`/`pkg` o la herramienta seleccionada.

- **Diseño Visual:**
  - Se mantiene la interfaz estilo Google Play Store oscura y pulida ya construida.
  - Se elimina el banner residual de texto bash (`echo ... Sesión activa para auditar...`), permitiendo que el usuario vea la salida nativa del comando sin ruido innecesario.

---

### 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Comunicación entre `TermuxStoreActivity` y `TermuxActivity`**
  - *Enfoque elegido:* Uso de `Intent` explícito con `FLAG_ACTIVITY_REORDER_TO_FRONT` y acción dedicada `ACTION_EXECUTE_IN_CURRENT_SESSION`.
  - *Por qué:* `TermuxActivity` está configurada como `singleTask`. Al enviar este intent, Android invoca el ciclo de vida `onNewIntent()` de forma limpia y segura, garantizando que la terminal se ponga al frente y reciba el comando sin recrear la actividad ni perder el estado del emulador.
  - *Alternativa descartada:* Mantener el servicio en segundo plano `TermuxService.ACTION_SERVICE_EXECUTE` con scripts `.sh`, la cual fue diseñada para invocar runners de nuevas sesiones o tareas background y provocaba la apertura de múltiples terminales.

- **Decisión 2: Inyección de comando en `TerminalSession`**
  - *Enfoque elegido:* Escribir a través del flujo I/O UTF-8 estándar de la sesión activa (`session.write(command + "\r")`).
  - *Por qué:* Simula de forma idéntica la pulsación de la tecla Enter por el usuario en el teclado virtual o físico, siendo 100% compatible con shells como `bash`, `zsh` o `sh`.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                  TermuxStoreActivity                   │
│                                                        │
│  User taps "Instalar" / "Ejecutar"                     │
│  Extract: item.getInstallCommand()                     │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ Intent (ACTION_EXECUTE_IN_CURRENT_SESSION)
                            │ + EXTRA_COMMAND
                            │ + FLAG_ACTIVITY_REORDER_TO_FRONT
                            ▼
┌────────────────────────────────────────────────────────┐
│                     TermuxActivity                     │
│                                                        │
│  onNewIntent(intent)                                   │
│  Extract EXTRA_COMMAND                                 │
│  Get getCurrentSession()                               │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ session.write(command + "\r")
                            ▼
┌────────────────────────────────────────────────────────┐
│                    TerminalSession                     │
│                                                        │
│  Injected directly into active PTY                     │
│  Executes in the existing session (No extra tabs)      │
└────────────────────────────────────────────────────────┘
```

- **Componentes a modificar:**
  1. `TermuxActivity.java`:
     - Declarar constantes `ACTION_EXECUTE_IN_CURRENT_SESSION` y `EXTRA_COMMAND`.
     - Implementar `onNewIntent(Intent intent)` para interceptar comandos entrantes cuando la actividad ya está en memoria.
     - Implementar `executeCommandInCurrentSession(String command)` que obtiene la sesión activa mediante `getCurrentSession()` y escribe la orden con retorno de carro.
  2. `TermuxStoreActivity.java`:
     - Refactorizar `executeShellCommandInTerminal`: eliminar la creación de archivos `.sh` temporales en disco y la llamada a `TermuxService.ACTION_SERVICE_EXECUTE`.
     - Reemplazarlo por el despacho del Intent directo a `TermuxActivity`.
