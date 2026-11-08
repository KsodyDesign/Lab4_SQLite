package com.example.lab4_sqlite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private var selectedId: Long? = null

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.cardContainer)
        val tvTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        val tvDate: TextView = itemView.findViewById(R.id.tvNoteDate)
        val tvSelectHint: TextView = itemView.findViewById(R.id.tvSelectHint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note_sqlite, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvTitle.text = note.title
        holder.tvContent.text = note.content
        holder.tvDate.text = dateFormat.format(Date(note.timestamp))

        val isSelected = (note.id == selectedId)
        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.bg_card_selected)
            holder.tvSelectHint.visibility = View.VISIBLE
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_card_normal)
            holder.tvSelectHint.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onNoteClick(note)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateData(newNotes: List<Note>) {
        this.notes = newNotes
        notifyDataSetChanged()
    }

    fun setSelectedId(id: Long?) {
        this.selectedId = id
        notifyDataSetChanged()
    }
}
