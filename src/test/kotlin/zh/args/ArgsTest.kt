package zh.args

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import zh.args.ArgsException.ErrorCode.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Args tests")
internal class ArgsTest {
    private fun Args.assertNextArgument(expected: Int) = assertThat(nextArgument(), `is`(expected))
    private fun Args.assertHasArgument(arugment: Char) = assertTrue { has(arugment) }
    private fun Args.assertHasNotArgument(arugment: Char) = assertFalse { has(arugment) }
    private fun Args.assertBoolean(argument: Char, expected: Boolean) = assertThat(getBoolean(argument), `is`(expected))
    private fun Args.assertInt(argument: Char, expected: Int) = assertThat(getInt(argument), `is`(expected))
    private fun Args.assertDouble(argument: Char, expected: Double) = assertEquals(expected, getDouble(argument), .001)
    private fun Args.assertString(argument: Char, expected: String) = assertThat(getString(argument), `is`(expected))
    private fun Args.assertStringArray(argument: Char, index: Int, expected: String) = assertThat(getStringArray(argument)[index], `is`(expected))
    private fun Args.assertStringArraySize(argument: Char, expectedSize: Int) = assertThat(getStringArray(argument).size, `is`(expectedSize))
    private fun Args.assertMap(argument: Char, key: String, expectedValue: String?) = assertThat(getMap(argument)[key], `is`(expectedValue))

    private fun ArgsException.assertErrorCode(expected: ArgsException.ErrorCode) = assertThat(errorCode, `is`(expected))
    private fun ArgsException.assertErrorArgumentId(expected: Char) = assertThat(errorArgumentId, `is`(expected))
    private fun ArgsException.assertErrorParameter(expected: String?) = assertThat(errorParameter, `is`(expected))

    @Test
    internal fun createWithNoSchemaAndArguments() {
        val args = Args("", emptyArray())

        args.assertNextArgument(0)
    }

    @Test
    internal fun createWithNoSchemaButOneArgument() {
        val exception = assertThrows<ArgsException> { Args("", arrayOf("-x")) }

        exception.assertErrorCode(UNEXPECTED_ARGUMENT)
        exception.assertErrorArgumentId('x')
    }

    @Test
    internal fun createWithNoSchemaButWithMultipleArguments() {
        val exception = assertThrows<ArgsException> { Args("", arrayOf("-x", "-y")) }

        exception.assertErrorCode(UNEXPECTED_ARGUMENT)
        exception.assertErrorArgumentId('x')
    }

    @Test
    internal fun nonLetterSchema() {
        val exception = assertThrows<ArgsException> { Args("*", emptyArray()) }

        exception.assertErrorCode(INVALID_ARGUMENT_NAME)
        exception.assertErrorArgumentId('*')
    }

    @Test
    internal fun invlaidArgumentFormat() {
        val exception = assertThrows<ArgsException> { Args("f~", emptyArray()) }

        exception.assertErrorCode(INVALID_ARGUMENT_FORMAT)
        exception.assertErrorArgumentId('f')
    }

    @Test
    internal fun simpleBooleanPresent() {
        val args = Args("x", arrayOf("-x"))

        args.assertBoolean('x', true)
        args.assertNextArgument(1)
    }

    @Test
    internal fun simpleStringPresent() {
        val args = Args("x*", arrayOf("-x", "param"))

        args.assertHasArgument('x')
        args.assertString('x', "param")
        args.assertNextArgument(2)
    }

    @Test
    internal fun testMissingStringArgument() {
        val exception = assertThrows<ArgsException> { Args("x*", arrayOf("-x")) }

        exception.assertErrorCode(MISSING_STRING)
        exception.assertErrorArgumentId('x')
    }

    @Test
    internal fun testSpacesInFormat() {
        val args = Args("x, y", arrayOf("-xy"))

        args.assertHasArgument('x')
        args.assertHasArgument('y')
        args.assertNextArgument(1)
    }

