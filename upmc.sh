#!/bin/sh

APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
UPMC_JAR=

for candidate in "$APP_DIR"/upmc-*.jar; do
    if [ ! -f "$candidate" ]; then
        continue
    fi

    if [ -n "$UPMC_JAR" ]; then
        message="More than one UPMC application file was found in $APP_DIR. Keep only the version you want to run."
        if command -v zenity >/dev/null 2>&1; then
            zenity --error --title="UPMC" --text="$message"
        else
            echo "$message" >&2
        fi
        exit 1
    fi

    UPMC_JAR=$candidate
done

if ! command -v java >/dev/null 2>&1; then
    message="Java is not installed or is not available on PATH. Install Java, then try UPMC again."
    if command -v zenity >/dev/null 2>&1; then
        zenity --error --title="UPMC" --text="$message"
    else
        echo "$message" >&2
    fi
    exit 127
fi

if [ -z "$UPMC_JAR" ]; then
    message="No UPMC application file (upmc-*.jar) was found in $APP_DIR."
    if command -v zenity >/dev/null 2>&1; then
        zenity --error --title="UPMC" --text="$message"
    else
        echo "$message" >&2
    fi
    exit 1
fi

exec java -Xmx384m -Duser.country=US -Duser.language=en -jar "$UPMC_JAR" "$@"
