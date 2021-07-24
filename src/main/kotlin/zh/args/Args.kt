package zh.args

import zh.args.ArgsException.ErrorCode.*

class Args(schema: String, args: Array<String>) {
    private val marshallers = hashMapOf<Char, ArgumentMarshaller>()
    private val argsFound = hashSetOf<Char>()
    private val currentArgument = args.toList().listIterator()

    init {
        parseSchema(schema)
        parseArgumentStrings()
    }

    @Throws(ArgsException::class)
    fun parseSchema(schema: String): Unit = schema.split(",")
        .filter { it.isNotEmpty() }
        .map { it.trim() }
        .forEach(this::parseSchemaElement)

    private fun parseSchemaElement(element: String) {
        val elementId = element[0]
        val elementTail = element.substring(1)
        validateSchemaElementId(elementId)
        val marshaller: ArgumentMarshaller = when {
            elementTail.isEmpty() -> BooleanArgumentMarshaller()
            elementTail == "*" -> StringArgumentMarshaller()
            elementTail == "#" -> IntegerArgumentMarshaller()
            elementTail == "##" -> DoubleArgumentMarshaller()
            elementTail == "[*]" -> StringArrayArgumentMarshaller()
            elementTail == "&" -> MapArgumentMarshaller()
            else -> throw ArgsException(
                errorCode = INVALID_ARGUMENT_FORMAT,
                errorArgumentId = elementId,
                errorParameter = elementTail)
        }
        marshallers[elementId] = marshaller
    }

    @Throws(ArgsException::class)
    private fun validateSchemaElementId(elementId: Char) {
        if (!elementId.isLetter())
            throw ArgsException(errorCode = INVALID_ARGUMENT_NAME, errorArgumentId = elementId)
    }

    @Throws(ArgsException::class)
    private fun parseArgumentStrings() {
        while (currentArgument.hasNext()) {
            val argString = currentArgument.next()
            if (argString.startsWith("-"))
                parseArgumentCharacters(argString.substring(1))
            else {
                currentArgument.previous()
                break;
            }
        }
    }

    private fun parseArgumentCharacters(argChars: String) = argChars.forEach { parseArgumentCharacter(it) }

    private fun parseArgumentCharacter(argChar: Char) {
        val marshaller = marshallers[argChar]
        if (marshaller == null)
            throw ArgsException(errorCode = UNEXPECTED_ARGUMENT, errorArgumentId = argChar)
        else {
            argsFound.add(argChar)
            try {
                marshaller.set(currentArgument)
            } catch (e: ArgsException) {
                e.errorArgumentId = argChar
                throw e
            }
        }
    }

    fun has(arg: Char): Boolean = argsFound.contains(arg)
    fun nextArgument(): Int = currentArgument.nextIndex()

    fun getBoolean(arg: Char): Boolean = BooleanArgumentMarshaller.getValue(marshallers[arg])
    fun getString(arg: Char): String = StringArgumentMarshaller.getValue(marshallers[arg])
    fun getInt(arg: Char): Int = IntegerArgumentMarshaller.getValue(marshallers[arg])
    fun getDouble(arg: Char): Double = DoubleArgumentMarshaller.getValue(marshallers[arg])
    fun getStringArray(arg: Char): Array<String> = StringArrayArgumentMarshaller.getValue(marshallers[arg])
    fun getMap(arg: Char): Map<String, String> = MapArgumentMarshaller.getValue(marshallers[arg])
}