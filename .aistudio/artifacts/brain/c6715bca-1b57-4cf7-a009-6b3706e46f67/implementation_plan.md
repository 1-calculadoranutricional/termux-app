# OpenWiki Technical Audit & Codebase Synchronization Plan

Comprehensive technical audit and documentation synchronization to align OpenWiki with the current state of the Termux Android codebase, ensuring 100% technical fidelity across IME/input handling, Termux Store architecture, package execution pipelines, and inter-module dependencies.

## User Review & Critical Decisions

> [!IMPORTANT]
> The audit scope and documentation guidelines have been confirmed:
> - **Audit Scope**: Full comprehensive audit across core modules, software keyboard / IME input pipelines, and the Termux Store ecosystem.
> - **Language & Format**: Technical English adhering to canonical OpenWiki standards with full YAML frontmatter (`type`, `title`, `description`, `tags`, `sources`, `verified`), Mermaid sequence/flowchart diagrams, and precise source file anchor references (`repo://<path>#L...`).

- **Confirmed Decision 1**: Standard OpenWiki technical English with strict source anchoring.
- **Confirmed Decision 2**: Create a dedicated new workflow document for Termux Store package lifecycle, installation, and execution (`openwiki/workflows/package-management-and-store.md`).
- **Confirmed Decision 3**: Synchronize `rendering-and-view.md` to document the advanced `InputConnection` architecture in `TerminalView.java` (predictive text word buffer, Enter key execution, DECSET 2004 bracketed paste mode for multiline clipboard pastes).

---

## 1. Overview & Audit Scope

### Current Codebase Reality vs. Documentation Gap Analysis

1. **Terminal View & IME Input Handling (`rendering-and-view.md`)**:
   - *Current Gap*: The document devotes only three brief bullet points to keyboard input, completely omitting the specialized `InputConnection` implementation in `TerminalView.java`.
   - *Code Reality*: `TerminalView` manages predictive text and autocorrect using an internal `mActiveWordBuffer`, implements `getTextBeforeCursor()`, `getTextAfterCursor()`, and `getSelectedText()` to prevent character duplication in modern IMEs (such as Gboard), handles solitary newlines (`\n`, `\r`, `\r\n`) by dispatching carriage return (`\r`) directly for immediate command execution, routes multiline clipboard pastes to `TerminalEmulator.paste()` with DECSET 2004 bracketed paste sequences (`\033[200~ ... \033[201~`) to prevent prompt corruption (`cmdand>`), and supports rich content media commits (`commitContent`).

2. **Termux Store Architecture & Package Lifecycle (`openwiki/workflows/package-management-and-store.md`)**:
   - *Current Gap*: The Termux Store subsystem (`com.termux.app.store`) lacks any dedicated workflow documentation. `overview.md` only has a single-sentence mention.
   - *Code Reality*: `com.termux.app.store` implements a full package discovery and installation catalog (`StorePackageItem`, `StorePackageAdapter`, `TermuxStoreActivity`) categorized by development, shells, editors, utilities, and networking. It features binary installation verification against `$PREFIX/bin/`, launch commands, silent/interactive terminal execution via `TermuxActivity.ACTION_EXECUTE_IN_CURRENT_SESSION`, and specialized shell script provisioning (e.g., `setup_zsh.sh` file generation in `$HOME` or cache with executable permissions for Zsh and Oh My Zsh). Curated catalog entries include Lazygit, Git, GitHub CLI, Neovim, Python, Node.js, and custom packages.

3. **Architecture Overview & Navigation (`overview.md`, `quickstart.md`, `modules-and-dependencies.md`)**:
   - *Current Gap*: `quickstart.md` and module docs omit the Store activity and its integration channels with the terminal session runner.
   - *Code Reality*: `TermuxStoreActivity` directly invokes `TermuxActivity` via `ACTION_EXECUTE_IN_CURRENT_SESSION` with `EXTRA_COMMAND`, bridging application UI with the active `TerminalSession`.

---

## 2. Documentation Architecture & Changes

