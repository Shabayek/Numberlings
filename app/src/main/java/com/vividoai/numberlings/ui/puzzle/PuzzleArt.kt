package com.vividoai.numberlings.ui.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.vividoai.numberlings.core.model.CountLayout
import com.vividoai.numberlings.core.model.ShapeKind
import com.vividoai.numberlings.core.model.TaskVisual
import com.vividoai.numberlings.core.model.VisualKind
import com.vividoai.numberlings.ui.theme.Cocoa
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
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        when (visual.kind) {
            VisualKind.OBJECTS -> drawLooseThings(visual.left, Sunflower, 0.10f, 0.90f)
            VisualKind.JOIN -> {
                drawLooseThings(visual.left, Sunflower, 0.05f, 0.45f)
                drawLooseThings(visual.right, Sky, 0.55f, 0.95f)
            }
            VisualKind.PARTIAL_JOIN -> {
                drawLooseThings(visual.left, Sunflower, 0.06f, 0.52f)
                drawEmptySlot(0.76f)
            }
            VisualKind.TAKE_AWAY -> drawTakeAway(visual.left, visual.right)
            VisualKind.DIFFERENCE -> drawDifference(visual.left, visual.right)
            VisualKind.NUMBER_LINE -> drawHopLine(visual.from, visual.hops, visual.step, textMeasurer)
            VisualKind.GROUPS_ARRAY -> drawArray(visual.rows, visual.cols)
            VisualKind.PLATES -> drawPlates(visual.left, visual.parts)
            VisualKind.PIZZA -> drawPizza(visual.outOf)
            VisualKind.SHAPES -> drawShape(visual.shape)
        }
    }
}

// ---- shared canvas building blocks ----------------------------------------

private fun DrawScope.center() = Offset(size.width / 2f, size.height / 2f)

/**
 * Loose skittles (counting, comparing, simple story problems).
 *
 * The picture must never lie: every count the engine asks for is drawn, all of
 * it, or the child counts the dots and is told they are wrong. Rows grow with
 * the number (up to twenty) and the dots shrink a little so they always fit.
 */
private fun DrawScope.drawLooseThings(
    count: Int,
    color: Color,
    bandTop: Float,
    bandBottom: Float
) {
    if (count <= 0) return
    val layout = CountLayout.of(count)
    val bandHeight = size.height * (bandBottom - bandTop)
    val radius = min(size.minDimension / 18f, bandHeight / (layout.rows * 2.8f))
        .coerceAtLeast(size.minDimension / 70f)
    for (i in 0 until count) {
        val row = i / layout.perRow
        val inRow = i % layout.perRow
        val inThisRow = minOf(layout.perRow, count - row * layout.perRow)
        val dotX = (inRow + 1) * size.width / (inThisRow + 1f)
        val dotY = if (layout.rows == 1) {
            size.height * (bandTop + bandBottom) / 2f
        } else {
            size.height * (bandTop + (bandBottom - bandTop) * row / (layout.rows - 1f))
        }
        drawCircle(color = color, radius = radius, center = Offset(dotX, dotY))
    }
}

