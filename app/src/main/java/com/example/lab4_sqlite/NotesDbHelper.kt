package com.example.lab4_sqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NotesDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "notes"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_CONTENT TEXT,
                $COLUMN_TIMESTAMP INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Создание заметки (Insert)
    @Synchronized
    fun insertNote(title: String, content: String): Long {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_TITLE, title)
                put(COLUMN_CONTENT, content)
                put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            }
            return db.insert(TABLE_NAME, null, values)
        } finally {
            close()
        }
    }

    // Получение всех заметок (Read)
    @Synchronized
    fun getAllNotes(): List<Note> {
        try {
            val notesList = mutableListOf<Note>()
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                "$COLUMN_TIMESTAMP DESC"
            )

            cursor.use { c ->
                val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
                val titleIndex = c.getColumnIndexOrThrow(COLUMN_TITLE)
                val contentIndex = c.getColumnIndexOrThrow(COLUMN_CONTENT)
                val timestampIndex = c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)

                while (c.moveToNext()) {
                    val note = Note(
                        id = c.getLong(idIndex),
                        title = c.getString(titleIndex),
                        content = c.getString(contentIndex) ?: "",
                        timestamp = c.getLong(timestampIndex)
                    )
                    notesList.add(note)
                }
            }
            return notesList
        } finally {
            close()
        }
    }

    // Обновление заметки (Update)
    @Synchronized
    fun updateNote(id: Long, title: String, content: String): Int {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_TITLE, title)
                put(COLUMN_CONTENT, content)
                put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            }
            return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        } finally {
            close()
        }
    }

    // Удаление заметки (Delete)
    @Synchronized
    fun deleteNote(id: Long): Int {
        try {
            val db = writableDatabase
            return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        } finally {
            close()
        }
    }
}
