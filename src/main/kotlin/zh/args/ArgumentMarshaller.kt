package zh.args

interface ArgumentMarshaller {
    @Throws(ArgsException::class)
    fun set(currentArgument: Iterator<String>)
}