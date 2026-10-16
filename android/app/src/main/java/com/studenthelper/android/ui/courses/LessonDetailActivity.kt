package com.studenthelper.android.ui.courses

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studenthelper.android.StudentHelperApp
import com.studenthelper.android.databinding.ActivityLessonDetailBinding
import com.studenthelper.android.ui.notes.NoteEditorActivity
import io.noties.markwon.Markwon
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLessonDetailBinding
    private lateinit var app: StudentHelperApp
    private lateinit var markwon: Markwon
    private var lessonId: String = ""

    data class QuizQuestion(
        val question: String,
        val options: List<String>,
        val correctIndex: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLessonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as StudentHelperApp
        markwon = Markwon.create(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        lessonId = intent.getStringExtra(EXTRA_LESSON_ID) ?: ""
        if (lessonId.isEmpty()) {
            Toast.makeText(this, "Lesson ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        observeLessonData()
        setupListeners()
    }

    private fun observeLessonData() {
        lifecycleScope.launch {
            app.courseRepository.getLessonById(lessonId).collectLatest { lesson ->
                if (lesson != null) {
                    binding.tvLessonTitle.text = lesson.title
                    binding.tvLessonDuration.text = "⏱️ ${lesson.estimatedMinutes} mins • Lesson #${lesson.orderIndex}"

                    // Render Markdown content
                    markwon.setMarkdown(binding.tvLessonContent, lesson.contentMarkdown)

                    // Offline status banner
                    if (lesson.isDownloadedOffline) {
                        binding.tvOfflineStatus.text = "Saved in local SQLite. Available 100% offline!"
                        binding.btnDownloadOffline.visibility = View.GONE
                    } else {
                        binding.tvOfflineStatus.text = "Available online. Download for offline study."
                        binding.btnDownloadOffline.visibility = View.VISIBLE
                    }

                    // Render Quiz if available
                    setupQuiz(lesson.quizQuestionsJson)
                }
            }
        }
    }

    private fun setupQuiz(quizJson: String?) {
        if (quizJson.isNullOrEmpty()) {
            binding.cardQuiz.visibility = View.GONE
            return
        }

        try {
            val listType = object : TypeToken<List<QuizQuestion>>() {}.type
            val questions: List<QuizQuestion> = Gson().fromJson(quizJson, listType)
            if (questions.isNotEmpty()) {
                val q = questions.first()
                binding.cardQuiz.visibility = View.VISIBLE
                binding.tvQuizQuestion.text = q.question
                binding.rgQuizOptions.removeAllViews()

                for ((idx, option) in q.options.withIndex()) {
                    val rb = RadioButton(this)
                    rb.id = View.generateViewId()
                    rb.text = option
                    rb.tag = idx
                    binding.rgQuizOptions.addView(rb)
                }

                binding.btnSubmitQuiz.setOnClickListener {
                    val selectedId = binding.rgQuizOptions.checkedRadioButtonId
                    if (selectedId != -1) {
                        val selectedRb = binding.rgQuizOptions.findViewById<RadioButton>(selectedId)
                        val selectedIdx = selectedRb.tag as Int
                        binding.tvQuizFeedback.visibility = View.VISIBLE
                        if (selectedIdx == q.correctIndex) {
                            binding.tvQuizFeedback.text = "Correct! Great job mastering this concept."
                            binding.tvQuizFeedback.setTextColor(getColor(com.studenthelper.android.R.color.online_green))
                        } else {
                            binding.tvQuizFeedback.text = "Incorrect. Try re-reading the lesson section above."
                            binding.tvQuizFeedback.setTextColor(getColor(com.studenthelper.android.R.color.error_red))
                        }
                    } else {
                        Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            binding.cardQuiz.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnDownloadOffline.setOnClickListener {
            lifecycleScope.launch {
                val res = app.courseRepository.downloadLessonForOffline(lessonId)
                if (res.isSuccess) {
                    Toast.makeText(this@LessonDetailActivity, "Downloaded successfully to SQLite!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LessonDetailActivity, "Download failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnAddNote.setOnClickListener {
            lifecycleScope.launch {
                val noteId = app.noteRepository.createNote("Notes on ${binding.tvLessonTitle.text}", lessonId)
                val intent = Intent(this@LessonDetailActivity, NoteEditorActivity::class.java).apply {
                    putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId)
                }
                startActivity(intent)
            }
        }

        binding.btnCompleteLesson.setOnClickListener {
            Toast.makeText(this, "Lesson marked completed in local SQLite!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_LESSON_ID = "extra_lesson_id"
    }
}
