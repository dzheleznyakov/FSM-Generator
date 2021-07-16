package zh.fsm.smc.optimizer

import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.semanticAnalyser.SemanticStateMachine

class Optimizer {
    private lateinit var semanticStateMachine: SemanticStateMachine
    private lateinit var optimizedStateMachine: OptimizedStateMachine

    public fun optimize(ast: SemanticStateMachine): OptimizedStateMachine {
        semanticStateMachine = ast
        optimizedStateMachine = OptimizedStateMachine()
        addHeader()
        addLists()
        addTransitions()
        return optimizedStateMachine
    }

    private fun addHeader(): Unit {
        optimizedStateMachine.header = OptimizedStateMachine.Header()
        with(optimizedStateMachine.header) {
            fsm = semanticStateMachine.fsmName
            initial = semanticStateMachine.initialState.name
            actions = semanticStateMachine.actionsClass
        }
    }

    private fun addLists() {
        addStates()
        addEvents()
        addActions()
    }

    private fun addStates() = semanticStateMachine.states.values
        .filter { !it.abstractState }
        .map { semanticState -> semanticState.name }
        .forEach { stateName -> optimizedStateMachine.states.add(stateName) }

    private fun addEvents() = optimizedStateMachine.events.addAll(semanticStateMachine.events)

    private fun addActions() = optimizedStateMachine.actions.addAll(semanticStateMachine.actions)

    private fun addTransitions() = semanticStateMachine.states.values
        .filter { !it.abstractState }
        .forEach { state -> StateOptimizer(state).addTransitionsForState() }

    inner class StateOptimizer(private val currentState: SemanticStateMachine.SemanticState) {
        private val eventsForThisState = hashSetOf<String>()

        internal fun addTransitionsForState() {
            val transition = OptimizedStateMachine.Transition()
            transition.currentState = currentState.name
            addSubTransitions(transition)
            optimizedStateMachine.transitions.add(transition)
        }

        private fun addSubTransitions(transition: OptimizedStateMachine.Transition) =
            makeRootFirstHierarchyOfStates()
                .forEach { stateInHierarchy -> addStateTransitions(transition, stateInHierarchy) }

        private fun makeRootFirstHierarchyOfStates(): List<SemanticStateMachine.SemanticState> {
            val hierarchy = arrayListOf<SemanticStateMachine.SemanticState>()
            addAllStatesInHierarchyLeafFirst(currentState, hierarchy)
            hierarchy.reverse()
            return hierarchy
        }

        private fun addStateTransitions(
            transition: OptimizedStateMachine.Transition,
            state: SemanticStateMachine.SemanticState
        ) = state.transitions
            .filter { semanticTransition -> eventExistsAndHasNotBeenOverridden(semanticTransition.event) }
            .forEach { semanticTransition -> addSubTransition(semanticTransition, transition) }

        private fun eventExistsAndHasNotBeenOverridden(event: String?) =
            event != "null" && !eventsForThisState.contains(event)

        private fun addSubTransition(
            semanticTransition: SemanticStateMachine.SemanticTransition,
            transition: OptimizedStateMachine.Transition
        ) {
            eventsForThisState.add(semanticTransition.event)
            val subTransition = OptimizedStateMachine.SubTransition()
            SubTransitionOptimizer(semanticTransition, subTransition).optimize()
            transition.subTransitions.add(subTransition)
        }

        inner class SubTransitionOptimizer(
            private val semanticTransition: SemanticStateMachine.SemanticTransition,
            private val subTransition: OptimizedStateMachine.SubTransition
        ) {
            fun optimize() {
                subTransition.event = semanticTransition.event
                subTransition.nextState = semanticTransition.nextState!!.name
                addExitActions(currentState)
                addEntryActions(semanticTransition.nextState!!)
                subTransition.actions.addAll(semanticTransition.actions)
            }

            private fun addExitActions(exitState: SemanticStateMachine.SemanticState) {
                val hierarchy = arrayListOf<SemanticStateMachine.SemanticState>()
                addAllStatesInHierarchyLeafFirst(exitState, hierarchy)
                hierarchy.reverse()
                hierarchy.forEach { superState -> subTransition.actions.addAll(superState.exitActions)}
            }

            private fun addEntryActions(entryState: SemanticStateMachine.SemanticState) {
                val hierarchy = arrayListOf<SemanticStateMachine.SemanticState>()
                addAllStatesInHierarchyLeafFirst(entryState, hierarchy)
                hierarchy.forEach { superState -> subTransition.actions.addAll(superState.entryActions)}
            }
        }
    }

    private fun addAllStatesInHierarchyLeafFirst(
        state: SemanticStateMachine.SemanticState,
        hierarchy: ArrayList<SemanticStateMachine.SemanticState>
    ) {
        state.superStates
            .filter { superState -> !hierarchy.contains(superState) }
            .forEach { superState -> addAllStatesInHierarchyLeafFirst(superState, hierarchy) }
        hierarchy.add(state)
    }
}