    @Test
    internal fun testSimpleIntPresent() {
        val args = Args("x#", arrayOf("-x", "42"))

        args.assertHasArgument('x')
        args.assertInt('x', 42)
        args.assertNextArgument(2)
    }

    @Test
    internal fun testInvalidInteger() {
        val exception = assertThrows<ArgsException> { Args("x#", arrayOf("-x", "forty two")) }

        exception.assertErrorCode(INVALID_INTEGER)
        exception.assertErrorArgumentId('x')
        exception.assertErrorParameter("forty two")
    }

    @Test
    internal fun testMissingInteger() {
        val exception = assertThrows<ArgsException> { Args("x#", arrayOf("-x")) }

        exception.assertErrorCode(MISSING_INTEGER)
        exception.assertErrorArgumentId('x')
    }

    @Test
    internal fun testSimpleDoublePresent() {
        val args = Args("x##", arrayOf("-x", "42.3"))

        args.assertHasArgument('x')
        args.assertDouble('x', 42.3)
    }

    @Test
    internal fun testInvalidDouble() {
        val exception = assertThrows<ArgsException> { Args("x##", arrayOf("-x", "forty two")) }

        exception.assertErrorCode(INVALID_DOUBLE)
        exception.assertErrorArgumentId('x')
        exception.assertErrorParameter("forty two")
    }

    @Test
    internal fun testMissingDouble() {
        val exception = assertThrows<ArgsException> { Args("x##", arrayOf("-x")) }

        exception.assertErrorCode(MISSING_DOUBLE)
        exception.assertErrorArgumentId('x')
    }

    @Test
    internal fun testStringArray() {
        val args = Args("x[*]", arrayOf("-x", "alpha"))

        args.assertHasArgument('x')
        args.assertStringArraySize('x', 1)
        args.assertStringArray('x', 0, "alpha")
    }

    @Test
    internal fun testMissingStringArrayElement() {
        val exception = assertThrows<ArgsException> { Args("x[*]", arrayOf("-x")) }

        exception.assertErrorCode(MISSING_STRING)
        exception.assertErrorArgumentId('x')
    }

    @Test
    internal fun manyStringArrayElements() {
        val args = Args("x[*]", arrayOf("-x", "alpha", "-x", "beta", "-x", "gamma"))

        args.assertHasArgument('x')
        args.assertStringArraySize('x', 3)
        args.assertStringArray('x', 0, "alpha")
        args.assertStringArray('x', 1, "beta")
        args.assertStringArray('x', 2, "gamma")
    }

    @Test
    internal fun testMapArgument() {
        val args = Args("f&", arrayOf("-f", "key1:val1,key2:val2"))

        args.assertHasArgument('f')
        args.assertMap('f', "key1", "val1")
        args.assertMap('f', "key2", "val2")
    }

    @Test
    internal fun testOneMapArgument() {
        val args = Args("f&", arrayOf("-f", "key1:val1"))

        args.assertHasArgument('f')
        args.assertMap('f', "key1", "val1")
    }

    @Test
    internal fun malFormedMapArgument() {
        val exception = assertThrows<ArgsException> { Args("f&", arrayOf("-f", "key1:val1,key2")) }

        exception.assertErrorCode(MALFORMED_MAP)
        exception.assertErrorArgumentId('f')
    }

    @Test
    internal fun testExtraArgument() {
        val args = Args("x,y*", arrayOf("-x", "-y", "alpha", "beta"))

        args.assertBoolean('x', true)
        args.assertString('y', "alpha")
        args.assertNextArgument(3)
    }

    @Test
    internal fun testExtraArgumentsThatLookLikeFlags() {
        val args = Args("x,y", arrayOf("-x", "alpha", "-y", "beta"))

        args.assertHasArgument('x')
        args.assertHasNotArgument('y')
        args.assertBoolean('x', true)
        args.assertBoolean('y', false)
        args.assertNextArgument(1)
    }
}