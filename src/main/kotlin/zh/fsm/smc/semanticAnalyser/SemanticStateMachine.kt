package zh.fsm.smc.semanticAnalyser

import java.util.*
import kotlin.collections.HashSet

class SemanticStateMachine {
    val errors = mutableListOf<AnalysisError>()
    val warnings = mutableListOf<AnalysisError>()
    val states: SortedMap<String, SemanticState> = TreeMap()
    val events = HashSet<String>()
    val actions = HashSet<String>()
    var initialState: SemanticState = SemanticState("")
    lateinit var actionsClass: String
    lateinit var fsmName: String

    override fun toString() = "" +
            "Actions: $actionsClass\n" +
            "FSM: $fsmName\n" +
            "Initial: ${initialState.name}" +
            statesToString()

    fun addError(analysisError: AnalysisError) = errors.add(analysisError)

    fun statesToString() = states.values.joinToString(separator = "", prefix = "{", postfix = "}\n")

    class SemanticState(val name: String) : Comparable<SemanticState> {
        val entryActions = mutableListOf<String>()
        val exitActions = mutableListOf<String>()
        var abstractState = false
        val superStates: SortedSet<SemanticState> = TreeSet()
        val transitions = mutableListOf<SemanticTransition>()

        override fun compareTo(other: SemanticState) = name.compareTo(other.name)

        override fun toString() =
            "\n  ${makeStateNameWithAdornments()} {\n${makeTransitionStrings()}  }\n"

        private fun makeTransitionStrings() =
            transitions.joinToString(separator = "", transform = this::makeTransitionString)

        private fun makeTransitionString(st: SemanticTransition) =
            "    ${st.event} ${makeNextStateName(st)} {${makeActions(st)}}\n"

        private fun makeNextStateName(st: SemanticTransition) =
            if (st.nextState == null) "null" else st.nextState!!.name

        private fun makeActions(st: SemanticTransition) =
            st.actions.joinToString(separator = " ")

        private fun makeStateNameWithAdornments(): String {
            var stateName = if (abstractState) "($name)" else name
            stateName = superStates.map { " :${it.name}" }.joinToString(separator = "", prefix = stateName)
            stateName = entryActions.map { " <$it" }.joinToString(separator = "", prefix = stateName)
            stateName = exitActions.map { " >$it" }.joinToString(separator = "", prefix = stateName)
            return stateName
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SemanticState) return false

            if (name != other.name) return false
            if (entryActions != other.entryActions) return false
            if (exitActions != other.exitActions) return false
            if (abstractState != other.abstractState) return false
            if (superStates != other.superStates) return false
            if (transitions != other.transitions) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + entryActions.hashCode()
            result = 31 * result + exitActions.hashCode()
            result = 31 * result + abstractState.hashCode()
            result = 31 * result + superStates.hashCode()
            result = 31 * result + transitions.hashCode()
            return result
        }
    }

    class AnalysisError(private val id: ID) {
        enum class ID {
            NO_FSM,
            NO_INITIAL,
            INVALID_HEADER,
            EXTRA_HEADER_IGNORED,
            UNDEFINED_STATE,
            UNDEFINED_SUPER_STATE,
            UNUSED_STATE,
            DUPLICATE_TRANSITION,
            ABSTRACT_STATE_USED_AS_NEXT_STATE,
            INCONSISTENT_ABSTRACTION,
            STATE_ACTIONS_MULTIPLY_DEFINED,
            CONFLICTING_SUPERSTATES
        }

        private var extra: Any? = null

        constructor(id: ID, extra: Any) : this(id) {
            this.extra = extra
        }

        override fun toString() = "Semantic error: ${id.name}($extra)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AnalysisError) return false

            if (id != other.id) return false
            if (extra != other.extra) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + extra.hashCode()
            return result
        }
    }

    class SemanticTransition {
        lateinit var event: String
        var nextState: SemanticState? = null
        val actions = mutableListOf<String>()
    }
}