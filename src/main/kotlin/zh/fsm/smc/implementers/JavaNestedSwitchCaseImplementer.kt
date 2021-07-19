package zh.fsm.smc.implementers

import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNode
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor

class JavaNestedSwitchCaseImplementer(private val flags: Map<String, String>) : NSCNodeVisitor {
    private val _output = StringBuilder()
    val output: String
        get() = _output.toString()
    private val javaPackage: String = flags["package"] ?: ""

    override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) {
        _output.append("switch(${switchCaseNode.variableName}) {\n")
        switchCaseNode.generateCases(this)
        _output.append("}\n")
    }

    override fun visit(caseNode: NSCNode.CaseNode) {
        _output.append("case ${caseNode.caseName}:\n")
        caseNode.caseActionNode.accept(this)
        _output.append("break;\n")
    }

    override fun visit(functionCallNode: NSCNode.FunctionCallNode) {
        _output.append("${functionCallNode.functionName}(")
        functionCallNode.argument?.accept(this)
        _output.append(");\n")
    }

    override fun visit(enumNode: NSCNode.EnumNode) {
        _output.append("private enum ")
            .append(enumNode.name)
            .append(" {")
            .append(enumNode.enumerators.joinToString(separator = ","))
            .append("}\n")
    }

    override fun visit(statePropertyNode: NSCNode.StatePropertyNode) {
        _output.append("private State state = State.")
            .append(statePropertyNode.initialState)
            .append(";\n")
            .append("private void setState(State s) {state = s;}\n")
    }

    override fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode) {
        eventDelegatorsNode.events.forEach { event ->
            _output.append("public void ")
                .append(event)
                .append("() {handleEvent(Event.")
                .append(event)
                .append(");}\n")
        }
    }

    override fun visit(fsmClassNode: NSCNode.FSMClassNode) {
        if (javaPackage != "")
            _output.append("package ").append(javaPackage).append(";\n")

        val actionsName = fsmClassNode.actionsName
        if (actionsName == "") _output.append("public abstract class ").append(fsmClassNode.className).append(" {\n")
        else _output.append("public abstract class ").append(fsmClassNode.className)
            .append(" implements ").append(actionsName).append(" {\n");

        _output.append("public abstract void unhandledTransition(String state, String event);\n")
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
                _output.append("protected abstract void $action();\n")
            }
        _output.append("}\n")
    }

    override fun visit(handleEventNode: NSCNode.HandleEventNode) {
        _output.append("private void handleEvent(Event event) {\n")
        handleEventNode.switchCase.accept(this)
        _output.append("}\n")
    }

    override fun visit(enumeratorNode: NSCNode.EnumeratorNode) {
        with(enumeratorNode) {
            _output.append("$enumeration.$enumerator")
        }
    }

    override fun visit(defaultCaseNode: NSCNode.DefaultCaseNode) {
        _output.append("default: unhandledTransition(state.name(), event.name()); break;\n")
    }
}