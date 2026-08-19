# Testing

This project uses JUnit 5 for pure client-logic regression tests. The build requires JDK 25.

Run the unit tests locally with:

```sh
JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64 ./gradlew --no-daemon test
```

`CorePurityTest` scans both the Java sources and compiled classes in `com.coflnet.core`. It fails if any core class references `net.minecraft` or `com.mojang`, keeping the extracted logic runnable without a Minecraft client.

## Regression baseline

On the pinned base commit these tests fail during `compileTestJava`, because the asserted `com.coflnet.core` classes do not exist and the logic is still inline in `CoflModClient`. With this extraction, all eight regression classes compile and pass under `./gradlew --no-daemon test`.
