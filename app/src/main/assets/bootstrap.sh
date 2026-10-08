#!/bin/sh
# IDEAS runtime bootstrap (runs inside the Alpine/PRoot rootfs).
# Installs Node.js and npm. Idempotent: safe to run again.
set -e

echo "[bootstrap] updating apk index"
/sbin/apk update

echo "[bootstrap] installing nodejs npm"
/sbin/apk add --no-cache nodejs npm

echo "[bootstrap] node: $(node -v)"
echo "[bootstrap] npm:  $(npm -v)"
echo "[bootstrap] done"
