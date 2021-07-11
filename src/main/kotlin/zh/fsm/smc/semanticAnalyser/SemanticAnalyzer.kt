package zh.fsm.smc.semanticAnalyser

import zh.fsm.smc.parser.FsmSyntax
import zh.fsm.smc.parser.FsmSyntax.Header
import zh.fsm.smc.semanticAnalyser.SemanticStateMachine.*
import zh.fsm.smc.semanticAnalyser.SemanticStateMachine.AnalysisError.ID.*
import java.lang.IllegalStateException

class SemanticAnalyzer {
    private lateinit var semanticStateMachine: SemanticStateMachine
    private val fsmHeader = Header.NullHeader()
    private val actionsHeader = Header()
    private val initialHeader = Header()

    fun analyze(fsm: FsmSyntax): SemanticStateMachine {
        semanticStateMachine = SemanticStateMachine()
        analyzeHeaders(fsm)
        checkSemanticValidity(fsm)
        produceSemanticStateMachine(fsm)
        return semanticStateMachine
    }

    private fun analyzeHeaders(fsm: FsmSyntax) {
        setHeaders(fsm)
        checkMissingHeaders()
    }

    private fun setHeaders(fsm: FsmSyntax) = fsm.headers.forEach { header -> when {
        isNamed(header, "fsm") -> fsmHeader.setHeader(header)
        isNamed(header, "actions") -> actionsHeader.setHeader(header)
        isNamed(header, "initial") -> initialHeader.setHeader(header)
        else -> semanticStateMachine.addError(AnalysisError(INVALID_HEADER, header))
    } }

    private fun isNamed(header: Header, headerName: String) = header.name.equals(headerName, ignoreCase = true)

    private fun Header.setHeader(header: Header) {
        if (isNullHeader(this)) {
            name = header.name
            value = header.value
        }
        else
            semanticStateMachine.addError(AnalysisError(EXTRA_HEADER_IGNORED, header))
    }

    private fun checkMissingHeaders() {
        if (isNullHeader(fsmHeader))
            semanticStateMachine.addError(AnalysisError(NO_FSM))
        if (isNullHeader(initialHeader))
            semanticStateMachine.addError(AnalysisError(NO_INITIAL))
    }

    private fun isNullHeader(header: Header) = header.name == ""

    private fun checkSemanticValidity(fsm: FsmSyntax) {
        createStateEventAndActionList(fsm)
        checkUndefinedStates(fsm)
        checkForUnusedStates(fsm)
        checkForDuplicateTransitions(fsm)
        checkThatAbstractStatesAreNotTargets(fsm)
        checkForInconsistentAbstraction(fsm)
        checkForMultiplyDefinedStateActions(fsm)
    }

    private fun createStateEventAndActionList(fsm: FsmSyntax) {
        addStateNamesToStateList(fsm)
        addEntryAndExitActionsToActionList(fsm)
        addEventsToEventList(fsm)
        addTransitionActionsToActionList(fsm)
    }

    private fun addStateNamesToStateList(fsm: FsmSyntax) =
        fsm.logic
            .map { t -> SemanticState(t.state.name) }
            .forEach { state -> semanticStateMachine.states[state.name] = state }

    private fun addEntryAndExitActionsToActionList(fsm: FsmSyntax) =
        fsm.logic.forEach { t ->
            t.state.entryActions.forEach { semanticStateMachine.actions.add(it) }
            t.state.exitActions.forEach { semanticStateMachine.actions.add(it) }
        }

    private fun addEventsToEventList(fsm: FsmSyntax) =
        fsm.logic
            .flatMap { t -> t.subTransitions }
            .mapNotNull { st -> st.event }
            .forEach { event -> semanticStateMachine.events.add(event) }

    private fun addTransitionActionsToActionList(fsm: FsmSyntax) =
        fsm.logic
            .flatMap { t -> t.subTransitions }
            .flatMap { st -> st.actions }
            .forEach { action -> semanticStateMachine.actions.add(action) }

    private fun checkUndefinedStates(fsm: FsmSyntax) {
        fsm.logic.forEach { t ->
            t.state.superStates.forEach { superState -> checkUndefinedState(superState, UNDEFINED_SUPER_STATE) }
            t.subTransitions.forEach { st -> checkUndefinedState(st.nextState, UNDEFINED_STATE) }
        }
        if (initialHeader.value != "" && !semanticStateMachine.states.containsKey(initialHeader.value))
            semanticStateMachine.errors.add(AnalysisError(UNDEFINED_STATE, "initial: ${initialHeader.value}"))
    }

