package com.example.lab4_sqlite

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private lateinit var tvMode: TextView
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnAdd: Button
    private lateinit var layoutEditButtons: LinearLayout
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button
    private lateinit var btnCancel: Button
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var dbHelper: NotesDbHelper
    private lateinit var adapter: NotesAdapter

    private val databaseErrors = kotlinx.coroutines.CoroutineExceptionHandler { _, error ->
        runOnUiThread {
            Toast.makeText(this, "Ошибка базы данных: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private var selectedNoteId: Long? = null

    override fun onSaveInstanceState(outState: Bundle) {
        selectedNoteId?.let { outState.putLong("selected_id", it) }
        outState.putString("edit_title", etTitle.text.toString())
        outState.putString("edit_content", etContent.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = NotesDbHelper(this)

        tvMode = findViewById(R.id.tvMode)
        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        btnAdd = findViewById(R.id.btnAdd)
        layoutEditButtons = findViewById(R.id.layoutEditButtons)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnDelete = findViewById(R.id.btnDelete)
        btnCancel = findViewById(R.id.btnCancel)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = NotesAdapter(emptyList()) { note ->
            switchToEditMode(note)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Добавление новой заметки
        btnAdd.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Заполните заголовок заметки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            lifecycleScope.launch(Dispatchers.IO + databaseErrors) {
                val newId = dbHelper.insertNote(title, content)
                withContext(Dispatchers.Main) {
                    if (newId != -1L) {
                        Toast.makeText(this@MainActivity, "Заметка сохранена в SQLite!", Toast.LENGTH_SHORT).show()
                        switchToCreateMode()
                        loadNotes()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка добавления", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Обновление выбранной заметки
        btnUpdate.setOnClickListener {
            val id = selectedNoteId ?: return@setOnClickListener
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Заголовок не может быть пустым", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            lifecycleScope.launch(Dispatchers.IO + databaseErrors) {
                val rows = dbHelper.updateNote(id, title, content)
                withContext(Dispatchers.Main) {
                    if (rows > 0) {
                        Toast.makeText(this@MainActivity, "Изменения сохранены!", Toast.LENGTH_SHORT).show()
                        switchToCreateMode()
                        loadNotes()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Удаление выбранной заметки
        btnDelete.setOnClickListener {
            val id = selectedNoteId ?: return@setOnClickListener

            hideKeyboard()

            lifecycleScope.launch(Dispatchers.IO + databaseErrors) {
                val deletedRows = dbHelper.deleteNote(id)
                withContext(Dispatchers.Main) {
                    if (deletedRows > 0) {
                        Toast.makeText(this@MainActivity, "Заметка удалена!", Toast.LENGTH_SHORT).show()
                        switchToCreateMode()
                        loadNotes()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Отмена режима редактирования
        btnCancel.setOnClickListener {
            hideKeyboard()
            switchToCreateMode()
        }

        // Первичная загрузка
        switchToCreateMode()
        loadNotes()
        if (savedInstanceState?.containsKey("selected_id") == true) {
            switchToEditMode(Note(
                id = savedInstanceState.getLong("selected_id"),
                title = savedInstanceState.getString("edit_title", ""),
                content = savedInstanceState.getString("edit_content", ""),
                timestamp = 0L
            ))
        }
    }

    private fun switchToCreateMode() {
        selectedNoteId = null
        adapter.setSelectedId(null)
        etTitle.text.clear()
        etContent.text.clear()

        tvMode.text = "➕ Создание новой заметки"
        tvMode.setTextColor(Color.parseColor("#00796B"))

        btnAdd.visibility = View.VISIBLE
        layoutEditButtons.visibility = View.GONE
    }

    private fun switchToEditMode(note: Note) {
        selectedNoteId = note.id
        adapter.setSelectedId(note.id)
        etTitle.setText(note.title)
        etContent.setText(note.content)

        tvMode.text = "✏️ Редактирование: ${note.title}"
        tvMode.setTextColor(Color.parseColor("#1976D2"))

        btnAdd.visibility = View.GONE
        layoutEditButtons.visibility = View.VISIBLE
    }

    private fun loadNotes() {
        lifecycleScope.launch(Dispatchers.IO + databaseErrors) {
            val notes = dbHelper.getAllNotes()
            withContext(Dispatchers.Main) {
                adapter.updateData(notes)
                if (notes.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
