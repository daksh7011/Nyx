# R8/ProGuard keep rules for the Nyx release build.
#
# Deliberately minimal. Verified (by unpacking the resolved artifacts) that kotlinx-serialization,
# kotlinx-coroutines, androidx.datastore, and androidx/JetBrains navigation all ship their own
# consumer rules that R8 auto-applies — so we must NOT re-declare them here. Koin (constructor-DSL),
# SQLDelight (framework SQLite), and Compose resources need nothing. Adding broad `-keep` rules
# would silently defeat shrinking.

# --- REQUIRED: cryptography-kotlin JDK provider (the only genuinely-needed app rule) ---
# provider-optimal resolves to cryptography-provider-jdk, whose provider is discovered via
# java.util.ServiceLoader over META-INF/services/dev.whyoleg.cryptography.CryptographyProviderContainer.
# The impl is only reached reflectively; if R8 strips it, CryptographyProvider.Default throws
# ("no providers registered") on the first encrypt/decrypt. This jar ships NO consumer rules.
-keep class dev.whyoleg.cryptography.providers.jdk.JdkCryptographyProviderContainer { public <init>(); }
# Version-proof safety net if that impl is ever renamed/split.
-if public class * implements dev.whyoleg.cryptography.CryptographyProviderContainer
-keep,allowobfuscation public class <1> { public <init>(); }

# --- OPTIONAL (defense-in-depth): strip verbose/debug logs from release ---
# Nyx has no logging today; this keeps it that way for any future code so plaintext/secrets can never
# reach logcat. Enumerate the exact static methods — a `<methods>;` wildcard would also mark inherited
# Object.wait()/notify() as side-effect-free and let R8 strip synchronization. Error logs (w/e/wtf)
# are intentionally kept.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# proguard-android-optimize.txt already keeps SourceFile + LineNumberTable and renames the source
# file, so mapping.txt retrace preserves line numbers — no extra -keepattributes needed here.
