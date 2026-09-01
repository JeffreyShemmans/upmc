# UPMC

Universal Port Message Compiler is a desktop utility for composing, sending,
receiving, and inspecting messages over serial and TCP connections.

This repository is a clean import of the historical version 1.1.8 source from
2013. The original Eclipse and Subversion metadata, compiled classes, and old
build output are intentionally excluded.

## Run

Download `upmc-1.2.0.jar` and the launcher for your operating system from the
latest GitHub Release. Keep the two files in the same folder.

On Linux, make the launcher executable once and run it:

```shell
chmod +x upmc.sh
./upmc.sh
```

On Windows, double-click `upmc.bat` or run it from Command Prompt:

```batch
upmc.bat
```

You can also launch the JAR directly with a Java runtime:

```shell
java -jar upmc-1.2.0.jar
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
`target/upmc-1.2.0.jar` and contains the JDOM and jSerialComm runtime
dependencies. Launch the locally built package with:

```shell
java -jar target/upmc-1.2.0.jar
```

## Continuous integration

GitHub Actions builds and tests UPMC for pull requests and pushes to `main`.
The workflow can also be run manually against any branch from the Actions tab.
Each successful run smoke-tests the executable JAR and publishes it with the
Linux and Windows launchers as the `upmc-standalone` workflow artifact. Pushing
a version tag such as `v1.2.0` builds the same source and publishes the JAR and
both launchers as GitHub Release downloads.

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
