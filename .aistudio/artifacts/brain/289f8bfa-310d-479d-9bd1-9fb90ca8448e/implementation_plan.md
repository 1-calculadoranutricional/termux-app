# Integración Secuencial: OpenWiki (Conceptual) → CodeGraph (Estructural) en AGENTS.md

Este plan define la reestructuración de `AGENTS.md` para establecer un flujo de dos etapas obligatorio al abordar cualquier tarea en el repositorio: primero contextualizar la arquitectura y conceptos del dominio mediante **OpenWiki**, y luego inspeccionar los símbolos y dependencias de código fuente con **CodeGraph**.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Decisiones confirmadas en la fase de clarificación con el usuario:
> - **OpenWiki es mandatorio y previo a CodeGraph**: Antes de ejecutar búsquedas de código o inspección de nodos, el agente debe contextualizarse en OpenWiki.
> - **Punto de anclaje inicial**: Lectura guiada a través de `openwiki/quickstart.md` para identificar qué dominio o subsistema atañe a la tarea (`architecture/`, `concepts/` o `workflows/`).
> - **Lectura selectiva para preservar contexto**: El agente no debe volcar toda la wiki, sino leer únicamente la página específica del módulo o flujo relevante detectada en el mapa de navegación.
> - **Reordenamiento físico**: La sección `<!-- OPENWIKI:START -->` se moverá por encima de `<!-- CODEGRAPH:START -->` para reflejar visualmente la jerarquía de ejecución.

---

### 1. Overview & Core Concept

- **Qué hace**: Transforma OpenWiki de una referencia opcional pasiva a una puerta de entrada obligatoria al repositorio, garantizando que el agente comprenda las fronteras entre módulos (`:app`, `:terminal-emulator`, `:terminal-view`, `:termux-shared`) y sus flujos de ejecución antes de examinar clases o métodos.
- **Audiencia objetivo**: Todos los agentes de IA que interactúen con el repositorio y reciban las instrucciones de `AGENTS.md`.
- **Valor clave**: Evita que el agente empiece a buscar código "a ciegas" con símbolos aislados; en su lugar, entiende la arquitectura global del subsistema antes de descender al detalle técnico con CodeGraph.

---

### 2. Flujo de Trabajo Secuencial del Agente

```
   ┌───────────────────────────────────────────────────────────────┐
   │                       NUEVA TAREA                             │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
                                   ▼
   ┌───────────────────────────────────────────────────────────────┐
   │ ETAPA 1: Contextualización Conceptual (OPENWIKI)              │
   │ 1. Consultar `openwiki/quickstart.md` (Wiki Routing Map)      │
   │ 2. Identificar el subsistema afectado:                        │
   │    - Architecture (overview, modules-and-dependencies)        │
   │    - Concepts (terminal-emulation, rendering-and-view)        │
   │    - Workflows (session-lifecycle, command-execution)         │
   │ 3. Leer únicamente la página específica del módulo relevante  │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
                                   ▼
   ┌───────────────────────────────────────────────────────────────┐
   │ ETAPA 2: Inspección Estructural de Código (CODEGRAPH)         │
   │ 1. Con los módulos/conceptos claros, consultar CodeGraph:     │
   │    - `codegraph explore "<symbol or question>"`               │
   │    - `codegraph node <symbol>`                                │
   │    - `codegraph query "<search>"`                             │
   │ 2. Ejecutar fallback con advertencia solo si CLI no existe    │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
                                   ▼
   ┌───────────────────────────────────────────────────────────────┐
   │ ETAPA 3: Edición y Sincronización                             │
   │ 1. Aplicar modificaciones de código                           │
   │ 2. Ejecutar `codegraph sync` inmediatamente                   │
   └───────────────────────────────────────────────────────────────┘
```

---

### 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Posición física de OpenWiki antes de CodeGraph**
  - *Enfoque*: Reorganizar el archivo colocando primero OpenWiki y luego CodeGraph.
  - *Razón*: Refuerza la secuencia temporal natural en el prompt del sistema: Concepto y Arquitectura → Grafo de Código e Implementación.

