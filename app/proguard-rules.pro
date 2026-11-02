# Compose and AndroidX ship their own consumer rules, nothing special is required here.
# Keep the calculator entry point (used from the manifest and reflection-free anyway).
-keep class com.narezany.calculator.MainActivity { *; }
