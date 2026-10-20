package com.vividoai.numberlings.ui.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vividoai.numberlings.core.model.ShapeKind
import com.vividoai.numberlings.core.model.TaskVisual
import com.vividoai.numberlings.core.model.VisualKind
import com.vividoai.numberlings.ui.theme.Leaf
import com.vividoai.numberlings.ui.theme.Rose
import com.vividoai.numberlings.ui.theme.Sky
import com.vividoai.numberlings.ui.theme.Sunflower
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Warm paper-cut-out art, all drawn by hand in Canvas. No image files to ship,
 * no licences to carry, and everything looks crisp on any screen (≤25 MB APK).
 */
@Composable
fun PuzzleArt(visual: TaskVisual, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        when (visual.kind) {
            VisualKind.OBJECTS -> drawLooseThings(visual.left, Sunflower)
            VisualKind.JOIN -> {
                drawLooseThings(visual.left, Sunflower, top = true)
                drawLooseThings(visual.right, Sky, top = false)
            }
            VisualKind.TAKE_AWAY -> drawTakeAway(visual.left, visual.right)
            VisualKind.DIFFERENCE -> drawDifference(visual.left, visual.right)
            VisualKind.NUMBER_LINE -> drawHopLine(visual.from, visual.hops)
            VisualKind.GROUPS_ARRAY -> drawArray(visual.rows, visual.cols)
            VisualKind.PLATES -> drawPlates(visual.left, visual.parts)
            VisualKind.PIZZA -> drawPizza(visual.outOf)
            VisualKind.SHAPES -> drawShape(visual.shape)
        }
    }
}

// ---- shared canvas building blocks ----------------------------------------

private fun DrawScope.center() = Offset(size.width / 2f, size.height / 2f)

private fun DrawScope.concentric(
    count: Int,
    color: Color,
    topRow: Boolean = true
) {
    if (count <= 0) return
    val ys = if (count <= 7) listOf(if (topRow) 0.3f else 0.7f) else listOf(0.2f, 0.5f, 0.8f)
    val perRow = ((count + ys.size - 1) / ys.size)
    var index = 0
    for (rowOffset in 0 until ys.size) {
        val rowY = size.height * ys[rowOffset]
        val dotCount = minOf(perRow, count - rowOffset * perRow)
        for (i in 0 until dotCount) {
            val xTrace = (i + 1) * size.width / (dotCount + 1)
            val yTrace = rowY
            drawCircle(color = color, radius = size.minDimension / 14f, center = Offset(xTrace, yTrace))
            index++
        }
    }
}

/** Loose skittles (counting, comparing, simple story problems). */
private fun DrawScope.drawLooseThings(count: Int, color: Color, top: Boolean = true) {
    if (count <= 0) return
    val filled = concentricPrefix(
        count = count,
        color = color,
        yList = when {
            count <= 12 && top -> listOf(0.25f, 0.5f)
            count <= 12 -> listOf(0.75f, 0.5f)
            // Counts above twelve get three slimmer rows so every skittle
            // stays visible — a hidden dot would make the puzzle a lie.
            top -> listOf(0.15f, 0.3f, 0.45f)
            else -> listOf(0.55f, 0.7f, 0.85f)
        }
    )
}

private fun DrawScope.concentricPrefix(count: Int, color: Color, yList: List<Float>): Int {
    var drawn = 0
    val rows = maxOf(1, (count + 5) / 6).coerceAtMost(yList.size)
    for (rowIndex in 0 until rows) {
        val rowY = size.height * yList[rowIndex.coerceIn(0, yList.size - 1)]
        val remaining = count - drawn
        if (remaining <= 0) break
        val rowDots = min(6, remaining)
        for (i in 0 until rowDots) {
            val dotX = (i + 1) * size.width / (rowDots + 1)
            drawCircle(color = color, radius = size.minDimension / 18f, center = Offset(dotX, rowY))
            drawn++
        }
    }
    return drawn
}

// --- convenience-ln wrappers ------------------------------------------------

private fun DrawScope.drawTakeAway(total: Int, takenAway: Int) {
    val left = (total - takenAway).coerceAtLeast(0)
    // Left side: treats still in the basket.
    for (i in 0 until left) {
        val column = i % 5
        val row = i / 5
        drawCircle(
            color = Leaf,
            radius = size.minDimension / 20f,
            center = Offset(
                size.width * (column + 1) / 6f,
                size.height * (row + 1) / 4f
            )
        )
    }
    // Crossed-out zones: gentle dashed circles show what is gone.
    for (i in 0 until takenAway.coerceAtMost(8)) {
        val cx = size.width * (i + 1) / takenAway.coerceAtLeast(1)
        drawCircle(
            color = Rose,
            radius = size.minDimension / 20f,
            center = Offset(cx, size.height * 3 / 4f),
            alpha = 0.45f
        )
    }
}

private fun DrawScope.drawDifference(a: Int, b: Int) {
    for (i in 0 until a) {
        drawCircle(
            color = Rose,
            radius = size.minDimension / 22f,
            center = Offset(size.width * (i + 1) / (a + 1f), size.height * 0.28f)
        )
    }
    for (i in 0 until b) {
        drawCircle(
            color = Sky,
            radius = size.minDimension / 22f,
            center = Offset(size.width * (i + 1) / (b + 1f), size.height * 0.72f)
        )
    }
}

