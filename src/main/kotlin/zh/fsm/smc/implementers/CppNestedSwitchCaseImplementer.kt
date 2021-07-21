package zh.fsm.smc.implementers

import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNode
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor

class CppNestedSwitchCaseImplementer(flags: Map<String, String>) : NSCNodeVisitor {
    private val _output = StringBuilder()
    val output: String
        get() = _output.toString()

    private var fsmName = ""
    private var actionsName = ""

    private val _errors = arrayListOf<Error>()
    val errors: List<Error>
        get() = _errors

    override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) = with(switchCaseNode) {
        _output += "switch ($variableName) {\n"
        val visitor = this@CppNestedSwitchCaseImplementer
        generateCases(visitor)
        _output += "}\n"
    }

    override fun visit(caseNode: NSCNode.CaseNode) = with(caseNode) {
        _output += "case ${switchName}_$caseName:\n"
        caseNode.caseActionNode.accept(this@CppNestedSwitchCaseImplementer)
        _output += "break;\n\n"
    }

    override fun visit(functionCallNode: NSCNode.FunctionCallNode) = with(functionCallNode) {
        _output += "$functionName("
        argument?.accept(this@CppNestedSwitchCaseImplementer)
        _output += ");\n"
    }

    override fun visit(enumNode: NSCNode.EnumNode) = with(enumNode) {
        val enumList = enumerators.map { "${name}_$it" }.joinToString(separator = ",")
        _output += "\tenum $name {$enumList};\n"
    }

    override fun visit(statePropertyNode: NSCNode.StatePropertyNode) = with(statePropertyNode) {
        _output += "State_$initialState"
    }

    override fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode) = with(eventDelegatorsNode) {
        events.forEach { event -> _output += "\tvoid $event() {processEvent(Event_$event, \"$event\");}\n" }
    }

    override fun visit(fsmClassNode: NSCNode.FSMClassNode) {
        if (fsmClassNode.actionsName == "") {
            _errors.add(Error.NO_ACTIONS)
            return
        }

        fsmName = fsmClassNode.className
        val includeGuard = fsmName.uppercase()
        _output += "#ifndef ${includeGuard}_H\n#define ${includeGuard}_H\n\n"

        actionsName = fsmClassNode.actionsName
        _output += "#include \"$actionsName.h\"\n"

        with(fsmClassNode) {
            val visitor = this@CppNestedSwitchCaseImplementer

            _output += "\n"
            _output += "class $fsmName : public $actionsName {\n"
            _output += "public:\n"
            _output += "\t$fsmName()\n\t: state("
            stateProperty.accept(visitor)
            _output += ")\n\t{}\n\n"

            delegators.accept(visitor)
            _output += "\nprivate:\n"
            stateEnum.accept(visitor)
            _output += "\tState state;\n"
            _output += "\tvoid setState(State s) {state=s;}\n"
            eventEnum.accept(visitor)
            handleEvent.accept(visitor)

            _output += "};\n\n"
            _output += "#endif\n"
        }
    }

    override fun visit(handleEventNode: NSCNode.HandleEventNode) = with(handleEventNode) {
        _output += "\tvoid processEvent(Event event, const char* eventName) {\n"
        switchCase.accept(this@CppNestedSwitchCaseImplementer)
        _output += "}\n\n"
    }

    override fun visit(enumeratorNode: NSCNode.EnumeratorNode) = with(enumeratorNode) {
        _output += "${enumeration}_$enumerator"
    }

    override fun visit(defaultCaseNode: NSCNode.DefaultCaseNode) = with(defaultCaseNode) {
        _output += "default:\n"
        _output += "unexpected_transition(\"$state\", eventName);\n"
        _output += "break;\n"
    }

    enum class Error {NO_ACTIONS}
}