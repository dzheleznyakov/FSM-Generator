package zh.fsm.smc.generators.nestedSwitchCaseGenerator

interface NSCNodeVisitor {
    fun visit(switchCaseNode: NSCNode.SwitchCaseNode)
    fun visit(caseNode: NSCNode.CaseNode)
    fun visit(functionCallNode: NSCNode.FunctionCallNode)
    fun visit(enumNode: NSCNode.EnumNode)
    fun visit(statePropertyNode: NSCNode.StatePropertyNode)
    fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode)
    fun visit(fsmClassNode: NSCNode.FSMClassNode)
    fun visit(handleEventNode: NSCNode.HandleEventNode)
    fun visit(enumeratorNode: NSCNode.EnumeratorNode)
    fun visit(defaultCaseNode: NSCNode.DefaultCaseNode)
}