# UPMC

Universal Port Message Compiler is a desktop utility for composing, sending,
receiving, and inspecting messages over serial and TCP connections.

This repository is a clean import of the historical version 1.1.8 source from
2013. The original Eclipse and Subversion metadata, compiled classes, and old
build output are intentionally excluded.

## Run

Download the packaged JAR from a successful GitHub Actions run, then launch it
with a Java runtime:

```shell
java -jar upmc-1.1.8-SNAPSHOT.jar
```

No Maven installation or source checkout is required to run the packaged
application.

## Build

UPMC requires JDK 21 and Maven for the current development build. The
historical source has been compiled successfully with OpenJDK 21 on Debian 13.

```shell
mvn clean package
```

The standalone executable is written to
`target/upmc-1.1.8-SNAPSHOT.jar` and contains the JDOM and jSerialComm runtime
dependencies. Launch the locally built package with:

```shell
java -jar target/upmc-1.1.8-SNAPSHOT.jar
```

## Continuous integration

GitHub Actions builds and tests UPMC on every push and pull request. Each
successful run also smoke-tests the executable JAR and publishes it as the
`upmc-standalone` workflow artifact.

## Tested communication

UPMC uses jSerialComm for cross-platform serial-port access. Its platform
libraries are included in the Maven dependency, so RXTX and a custom
`java.library.path` are no longer required.

Serial communication has been tested successfully with a USB-to-serial adapter
and loopback connection. The TCP client and TCP server have also been tested
and continue to work as before. On Linux, the current user must have permission
to access the selected serial device; this commonly means membership in the
`dialout` group.

## License

UPMC is available under the [MIT License](LICENSE).
