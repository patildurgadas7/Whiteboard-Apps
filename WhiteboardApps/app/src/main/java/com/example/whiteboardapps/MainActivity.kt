package com.example.whiteboardapps

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
    }

    // ===== COLORS =====

    fun setRed(view: View) = drawingView.setColor(Color.RED)
    fun setBlue(view: View) = drawingView.setColor(Color.BLUE)
    fun setGreen(view: View) = drawingView.setColor(Color.GREEN)
    fun setYellow(view: View) = drawingView.setColor(Color.YELLOW)
    fun setBlack(view: View) = drawingView.setColor(Color.BLACK)

    // ===== STROKE =====

    fun setThin(view: View) = drawingView.setStrokeWidth(4f)
    fun setMedium(view: View) = drawingView.setStrokeWidth(8f)
    fun setThick(view: View) = drawingView.setStrokeWidth(16f)

    // ===== ACTIONS =====

    fun clearCanvas(view: View) = drawingView.clear()

    fun setEraser(view: View) = drawingView.toggleEraser()

    // ===== SHAPES =====

    fun setRectangle(view: View) = drawingView.setMode(Mode.RECTANGLE)
    fun setCircle(view: View) = drawingView.setMode(Mode.CIRCLE)
    fun setLine(view: View) = drawingView.setMode(Mode.LINE)
    fun setPolygon(view: View) = drawingView.setMode(Mode.POLYGON)
    fun setText(view: View) = drawingView.setMode(Mode.TEXT)
    fun setDraw(view: View) = drawingView.setMode(Mode.DRAW)

    // ===== SAVE =====

    fun saveDrawing(view: View) {

        val data = drawingView.exportData()

        val gson = Gson()
        val json = gson.toJson(data)

        val fileName = "whiteboard_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date()) + ".json"

        val file = File(filesDir, fileName)
        file.writeText(json)
    }

    // ===== LOAD =====

    fun loadDrawing(view: View) {

        val files = filesDir.listFiles() ?: return
        if (files.isEmpty()) return

        // get latest file safely
        val latestFile = files.maxByOrNull { it.lastModified() } ?: return

        val json = latestFile.readText()
        if (json.isEmpty()) return

        val gson = Gson()

        try {
            val data = gson.fromJson(json, WhiteboardData::class.java)
            drawingView.loadData(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}