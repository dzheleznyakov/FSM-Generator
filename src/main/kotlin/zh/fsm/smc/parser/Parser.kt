package zh.fsm.smc.parser

/*
<FSM> ::= <header>* <logic>
<header> ::= "Actions:" <name> | "FSM:" <name> | "Initial:" <name>

<logic> ::= "{" <transition>* "}"

<transition> ::= <state-spec> <subtransition>
             |   <state-spec> "{" <subtransition>* "}"

<subtransition>   ::= <event-spec> <next-state> <action-spec>
<action-spec>     ::= <action> | "{" <action>* "}" | "-"
<state-spec>      ::= <state> <state-modifiers>
<state-modifiers> ::= "" | <state-modifier> | <state-modifier> <state-modifiers>
<state-modifier>  ::= ":" <state>
                  |   "<" <action-spec>
                  |   ">" <action-spec>

<next-state> ::= <state> | "-"
<event-spec> :: <event> | "-"
<action> ::= <name>
<state> ::= <name>
<event> ::= <name>
*/

import zh.fsm.smc.lexer.TokenCollector
import zh.fsm.smc.parser.ParserState.*
import zh.fsm.smc.parser.ParserEvent.*

class Parser(private val builder: Builder) : TokenCollector {
    private var state: ParserState = HEADER

    override fun openBrace(line: Int, pos: Int) = handleEvent(OPEN_BRACE, line, pos)
    override fun closedBrace(line: Int, pos: Int) = handleEvent(CLOSED_BRACE, line, pos)
    override fun openParen(line: Int, pos: Int) = handleEvent(OPEN_PAREN, line, pos)
    override fun closedParen(line: Int, pos: Int) = handleEvent(CLOSED_PAREN, line, pos)
    override fun openAngle(line: Int, pos: Int) = handleEvent(OPEN_ANGLE, line, pos)
    override fun closedAngle(line: Int, pos: Int) = handleEvent(CLOSED_ANGLE, line, pos)
    override fun dash(line: Int, pos: Int) = handleEvent(DASH, line, pos)
    override fun colon(line: Int, pos: Int) = handleEvent(COLON, line, pos)
    override fun name(name: String, line: Int, pos: Int) {
        builder.setName(name)
        handleEvent(NAME, line, pos)
    }
    override fun error(line: Int, pos: Int) {
        builder.syntaxError(line, pos)
    }

    class Transition(
        val currentState: ParserState,
        val event: ParserEvent,
        val newState: ParserState,
        val action: ((Builder) -> Unit)
    )

