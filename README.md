# Slang Gradle Plugin

A simple Gradle plugin to add Slang shading language support.

# Requirements

- Gradle 9.0 or higher, running on Java 17 or higher.
- `slangc`: either downloaded by the plugin at a pinned version (recommended) or taken from `PATH`.
- `spirv-reflect` on `PATH` only when `generateReflectionInfo` is enabled. The Vulkan SDK ships it.

# Pinned compiler

```kotlin
slang {
    compilerOptions {
        downloadCompiler(
            version = "2026.19",
            sha256 = mapOf(
                "windows-x86_64" to "<sha256 of slang-2026.19-windows-x86_64.zip>",
                "linux-x86_64" to "<sha256 of slang-2026.19-linux-x86_64.tar.gz>",
            ),
        )
    }
}
```

The archive comes from the official GitHub release, is verified against the hash for the current host, and is
cached under `<gradle user home>/caches/slang-gradle-plugin`. GitHub shows each asset's SHA-256 on the release
page. Without `compilerVersion`, the plugin uses `compilerExecutable` or `slangc` on `PATH`.