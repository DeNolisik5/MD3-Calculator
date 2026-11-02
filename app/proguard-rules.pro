# Compose and AndroidX ship their own consumer rules, nothing special is required here.
# Keep the calculator entry point (used from the manifest and reflection-free anyway).
-keep class com.narezany.calculator.MainActivity { *; }

# The ViewModel is created reflectively by SavedStateViewModelFactory; keep its
# (SavedStateHandle) constructor so the R8 release build can always instantiate it.
-keepclassmembers class com.narezany.calculator.calc.CalculatorViewModel {
    <init>(androidx.lifecycle.SavedStateHandle);
}
