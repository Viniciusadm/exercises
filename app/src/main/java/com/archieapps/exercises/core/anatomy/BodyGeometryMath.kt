package com.archieapps.exercises.core.anatomy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.PathNode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Tudo aqui opera sobre PathNode e floats, sem tocar android.graphics: e por isso que
 * os bounds sao derivados dos nos em vez de Path.getBounds(), que seria uma linha mas
 * nao rodaria em teste de JVM.
 */
data class BodyBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun area(): Float = width * height

    fun union(other: BodyBounds) = BodyBounds(
        left = min(left, other.left),
        top = min(top, other.top),
        right = max(right, other.right),
        bottom = max(bottom, other.bottom),
    )

    fun resizedAround(width: Float, height: Float) = BodyBounds(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f,
    )

    operator fun contains(point: Offset): Boolean =
        point.x in left..right && point.y in top..bottom
}

data class FitTransform(val scale: Float, val dx: Float, val dy: Float) {
    fun toViewBox(offset: Offset): Offset =
        if (scale <= 0f) Offset.Zero else Offset((offset.x - dx) / scale, (offset.y - dy) / scale)
}

/**
 * Curvas entram pelo casco dos pontos de controle, nao pelo extremo real. E um
 * superconjunto conservador: a caixa fica alguns centesimos maior que a tinta, o que
 * na pratica vira uma margem em volta da figura, e economiza a matematica de extremo
 * de Bezier.
 */
fun boundsOf(nodes: List<PathNode>): BodyBounds? {
    if (nodes.isEmpty()) return null

    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    var seen = false

    var currentX = 0f
    var currentY = 0f
    var startX = 0f
    var startY = 0f

    fun include(x: Float, y: Float) {
        left = min(left, x)
        top = min(top, y)
        right = max(right, x)
        bottom = max(bottom, y)
        seen = true
    }

    fun moveTo(x: Float, y: Float) {
        currentX = x
        currentY = y
        startX = x
        startY = y
        include(x, y)
    }

    fun lineTo(x: Float, y: Float) {
        currentX = x
        currentY = y
        include(x, y)
    }

    nodes.forEach { node ->
        when (node) {
            is PathNode.MoveTo -> moveTo(node.x, node.y)
            is PathNode.RelativeMoveTo -> moveTo(currentX + node.dx, currentY + node.dy)

            is PathNode.LineTo -> lineTo(node.x, node.y)
            is PathNode.RelativeLineTo -> lineTo(currentX + node.dx, currentY + node.dy)

            is PathNode.HorizontalTo -> lineTo(node.x, currentY)
            is PathNode.RelativeHorizontalTo -> lineTo(currentX + node.dx, currentY)

            is PathNode.VerticalTo -> lineTo(currentX, node.y)
            is PathNode.RelativeVerticalTo -> lineTo(currentX, currentY + node.dy)

            is PathNode.CurveTo -> {
                include(node.x1, node.y1)
                include(node.x2, node.y2)
                lineTo(node.x3, node.y3)
            }

            is PathNode.RelativeCurveTo -> {
                include(currentX + node.dx1, currentY + node.dy1)
                include(currentX + node.dx2, currentY + node.dy2)
                lineTo(currentX + node.dx3, currentY + node.dy3)
            }

            is PathNode.ReflectiveCurveTo -> {
                include(node.x1, node.y1)
                lineTo(node.x2, node.y2)
            }

            is PathNode.RelativeReflectiveCurveTo -> {
                include(currentX + node.dx1, currentY + node.dy1)
                lineTo(currentX + node.dx2, currentY + node.dy2)
            }

            is PathNode.QuadTo -> {
                include(node.x1, node.y1)
                lineTo(node.x2, node.y2)
            }

            is PathNode.RelativeQuadTo -> {
                include(currentX + node.dx1, currentY + node.dy1)
                lineTo(currentX + node.dx2, currentY + node.dy2)
            }

            is PathNode.ReflectiveQuadTo -> lineTo(node.x, node.y)
            is PathNode.RelativeReflectiveQuadTo -> lineTo(currentX + node.dx, currentY + node.dy)

            is PathNode.ArcTo -> {
                include(node.arcStartX - node.horizontalEllipseRadius, node.arcStartY - node.verticalEllipseRadius)
                include(node.arcStartX + node.horizontalEllipseRadius, node.arcStartY + node.verticalEllipseRadius)
                lineTo(node.arcStartX, node.arcStartY)
            }

            is PathNode.RelativeArcTo -> {
                val endX = currentX + node.arcStartDx
                val endY = currentY + node.arcStartDy
                include(endX - node.horizontalEllipseRadius, endY - node.verticalEllipseRadius)
                include(endX + node.horizontalEllipseRadius, endY + node.verticalEllipseRadius)
                lineTo(endX, endY)
            }

            PathNode.Close -> {
                currentX = startX
                currentY = startY
            }
        }
    }

    return if (seen) BodyBounds(left, top, right, bottom) else null
}

