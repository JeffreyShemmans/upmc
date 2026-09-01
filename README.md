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

The v1.2.0 standalone JAR and platform launchers have been tested successfully
on Debian 13 and Windows 10. Windows 10 validation on 1 September 2026 included
launching with `upmc.bat`, detecting an FTDI Quad RS-232-HS USB-to-serial
adapter (`0403:6011`), and communicating through its serial ports. Serial
communication has also been tested on Linux with a USB-to-serial adapter and
loopback connection.

The TCP client and TCP server continue to work on both platforms. On Linux, the
current user must have permission to access the selected serial device; this
commonly means membership in the `dialout` group.

## License

UPMC is available under the [MIT License](LICENSE).
