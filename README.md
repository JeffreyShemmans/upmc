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

UPMC still uses the historical RXTX serial-port library. Maven supplies its
Java library, but RXTX also requires a platform-specific native library such
as `librxtxSerial.so` on Linux. Opening Port Setup without that library produces
an `UnsatisfiedLinkError`.

If a compatible RXTX native library is already installed, its directory can be
provided when starting Maven:

```shell
MAVEN_OPTS="-Djava.library.path=/path/to/rxtx/native-libraries" mvn exec:java
```

The original application has been tested using an RXTX 2.2pre2 Linux native
library with the RXTX 2.1-7 Java library. Port enumeration worked, but RXTX
reported a version-mismatch warning. Serial communication has not yet been
validated on modern Linux. Replacing RXTX with a maintained serial library is
planned; until then, serial support should be considered experimental.

## License

UPMC is available under the [MIT License](LICENSE).
