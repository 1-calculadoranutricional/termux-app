---
type: concept
title: Filesystem & POSIX Dispatcher Subsystem
description: Low-level POSIX filesystem inspection, NativeDispatcher syscall bridging, FileAttributes metadata extraction, and safe file operations.
tags: [filesystem, posix, native-dispatcher, file-attributes, permissions, stat, lstat, symlinks]
sources:
  - id: openwiki-source-native-dispatcher
    resource: repo://termux-shared/src/main/java/com/termux/shared/file/filesystem/NativeDispatcher.java
  - id: openwiki-source-file-attributes
    resource: repo://termux-shared/src/main/java/com/termux/shared/file/filesystem/FileAttributes.java
  - id: openwiki-source-file-permissions
    resource: repo://termux-shared/src/main/java/com/termux/shared/file/filesystem/FilePermissions.java
  - id: openwiki-source-file-types
    resource: repo://termux-shared/src/main/java/com/termux/shared/file/filesystem/FileTypes.java
  - id: openwiki-source-unix-constants
    resource: repo://termux-shared/src/main/java/com/termux/shared/file/filesystem/UnixConstants.java
  - id: openwiki-source-file-utils
    resource: repo://termux-shared/src/main/java/com/termux/shared/file/FileUtils.java
verified:
  - by: manual-audit
    at: 2026-11-04T17:40:00.000Z
---

# Filesystem & POSIX Dispatcher Subsystem

Because Termux operates as a complete Linux distribution inside Android's app sandbox, standard Java `java.io.File` abstractions are insufficient. `java.io.File` cannot differentiate between symbolic links and regular files, cannot read POSIX file mode bits (`rwxrwxrwx`), cannot determine numeric user/group IDs (`st_uid`, `st_gid`), and cannot access nanosecond-precision timestamps (`st_atime_nsec`, `st_mtime_nsec`, `st_ctime_nsec`).

To provide fidelity with POSIX filesystem semantics, `com.termux.shared.file.filesystem` provides a native bridging layer via `NativeDispatcher` and `FileAttributes`.

---

## Architectural Topology & Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                   FILESYSTEM DISPATCHER TOPOLOGY                       │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   Client Code (TermuxInstaller, FileUtils, RunCommandService)          │
│         │                                                              │
│         ▼                                                              │
│   FileAttributes.get(filePath, followLinks)                            │
│         │                                                              │
│         ├── followLinks == true  ──► NativeDispatcher.stat(path, attrs)│
│         └── followLinks == false ──► NativeDispatcher.lstat(path,attrs)│
│                                           │                            │
│                                           ▼                            │
│                                  android.system.Os                     │
│                                  (Os.stat / Os.lstat / Os.fstat)       │
│                                           │                            │
│                                           ▼                            │
│                                  Linux Kernel Syscalls                 │
│                                  stat(2), lstat(2), fstat(2)           │
│                                           │                            │
│                                           ▼                            │
│                                  android.system.StructStat             │
│                                           │                            │
│                                           ▼                            │
│                                  FileAttributes.loadFromStructStat()   │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## The `NativeDispatcher` Bridge

`NativeDispatcher` encapsulates direct interactions with the Android bionic libc subsystem through `android.system.Os`:

```java
public class NativeDispatcher {
    public static void stat(String filePath, FileAttributes fileAttributes) throws IOException;
    public static void lstat(String filePath, FileAttributes fileAttributes) throws IOException;
    public static void fstat(FileDescriptor fileDescriptor, FileAttributes fileAttributes) throws IOException;
}
```

### Syscall Differentiation
- **`stat`**: Follows symbolic links to report the attributes of the target file.
- **`lstat`**: Does not follow symbolic links, reporting the attributes of the symlink itself (crucial for bootstrap extraction, symlink integrity validation, and package relocation).
- **`fstat`**: Inspects an open, valid `FileDescriptor` directly.

Exceptions from the native OS layer (`android.system.ErrnoException`) are caught and wrapped in human-readable `java.io.IOException` instances preserving original errno error strings.

---

