# Hermes Agent — ProGuard / R8 rules
# Phase 1 (Foundation) of the technical plan.

# --- Kotlin metadata ---
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# --- Coroutines ---
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.flow.**

# --- Hilt / Dagger (handled by plugin, kept defensively) ---
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# --- Retrofit / OkHttp ---
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Kotlinx Serialization ---
-keepattributes *Annotation*
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hermes.agent.**$$serializer { *; }
-keepclassmembers class com.hermes.agent.** {
    *** Companion;
}

# --- Hermes app entities / DTOs (serializable) ---
-keep @kotlinx.serialization.Serializable class com.hermes.agent.data.remote.dto.** { *; }

# =====================================================================
# Merged feature modules. Neither Octo Jotter nor Sassy Butler ever
# shipped minified (both set isMinifyEnabled=false standalone), so every
# reflective surface they rely on has to be declared here explicitly.
# =====================================================================

# --- ONNX Runtime (:feature:butler) ---
# TtsEngine talks to libonnxruntime.so through JNI; the Java peers are looked up
# by name from native code, so they must not be renamed or stripped.
-keep class ai.onnxruntime.** { *; }
-keepclasseswithmembernames class ai.onnxruntime.** { native <methods>; }
-dontwarn ai.onnxruntime.**

# --- Mozilla Rhino (:feature:jotter community-plugin scripting) ---
# Rhino resolves Java members reflectively when scripts touch host objects.
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**

# --- Moshi (:feature:jotter) ---
# Two adapter styles are in use: 26 @JsonClass codegen adapters, and a reflective
# KotlinJsonAdapterFactory (Converters, RetrofitClient, PluginRepository, NoteViewModel).
# The reflective path reads Kotlin metadata and constructor parameter names.
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }          # generated adapters
-keepnames @com.squareup.moshi.JsonClass class *
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keepclassmembers @com.squareup.moshi.JsonClass class * { <init>(...); }
-keep class kotlin.Metadata { *; }
-dontwarn org.jetbrains.annotations.**

# --- Octo Jotter: types crossing a reflective Moshi adapter ---
# RegistryIndex / PluginManifest (PluginRepository), DatabaseBackup (export), and the
# GitHub DTOs behind the Retrofit Moshi converter are all constructed by name.
-keep class com.l3ad3r1.octojotter.plugin.** { *; }
-keep class com.l3ad3r1.octojotter.data.remote.** { *; }
-keep class com.l3ad3r1.octojotter.data.local.DatabaseBackup { *; }

# --- Sassy Butler ---
# AlarmStore persists with org.json by hand (no reflection), but AlarmReceiver,
# AlarmForegroundService and both Activities are entry points named in the manifest.
-keep class com.sassybutler.alarm.AlarmReceiver { *; }
-keep class com.sassybutler.alarm.AlarmForegroundService { *; }
-keep class com.sassybutler.alarm.AlarmActivity { *; }
-keep class com.sassybutler.alarm.MainAlarmSetupActivity { *; }
