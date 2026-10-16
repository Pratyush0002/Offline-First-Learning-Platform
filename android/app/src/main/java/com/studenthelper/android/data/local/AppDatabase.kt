package com.studenthelper.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.studenthelper.android.data.local.dao.*
import com.studenthelper.android.data.local.entity.*

@Database(
    entities = [
        CourseEntity::class,
        LessonEntity::class,
        NoteEntity::class,
        CrdtOpEntity::class,
        LessonProgressEntity::class,
        VectorClockEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun lessonDao(): LessonDao
    abstract fun noteDao(): NoteDao
    abstract fun crdtOpDao(): CrdtOpDao
    abstract fun progressDao(): ProgressDao
    abstract fun vectorClockDao(): VectorClockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_helper.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
