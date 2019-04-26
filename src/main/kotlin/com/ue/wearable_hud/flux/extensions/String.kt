package com.ue.wearable_hud.flux.extensions

val vt100EscapeSequences = Regex("((\u001b\\[|\u009b)[\u0030-\u003f]*[\u0020-\u002f]*[\u0040-\u007e])+")

fun String.safeSlice(indices: IntRange): String {
    val endOfString = this.length - 1
    val safeRange = IntRange(indices.start, Math.min(indices.endInclusive, endOfString))

    return this.slice(safeRange)
}

fun String.visibleCharSlice(indices: IntRange): String {
    if (indices.first >= this.length) {
        return ""
    }

    val matches = vt100EscapeSequences.findAll(this)
    if (matches.count() == 0) {
        return safeSlice(indices)
    }

    // Find where to slice, ignoring unprintable VT100 escape sequences
    var startIndex = 0
    var lenSoFar = 0
    var startSliceIndex = -1
    var endSliceIndex = -1
    var leadingVT100Code = ""
    var prevMatch: MatchResult? = null

    matches.forEach { match ->
        // Find our slice points
        val endIndex = match.range.start
        val windowSize = endIndex - startIndex

        if (startSliceIndex < 0 && lenSoFar + windowSize > indices.start) {
            startSliceIndex = (indices.start - lenSoFar) + startIndex
            leadingVT100Code = prevMatch?.value ?: ""
        }

        if (endSliceIndex < 0 && lenSoFar + windowSize > indices.endInclusive) {
            endSliceIndex = ((indices.endInclusive - lenSoFar) + startIndex)
        }

        prevMatch = match
        lenSoFar += (endIndex - startIndex)
        startIndex = match.range.endInclusive + 1
    }

    if (endSliceIndex < 0) {
        endSliceIndex = this.length - 1
    }

    val trailingVT100Sequence = if (endSliceIndex >= this.length - 1) {
        ""
    } else {
        vt100EscapeSequences.findAll(this).findLast { true }?.value ?: ""
    }

    return leadingVT100Code + this.safeSlice(startSliceIndex..endSliceIndex) + trailingVT100Sequence
}
