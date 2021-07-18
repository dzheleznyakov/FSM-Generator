package zh.fsm.smc.generators.nestedSwitchCaseGenerator

import zh.fsm.smc.OptimizedStateMachine

class NSCGenerator {
    private lateinit var stateEnumNode: NSCNode.EnumNode
    private lateinit var eventEnumNode: NSCNode.EnumNode
    private lateinit var eventDelegatorsNode: NSCNode.EventDelegatorsNode
    private lateinit var statePropertyNode: NSCNode.StatePropertyNode
    private lateinit var handleEventNode: NSCNode.HandleEventNode
    private lateinit var stateSwitch: NSCNode.SwitchCaseNode

    fun generate(sm: OptimizedStateMachine): NSCNode {
        eventDelegatorsNode = NSCNode.EventDelegatorsNode(sm.events)
        statePropertyNode = NSCNode.StatePropertyNode(sm.header.initial)
        stateEnumNode = NSCNode.EnumNode("State", sm.states)
        eventEnumNode = NSCNode.EnumNode("Event", sm.events)
        stateSwitch = NSCNode.SwitchCaseNode("state")
        addStateCases(sm)
        handleEventNode = NSCNode.HandleEventNode(stateSwitch)
        return makeFsmNode(sm)
    }

    private fun addStateCases(sm: OptimizedStateMachine) = sm.transitions.forEach { t -> addStateCase(stateSwitch, t) }

    private fun addStateCase(stateSwitch: NSCNode.SwitchCaseNode, t: OptimizedStateMachine.Transition) {
        val stateCaseNode = NSCNode.CaseNode("State", t.currentState)
        addEventCases(stateCaseNode, t)
        stateSwitch.caseNodes.add(stateCaseNode)
    }

    private fun addEventCases(stateCaseNode: NSCNode.CaseNode, t: OptimizedStateMachine.Transition) {
        val eventSwitch = NSCNode.SwitchCaseNode("event")
        stateCaseNode.caseActionNode = eventSwitch
        t.subTransitions.forEach { st -> addEventCase(eventSwitch, st) }
        eventSwitch.caseNodes.add(NSCNode.DefaultCaseNode(t.currentState))
    }

    private fun addEventCase(eventSwitch: NSCNode.SwitchCaseNode, st: OptimizedStateMachine.SubTransition) {
        val eventCaseNode = NSCNode.CaseNode("Event", st.event)
        addActions(st, eventCaseNode)
        eventSwitch.caseNodes.add(eventCaseNode)
    }

    private fun addActions(st: OptimizedStateMachine.SubTransition, eventCaseNode: NSCNode.CaseNode) {
        val actions = NSCNode.CompositeNode()
        addSetStateNode(st.nextState, actions)
        st.actions.map(NSCNode::FunctionCallNode).forEach(actions::add)
        eventCaseNode.caseActionNode = actions
    }

    private fun addSetStateNode(stateName: String, actions: NSCNode.CompositeNode) {
        val enumeratorNode = NSCNode.EnumeratorNode("State", stateName)
        val setStateNode = NSCNode.FunctionCallNode("setState", enumeratorNode)
        actions.add(setStateNode)
    }

    private fun makeFsmNode(sm: OptimizedStateMachine): NSCNode.FSMClassNode = NSCNode.FSMClassNode().apply {
        className = sm.header.fsm
        actionsName = sm.header.actions
        stateEnum = stateEnumNode
        eventEnum = eventEnumNode
        delegators = eventDelegatorsNode
        stateProperty = statePropertyNode
        handleEvent = handleEventNode
        actions = sm.actions
    }

}
