package zh.fsm.smc.lexer

import org.junit.jupiter.api.DisplayName

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Testing Simple Lexer")
internal class SimpleLexerTest : BaseLexerTest() {
    override fun getLexer(): Lexer = SimpleLexer(this)
}