/** An empty, dashed space where something is still missing — never counted. */
private fun DrawScope.drawEmptySlot(centerY: Float) {
    val slotRadius = size.minDimension / 9f
    val center = Offset(size.width / 2f, size.height * centerY)
    val dash = 14f
    var angle = 0f
    while (angle < 360f) {
        val start = Offset(
            center.x + slotRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
            center.y + slotRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        val endAngle = angle + dash * 1.6f
        val end = Offset(
            center.x + slotRadius * cos(Math.toRadians(endAngle.toDouble())).toFloat(),
            center.y + slotRadius * sin(Math.toRadians(endAngle.toDouble())).toFloat()
        )
        drawLine(color = Cocoa.copy(alpha = 0.45f), strokeWidth = 4f, start = start, end = end)
        angle += dash * 2f
    }
    // A little plus: "more comes here", without saying how much.
    val arm = slotRadius * 0.45f
    drawLine(
        color = Cocoa.copy(alpha = 0.6f),
        strokeWidth = 5f,
        start = Offset(center.x - arm, center.y),
        end = Offset(center.x + arm, center.y)
    )
    drawLine(
        color = Cocoa.copy(alpha = 0.6f),
        strokeWidth = 5f,
        start = Offset(center.x, center.y - arm),
        end = Offset(center.x, center.y + arm)
    )
}

// --- convenience-ln wrappers ------------------------------------------------

private fun DrawScope.drawTakeAway(total: Int, takenAway: Int) {
    val all = total.coerceAtLeast(0)
    if (all == 0) return
    val gone = takenAway.coerceIn(0, all)
    val perRow = if (all <= 8) 4 else 5
    val rows = (all + perRow - 1) / perRow
    val radius = min(size.minDimension / 20f, size.height / (rows * 3.2f))
        .coerceAtLeast(size.minDimension / 70f)
    // Everything that was there is still pictured, so the child can count the
    // starting amount. The ones on their way out are faded with a small
    // swoosh — one set, never a second pile that hands over the answer.
    for (i in 0 until all) {
        val row = i / perRow
        val inRow = i % perRow
        val inThisRow = minOf(perRow, all - row * perRow)
        val dotX = (inRow + 1) * size.width / (inThisRow + 1f)
        val dotY = size.height * (0.22f + 0.36f * row / maxOf(1f, rows - 1f))
        val center = Offset(dotX, dotY)
        val leaving = i < gone
        drawCircle(
            color = if (leaving) Leaf.copy(alpha = 0.3f) else Leaf,
            radius = radius,
            center = center
        )
        if (leaving) {
            // a small arc: this one is on its way out
            drawArc(
                color = Rose.copy(alpha = 0.75f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.5f, center.y - radius * 2.2f),
                size = androidx.compose.ui.geometry.Size(radius * 3f, radius * 2.4f)
            )
        }
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

/**
 * The hopping line. A pre-reader needs to see where counting starts and how
 * big the hops are, so the line carries its own start mark, one mark per hop,
 * and the landing pad at the end.
 */
private fun DrawScope.drawHopLine(from: Int, hops: Int, step: Int, textMeasurer: TextMeasurer) {
    val lineY = size.height / 2f
    val startX = size.width * 0.12f
    val endX = size.width * 0.88f
    drawLine(
        color = Leaf,
        strokeWidth = 5f,
        start = Offset(startX, lineY),
        end = Offset(endX, lineY)
    )

    val total = hops.coerceAtLeast(0)
    if (step > 0 && total > 0) {
        val marks = (total / step).coerceAtLeast(1)
        for (i in 0..marks) {
            val x = startX + (endX - startX) * i / marks
            drawLine(
                color = Leaf.copy(alpha = 0.45f),
                strokeWidth = 3f,
                start = Offset(x, lineY - 13f),
                end = Offset(x, lineY + 13f)
            )
        }
        // one arc per hop: the frog's own counting
        for (i in 0 until marks) {
            val x0 = startX + (endX - startX) * i / marks
            val x1 = startX + (endX - startX) * (i + 1) / marks
            drawArc(
                color = Sunflower.copy(alpha = 0.55f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x0, lineY - 24f),
                size = androidx.compose.ui.geometry.Size(x1 - x0, 48f)
            )
        }
    }

    // The landing pad, and the numbers the line counts between.
    val landingX = endX
    drawCircle(color = Sunflower, radius = size.minDimension / 12f, center = Offset(landingX, lineY))

    val numberStyle = TextStyle(color = Cocoa, fontSize = 15.sp)
    val startLabel = textMeasurer.measure("0", style = numberStyle)
    drawText(
        startLabel,
        topLeft = Offset(startX - startLabel.size.width / 2f, lineY + 30f)
    )
    val endLabel = textMeasurer.measure(total.toString(), style = numberStyle)
    drawText(
        endLabel,
        topLeft = Offset(landingX - endLabel.size.width / 2f, lineY + 30f)
    )
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

/**
 * Sharing: the treats waiting to be shared, in one honest row, and the plates
 * they will go onto. The picture deliberately does not do the dividing for the
 * child — the answer is worked out, not counted off the plate.
 */
private fun DrawScope.drawPlates(things: Int, plates: Int) {
    drawLooseThings(things, Sunflower, 0.08f, 0.44f)
    val plateRadius = size.minDimension / 7f
    for (i in 0 until plates) {
        val plateX = size.width * (i + 1) / (plates + 1f)
        val center = Offset(plateX, size.height * 0.74f)
        drawCircle(color = Sky.copy(alpha = 0.22f), radius = plateRadius, center = center)
        // A dashed rim reads as "waiting for treats", not as missing art.
        var angle = 0f
        while (angle < 360f) {
            val start = Offset(
                center.x + plateRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
                center.y + plateRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            )
            val endAngle = angle + 12f
            val end = Offset(
                center.x + plateRadius * cos(Math.toRadians(endAngle.toDouble())).toFloat(),
                center.y + plateRadius * sin(Math.toRadians(endAngle.toDouble())).toFloat()
            )
            drawLine(color = Sky.copy(alpha = 0.7f), strokeWidth = 4f, start = start, end = end)
            angle += 24f
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
