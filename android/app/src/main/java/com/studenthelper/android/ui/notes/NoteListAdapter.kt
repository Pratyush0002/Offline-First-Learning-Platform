package com.studenthelper.android.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studenthelper.android.R
import com.studenthelper.android.data.local.entity.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

class NoteListAdapter(
    private val onNoteClicked: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NoteListAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view, onNoteClicked)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NoteViewHolder(
        itemView: View,
        private val onClick: (NoteEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvSnippet: TextView = itemView.findViewById(R.id.tvNoteSnippet)
        private val tvTags: TextView = itemView.findViewById(R.id.tvNoteTags)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvSyncStatus)
        private val tvUpdated: TextView = itemView.findViewById(R.id.tvLastUpdated)

        fun bind(note: NoteEntity) {
            tvTitle.text = note.title
            tvSnippet.text = note.contentMarkdown.replace("#", "").trim()
            tvTags.text = "CRDT Clock: ${note.titleClock} • ${note.titleClientId}"

            if (note.isSyncedWithServer) {
                tvStatus.text = "Synced"
                tvStatus.setTextColor(itemView.context.getColor(R.color.online_green))
            } else {
                tvStatus.text = "Saved Offline (Pending Sync)"
                tvStatus.setTextColor(itemView.context.getColor(R.color.offline_orange))
            }

            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            tvUpdated.text = sdf.format(Date(note.updatedAt))

            itemView.setOnClickListener { onClick(note) }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
            oldItem == newItem
    }
}
