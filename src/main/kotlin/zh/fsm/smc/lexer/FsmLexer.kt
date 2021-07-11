package zh.fsm.smc.lexer

class FsmLexer(private val collector: TokenCollector) : Lexer {
    private var line = -1
    private var linePos = -1
    private var totalPos = -1
    private var state = State.NEW_TOKEN
    private var nameStart = -1
    private lateinit var input: String

    override fun lex(input: String) {
        init(input)
        for (i in input.indices)
            handleEvent(input[i]).also { ++totalPos }
        handleEvent(0.toChar())
    }

    private fun init(input: String) {
        this.input = input
        line = 1
        linePos = 0
        totalPos = 0
    }

    private fun handleEvent(event: Char) {
        (transitions[state] ?: return)
            .stream()
            .filter { tr -> tr.eventTest(event) }
            .findAny()
            .orElse(errorTransition)
            .run(event)
    }

    private fun Transition.run(event: Char) {
        state = newState
        action(event)
    }

    enum class State {
        NEW_TOKEN, NAME, BACKSLASH, COMMENT, EOF
    }

    inner class Transition (
        val eventTest: (Char) -> Boolean,
        val newState: State,
        val action: (Char) -> Unit)

    private val errorTransition = Transition({ true }, State.NEW_TOKEN) { ++linePos; collector.error(line, linePos) }

    private val transitions = mapOf(
        State.NEW_TOKEN to listOf(
            Transition({ it == '\n' }, State.NEW_TOKEN) { ++line; linePos = 0 },
            Transition({ it == '{' }, State.NEW_TOKEN) { ++linePos; collector.openBrace(line, linePos) },
            Transition({ it == '}' }, State.NEW_TOKEN) { ++linePos; collector.closedBrace(line, linePos) },
            Transition({ it == '(' }, State.NEW_TOKEN) { ++linePos; collector.openParen(line, linePos) },
            Transition({ it == ')' }, State.NEW_TOKEN) { ++linePos; collector.closedParen(line, linePos) },
            Transition({ it == '<' }, State.NEW_TOKEN) { ++linePos; collector.openAngle(line, linePos) },
            Transition({ it == '>' }, State.NEW_TOKEN) { ++linePos; collector.closedAngle(line, linePos) },
            Transition({ it == '-' }, State.NEW_TOKEN) { ++linePos; collector.dash(line, linePos) },
            Transition({ it == '*' }, State.NEW_TOKEN) { ++linePos; collector.dash(line, linePos) },
            Transition({ it == ':' }, State.NEW_TOKEN) { ++linePos; collector.colon(line, linePos) },
            Transition({ it == '/' }, State.BACKSLASH) { ++linePos },
            Transition(Char::isWhitespace, State.NEW_TOKEN) { ++linePos },
            Transition(this::isNameChar, State.NAME) { nameStart = totalPos; ++linePos },
            Transition({ it == 0.toChar() }, State.EOF) {}
        ),
        State.BACKSLASH to listOf(
            Transition({ it == '/' }, State.COMMENT, { ++linePos })
        ),
        State.COMMENT to listOf(
            Transition({ it != '\n' }, State.COMMENT, { ++linePos }),
            Transition({ it == '\n' }, State.NEW_TOKEN, { ++line; linePos = 0 })
        ),
        State.NAME to listOf(
            Transition(this::isNameChar, State.NAME) { ++linePos },
            Transition({ !isNameChar(it) }, State.NEW_TOKEN) {
                val name = input.substring(nameStart, totalPos)
                collector.name(name, line, nameStart)
                handleEvent(it)
            }
        )
    )

    private fun isNameChar(ch: Char) = ch.isLetterOrDigit() || ch == '_'
}