# Gson
-keepattributes Signature,Annotation,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keep class com.music.purelymusic.model.** { *; }
-keep class com.music.purelymusic.data.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Retrofit
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3 / ExoPlayer
-dontwarn androidx.media3.**

# Coil
-dontwarn coil.**

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# BuildConfig
-keep class com.music.purelymusic.BuildConfig { *; }
