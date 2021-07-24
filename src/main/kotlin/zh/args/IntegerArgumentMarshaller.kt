package zh.args

import zh.args.ArgsException.ErrorCode.INVALID_INTEGER
import zh.args.ArgsException.ErrorCode.MISSING_INTEGER

class IntegerArgumentMarshaller : ArgumentMarshaller {
    private var intValue = 0

    override fun set(currentArgument: Iterator<String>) {
        var parameter = ""
        try {
            parameter = currentArgument.next()
            intValue = parameter.toInt()
        } catch (e: NoSuchElementException) {
            throw ArgsException(errorCode = MISSING_INTEGER)
        } catch (e: NumberFormatException) {
            throw ArgsException(errorCode = INVALID_INTEGER, errorParameter = parameter)
        }
    }

    companion object {
        fun getValue(am: ArgumentMarshaller?): Int =
            if (am is IntegerArgumentMarshaller) am.intValue
            else 0
    }
}