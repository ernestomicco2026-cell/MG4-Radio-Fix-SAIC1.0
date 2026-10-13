#!/usr/bin/env bash
# Costruisce "Radio Fix Remover": piccola app che toglie la patch e riporta il servizio radio di fabbrica,
# come `adb uninstall com.saicmotor.service.radio`, ma installabile da chiavetta USB (senza ADB).
#
#   ./build.sh            -> RadioFixRemover_<versione>.apk nella cartella corrente
#
# Requisiti: Java 11+ (javac, jar, jarsigner, keytool), git, curl, unzip, openssl.
# Usa gli stessi strumenti di patcher/build.sh (scaricati in patcher/tools al primo avvio).
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
T="$HERE/../patcher/tools"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$T"

APKTOOL_VER=2.9.3
AOSP_FP="C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8"
say() { printf '\n==> %s\n' "$*"; }

# --------------------------------------------------------------- strumenti
if [ ! -f "$T/apktool.jar" ]; then
  say "Scarico apktool $APKTOOL_VER"
  curl -sSL -o "$T/apktool.jar" "https://github.com/iBotPeaches/Apktool/releases/download/v$APKTOOL_VER/apktool_$APKTOOL_VER.jar"
fi
if [ ! -d "$T/dex2jar" ]; then
  say "Scarico dex2jar"
  curl -sSL -o "$T/d2j.zip" https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip
  unzip -q "$T/d2j.zip" -d "$T/d2j_tmp" && mv "$T"/d2j_tmp/* "$T/dex2jar" && rm -rf "$T/d2j_tmp" "$T/d2j.zip"
  chmod +x "$T"/dex2jar/*.sh
fi
if [ ! -f "$T/android.jar" ]; then
  say "Scarico android.jar (API 28)"
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Sable/android-platforms.git "$T/ap"
  (cd "$T/ap" && git sparse-checkout set android-28)
  cp "$T/ap/android-28/android.jar" "$T/android.jar" && rm -rf "$T/ap"
fi
if [ ! -f "$T/platform.p12" ]; then
  say "Scarico la chiave di piattaforma pubblica AOSP (test-key)"
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/aosp-mirror/platform_build.git "$T/pb"
  (cd "$T/pb" && git sparse-checkout set target/product/security)
  S="$T/pb/target/product/security"
  openssl pkcs8 -inform DER -nocrypt -in "$S/platform.pk8" -out "$T/platform.key"
  openssl pkcs12 -export -in "$S/platform.x509.pem" -inkey "$T/platform.key" -name platform \
    -passout pass:android -out "$T/platform.p12"
  rm -rf "$T/pb" "$T/platform.key"
fi

# --------------------------------------------------------------- compilazione
say "Compilo"
mkdir -p "$WORK/cls" "$WORK/apk"
javac --release 8 -nowarn -cp "$T/android.jar" -d "$WORK/cls" \
  "$HERE"/src/com/ernestomicco/radiofixremover/*.java
(cd "$WORK/cls" && jar cf "$WORK/app.jar" com)
cp -r "$HERE/AndroidManifest.xml" "$HERE/apktool.yml" "$HERE/res" "$WORK/apk/"
"$T/dex2jar/d2j-jar2dex.sh" -f -o "$WORK/apk/classes.dex" "$WORK/app.jar" >/dev/null 2>&1
[ -s "$WORK/apk/classes.dex" ] || { echo "STOP: classes.dex non creato"; exit 1; }

# --------------------------------------------------------------- APK e firma
say "Costruisco e firmo l'APK"
VER="$(awk -F"'" '/versionName:/{print $2}' "$HERE/apktool.yml")"
OUT="$(pwd)/RadioFixRemover_${VER}.apk"
java -jar "$T/apktool.jar" -q b "$WORK/apk" -o "$OUT"
jarsigner -keystore "$T/platform.p12" -storetype PKCS12 -storepass android \
  -sigalg SHA256withRSA -digestalg SHA-256 "$OUT" platform >/dev/null 2>&1
jarsigner -verify "$OUT" >/dev/null 2>&1 || { echo "STOP: firma non valida"; exit 1; }
FP="$(keytool -printcert -jarfile "$OUT" 2>/dev/null | awk '/SHA256:/{print $2; exit}')"
[ "$FP" = "$AOSP_FP" ] || { echo "STOP: firmato con una chiave diversa ($FP)"; exit 1; }

say "Fatto: $OUT"
