package zh.fsm.smc.lexer

import org.junit.jupiter.api.DisplayName

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Testing Lexer")
internal class LexerTest : TokenCollector {
    private var tokens = ""
    private val lexer = Lexer(this)
    private var firstToken = true

    private fun addToken(token: String) {
        if (!firstToken)
            tokens += ","
        tokens += token
        firstToken = false
    }

    private fun assertLexResult(input: String, expected: String) {
        lexer.lex(input)
        assertThat(tokens, `is`(equalTo(expected)))
    }

    override fun openBrace(line: Int, pos: Int) = addToken("OB")
    override fun closedBrace(line: Int, pos: Int) = addToken("CB")
    override fun openParen(line: Int, pos: Int) = addToken("OP")
    override fun closedParen(line: Int, pos: Int) = addToken("CP")
    override fun openAngle(line: Int, pos: Int) = addToken("OA")
    override fun closedAngle(line: Int, pos: Int) = addToken("CA")
    override fun dash(line: Int, pos: Int) = addToken("D")
    override fun colon(line: Int, pos: Int) = addToken("C")
    override fun name(name: String, line: Int, pos: Int) = addToken("#$name#")
    override fun error(line: Int, pos: Int) = addToken("E$line/$pos")

    @Nested
    @DisplayName("Finds single tokens")
    inner class SingleTokenTest {
        @Test
        @DisplayName("Open brace")
        internal fun findsOpenBrace() = assertLexResult("{", "OB")

        @Test
        @DisplayName("Closed brace")
        internal fun findsClosedBrace() = assertLexResult("}", "CB")

        @Test
        @DisplayName("Open paren")
        internal fun findsOpenParen() = assertLexResult("(", "OP")

        @Test
        @DisplayName("Closed paren")
        internal fun findsClosedParen() = assertLexResult(")", "CP")

        @Test
        @DisplayName("Open angle")
        internal fun findsOpenAngle() = assertLexResult("<", "OA")

        @Test
        @DisplayName("Closed angle")
        internal fun findsClosedAngle() = assertLexResult(">", "CA")

        @Test
        @DisplayName("Dash")
        internal fun findsDash() = assertLexResult("-", "D")

        @Test
        @DisplayName("Star as dash")
        internal fun findsStarAsDash() = assertLexResult("*", "D")

        @Test
        @DisplayName("Colon")
        internal fun findsColon() = assertLexResult(":", "C")

        @Test
        @DisplayName("Simple name")
        internal fun findsSimpleName() = assertLexResult("name", "#name#")

        @Test
        @DisplayName("Complex name")
        internal fun findsComplexName() = assertLexResult("Abc_Ebb", "#Abc_Ebb#")

        @Test
        @DisplayName("Throws error if necessary")
        internal fun error() = assertLexResult(".", "E1/1")

        @Test
        @DisplayName("Just one whitespace")
        internal fun justOneWhiteSpace() = assertLexResult(" ", "")

        @Test
        @DisplayName("Whitespace before a token")
        internal fun oneSpaceBefore() = assertLexResult("  \t\n \r  -", "D")
    }

    @Nested
    @DisplayName("Ignores comments")
    inner class CommentTests {
        @Test
        @DisplayName("Comment after token is cut off")
        internal fun commentAfterToken() = assertLexResult("-//comment\n", "D")

        @Test
        @DisplayName("Parses multiple lines with comments")
        internal fun commentLines() = assertLexResult("""
                //comment 1
                -//comment 2
                //comment 3
                -//comment 4
            """.trimIndent(), "D,D")
    }

    @Nested
    @DisplayName("Multiple tokens")
    inner class MultipleTokensTest {
        @Test
        @DisplayName("Parse simple sequence of tokens")
        internal fun simpleSequence() = assertLexResult("{}", "OB,CB")

        @Test
        @DisplayName("Parse complex sequence of tokens")
        internal fun complexSequence() = assertLexResult("FSM:fsm{this}", "#FSM#,C,#fsm#,OB,#this#,CB")

        @Test
        @DisplayName("Parse all tokens")
        internal fun allTokens() = assertLexResult("{}()<>-: name .", "OB,CB,OP,CP,OA,CA,D,C,#name#,E1/15")

        @Test
        @DisplayName("Parse multiple lines")
        internal fun multipleLines() = assertLexResult("FSM:fsm.\n{bob-.}", "#FSM#,C,#fsm#,E1/8,OB,#bob#,D,E2/6,CB")
    }
}