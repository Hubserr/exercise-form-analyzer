package com.formanalyzer.app.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.formanalyzer.app.R
import com.formanalyzer.app.data.SavedSession
import com.formanalyzer.app.databinding.ItemSessionHistoryBinding
import com.formanalyzer.app.squat.SquatErrorType
import java.text.DateFormat
import java.util.Date

/** Shows saved sets in the training history list. */
class SessionHistoryAdapter :
    ListAdapter<SavedSession, SessionHistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemSessionHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val summary = getItem(position).summary
        val context = holder.itemView.context

        holder.binding.sessionDateText.text = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(summary.startedAtMs))
        holder.binding.sessionRepsText.text = context.getString(
            R.string.history_reps,
            summary.totalReps,
            summary.correctReps,
            formatDuration(summary.durationMs)
        )
        holder.binding.sessionErrorsText.text = context.getString(
            R.string.history_errors,
            summary.errorCount(SquatErrorType.INSUFFICIENT_DEPTH),
            summary.errorCount(SquatErrorType.KNEE_VALGUS),
            summary.errorCount(SquatErrorType.EXCESSIVE_BACK_ROUNDING)
        )
    }

    private companion object {
        /** Lets the list animate only the rows that actually changed. */
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SavedSession>() {
            override fun areItemsTheSame(oldItem: SavedSession, newItem: SavedSession) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SavedSession, newItem: SavedSession) =
                oldItem == newItem
        }
    }
}