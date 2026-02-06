# Add project specific ProGuard rules here.

# 保留行号信息便于调试
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== Kotlinx Serialization ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 保留项目中使用 @Serializable 的类
-keep,includedescriptorclasses class cn.whc.launcher.data.cache.**$$serializer { *; }
-keepclassmembers class cn.whc.launcher.data.cache.** {
    *** Companion;
}
-keepclasseswithmembers class cn.whc.launcher.data.cache.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==================== Hilt ====================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ==================== Compose ====================
-dontwarn androidx.compose.**

# ==================== Coil ====================
-dontwarn coil.**

# ==================== Pinyin4j ====================
-keep class net.sourceforge.pinyin4j.** { *; }
-dontwarn net.sourceforge.pinyin4j.**