- **Decisión 2: Enrutamiento mediante `quickstart.md` y lectura modular acotada**
  - *Enfoque*: El agente debe abrir `openwiki/quickstart.md` para ubicar el tema, y luego abrir solamente el archivo correspondiente (por ejemplo, `openwiki/workflows/command-execution.md`).
  - *Razón*: Evita saturar la ventana de contexto leyendo toda la wiki completa, al tiempo que garantiza una comprensión arquitectónica profunda del área involucrada.

- **Decisión 3: Prohibición de saltar directo a CodeGraph sin marco conceptual**
  - *Enfoque*: Declarar explícitamente que no se permite iniciar con `codegraph` o búsquedas de archivos sin haber consultado primero el marco conceptual en OpenWiki si la tarea involucra componentes del sistema.

---

### 4. Technical Architecture & Rule Specification *(Technical Reference)*

#### Nueva Estructura Propuesta para `AGENTS.md`

```markdown
<!-- AGENTMEMORY:START -->
## Agent memory (agentmemory)
...
<!-- AGENTMEMORY:END -->

<!-- OPENWIKI:START -->
## OpenWiki & Conceptual Framework (MANDATORY STEP 1)

In repositories with an `openwiki/` directory, you **MUST** ground your conceptual understanding of the repository architecture and subsystem workflows **BEFORE** using CodeGraph or inspecting code:

1. **Mandatory Conceptual Routing**:
   - At the start of a task involving architecture, features, or workflows, first inspect `openwiki/quickstart.md` to identify the relevant domain.
   - The repository is organized into three major areas:
     - **Architecture**: `openwiki/architecture/overview.md` (topology & services) and `modules-and-dependencies.md` (multi-module Gradle structure).
     - **Concepts**: `openwiki/concepts/terminal-emulation.md` (VT/ANSI engine) and `rendering-and-view.md` (terminal view & touch pipelines).
     - **Workflows**: `openwiki/workflows/session-lifecycle.md` (session creation & execution) and `command-execution.md` (RunCommandService & intent APIs).

2. **Scoped Context Loading**:
   - To avoid context overload, do NOT read all wiki files at once. Read ONLY the specific subpage identified from `quickstart.md` that maps directly to the current task.

3. **Authoritative Alignment**:
   - Use OpenWiki to understand module boundaries, responsibilities, and system lifecycles. Once the conceptual model is grounded, proceed to **Step 2 (CodeGraph)** to locate exact symbols and execution graphs.
   - Do not manually edit generated OpenWiki pages unless explicitly instructed; the scheduled GitHub Action refreshes them.
<!-- OPENWIKI:END -->

<!-- CODEGRAPH:START -->
## CodeGraph (MANDATORY STEP 2)

In repositories indexed by CodeGraph (a `.codegraph/` directory exists at the repo root), once you have grounded the conceptual model via OpenWiki, you **MUST** strictly adhere to the following rules:

1. **Mandatory First-Hop Lookup**:
   - You **MUST** use CodeGraph as your primary tool before executing any `grep`, `find`, or raw file inspection (`view_file`).
   - Use `codegraph explore "<symbol or question>"` for combined code, definitions, and call paths in a single pass.
   - Use `codegraph node <symbol>` to inspect caller/callee trees and dependencies.
   - Use `codegraph query "<search>"` for fast symbol and identifier searches.
   - Bypassing CodeGraph in favor of arbitrary file reading or text grepping when `.codegraph/` is present is strictly prohibited.

2. **Mandatory Index Synchronization**:
   - You **MUST** run `codegraph sync` immediately after creating, modifying, or deleting source files to preserve index freshness for downstream tasks.

3. **Fallback Protocol**:
   - If the `codegraph` binary is not present in `$PATH` or fails with an execution error, you **MUST** explicitly state a warning to the user (e.g. `[Warning: CodeGraph CLI unavailable, falling back to grep/view_file]`) before proceeding with standard inspection tools.

If there is no `.codegraph/` directory present at the root, skip CodeGraph entirely.
<!-- CODEGRAPH:END -->
```

#### Plan de Verificación
1. Editar `/AGENTS.md` recolocando y reformulando las secciones en el orden: `agentmemory` -> `OPENWIKI (Step 1)` -> `CODEGRAPH (Step 2)`.
2. Verificar la integridad de los tags delimitadores y la ausencia de texto residual de la versión anterior.
3. Ejecutar sincronización y validar la consistencia del archivo.
