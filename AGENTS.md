<!-- OPENWIKI:START -->
## OpenWiki & Conceptual Framework (MANDATORY STEP 1)

In repositories with an `openwiki/` directory, you **MUST** ground your conceptual understanding of the repository architecture and subsystem workflows **BEFORE** using CodeGraph or inspecting code:

1. **Mandatory Conceptual Routing**:
   - At the start of a task involving architecture, features, or workflows, first inspect `openwiki/quickstart.md` to identify the relevant domain.
   - The repository is organized into three major areas across 4 modules (`:app`, `:termux-shared`, `:terminal-view`, `:terminal-emulator`):
     - **Architecture**: `openwiki/architecture/overview.md` (topology & services) and `modules-and-dependencies.md` (multi-module Gradle structure).
     - **Concepts**: `openwiki/concepts/terminal-emulation.md` (VT/ANSI engine) and `rendering-and-view.md` (terminal view & touch pipelines).
     - **Workflows**: `openwiki/workflows/session-lifecycle.md` (session creation & execution) and `command-execution.md` (RunCommandService & intent APIs).

2. **Scoped Context Loading & Symbol Extraction**:
   - Read ONLY the specific subpage identified from `quickstart.md` mapping to the task to avoid context overload.
   - **Extract Anchor Symbols**: Collect the primary classes and interfaces listed under the page's YAML `sources:` block (e.g. `TerminalEmulator`, `RunCommandService`, `TermuxSession`) to serve as immediate entry points for Step 2.

3. **Authoritative Alignment**:
   - Use OpenWiki to understand module boundaries, responsibilities, and system lifecycles. Once grounded, proceed to **Step 2 (CodeGraph)** to locate exact symbols and execution graphs.
   - Do not manually edit generated OpenWiki pages unless explicitly instructed; the scheduled GitHub Action refreshes them.
<!-- OPENWIKI:END -->

<!-- CODEGRAPH:START -->
## CodeGraph (MANDATORY STEP 2 - NON-INTERACTIVE)

In repositories where `.codegraph/` exists, CodeGraph serves as the mandatory structural navigation engine. It **MUST** always be present and executed in non-interactive mode.

1. **Automatic Installation & Initialization**:
   - If `codegraph` is not found in `$PATH`, the agent **MUST** immediately install it via:
     ```bash
     npm install -g @colbymchenry/codegraph
     ```
   - If the index is not initialized or database is missing, initialize non-interactively with:
     ```bash
     codegraph init -y --no-color
     ```

2. **Strict Non-Interactive Execution**:
   - All CodeGraph commands **MUST** use flags that prevent prompts, interactive TTY requirements, or ANSI formatting issues:
     - Always pass `--no-color` (or set `CODEGRAPH_TELEMETRY=0`).
     - Never run interactive commands or omit `-y` on initialization.
     - For index synchronization: `codegraph sync --no-color` (or `codegraph sync -q`).

3. **Mandatory First-Hop Lookup via OpenWiki Anchors**:
   - You **MUST** use CodeGraph as your primary tool before executing any `grep`, `find`, or raw file inspection (`view_file`).
   - Feed the anchor symbols discovered in Step 1 directly into:
     - `codegraph explore --no-color "<symbol or query>"` (shows source code + call paths).
     - `codegraph node --no-color <symbol>` (inspects caller/callee hierarchy and definitions).
     - `codegraph query --no-color "<search>"` (fast symbol lookup).
   - For JNI/native methods identified in the graph, inspect the corresponding C sources under `src/main/jni/` or `src/main/cpp/`.
   - Bypassing CodeGraph in favor of arbitrary file reading or text grepping when `.codegraph/` is present is strictly prohibited.

4. **Mandatory Index Synchronization**:
   - You **MUST** run `codegraph sync --no-color` immediately after creating, modifying, or deleting source files to preserve index freshness for downstream tasks.
<!-- CODEGRAPH:END -->
