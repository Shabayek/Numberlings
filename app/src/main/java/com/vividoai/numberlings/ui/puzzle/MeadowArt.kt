package com.vividoai.numberlings.ui.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vividoai.numberlings.core.model.Meadow
import kotlin.math.cos
import kotlin.math.sin

/**
 * One warm illustration per meadow for the village map — pure Canvas again,
 * tiny and licence-clean. Each drawing shows what the meadow teaches:
 *
 *  COUNT   → a sunny meadow of five little flowers
 *  ADD     → two stone piles meeting with a golden plus sign
 *  SUB     → a brook with fallen treats drifting away
 *  TIMES   → a tidy orchard grid of little trees
 *  SHARE   → a long table with two plates sharing cakes
 *  SHAPE   → a patchwork forest of triangles, circles and a house
 *
 * The village map shows each drawing softly dimmed when its meadow is still asleep,
 * so the art invites without shouting. */
@Composable
fun MeadowArt(meadow: Meadow, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val ground = when (meadow) {
            Meadow.COUNT -> Color(0xFFB9DCA0)
            Meadow.ADD -> Color(0xFFEBB9B9)
            Meadow.SUB -> Color(0xFFA9CBE0)
            Meadow.TIMES -> Color(0xFFE8D9A2)
            Meadow.SHARE -> Color(0xFFC3B8DF)
            Meadow.SHAPE -> Color(0xFFD6C0AB)
        }
        val sky = Color(0xFFFFF3E0)
        drawRect(brush = Brush.verticalGradient(listOf(sky, ground)), size = size)
        // Rolling hill horizon under every meadow, so each card shares one horizon line.
        drawCircle(
            color = ground.copy(alpha = 0.55f),
            radius = size.width / 0.9f,
            center = Offset(size.width / 2f, size.height * 1.25f)
        )
        when (meadow) {
            Meadow.COUNT -> countGrove()
            Meadow.ADD -> addBridge()
            Meadow.SUB -> subBrook()
            Meadow.TIMES -> timesOrchard()
            Meadow.SHARE -> shareTable()
            Meadow.SHAPE -> shapeForest()
        }
    }
}

private val Petal = Color(0xFFE58F8F)
private val Leaf = Color(0xFF6FA07A)
private val Wood = Color(0xFFB78456)
private val Stone = Color(0xFFC9C2B8)
private val Water = Color(0xFF9FC7DD)
private val Sunflower = Color(0xFFF2B84B)
private val CreamPlate = Color(0xFFFDF6E3)
private val Berry = Color(0xFF8A5A7B)

private fun DrawScope.countGrove() {
    val positions = listOf(0.18f, 0.34f, 0.5f, 0.66f, 0.82f)
    val colors = listOf(Petal, Sunflower, Water, Petal, Leaf)
    positions.forEachIndexed { index, fx ->
        val stemBase = Offset(size.width * fx, size.height * 0.82f)
        drawLine(Leaf, stemBase, Offset(stemBase.x, size.height * 0.62f), size.height / 22f)
        drawCircle(
            color = colors[index],
            radius = size.height / 13f,
            center = Offset(stemBase.x, size.height * 0.56f)
        )
        drawCircle(
            color = Sunflower.copy(alpha = 0.9f),
            radius = size.height / 34f,
            center = Offset(stemBase.x, size.height * 0.56f)
        )
    }
    // One friendly sun.
    drawCircle(
        color = Sunflower,
        radius = size.height / 10f,
        center = Offset(size.width * 0.86f, size.height * 0.16f)
    )
}

private fun DrawScope.addBridge() {
    val stoneY = size.height * 0.72f
    repeat(3) { i ->
        drawCircle(Stone, size.height / 14f, Offset(size.width * (0.14f + 0.1f * i), stoneY))
    }
    repeat(2) { i ->
        drawCircle(Stone.copy(alpha = 0.85f), size.height / 14f, Offset(size.width * (0.72f + 0.1f * i), stoneY))
    }
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arm = size.height / 4.5f
    drawRect(Sunflower, topLeft = Offset(cx - arm / 6f, cy - arm), size = androidx.compose.ui.geometry.Size(arm / 3f, arm * 2f))
    drawRect(Sunflower, topLeft = Offset(cx - arm, cy - arm / 6f), size = androidx.compose.ui.geometry.Size(arm * 2f, arm / 3f))
}

private fun DrawScope.subBrook() {
    // Water band.
    drawRect(
        Water,
        topLeft = Offset(0f, size.height * 0.58f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.26f)
    )
    // Basket on the bank.
    drawRoundRect(
        Wood,
        topLeft = Offset(size.width * 0.12f, size.height * 0.3f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.2f, size.height * 0.24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 20f)
    )
    // Treats drifting away.
    val spots = listOf(0.42f, 0.55f, 0.68f, 0.8f)
    spots.forEachIndexed { index, fx ->
        drawCircle(
            Petal.copy(alpha = 0.85f - index * 0.12f),
            size.height / 16f,
            Offset(size.width * fx, size.height * 0.7f + index * size.height * 0.02f)
        )
    }
}

private fun DrawScope.timesOrchard() {
    repeat(2) { row ->
        repeat(3) { col ->
            val x = size.width * (0.2f + 0.3f * col)
            val y = size.height * (0.42f + 0.28f * row)
            drawRect(Wood, topLeft = Offset(x - size.height / 44f, y), size = androidx.compose.ui.geometry.Size(size.height / 22f, size.height * 0.14f))
            drawCircle(Leaf, size.height / 12f, Offset(x, y))
            drawCircle(Sunflower.copy(alpha = 0.85f), size.height / 30f, Offset(x, y))
        }
    }
}

private fun DrawScope.shareTable() {
    drawRect(
        Wood,
        topLeft = Offset(size.width * 0.14f, size.height * 0.6f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.72f, size.height / 16f)
    )
    listOf(0.28f, 0.72f).forEach { fx ->
        val plate = Offset(size.width * fx, size.height * 0.585f)
        drawCircle(CreamPlate, size.height / 10f, plate)
        repeat(3) { i ->
            val angle = i * 2.09f
            drawCircle(
                Petal,
                size.height / 26f,
                Offset(plate.x + (cos(angle) * size.height / 26f), plate.y + (sin(angle) * size.height / 26f))
            )
        }
    }
}

private fun DrawScope.shapeForest() {
    // Triangle trees.
    listOf(0.2f, 0.5f).forEachIndexed { index, fx ->
        val base = Offset(size.width * fx, size.height * 0.84f)
        val height = size.height * (if (index == 0) 0.4f else 0.3f)
        drawPathLoop(
            listOf(base, Offset(base.x + size.width * 0.09f, base.y), Offset(base.x, base.y - height)),
            Leaf
        )
    }
    // A circle bush.
    drawCircle(Leaf.copy(alpha = 0.8f), size.height / 9f, Offset(size.width * 0.74f, size.height * 0.8f))
    // The little shape-house.
    val houseLeft = size.width * 0.8f
    drawRect(
        CreamPlate,
        topLeft = Offset(houseLeft, size.height * 0.7f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.14f, size.height * 0.14f)
    )
    drawPathLoop(
        listOf(
            Offset(houseLeft - size.width * 0.012f, size.height * 0.7f),
            Offset(houseLeft + size.width * 0.078f, size.height * 0.7f),
            Offset(houseLeft + size.width * 0.068f, size.height * 0.605f),
            Offset(houseLeft, size.height * 0.585f)
        ),
        Berry
    )
}

private fun DrawScope.drawPathLoop(points: List<Offset>, color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        close()
    }
    drawPath(path = path, color = color)
}
