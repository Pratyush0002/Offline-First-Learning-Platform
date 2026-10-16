package com.studenthelper.android.ui.courses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.studenthelper.android.R
import com.studenthelper.android.data.local.entity.CourseEntity

class CourseListAdapter(
    private val onCourseClicked: (CourseEntity) -> Unit
) : ListAdapter<CourseEntity, CourseListAdapter.CourseViewHolder>(CourseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view, onCourseClicked)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CourseViewHolder(
        itemView: View,
        private val onClick: (CourseEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvSubject: TextView = itemView.findViewById(R.id.tvCourseSubject)
        private val tvLevel: TextView = itemView.findViewById(R.id.tvCourseLevel)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvCourseTitle)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvCourseDescription)
        private val tvOffline: TextView = itemView.findViewById(R.id.tvOfflineStatus)
        private val btnView: MaterialButton = itemView.findViewById(R.id.btnViewLessons)

        fun bind(course: CourseEntity) {
            tvSubject.text = course.subject
            tvLevel.text = course.level
            tvTitle.text = course.title
            tvDesc.text = course.description

            if (course.isDownloadedOffline) {
                tvOffline.text = "Saved Offline (SQLite)"
                tvOffline.visibility = View.VISIBLE
            } else {
                tvOffline.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(course) }
            btnView.setOnClickListener { onClick(course) }
        }
    }

    class CourseDiffCallback : DiffUtil.ItemCallback<CourseEntity>() {
        override fun areItemsTheSame(oldItem: CourseEntity, newItem: CourseEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CourseEntity, newItem: CourseEntity): Boolean =
            oldItem == newItem
    }
}
