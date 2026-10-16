package com.studenthelper.android

import android.app.Application
import com.studenthelper.android.data.local.AppDatabase
import com.studenthelper.android.data.remote.ApiClient
import com.studenthelper.android.data.repository.CourseRepository
import com.studenthelper.android.data.repository.NoteRepository
import com.studenthelper.android.data.repository.SyncRepository
import com.studenthelper.android.network.NetworkStatusMonitor
import com.studenthelper.android.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class StudentHelperApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var networkMonitor: NetworkStatusMonitor
        private set

    lateinit var courseRepository: CourseRepository
        private set

    lateinit var noteRepository: NoteRepository
        private set

    lateinit var syncRepository: SyncRepository
        private set

    val clientId: String by lazy {
        val prefs = getSharedPreferences("student_helper_prefs", MODE_PRIVATE)
        var id = prefs.getString("client_id", null)
        if (id == null) {
            id = "android-device-" + UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString("client_id", id).apply()
        }
        id
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Room SQLite Database
        database = AppDatabase.getInstance(this)

        // 2. Initialize Repositories
        val apiService = ApiClient.apiService
        courseRepository = CourseRepository(database.courseDao(), database.lessonDao(), apiService)
        noteRepository = NoteRepository(database.noteDao(), database.crdtOpDao(), clientId)
        syncRepository = SyncRepository(database.crdtOpDao(), database.vectorClockDao(), noteRepository, apiService, clientId)

        // 3. Initialize Network Status Monitor
        networkMonitor = NetworkStatusMonitor(this)

        // 4. Schedule Background WorkManager Sync
        SyncScheduler.schedulePeriodicSync(this)

        // 5. Reactive trigger: when internet returns, immediately sync pending offline ops!
        applicationScope.launch {
            networkMonitor.isOnline.collectLatest { isOnline ->
                if (isOnline) {
                    SyncScheduler.triggerImmediateSync(this@StudentHelperApp)
                    courseRepository.refreshCourses()
                }
            }
        }
    }
}
