# UPMC

Universal Port Message Compiler is a desktop utility for composing, sending,
receiving, and inspecting messages over serial and TCP connections.

This repository is a clean import of the historical version 1.1.8 source from
2013. The original Eclipse and Subversion metadata, compiled classes, and old
build output are intentionally excluded.

## Build

UPMC requires JDK 21 and Maven for the current development build. The
historical source has been compiled successfully with OpenJDK 21 on Debian 13.

```shell
mvn clean package
```

## Run for development

Launch the application from the project directory with:

```shell
mvn exec:java
```

The graphical interface starts on OpenJDK 21. The JAR currently produced in
`target/` is not yet a standalone distribution, so use Maven to launch this
historical baseline.

## Current limitations

UPMC uses jSerialComm for cross-platform serial-port access. Its platform
libraries are included in the Maven dependency, so RXTX and a custom
`java.library.path` are no longer required.

Port enumeration and serial-settings translation have been tested on OpenJDK
21. End-to-end serial communication still requires testing with physical or
loopback hardware. On Linux, the current user must have permission to access
the selected serial device; this commonly means membership in the `dialout`
group.

## License

UPMC is available under the [MIT License](LICENSE).