/**
 * Medido nos arquivos upstream: frente e costas convivem no mesmo espaco de coordenadas,
 * lado a lado (frente em x aprox. 3..28, costas em 40..65). A uniao crua daria uma caixa
 * com o dobro da largura da figura e metade do canvas vazia em cada vista. Entao: caixa
 * propria de cada vista para posicao, tamanho comum para escala, de forma que a silhueta
 * nao pule nem redimensione ao alternar.
 */
fun normalizeViewBoxes(front: BodyBounds, back: BodyBounds): Pair<BodyBounds, BodyBounds> {
    val width = max(front.width, back.width)
    val height = max(front.height, back.height)

    return front.resizedAround(width, height) to back.resizedAround(width, height)
}

fun fitTransform(canvasWidth: Float, canvasHeight: Float, bounds: BodyBounds): FitTransform {
    if (bounds.width <= 0f || bounds.height <= 0f || canvasWidth <= 0f || canvasHeight <= 0f) {
        return FitTransform(0f, 0f, 0f)
    }

    val scale = min(canvasWidth / bounds.width, canvasHeight / bounds.height)

    return FitTransform(
        scale = scale,
        dx = (canvasWidth - bounds.width * scale) / 2f - bounds.left * scale,
        dy = (canvasHeight - bounds.height * scale) / 2f - bounds.top * scale,
    )
}

/**
 * Compose nao expoe distancia ponto-a-path, entao a tolerancia de toque e amostrada em
 * anel: dois aneis de oito pontos cobrem uma polpa de dedo sem custo perceptivel.
 */
fun toleranceRing(center: Offset, radius: Float): List<Offset> {
    if (radius <= 0f) return emptyList()

    val diagonal = radius * 0.7071f
    val half = radius / 2f
    val halfDiagonal = half * 0.7071f

    return listOf(
        Offset(center.x + radius, center.y), Offset(center.x - radius, center.y),
        Offset(center.x, center.y + radius), Offset(center.x, center.y - radius),
        Offset(center.x + diagonal, center.y + diagonal), Offset(center.x - diagonal, center.y + diagonal),
        Offset(center.x + diagonal, center.y - diagonal), Offset(center.x - diagonal, center.y - diagonal),
        Offset(center.x + half, center.y), Offset(center.x - half, center.y),
        Offset(center.x, center.y + half), Offset(center.x, center.y - half),
        Offset(center.x + halfDiagonal, center.y + halfDiagonal),
        Offset(center.x - halfDiagonal, center.y + halfDiagonal),
        Offset(center.x + halfDiagonal, center.y - halfDiagonal),
        Offset(center.x - halfDiagonal, center.y - halfDiagonal),
    )
}

fun distanceTo(center: Offset, point: Offset): Float =
    abs(center.x - point.x) + abs(center.y - point.y)
