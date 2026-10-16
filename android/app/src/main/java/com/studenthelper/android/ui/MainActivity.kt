package com.studenthelper.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.studenthelper.android.R
import com.studenthelper.android.StudentHelperApp
import com.studenthelper.android.databinding.ActivityMainBinding
import com.studenthelper.android.sync.SyncScheduler
import com.studenthelper.android.ui.courses.CourseListAdapter
import com.studenthelper.android.ui.courses.LessonDetailActivity
import com.studenthelper.android.ui.notes.NoteEditorActivity
import com.studenthelper.android.ui.notes.NoteListAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var app: StudentHelperApp

    private lateinit var courseAdapter: CourseListAdapter
    private lateinit var noteAdapter: NoteListAdapter
    private lateinit var recyclerView: RecyclerView

    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as StudentHelperApp

        setupRecyclerView()
        observeNetworkAndSyncStatus()
        setupTabs()
        setupFab()
    }

    private fun setupRecyclerView() {
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        binding.fragmentContainer.addView(recyclerView)

        courseAdapter = CourseListAdapter { course ->
            lifecycleScope.launch {
                // Fetch first lesson or show lessons
                val lessons = app.database.lessonDao().getLessonDirect("lesson-dsa-big-o")
                val intent = Intent(this@MainActivity, LessonDetailActivity::class.java).apply {
                    putExtra(LessonDetailActivity.EXTRA_LESSON_ID, lessons?.id ?: "lesson-dsa-big-o")
                }
                startActivity(intent)
            }
        }

        noteAdapter = NoteListAdapter { note ->
            val intent = Intent(this, NoteEditorActivity::class.java).apply {
                putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
            }
            startActivity(intent)
        }

        recyclerView.adapter = courseAdapter
        loadCourses()
    }

    private fun observeNetworkAndSyncStatus() {
        // Observe real-time network connectivity
        lifecycleScope.launch {
            app.networkMonitor.isOnline.collectLatest { isOnline ->
                if (isOnline) {
                    binding.statusIndicatorDot.setBackgroundResource(R.drawable.circle_green)
                    binding.tvNetworkStatus.text = getString(R.string.status_online)
                } else {
                    binding.statusIndicatorDot.setBackgroundResource(R.drawable.circle_orange)
                    binding.tvNetworkStatus.text = getString(R.string.status_offline)
                }
            }
        }

        // Observe pending unsynced operations in Room SQLite
        lifecycleScope.launch {
            app.syncRepository.unsyncedCount.collectLatest { count ->
                if (count > 0) {
                    binding.tvOutboxBadge.text = "⚡ $count pending ops"
                    binding.tvOutboxBadge.visibility = View.VISIBLE
                } else {
                    binding.tvOutboxBadge.text = "All synced"
                    binding.tvOutboxBadge.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                when (currentTab) {
                    0 -> {
                        recyclerView.adapter = courseAdapter
                        binding.fabAction.text = "Refresh Courses"
                        loadCourses()
                    }
                    1 -> {
                        recyclerView.adapter = noteAdapter
                        binding.fabAction.text = getString(R.string.new_note)
                        loadNotes()
                    }
                    2 -> {
                        triggerManualSync()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab?.position == 2) triggerManualSync()
            }
        })
    }

    private fun loadCourses() {
        lifecycleScope.launch {
            app.courseRepository.allCourses.collectLatest { courses ->
                courseAdapter.submitList(courses)
            }
        }
    }

    private fun loadNotes() {
        lifecycleScope.launch {
            app.noteRepository.allNotes.collectLatest { notes ->
                noteAdapter.submitList(notes)
            }
        }
    }

    private fun setupFab() {
        binding.fabAction.setOnClickListener {
            if (currentTab == 0) {
                lifecycleScope.launch {
                    val res = app.courseRepository.refreshCourses()
                    if (res.isSuccess) {
                        Toast.makeText(this@MainActivity, "Course catalog updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Offline: using locally cached courses in SQLite", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                lifecycleScope.launch {
                    val noteId = app.noteRepository.createNote("New Study Note", null)
                    val intent = Intent(this@MainActivity, NoteEditorActivity::class.java).apply {
                        putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun triggerManualSync() {
        Toast.makeText(this, "Triggering CRDT Delta Sync with Spring Boot...", Toast.LENGTH_SHORT).show()
        SyncScheduler.triggerImmediateSync(this)
    }
}
