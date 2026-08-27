# UPMC

Universal Port Message Compiler is a desktop utility for composing, sending,
receiving, and inspecting messages over serial and TCP connections.

This repository is a clean import of the historical version 1.1.8 source from
2013. The original Eclipse and Subversion metadata, compiled classes, and old
build output are intentionally excluded.

## Build

UPMC requires JDK 21 and Maven for the current development build.

```shell
mvn clean package
```

The application currently retains its historical RXTX serial-port dependency.
Serial operation has not yet been validated on modern Linux systems.

## License

UPMC is available under the [MIT License](LICENSE).
