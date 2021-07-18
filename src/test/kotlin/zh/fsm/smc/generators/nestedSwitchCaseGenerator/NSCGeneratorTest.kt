package zh.fsm.smc.generators.nestedSwitchCaseGenerator

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.lexer.FsmLexer
import zh.fsm.smc.lexer.Lexer
import zh.fsm.smc.optimizer.Optimizer
import zh.fsm.smc.parser.Parser
import zh.fsm.smc.parser.ParserEvent
import zh.fsm.smc.parser.SyntaxBuilder
import zh.fsm.smc.semanticAnalyser.SemanticAnalyzer

@DisplayName("Test NestedSwitchCase Generator")
internal class NSCGeneratorTest {
    private val builder = SyntaxBuilder()
    private val parser = Parser(builder)
    private val lexer: Lexer = FsmLexer(parser)
    private val analyzer = SemanticAnalyzer()
    private val optimizer = Optimizer()
    private val stdHead = "Initial: I FSM:f Actions:acts"
    private val generator = NSCGenerator()
    private lateinit var implementer: NSCNodeVisitor
    private var output = ""

    private fun produceStateMachine(fsmSyntax: String): OptimizedStateMachine {
        lexer.lex(fsmSyntax)
        parser.handleEvent(ParserEvent.EOF, -1, -1)
        val ast = analyzer.analyze(builder.getFsm())
        return optimizer.optimize(ast)
    }

    private fun headerAndSttToSm(header: String, stt: String): OptimizedStateMachine = produceStateMachine("$header $stt")

    private fun assertGenerated(stt: String, switchCase: String): Unit {
        val sm = headerAndSttToSm(stdHead, stt)
        generator.generate(sm).accept(implementer)
        assertThat(output, equalTo(switchCase))
    }

