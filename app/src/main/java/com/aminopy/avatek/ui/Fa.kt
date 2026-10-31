package com.aminopy.avatek.ui

import com.aminopy.avatek.formatDuration

/** Persian-first number rendering. */
fun String.fa(): String = buildString {
    for (c in this@fa) append(if (c in '0'..'9') '۰' + (c - '0') else c)
}

fun Int.fa(): String = toString().fa()
fun Long.fa(): String = toString().fa()
fun faDuration(ms: Long): String = formatDuration(ms).fa()

/** "۲ ساعت و ۱۰ دقیقه" */
fun faTotal(ms: Long): String {
    val minutes = (ms / 60_000L).toInt()
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h.fa()} ساعت و ${m.fa()} دقیقه"
        h > 0 -> "${h.fa()} ساعت"
        else -> "${m.fa()} دقیقه"
    }
}
