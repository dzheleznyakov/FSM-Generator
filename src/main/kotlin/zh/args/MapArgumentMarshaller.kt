package zh.args

import zh.args.ArgsException.ErrorCode.MALFORMED_MAP
import zh.args.ArgsException.ErrorCode.MISSING_MAP

class MapArgumentMarshaller : ArgumentMarshaller {
    private val map = hashMapOf<String, String>()

    override fun set(currentArgument: Iterator<String>) {
        try {
            val mapEntries = currentArgument.next().split(",")
                .map { entry -> entry.split(":") }
                .filter { entryComponents ->
                    if (entryComponents.size != 2)
                        throw ArgsException(errorCode = MALFORMED_MAP, errorParameter = entryComponents.joinToString(":"))
                    else true
                }
                .map { entryComponents -> entryComponents[0] to entryComponents[1] }
            map.putAll(mapEntries)
        } catch (e: NoSuchElementException) {
            throw ArgsException(errorCode = MISSING_MAP)
        }
    }

    companion object {
        fun getValue(am: ArgumentMarshaller?): Map<String, String> =
            if (am is MapArgumentMarshaller) am.map
            else emptyMap()
    }
}