package com.studenthelper.android.data.repository

import com.studenthelper.android.data.local.dao.CourseDao
import com.studenthelper.android.data.local.dao.LessonDao
import com.studenthelper.android.data.local.entity.CourseEntity
import com.studenthelper.android.data.local.entity.LessonEntity
import com.studenthelper.android.data.remote.LearningApiService
import kotlinx.coroutines.flow.Flow

class CourseRepository(
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val apiService: LearningApiService
) {

    val allCourses: Flow<List<CourseEntity>> = courseDao.getAllCourses()

    fun getLessonsForCourse(courseId: String): Flow<List<LessonEntity>> {
        return lessonDao.getLessonsByCourse(courseId)
    }

    fun getLessonById(lessonId: String): Flow<LessonEntity?> {
        return lessonDao.getLessonById(lessonId)
    }

    suspend fun refreshCourses(): Result<Unit> {
        return try {
            val remoteCourses = apiService.getCourses()
            val entities = remoteCourses.map { c ->
                CourseEntity(
                    id = c.id,
                    title = c.title,
                    description = c.description,
                    subject = c.subject,
                    level = c.level,
                    thumbnailUrl = c.thumbnailUrl,
                    estimatedHours = c.estimatedHours
                )
            }
            courseDao.insertCourses(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadLessonForOffline(lessonId: String): Result<LessonEntity> {
        return try {
            val pkg = apiService.downloadLessonPackage(lessonId)

            val courseEntity = CourseEntity(
                id = pkg.course.id,
                title = pkg.course.title,
                description = pkg.course.description,
                subject = pkg.course.subject,
                level = pkg.course.level,
                thumbnailUrl = pkg.course.thumbnailUrl,
                estimatedHours = pkg.course.estimatedHours,
                isDownloadedOffline = true,
                downloadedAt = System.currentTimeMillis()
            )
            courseDao.insertCourse(courseEntity)

            val lessonEntity = LessonEntity(
                id = pkg.lesson.id,
                courseId = pkg.lesson.courseId,
                title = pkg.lesson.title,
                orderIndex = pkg.lesson.orderIndex,
                summary = pkg.lesson.summary,
                contentMarkdown = pkg.lesson.contentMarkdown,
                estimatedMinutes = pkg.lesson.estimatedMinutes,
                quizQuestionsJson = pkg.lesson.quizQuestionsJson,
                resourcesJson = pkg.lesson.resourcesJson,
                isDownloadedOffline = true
            )
            lessonDao.insertLesson(lessonEntity)

            // Cache sibling lessons metadata as well
            val siblings = pkg.siblingLessons.map { s ->
                LessonEntity(
                    id = s.id,
                    courseId = s.courseId,
                    title = s.title,
                    orderIndex = s.orderIndex,
                    summary = s.summary,
                    contentMarkdown = s.contentMarkdown,
                    estimatedMinutes = s.estimatedMinutes,
                    quizQuestionsJson = s.quizQuestionsJson,
                    resourcesJson = s.resourcesJson,
                    isDownloadedOffline = (s.id == lessonId)
                )
            }
            lessonDao.insertLessons(siblings)

            Result.success(lessonEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
