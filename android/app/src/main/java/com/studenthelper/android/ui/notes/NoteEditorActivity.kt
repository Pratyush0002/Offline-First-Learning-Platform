package com.studenthelper.android.ui.notes

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.studenthelper.android.R
import com.studenthelper.android.StudentHelperApp
import com.studenthelper.android.crdt.BlockType
import com.studenthelper.android.databinding.ActivityNoteEditorBinding
import com.studenthelper.android.sync.SyncScheduler
import io.noties.markwon.Markwon
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var app: StudentHelperApp
    private lateinit var markwon: Markwon
    private var noteId: String = ""
    private var selectedBlockType: BlockType = BlockType.PARAGRAPH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as StudentHelperApp
        markwon = Markwon.create(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: ""
        if (noteId.isEmpty()) {
            Toast.makeText(this, "Note ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        observeNote()
        setupButtons()
    }

    private fun observeNote() {
        lifecycleScope.launch {
            app.noteRepository.getNote(noteId).collectLatest { note ->
                if (note != null) {
                    if (binding.etNoteTitle.text.isNullOrEmpty() || binding.etNoteTitle.text.toString() != note.title) {
                        binding.etNoteTitle.setText(note.title)
                    }

                    binding.tvLamportClock.text = "Clock: ${note.titleClock} • ${note.titleClientId}"

                    if (note.isSyncedWithServer) {
                        binding.tvSyncBadge.text = "● Fully Synced with Server"
                        binding.tvSyncBadge.setTextColor(getColor(R.color.online_green))
                    } else {
                        binding.tvSyncBadge.text = "● Saved in SQLite (Pending Offline Sync)"
                        binding.tvSyncBadge.setTextColor(getColor(R.color.offline_orange))
                    }

                    // Render preview
                    markwon.setMarkdown(binding.tvMarkdownPreview, note.contentMarkdown)

                    // Render tags
                    binding.chipGroupTags.removeAllViews()
                    val tagList = listOf("OfflineStudy", "ExamPrep", "KeyConcepts")
                    for (t in tagList) {
                        val chip = Chip(this@NoteEditorActivity).apply {
                            text = "#$t"
                            isCheckable = false
                        }
                        binding.chipGroupTags.addView(chip)
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnAddParagraph.setOnClickListener {
            selectedBlockType = BlockType.PARAGRAPH
            binding.etBlockContent.hint = "Write paragraph text..."
            Toast.makeText(this, "Selected: Paragraph Block", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddTodo.setOnClickListener {
            selectedBlockType = BlockType.TODO_ITEM
            binding.etBlockContent.hint = "Write action item..."
            Toast.makeText(this, "Selected: Checklist Todo Item", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddCode.setOnClickListener {
            selectedBlockType = BlockType.CODE_SNIPPET
            binding.etBlockContent.hint = "Paste code snippet or formula..."
            Toast.makeText(this, "Selected: Code Snippet Block", Toast.LENGTH_SHORT).show()
        }

        binding.btnCommitBlock.setOnClickListener {
            val content = binding.etBlockContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter block content", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Update title if changed
                val newTitle = binding.etNoteTitle.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    app.noteRepository.updateTitle(noteId, newTitle)
                }

                // Add CRDT block
                app.noteRepository.addBlock(noteId, content, selectedBlockType)
                binding.etBlockContent.setText("")
                Toast.makeText(this@NoteEditorActivity, "Block appended to CRDT sequence in SQLite!", Toast.LENGTH_SHORT).show()

                // Trigger background sync if online
                SyncScheduler.triggerImmediateSync(this@NoteEditorActivity)
            }
        }

        binding.btnDeleteNote.setOnClickListener {
            lifecycleScope.launch {
                app.noteRepository.deleteNote(noteId)
                Toast.makeText(this@NoteEditorActivity, "Note deleted (CRDT tombstone recorded)", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