```
┌────────────────────────────────────────────────────────────────────────┐
│                         OpenWiki Routing Map                           │
├────────────────────────────────┬───────────────────────────────────────┤
│ Architecture                   │ Concepts                              │
│ ├─ overview.md (Updated)       │ ├─ terminal-emulation.md              │
│ └─ modules-and-dependencies.md │ └─ rendering-and-view.md (Updated)    │
│                                │    └─ InputConnection & IME Pipeline  │
├────────────────────────────────┴───────────────────────────────────────┤
│ Workflows                                                              │
│ ├─ session-lifecycle.md                                                │
│ ├─ command-execution.md                                                │
│ └─ package-management-and-store.md (NEW)                              │
│    └─ StorePackageItem, Category Filtering, Terminal Session Dispatch │
└────────────────────────────────────────────────────────────────────────┘
```

### Detailed File Modification Plan

#### 1. Update `openwiki/concepts/rendering-and-view.md`
- **Expand Section**: *Keyboard, IME, & Hardware Keys*.
- **Add Subsections**:
  - `InputConnection Lifecycle & Word Buffering`: Document `mActiveWordBuffer`, word boundaries, `appendToBuffer()`, `removeFromBuffer()`, and cursor text retrieval (`getTextBeforeCursor()`, `getTextAfterCursor()`, `getSelectedText()`).
  - `Autocorrect & Predictive IME Synchronization`: Detail how pass-through `commitCorrection()` prevents duplicated text in modern keyboards like Gboard.
  - `Command Execution vs. Bracketed Paste Handling`: Detail the branching in `commitText()`:
    - Solitary newlines (`\n`, `\r`, `\r\n`) dispatch `\r` immediately to execute commands.
    - Multiline blocks (clipboard pastes) activate DECSET 2004 bracketed paste mode (`\033[200~ ... \033[201~`) via `mEmulator.paste()` to prevent premature execution and command prompt corruption.
  - `Rich Content Commit`: Document `commitContent(InputContentInfoCompat)` handling.
- **Update Sources**: Anchor to `repo://terminal-view/src/main/java/com/termux/view/TerminalView.java`.

#### 2. Create `openwiki/workflows/package-management-and-store.md` (NEW)
- **Frontmatter**: Standard YAML (`type: workflow`, `title: Termux Store & Package Management Workflow`, tags, sources).
- **Architecture & Sequence Diagram**: Mermaid diagram illustrating user browsing in `TermuxStoreActivity`, binary status inspection in `$PREFIX/bin/`, package action dispatch (`ACTION_EXECUTE_IN_CURRENT_SESSION`), and Zsh bootstrap script file generation (`setup_zsh.sh`).
- **Core Components**:
  - `StorePackageItem`: Schema definition (`id`, `name`, `description`, `category`, `iconRes`, `installCommand`, `binaryPath`, `launchCommand`, `isInstalled`, `uninstallCommand`).
  - `StorePackageAdapter`: Search filtering, categorized view rendering, and installation state checks.
  - `Execution Pipelines`:
    - Direct command dispatch via `TermuxActivity.ACTION_EXECUTE_IN_CURRENT_SESSION`.
    - Automated shell script generation (`setup_zsh.sh`) written to `$HOME` or app internal cache with `chmod +x` executable permissions.
  - Curated Catalog Overview: Development (Git, Lazygit, GitHub CLI), Shells (Zsh, Fish), Editors (Neovim, Nano, Micro), Utilities, and Languages (Python, Node.js).
- **Sources**: Anchor to `repo://app/src/main/java/com/termux/app/store/TermuxStoreActivity.java`, `StorePackageItem.java`, `StorePackageAdapter.java`.

#### 3. Update `openwiki/architecture/overview.md`
- Deepen the description of `TermuxStoreActivity` under *Auxiliary Activities*.
- Add `TermuxStoreActivity` to the architectural topology sequence diagram and component relationships showing how it commands the active terminal session via `ACTION_EXECUTE_IN_CURRENT_SESSION`.

#### 4. Update Navigation & Indexes
- Update `openwiki/quickstart.md`: Add `[Termux Store & Package Management](workflows/package-management-and-store.md)` to the routing map and project structure.
- Update `openwiki/workflows/index.md`: Add link and description for `package-management-and-store.md`.

---

## 3. Verification & CodeGraph Synchronization

1. **Strict Non-Interactive Synchronization**:
   - Run `codegraph sync --no-color` immediately upon completing all file additions and updates to ensure the repository graph and OpenWiki index are synchronized.
2. **Link & Reference Integrity**:
   - Verify all markdown links, YAML anchors (`repo://...`), and Mermaid diagrams for syntax correctness.
3. **Applet Compilation Check**:
   - Run `compile_applet` to ensure overall project health and clean build state.
