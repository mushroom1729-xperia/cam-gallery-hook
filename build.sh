#!/bin/bash
# Manual (Gradle-free) build of the CamGalleryHook LSPosed module APK.
set -euo pipefail
cd "$(dirname "$0")"

TOOLS=${TOOLS:-~/workspace/tools}
export JAVA_HOME=$TOOLS/jdk
export PATH=$JAVA_HOME/bin:$PATH
ANDROID_JAR=$TOOLS/android-sdk/platforms/android-35/android.jar
BT=$TOOLS/android-sdk/build-tools/android-14
XPOSED_JAR=$TOOLS/libxposed-api-classes.jar

rm -rf build
mkdir -p build/classes build/dex build/stage/META-INF/xposed

echo "== javac"
javac -source 8 -target 8 -nowarn \
    -cp "$ANDROID_JAR:$XPOSED_JAR" \
    -d build/classes \
    $(find src -name '*.java')

echo "== d8"
$BT/d8 --release --min-api 31 \
    --lib "$ANDROID_JAR" \
    --classpath "$XPOSED_JAR" \
    --output build/dex \
    $(find build/classes -name '*.class')

echo "== aapt2 compile + link"
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk \
    -I "$ANDROID_JAR" \
    --manifest AndroidManifest.xml \
    --min-sdk-version 31 --target-sdk-version 35 \
    build/res.zip

echo "== pack"
cp build/dex/classes.dex build/stage/classes.dex
cp xposed/java_init.list xposed/module.prop xposed/scope.list build/stage/META-INF/xposed/
cp build/base.apk build/unsigned.apk
(cd build/stage && zip -q -r ../unsigned.apk classes.dex META-INF)

echo "== zipalign + sign"
$BT/zipalign -p -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f debug.keystore ]; then
    keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=CamGalleryHook Debug,O=Muse,C=JP" >/dev/null 2>&1
fi
$BT/apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
    --out CamGalleryHook.apk build/aligned.apk
$BT/apksigner verify CamGalleryHook.apk
echo "== done: $(ls -la CamGalleryHook.apk | awk '{print $5}') bytes"
unzip -l CamGalleryHook.apk
