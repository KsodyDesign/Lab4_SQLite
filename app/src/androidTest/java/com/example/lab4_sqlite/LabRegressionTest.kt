package com.example.lab4_sqlite

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.widget.EditText
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class LabRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var activity: Activity
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun launch(type: Class<out Activity>) {
        activity = instrumentation.startActivitySync(Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        main { activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        Thread.sleep(1200)
        instrumentation.waitForIdleSync()
        current()
    }
    private fun enter(id: Int, text: String) = main { activity.findViewById<EditText>(id).setText(text) }
    private fun click(id: Int) {
        Thread.sleep(2400)
        main { activity.findViewById<android.view.View>(id).performClick() }
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
    }
    private fun text(id: Int): String {
        var result = ""
        main { result = activity.findViewById<TextView>(id).text.toString() }
        return result
    }
    private fun current() {
        main { activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first() }
    }
    private fun rotate() {
        val old = activity
        main { activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        Thread.sleep(1800)
        instrumentation.waitForIdleSync()
        current()
        assertNotSame("Activity must recreate on rotation", old, activity)
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(1000)
        val file = File(context.getExternalFilesDir(null), "audit/$name.png")
        file.parentFile!!.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    private fun finish() {
        main { activity.finish() }
        instrumentation.waitForIdleSync()
    }
    private fun field(name: String): Any? {
        val field = activity.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(activity)
    }

    @Test fun sqliteCrudAndPersistence() {
        val db = NotesDbHelper(context)
        db.getAllNotes().forEach { db.deleteNote(it.id) }
        launch(MainActivity::class.java)
        click(R.id.btnAdd)
        assertTrue(db.getAllNotes().isEmpty())
        screenshot("01-empty-title")
        enter(R.id.etTitle, "SQLite title")
        enter(R.id.etContent, "SQLite content")
        click(R.id.btnAdd)
        Thread.sleep(600)
        assertEquals("SQLite title", db.getAllNotes().single().title)
        screenshot("02-inserted")
        finish()
        launch(MainActivity::class.java)
        val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        main { list.findViewHolderForAdapterPosition(0)!!.itemView.performClick() }
        assertEquals("SQLite title", text(R.id.etTitle))
        rotate()
        assertEquals(android.view.View.VISIBLE, activity.findViewById<android.view.View>(R.id.layoutEditButtons).visibility)
        main { activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        Thread.sleep(1600)
        current()
        enter(R.id.etTitle, "Updated SQLite")
        click(R.id.btnUpdate)
        Thread.sleep(600)
        assertEquals("Updated SQLite", db.getAllNotes().single().title)
        screenshot("03-updated")
        main { activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView).findViewHolderForAdapterPosition(0)!!.itemView.performClick() }
        click(R.id.btnDelete)
        Thread.sleep(600)
        assertTrue(db.getAllNotes().isEmpty())
        screenshot("04-deleted")
        finish()
        db.close()
    }
}
