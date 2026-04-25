package com.example.nodegraph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nodegraph.domain.repository.GraphStepRepository
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

private const val MIN_OUTER_COUNT = 2
private const val MAX_OUTER_COUNT = 5
private const val INITIAL_OUTER_COUNT = 3
private const val TRANSITION_MILLIS = 8000
private const val TWO_PI = (2.0 * kotlin.math.PI).toFloat()
private val FAN_HALF_ANGLE = (kotlin.math.PI / 4.0).toFloat()
private const val EDGE_CURVATURE = 0.28f
private const val RING_RADIUS = 2.0f

private fun clampOuterCount(count: Int): Int =
    count.coerceIn(MIN_OUTER_COUNT, MAX_OUTER_COUNT)

private fun rightFanAngle(i: Int, count: Int): Float {
    if (count <= 1) return 0f
    return -FAN_HALF_ANGLE + (2f * FAN_HALF_ANGLE) * i.toFloat() / (count - 1).toFloat()
}

private data class WorldNode(val id: Int, val label: String, val worldPos: Offset)
private data class WorldEdge(val fromId: Int, val toId: Int)
private data class World(val nodes: List<WorldNode>, val edges: List<WorldEdge>)

private data class GraphState(
    val world: World,
    val centerId: Int,
    val outerIds: List<Int>
)

private data class Transition(
    val from: GraphState,
    val to: GraphState,
    val spawnedIds: Set<Int>
)

private class NodeFactory {
    private var nextId = 0
    fun create(worldPos: Offset): WorldNode {
        val id = nextId++
        return WorldNode(id = id, label = "N${id + 1}", worldPos = worldPos)
    }
}

private fun buildInitialState(factory: NodeFactory, outerCount: Int): GraphState {
    val center = factory.create(Offset.Zero)
    val count = clampOuterCount(outerCount)
    val outer = List(count) { i ->
        val a = rightFanAngle(i, count)
        factory.create(Offset(cos(a) * RING_RADIUS, sin(a) * RING_RADIUS))
    }
    val edges = outer.map { WorldEdge(center.id, it.id) }
    return GraphState(
        world = World(nodes = listOf(center) + outer, edges = edges),
        centerId = center.id,
        outerIds = outer.map { it.id }
    )
}

private fun buildTransition(
    current: GraphState,
    target: WorldNode,
    factory: NodeFactory,
    outerCount: Int
): Transition {
    val count = clampOuterCount(outerCount)
    val newOuter = List(count) { i ->
        val a = rightFanAngle(i, count)
        factory.create(
            Offset(
                target.worldPos.x + cos(a) * RING_RADIUS,
                target.worldPos.y + sin(a) * RING_RADIUS
            )
        )
    }
    val newEdges = newOuter.map { WorldEdge(target.id, it.id) }
    val newWorld = World(
        nodes = current.world.nodes + newOuter,
        edges = current.world.edges + newEdges
    )
    val newState = GraphState(
        world = newWorld,
        centerId = target.id,
        outerIds = newOuter.map { it.id }
    )
    return Transition(
        from = current,
        to = newState,
        spawnedIds = newOuter.map { it.id }.toSet()
    )
}

