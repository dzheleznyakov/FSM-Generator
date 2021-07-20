package zh.fsm.smc.implementers

import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNode
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor

class CNestedSwitchCaseImplementer(flags: Map<String, String>) : NSCNodeVisitor {
    private val _errors = arrayListOf<Error>()
    val errors: List<Error>
        get() = _errors

    private var _fsmHeader = StringBuilder()
    val fsmHeader: String
        get() = _fsmHeader.toString()

    private var _fsmImplementation = StringBuilder()
    val fsmImplementation: String
        get() = _fsmImplementation.toString()

    private var fsmName = ""
    private var actionsName = ""

    override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) {
        _fsmImplementation += "switch (${switchCaseNode.variableName}) {\n"
        switchCaseNode.generateCases(this)
        _fsmImplementation += "}\n"
    }

    override fun visit(caseNode: NSCNode.CaseNode) {
        _fsmImplementation += "case ${caseNode.caseName}:\n"
        caseNode.caseActionNode.accept(this)
        _fsmImplementation += "break;\n\n"
    }

    override fun visit(functionCallNode: NSCNode.FunctionCallNode) {
        with(functionCallNode) {
            _fsmImplementation += "${functionName}(fsm"
            if (argument != null) {
                _fsmImplementation += ", "
                argument.accept(this@CNestedSwitchCaseImplementer)
            }
            _fsmImplementation += ");\n"
        }

    }

    override fun visit(enumNode: NSCNode.EnumNode) {
        with(enumNode) {
            val enumeratorsStr = enumerators.joinToString(separator = ",")
            _fsmImplementation += "enum $name {$enumeratorsStr};\n"
        }
    }

    override fun visit(statePropertyNode: NSCNode.StatePropertyNode) {
        _fsmImplementation += "struct $fsmName *make_$fsmName(struct $actionsName* actions) {\n"
        _fsmImplementation += "\tstruct $fsmName *fsm = malloc(sizeof(struct $fsmName));\n"
        _fsmImplementation += "\tfsm->actions = actions;\n"
        _fsmImplementation += "\tfsm->state = ${statePropertyNode.initialState};\n"
        _fsmImplementation += "\treturn fsm;\n"
        _fsmImplementation += "}\n\n"

        _fsmImplementation += "static void setState(struct $fsmName *fsm, enum State state) {\n"
        _fsmImplementation += "\tfsm->state = state;\n"
        _fsmImplementation += "}\n\n"
    }

    override fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode) {
        eventDelegatorsNode.events.forEach { event ->
            _fsmHeader += "void ${fsmName}_$event(struct $fsmName*);\n"

            _fsmImplementation += "void ${fsmName}_$event(struct $fsmName* fsm) {\n"
            _fsmImplementation += "\tprocessEvent(fsm->state, $event, fsm, \"$event\");\n"
            _fsmImplementation += "}\n"
        }
    }

    override fun visit(fsmClassNode: NSCNode.FSMClassNode) {
        if (fsmClassNode.actionsName == "") {
            _errors.add(Error.NO_ACTION)
            return
        }

        actionsName = fsmClassNode.actionsName
        fsmName = fsmClassNode.className

        with(fsmClassNode) {
            _fsmImplementation += "#include <stdlib.h>\n"
            _fsmImplementation += "#include \"$actionsName.h\"\n"
            _fsmImplementation += "#include \"$fsmName.h\"\n\n"
            val visitor = this@CNestedSwitchCaseImplementer
            this.eventEnum.accept(visitor)
            this.stateEnum.accept(visitor)

            _fsmImplementation += "\n"
            _fsmImplementation += "struct $fsmName {\n"
            _fsmImplementation += "\tenum State state;\n"
            _fsmImplementation += "\tstruct $actionsName *actions;\n"
            _fsmImplementation += "};\n\n"

            stateProperty.accept(visitor)

            actions.forEach { action ->
                _fsmImplementation += "static void $action(struct $fsmName *fsm) {\n"
                _fsmImplementation += "\tfsm->actions->$action();\n"
                _fsmImplementation += "}\n\n"
            }
            handleEvent.accept(visitor)

            val includeGuard = fsmName.uppercase()
            _fsmHeader += "#ifndef ${includeGuard}_H\n#define ${includeGuard}_H\n\n"
            _fsmHeader += "struct $actionsName;\n"
            _fsmHeader += "struct $fsmName;\n"
            _fsmHeader += "struct $fsmName *make_$fsmName(struct $actionsName*);\n"
            delegators.accept(visitor)
            _fsmHeader += "#endif\n"
        }
    }

    override fun visit(handleEventNode: NSCNode.HandleEventNode) {
        _fsmImplementation +=
            "static void processEvent(enum State state, enum Event event, struct $fsmName *fsm, char *event_name) {\n"
        handleEventNode.switchCase.accept(this)
        _fsmImplementation += "}\n\n"
    }

    override fun visit(enumeratorNode: NSCNode.EnumeratorNode) {
        _fsmImplementation += enumeratorNode.enumerator
    }

    override fun visit(defaultCaseNode: NSCNode.DefaultCaseNode) {
        _fsmImplementation += "default:\n"
        _fsmImplementation += "(fsm->actions->unexpected_transition)(\"${defaultCaseNode.state}\", event_name);\n"
        _fsmImplementation += "break;\n"
    }

    enum class Error {
        NO_ACTION
    }
}