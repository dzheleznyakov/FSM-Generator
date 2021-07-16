package zh.fsm.smc.optimizer

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.DisplayName

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.lexer.FsmLexer
import zh.fsm.smc.lexer.Lexer
import zh.fsm.smc.parser.Parser
import zh.fsm.smc.parser.ParserEvent
import zh.fsm.smc.parser.SyntaxBuilder
import zh.fsm.smc.semanticAnalyser.SemanticAnalyzer

@DisplayName("Testing Optimizer")
internal class OptimizerTest {
    private lateinit var lexer: Lexer
    private lateinit var parser: Parser
    private lateinit var builder: SyntaxBuilder
    private lateinit var analyzer: SemanticAnalyzer
    private lateinit var optimizer: Optimizer
    private lateinit var optimizedStateMachine: OptimizedStateMachine

    @BeforeEach
    internal fun setUp() {
        builder = SyntaxBuilder()
        parser = Parser(builder)
        lexer = FsmLexer(parser)
        analyzer = SemanticAnalyzer()
        optimizer = Optimizer()
    }

    private fun produceStateMachineWithHeader(s: String): OptimizedStateMachine =
        produceStateMachine("fsm:f initial:i actions:a $s")

    private fun produceStateMachine(fsmSyntax: String): OptimizedStateMachine {
        lexer.lex(fsmSyntax)
        parser.handleEvent(ParserEvent.EOF, -1, -1)
        val ast = analyzer.analyze(builder.getFsm())
        return optimizer.optimize(ast)
    }

    fun assertOptimization(syntax: String, stateMachine: String) {
        optimizedStateMachine = produceStateMachineWithHeader(syntax)
        assertThat(
            optimizedStateMachine.transitionsToString().compressWithSpace(),
            equalTo(stateMachine.compressWithSpace()))
    }

    @Nested
    inner class BasicOptimizationFunctions {
        @Test
        @DisplayName("Test headers")
        internal fun header() {
            val header = produceStateMachineWithHeader("{i e i -}").header
            assertThat(header.fsm, equalTo("f"))
            assertThat(header.initial, equalTo("i"))
            assertThat(header.actions, equalTo("a"))
        }

        @Test
        @DisplayName("Test that all the states are preserved")
        internal fun statesArePreserved() {
            val states = produceStateMachineWithHeader("{i e s - s e i -}").states
            assertThat(states, hasSize(2))
            assertThat(states, contains("i", "s"))
        }

        @Test
        @DisplayName("Test that abstract states are removed")
        internal fun abstractStatesAreRemoved() {
            val states = produceStateMachineWithHeader("{(b) - - - i:b e i -}").states
            assertThat(states, not(hasItem("b")))
        }

        @Test
        @DisplayName("Test that events are preserved")
        internal fun eventsArePreserved() {
            val events = produceStateMachineWithHeader("{i e1 s - s e2 i -}").events
            assertThat(events, hasSize(2))
            assertThat(events, contains("e1", "e2"))
        }

        @Test
        @DisplayName("Test that actions are preserved")
        internal fun actionsArePreserved() {
            val actions = produceStateMachineWithHeader("{i e1 s a1 s e2 i a2}").actions
            assertThat(actions, hasSize(2))
            assertThat(actions, contains("a1", "a2"))
        }

        @Test
        @DisplayName("Test simple state machine")
        internal fun simpleStateMachine() {
            assertOptimization("" +
                    "{i e i a1}",
            "" +
                    "i {\n" +
                    "  e i {a1}\n" +
                    "}\n")

            assertThat(optimizedStateMachine.transitions, hasSize(1))
        }
    }