private fun DrawScope.drawHopLine(from: Int, hops: Int) {
    val lineY = size.height / 2f
    drawLine(
        color = Leaf,
        strokeWidth = 5f,
        start = Offset(size.width * 0.08f, lineY),
        end = Offset(size.width * 0.92f, lineY)
    )
    // Landing pad circle near where the hops end.
    val landingX = size.width * 0.92f
    drawCircle(color = Sunflower, radius = size.minDimension / 12f, center = Offset(landingX, lineY))
    when (hops) {
        in 0..20 -> drawCircle(
            color = Sunflower.copy(alpha = 0.35f),
            radius = size.minDimension / 12f + hops * 2f,
            center = Offset(landingX, lineY)
        )
    }
}

private fun DrawScope.drawArray(rows: Int, cols: Int) {
    val radius = size.minDimension / (rows + cols) / 1.6f
    for (row in 0 until rows) {
        for (column in 0 until cols) {
            drawCircle(
                color = Sunflower,
                radius = radius,
                center = Offset(
                    size.width * (column + 1) / (cols + 1f),
                    size.height * (row + 1) / (rows + 1f)
                )
            )
            drawRect(
                color = Color(0xFFF2B84B).copy(alpha = 0.15f),
                topLeft = Offset(
                    size.width * (column + 1) / (cols + 1f) - radius,
                    size.height * (row + 1) / (rows + 1f) - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
    }
}

private fun DrawScope.drawPlates(things: Int, plates: Int) {
    // Plates row, then add the things spread evenly.
    for (i in 0 until plates) {
        val plateX = size.width * (i + 1) / (plates + 1f)
        drawCircle(
            color = Sky.copy(alpha = 0.3f),
            radius = size.minDimension / 6f,
            center = Offset(plateX, size.height * 0.72f),
        )
    }
    val perPlate = things / plates
    val leftovers = things % plates
    if (leftovers == 0) drawCakesEven(plates, perPlate)
}

private fun DrawScope.drawCakesEven(plates: Int, perPlate: Int) {
    for (plate in 0 until plates) {
        val plateX = size.width * (plate + 1) / (plates + 1f)
        repeat(perPlate) { cakeIndex ->
            drawCircle(
                color = Rose,
                radius = size.minDimension / 26f,
                center = Offset(
                    plateX + cos(cakeIndex * 3f) * size.minDimension / 40f,
                    size.height * 0.72f + sin(cakeIndex * 3f) * size.minDimension / 40f
                )
            )
        }
    }
}

private fun DrawScope.drawPizza(slices: Int) {
    val radius = min(size.width, size.height) / 3f
    val center = center()
    for (i in 0 until slices) {
        val sliceStart = (i * 2 * Math.PI / slices).toFloat()
        drawArc(
            color = Sunflower.copy(alpha = if (i % 2 == 0) 0.85f else 0.6f),
            startAngle = Math.toDegrees(sliceStart.toDouble()).toFloat(),
            sweepAngle = (360f / slices),
            useCenter = true
        )
        // Radial spokes help tiny eyes see equal parts.
        drawLine(
            color = Color(0xFF8A5A7B),
            strokeWidth = 3f,
            start = center,
            end = Offset(
                center.x + radius * cos(sliceStart),
                center.y + radius * sin(sliceStart)
            )
        )
    }
}

private fun DrawScope.drawShape(kind: ShapeKind) {
    val center = center()
    val radius = min(size.width, size.height) / 4f
    when (kind) {
        ShapeKind.CIRCLE -> drawCircle(color = Sunflower, radius = radius, center = center)
        ShapeKind.TRIANGLE -> drawEquilateralTriangle(center, radius)
        ShapeKind.RECTANGLE -> drawRoundedRect(center, radius)
        ShapeKind.HEART -> drawHeartish(center, radius)
        ShapeKind.STAR -> drawStar(center, radius)
    }
}

private fun DrawScope.drawEquilateralTriangle(center: Offset, radius: Float) {
    val points = listOf(
        Offset(center.x, center.y - radius),
        Offset(center.x - radius, center.y + radius * 0.9f),
        Offset(center.x + radius, center.y + radius * 0.9f)
    )
    drawPathLoop(points, Sunflower)
}

private fun DrawScope.drawPathLoop(points: List<Offset>, color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { to -> lineTo(to.x, to.y) }
        close()
    }
    drawPath(color = color, path = path)
}

private fun DrawScope.drawRoundedRect(center: Offset, radius: Float) {
    drawRoundRect(color = Leaf,
        topLeft = Offset(center.x - radius, center.y - radius / 1.5f),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 1.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius / 5f)
    )
}

private fun DrawScope.drawHeartish(center: Offset, radius: Float) {
    drawCircle(color = Rose, radius = radius / 2f * 1.2f, center = center + Offset(-radius / 3f, -radius / 4f))
    drawCircle(color = Rose, radius = radius / 2f * 1.2f, center = center + Offset(radius / 3f, -radius / 4f))
    drawPathLoop(
        listOf(
            Offset(center.x - radius / 2f * 0.9f, center.y),
            Offset(center.x, center.y + radius / 1.4f),
            Offset(center.x + radius / 2f * 0.9f, center.y)
        ),
        Rose
    )
}

private fun DrawScope.drawStar(center: Offset, radius: Float) {
    val points = mutableListOf<Offset>()
    for (tip in 0 until 10) {
        val spoke = if (tip % 2 == 0) radius else radius / 2.4f
        val theta = Math.PI * tip / 5f
        points.add(Offset(
            center.x + (spoke * sin(theta)).toFloat(),
            center.y - (spoke * cos(theta)).toFloat()
        ))
    }
    drawPathLoop(points, Sunflower)
}