    open inner class EmptyVisitor : NSCNodeVisitor {
        override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) {}
        override fun visit(caseNode: NSCNode.CaseNode) {}
        override fun visit(functionCallNode: NSCNode.FunctionCallNode) {}
        override fun visit(enumNode: NSCNode.EnumNode) {}
        override fun visit(statePropertyNode: NSCNode.StatePropertyNode) {}
        override fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode) {}

        override fun visit(handleEventNode: NSCNode.HandleEventNode) = handleEventNode.switchCase.accept(this)

        override fun visit(enumeratorNode: NSCNode.EnumeratorNode) = with(enumeratorNode) {
            output += "$enumeration.$enumerator"
        }

        override fun visit(defaultCaseNode: NSCNode.DefaultCaseNode) {
            output += " default(${defaultCaseNode.state});"
        }

        override fun visit(fsmClassNode: NSCNode.FSMClassNode) {
            fsmClassNode.delegators.accept(this)
            fsmClassNode.stateEnum.accept(this)
            fsmClassNode.eventEnum.accept(this)
            fsmClassNode.stateProperty.accept(this)
            fsmClassNode.handleEvent.accept(this)
        }
    }

    @Nested
    inner class SwitchCaseTests {
        @BeforeEach
        internal fun setUp() {
            implementer = SwitchCaseTestVisitor()
        }

        inner class SwitchCaseTestVisitor : EmptyVisitor() {
            override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) {
                output += "s ${switchCaseNode.variableName} {"
                switchCaseNode.generateCases(this)
                output += "}"
            }

            override fun visit(caseNode: NSCNode.CaseNode) {
                output += "case ${caseNode.caseName} {"
                caseNode.caseActionNode.accept(this)
                output += "}"
            }

            override fun visit(functionCallNode: NSCNode.FunctionCallNode) {
                output += "${functionCallNode.functionName}("
                functionCallNode.argument?.accept(this)
                output += ") "
            }
        }

        @Test
        @DisplayName("Test FSM with just one transition")
        internal fun oneTransition() = assertGenerated(
            "{I e I a}",
            "s state {case I {s event {case e {setState(State.I) a() } default(I);}}}"
        )

        @Test
        @DisplayName("Test FSM with two transitions")
        internal fun twoTransitions() = assertGenerated(
            "{I e1 S a1 S e2 I a2}",
            "" +
                    "s state {" +
                    "case I {s event {case e1 {setState(State.S) a1() } default(I);}}" +
                    "case S {s event {case e2 {setState(State.I) a2() } default(S);}}" +
                    "}"
        )

        @Test
        @DisplayName("Test a full two state FSM")
        internal fun twoStatesTwoEventsFourActions() = assertGenerated(
            "" +
                    "{" +
                    "  I e1 S a1" +
                    "  I e2 - a2" +
                    "  S e1 I a3" +
                    "  S e2 - a4" +
                    "}",
            "" +
                    "s state {" +
                    "case I {s event {case e1 {setState(State.S) a1() }" +
                    "case e2 {setState(State.I) a2() } default(I);}}" +
                    "case S {s event {case e1 {setState(State.I) a3() }" +
                    "case e2 {setState(State.S) a4() } default(S);}}" +
                    "}"
        )
    }

    @Nested
    inner class EnumTests {
        @BeforeEach
        internal fun setUp() {
            implementer = EnumTestVisitor()
        }

        inner class EnumTestVisitor : EmptyVisitor() {
            override fun visit(enumNode: NSCNode.EnumNode) = with(enumNode) {
                output += "enum $name $enumerators "
            }
        }

        @Test
        @DisplayName("Test generation of state and event enums")
        internal fun statesAndEvents() = assertGenerated(
            "" +
                    "{" +
                    "  I e1 S a1" +
                    "  I e2 - a2" +
                    "  S e1 I a3" +
                    "  S e2 - a4" +
                    "}",
            "enum State [I, S] enum Event [e1, e2] "
        )
    }

    @Nested
    inner class StatePropertyTest {
        @BeforeEach
        internal fun setUp() {
            implementer = StatePropertyTestVisitor()
        }

        inner class StatePropertyTestVisitor : EmptyVisitor() {
            override fun visit(statePropertyNode: NSCNode.StatePropertyNode) = with(statePropertyNode) {
                output += "state property = $initialState"
            }
        }

        @Test
        @DisplayName("Test that state property is generated")
        internal fun statePropertyIsCreated() = assertGenerated(
            "{I e I a}",
            "state property = I"
        )
    }

    @Nested
    inner class EventDelegatorsTests {
        @BeforeEach
        internal fun setUp() {
            implementer = EventDelegatorsTestVisitor()
        }

        inner class EventDelegatorsTestVisitor : EmptyVisitor() {
            override fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode) {
                output += "delegators ${eventDelegatorsNode.events}"
            }
        }

        @Test
        @DisplayName("Test that event delegators are generated")
        internal fun eventDelegatorsAreGenerated() = assertGenerated(
            "" +
                    "{" +
                    "  I e1 S a1 " +
                    "  I e2 - a2" +
                    "  S e1 I a3" +
                    "  S e2 - a4" +
                    "}",
            "delegators [e1, e2]"
        )
    }

    @Nested
    inner class HandleEventTests {
        @BeforeEach
        internal fun setUp() {
            implementer = HandleEventTestVisitor()
        }

        inner class HandleEventTestVisitor : EmptyVisitor() {
            override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) {
                output += "s"
            }

            override fun visit(handleEventNode: NSCNode.HandleEventNode) {
                output += "he("
                handleEventNode.switchCase.accept(this)
                output += ")"
            }
        }

        @Test
        @DisplayName("Test that handle event is generated")
        internal fun handleEventIsGenerated() = assertGenerated(
            "{I e I a}",
            "he(s)"
        )
    }

    @Nested
    inner class FsmClassTests {
        @BeforeEach
        internal fun setUp() {
            implementer = FsmClassTestVisitor()
        }

        inner class FsmClassTestVisitor : EmptyVisitor() {
            override fun visit(switchCaseNode: NSCNode.SwitchCaseNode) {
                output += "sc"
            }

            override fun visit(enumNode: NSCNode.EnumNode) {
                output += "e "
            }

            override fun visit(statePropertyNode: NSCNode.StatePropertyNode) {
                output += "p "
            }

            override fun visit(eventDelegatorsNode: NSCNode.EventDelegatorsNode) {
                output += "d "
            }

            override fun visit(handleEventNode: NSCNode.HandleEventNode) {
                output += "he "
            }

            override fun visit(fsmClassNode: NSCNode.FSMClassNode) = with(fsmClassNode) {
                output += "class $className:$actionsName {"
                val visitor = this@FsmClassTestVisitor
                delegators.accept(visitor)
                stateEnum.accept(visitor)
                eventEnum.accept(visitor)
                stateProperty.accept(visitor)
                handleEvent.accept(visitor)
                handleEvent.switchCase.accept(visitor)
                output += "}"
            }
        }

        @Test
        @DisplayName("Test that FSM Class node is generated")
        internal fun fsmClassNodeIsGenerated() = assertGenerated(
            "{I e I a}",
            "class f:acts {d e e p he sc}"
        )
    }
}