@Composable
fun App(repository: GraphStepRepository) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val factory = remember { NodeFactory() }
            var state by remember { mutableStateOf(buildInitialState(factory, INITIAL_OUTER_COUNT)) }
            var pickerSeed by remember { mutableStateOf(Random.nextInt()) }
            var transition by remember { mutableStateOf<Transition?>(null) }
            var loading by remember { mutableStateOf(false) }
            val progress = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(transition) {
                val t = transition ?: return@LaunchedEffect
                progress.snapTo(0f)
                progress.animateTo(1f, tween(durationMillis = TRANSITION_MILLIS))
                state = t.to
                pickerSeed = Random.nextInt()
                transition = null
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val centerNode = state.world.nodes.first { it.id == state.centerId }
                Text(
                    text = "Center: ${centerNode.label}  |  ${state.world.nodes.size} nodes in world",
                    style = MaterialTheme.typography.titleMedium
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    NodeGraph(
                        state = state,
                        transition = transition,
                        progress = progress.value,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (transition == null) {
                val outerNodes = state.outerIds.mapNotNull { id ->
                    state.world.nodes.firstOrNull { it.id == id }
                }
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Where to go next?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RandomImage(
                                seed = pickerSeed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(4.dp))
                            outerNodes.forEach { node ->
                                Button(
                                    enabled = !loading,
                                    onClick = {
                                        loading = true
                                        scope.launch {
                                            val nextStep = runCatching { repository.getNextStep() }.getOrNull()
                                            val count = nextStep?.nodeCount
                                                ?: Random.nextInt(MIN_OUTER_COUNT, MAX_OUTER_COUNT + 1)
                                            transition = buildTransition(state, node, factory, count)
                                            loading = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.height(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Move to ${node.label}")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

@Composable
private fun RandomImage(seed: Int, modifier: Modifier = Modifier) {
    val rng = remember(seed) { Random(seed) }
    val colorA = remember(seed) {
        Color(
            red = 0.3f + rng.nextFloat() * 0.5f,
            green = 0.3f + rng.nextFloat() * 0.5f,
            blue = 0.5f + rng.nextFloat() * 0.4f,
            alpha = 1f
        )
    }
    val colorB = remember(seed) {
        Color(
            red = 0.2f + rng.nextFloat() * 0.5f,
            green = 0.4f + rng.nextFloat() * 0.4f,
            blue = 0.3f + rng.nextFloat() * 0.5f,
            alpha = 1f
        )
    }
    val blobs = remember(seed) {
        List(8) {
            BlobSpec(
                cx = rng.nextFloat(),
                cy = rng.nextFloat(),
                r = 0.08f + rng.nextFloat() * 0.18f,
                alpha = 0.18f + rng.nextFloat() * 0.25f
            )
        }
    }
    Canvas(modifier = modifier) {
        drawRect(brush = Brush.linearGradient(listOf(colorA, colorB)), size = size)
        val minDim = min(size.width, size.height)
        blobs.forEach { b ->
            drawCircle(
                color = Color.White.copy(alpha = b.alpha),
                radius = b.r * minDim,
                center = Offset(b.cx * size.width, b.cy * size.height)
            )
        }
    }
}

private data class BlobSpec(val cx: Float, val cy: Float, val r: Float, val alpha: Float)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

private fun smoothstep(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val c = t.coerceIn(0f, 1f)
    return Color(
        red = lerp(a.red, b.red, c),
        green = lerp(a.green, b.green, c),
        blue = lerp(a.blue, b.blue, c),
        alpha = lerp(a.alpha, b.alpha, c)
    )
}

private fun bezierControl(start: Offset, end: Offset): Offset {
    val mx = (start.x + end.x) / 2f
    val my = (start.y + end.y) / 2f
    val dx = end.x - start.x
    val dy = end.y - start.y
    return Offset(mx + (-dy) * EDGE_CURVATURE, my + dx * EDGE_CURVATURE)
}

private fun bezierPoint(t: Float, p0: Offset, ctrl: Offset, p1: Offset): Offset {
    val omt = 1f - t
    return Offset(
        omt * omt * p0.x + 2f * omt * t * ctrl.x + t * t * p1.x,
        omt * omt * p0.y + 2f * omt * t * ctrl.y + t * t * p1.y
    )
}

@Composable
private fun NodeGraph(
    state: GraphState,
    transition: Transition?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val centerColor = MaterialTheme.colorScheme.primary
    val outerColor = MaterialTheme.colorScheme.tertiary
    val edgeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onPrimary
    val outerLabelColor = MaterialTheme.colorScheme.onTertiary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 18.sp)
    val outerLabelStyle = TextStyle(color = outerLabelColor, fontSize = 15.sp)

    Box(
        modifier = modifier.drawBehind {
            val w = size.width
            val h = size.height
            val screenCenter = Offset(w / 2f, h / 2f)
            val pixelsPerUnit = min(w, h) * 0.25f
            val outerRadius = min(w, h) * 0.085f
            val centerRadius = min(w, h) * 0.125f

            val effectiveWorld = transition?.to?.world ?: state.world
            val fromCenterId = transition?.from?.centerId ?: state.centerId
            val toCenterId = transition?.to?.centerId ?: state.centerId

            val ph1 = if (transition != null) smoothstep((progress * 2f).coerceAtMost(1f)) else 1f
            val ph2 = if (transition != null) smoothstep(((progress - 0.5f) * 2f).coerceIn(0f, 1f)) else 1f

            val fromCenterPos = effectiveWorld.nodes.first { it.id == fromCenterId }.worldPos
            val toCenterPos = effectiveWorld.nodes.first { it.id == toCenterId }.worldPos
            val cameraPos = lerp(fromCenterPos, toCenterPos, ph1)

            fun worldToScreen(wp: Offset): Offset = Offset(
                screenCenter.x + (wp.x - cameraPos.x) * pixelsPerUnit,
                screenCenter.y + (wp.y - cameraPos.y) * pixelsPerUnit
            )

            fun effectiveWorldPos(node: WorldNode): Offset {
                return if (transition != null && node.id in transition.spawnedIds) {
                    val ctrl = bezierControl(toCenterPos, node.worldPos)
                    bezierPoint(ph2, toCenterPos, ctrl, node.worldPos)
                } else {
                    node.worldPos
                }
            }

            fun nodeAlpha(node: WorldNode): Float =
                if (transition != null && node.id in transition.spawnedIds) ph2 else 1f

            // Edges: full Bezier for preexisting edges; partial Bezier (grows with its new
            // node along the same curve) for edges connecting spawned nodes. Because the new
            // node travels the curve, it can't cross unrelated nodes the straight chord might.
            effectiveWorld.edges.forEach { edge ->
                val fromNode = effectiveWorld.nodes.first { it.id == edge.fromId }
                val toNode = effectiveWorld.nodes.first { it.id == edge.toId }
                val toIsNew = transition != null && edge.toId in transition.spawnedIds
                val a = if (toIsNew) ph2 else 1f
                if (a <= 0f) return@forEach

                val startWorld = fromNode.worldPos
                val targetWorld = toNode.worldPos
                val fullControl = bezierControl(startWorld, targetWorld)

                val (ctrlWorld, endWorld) = if (toIsNew) {
                    val partialCtrl = lerp(startWorld, fullControl, ph2)
                    val partialEnd = bezierPoint(ph2, startWorld, fullControl, targetWorld)
                    partialCtrl to partialEnd
                } else {
                    fullControl to targetWorld
                }

                val startScreen = worldToScreen(startWorld)
                val ctrlScreen = worldToScreen(ctrlWorld)
                val endScreen = worldToScreen(endWorld)
                val path = Path().apply {
                    moveTo(startScreen.x, startScreen.y)
                    quadraticBezierTo(ctrlScreen.x, ctrlScreen.y, endScreen.x, endScreen.y)
                }
                drawPath(
                    path = path,
                    color = edgeColor.copy(alpha = edgeColor.alpha * a),
                    style = Stroke(width = 4f)
                )
            }

            // Nodes
            effectiveWorld.nodes.forEach { node ->
                val centerness = when (node.id) {
                    toCenterId -> ph1
                    fromCenterId -> 1f - ph1
                    else -> 0f
                }
                val radius = lerp(outerRadius, centerRadius, centerness)
                val fill = lerpColor(outerColor, centerColor, centerness)
                val lblColor = lerpColor(outerLabelColor, labelColor, centerness)
                val style = if (centerness >= 0.5f) labelStyle else outerLabelStyle
                drawNode(
                    center = worldToScreen(effectiveWorldPos(node)),
                    radius = radius,
                    fill = fill,
                    alpha = nodeAlpha(node),
                    label = node.label,
                    style = style,
                    labelColor = lblColor,
                    textMeasurer = textMeasurer
                )
            }
        }
    )
}

private fun DrawScope.drawNode(
    center: Offset,
    radius: Float,
    fill: Color,
    alpha: Float,
    label: String,
    style: TextStyle,
    labelColor: Color,
    textMeasurer: TextMeasurer
) {
    if (alpha <= 0f) return
    drawCircle(color = fill.copy(alpha = fill.alpha * alpha), radius = radius, center = center)
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f * alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 3f)
    )
    val styled = style.copy(color = labelColor.copy(alpha = labelColor.alpha * alpha))
    val measured = textMeasurer.measure(label, styled)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            center.x - measured.size.width / 2f,
            center.y - measured.size.height / 2f
        )
    )
}
