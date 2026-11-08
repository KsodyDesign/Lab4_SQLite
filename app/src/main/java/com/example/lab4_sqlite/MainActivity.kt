package com.example.lab4_sqlite

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button
    private lateinit var btnClear: Button
    private lateinit var recyclerView: RecyclerView

    private lateinit var dbHelper: NotesDbHelper
    private lateinit var adapter: NotesAdapter

    private var selectedNoteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = NotesDbHelper(this)

        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        btnAdd = findViewById(R.id.btnAdd)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnDelete = findViewById(R.id.btnDelete)
        btnClear = findViewById(R.id.btnClear)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = NotesAdapter(emptyList()) { note ->
            selectedNoteId = note.id
            etTitle.setText(note.title)
            etContent.setText(note.content)
            Toast.makeText(this, "Выбрана заметка: ${note.title}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Добавление новой заметки
        btnAdd.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Заголовок не может быть пустым", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val newId = dbHelper.insertNote(title, content)
                withContext(Dispatchers.Main) {
                    if (newId != -1L) {
                        Toast.makeText(this@MainActivity, "Заметка добавлена (ID: $newId)", Toast.LENGTH_SHORT).show()
                        clearInputs()
                        loadNotes()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка добавления заметки", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Обновление выбранной заметки
        btnUpdate.setOnClickListener {
            val id = selectedNoteId
            if (id == null) {
                Toast.makeText(this, "Выберите заметку из списка для обновления", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Заголовок не может быть пустым", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val rows = dbHelper.updateNote(id, title, content)
                withContext(Dispatchers.Main) {
                    if (rows > 0) {
                        Toast.makeText(this@MainActivity, "Заметка обновлена", Toast.LENGTH_SHORT).show()
                        clearInputs()
                        loadNotes()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Удаление выбранной заметки
        btnDelete.setOnClickListener {
            val id = selectedNoteId
            if (id == null) {
                Toast.makeText(this, "Выберите заметку из списка для удаления", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val deletedRows = dbHelper.deleteNote(id)
                withContext(Dispatchers.Main) {
                    if (deletedRows > 0) {
                        Toast.makeText(this@MainActivity, "Заметка удалена", Toast.LENGTH_SHORT).show()
                        clearInputs()
                        loadNotes()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Очистка полей ввода
        btnClear.setOnClickListener {
            clearInputs()
        }

        // Первичная загрузка заметок
        loadNotes()
    }

    private fun loadNotes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val notes = dbHelper.getAllNotes()
            withContext(Dispatchers.Main) {
                adapter.updateData(notes)
            }
        }
    }

    private fun clearInputs() {
        selectedNoteId = null
        etTitle.text.clear()
        etContent.text.clear()
    }
}