    private fun checkUndefinedState(referencedState: String?, errorCode: AnalysisError.ID) {
        if (referencedState != null && !semanticStateMachine.states.containsKey(referencedState)) {
            semanticStateMachine.errors.add(AnalysisError(errorCode, referencedState))
        }
    }

    private fun checkForUnusedStates(fsm: FsmSyntax) {
        findStatesDefinedButNotUsed(findUsedStates(fsm))
    }

    private fun findUsedStates(fsm: FsmSyntax): Set<String> {
        val usedStates = HashSet<String>()
        if (!isNullHeader(initialHeader))
            usedStates.add(initialHeader.value)
        usedStates.addAll(getSuperStates(fsm))
        usedStates.addAll(getNextStates(fsm))
        return usedStates
    }

    private fun getSuperStates(fsm: FsmSyntax): Set<String> =
        fsm.logic
            .flatMap { t -> t.state.superStates }
            .toHashSet()

    private fun getNextStates(fsm: FsmSyntax): Set<String> =
        fsm.logic.flatMap { t ->
            t.subTransitions.map { st -> st.nextState ?: t.state.name }
        }
            .toHashSet()

    private fun findStatesDefinedButNotUsed(usedStates: Set<String>) {
        semanticStateMachine.states.keys
            .filter { definedState -> !usedStates.contains(definedState) }
            .forEach { state -> semanticStateMachine.errors.add(AnalysisError(UNUSED_STATE, state)) }
    }

    private fun checkForDuplicateTransitions(fsm: FsmSyntax) {
        val transitionKeys = hashSetOf<String>()
        fsm.logic.forEach { t -> t.subTransitions.forEach { st ->
            val key = "${t.state.name}(${st.event})"
            if (transitionKeys.contains(key))
                semanticStateMachine.errors.add(AnalysisError(DUPLICATE_TRANSITION, key))
            else
                transitionKeys.add(key)
        } }
    }

    private fun checkThatAbstractStatesAreNotTargets(fsm: FsmSyntax) {
        val abstractStates = findAbstractStates(fsm)
        fsm.logic.forEach { t -> t.subTransitions.forEach { st ->
            if (abstractStates.contains(st.nextState))
                semanticStateMachine.errors.add(AnalysisError(
                    ABSTRACT_STATE_USED_AS_NEXT_STATE,
                    "${t.state.name}(${st.event})->${st.nextState}"
                ))
        } }
    }

    private fun findAbstractStates(fsm: FsmSyntax): Set<String> =
        fsm.logic
            .filter { t -> t.state.abstractState }
            .map { t -> t.state.name }
            .toHashSet()

    private fun checkForInconsistentAbstraction(fsm: FsmSyntax) {
        val abstractStates = findAbstractStates(fsm)
        fsm.logic
            .filter { t -> !t.state.abstractState && abstractStates.contains(t.state.name) }
            .map { t -> AnalysisError(INCONSISTENT_ABSTRACTION, t.state.name) }
            .forEach { err -> semanticStateMachine.warnings.add(err) }
    }

