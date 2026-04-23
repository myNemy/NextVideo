# Development

## Requirements

- JDK 17
- Android SDK (Android Studio) **or** build via container

## Local build (Android SDK)

Create `local.properties` in the repo root:

```
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew :app:assembleDebug
```

## Container build (Podman)

If you don't have an Android SDK installed locally, you can build using an SDK container:

```bash
podman pull ghcr.io/cirruslabs/android-sdk:35

podman run --rm \
  -v "$PWD:/project:Z" \
  -w /project \
  -e ANDROID_HOME=/opt/android-sdk-linux \
  -e ANDROID_SDK_ROOT=/opt/android-sdk-linux \
  ghcr.io/cirruslabs/android-sdk:35 \
  bash -lc 'chmod +x ./gradlew; ./gradlew :app:assembleDebug --no-daemon'
```

APK output:

- `app/build/outputs/apk/debug/`

