// ============================================================
// FILE: MorseUtil.kt
// ============================================================

package com.example.sos.Morse

fun decodeMorseSymbol(symbol: String): Char? = reverseMorseMap[symbol]

fun encodeMorse(plain: String): String =
    plain.uppercase().map { morseMap[it] ?: "" }.joinToString(" ")

/**
 * Goertzel — raw magnitude, NOT divided by N.
 * Returns values in the hundreds for real tones at 8kHz/16-bit.
 */
fun goertzel(samples: ShortArray, targetFreq: Double, sampleRate: Int): Double {
    val n = samples.size
    if (n == 0) return 0.0
    val omega    = 2.0 * Math.PI * targetFreq / sampleRate
    val cosOmega = 2.0 * Math.cos(omega)
    var q1 = 0.0; var q2 = 0.0
    for (s in samples) {
        val q0 = cosOmega * q1 - q2 + s.toDouble()
        q2 = q1; q1 = q0
    }
    return Math.sqrt(q1 * q1 + q2 * q2 - q1 * q2 * cosOmega)
}

fun rmsEnergy(samples: ShortArray): Double {
    if (samples.isEmpty()) return 0.0
    val sum = samples.fold(0.0) { acc, s ->
        acc + (s.toDouble() / Short.MAX_VALUE).let { it * it }
    }
    return Math.sqrt(sum / samples.size)
}

object MorseTiming {
    // ── Playback constants (VibrationMorseScreen & SosTemplatesScreen) ──────
    const val DOT_MS        = 100L
    const val DASH_MS       = 300L
    const val SYMBOL_GAP_MS = 100L
    const val LETTER_GAP_MS = 300L
    const val WORD_GAP_MS   = 700L

    // ── Audio detection (vars — overwritten by AdaptiveTimer at runtime) ────
    const val PRESENCE_THRESHOLD = 500.0
    const val SNR_RATIO          = 3.0

    var DOT_DASH_BOUNDARY_MS = 200L  // midpoint between dot and dash
    var LETTER_SILENCE_MS    = 400L  // silence long enough to end a letter
    var WORD_SILENCE_MS      = 900L  // silence long enough to end a word
}

/**
 * AdaptiveTimer — learns dot duration from the first REQUIRED_SAMPLES tones,
 * then derives all thresholds via standard Morse ratios:
 *
 *   dash        = 3× dot
 *   letter gap  = 3× dot  →  boundary at 4× dot (well above dash)
 *   word gap    = 7× dot
 *   dot/dash boundary = 2× dot (midpoint between 1× and 3×)
 *
 * Collapse guard: if k-means fails to separate two clusters (ratio < 1.8),
 * we assume all collected tones are dots and derive dash from 3:1 ratio.
 */
class AdaptiveTimer {

    private val durations   = mutableListOf<Long>()
    private var _calibrated = false
    private var _dotMs      = 120L

    val isCalibrated: Boolean get() = _calibrated
    val dotMs:        Long    get() = _dotMs

    companion object {
        private const val REQUIRED_SAMPLES = 10
        private const val MIN_TONE_MS      = 30L
        private const val KMEANS_ITERS     = 10
        private const val MIN_CLUSTER_RATIO = 1.8
    }

    fun recordTone(durationMs: Long) {
        if (_calibrated || durationMs < MIN_TONE_MS) return
        durations.add(durationMs)
        if (durations.size >= REQUIRED_SAMPLES) calibrate()
    }

    private fun calibrate() {
        val sorted = durations.sorted()
        var dotCenter  = sorted.first().toDouble()
        var dashCenter = sorted.last().toDouble()

        // k-means with k=2
        repeat(KMEANS_ITERS) {
            val dots   = durations.filter { absd(it - dotCenter)  < absd(it - dashCenter) }
            val dashes = durations.filter { absd(it - dashCenter) <= absd(it - dotCenter) }
            if (dots.isNotEmpty())   dotCenter  = dots.map  { it.toDouble() }.average()
            if (dashes.isNotEmpty()) dashCenter = dashes.map { it.toDouble() }.average()
        }

        // Collapse guard — if both clusters merged, treat median as dot
        val ratio = if (dotCenter > 0) dashCenter / dotCenter else 0.0
        if (ratio < MIN_CLUSTER_RATIO) {
            // All tones look the same — assume they're dots
            dotCenter  = sorted[sorted.size / 2].toDouble()
            dashCenter = dotCenter * 3.0
        }

        _dotMs = dotCenter.toLong().coerceAtLeast(MIN_TONE_MS)

        // Derive thresholds — keep boundaries clearly separated
        MorseTiming.DOT_DASH_BOUNDARY_MS = _dotMs * 2   // between 1× and 3×
        MorseTiming.LETTER_SILENCE_MS    = _dotMs * 4   // above dash (3×), below word gap
        MorseTiming.WORD_SILENCE_MS      = _dotMs * 7   // standard word gap

        _calibrated = true
    }

    fun reset() {
        durations.clear()
        _calibrated = false
        _dotMs      = 120L
        MorseTiming.DOT_DASH_BOUNDARY_MS = 200L
        MorseTiming.LETTER_SILENCE_MS    = 400L
        MorseTiming.WORD_SILENCE_MS      = 900L
    }

    private fun absd(x: Double) = if (x < 0.0) -x else x
    private fun absd(x: Long)   = absd(x.toDouble())
}