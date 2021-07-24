package zh.args

import zh.args.ArgsException.ErrorCode.INVALID_DOUBLE
import zh.args.ArgsException.ErrorCode.MISSING_DOUBLE

class DoubleArgumentMarshaller : ArgumentMarshaller {
    private var doubleValue = 0.0

    override fun set(currentArgument: Iterator<String>) {
        var parameter = ""
        try {
            parameter = currentArgument.next()
            doubleValue = parameter.toDouble()
        } catch (e: NoSuchElementException) {
            throw ArgsException(errorCode = MISSING_DOUBLE)
        } catch (e: NumberFormatException) {
            throw ArgsException(errorCode = INVALID_DOUBLE, errorParameter = parameter)
        }
    }

    companion object {
        fun getValue(am: ArgumentMarshaller?): Double =
            if (am is DoubleArgumentMarshaller) am.doubleValue
            else 0.0
    }
}