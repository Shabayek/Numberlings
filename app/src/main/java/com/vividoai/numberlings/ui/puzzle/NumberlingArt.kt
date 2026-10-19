package com.vividoai.numberlings.ui.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.Leaf
import com.vividoai.numberlings.ui.theme.Rose

/**
 * How a Numberling is feeling right now. The face changes gently; the body
 * never leaves the friend.
 */
enum class Mood { HAPPY, CHEER, THINK, OOPS, SLEEPY }

/**
 * A Numberling: a small round friend with a sprout on its head. Pure Canvas,
 * like all the art in this game — warm, readable, licence-clean. Every meadow
 * grows its own colour of Numberling, and they mostly just want puzzle friends.
 */
@Composable
fun Numberling(
    modifier: Modifier = Modifier,
    mood: Mood = Mood.HAPPY,
    body: Color = Leaf
) {
    Canvas(modifier = modifier) { drawFriend(mood, body) }
}

private fun DrawScope.drawFriend(mood: Mood, body: Color) {
    val w = size.width
    val h = size.height
    val d = minOf(w, h)

    val rim = darker(body)
    val ink = Cocoa

    // Body: a soft, slightly bottom-heavy oval.
    val bodyLeft = w * 0.10f
    val bodyTop = h * 0.26f
    val bodyW = w * 0.80f
    val bodyH = h * 0.70f
    drawOval(body, topLeft = Offset(bodyLeft, bodyTop), size = Size(bodyW, bodyH))
    drawOval(
        rim,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyW, bodyH),
        style = Stroke(width = d * 0.035f)
    )

    // Sprout on the head: a stem and two little leaves.
    val stemBase = Offset(w / 2f, bodyTop + h * 0.01f)
    val stemTip = Offset(w / 2f, bodyTop - h * 0.11f)
    drawLine(rim, stemBase, stemTip, strokeWidth = d * 0.045f, cap = StrokeCap.Round)
    drawSproutLeaf(stemTip, angle = -125f, length = h * 0.10f, rim)
    drawSproutLeaf(stemTip, angle = -55f, length = h * 0.10f, rim)

    // Cheeks: cozy warmth in every mood.
    drawCircle(Rose.copy(alpha = 0.35f), d * 0.055f, Offset(w * 0.20f, h * 0.60f))
    drawCircle(Rose.copy(alpha = 0.35f), d * 0.055f, Offset(w * 0.80f, h * 0.60f))

    // Eyes.
    val eyeY = h * 0.47f
    val eyeDX = w * 0.16f
    when (mood) {
        Mood.SLEEPY -> {
            drawArc(
                ink, startAngle = 190f, sweepAngle = 160f, useCenter = false,
                topLeft = Offset(w * 0.5f - eyeDX - d * 0.09f, eyeY - d * 0.06f),
                size = Size(d * 0.18f, d * 0.12f),
                style = Stroke(width = d * 0.035f, cap = StrokeCap.Round)
            )
            drawArc(
                ink, startAngle = 190f, sweepAngle = 160f, useCenter = false,
                topLeft = Offset(w * 0.5f + eyeDX - d * 0.09f, eyeY - d * 0.06f),
                size = Size(d * 0.18f, d * 0.12f),
                style = Stroke(width = d * 0.035f, cap = StrokeCap.Round)
            )
        }
        else -> {
            val upWhenThinking = mood == Mood.THINK
            repeat(2) { i ->
                val sign = if (i == 0) -1f else 1f
                val cx = w / 2f + sign * eyeDX
                drawCircle(Color.White, d * 0.085f, Offset(cx, eyeY))
                drawCircle(
                    ink,
                    d * 0.045f,
                    Offset(
                        cx + if (upWhenThinking) sign * d * 0.015f else 0f,
                        eyeY - if (upWhenThinking) d * 0.03f else 0f
                    )
                )
            }
        }
    }

    // Mouth.
    val mouthY = h * 0.66f
    val mouthW = d * 0.24f
    val mouthH = d * 0.14f
    when (mood) {
        Mood.HAPPY -> drawArc(
            rim, startAngle = 15f, sweepAngle = 150f, useCenter = false,
            topLeft = Offset(w / 2f - mouthW / 2f, mouthY - mouthH / 2f),
            size = Size(mouthW, mouthH),
            style = Stroke(width = d * 0.04f, cap = StrokeCap.Round)
        )
        Mood.CHEER -> drawArc(
            rim, startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(w / 2f - mouthW / 2f, mouthY - mouthH / 2f),
            size = Size(mouthW, mouthH)
        )
        Mood.OOPS -> drawCircle(
            rim,
            radius = d * 0.05f,
            center = Offset(w / 2f, mouthY),
            style = Stroke(width = d * 0.04f, cap = StrokeCap.Round)
        )
        else -> drawLine(
            rim,
            Offset(w / 2f - mouthW * 0.3f, mouthY),
            Offset(w / 2f + mouthW * 0.3f, mouthY),
            strokeWidth = d * 0.04f,
            cap = StrokeCap.Round
        )
    }

    // Sleepy z's float beside the head.
    if (mood == Mood.SLEEPY) {
        drawLetterZ(Offset(w * 0.82f, h * 0.22f), d * 0.10f, rim)
        drawLetterZ(Offset(w * 0.90f, h * 0.10f), d * 0.07f, rim.copy(alpha = 0.7f))
    }
}

private fun DrawScope.drawSproutLeaf(base: Offset, angle: Float, length: Float, color: Color) {
    rotate(angle, pivot = base) {
        drawOval(
            color,
            topLeft = Offset(base.x, base.y - length),
            size = Size(length * 0.55f, length * 0.9f)
        )
    }
}

private fun DrawScope.drawLetterZ(topRight: Offset, sizePx: Float, color: Color) {
    val x = topRight.x
    val y = topRight.y
    drawLine(color, Offset(x - sizePx, y), Offset(x, y), strokeWidth = sizePx * 0.22f, cap = StrokeCap.Round)
    drawLine(color, Offset(x, y), Offset(x - sizePx, y + sizePx), strokeWidth = sizePx * 0.22f, cap = StrokeCap.Round)
    drawLine(color, Offset(x - sizePx, y + sizePx), Offset(x, y + sizePx), strokeWidth = sizePx * 0.22f, cap = StrokeCap.Round)
}

/** A softer, deeper version of any body colour — used for rims and mouths. */
private fun darker(color: Color): Color = Color(
    red = color.red * 0.72f,
    green = color.green * 0.72f,
    blue = color.blue * 0.72f,
    alpha = color.alpha
)

/** The player's own Numberling, in the meadow's colour. */
fun NumberlingBodyColor(meadowOrdinal: Int, palette: List<Color>): Color =
    palette[meadowOrdinal % palette.size]
