package zh.args

class BooleanArgumentMarshaller : ArgumentMarshaller {
    private var booleanValue = false

    override fun set(currentArgument: Iterator<String>) {
        booleanValue = true
    }

    companion object {
        fun getValue(am: ArgumentMarshaller?): Boolean =
            if (am is BooleanArgumentMarshaller) am.booleanValue
            else false
    }
}