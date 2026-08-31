## Why

IzPack's build has no compile-time static analysis: the compiler plugin runs javac with no checks beyond javac itself. Google Error Prone plugs into javac and catches common programming mistakes at compile time — broken `equals`/`hashCode` contracts, locks on boxed primitives, raw `Comparable`, legacy `Hashtable` API misuse — before they ship. A sandboxed dry-run against the current codebase (Error Prone 2.50.0, JDK 21/25) already found 4 such defects in main sources and 2 in test sources, plus ~2,800 lower-severity findings across main and test sources that give the project a concrete escalation backlog — much of it mechanical to clear, since most of these checks carry Error Prone suggested fixes for automated patching. Error Prone ≥ 2.43 requires a JDK 21+ runtime to execute, so adopting a current, maintained Error Prone means dropping the JDK 17 leg from the CI build matrix (the produced bytecode still targets Java 17).

## What Changes

- Add Google Error Prone to the Maven build via `maven-compiler-plugin` (`annotationProcessorPaths` + `-Xplugin:ErrorProne`), active on every `compile` and `testCompile`
- Add `.mvn/jvm.config` granting the `--add-exports`/`--add-opens` flags Error Prone needs to reach `com.sun.tools.javac.*` internals when javac runs in-process inside the Maven JVM (without it, every module fails with `IllegalAccessError` on JDK 17/21/25)
- Pin `error_prone_core` 2.50.0 (2.47.0 has a known crash on JDK 21/25, google/error-prone#5493)
- **BREAKING (build infrastructure only)**: remove JDK 17 from the CI build matrix; builds run on JDK 21 and JDK 25. `maven.compiler.release` stays 17 — artifacts still run on Java 17+
- Fix the 4 default-severity errors in main sources (`ComparableType`, `EqualsHashCode`, `LockOnBoxedPrimitive` ×2, `HashtableContains`) and resolve the 2 default-severity errors in test sources (`SelfAssertion`, `IdentityBinaryExpression`)
- Add an opt-out property so developers can skip Error Prone locally to avoid the ~4× clean-compile overhead (13s → ~55s measured)
- Defer NullAway (ships inside `error_prone_core` 2.47+) to a possible phase 2; it needs nullness annotations on key APIs first and is out of scope here

## Capabilities

### New Capabilities
- `errorprone`: the compile-time static analysis gate — how Error Prone is wired into the Maven build, which checks run at which severity, the JDK requirements, the JVM exports prerequisite, and the opt-out mechanism

### Modified Capabilities
<!-- none: this project has no existing specs; all changes are new-build-tooling behavior captured under the new capability -->

## Impact

- Root `pom.xml`: `error-prone.version` property, `maven-compiler-plugin` configuration in `pluginManagement`
- New `.mvn/jvm.config` (10 `--add-exports`/`--add-opens` flags)
- `.github/workflows/maven.yml`: JDK matrix `['17', '21']` → `['21', '25']`
- `renovate.json`: Error Prone updates pinned to no auto-merge (2.47.0 crash precedent)
- Bug fixes in 4 main-source files (`izpack-util`, `izpack-api`, `izpack-panel`, `izpack-compiler`) and 1–2 test files (`izpack-tools`, `izpack-core`)
- Builds must go through `./mvnw` (or equivalent `MAVEN_OPTS`) to pick up `.mvn/jvm.config`; the CI workflow and the shipped wrapper already do
- Clean-compile wall time rises ~4×; mitigated by the opt-out property and incremental compilation
- No annotation processors exist in the project, so switching to `annotationProcessorPaths` does not suppress any processor discovery
