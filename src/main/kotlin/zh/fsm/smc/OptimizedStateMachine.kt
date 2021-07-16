package zh.fsm.smc

class OptimizedStateMachine {
    val states = arrayListOf<String>()
    val events = arrayListOf<String>()
    val actions = arrayListOf<String>()
    lateinit var header: Header
    val transitions = arrayListOf<Transition>()

    fun transitionsToString(): String = transitions.joinToString(separator = "")

    override fun toString(): String {
        val transitionsString = transitionsToString()
            .replace("\n".toRegex(), "\n  ")
            .run { substring(0, length - 2) }
        return "Initial: ${header.initial}\n" +
                "FSM: ${header.fsm}\nActions:${header.actions}\n" +
                "{\n" +
                "  ${transitionsString}}\n"
    }

    class Header {
        lateinit var initial: String
        lateinit var fsm: String
        lateinit var actions: String
    }

    class Transition {
        lateinit var currentState: String
        val subTransitions: ArrayList<SubTransition> = arrayListOf()

        override fun toString(): String = "" +
                "$currentState {\n" +
                subTransitions.map { it.toString() }.joinToString(separator = "") +
                "}\n"
    }

    class SubTransition {
        lateinit var event: String
        lateinit var nextState: String
        val actions: ArrayList<String> = arrayListOf()

        private fun actionsToString(): String =
            if (actions.isEmpty()) ""
            else actions.joinToString(separator = " ")

        override fun toString(): String =
            "  $event $nextState {${actionsToString()}}\n"
    }
}