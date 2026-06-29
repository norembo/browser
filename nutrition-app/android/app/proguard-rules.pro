# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.nutrisnap.app.**$$serializer { *; }
-keepclassmembers class com.nutrisnap.app.** { *** Companion; }
-keepclasseswithmembers class com.nutrisnap.app.** { kotlinx.serialization.KSerializer serializer(...); }
