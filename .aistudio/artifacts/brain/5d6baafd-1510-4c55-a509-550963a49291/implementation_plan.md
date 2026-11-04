# Plan de Calibración de AGENTS.md para Google AI Studio

Reescritura y optimización de `AGENTS.md` concebido como el manual operativo directo e inyectable para el agente de desarrollo en Google AI Studio Build, mapeando cada fase del protocolo a las herramientas nativas del entorno (`view_file`, `replace_file_content`, `compile_applet`, `run_command`) y eliminando conceptos abstractos de subagentes.

---

## Decisiones Críticas Confirmadas por el Usuario

> [!IMPORTANT]
> - **Consumidor Único**: El archivo `AGENTS.md` es ingerido automáticamente por la plataforma como `<RULE[AGENTS_md]>` en cada nueva sesión de este repositorio dentro de Google AI Studio.
> - **Eliminación de Abstracciones**: Se eliminan todas las directivas genéricas de subagentes externos o equipos multiagente abstractos.
> - **Mapeo a Herramientas Nativas**: Cada paso del protocolo debe ordenar taxativamente qué herramienta de AI Studio invocar (`compile_applet`, `run_command`, `view_file`, `replace_file_content`).

---

## 1. Diagnóstico y Valor de la Calibración

En la arquitectura de Google AI Studio, cualquier texto en `AGENTS.md` se convierte en una **regla prioritaria de comportamiento** para el agente (`<RULE[AGENTS_md]>`).
- **Problema previo**: Hablar de "delegación a subagentes" o "manifiestos de subagentes" confunde el razonamiento del modelo, haciéndole esperar la existencia de otros agentes paralelos en lugar de ejecutar la tarea de forma autónoma.
- **Solución**: Un protocolo en primera persona para el agente de AI Studio, estructurado en **4 fases operativas y 1 sección de resiliencia del entorno**, que guía de principio a fin su ciclo de vida en cada turno.

---

## 2. Mapa de Fases Calibradas para AI Studio

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                 PROTOCOLO OPERATIVO GOOGLE AI STUDIO (AGENTS.md)            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [FASE 1: CONTEXTUALIZACIÓN] ──► view_file en openwiki/quickstart.md        │
│              │                   Extracción de símbolos ancla (sources)     │
│              ▼                                                              │
│  [FASE 2: NAVEGACIÓN GRAFO]  ──► run_command(codegraph explore/node/query)  │
│              │                   (Instalación e inicialización si 127)      │
│              ▼                                                              │
│  [FASE 3: IMPLEMENTACIÓN]    ──► replace_file_content / write_to_file       │
│              │                   Edición atómica en un único turno          │
│              ▼                                                              │
│  [FASE 4: CIERRE & COMPUERTAS]► compile_applet (Compilación limpia)         │
│              │                   run_command(gradle :app:testDebugUnitTest) │
│              │                   run_command(codegraph sync --no-color)     │
│              ▼                                                              │
│  [RESILIENCIA DEL ENTORNO]   ──► Reglas críticas de sandbox (no adb,        │
│                                  gradle wrapper no soportado, keystore)     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Especificación Detallada de Secciones

### Fase 1: Contextualización Conceptual Obligatoria (OpenWiki)
- Conservar etiquetas canónicas `<!-- OPENWIKI:START -->` y `<!-- OPENWIKI:END -->`.
- Instrucción taxativa: Usar `view_file` en `openwiki/quickstart.md` y luego en la subpágina pertinente antes de tocar código.
- Extraer los símbolos listados en `sources:` como punto de entrada de la Fase 2.

### Fase 2: Navegación Estructural y Relacional (CodeGraph)
- Conservar etiquetas canónicas `<!-- CODEGRAPH:START -->` y `<!-- CODEGRAPH:END -->`.
- Instrucción taxativa: Invocar `run_command` con `codegraph explore --no-color`, `codegraph node --no-color` o `codegraph query --no-color`.
- Si `codegraph` no existe (código 127), autoinstalar de inmediato con `run_command(CommandLine="npm install -g @colbymchenry/codegraph")`.
- Si reporta `CodeGraph not initialized`, ejecutar `run_command(CommandLine="codegraph init -y --no-color")`.

### Fase 3: Implementación y Modificación de Código
- Uso preferente de `replace_file_content` para ediciones atómicas de bloques contiguos.
- Regla de lectura previa inteligente (*Read-Before-Write*).
- Prohibición total de stubs ficticios o botones sin manejador real.

### Fase 4: Quality Gate y Cierre de Integridad (*Definition of Done*)
- **Compuerta 1**: Ejecutar la herramienta nativa `compile_applet`.
- **Compuerta 2**: Ejecutar `run_command(CommandLine="gradle :app:testDebugUnitTest")` (utilizando siempre `gradle`, nunca `./gradlew`).
- **Compuerta 3**: Ejecutar `run_command(CommandLine="codegraph sync --no-color")`.
- **Compuerta 4**: Verificar si los cambios impactaron contratos de OpenWiki y actualizar claims en `.claims/`.

### Sección de Resiliencia y Restricciones del Entorno AI Studio
- **Prohibición de ADB / Emulador local**: No intentar ejecutar pruebas `androidTest` o comandos `adb`.
- **Gradle Wrapper**: No usar jamás `./gradlew` (no soportado en el contenedor; usar siempre `gradle`).
- **Preservación de Keystores**: No modificar `testkey_untrusted.jks`, `debug.keystore` ni los bloques `signingConfigs`.
- **Metadatos de Plataforma**: Mantener en sincronía `metadata.json` con `app_name`.

---

## 4. Plan de Acción de Ejecución

1. **Escritura de `AGENTS.md`**:
   - Reemplazar `/AGENTS.md` con la versión calibrada específicamente para el agente de Google AI Studio.
2. **Sincronización y Validación**:
   - Ejecutar `codegraph sync --no-color`.
   - Ejecutar `compile_applet` para confirmar integridad total.
