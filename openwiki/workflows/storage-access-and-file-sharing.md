---
type: workflow
title: Storage Access Framework & External File Sharing Workflow
description: Document provider integration via TermuxDocumentsProvider and external file/URL sharing via FileReceiverActivity.
tags: [storage-access-framework, documents-provider, file-sharing, file-receiver, url-opener, termux-file-editor]
sources:
  - id: openwiki-source-documents-provider
    resource: repo://app/src/main/java/com/termux/filepicker/TermuxDocumentsProvider.java
  - id: openwiki-source-file-receiver
    resource: repo://app/src/main/java/com/termux/app/api/file/FileReceiverActivity.java
  - id: openwiki-source-manifest
    resource: repo://app/src/main/AndroidManifest.xml
verified:
  - by: manual-audit
    at: 2026-11-04T03:00:00.000Z
---

# Storage Access Framework & External File Sharing Workflow

Termux integrates bidirectional file access with the Android operating system and third-party apps through two primary mechanisms:
1. **`TermuxDocumentsProvider`**: A Storage Access Framework (SAF) `DocumentsProvider` exposing the Termux home directory (`$HOME`) to external file pickers and apps.
2. **`FileReceiverActivity`**: An activity handling standard Android share targets (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`), routing received files, URLs, and scripts into Termux environments.

---

## Architectural Interaction Flow

```mermaid
sequenceDiagram
    participant Ext as External App / System File Picker
    participant DP as TermuxDocumentsProvider (SAF)
    participant FR as FileReceiverActivity
    participant FS as Termux Filesystem ($HOME)
    participant Act as TermuxActivity / Execution Service

    alt Storage Access Framework (SAF)
        Ext->>DP: queryRoots() / queryChildDocuments(parentDocId)
        DP->>FS: List files in $HOME
        FS-->>DP: File hierarchy & metadata
        DP-->>Ext: MatrixCursor (documentId, mime, flags, size)
        Ext->>DP: openDocument(documentId, mode) / createDocument()
        DP->>FS: ParcelFileDescriptor.open(file, accessMode)
        FS-->>Ext: Direct I/O streaming
    else External Share Intent (ACTION_SEND)
        Ext->>FR: Intent(ACTION_SEND, streamUri or extraText)
        alt Shared Text is URL
            FR->>Act: Execute ~/bin/termux-url-opener <url>
        alt Shared Text or File
            FR->>FS: Save to ~/downloads/<filename>
            opt File Editor Hook
                FR->>Act: Execute ~/bin/termux-file-editor <saved_file>
            end
        end
    end
```
*Interaction flow for SAF document browsing and external share intent processing.*

---

## 1. Storage Access Framework (`TermuxDocumentsProvider`)

`TermuxDocumentsProvider` extends Android's `android.provider.DocumentsProvider` and is declared in `AndroidManifest.xml` with authority `${applicationId}.documents`:

```xml
<provider
    android:name="com.termux.filepicker.TermuxDocumentsProvider"
    android:authorities="com.termux.documents"
    android:exported="true"
    android:grantUriPermissions="true"
    android:permission="android.permission.MANAGE_DOCUMENTS">
    <intent-filter>
        <action android:name="android.content.action.DOCUMENTS_PROVIDER" />
    </intent-filter>
</provider>
```

### Core Responsibilities & Methods
- **Root Exposure (`queryRoots`)**: Exposes a primary root pointing to `TermuxConstants.TERMUX_HOME_DIR` (`/data/data/com.termux/files/home`). The root is configured with flags `FLAG_SUPPORTS_SEARCH | FLAG_SUPPORTS_IS_CHILD | FLAG_SUPPORTS_RECENTS`.
- **Document Metadata (`queryDocument`, `queryChildDocuments`)**: Queries directory children and returns a `MatrixCursor` containing:
  - `Document.COLUMN_DOCUMENT_ID`: Unique file identifier represented as the relative file path from `$HOME`.
  - `Document.COLUMN_MIME_TYPE`: Detected MIME type (`Document.MIME_TYPE_DIR` for directories or resolved via `MimeTypeMap`).
  - `Document.COLUMN_DISPLAY_NAME`: File or folder name.
  - `Document.COLUMN_LAST_MODIFIED`: Modification timestamp.
  - `Document.COLUMN_FLAGS`: Permissions including `FLAG_SUPPORTS_DELETE`, `FLAG_SUPPORTS_WRITE`, and `FLAG_DIR_SUPPORTS_CREATE`.
  - `Document.COLUMN_SIZE`: File size in bytes.
- **File I/O (`openDocument`)**: Opens a file using `ParcelFileDescriptor.open(file, accessMode)` for modes `"r"`, `"w"`, `"wt"`, `"wa"`, `"rw"`, and `"rwt"`.
- **Document Management (`createDocument`, `deleteDocument`)**:
  - `createDocument`: Creates empty regular files or directories (`mkdir()`) inside target parent folders.
  - `deleteDocument`: Deletes target files or recursively removes directory trees.
- **Security Boundary**: Validates that all requested document paths resolve within `BASE_DIR` (`$HOME`), preventing traversal out of the Termux home environment.

---

## 2. External Share Handling (`FileReceiverActivity`)

`FileReceiverActivity` (`com.termux.app.api.file.FileReceiverActivity`) registers for intent actions `Intent.ACTION_SEND` and `Intent.ACTION_SEND_MULTIPLE` across all MIME types (`*/*`).

### Execution Workflows

1. **URL Interception**:
   - If the incoming shared content is a valid URL (`Patterns.WEB_URL` or starts with `http://`, `https://`, `ftp://`, `mailto:`), it checks for the executable `~/bin/termux-url-opener`.
   - If present, it spawns an execution command passing the URL as an argument (`~/bin/termux-url-opener "<url>"`).
   - If absent, it opens the URL in the system browser via `Intent.ACTION_VIEW`.

2. **File & Stream Reception**:
   - Resolves the shared attachment's display name via `OpenableColumns.DISPLAY_NAME` from the URI.
   - If no name is provided, prompts the user via `TextInputDialogUtils.textInput()`.
   - Saves the stream into `~/downloads/<displayName>` (`TERMUX_RECEIVEDIR`).
   - If the file editor hook exists (`~/bin/termux-file-editor`), it executes the editor binary pointing to the newly saved file in the active or new session.

---

## Invariants & Security Policies

- **Mutual Exclusion with `ACTION_GET_CONTENT`**: SAF provider replaces `ACTION_GET_CONTENT` to prevent duplicate ambiguous entries in the system picker UI.
- **Path Confinement**: All SAF read/write operations are confined to `$HOME`. System directories (`$PREFIX/usr`, `$PREFIX/etc`, `/system`) are not exposed via the documents provider.
- **Executable Hooks**: Script hooks (`termux-url-opener` and `termux-file-editor`) must reside within `$HOME/bin/` and have executable permissions to be invoked by `FileReceiverActivity`.
