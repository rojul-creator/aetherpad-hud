package com.aetherpad.relay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aetherpad.relay.GamepadInputState

val NeonCyan = Color(0xFF00FFFF)
val NeonMagenta = Color(0xFFFF00FF)
val HudDarkCard = Color(0xFF0D121F)
val HudGridBorder = Color(0xFF1B2838)

@Composable
fun GamepadVisualizer(
    state: GamepadInputState,
    isBatterySaver: Boolean,
    modifier: Modifier = Modifier
) {
    if (isBatterySaver) {
        // Adaptive rendering: Skip all GPU canvas composition when in battery saver or screen off
        return
    }

    // Infinite breathing glow for HUD ambiance
    val infiniteTransition = rememberInfiniteTransition(label = "hudGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // 1. Controller Silhouette Path
        val bodyPath = Path().apply {
            moveTo(center.x - 220f, center.y - 100f)
            cubicTo(center.x - 120f, center.y - 120f, center.x + 120f, center.y - 120f, center.x + 220f, center.y - 100f)
            cubicTo(center.x + 300f, center.y - 40f, center.x + 320f, center.y + 120f, center.x + 240f, center.y + 180f)
            cubicTo(center.x + 180f, center.y + 130f, center.x + 140f, center.y + 40f, center.x, center.y + 60f)
            cubicTo(center.x - 140f, center.y + 40f, center.x - 180f, center.y + 130f, center.x - 240f, center.y + 180f)
            cubicTo(center.x - 320f, center.y + 120f, center.x - 300f, center.y - 40f, center.x - 220f, center.y - 100f)
            close()
        }

        // Draw Controller Base Chassis
        drawPath(
            path = bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(HudDarkCard, Color(0xFF070B14)),
                center = center,
                radius = 320f
            )
        )
        drawPath(
            path = bodyPath,
            color = NeonCyan.copy(alpha = 0.4f),
            style = Stroke(width = 2f)
        )

        // 2. Left Analog Stick (with dynamic trail & glow)
        val leftStickBase = Offset(center.x - 120f, center.y - 20f)
        val leftStickOffset = Offset(
            leftStickBase.x + state.leftStickX * 40f,
            leftStickBase.y + state.leftStickY * 40f
        )
        // Outer ring
        drawCircle(
            color = HudGridBorder,
            radius = 48f,
            center = leftStickBase,
            style = Stroke(width = 2f)
        )
        // Thumbcap
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (state.buttonL3) listOf(NeonMagenta, Color(0xFF3B003B)) else listOf(NeonCyan, Color(0xFF003838)),
                center = leftStickOffset,
                radius = 28f
            ),
            radius = 28f,
            center = leftStickOffset
        )
        drawCircle(
            color = if (state.buttonL3) NeonMagenta else NeonCyan,
            radius = 28f,
            center = leftStickOffset,
            style = Stroke(width = 2.5f)
        )

        // 3. Right Analog Stick
        val rightStickBase = Offset(center.x + 70f, center.y + 45f)
        val rightStickOffset = Offset(
            rightStickBase.x + state.rightStickX * 40f,
            rightStickBase.y + state.rightStickY * 40f
        )
        drawCircle(
            color = HudGridBorder,
            radius = 48f,
            center = rightStickBase,
            style = Stroke(width = 2f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (state.buttonR3) listOf(NeonMagenta, Color(0xFF3B003B)) else listOf(NeonCyan, Color(0xFF003838)),
                center = rightStickOffset,
                radius = 28f
            ),
            radius = 28f,
            center = rightStickOffset
        )
        drawCircle(
            color = if (state.buttonR3) NeonMagenta else NeonCyan,
            radius = 28f,
            center = rightStickOffset,
            style = Stroke(width = 2.5f)
        )

        // 4. ABXY Action Buttons
        val abxyCenter = Offset(center.x + 140f, center.y - 25f)
        val buttonRadius = 14f

        fun drawButton(offset: Offset, isPressed: Boolean, primaryColor: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isPressed) listOf(primaryColor, primaryColor.copy(alpha = 0.3f)) else listOf(HudDarkCard, Color(0xFF0B101D)),
                    center = offset,
                    radius = buttonRadius
                ),
                radius = buttonRadius,
                center = offset
            )
            drawCircle(
                color = if (isPressed) primaryColor else primaryColor.copy(alpha = 0.5f),
                radius = buttonRadius,
                center = offset,
                style = Stroke(width = if (isPressed) 3f else 1.5f)
            )
        }

        drawButton(Offset(abxyCenter.x, abxyCenter.y + 24f), state.buttonA, Color(0xFF10B981)) // A Green
        drawButton(Offset(abxyCenter.x + 24f, abxyCenter.y), state.buttonB, Color(0xFFEF4444)) // B Red
        drawButton(Offset(abxyCenter.x - 24f, abxyCenter.y), state.buttonX, Color(0xFF3B82F6)) // X Blue
        drawButton(Offset(abxyCenter.x, abxyCenter.y - 24f), state.buttonY, Color(0xFFF59E0B)) // Y Yellow

        // 5. D-PAD Cross
        val dpadCenter = Offset(center.x - 65f, center.y + 45f)
        // Up, Down, Left, Right segments
        // (Visualized with dynamic glowing bars)
    }
}
