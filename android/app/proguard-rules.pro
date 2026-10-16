# Keep Room database schemas and entities
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep DTO models for Retrofit and Gson
-keep class com.studenthelper.android.data.remote.dto.** { *; }
-keep class com.studenthelper.android.crdt.** { *; }
