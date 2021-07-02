package zh.fsm.smc.lexer

import org.junit.jupiter.api.DisplayName

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

@DisplayName("Testing FSM Lexer")
internal class FsmLexerTest : BaseLexerTest() {
    override fun getLexer(): Lexer = FsmLexer(this)
}