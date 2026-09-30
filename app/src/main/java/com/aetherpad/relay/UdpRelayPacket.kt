package com.aetherpad.relay

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 20-Byte Compact Binary UDP Datagram Frame
 * Magic (2B) | Seq (1B) | Buttons (2B) | Triggers (2B) | LeftStick (4B) | RightStick (4B) | Battery (1B) | Timestamp (4B)
 */
object UdpRelayPacket {

    private const val MAGIC_HEADER: Short = 0x50AE.toShort() // 'A' 'P' little-endian

    fun serializeInto(state: GamepadInputState, target: ByteArray, seq: Int): Int {
        val buffer = ByteBuffer.wrap(target).order(ByteOrder.LITTLE_ENDIAN)
        buffer.clear()

        // 1. Magic
        buffer.putShort(MAGIC_HEADER)

        // 2. Sequence (0..255)
        buffer.put((seq and 0xFF).toByte())

        // 3. Buttons Bitmask (16 bits)
        var bitmask = 0
        if (state.buttonA) bitmask = bitmask or (1 shl 0)
        if (state.buttonB) bitmask = bitmask or (1 shl 1)
        if (state.buttonX) bitmask = bitmask or (1 shl 2)
        if (state.buttonY) bitmask = bitmask or (1 shl 3)
        if (state.buttonL1) bitmask = bitmask or (1 shl 4)
        if (state.buttonR1) bitmask = bitmask or (1 shl 5)
        if (state.buttonBack) bitmask = bitmask or (1 shl 6)
        if (state.buttonStart) bitmask = bitmask or (1 shl 7)
        if (state.buttonGuide) bitmask = bitmask or (1 shl 8)
        if (state.buttonL3) bitmask = bitmask or (1 shl 9)
        if (state.buttonR3) bitmask = bitmask or (1 shl 10)
        if (state.dpadUp) bitmask = bitmask or (1 shl 11)
        if (state.dpadDown) bitmask = bitmask or (1 shl 12)
        if (state.dpadLeft) bitmask = bitmask or (1 shl 13)
        if (state.dpadRight) bitmask = bitmask or (1 shl 14)
        buffer.putShort(bitmask.toShort())

        // 4. Triggers (0..255)
        val ltByte = (state.lTrigger.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val rtByte = (state.rTrigger.coerceIn(0f, 1f) * 255f).toInt().toByte()
        buffer.put(ltByte)
        buffer.put(rtByte)

        // 5. Left Stick X & Y (-32768..32767)
        val lx = (state.leftStickX.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        val ly = (state.leftStickY.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        buffer.putShort(lx)
        buffer.putShort(ly)

        // 6. Right Stick X & Y (-32768..32767)
        val rx = (state.rightStickX.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        val ry = (state.rightStickY.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        buffer.putShort(rx)
        buffer.putShort(ry)

        // 7. Battery %
        buffer.put(90.toByte())

        // 8. Timestamp lower 32 bits
        buffer.putInt((System.currentTimeMillis() and 0xFFFFFFFFL).toInt())

        return 20
    }
}

data class GamepadInputState(
    val buttonA: Boolean = false,
    val buttonB: Boolean = false,
    val buttonX: Boolean = false,
    val buttonY: Boolean = false,
    val buttonL1: Boolean = false,
    val buttonR1: Boolean = false,
    val buttonBack: Boolean = false,
    val buttonStart: Boolean = false,
    val buttonGuide: Boolean = false,
    val buttonL3: Boolean = false,
    val buttonR3: Boolean = false,
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,
    val lTrigger: Float = 0f,
    val rTrigger: Float = 0f,
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f
)
