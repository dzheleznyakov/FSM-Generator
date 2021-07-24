package zh.args

class ArgsException(
    var errorArgumentId: Char = '\u0000',
    val errorParameter: String? = null,
    val errorCode: ErrorCode = ErrorCode.OK,
    message: String? = null
) : Exception(message) {

    fun errorMessage(): String = when (errorCode) {
        ErrorCode.OK -> "TILT: Should not get here."
        ErrorCode.UNEXPECTED_ARGUMENT -> "Argument -$errorArgumentId unexpected."
        ErrorCode.MISSING_STRING -> "Could not find string parameter for -$errorArgumentId."
        ErrorCode.INVALID_INTEGER -> "Argument -$errorArgumentId expects an integer but was '$errorParameter'."
        ErrorCode.MISSING_INTEGER -> "Could not find integer parameter for -$errorArgumentId."
        ErrorCode.INVALID_DOUBLE -> "Argument -$errorArgumentId expects a double but was '$errorParameter'."
        ErrorCode.MISSING_DOUBLE -> "Could not find double parameter for -$errorArgumentId."
        ErrorCode.MALFORMED_MAP -> "Argument -$errorArgumentId expects to be a map but was '$errorParameter'."
        ErrorCode.MISSING_MAP -> "Could not find map parameter for -$errorArgumentId."
        ErrorCode.INVALID_ARGUMENT_NAME -> "'$errorArgumentId' is not a valid argument name."
        ErrorCode.INVALID_ARGUMENT_FORMAT -> "'$errorParameter' is not a valid argument format."
        else -> ""
    }

    enum class ErrorCode {
        OK,
        INVALID_ARGUMENT_FORMAT,
        UNEXPECTED_ARGUMENT,
        INVALID_ARGUMENT_NAME,
        MISSING_STRING,
        MISSING_INTEGER,
        INVALID_INTEGER,
        MISSING_DOUBLE,
        INVALID_DOUBLE,
        MALFORMED_MAP,
        MISSING_MAP
    }
}