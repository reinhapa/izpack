## Purpose

Compile-time static analysis gate for the IzPack build using Google Error Prone, covering how the analysis is wired into Maven, at what severity checks run, which JDKs are supported, and how the gate can be bypassed locally.

## ADDED Requirements

### Requirement: Error Prone runs on every compilation
The build SHALL execute Error Prone static analysis during compilation of all main and test sources of every module, via the Maven compiler plugin.

#### Scenario: Analysis runs on a normal build
- **WHEN** a developer or CI runs `./mvnw clean compile` (or `test-compile`) on JDK 21 or later
- **THEN** Error Prone analyzes every Java source file and reports findings as compilation diagnostics

#### Scenario: Error-severity findings fail the build
- **WHEN** Error Prone reports a finding at ERROR severity
- **THEN** the compilation fails and the build stops

### Requirement: Checks run at their default severities
Error Prone checks SHALL run at their built-in default severities, with no blanket severity overrides, so that checks Error Prone classifies as errors always gate the build.

#### Scenario: Default severity is preserved
- **WHEN** the build runs with no Error Prone flags beyond the plugin activation
- **THEN** each check uses the severity defined by the pinned Error Prone version

### Requirement: Known-good baseline
After this change is applied, a clean build of all modules SHALL produce zero Error Prone findings at ERROR severity on JDK 21 and JDK 25.

#### Scenario: Clean build passes the gate
- **WHEN** `./mvnw clean compile` runs on JDK 21 or JDK 25
- **THEN** the build succeeds with no Error Prone errors

### Requirement: JDK 21 is the minimum build JDK
The build SHALL require JDK 21 or later to run Error Prone; JDK 17 SHALL be removed from the CI build matrix. Artifacts SHALL still be compiled with `maven.compiler.release` set to 17 so they remain runnable on Java 17+ at runtime.

#### Scenario: CI builds only on supported JDKs
- **WHEN** the CI workflow runs
- **THEN** the build matrix contains JDK 21 and JDK 25; no job builds with JDK 17

#### Scenario: Bytecode compatibility is unchanged
- **WHEN** any module is compiled
- **THEN** the produced class files target Java 17 (major version 61)

### Requirement: Maven JVM grants javac-internal access
The Maven JVM SHALL be started with `--add-exports` and `--add-opens` flags for the `com.sun.tools.javac` packages that Error Prone accesses, configured in `.mvn/jvm.config` so that `./mvnw` applies them automatically.

#### Scenario: In-process javac does not crash
- **WHEN** `./mvnw compile` runs on JDK 21 or later
- **THEN** compilation proceeds without `IllegalAccessError` from the Error Prone plugin

### Requirement: Local opt-out from Error Prone
The build SHALL provide a Maven property (`errorprone.skip`) that disables Error Prone for a single invocation, so developers can trade analysis for compile speed locally.

#### Scenario: Opt-out skips the analysis
- **WHEN** a developer runs `./mvnw compile -Derrorprone.skip=true`
- **THEN** compilation succeeds without running Error Prone checks

#### Scenario: Default is enforced
- **WHEN** a build runs without `errorprone.skip` set
- **THEN** Error Prone runs (the opt-out is not implicitly active)
