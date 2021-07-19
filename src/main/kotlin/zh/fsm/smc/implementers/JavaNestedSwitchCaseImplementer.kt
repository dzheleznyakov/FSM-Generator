package zh.fsm.smc.implementers

import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNode
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor

class JavaNestedSwitchCaseImplementer(private val flags: Map<String, String>) : NSCNodeVisitor {
    private val _output = StringBuilder()
    val output: String
        get() = _output.toString()

    private val javaPackage: String = flags["package"] ?: ""

    override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) {
        _output += "switch(${switchCaseNode.variableName}) {\n"
        switchCaseNode.generateCases(this)
        _output += "}\n"
    }

    override fun visit(caseNode: NSCNode.CaseNode) {
        _output += "case ${caseNode.caseName}:\n"
        caseNode.caseActionNode.accept(this)
        _output += "break;\n"
    }

    override fun visit(functionCallNode: NSCNode.FunctionCallNode) {
        _output += "${functionCallNode.functionName}("
        functionCallNode.argument?.accept(this)
        _output += ");\n"
    }

    override fun visit(enumNode: NSCNode.EnumNode) {
        val enumeratorsStr = enumNode.enumerators.joinToString(separator = ",")
        _output += "private enum ${enumNode.name} {$enumeratorsStr}\n"
    }

    override fun visit(statePropertyNode: NSCNode.StatePropertyNode) {
        with (statePropertyNode) {
            _output +="private State state = State.$initialState;\n" +
                    "private void setState(State s) {state = s;}\n"
        }
    }

    override fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode) {
        eventDelegatorsNode.events.forEach { event ->
            _output += "public void $event() {handleEvent(Event.$event);}\n"
        }
    }

    override fun visit(fsmClassNode: NSCNode.FSMClassNode) {
        if (javaPackage != "")
            _output += "package $javaPackage;\n"

        val actionsName = fsmClassNode.actionsName
        _output += "public abstract class ${fsmClassNode.className}"
        if (actionsName != "")
            _output += " implements $actionsName"
        _output += " {\n"

        _output += "public abstract void unhandledTransition(String state, String event);\n"
        with(fsmClassNode) {
            val visitor = this@JavaNestedSwitchCaseImplementer
            stateEnum.accept(visitor)
            eventEnum.accept(visitor)
            stateProperty.accept(visitor)
            delegators.accept(visitor)
            handleEvent.accept(visitor)
        }

        if (actionsName == "")
            fsmClassNode.actions.forEach { action ->
                _output += "protected abstract void $action();\n"
            }
        _output += "}\n"
    }

    override fun visit(handleEventNode: NSCNode.HandleEventNode) {
        _output += "private void handleEvent(Event event) {\n"
        handleEventNode.switchCase.accept(this)
        _output += "}\n"
    }

    override fun visit(enumeratorNode: NSCNode.EnumeratorNode) {
        with(enumeratorNode) {
            _output += "$enumeration.$enumerator"
        }
    }

    override fun visit(defaultCaseNode: NSCNode.DefaultCaseNode) {
        _output += "default: unhandledTransition(state.name(), event.name()); break;\n"
    }
}