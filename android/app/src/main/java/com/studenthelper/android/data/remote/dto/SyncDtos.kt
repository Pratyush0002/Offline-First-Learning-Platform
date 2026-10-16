package com.studenthelper.android.data.remote.dto

import com.studenthelper.android.crdt.CrdtOperation
import com.studenthelper.android.crdt.VersionVector

data class SyncRequestDto(
    val clientId: String,
    val deviceName: String,
    val clientVector: VersionVector,
    val operations: List<CrdtOperation>
)

data class SyncResponseDto(
    val status: String,
    val acknowledgedOpIds: List<String>,
    val missingOperations: List<CrdtOperation>,
    val serverVector: VersionVector,
    val serverTimestamp: Long
)

data class CourseDto(
    val id: String,
    val title: String,
    val description: String,
    val subject: String,
    val level: String,
    val thumbnailUrl: String?,
    val estimatedHours: Int
)

data class LessonDto(
    val id: String,
    val courseId: String,
    val title: String,
    val orderIndex: Int,
    val summary: String,
    val contentMarkdown: String,
    val estimatedMinutes: Int,
    val quizQuestionsJson: String?,
    val resourcesJson: String?
)

data class CourseWithLessonsDto(
    val course: CourseDto,
    val lessons: List<LessonDto>,
    val completedLessonsCount: Int,
    val overallProgressPercent: Int
)

data class LessonPackageDto(
    val course: CourseDto,
    val lesson: LessonDto,
    val siblingLessons: List<LessonDto>,
    val downloadedAt: Long
)
