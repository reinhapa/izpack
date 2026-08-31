## 1. Build wiring

- [ ] 1.1 Add `error-prone.version` property (2.50.0) and configure `maven-compiler-plugin` in root `pom.xml` `pluginManagement`: `annotationProcessorPaths` with `error_prone_core`, plus `compilerArgs` (`-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`, `-Xplugin:ErrorProne`, `-XDaddTypeAnnotationsToSymbol=true`, `-Xmaxwarns 10000`). Verify: `./mvnw -pl izpack-util compile` on JDK 21 emits Error Prone diagnostics carrying check names (e.g. `[ComparableType]`)
- [ ] 1.2 Create `.mvn/jvm.config` with the 10 `--add-exports`/`--add-opens` flags for `com.sun.tools.javac.{api,file,main,model,parser,processing,tree,util}` and `com.sun.tools.javac.{code,comp}`. Verify: `./mvnw clean compile` on JDK 21 and JDK 25 no longer fails with `IllegalAccessError` from the Error Prone plugin
- [ ] 1.3 Downgrade the two test-source error checks by appending `-Xep:SelfAssertion:WARN -Xep:IdentityBinaryExpression:WARN` to the `-Xplugin:ErrorProne` arg (design D5). Verify: `./mvnw clean test-compile` proceeds past `izpack-tools` and `izpack-core`

## 2. Fix main-source errors

- [ ] 2.1 Fix `ComparableType` in `izpack-util` `Resource.java`: parameterize the raw `Comparable` as `Comparable<Resource>`. Verify: no `ComparableType` diagnostic and `izpack-util` tests pass
- [ ] 2.2 Fix `EqualsHashCode` in `izpack-api` `Pack.java`: add a `hashCode()` consistent with `equals()`. Verify: no `EqualsHashCode` diagnostic and existing `Pack` tests pass
- [ ] 2.3 Fix `LockOnBoxedPrimitive` (2 sites) in `izpack-panel` `ProcessPanelWorker.java`: replace `synchronized` on boxed primitives with a dedicated final lock object. Verify: no `LockOnBoxedPrimitive` diagnostic and process-panel tests pass
- [ ] 2.4 Fix `HashtableContains` in `izpack-compiler` `CompilerConfig.java`: replace legacy `Hashtable.contains()` with `containsValue()`. Verify: no `HashtableContains` diagnostic and compiler tests pass

## 3. Test-source findings

- [ ] 3.1 Review the `SelfAssertion` finding in `izpack-tools` tests; fix the self-asserting test or suppress with an explanatory comment. Verify: the test still asserts meaningful behavior
- [ ] 3.2 Review the `IdentityBinaryExpression` findings in `RulesEngineImplTest` (`izpack-core`); determine whether the `x == x` / `x != x` comparisons are intentional rules-engine test data; fix or suppress with explanation. Verify: `izpack-core` tests pass

## 4. Local opt-out

- [ ] 4.1 Add an `errorprone.skip` Maven profile that overrides `compilerArgs` (`combine.self="override"`) so `-Xplugin:ErrorProne` is removed when `-Derrorprone.skip=true` is set (design D6). Verify: `./mvnw compile -Derrorprone.skip=true` succeeds with no Error Prone diagnostics, while a default build still runs the checks

## 5. CI matrix and version pinning

- [ ] 5.1 Update `.github/workflows/maven.yml`: build matrix `jdk: ['17', '21']` → `jdk: ['21', '25']`. Verify: the workflow contains no JDK 17 job and both the 21 and 25 legs run the full compile
- [ ] 5.2 Configure `renovate.json` so `error_prone_core` updates are not auto-merged (design D7, 2.47.0 regression precedent). Verify: renovate config restricts auto-merge for the Error Prone dependency

## 6. Verification

- [ ] 6.1 Full clean build on JDK 21: `./mvnw clean install -DskipTests` green with zero Error Prone errors
- [ ] 6.2 Full clean build on JDK 25: `./mvnw clean install -DskipTests` green with zero Error Prone errors
- [ ] 6.3 Verify produced class files still target Java 17 (major version 61) on a compiled module, confirming runtime compatibility is unchanged
- [ ] 6.4 Measure clean-compile wall time with and without `-Derrorprone.skip=true` and record the overhead for the team

## 7. Documentation

- [ ] 7.1 Document the Error Prone adoption in the README/contributing docs: checks run at compile time, builds must use `./mvnw` (for `.mvn/jvm.config`), and the `-Derrorprone.skip=true` opt-out. Verify: all three points are documented
