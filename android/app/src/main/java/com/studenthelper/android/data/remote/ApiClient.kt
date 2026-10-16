package com.studenthelper.android.data.remote

import com.studenthelper.android.data.remote.dto.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface LearningApiService {

    @GET("api/courses")
    suspend fun getCourses(): List<CourseDto>

    @GET("api/courses/{id}")
    suspend fun getCourseDetail(
        @Path("id") courseId: String,
        @Query("studentId") studentId: String = "student-android"
    ): CourseWithLessonsDto

    @GET("api/lessons/{id}/download")
    suspend fun downloadLessonPackage(
        @Path("id") lessonId: String,
        @Query("studentId") studentId: String = "student-android"
    ): LessonPackageDto

    @POST("api/sync/delta")
    suspend fun syncDelta(@Body request: SyncRequestDto): SyncResponseDto
}

object ApiClient {
    // 10.0.2.2 points to host machine from Android Emulator.
    // Replace with local IP (e.g., http://192.168.1.X:8080/) when testing on physical device.
    var baseUrl: String = "http://10.0.2.2:8080/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val apiService: LearningApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LearningApiService::class.java)
    }
}
