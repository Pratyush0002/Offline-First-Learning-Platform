package com.studenthelper.android.ui.courses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studenthelper.android.R
import com.studenthelper.android.data.local.entity.LessonEntity

class LessonListAdapter(
    private val onLessonClicked: (LessonEntity) -> Unit
) : ListAdapter<LessonEntity, LessonListAdapter.LessonViewHolder>(LessonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view, onLessonClicked)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LessonViewHolder(
        itemView: View,
        private val onClick: (LessonEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex: TextView = itemView.findViewById(R.id.tvLessonIndex)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvLessonTitle)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvLessonSummary)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvDownloadBadge)

        fun bind(lesson: LessonEntity) {
            tvIndex.text = lesson.orderIndex.toString()
            tvTitle.text = lesson.title
            tvSummary.text = lesson.summary

            if (lesson.isDownloadedOffline) {
                tvBadge.text = "Offline"
                tvBadge.visibility = View.VISIBLE
            } else {
                tvBadge.text = "Online"
                tvBadge.visibility = View.VISIBLE
            }

            itemView.setOnClickListener { onClick(lesson) }
        }
    }

    class LessonDiffCallback : DiffUtil.ItemCallback<LessonEntity>() {
        override fun areItemsTheSame(oldItem: LessonEntity, newItem: LessonEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LessonEntity, newItem: LessonEntity): Boolean =
            oldItem == newItem
    }
}
