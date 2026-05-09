package com.example.mindarc.ui.screen

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Normalized outline (0..1) for trace-to-earn. Scale to canvas when drawing.
 * Points are ordered for drawing a continuous path.
 */
data class TraceOutline(
    val name: String,
    val points: List<Offset>
)

private const val PI = Math.PI

object TraceOutlines {

    private fun deg(degrees: Double) = degrees * PI / 180

    /** 5-point star: outer and inner vertices, points along edges for density. */
    fun star(): TraceOutline {
        val cx = 0.5f
        val cy = 0.5f
        val outerR = 0.38f
        val innerR = 0.18f
        val out = mutableListOf<Offset>()
        for (i in 0 until 5) {
            val a1 = deg(90.0 + i * 72).toFloat()
            val a2 = deg(90.0 + i * 72 + 36).toFloat()
            val p1 = Offset(cx + outerR * cosF(a1), cy - outerR * sinF(a1))
            val p2 = Offset(cx + innerR * cosF(a2), cy - innerR * sinF(a2))
            out.add(p1)
            out.add(p2)
        }
        out.add(out.first()) // close
        return TraceOutline("Star", densify(out, 8))
    }

    /** Simple flower: center circle + 5 petals as arcs. */
    fun flower(): TraceOutline {
        val cx = 0.5f
        val cy = 0.5f
        val out = mutableListOf<Offset>()
        val petalRadius = 0.2f
        val centerR = 0.08f
        for (i in 0 until 5) {
            val baseAngle = deg(i * 72.0).toFloat()
            for (k in 0..6) {
                val t = k / 6f
                val a = baseAngle + t * (PI / 2).toFloat()
                val r = centerR + petalRadius * sinF(t * PI.toFloat())
                out.add(Offset(cx + r * cosF(a), cy - r * sinF(a)))
            }
        }
        out.add(out.first())
        return TraceOutline("Flower", densify(out, 6))
    }

    /** Heart: two circles + triangle approximation. */
    fun heart(): TraceOutline {
        val out = mutableListOf<Offset>()
        val cx = 0.5f
        val cy = 0.48f
        val scale = 0.22f
        for (t in 0..60) {
            val u = t / 60f * 2 * PI.toFloat()
            val x = 16 * sinF(u) * sinF(u) * sinF(u)
            val y = 13 * cosF(u) - 5 * cosF(2 * u) - 2 * cosF(3 * u) - cosF(4 * u)
            out.add(Offset(cx + (x * scale).toFloat() / 16f, cy - (y * scale).toFloat() / 16f))
        }
        out.add(out.first())
        return TraceOutline("Heart", densify(out, 4))
    }

    /** Simple motorcycle side view: two wheels + body line. */
    fun motorcycle(): TraceOutline {
        val out = mutableListOf<Offset>()
        val margin = 0.15f
        val w = 1f - 2 * margin
        val h = 1f - 2 * margin
        val left = margin
        val top = margin
        // Rear wheel center (0.25, 0.75), front (0.75, 0.75), radius ~0.12
        val r = 0.12f
        for (i in 0..24) {
            val a = i / 24f * 2 * PI.toFloat()
            out.add(Offset(left + 0.25f * w + r * cosF(a), top + 0.75f * h - r * sinF(a)))
        }
        out.add(out.first())
        for (i in 0..24) {
            val a = i / 24f * 2 * PI.toFloat()
            out.add(Offset(left + 0.75f * w + r * cosF(a), top + 0.75f * h - r * sinF(a)))
        }
        // Body: rear wheel top -> handlebar -> front wheel top
        out.add(Offset(left + 0.25f * w, top + 0.75f * h - r))
        out.add(Offset(left + 0.35f * w, top + 0.45f * h))
        out.add(Offset(left + 0.65f * w, top + 0.4f * h))
        out.add(Offset(left + 0.75f * w, top + 0.75f * h - r))
        return TraceOutline("Motorcycle", densify(out, 5))
    }

    /** Simple house shape. */
    fun house(): TraceOutline {
        val out = mutableListOf<Offset>()
        val cx = 0.5f
        out.add(Offset(cx, 0.22f))   // roof peak
        out.add(Offset(0.78f, 0.55f)) // roof right
        out.add(Offset(0.78f, 0.88f)) // wall right bottom
        out.add(Offset(0.22f, 0.88f)) // wall left bottom
        out.add(Offset(0.22f, 0.55f)) // roof left
        out.add(Offset(cx, 0.22f))
        return TraceOutline("House", densify(out, 10))
    }

    /** Interpolate between consecutive points to get more samples for distance calc. */
    private fun densify(points: List<Offset>, stepsPerSegment: Int): List<Offset> {
        if (points.size < 2) return points
        val result = mutableListOf<Offset>()
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            for (s in 0 until stepsPerSegment) {
                val t = s / stepsPerSegment.toFloat()
                result.add(Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y)))
            }
        }
        return result
    }

    private fun cosF(x: Float) = kotlin.math.cos(x.toDouble()).toFloat()
    private fun sinF(x: Float) = kotlin.math.sin(x.toDouble()).toFloat()

    fun all(): List<TraceOutline> = listOf(
        star(),
        flower(),
        heart(),
        motorcycle(),
        house()
    )

    fun random() = all().random()
}

/** Distance from point to nearest point on the outline (outline in same coordinate system as point). */
fun distanceToOutline(point: Offset, outlinePoints: List<Offset>): Float {
    if (outlinePoints.isEmpty()) return Float.MAX_VALUE
    var min = Float.MAX_VALUE
    for (p in outlinePoints) {
        val dx = point.x - p.x
        val dy = point.y - p.y
        val d = sqrt(dx * dx + dy * dy)
        if (d < min) min = d
    }
    return min
}
