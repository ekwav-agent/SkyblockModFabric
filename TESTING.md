# Testing

This project uses JUnit 5 for pure client-logic regression tests. The build requires JDK 25.

Run the unit tests locally with:

```sh
JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64 ./gradlew --no-daemon test
```

The `testserver` subproject is included in both `test` and `build`. Its tests
lock the schema-1 scenario order, exact `main` branch dependency tuple, stable
observation labels, and the production `MenuClassifier`/`ScoreboardParser`
seams. `verifyScenarioJar` also rejects any resource outside the minimal
manifest, `fabric.mod.json`, owned scenario classes, and scenario index.

The resulting server-side scenario mod is
`testserver/build/libs/skycofl-scenario-server.jar`. It is not a Minecraft or
Fabric runtime and cannot be run standalone; licensed runtime validation is a
separate host-owned gate.

`CorePurityTest` scans both the Java sources and compiled classes in `com.coflnet.core`. It fails if any core class references `net.minecraft` or `com.mojang`, keeping the extracted logic runnable without a Minecraft client.

## Regression baseline

The reserved comparison command loads a real response through the loopback description backend, then
selects a different container title through the production adapter. It compiles and executes against
pinned base `328d211cbf4dd9d5b9082b476af20d55f05c12b5`, where it fails because container initialization
returns the previous global info display. The patch returns an empty title-scoped cache miss and passed
locally. The trusted host, not the mutable implementation workspace, owns the exact-base execution and
evidence. Position-sensitive deduplication and evicted-display retry remain covered by the ordinary
focused state tests.
