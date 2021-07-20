package zh.fsm.smc.implementers

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers

fun assertWhitespaceEquivalent(generated: String, expected: String) {
    MatcherAssert.assertThat(generated.compressWhiteSpace(), Matchers.equalTo(expected.compressWhiteSpace()))
}

fun String.compressWhiteSpace(): String =
    replace("\\n+".toRegex(), "\n").replace("[\t ]+".toRegex(), " ").replace(" *\n *".toRegex(), "\n")
