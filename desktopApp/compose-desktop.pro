# App-specific ProGuard keep rules for the Nyx desktop (JVM) release build.
#
# The Compose Desktop Gradle plugin already auto-applies default-compose-desktop-rules.pro, which keeps
# skiko/skia, ALL of kotlinx-coroutines (incl. the coroutines-swing Main dispatcher), the full
# kotlinx-serialization ruleset (covers the @Serializable navigation routes), kotlinx-datetime and
# Material3. Do NOT duplicate any of those here. Unlike Android, library consumer rules are NOT applied
# on the JVM, so the three ServiceLoader/reflection libraries below need explicit app-authored keeps.

# --- SQLite JDBC (SQLDelight sqlite-driver -> org.xerial sqlite-jdbc) ---
# JdbcSqliteDriver connects via java.sql.DriverManager.getConnection(...), which discovers the driver
# through META-INF/services/java.sql.Driver (= org.sqlite.JDBC). ProGuard cannot see this reflective
# use and would strip it -> "No suitable driver found for jdbc:sqlite:...". Keeping org.sqlite.** with
# members also preserves org.sqlite.SQLiteJDBCLoader, which computes the native-library path (mangling
# it yields UnsatisfiedLinkError).
-keep class org.sqlite.** { *; }
-keepnames class org.sqlite.**
-dontwarn org.sqlite.**

# --- JDK crypto provider (cryptography-kotlin provider-optimal -> cryptography-provider-jdk) ---
# CryptographyProvider.Default loads the provider via java.util.ServiceLoader over
# META-INF/services/dev.whyoleg.cryptography.CryptographyProviderContainer. Same class as on Android;
# without the keep, every encrypt/decrypt/vault op fails with a provider-not-found error.
-keep class dev.whyoleg.cryptography.CryptographyProviderContainer
-keep class * implements dev.whyoleg.cryptography.CryptographyProviderContainer { *; }
-keep class dev.whyoleg.cryptography.providers.jdk.** { *; }
-dontwarn dev.whyoleg.cryptography.**

# --- DataStore Preferences (desktop uses shaded protobuf-lite, field reflection) ---
# On the JVM, PreferencesFileSerializer serializes via generated PreferencesProto$* messages that extend
# the shaded androidx.datastore.preferences.protobuf.GeneratedMessageLite and read field info reflectively.
# The datastore consumer rule is NOT auto-applied off-Android, so a shrinking build can strip those
# fields -> settings persistence throws InvalidProtocolBufferException on read-back.
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { <fields>; }
-keep class androidx.datastore.preferences.PreferencesProto** { *; }
-dontwarn androidx.datastore.preferences.protobuf.**

# --- Retrace clarity (packaged app stack traces keep line numbers) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
