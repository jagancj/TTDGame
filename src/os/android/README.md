# OpenTTD Android OS Abstraction Layer

## Architecture Overview

OpenTTD uses a modular OS abstraction layer structured under `src/os/`:
- `src/os/macosx/` - Apple macOS backend (Objective-C++, Cocoa)
- `src/os/unix/` - POSIX Unix backend (Linux, BSD, Haiku, Android)
- `src/os/windows/` - Win32 backend

### Android Platform Integration

Android is built on top of the Linux kernel and a POSIX-compliant C library (Bionic). Consequently:
1. **Core OS Operations:** OpenTTD leverages the POSIX abstractions in `src/os/unix/` (e.g. `unix_main.cpp`, `unix.cpp`, `crashlog_unix.cpp`, and `library_loader_unix.cpp`).
2. **Audio, Video, and Input:** Provided through **SDL (SDL3 / SDL2)**. SDL wraps Android JNI, SurfaceView/NativeActivity, OpenGL ES rendering, and touch events into standard SDL events.
3. **Application Entry Point:** In Android, `SDLActivity` loads the compiled shared library (`libopenttd.so` or `libmain.so`) and invokes `SDL_main`, which redirects to `main()` in `src/os/unix/unix_main.cpp`.
4. **File Storage & Basesets:** On Android, bundled base graphics (OpenGFX) and fonts packaged in the APK's `assets/baseset/` are extracted by `MainActivity` to the application's internal files directory (`filesDir/baseset/`) so OpenTTD's native C++ POSIX file I/O can immediately locate them.

