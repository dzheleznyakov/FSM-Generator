package zh.fsm.smc.parser

class FsmSyntax {
    val headers = mutableListOf<Header>()
    val logic = mutableListOf<Transition>()
    val errors = mutableListOf<SyntaxError>()
    var done = false

    class Header(var name: String, var value: String) {
        constructor() : this("", "")

        companion object {
            fun NullHeader() = Header()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Header) return false

            if (name != other.name) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }
    }

    class Transition {
        lateinit var state: StateSpec
        val subTransitions = mutableListOf<SubTransition>()
    }

    class StateSpec {
        lateinit var name: String
        val superStates = mutableListOf<String>()
        val entryActions = mutableListOf<String>()
        val exitActions = mutableListOf<String>()
        var abstractState = false
    }

    class SubTransition(val event: String?) {
        var nextState: String? = null
        val actions = mutableListOf<String>()
    }

    class SyntaxError(
        val type: Type,
        var msg: String,
        val lineNumber: Int,
        val position: Int
    ) {
        override fun toString() =
            "Syntax Error Line: $lineNumber, Position: $position.  (${type.name}) $msg"

        enum class Type { HEADER, STATE, TRANSITION, TRANSITION_GROUP, END, SYNTAX }
    }

    override fun toString(): String = formatHeaders() +
            formatLogic() +
            (if (done) ".\n" else "") +
            formatErrors()

    private fun formatHeaders(): String =
        headers.joinToString(separator = "", transform = this::formatHeader)

    private fun formatHeader(header: Header): String = header.run { "$name:$value\n" }

    private fun formatLogic(): String =
        if (logic.isNotEmpty()) "{\n${formatTransitions()}}\n"
        else ""

    private fun formatTransitions(): String =
        logic.joinToString(separator = "", transform = this::formatTransition)

    private fun formatTransition(transition: Transition): String =
        "  ${formatStateName(transition.state)} ${formatSubTransitions(transition)}\n"

    private fun formatStateName(stateSpec: StateSpec): String {
        var stateName = if (stateSpec.abstractState) "(${stateSpec.name})" else stateSpec.name
        stateSpec.superStates.forEach { stateName += ":${it}" }
        stateSpec.entryActions.forEach { stateName += " <${it}" }
        stateSpec.exitActions.forEach { stateName += " >${it}" }
        return stateName
    }

    private fun formatSubTransitions(transition: Transition): String =
        if (transition.subTransitions.size == 1) formatSubTransition(transition.subTransitions[0])
        else transition.subTransitions
            .map(this::formatSubTransition)
            .map { "    $it"}
            .joinToString(separator = "", transform = { s -> "$s\n"}, prefix = "{\n", postfix = "  }")

    private fun formatSubTransition(subTransition: SubTransition): String =
        subTransition.run { "$event $nextState ${formatActions(this)}" }

    private fun formatActions(subTransition: SubTransition): String =
        if (subTransition.actions.size == 1) subTransition.actions[0]
        else subTransition.actions
            .joinToString(separator = " ", prefix = "{", postfix = "}")

    private fun formatErrors(): String =
        if (errors.isNotEmpty()) formatError(errors[0])
        else ""

    private fun formatError(error: SyntaxError): String = error.run {
        "Syntax error: $type. $msg. line $lineNumber, position $position.\n"
    }

    fun getError() = formatErrors()
}