package zh.args

import zh.args.ArgsException.ErrorCode.MISSING_STRING

class StringArrayArgumentMarshaller : ArgumentMarshaller {
    private val strings = arrayListOf<String>()

    override fun set(currentArgument: Iterator<String>) {
        try {
            strings.add(currentArgument.next())
        } catch (e: NoSuchElementException) {
            throw ArgsException(errorCode = MISSING_STRING)
        }
    }

    companion object {
        fun getValue(am: ArgumentMarshaller?): Array<String> =
            if (am is StringArrayArgumentMarshaller) am.strings.toArray(arrayOf())
            else emptyArray()
    }
}