    private fun checkForMultiplyDefinedStateActions(fsm: FsmSyntax) {
        val firstActionsForState = hashMapOf<String, String>()
        fsm.logic
            .filter(this::specifiesStateAction)
            .forEach { t ->
                val actionsKey = makeActionsKey(t)
                if (firstActionsForState.containsKey(t.state.name)) {
                    if (firstActionsForState[t.state.name] != actionsKey)
                        semanticStateMachine.errors.add(AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, t.state.name))
                } else
                    firstActionsForState[t.state.name] = actionsKey
            }
    }

    private fun specifiesStateAction(t: FsmSyntax.Transition): Boolean =
        t.state.entryActions.isNotEmpty() || t.state.exitActions.isNotEmpty()

    private fun makeActionsKey(t: FsmSyntax.Transition): String {
        val actions = arrayListOf<String>()
        actions.addAll(t.state.entryActions)
        actions.addAll(t.state.exitActions)
        return commaList(actions)
    }

    private fun commaList(list: List<String>): String =
        if (list.isEmpty()) ""
        else list.joinToString(separator = ",")


    private fun produceSemanticStateMachine(fsm: FsmSyntax) {
        if (semanticStateMachine.errors.isEmpty()) {
            compileHeaders()
            fsm.logic
                .map { t -> t to compileState(t) }
                .forEach { (t, state) -> compileTransitions(t, state) }
        }
        SuperClassCrawler().checkSuperClassTransitions()
    }

    private fun compileHeaders() {
        semanticStateMachine.initialState = semanticStateMachine.states[initialHeader.value] as SemanticState
        semanticStateMachine.actionsClass = actionsHeader.value
        semanticStateMachine.fsmName = fsmHeader.value
    }

    private fun compileState(t: FsmSyntax.Transition): SemanticState {
        val state = semanticStateMachine.states[t.state.name]
            ?: throw IllegalStateException("It should already have the value for the deader ${t.state.name}")
        state.entryActions.addAll(t.state.entryActions)
        state.exitActions.addAll(t.state.exitActions)
        state.abstractState = state.abstractState || t.state.abstractState
        t.state.superStates.forEach { superStateName ->
            state.superStates.add(semanticStateMachine.states[superStateName])
        }
        return state
    }

    private fun compileTransitions(t: FsmSyntax.Transition, state: SemanticState) =
        t.subTransitions.forEach { st -> compileTransition(state, st)}

    private fun compileTransition(state: SemanticState, st: FsmSyntax.SubTransition) {
        with(SemanticTransition()) {
            event = st.event ?: "null"
            nextState = if (st.nextState == null) state else semanticStateMachine.states[st.nextState]
            actions.addAll(st.actions)
            state.transitions.add(this)
        }
    }

    private inner class SuperClassCrawler {
        inner class TransitionTuple(
            val currentState: String,
            val event: String,
            val nextState: String,
            val actions: MutableList<String>
        )
        {
            override fun equals(other: Any?): Boolean {
                return super.equals(other)
            }

            override fun hashCode(): Int {
                return super.hashCode()
            }
        }

        private var concreteState: SemanticState? = null
        private var transitionTuples = hashMapOf<String, TransitionTuple>()

        fun checkSuperClassTransitions() =
            semanticStateMachine.states.values
                .filter { !it.abstractState }
                .forEach { state ->
                    concreteState = state
                    checkTransitionsForState(concreteState as SemanticState)
                }

        private fun checkTransitionsForState(state: SemanticState) {
            state.superStates.forEach(this::checkTransitionsForState)
            checkStateForPreviouslyDefinedTransition(state)
        }

        private fun checkStateForPreviouslyDefinedTransition(state: SemanticState) =
            state.transitions.forEach { st -> checkTransitionFoPreviousDefinition(state, st)}

        private fun checkTransitionFoPreviousDefinition(state: SemanticState, st: SemanticTransition) {
            val thisTuple = TransitionTuple(state.name, st.event, st.nextState!!.name, st.actions)
            if (transitionTuples.containsKey(thisTuple.event))
                determineIfThePreviousDefinitionIsAnError(state, thisTuple)
            else
                transitionTuples[thisTuple.event] = thisTuple
        }

        private fun determineIfThePreviousDefinitionIsAnError(state: SemanticState, thisTuple: TransitionTuple) {
            val previousTuple = transitionTuples[thisTuple.event] as TransitionTuple
            if (!transitionsHaveSameOutcomes(thisTuple, previousTuple))
                checkForOverriddenTransition(state, thisTuple, previousTuple)
        }

        private fun transitionsHaveSameOutcomes(t1: TransitionTuple, t2: TransitionTuple): Boolean =
            t1.nextState == t2.nextState && t1.actions == t2.actions

        private fun checkForOverriddenTransition(
            state: SemanticState,
            thisTuple: TransitionTuple,
            previousTuple: TransitionTuple
        ) {
            val definingState = semanticStateMachine.states[previousTuple.currentState] as SemanticState
            if (!isSuperStateOf(definingState, state))
                semanticStateMachine.errors.add(
                    AnalysisError(CONFLICTING_SUPERSTATES, "${concreteState!!.name}|${thisTuple.event}"))
            else
                transitionTuples[thisTuple.event] = thisTuple
        }

        private fun isSuperStateOf(possibleSuperState: SemanticState, state: SemanticState): Boolean {
            if (state == possibleSuperState) return true
            for (superState in state.superStates)
                if (isSuperStateOf(possibleSuperState, superState))
                    return true
            return false
        }
    }
}
