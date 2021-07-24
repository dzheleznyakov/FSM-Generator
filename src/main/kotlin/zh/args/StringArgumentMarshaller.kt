package zh.args

import zh.args.ArgsException.ErrorCode.MISSING_STRING

class StringArgumentMarshaller : ArgumentMarshaller {
    private var stringValue = ""

    override fun set(currentArgument: Iterator<String>) {
        try {
            stringValue = currentArgument.next()
        } catch (e: NoSuchElementException) {
            throw ArgsException(errorCode = MISSING_STRING)
        }
    }

    companion object {
        fun getValue(am: ArgumentMarshaller?): String =
            if (am is StringArgumentMarshaller) am.stringValue
            else ""
    }
}