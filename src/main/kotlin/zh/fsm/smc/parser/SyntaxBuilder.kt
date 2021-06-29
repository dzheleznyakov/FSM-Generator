package zh.fsm.smc.parser

import zh.fsm.smc.parser.FsmSyntax.*
import zh.fsm.smc.parser.FsmSyntax.SyntaxError.Type.*

class SyntaxBuilder : Builder {
    private val fsm = FsmSyntax()
    private lateinit var header: Header
    private lateinit var parsedName: String
    private lateinit var transition: Transition
    private lateinit var subTransition: SubTransition

    override fun newHeaderWithName() {
        header = Header()
        header.name = parsedName
    }

    override fun addHeaderWithValue() {
        header.value = parsedName
        fsm.headers.add(header)
    }

    override fun setStateName() {
        transition = Transition()
        fsm.logic.add(transition)
        transition.state = StateSpec()
        transition.state.name = parsedName
    }

    override fun done() {
        fsm.done = true
    }

    override fun setSuperStateName() {
        setStateName()
        transition.state.abstractState = true
    }

    override fun setEvent() {
        subTransition = SubTransition(parsedName)
    }

    override fun setNullEvent() {
        subTransition = SubTransition(null)
    }

    override fun setEntryAction() {
        transition.state.entryActions.add(parsedName)
    }

    override fun setExitAction() {
        transition.state.exitActions.add(parsedName)
    }

    override fun setStateBase() {
        transition.state.superStates.add(parsedName)
    }

    override fun setNextState() {
        subTransition.nextState = parsedName
    }

    override fun setName(name: String) {
        parsedName = name
    }

    override fun transitionWithAction() {
        subTransition.actions.add(parsedName)
        transition.subTransitions.add(subTransition)
    }

    override fun transitionWithNullAction() {
        transition.subTransitions.add(subTransition)
    }

    override fun transitionWithActions() {
        transition.subTransitions.add(subTransition)
    }

    override fun setNullNextState() {
        subTransition.nextState = null
    }

    override fun addAction() {
        subTransition.actions.add(parsedName)
    }

    override fun syntaxError(line: Int, pos: Int) {
        fsm.errors.add(SyntaxError(SYNTAX, "", line, pos))
    }

    override fun headerError(state: ParserState, event: ParserEvent, line: Int, pos: Int) {
        fsm.errors.add(SyntaxError(HEADER, "$state|$event", line, pos))
    }

    override fun stateStecError(state: ParserState, event: ParserEvent, line: Int, pos: Int) {
        fsm.errors.add(SyntaxError(STATE, "$state|$event", line, pos))
    }

    override fun transitionError(state: ParserState, event: ParserEvent, line: Int, pos: Int) {
        fsm.errors.add(SyntaxError(TRANSITION, "$state|$event", line, pos))
    }

    override fun transitionGroupError(state: ParserState, event: ParserEvent, line: Int, pos: Int) {
        fsm.errors.add(SyntaxError(TRANSITION_GROUP, "$state|$event", line, pos))
    }

    override fun endError(state: ParserState, event: ParserEvent, line: Int, pos: Int) {
        fsm.errors.add(SyntaxError(END, "$state|$event", line, pos))
    }

    fun getFsm(): FsmSyntax {
        return fsm;
    }
}