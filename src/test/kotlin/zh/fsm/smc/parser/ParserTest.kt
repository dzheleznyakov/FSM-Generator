package zh.fsm.smc.parser

import org.junit.jupiter.api.DisplayName

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zh.fsm.smc.lexer.Lexer
import zh.fsm.smc.parser.ParserEvent.*

@DisplayName("Testing Parser")
internal class ParserTest {
    private lateinit var lexer: Lexer
    private lateinit var parser: Parser
    private lateinit var builder: SyntaxBuilder

    @BeforeEach
    internal fun setUp() {
        builder = SyntaxBuilder()
        parser = Parser(builder)
        lexer = Lexer(parser)
    }

    private fun assertParseResult(input: String, expected: String) {
        lexer.lex(input)
        parser.handleEvent(EOF, -1, -1)
        val actual = builder.getFsm().toString()
        assertThat(actual, `is`(equalTo(expected)))
    }

    private fun assertParseResult(input: String, vararg expectedLines: String) {
        lexer.lex(input)
        parser.handleEvent(EOF, -1, -1)
        val actual = builder.getFsm().toString()
        assertThat(actual, `is`(equalTo(expectedLines.joinToString(separator = "\n"))))
    }

    private fun assertParseError(input: String, expected: String) {
        lexer.lex(input)
        parser.handleEvent(EOF, -1, -1)
        val actual = builder.getFsm().getError()
        assertThat(actual, `is`(equalTo(expected)))
    }

