package com.ue.wearable_hud.flux.extensions

fun String.safeSlice(indices: IntRange):String  {
    if (indices.first >= this.length) { return "" }

    val safeRange = IntRange(indices.first, Math.min(this.length - 1, indices.last))
    return this.slice(safeRange)
}