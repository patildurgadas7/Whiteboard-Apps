package com.example.whiteboardapps

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import kotlin.math.*
import com.google.gson.Gson

enum class Mode {
    DRAW, RECTANGLE, CIRCLE, LINE, POLYGON, TEXT
}

// ===== MODELS =====

data class Stroke(val path: Path, val color: Int, val width: Float)

data class Shape(
    val type: Mode,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Int
)

data class TextItem(
    var text: String,
    var x: Float,
    var y: Float,
    var color: Int,
    var size: Float
)

// ===== SAVE MODELS =====

data class StrokeData(
    val points: List<List<Float>>,
    val color: String,
    val width: Float
)

data class ShapeData(
    val type: String,
    val start: List<Float>,
    val end: List<Float>,
    val color: String
)

data class TextData(
    val text: String,
    val position: List<Float>,
    val color: String,
    val size: Float
)

data class WhiteboardData(
    val strokes: List<StrokeData>,
    val shapes: List<ShapeData>,
    val texts: List<TextData>
)

// ===== VIEW =====

class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var currentColor = Color.BLACK
    private var strokeWidth = 8f
    private var currentMode = Mode.DRAW

    private val strokes = mutableListOf<Stroke>()
    private val shapes = mutableListOf<Shape>()
    private val texts = mutableListOf<TextItem>()

    private var currentPath = Path()

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f

    private var isEraser = false

    private var selectedText: TextItem? = null
    private var isDraggingText = false

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.WHITE)

        // Draw strokes
        for (stroke in strokes) {
            paint.color = stroke.color
            paint.strokeWidth = stroke.width
            canvas.drawPath(stroke.path, paint)
        }

        // Draw shapes
        for (shape in shapes) {
            paint.color = shape.color

            when (shape.type) {
                Mode.RECTANGLE -> canvas.drawRect(shape.startX, shape.startY, shape.endX, shape.endY, paint)

                Mode.CIRCLE -> {
                    val r = hypot(
                        (shape.endX - shape.startX).toDouble(),
                        (shape.endY - shape.startY).toDouble()
                    ).toFloat()
                    canvas.drawCircle(shape.startX, shape.startY, r, paint)
                }

                Mode.LINE -> canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, paint)

                Mode.POLYGON -> {
                    val path = Path()
                    val sides = 6
                    val cx = (shape.startX + shape.endX) / 2
                    val cy = (shape.startY + shape.endY) / 2
                    val r = hypot(
                        (shape.endX - shape.startX).toDouble(),
                        (shape.endY - shape.startY).toDouble()
                    ).toFloat() / 2

                    for (i in 0 until sides) {
                        val angle = 2 * Math.PI * i / sides
                        val x = (cx + r * cos(angle)).toFloat()
                        val y = (cy + r * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }

                else -> {}
            }
        }

        // Draw text
        for (t in texts) {
            paint.color = t.color
            paint.textSize = t.size
            paint.style = Paint.Style.FILL
            canvas.drawText(t.text, t.x, t.y, paint)
        }

        // Preview shapes
        if (currentMode != Mode.DRAW && currentMode != Mode.TEXT) {
            paint.color = currentColor

            when (currentMode) {
                Mode.RECTANGLE -> canvas.drawRect(startX, startY, endX, endY, paint)
                Mode.CIRCLE -> {
                    val r = hypot((endX - startX).toDouble(), (endY - startY).toDouble()).toFloat()
                    canvas.drawCircle(startX, startY, r, paint)
                }
                Mode.LINE -> canvas.drawLine(startX, startY, endX, endY, paint)
                else -> {}
            }
        } else {
            paint.color = currentColor
            paint.strokeWidth = strokeWidth
            canvas.drawPath(currentPath, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        // TEXT MODE
        if (currentMode == Mode.TEXT) {

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    val clicked = texts.find {
                        val w = paint.measureText(it.text)
                        val h = it.size
                        event.x in it.x..(it.x + w) &&
                                event.y in (it.y - h)..it.y
                    }

                    if (clicked != null) {
                        selectedText = clicked
                        isDraggingText = true
                    } else {
                        showTextDialog(event.x, event.y)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDraggingText && selectedText != null) {
                        selectedText!!.x = event.x
                        selectedText!!.y = event.y
                        invalidate()
                    }
                }

                MotionEvent.ACTION_UP -> isDraggingText = false
            }

            return true
        }

        // DRAW
        if (currentMode == Mode.DRAW) {

            if (isEraser) {
                texts.removeAll {
                    abs(it.x - event.x) < 60 && abs(it.y - event.y) < 60
                }
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> currentPath.moveTo(event.x, event.y)
                MotionEvent.ACTION_MOVE -> currentPath.lineTo(event.x, event.y)
                MotionEvent.ACTION_UP -> {
                    strokes.add(Stroke(Path(currentPath), currentColor, strokeWidth))
                    currentPath.reset()
                }
            }
        } else {
            // SHAPES
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    endX = event.x
                    endY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    shapes.add(Shape(currentMode, startX, startY, event.x, event.y, currentColor))
                }
            }
        }

        invalidate()
        return true
    }

    private fun showTextDialog(x: Float, y: Float) {
        val input = EditText(context)

        AlertDialog.Builder(context)
            .setTitle("Enter Text")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    texts.add(TextItem(text, x, y, currentColor, 50f))
                    invalidate()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ===== SAVE =====

    fun exportData(): WhiteboardData {

        val strokeList = strokes.map { stroke ->
            val pathMeasure = PathMeasure(stroke.path, false)
            val points = mutableListOf<List<Float>>()

            val pos = FloatArray(2)
            var distance = 0f

            while (distance < pathMeasure.length) {
                pathMeasure.getPosTan(distance, pos, null)
                points.add(listOf(pos[0], pos[1]))
                distance += 5
            }

            StrokeData(points,
                String.format("#%06X", 0xFFFFFF and stroke.color),
                stroke.width)
        }

        val shapeList = shapes.map {
            ShapeData(
                it.type.name,
                listOf(it.startX, it.startY),
                listOf(it.endX, it.endY),
                String.format("#%06X", 0xFFFFFF and it.color)
            )
        }

        val textList = texts.map {
            TextData(
                it.text,
                listOf(it.x, it.y),
                String.format("#%06X", 0xFFFFFF and it.color),
                it.size
            )
        }

        return WhiteboardData(strokeList, shapeList, textList)
    }

    fun loadData(data: WhiteboardData) {

        strokes.clear()
        shapes.clear()
        texts.clear()

        for (s in data.strokes) {
            val path = Path()
            if (s.points.isNotEmpty()) {
                path.moveTo(s.points[0][0], s.points[0][1])
                for (p in s.points) path.lineTo(p[0], p[1])
            }
            strokes.add(Stroke(path, Color.parseColor(s.color), s.width))
        }

        for (sh in data.shapes) {
            shapes.add(
                Shape(
                    Mode.valueOf(sh.type),
                    sh.start[0],
                    sh.start[1],
                    sh.end[0],
                    sh.end[1],
                    Color.parseColor(sh.color)
                )
            )
        }

        for (t in data.texts) {
            texts.add(
                TextItem(
                    t.text,
                    t.position[0],
                    t.position[1],
                    Color.parseColor(t.color),
                    t.size
                )
            )
        }

        invalidate()
    }

    // ===== CONTROLS =====

    fun setColor(color: Int) {
        currentColor = color
        isEraser = false
        currentMode = Mode.DRAW
    }

    fun setStrokeWidth(width: Float) {
        strokeWidth = width
    }

    fun toggleEraser() {
        isEraser = !isEraser
        currentColor = if (isEraser) Color.WHITE else Color.BLACK
        currentMode = Mode.DRAW
    }

    fun setMode(mode: Mode) {
        currentMode = mode
    }

    fun clear() {
        strokes.clear()
        shapes.clear()
        texts.clear()
        currentPath.reset()
        invalidate()
    }
}