## POSIX File Attributes (`FileAttributes`)

`FileAttributes` mirrors the fields of the POSIX C `struct stat` structure:

### Low-Level Fields (`struct stat`)

| Field | Type | Description |
|---|---|---|
| **`st_mode`** | `int` | Bitmask encoding file type (`S_IFMT`) and POSIX permission bits (`0777`). |
| **`st_ino`** | `long` | Inode number unique within the device. |
| **`st_dev`** | `long` | Device ID hosting the filesystem inode. |
| **`st_rdev`** | `long` | Device ID for special block/character files. |
| **`st_nlink`** | `long` | Number of hard links pointing to the inode. |
| **`st_uid`** | `int` | POSIX User ID of file owner. |
| **`st_gid`** | `int` | POSIX Group ID of file group. |
| **`st_size`** | `long` | Total file size in bytes. |
| **`st_blksize`**| `long` | Preferred block size for filesystem I/O. |
| **`st_blocks`** | `long` | Number of 512-byte blocks allocated. |
| **`st_atime_sec / nsec`** | `long` | Last access timestamp (seconds & nanoseconds). |
| **`st_mtime_sec / nsec`** | `long` | Last modification timestamp (seconds & nanoseconds). |
| **`st_ctime_sec / nsec`** | `long` | Status/metadata change timestamp (seconds & nanoseconds). |

### File Identity (`FileKey`)
To uniquely identify files across the filesystem regardless of hard links or relative paths, `FileAttributes.fileKey()` generates a `FileKey` composite key:
$$\text{FileKey} = (\text{st\_dev}, \text{st\_ino})$$

---

## File Types & Permissions Parsing

### File Types (`FileType` & `FileTypes`)
File modes are decoded against POSIX bitmasks defined in `UnixConstants`:
- `isRegularFile()`: `(st_mode & S_IFMT) == S_IFREG`
- `isDirectory()`: `(st_mode & S_IFMT) == S_IFDIR`
- `isSymbolicLink()`: `(st_mode & S_IFMT) == S_IFLNK`
- `isCharacterDevice()`: `(st_mode & S_IFMT) == S_IFCHR`
- `isBlockDevice()`: `(st_mode & S_IFMT) == S_IFBLK`
- `isFifo()`: `(st_mode & S_IFMT) == S_IFIFO`
- `isSocket()`: `(st_mode & S_IFMT) == S_IFSOCK`

### POSIX Permissions (`FilePermissions`)
`FilePermissions` converts numeric octal permission masks (e.g., `0755`) to/from symbolic string representations (`rwxr-xr-x`):
- **User Bits**: `OWNER_READ` (0400), `OWNER_WRITE` (0200), `OWNER_EXECUTE` (0100)
- **Group Bits**: `GROUP_READ` (0040), `GROUP_WRITE` (0020), `GROUP_EXECUTE` (0010)
- **Others Bits**: `OTHERS_READ` (0004), `OTHERS_WRITE` (0002), `OTHERS_EXECUTE` (0001)

---

## Safe File Operations (`FileUtils`)

Building upon `NativeDispatcher` and `FileAttributes`, `FileUtils` enforces safe file manipulations:
1. **Atomic File Replacement**: Writing to `.tmp` files before renaming prevents data corruption on unexpected crashes.
2. **Directory Deletion with Symlink Safety**: Recursively traversing directories avoids following directory symlinks into external paths (`lstat` check before recursion).
3. **Permission Enforcement**: Checking and applying `chmod` equivalent modes on executable shell scripts and bootstrap staging trees.

---

## Invariants & Error Handling

- **Symlink Distinctions**: Operations validating Termux internal binaries or symlinks (such as `SYMLINKS.txt` resolution) MUST use `FileAttributes.get(path, false)` (`followLinks = false`) to inspect the symlink target rather than the dereferenced file.
- **Null Safety**: Empty or null paths immediately throw `IOException` before delegating to `Os.stat()`.
- **Descriptor Validity**: `fstat` verifies `fileDescriptor.valid()` prior to syscall dispatch.
