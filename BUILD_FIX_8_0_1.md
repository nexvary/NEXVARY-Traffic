# NEXVARY Traffic Lab v8.0.1 build fix

Android Gradle Plugin 8.x disables BuildConfig generation unless enabled when custom BuildConfig fields are declared.

The app uses:

```gradle
buildConfigField 'String', 'RECOVERY_ENDPOINT', ...
```

Therefore the module now explicitly enables:

```gradle
buildFeatures {
    buildConfig true
}
```

This resolves the Gradle error: `defaultConfig contains custom BuildConfig fields, but the feature is disabled.`