    @Nested
    inner class IncrementalTests {
        @Test
        @DisplayName("Parse a single header")
        internal fun parseOneHeader() = assertParseResult("N:V{}", "N:V\n.\n")

        @Test
        @DisplayName("Parse multiple headers")
        internal fun parseManyHeaders() = assertParseResult("  N1 : V1\tN2 : V2\n{}", "N1:V1\nN2:V2\n.\n")

        @Test
        @DisplayName("Parse input with no headers")
        internal fun noHeaders() = assertParseResult("{}", ".\n")

        @Test
        @DisplayName("Parse a simple transition")
        internal fun simpleTransition() = assertParseResult("{ s e ns a }", "" +
                "{\n" +
                "  s e ns a\n" +
                "}\n" +
                ".\n")

        @Test
        @DisplayName("Parse a transition with null action")
        internal fun transitionWithNullAction() = assertParseResult("{s e ns - }", "" +
                "{\n" +
                "  s e ns {}\n" +
                "}\n" +
                ".\n")

        @Test
        @DisplayName("Parse a transition with multiple actions")
        internal fun transitionWithManyActions() = assertParseResult("{s e ns {a1 a2} }", "" +
                "{\n" +
                "  s e ns {a1 a2}\n" +
                "}\n" +
                ".\n")

        @Test
        @DisplayName("Parse a state with subtransition")
        internal fun stateWithSubTransition() = assertParseResult("{s {e ns a}}",
            "{",
            "  s e ns a",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with multiple subtransitions")
        internal fun stateWithSeveralSubTransitions() = assertParseResult("{s {e1 ns a1 e2 ns a2}}",
            "{",
            "  s {",
            "    e1 ns a1",
            "    e2 ns a2",
            "  }",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse many transitions")
        internal fun manyTransitions() = assertParseResult("{s1 e1 s2 a1 s2 e2 s3 a2}",
            "{",
            "  s1 e1 s2 a1",
            "  s2 e2 s3 a2",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse super-state")
        internal fun superState() = assertParseResult("{ (ss) e s a }",
            "{",
            "  (ss) e s a",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with entry action")
        internal fun entryAction() = assertParseResult("{s <ea e ns a}",
            "{",
            "  s <ea e ns a",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with exit action")
        internal fun exitAction() = assertParseResult("{s >xa e ns a}",
            "{",
            "  s >xa e ns a",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with a base state")
        internal fun derivedState() = assertParseResult("{s:ss e ns a}",
            "{",
            "  s:ss e ns a",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with all the adornments")
        internal fun allStateAdornments() = assertParseResult("{(s)<ea>xa:ss e ns a}",
            "{",
            "  (s):ss <ea >xa e ns a",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with no sub-transitions")
        internal fun stateWithNoSubtransitions() = assertParseResult("{s { }}",
            "{",
            "  s {",
            "  }",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with all dashes")
        internal fun stateWithAllDashes() = assertParseResult("{s - - -}",
            "{",
            "  s null null {}",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with multiple superstates")
        internal fun multipleSuperStates() = assertParseResult("{s :x :y - - -}",
            "{",
            "  s:x:y null null {}",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with multiple entry actions")
        internal fun multipleEntryActions() = assertParseResult("{s <x <y - - -}",
            "{",
            "  s <x <y null null {}",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with multiple exit actions")
        internal fun multipleExitActions() = assertParseResult("{s >x >y - - -}",
            "{",
            "  s >x >y null null {}",
            "}",
            ".\n"
        )

        @Test
        @DisplayName("Parse a state with multiple grouped entry and exit actions")
        internal fun multipleEntryAndExitActionsWithBraces() = assertParseResult("{s <{u v} >{w x} - - -}",
            "{",
            "  s <u <v >w >x null null {}",
            "}",
            ".\n"
        )
    }

    @Nested
    inner class AcceptanceTests {
        @Test
        @DisplayName("One Coin Turnstile FSM")
        internal fun simpleOneCoinTurnstile() = assertParseResult(
            "" +
                    "Actions: Turnstile\n" +
                    "FSM: OneCoinTurnstile\n" +
                    "Initial: Locked\n" +
                    "{\n" +
                    "  Locked\tCoin\tUnlocked\t{alarmOff unlock}\n" +
                    "  Locked \tPass\tLocked\t\talarmOn\n" +
                    "  Unlocked\tCoin\tUnlocked\tthankYou\n" +
                    "  Unlocked\tPass\tLocked\t\tlock\n" +
                    "}",
            "" +
                    "Actions:Turnstile\n" +
                    "FSM:OneCoinTurnstile\n" +
                    "Initial:Locked\n" +
                    "{\n" +
                    "  Locked Coin Unlocked {alarmOff unlock}\n" +
                    "  Locked Pass Locked alarmOn\n" +
                    "  Unlocked Coin Unlocked thankYou\n" +
                    "  Unlocked Pass Locked lock\n" +
                    "}\n" +
                    ".\n"
        )

        @Test
        @DisplayName("Two Coin Turnstile FSM without super state")
        internal fun twoCoinTurnstileWithoutSuperState() = assertParseResult(
            "" +
                    "Actions: Turnstile\n" +
                    "FSM: TwoCoinTurnstile\n" +
                    "Initial: Locked\n" +
                    "{\n" +
                    "\tLocked {\n" +
                    "\t\tPass\tAlarming\talarmOn\n" +
                    "\t\tCoin\tFirstCoin\t-\n" +
                    "\t\tReset\tLocked\t{lock alarmOff}\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tAlarming\tReset\tLocked {lock alarmOff}\n" +
                    "\t\n" +
                    "\tFirstCoin {\n" +
                    "\t\tPass\tAlarming\t-\n" +
                    "\t\tCoin\tUnlocked\tunlock\n" +
                    "\t\tReset\tLocked {lock alarmOff}\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tUnlocked {\n" +
                    "\t\tPass\tLocked\tlock\n" +
                    "\t\tCoin\t-\t\tthankYou\n" +
                    "\t\tReset\tLocked {lock alarmOff}\n" +
                    "\t}\n" +
                    "}",
            "" +
                    "Actions:Turnstile\n" +
                    "FSM:TwoCoinTurnstile\n" +
                    "Initial:Locked\n" +
                    "{\n" +
                    "  Locked {\n" +
                    "    Pass Alarming alarmOn\n" +
                    "    Coin FirstCoin {}\n" +
                    "    Reset Locked {lock alarmOff}\n" +
                    "  }\n" +
                    "  Alarming Reset Locked {lock alarmOff}\n" +
                    "  FirstCoin {\n" +
                    "    Pass Alarming {}\n" +
                    "    Coin Unlocked unlock\n" +
                    "    Reset Locked {lock alarmOff}\n" +
                    "  }\n" +
                    "  Unlocked {\n" +
                    "    Pass Locked lock\n" +
                    "    Coin null thankYou\n" +
                    "    Reset Locked {lock alarmOff}\n" +
                    "  }\n" +
                    "}\n" +
                    ".\n"
        )

        @Test
        @DisplayName("Two Coin Turnstile FSM with super state")
        internal fun twoCoinTurnstileWithSuperState() = assertParseResult(
            "" +
                    "Actions: Turnstile\n" +
                    "FSM: TwoCoinTurnstile\n" +
                    "Initial: Locked\n" +
                    "{\n" +
                    "    (Base)\tReset\tLocked\tlock\n" +
                    "\n" +
                    "\tLocked : Base {\n" +
                    "\t\tPass\tAlarming\t-\n" +
                    "\t\tCoin\tFirstCoin\t-\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tAlarming : Base\t<alarmOn >alarmOff -\t-\t-\n" +
                    "\t\n" +
                    "\tFirstCoin : Base {\n" +
                    "\t\tPass\tAlarming\t-\n" +
                    "\t\tCoin\tUnlocked\tunlock\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tUnlocked : Base {\n" +
                    "\t\tPass\tLocked\tlock\n" +
                    "\t\tCoin\t-\t\tthankYou\n" +
                    "\t}\n" +
                    "}",
            "" +
                    "Actions:Turnstile\n" +
                    "FSM:TwoCoinTurnstile\n" +
                    "Initial:Locked\n" +
                    "{\n" +
                    "  (Base) Reset Locked lock\n" +
                    "  Locked:Base {\n" +
                    "    Pass Alarming {}\n" +
                    "    Coin FirstCoin {}\n" +
                    "  }\n" +
                    "  Alarming:Base <alarmOn >alarmOff null null {}\n" +
                    "  FirstCoin:Base {\n" +
                    "    Pass Alarming {}\n" +
                    "    Coin Unlocked unlock\n" +
                    "  }\n" +
                    "  Unlocked:Base {\n" +
                    "    Pass Locked lock\n" +
                    "    Coin null thankYou\n" +
                    "  }\n" +
                    "}\n" +
                    ".\n"
        )
    }

    @Nested
    inner class ErrorTests {
        @Test
        @DisplayName("Error parsing an empty string")
        internal fun parseNothing() = assertParseError("", "Syntax error: HEADER. HEADER|EOF. line -1, position -1.\n")

        @Test
        @DisplayName("Error parsing a header with no colon")
        internal fun headerWithNoColon() = assertParseError("A B { s e ns a }",
            "Syntax error: HEADER. HEADER_COLON|NAME. line 1, position 2.\n")

        @Test
        @DisplayName("Error parsing a header with no value")
        internal fun headerWithNoValue() = assertParseError(
            "A: {s e ns a}", "Syntax error: HEADER. HEADER_VALUE|OPEN_BRACE. line 1, position 3.\n"
        )

        @Test
        @DisplayName("Error parsing a very short transition")
        internal fun transitionWayTooShort() = assertParseError(
            "{s}", "Syntax error: STATE. STATE_MODIFIER|CLOSED_BRACE. line 1, position 2.\n"
        )

        @Test
        @DisplayName("Error parsing a short transition")
        internal fun shortTransition() = assertParseError(
            "{s e}", "Syntax error: TRANSITION. SINGLE_EVENT|CLOSED_BRACE. line 1, position 4.\n"
        )

        @Test
        @DisplayName("Error parsing transition with no action")
        internal fun transitionNoAction() = assertParseError(
            "{s e ns}", "Syntax error: TRANSITION. SINGLE_NEXT_STATE|CLOSED_BRACE. line 1, position 7.\n"
        )

        @Test
        @DisplayName("Error parsing logic with no closing brace")
        internal fun noClosingBrace() = assertParseError(
            "{", "Syntax error: STATE. STATE_SPEC|EOF. line -1, position -1.\n"
        )

        @Test
        @DisplayName("Error parsing dash state")
        internal fun initialStateDash() = assertParseError(
            "{- e ns a}", "Syntax error: STATE. STATE_SPEC|DASH. line 1, position 1.\n"
        )

        @Test
        @DisplayName("Error parsing spec with lexical error")
        internal fun lexicalError() = assertParseError(
            "{.}", "Syntax error: SYNTAX. . line 1, position 2.\n"
        )
    }
}