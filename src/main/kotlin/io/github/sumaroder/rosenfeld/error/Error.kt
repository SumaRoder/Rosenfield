package io.github.sumaroder.rosenfeld.error

import io.github.sumaroder.rosenfeld.tokenize.TokenInfo

class RosenfeldError(message: String) : RuntimeException(message) {
    constructor(info: TokenInfo, reason: String) : this("${info} -> $reason")
}
object ErrorHandler {
    fun report(info: TokenInfo, reason: String, sourceLine: String? = null): Nothing {
        val sb = StringBuilder()
        sb.appendLine("$reason at $info")
        if (sourceLine != null) {
            sb.appendLine("  > $sourceLine")
            val padding = " ".repeat(info.column + 3)
            sb.appendLine("$padding^--- Here")
        }
        throw RosenfeldError(sb.toString())
    }
}