    private val nothing: (Builder) -> Unit = {}
    internal val transitions = arrayOf(
        Transition(HEADER, NAME, HEADER_COLON, Builder::newHeaderWithName),
        Transition(HEADER, OPEN_BRACE, STATE_SPEC, nothing),
        Transition(HEADER_COLON, COLON, HEADER_VALUE, nothing),
        Transition(HEADER_VALUE, NAME, HEADER, Builder::addHeaderWithValue),
        Transition(STATE_SPEC, OPEN_PAREN, SUPER_STATE_NAME, nothing),
        Transition(STATE_SPEC, NAME, STATE_MODIFIER, Builder::setStateName),
        Transition(STATE_SPEC, CLOSED_BRACE, END, Builder::done),
        Transition(SUPER_STATE_NAME, NAME, SUPER_STATE_CLOSE, Builder::setSuperStateName),
        Transition(SUPER_STATE_CLOSE, CLOSED_PAREN, STATE_MODIFIER, nothing),
        Transition(STATE_MODIFIER, OPEN_ANGLE, ENTRY_ACTION, nothing),
        Transition(STATE_MODIFIER, CLOSED_ANGLE, EXIT_ACTION, nothing),
        Transition(STATE_MODIFIER, COLON, STATE_BASE, nothing),
        Transition(STATE_MODIFIER, NAME, SINGLE_EVENT, Builder::setEvent),
        Transition(STATE_MODIFIER, DASH, SINGLE_EVENT, Builder::setNullEvent),
        Transition(STATE_MODIFIER, OPEN_BRACE, SUBTRANSITION_GROUP, nothing),
        Transition(ENTRY_ACTION, NAME, STATE_MODIFIER, Builder::setEntryAction),
        Transition(ENTRY_ACTION, OPEN_BRACE, MULTIPLE_ENTRY_ACTIONS, nothing),
        Transition(MULTIPLE_ENTRY_ACTIONS, NAME, MULTIPLE_ENTRY_ACTIONS, Builder::setEntryAction),
        Transition(MULTIPLE_ENTRY_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, nothing),
        Transition(EXIT_ACTION, NAME, STATE_MODIFIER, Builder::setExitAction),
        Transition(EXIT_ACTION, OPEN_BRACE, MULTIPLE_EXIT_ACTIONS, nothing),
        Transition(MULTIPLE_EXIT_ACTIONS, NAME, MULTIPLE_EXIT_ACTIONS, Builder::setExitAction),
        Transition(MULTIPLE_EXIT_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, nothing),
        Transition(STATE_BASE, NAME, STATE_MODIFIER, Builder::setStateBase),
        Transition(SINGLE_EVENT, NAME, SINGLE_NEXT_STATE, Builder::setNextState),
        Transition(SINGLE_EVENT, DASH, SINGLE_NEXT_STATE, Builder::setNullNextState),
        Transition(SINGLE_NEXT_STATE, NAME, STATE_SPEC, Builder::transitionWithAction),
        Transition(SINGLE_NEXT_STATE, DASH, STATE_SPEC, Builder::transitionWithNullAction),
        Transition(SINGLE_NEXT_STATE, OPEN_BRACE, SINGLE_ACTION_GROUP, nothing),
        Transition(SINGLE_ACTION_GROUP, NAME, SINGLE_ACTION_GROUP_NAME, Builder::addAction),
        Transition(SINGLE_ACTION_GROUP, CLOSED_BRACE, STATE_SPEC, Builder::transitionWithNullAction),
        Transition(SINGLE_ACTION_GROUP_NAME, NAME, SINGLE_ACTION_GROUP_NAME, Builder::addAction),
        Transition(SINGLE_ACTION_GROUP_NAME, CLOSED_BRACE, STATE_SPEC, Builder::transitionWithActions),
        Transition(SUBTRANSITION_GROUP, CLOSED_BRACE, STATE_SPEC, nothing),
        Transition(SUBTRANSITION_GROUP, NAME, GROUP_EVENT, Builder::setEvent),
        Transition(SUBTRANSITION_GROUP, DASH, GROUP_EVENT, Builder::setNullEvent),
        Transition(GROUP_EVENT, NAME, GROUP_NEXT_STATE, Builder::setNextState),
        Transition(GROUP_EVENT, DASH, GROUP_NEXT_STATE, Builder::setNullNextState),
        Transition(GROUP_NEXT_STATE, NAME, SUBTRANSITION_GROUP, Builder::transitionWithAction),
        Transition(GROUP_NEXT_STATE, DASH, SUBTRANSITION_GROUP, Builder::transitionWithNullAction),
        Transition(GROUP_NEXT_STATE, OPEN_BRACE, GROUP_ACTION_GROUP, nothing),
        Transition(GROUP_ACTION_GROUP, NAME, GROUP_ACTION_GROUP_NAME, Builder::addAction),
        Transition(GROUP_ACTION_GROUP, CLOSED_BRACE, SUBTRANSITION_GROUP, Builder::transitionWithNullAction),
        Transition(GROUP_ACTION_GROUP_NAME, NAME, GROUP_ACTION_GROUP_NAME, Builder::addAction),
        Transition(GROUP_ACTION_GROUP_NAME, CLOSED_BRACE, SUBTRANSITION_GROUP, Builder::transitionWithActions),
        Transition(END, EOF, END, nothing)
    )

    fun handleEvent(event: ParserEvent, line: Int, pos: Int) {
        for (t: Transition in transitions)
            if (t.currentState == state && t.event == event) {
                state = t.newState
                t.action(builder)
                return
            }
        handleEventError(event, line, pos)
    }

    private fun handleEventError(event: ParserEvent, line: Int, pos: Int) = when(state) {
        HEADER,
        HEADER_COLON,
        HEADER_VALUE
            -> builder.headerError(state, event, line, pos)
        STATE_SPEC,
        SUPER_STATE_NAME,
        SUPER_STATE_CLOSE,
        STATE_MODIFIER,
        EXIT_ACTION,
        ENTRY_ACTION,
        STATE_BASE
            -> builder.stateStecError(state, event, line, pos)
        SINGLE_EVENT,
        SINGLE_NEXT_STATE,
        SINGLE_ACTION_GROUP,
        SINGLE_ACTION_GROUP_NAME
            -> builder.transitionError(state, event, line, pos)
        SUBTRANSITION_GROUP,
        GROUP_EVENT,
        GROUP_NEXT_STATE,
        GROUP_ACTION_GROUP,
        GROUP_ACTION_GROUP_NAME
            -> builder.transitionGroupError(state, event, line, pos)
        END
            -> builder.endError(state, event, line, pos)
        else
            -> throw IllegalStateException("Error while in state [$state] is not expected")
    }
}