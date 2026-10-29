#!/usr/bin/env bash
# Costruisce il servizio radio corretto partendo dall'APK ORIGINALE della tua auto.
#
#   ./build.sh /percorso/RadioService_eh32_eu_P.apk
#
# Requisiti: Java 11+ (java, javac, jar, jarsigner, keytool), git, curl, unzip, zip, python3, openssl.
# Gli strumenti (apktool, dex2jar, android.jar, chiave AOSP) vengono scaricati in ./tools al primo avvio.
#
# Lo script si FERMA (senza produrre nulla) se:
#   - l'APK non e' il servizio radio EH32 (pacchetto com.saicmotor.service.radio)
#   - l'APK originale non e' firmato con la chiave di piattaforma AOSP (l'aggiornamento non si installerebbe)
#   - il codice del tuo firmware e' diverso da quello previsto (un hook non trova il punto da modificare)
set -euo pipefail

IN="${1:?Uso: ./build.sh RadioService_originale.apk}"
HERE="$(cd "$(dirname "$0")" && pwd)"
T="$HERE/tools"
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

# --------------------------------------------------------------- controlli sull'APK originale
say "Controllo l'APK originale"
FP="$(keytool -printcert -jarfile "$IN" 2>/dev/null | awk '/SHA256:/{print $2; exit}')"
if [ "$FP" != "$AOSP_FP" ]; then
  echo "STOP: l'APK originale e' firmato con un'altra chiave ($FP)."
  echo "      Un aggiornamento firmato con la chiave AOSP non si installerebbe sulla tua auto."
  exit 1
fi
java -jar "$T/apktool.jar" -q d -f -o "$WORK/src" "$IN"
PKG="$(grep -o 'package="[^"]*"' "$WORK/src/AndroidManifest.xml" | cut -d'"' -f2)"
if [ "$PKG" != "com.saicmotor.service.radio" ]; then
  echo "STOP: questo APK e' '$PKG', non il servizio radio EH32 (com.saicmotor.service.radio)."
  echo "      Se la tua radio e' un'unica app 'com.saicmotor.radio' hai il firmware LL: questa patch non fa per te."
  exit 1
fi
VER="$(awk '/versionName:/{print $2}' "$WORK/src/apktool.yml")"
echo "Pacchetto: $PKG   versione: $VER"

# --------------------------------------------------------------- hook smali
say "Applico le modifiche"
python3 -I "$HERE/patch_smali.py" "$WORK/src"
sed -i "s/^  versionName: .*/  versionName: ${VER}-radiofix5/" "$WORK/src/apktool.yml"

# --------------------------------------------------------------- codice RadioFix
say "Compilo RadioFix"
"$T/dex2jar/d2j-dex2jar.sh" -f -o "$WORK/app.jar" "$IN" >/dev/null 2>&1
mkdir -p "$WORK/stub" "$WORK/cls"
javac --release 8 -nowarn -d "$WORK/stub" "$HERE"/stub/android/hardware/radio/*.java
javac --release 8 -nowarn -cp "$WORK/stub:$T/android.jar:$WORK/app.jar" -d "$WORK/cls" \
  "$HERE/src/com/saicmotor/service/radio/fix/RadioFix.java"
(cd "$WORK/cls" && jar cf "$WORK/radiofix.jar" com)
"$T/dex2jar/d2j-jar2dex.sh" -f -o "$WORK/radiofix.dex" "$WORK/radiofix.jar" >/dev/null 2>&1

# --------------------------------------------------------------- ricostruzione e firma
say "Ricostruisco e firmo l'APK"
java -jar "$T/apktool.jar" -q b "$WORK/src" -o "$WORK/out.apk"
LIST="$(unzip -Z1 "$WORK/out.apk")"
BEFORE="$(grep -cE '^classes[0-9]*\.dex$' <<<"$LIST")"
N=2; while grep -qx "classes$N.dex" <<<"$LIST"; do N=$((N+1)); done
mkdir -p "$WORK/z" && cp "$WORK/radiofix.dex" "$WORK/z/classes$N.dex"
(cd "$WORK/z" && zip -q "$WORK/out.apk" "classes$N.dex")
AFTER="$(unzip -Z1 "$WORK/out.apk" | grep -cE '^classes[0-9]*\.dex$')"
[ "$AFTER" -eq $((BEFORE + 1)) ] || { echo "STOP: errore nell'aggiunta di classes$N.dex"; exit 1; }
echo "Codice RadioFix aggiunto come classes$N.dex"
OUT="$(pwd)/RadioService_radiofix5_${VER}.apk"
cp "$WORK/out.apk" "$OUT"
jarsigner -keystore "$T/platform.p12" -storetype PKCS12 -storepass android \
  -sigalg SHA256withRSA -digestalg SHA-256 "$OUT" platform >/dev/null 2>&1
jarsigner -verify "$OUT" >/dev/null 2>&1 || { echo "STOP: firma non valida"; exit 1; }

say "Fatto: $OUT"
echo "Installa con:  adb install -r \"$(basename "$OUT")\"   e poi RIAVVIA l'unita'."
