package com.example.sos

/**
 * MorseUtil — Shared Morse Code utilities used by all Morse-related screens.
 * morseMap already exists in MorseScreen.kt; this file provides Goertzel
 * detector and timing-based decode helpers.
 */

/** Convert String of dots/dashes to one character, null if unknown */
fun decodeMorseSymbol(symbol: String): Char? = reverseMorseMap[symbol]

/** Encode a plain text string to morse string */
fun encodeMorse(plain: String): String =
    plain.uppercase().map { morseMap[it] ?: "" }.joinToString(" ")

/**
 * Goertzel algorithm — detects energy at a specific frequency in a sample buffer.
 * targetFreq: Hz, sampleRate: samples/sec
 * Returns relative magnitude (higher = more energy at that frequency)
 */
fun goertzel(samples: ShortArray, targetFreq: Double, sampleRate: Int): Double {
    val n = samples.size
    val omega = 2.0 * Math.PI * targetFreq / sampleRate
    val cosOmega = 2.0 * Math.cos(omega)
    var q0 = 0.0; var q1 = 0.0; var q2 = 0.0
    for (s in samples) {
        q0 = cosOmega * q1 - q2 + (s.toDouble() / Short.MAX_VALUE)
        q2 = q1; q1 = q0
    }
    return Math.sqrt(q1 * q1 + q2 * q2 - q1 * q2 * cosOmega) / n
}

/** Root mean square energy of a sample buffer */
fun rmsEnergy(samples: ShortArray): Double {
    if (samples.isEmpty()) return 0.0
    val sum = samples.fold(0.0) { acc, s -> acc + (s.toDouble() / Short.MAX_VALUE).let { it * it } }
    return Math.sqrt(sum / samples.size)
}

object MorseTiming {
    const val DOT_MS = 100L         // Standard dit duration
    const val DASH_MS = 300L        // Standard dah = 3× dot
    const val SYMBOL_GAP_MS = 100L  // Gap between symbols within a letter
    const val LETTER_GAP_MS = 300L  // Gap between letters
    const val WORD_GAP_MS = 700L    // Gap between words

    // Audio detection thresholds
    const val PRESENCE_THRESHOLD = 0.05   // Energy above this = tone present
    const val DOT_DASH_BOUNDARY_MS = 200L // Tone shorter → dot, longer → dash
    const val LETTER_SILENCE_MS = 400L    // Silence longer → new letter
    const val WORD_SILENCE_MS = 900L      // Silence longer → new word
}
