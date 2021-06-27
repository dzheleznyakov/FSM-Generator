package zh.fsm.smc.lexer

class Lexer(private val collector: TokenCollector) {
    private var lineNumber = -1
    private var position = -1;

    fun lex(input: String) {
        lineNumber = 1
        input.split("\n")
            .asSequence()
            .map { line -> lexLine(line) }
            .forEach { ++lineNumber }
    }

    private fun lexLine(line: String) {
        position = 0
        while (position < line.length)
            lexToken(line)
    }

    private fun lexToken(line: String) {
        if (!findToken(line)) {
            collector.error(lineNumber, position + 1)
            ++position
        }
    }

    private fun findToken(line: String): Boolean {
        return findWhiteSpace(line) ||
                findSingleCharacterToken(line) ||
                findName(line)
    }

    private companion object Patterns {
        val whitePattern = "^\\s+".toPattern()
        val commentPattern = "^//.*$".toPattern()
        val namePattern = "^\\w+".toPattern()
        val whitePatterns = arrayOf(whitePattern, commentPattern)
    }

    private fun findWhiteSpace(line: String): Boolean = whitePatterns
        .asSequence()
        .map { pattern -> pattern.matcher(line.substring(position)) }
        .filter { matcher -> matcher.find() }
        .onEach { matcher ->
            position += matcher.end()
        }
        .filter { true }
        .any()

    private fun findSingleCharacterToken(line: String): Boolean {
        val c = line.substring(position, position + 1)
        when(c) {
            "{" -> collector::openBrace
            "}" -> collector::closedBrace
            "(" -> collector::openParen
            ")" -> collector::closedParen
            "<" -> collector::openAngle
            ">" -> collector::closedAngle
            "-" -> collector::dash
            "*" -> collector::dash
            ":" -> collector::colon
            else -> return false
        }(lineNumber, position)
        ++position
        return true
    }

    private fun findName(line: String): Boolean {
        val nameMatcher = namePattern.matcher(line.substring(position))
        if (nameMatcher.find()) {
            collector.name(nameMatcher.group(0), lineNumber, position)
            position += nameMatcher.end()
            return true
        }
        return false
    }
}
