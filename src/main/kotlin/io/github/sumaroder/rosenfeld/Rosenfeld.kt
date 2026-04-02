package io.github.sumaroder.rosenfeld

import io.github.sumaroder.rosenfeld.interpreter.Interpreter
import io.github.sumaroder.rosenfeld.parse.Parser
import io.github.sumaroder.rosenfeld.tokenize.Tokenizer
import java.io.File

fun main(args: Array<String>) {
    val filePath = args.firstOrNull() ?: "src/main/resources/a.nk"

    val content = if (File(filePath).exists()) {
        File(filePath).readText()
    } else {
        val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("a.nk")
            ?: throw IllegalArgumentException("File not found: $filePath")
        inputStream.bufferedReader().use { it.readText() }
    }

    val tokens = Tokenizer.tokenize(content)
    val parser = Parser(tokens)
    val program = parser.parse()

    val interpreter = Interpreter()
    try {
        interpreter.interpret(program)
        interpreter.callMainIfExists()
    } catch (e: Exception) {
        println("Runtime error: ${e.message}")
        e.printStackTrace()
    }
}
