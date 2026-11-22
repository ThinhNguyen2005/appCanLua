package com.giathinh.canlua.data.bluetooth

/**
 * Contract for decoding one serial line emitted by a weighing indicator into [ScaleReading].
 */
fun interface ScaleProtocolParser {
    fun parse(rawLine: String): ScaleReading?
}

/**
 * Parses common ASCII weighing indicator formats such as:
 * - `ST,GS,+  52.40kg` -> `52.40 kg`, `isStable = true`
 * - `US,NT,  12.5 kg` -> `12.5 kg`, `isStable = false`
 * - `52.40` or `52,40` -> `52.40 kg`, `isStable = null`
 */
class GenericAsciiScaleParser : ScaleProtocolParser {

    override fun parse(rawLine: String): ScaleReading? {
        val cleaned = sanitizeScaleLine(rawLine)
        if (cleaned.isEmpty() || cleaned.startsWith("=")) return null

        val upper = cleaned.uppercase()
        if (ERROR_REGEX.containsMatchIn(upper)) return null

        val isStable = when {
            STABLE_REGEX.containsMatchIn(upper) -> true
            UNSTABLE_REGEX.containsMatchIn(upper) -> false
            else -> null
        }

        val match = NUMBER_REGEX.find(cleaned) ?: return null
        val numberToken = match.value
            .replace(" ", "")
            .replace(',', '.')
        val weightKg = numberToken.toDoubleOrNull() ?: return null
        if (!weightKg.isFinite() || weightKg < 0.0 || weightKg > MAX_PARSE_KG) return null

        return ScaleReading(
            weightKg = weightKg,
            isStable = isStable,
            rawLine = cleaned
        )
    }

    private companion object {
        const val MAX_PARSE_KG = 500.0
        val ERROR_REGEX = Regex("\\b(OL|OVERLOAD|ERR|ERROR)\\b")
        val STABLE_REGEX = Regex("\\bST\\b")
        val UNSTABLE_REGEX = Regex("\\b(US|MO)\\b")
        val NUMBER_REGEX = Regex("[+-]?\\s*\\d+(?:[.,]\\d+)?")
    }
}

/**
 * Parses Yaohua XK3190-series reversed ASCII frames prefixed with `=` (e.g. `=04.2500` -> `52.40`).
 */
class YaohuaReversedScaleParser : ScaleProtocolParser {

    override fun parse(rawLine: String): ScaleReading? {
        val cleaned = sanitizeScaleLine(rawLine)
        if (!cleaned.startsWith("=")) return null

        val payload = cleaned.substring(1).trim()
        if (payload.isEmpty()) return null

        val reversed = payload.reversed().replace(',', '.')
        if (!REVERSED_NUMBER_REGEX.matches(reversed)) return null

        val weightKg = reversed.toDoubleOrNull() ?: return null
        if (!weightKg.isFinite() || weightKg < 0.0 || weightKg > MAX_PARSE_KG) return null

        return ScaleReading(
            weightKg = weightKg,
            isStable = null,
            rawLine = cleaned
        )
    }

    private companion object {
        const val MAX_PARSE_KG = 500.0
        val REVERSED_NUMBER_REGEX = Regex("^[+-]?\\d+(?:\\.\\d+)?$")
    }
}

/**
 * Auto-detects scale protocol by trying registered parsers in priority order.
 */
class CompositeScaleParser(
    private val parsers: List<ScaleProtocolParser> = listOf(
        YaohuaReversedScaleParser(),
        GenericAsciiScaleParser()
    )
) : ScaleProtocolParser {

    override fun parse(rawLine: String): ScaleReading? {
        for (parser in parsers) {
            parser.parse(rawLine)?.let { return it }
        }
        return null
    }
}

private fun sanitizeScaleLine(rawLine: String): String =
    rawLine.filter { ch -> ch >= ' ' }.trim()
