# Files

- [Native JNI & Pseudoterminal (PTY) Subsystem](native-jni-and-pty.md) - Architecture and low-level mechanics of native PTY subprocess spawning, termios configuration, and local Unix domain socket credential verification.
- [Settings & Properties Architecture](settings-and-preferences.md) - Dual configuration subsystem spanning Android SharedPreferences and file-based termux.properties with live reload broadcast mechanics.
- [Subprocess IPC & Android Manager Socket Server](ipc-and-socket-server.md) - Native AF_UNIX domain socket server architecture, peer credential validation, JNI integration, and high-performance am intent execution without JVM startup overhead.
- [Terminal View & Rendering](rendering-and-view.md) - UI rendering, touch gestures, touch input handling, and text selection in terminal-view.
- [Terminal Emulation Engine](terminal-emulation.md) - Core terminal emulation engine, buffer management, character width calculations, and escape sequence processing.
- [Virtual Keyboard & Extra Keys Toolbar](virtual-keyboard-and-extra-keys.md) - Architecture of the virtual extra keys bar, touch gestures, special modifier buttons, termux.properties matrix configuration, and terminal input dispatching.
