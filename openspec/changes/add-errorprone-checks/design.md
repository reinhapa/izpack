## Context

See proposal.md for motivation. Key facts established by a sandboxed dry-run (repo copied to /tmp, Error Prone wired into the copy only — the real repo was untouched):

- maven-compiler-plugin 3.16.0 is already pinned; no `compilerArgs` are set today
- Stock Error Prone 2.50.0 on JDK 21/25: **2,348 warnings and only 4 error-severity findings** across ~1,000 main sources; test sources add ~488 warnings and 2 error-severity findings
- Error Prone runs javac **in-process** inside the Maven JVM and needs 10 `--add-exports`/`--add-opens` flags for `com.sun.tools.javac.*`, otherwise every module dies with `IllegalAccessError` (verified on JDK 17, 21 and 25)
- Error Prone class-file compatibility: ≤ 2.42.0 runs on JDK 17+, ≥ 2.43.0 requires JDK 21+ runtime; 2.47.0 has a crash regression on JDK 21/25 (google/error-prone#5493); 2.50.0 is current and clean
- Clean-compile wall time: 13.5s baseline → ~54s with Error Prone (~4×)
- The project uses no annotation processors, so moving to `annotationProcessorPaths` suppresses nothing
- Spotless already ratchets from `origin/master`; OWASP dependency-check and JaCoCo are unaffected

## Goals / Non-Goals

**Goals:**
- Gate the build on Error Prone's default-severity errors with a zero-error baseline on day one
- Keep the gate on in CI (JDK 21 and JDK 25 build legs) and in IDEs
- Make the ~4× compile-time cost opt-out-able for local development
- Fix the 6 real defects Error Prone already found (4 main, 2 test)

**Non-Goals:**
- Raising the runtime baseline (`maven.compiler.release` stays 17; only the *build* JDK matrix changes)
- Enabling NullAway (ships inside error_prone_core 2.47+; needs API annotation — possible phase 2)
- Adopting Checkstyle/SpotBugs/PMD
- Escalating any of the ~2,800 warnings to ERROR in this change (backlog for follow-ups)
- Ratcheting Error Prone to only-changed-files (Error Prone has no native ratchet; not needed with a zero-error baseline)

## Decisions

### D1: Error Prone 2.50.0, JDK matrix drops 17, builds on 21 and 25
Pinning `error_prone_core` 2.50.0 and removing JDK 17 from CI. 2.50.0 is the current release, carries ~60 more findings than 2.42.0, and is verified to build the whole project on JDK 21 and 25. The build matrix becomes `['21', '25']`: JDK 25 is already used by the deploy/checks jobs, and a dedicated 25 build leg is cheap insurance against JDK-specific findings (Error Prone behavior can vary with the running JDK). Alternative considered: 2.42.0 + keeping the JDK 17 leg — rejected because 2.42 is a stale check set and the user chose to drop JDK 17; keeping a second, analysis-free CI leg would also be misleading. Bytecode target stays 17, so runtime compatibility is unchanged.

### D2: Official Maven integration shape
`annotationProcessorPaths` (error_prone_core) + `compilerArgs` (`-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`, `-Xplugin:ErrorProne`, `-XDaddTypeAnnotationsToSymbol=true` for JDK 21+, `-Xmaxwarns 10000` so per-module output isn't truncated at 100). This is the documented integration and requires no forking. Alternative: `<fork>true</fork>` with `-J--add-exports` args — rejected: forks a JVM per module compilation (slower) and the exports would still be needed.

### D3: `.mvn/jvm.config` for the javac-internal exports
The 10 `--add-exports`/`--add-opens` flags live in `.mvn/jvm.config`, which `./mvnw` applies to the Maven JVM automatically (javac runs in-process inside it). Alternatives: `MAVEN_OPTS` (not enforceable in CI without workflow edits) and forking (D2). Caveat accepted: raw `mvn` from PATH won't see the file — the project ships the wrapper and CI uses it; to be documented in the README.

### D4: Severity policy — stock defaults, errors gate, warnings inform
No blanket flags. The 4 main-source errors (`ComparableType` in izpack-util `Resource.java`, `EqualsHashCode` in izpack-api `Pack.java`, `LockOnBoxedPrimitive` ×2 in izpack-panel `ProcessPanelWorker.java`, `HashtableContains` in izpack-compiler `CompilerConfig.java`) are fixed. The ~2,800 warnings remain visible but non-gating; a curated escalation list (e.g. `ReferenceEquality`, `EmptyCatch`, `CatchAndPrintStackTrace`, `ClassNewInstance`, `WaitNotInLoop`, `UnsynchronizedOverridesSynchronized`) can be promoted to ERROR in follow-ups. Those follow-ups are largely mechanical and can reuse Error Prone's suggested-fixes patching (`-XepPatchChecks` / `-XepPatchLocation`, e.g. for `MissingOverride`, `UnnecessaryParentheses`) to auto-apply fixes — parked as a separate cleanup pass, not part of this change. Alternative: `-XepAllErrorsAsWarnings` + `failOnWarning=true` — rejected: it would gate on ~2,800 warnings immediately.

### D5: Test-source analysis stays on, with two checks downgraded
The same plugin configuration applies to `testCompile`, and test-source analysis remains enabled (confirmed scope, not optional). `SelfAssertion` (izpack-tools) and `IdentityBinaryExpression` (izpack-core `RulesEngineImplTest`) are the error-severity findings in tests; `IdentityBinaryExpression` hits `x == x` / `x != x` comparisons that may be intentional rules-engine test data, so both checks are set to WARN in this change and the underlying code is reviewed case-by-case (tasks 3.1, 3.2). `ComplexBooleanConstant` (162) and `OperatorPrecedence` (108) dominate test warnings and are left informational.

### D6: Opt-out via `errorprone.skip` property
A Maven profile activated by `-Derrorprone.skip=true` overrides `compilerArgs` (via `combine.self="override"`), removing `-Xplugin:ErrorProne` so no analysis runs; `annotationProcessorPaths` stays but is inert without the plugin flag. Default remains "analysis on".

### D7: Pin the version against auto-updates
The project uses renovate.json; the Error Prone dependency must not be auto-merged without a green build (2.47.0 showed a silent crash regression). Renovate should be configured to not auto-merge `error_prone_core` updates.

## Risks / Trade-offs

- **[~4× clean-compile time]** → opt-out property (D6); incremental compiles are far cheaper; CI cost is acceptable (~55s)
- **[Error Prone version regressions]** (2.47.0 precedent) → pin exact version, no auto-merge (D7), full build verification on upgrade
- **[`.mvn/jvm.config` only honored via `./mvnw`]** → document in README; IntelliJ applies `.mvn/jvm.config` when it delegates to the wrapper
- **[Intentional test patterns flagged]** (e.g. `x == x` in rules tests) → downgraded to WARN (D5); `@SuppressWarnings` with explanation where intentional
- **[Future JDKs (26+) may break Error Prone]** → track Error Prone releases; the JDK matrix change is isolated to one workflow line
- **[JDK 17 leg removal is a breaking build-infra change]** → bytecode still targets 17; only build/test coverage of the JDK 17 JVM is dropped
- **[Two-JDK matrix doubles CI compile cost]** → the 25 leg reuses the same checkout and cached Maven repo; measured clean compile is ~55s, acceptable for the coverage gain

## Migration Plan

Single PR, build-configuration only (no runtime behavior change):

1. Wire plugin config + `.mvn/jvm.config` + `error-prone.version` property into root `pom.xml`
2. Fix the 4 main-source errors; resolve the 2 test-source errors (fix or suppress with explanation)
3. Add the `errorprone.skip` opt-out profile
4. Update `.github/workflows/maven.yml` matrix to `['21', '25']`
5. Verify: `./mvnw clean compile` and `test-compile` green on JDK 21 and 25; `-Derrorprone.skip=true` path verified
6. Rollback: revert the PR — no data, API, or artifact changes

## Open Questions

None — all questions from the exploration were resolved: the CI matrix gains a JDK 25 leg (D1), test-source analysis stays on (D5), and NullAway is deferred to a possible phase 2 (Non-Goals).
