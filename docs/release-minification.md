# Release minification and mapping files

Since #88, the `release` build type enables R8 code shrinking/obfuscation and
resource shrinking in `app/build.gradle.kts`:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

The `beta` build type inherits both through `initWith(release)`. Debug builds
stay unminified. Keep rules for reflection/JNI dependencies live in
`app/proguard-rules.pro`; extend them only when a real dependency needs it.

## Mapping output

After `:app:assembleRelease` or `:app:bundleRelease`, R8 writes:

- `app/build/outputs/mapping/release/mapping.txt` - the deobfuscation file
- `app/build/outputs/mapping/release/configuration.txt` - effective rules
- `app/build/outputs/mapping/release/seeds.txt` - kept entry points
- `app/build/outputs/mapping/release/usage.txt` - removed code

The beta variant writes the same files under
`app/build/outputs/mapping/beta/`.

Mapping files are build output: `build/` is already ignored, and they must not
be committed.

## Keeping mapping.txt with the AAB for Play

The deploytool `gradle_aab` artifact must keep the release `mapping.txt`
together with the AAB under `/srv/release-artifacts/klipsilo/` (for example
`KlipSilo-<version>-mapping.txt` beside the AAB). Upload that file to Google
Play Console with the release (App bundle explorer -> Downloads ->
Deobfuscation file) so crash reports are readable. This repository only
documents the requirement; the copy happens in the deploy job that already
copies the AAB. Do not SSH to jens-deploy from an app change.

Beta mapping is useful when diagnosing tester crash reports, but only the
release mapping is uploaded to Play.