    @Nested
    inner class EntryAndExitActions {
        @Test
        @DisplayName("Test that entry actions are added to the source state actions")
        internal fun entryFunctionsAdded() = assertOptimization(
            "" +
                    "{" +
                    "  i e s a1" +
                    "  i e2 s a2" +
                    "  s <n1 <n2 e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {n1 n2 a1}\n" +
                    "  e2 s {n1 n2 a2}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )

        @Test
        @DisplayName("Test that exit actions are added to the source state actions")
        internal fun exitFunctionsAdded() = assertOptimization(
            "" +
                    "{" +
                    "  i >x2 >x1 e s a1" +
                    "  i e2 s a2" +
                    "  s e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {x2 x1 a1}\n" +
                    "  e2 s {x2 x1 a2}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )

        @Test
        @DisplayName("Test the entry and exit actions of the superstate are added")
        internal fun firstSuperStateEntryAndExitActionsAdded() =
            assertOptimization(
                "" +
                        "{" +
                        "  (ib) >ibx1 >ibx2 - - -" +
                        "  (sb) <sbn1 <sbn2 - - -" +
                        "  i:ib >x e s a" +
                        "  s:sb <n e i -" +
                        "}",
                "" +
                        "i {\n" +
                        "  e s {x ibx1 ibx2 sbn1 sbn2 n a}\n" +
                        "}\n" +
                        "s {\n" +
                        "  e i {}\n" +
                        "}\n"
            )

        @Test
        @DisplayName("Entry and exit actions from super states up the hierarchy are added")
        internal fun multipleSuperStateEntryAndExitActionsAreAdded() =
            assertOptimization(
                "" +
                        "{" +
                        "  (ib1) >ib1x - - -" +
                        "  (ib2) : ib1 >ib2x - - -" +
                        "  (sb1) <sb1n - - -" +
                        "  (sb2) : sb1 <sb2n - - -" +
                        "  i:ib2 >x e s a" +
                        "  s:sb2 <n e i -" +
                        "}",
                "" +
                        "i {\n" +
                        "  e s {x ib2x ib1x sb1n sb2n n a}\n" +
                        "}\n" +
                        "s {\n" +
                        "  e i {}\n" +
                        "}\n"
            )

        @Test
        @DisplayName("Entry and exit actions from diamond super states are added")
        internal fun diamondSuperStateEntryAndExitActionsAreAdded() = assertOptimization(
            "" +
                    "{" +
                    "  (ib1) >ib1x - - -" +
                    "  (ib2) : ib1 >ib2x - - -" +
                    "  (ib3) : ib1 >ib3x - - -" +
                    "  (sb1) <sb1n - - -" +
                    "  (sb2) :sb1 <sb2n - - -" +
                    "  (sb3) :sb1 <sb3n - - -" +
                    "  i:ib2 :ib3 >x e s a" +
                    "  s :sb2 :sb3 <n e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {x ib3x ib2x ib1x sb1n sb2n sb3n n a}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )
    }

    @Nested
    inner class SuperStateTransitions {
        @Test
        @DisplayName("Transitions from super states are added")
        internal fun simpleInheritanceOfTransitions() = assertOptimization(
            "" +
                    "{" +
                    "  (b) be s ba" +
                    "  i:b e s a" +
                    "  s e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {a}\n" +
                    "  be s {ba}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )

        @Test
        @DisplayName("Transitions from super states up the hierarchy are added")
        internal fun deepInheritanceOfTransitions() = assertOptimization(
            "" +
                    "{" +
                    "  (b1) {" +
                    "    b1e1 s b1a1" +
                    "    b1e2 s b1a2" +
                    "  }" +
                    "  (b2):b1 b2e s b2a" +
                    "  i:b2 e s a" +
                    "  s e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {a}\n" +
                    "  b2e s {b2a}\n" +
                    "  b1e1 s {b1a1}\n" +
                    "  b1e2 s {b1a2}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )

        @Test
        @DisplayName("Transitions from different super stateas are added")
        internal fun multipleInheritanceOfTransitions() = assertOptimization(
            "" +
                    "{" +
                    "  (b1) b1e s b1a" +
                    "  (b2) b2e s b2a" +
                    "  i:b1 :b2 e s a" +
                    "  s e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {a}\n" +
                    "  b2e s {b2a}\n" +
                    "  b1e s {b1a}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )

        @Test
        @DisplayName("Transitions from diamond super states are added")
        internal fun diamondInheritanceOfTransitions() = assertOptimization(
            "" +
                    "{" +
                    "  (b) be s ba" +
                    "  (b1):b b1e s b1a" +
                    "  (b2):b b2e s b2a" +
                    "  i:b1 :b2 e s a" +
                    "  s e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {a}\n" +
                    "  b2e s {b2a}\n" +
                    "  b1e s {b1a}\n" +
                    "  be s {ba}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )

        @Test
        @DisplayName("Transition is not inherited from the sub state if it is overridden")
        internal fun overridingTransitions() = assertOptimization(
            "" +
                    "{" +
                    "  (b) e s2 a2" +
                    "  i:b e s a" +
                    "  s e i -" +
                    "  s2 e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {a}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n" +
                    "s2 {\n" +
                    "  e i {}\n" +
                    "}\n"
        )

        @Test
        @DisplayName("Duplicated transitions are eliminated")
        internal fun eliminationOfDuplicateTransitions() = assertOptimization(
            "" +
                    "{" +
                    "  (b) e s a" +
                    "  i:b e s a" +
                    "  s e i -" +
                    "}",
            "" +
                    "i {\n" +
                    "  e s {a}\n" +
                    "}\n" +
                    "s {\n" +
                    "  e i {}\n" +
                    "}\n"
        )
    }

    @Nested
    inner class AcceptanceTests {
        @Test
        @DisplayName("")
        internal fun turnstile3() {
            val smString = produceStateMachine(
                "" +
                        "Actions: Turnstile\n" +
                        "FSM: TwoCoinTurnstile\n" +
                        "Initial: Locked\n" +
                        "{" +
                        "    (Base)  Reset  Locked  lock" +
                        "" +
                        "  Locked : Base {" +
                        "    Pass  Alarming  -" +
                        "    Coin  FirstCoin -" +
                        "  }" +
                        "" +
                        "  Alarming : Base <alarmOn >alarmOff -  -  -" +
                        "" +
                        "  FirstCoin : Base {" +
                        "    Pass  Alarming  -" +
                        "    Coin  Unlocked  unlock" +
                        "  }" +
                        "" +
                        "  Unlocked : Base {" +
                        "    Pass  Locked  lock" +
                        "    Coin  -       thankyou" +
                        "}"
            ).toString()

            assertThat(smString, equalTo("" +
                    "Initial: Locked\n" +
                    "FSM: TwoCoinTurnstile\n" +
                    "Actions:Turnstile\n" +
                    "{\n" +
                    "  Alarming {\n" +
                    "    Reset Locked {alarmOff lock}\n" +
                    "  }\n" +
                    "  FirstCoin {\n" +
                    "    Pass Alarming {alarmOn}\n" +
                    "    Coin Unlocked {unlock}\n" +
                    "    Reset Locked {lock}\n" +
                    "  }\n" +
                    "  Locked {\n" +
                    "    Pass Alarming {alarmOn}\n" +
                    "    Coin FirstCoin {}\n" +
                    "    Reset Locked {lock}\n" +
                    "  }\n" +
                    "  Unlocked {\n" +
                    "    Pass Locked {lock}\n" +
                    "    Coin Unlocked {thankyou}\n" +
                    "    Reset Locked {lock}\n" +
                    "  }\n" +
                    "}\n"))
        }
    }

    private fun String.compressWithSpace(): String = this
        .replace("\\n+".toRegex(), "\n")
        .replace("[\t ]+".toRegex(), " ")
        .replace(" *\n *".toRegex(), "\n")
}