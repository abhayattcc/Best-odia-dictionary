# Add project specific ProGuard rules here.
-keep class com.abhayattcc.dictionaryreader.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.github.barteksc.**
-dontwarn com.google.android.gms.**
-dontwarn com.android.billingclient.**
-keep class androidx.room.** { *; }
-keep class kotlinx.coroutines.** { *; }