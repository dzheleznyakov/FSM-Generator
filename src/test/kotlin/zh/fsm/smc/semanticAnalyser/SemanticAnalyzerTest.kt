package zh.fsm.smc.semanticAnalyser

import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zh.fsm.smc.lexer.FsmLexer
import zh.fsm.smc.parser.FsmSyntax.Header
import zh.fsm.smc.parser.Parser
import zh.fsm.smc.parser.ParserEvent
import zh.fsm.smc.parser.SyntaxBuilder
import zh.fsm.smc.semanticAnalyser.SemanticStateMachine.AnalysisError
import zh.fsm.smc.semanticAnalyser.SemanticStateMachine.AnalysisError.ID.*
import zh.fsm.smc.semanticAnalyser.SemanticStateMachine.SemanticState

@DisplayName("Testing SemanticAnalyzer")
internal class SemanticAnalyzerTest {
    private lateinit var lexer: FsmLexer
    private lateinit var parser: Parser
    private lateinit var builder: SyntaxBuilder
    private lateinit var analyzer: SemanticAnalyzer

    @BeforeEach
    internal fun setUp() {
        builder = SyntaxBuilder()
        parser = Parser(builder)
        lexer = FsmLexer(parser)
        analyzer = SemanticAnalyzer()
    }

    private fun produceAst(input: String): SemanticStateMachine {
        lexer.lex(input)
        parser.handleEvent(ParserEvent.EOF, -1, -1)
        return analyzer.analyze(builder.getFsm())
    }

    private fun assertSemanticResult(input: String, expected: String) {
        val semanticStateMachine = produceAst(input)
        assertThat(semanticStateMachine.toString(), `is`(expected))
    }

    private fun hasError(errorCode: AnalysisError.ID, extra: Any? = null): Matcher<MutableIterable<*>> =
        if (extra == null) hasItem(AnalysisError(errorCode))
        else hasItem(AnalysisError(errorCode, extra))

    @Nested
    inner class SemanticErrors {
        @Nested
        inner class HeaderErrors {
            @Test
            @DisplayName("Missing mandatory headers")
            internal fun noHeaders() {
                val errors = produceAst("{}").errors
                assertThat(errors, hasError(NO_FSM))
                assertThat(errors, hasError(NO_INITIAL))
            }

            @Test
            @DisplayName("Mandatory headers are present")
            internal fun presentHeaders() {
                val errors = produceAst("FSM:f Initial:i {}").errors
                assertThat(errors, not(hasError(NO_FSM)))
                assertThat(errors, not(hasError(NO_INITIAL)))
            }

            @Test
            @DisplayName("Mandatory FSM header is missing")
            internal fun missingFsm() {
                val errors = produceAst("Actions: a Initial:i {}").errors
                assertThat(errors, not(hasError(NO_INITIAL)))
                assertThat(errors, hasError(NO_FSM))
            }

            @Test
            @DisplayName("Mandatory Initial header is missing")
            internal fun missingInitial() {
                val errors = produceAst("Actions: a Fsm: f {}").errors
                assertThat(errors, not(hasError(NO_FSM)))
                assertThat(errors, hasError(NO_INITIAL))
            }

            @Test
            @DisplayName("All allowed headers are present")
            internal fun nothingMissing() {
                val errors = produceAst("Initial: f Actions: a FSM: f {}").errors
                assertThat(errors, not(hasError(NO_FSM)))
                assertThat(errors, not(hasError(NO_INITIAL)))
            }

            @Test
            @DisplayName("Unexpected header is present")
            internal fun unexpectedHeaders() {
                val errors = produceAst("X: x {s - - -}").errors
                assertThat(errors, hasError(INVALID_HEADER, Header("X", "x")))
            }

            @Test
            @DisplayName("A header is defined twice with different value")
            internal fun duplicateHeader() {
                val errors = produceAst("fsm:f fsm: x {s - - -}").errors
                assertThat(errors, hasError(EXTRA_HEADER_IGNORED, Header("fsm", "x")))
            }

            @Test
            @DisplayName("Initial state is not defined")
            internal fun initialStateMustBeDefined() {
                val errors = produceAst("initial: i {s - - -}").errors
                assertThat(errors, hasError(UNDEFINED_STATE, "initial: i"))
            }
        }
    }

    @Nested
    inner class StateErrors {
        @Test
        @DisplayName("Transition can have null next state")
        internal fun nullNextStateIsNotUndefined() {
            val errors = produceAst("{s - - -}").errors
            assertThat(errors, not(hasError(UNDEFINED_STATE)))
        }

        @Test
        @DisplayName("Next state is not defined")
        internal fun undefinedState() {
            val errors = produceAst("{s - s2 -}").errors
            assertThat(errors, hasError(UNDEFINED_STATE, "s2"))
        }

        @Test
        @DisplayName("The same state can be the next state")
        internal fun noUndefinedStates() {
            val errors = produceAst("{s - s -}").errors
            assertThat(errors, not(hasError(UNDEFINED_STATE, "s")))
        }

        @Test
        @DisplayName("Undefined super state produces an error")
        internal fun undefinedSuperState() {
            val errors = produceAst("{s:ss - - -}").errors
            assertThat(errors, hasError(UNDEFINED_SUPER_STATE, "ss"))
        }

        @Test
        @DisplayName("Defined super state does not produce an error")
        internal fun definedSuperState() {
            val errors = produceAst("{ss - - - s:ss - - -}").errors
            assertThat(errors, not(hasError(UNDEFINED_SUPER_STATE, "ss")))
        }

        @Test
        @DisplayName("Unused states produce errors")
        internal fun unusedStates() {
            val errors = produceAst("{s e n -}").errors
            assertThat(errors, hasError(UNUSED_STATE, "s"))
        }

        @Test
        @DisplayName("If there are no unused states, then there are no corresponding errors")
        internal fun noUnusedStates() {
            val errors = produceAst("{s e s -}").errors
            assertThat(errors, not(hasError(UNUSED_STATE, "s")))
        }

        @Test
        @DisplayName("If the next state is null, then the current state is used")
        internal fun nextStateNullIsImplicitUse() {
            val errors = produceAst("{s e - -}").errors
            assertThat(errors, not(hasError(UNUSED_STATE, "s")))
        }

        @Test
        @DisplayName("Using a state solely as a base is a valid usage")
        internal fun usedAsBasedIsValidUsage() {
            val errors = produceAst("{b e n - s:b e2 s -}").errors
            assertThat(errors, not(hasError(UNUSED_STATE, "b")))
        }

        @Test
        @DisplayName("Using a state solely as initial is a valid usage")
        internal fun usedAsInitialStateIsValidUsage() {
            val errors = produceAst("initial: b { b e n -}").errors
            assertThat(errors, not(hasError(UNUSED_STATE, "b")))
        }

        @Test
        @DisplayName("Super states should not have conflicting transitions")
        internal fun errorIfSuperStatesHaveConflictingTransitions() {
            val errors = produceAst("" +
                    "FSM: f Actions: act Initial: s" +
                    "{" +
                    "  (ss1) e1 s1 -" +
                    "  (ss2) e1 s2 -" +
                    "  s:ss1:ss2 e2 s3 a" +
                    "  s2 e s -" +
                    "  s1 e s -" +
                    "  s3 e s -" +
                    "}").errors
            assertThat(errors, hasError(CONFLICTING_SUPERSTATES, "s|e1"))
        }

        @Test
        @DisplayName("If the transition is overridden, then this does not cause an error")
        internal fun noErrorForOverriddenTransition() {
            val errors = produceAst("" +
                    "FSM: f Actions: act Initial: s" +
                    "{" +
                    "  (ss1) e1 s1 -" +
                    "  s:ss1 e1 s3 a" +
                    "  s1 e s -" +
                    "  s3 e s -" +
                    "}").errors
            assertThat(errors, not(hasError(CONFLICTING_SUPERSTATES, "s|e1")))
        }

        @Test
        @DisplayName("If super states have identical transitions, then this does not cause an error")
        internal fun noErrorIfSuperstatesHaveIdenticalTransitions() {
            val errors = produceAst("" +
                    "FSM: f Actions: act Initial: s" +
                    "{" +
                    "  (ss1) e1 s1 ax" +
                    "  (ss2) e1 s1 ax" +
                    "  s:ss1:ss2 e2 s3 a" +
                    "  s1 e s -" +
                    "  s3 e s -" +
                    "}").errors
            assertThat(errors, not(hasError(CONFLICTING_SUPERSTATES, "s|e1")))
        }

        @Test
        @DisplayName("Super states having different actions in the same transition, this causes error")
        internal fun errorIfSuperstatesHaveDifferentActionsInSameTransition() {
            val errors = produceAst("" +
                    "FSM: f Actions: act Initial: s" +
                    "{" +
                    "  (ss1) e1 s1 a1" +
                    "  (ss2) e1 s1 a2" +
                    "  s:ss1:ss2 e2 s3 a" +
                    "  s1 e s -" +
                    "  s3 e s -" +
                    "}").errors
            assertThat(errors, hasError(CONFLICTING_SUPERSTATES, "s|e1"))
        }
    }

    @Nested
    inner class TransitionErrors {
        @Test
        @DisplayName("Duplicate transitions cause an error")
        internal fun duplicateTransitions() {
            val errors = produceAst("{s e - - s e - -}").errors
            assertThat(errors, hasError(DUPLICATE_TRANSITION, "s(e)"))
        }

        @Test
        @DisplayName("No duplicate transitions does not cause the correspoiding error")
        internal fun noDuplicateTransitions() {
            val errors = produceAst("{s e - -}").errors
            assertThat(errors, not(hasError(DUPLICATE_TRANSITION, "s(e)")))
        }

        @Test
        @DisplayName("No abstract state can be a transition target")
        internal fun noAbstractStatesCannotBeTargets() {
            val errors = produceAst("{(as) e - - s e as -}").errors
            assertThat(errors, hasError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->as"))
        }

        @Test
        @DisplayName("Abstract states can be used as super states")
        internal fun abstractStatesCanBeUsedAsSuperstates() {
            val errors = produceAst("{(as) e - - s:as e s -}").errors
            assertThat(errors, not(hasError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->as")))
        }

        @Test
        @DisplayName("The same entry or exit action defined multiple times does not cause an error")
        internal fun entryAndExitActionsNotMultiplyDefined() {
            val errors = produceAst("" +
                    "{" +
                    "  s - - -" +
                    "  s - - -" +
                    "  es - - -" +
                    "  es <x - - -" +
                    "  es <x - - -" +
                    "  xs >x - - -" +
                    "  xs >{x} - - -" +
                    "}").errors
            assertThat(errors, not(hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "s")))
            assertThat(errors, not(hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "es")))
            assertThat(errors, not(hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "xs")))
        }

        @Test
        @DisplayName("If a state has multiple different entry actions, this causes an error")
        internal fun errorIfStateHasMultipleEntryActionDefinitions() {
            val errors = produceAst("" +
                    "{" +
                    "  s - - - " +
                    "  ds <x - - -" +
                    "  ds <y - - -" +
                    "}").errors
            assertThat(errors, not(hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "s")))
            assertThat(errors, hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds"))
        }

        @Test
        @DisplayName("If a state has multiple different exit actions, this causes an error")
        internal fun errorIfStateHasMultipleExitActionDefinitions() {
            val errors = produceAst("" +
                    "{" +
                    "  s - - - " +
                    "  ds >x - - -" +
                    "  ds >y - - -" +
                    "}").errors
            assertThat(errors, not(hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "s")))
            assertThat(errors, hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds"))
        }

        @Test
        @DisplayName("If a state has multiply defined entry and exit actions, this causes an error")
        internal fun errorIfStateHasMultiplyDefinedEntryAndExitActions() {
            val errors = produceAst("{ds >x - - - ds <y - - -}").errors
            assertThat(errors, hasError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds"))
        }
    }

    @Nested
    inner class Warnings {
        @Test
        @DisplayName("If a state is used as both abstract and conrete, this generates a warning")
        internal fun warnIfStateIsUsedAsBothAbstractAndConcrete() {
            val warnings = produceAst("{(ias) e - - ias e - - (cas) e - -}").warnings
            assertThat(warnings, not(hasError(INCONSISTENT_ABSTRACTION, "cas")))
            assertThat(warnings, hasError(INCONSISTENT_ABSTRACTION, "ias"))
        }
    }

    @Nested
    inner class Lists {
        @Test
        @DisplayName("Test parsing single state FSM")
        internal fun oneState() {
            val ast = produceAst("{s - - -}")
            assertThat(ast.states.values, contains(SemanticState("s")))
        }

        @Test
        @DisplayName("Test parsing multiple states FSM")
        internal fun manyStates() {
            val ast = produceAst("{s1 - - - s2 - - - s3 - - -}")
            assertThat(ast.states.values, hasItems(
                SemanticState("s1"),
                SemanticState("s2"),
                SemanticState("s3")
            ))
        }

        @Test
        @DisplayName("States are in the map with state names as keyes")
        internal fun statesAreKeyedByName() {
            val states = produceAst("{s1 - - - s2 - - - s3 - - -}").states
            assertThat(states["s1"], `is`(equalTo(SemanticState("s1"))))
            assertThat(states["s2"], `is`(equalTo(SemanticState("s2"))))
            assertThat(states["s3"], `is`(equalTo(SemanticState("s3"))))
        }

        @Test
        @DisplayName("Test parsing a FSM with multiple events")
        internal fun manyEvents() {
            val events = produceAst("{s1 e1 - - s2 e2 - - s3 e3 - -}").events
            assertThat(events, hasSize(3))
            assertThat(events, hasItems("e1", "e2", "e3"))
        }

        @Test
        @DisplayName("Duplicate events do not count")
        internal fun manyEventsButNoDuplicates() {
            val events = produceAst("{s1 e1 - - s2 e2 - - s3 e1 - -}").events
            assertThat(events, hasSize(2))
            assertThat(events, hasItems("e1", "e2"))
        }

        @Test
        @DisplayName("Null events do not count")
        internal fun noNullEvents() {
            val events = produceAst("{(s1) - - -}").events
            assertThat(events, empty())
        }

        @Test
        @DisplayName("Duplicate actions do not count")
        internal fun manyActionsButNoDuplicates() {
            val actions = produceAst("{s1 e1 - {a1 a2} s2 e2 - {a3 a1}}").actions
            assertThat(actions, hasSize(3))
            assertThat(actions, hasItems("a1", "a2", "a3"))
        }

        @Test
        @DisplayName("")
        internal fun entryAndExitActionsCountedAsActions() {
            val actions = produceAst("{s <ea >xa - - a}").actions
            assertThat(actions, hasItems("ea", "xa"))
        }
    }

    @Nested
    inner class Logic {
        private fun String.addHeaders() = "initial:s fsm:f actions:a $this"


        private fun String.assertSyntaxToAst(ast: String): Unit = this
            .addHeaders()
            .let { syntax -> produceAst(syntax).statesToString() }
            .let { states -> assertThat(states, equalTo(ast)) }

        @Test
        @DisplayName("Test parsing transition with one state")
        internal fun oneTransition() = "{s e s a}".assertSyntaxToAst("" +
                "{\n" +
                "  s {\n" +
                "    e s {a}\n" +
                "  }\n" +
                "}\n")

        @Test
        @DisplayName("Two transitions for one state are aggregated")
        internal fun twoTransitionsAreAggregated() = "{s e1 s a s e2 s a}".assertSyntaxToAst("" +
                "{\n" +
                "  s {\n" +
                "    e1 s {a}\n" +
                "    e2 s {a}\n" +
                "  }\n" +
                "}\n")

        @Test
        @DisplayName("Super states are aggregated")
        internal fun superStatesAreAggregated() = "{s:b1 e1 s a s:b2 e2 s a (b1) e s - (b2) e s -}".assertSyntaxToAst(
                "" +
                        "{\n" +
                        "  (b1) {\n" +
                        "    e s {}\n" +
                        "  }\n" +
                        "\n" +
                        "  (b2) {\n" +
                        "    e s {}\n" +
                        "  }\n" +
                        "\n" +
                        "  s :b1 :b2 {\n" +
                        "    e1 s {a}\n" +
                        "    e2 s {a}\n" +
                        "  }\n" +
                        "}\n"
            )

        @Test
        @DisplayName("Null next state refers to self")
        internal fun nullNextStateRefersToSelf() = "{s e - a}".assertSyntaxToAst("" +
                "{\n" +
                "  s {\n" +
                "    e s {a}\n" +
                "  }\n" +
                "}\n")

        @Test
        @DisplayName("Actions remain in the same order they are written")
        internal fun actionsRemainInOrder() = "{s e s {the quick brown frog jumped over the lazy dogs back}}".assertSyntaxToAst("" +
                "{\n" +
                "  s {\n" +
                "    e s {the quick brown frog jumped over the lazy dogs back}\n" +
                "  }\n" +
                "}\n")

        @Test
        @DisplayName("Entry and exit actions remain in the same order they are written")
        internal fun entryAndExitActionsRemainInOrder() = "{s <{d o} <g >{c a} >t e s a}".assertSyntaxToAst("" +
                "{\n" +
                "  s <d <o <g >c >a >t {\n" +
                "    e s {a}\n" +
                "  }\n" +
                "}\n")
    }

    @Nested
    inner class AcceptanceTests {
        @Test
        @DisplayName("Subway One Coin Turnstile")
        internal fun subwayTurnstileOne() {
            val actual = produceAst("" +
                    "Actions: Turnstile\n" +
                    "FSM: OneCoinTurnstile\n" +
                    "Initial: Locked\n" +
                    "{\n" +
                    "  Locked\tCoin\tUnlocked\t{alarmOff unlock}\n" +
                    "  Locked \tPass\tLocked\talarmOn\n" +
                    "  Unlocked\tCoin\tUnlocked\tthankyou\n" +
                    "  Unlocked\tPass\tLocked\t\tlock\n" +
                    "}").toString()

                val expected = "" +
                        "Actions: Turnstile\n" +
                        "FSM: OneCoinTurnstile\n" +
                        "Initial: Locked{\n" +
                        "  Locked {\n" +
                        "    Coin Unlocked {alarmOff unlock}\n" +
                        "    Pass Locked {alarmOn}\n" +
                        "  }\n" +
                        "\n" +
                        "  Unlocked {\n" +
                        "    Coin Unlocked {thankyou}\n" +
                        "    Pass Locked {lock}\n" +
                        "  }\n" +
                        "}\n"

            assertThat(actual, equalTo(expected))
        }

        @Test
        @DisplayName("Subway Two Coins Turnstile")
        internal fun subwayTurnstileTwo() {
            val actual = produceAst("" +
                    "Actions: Turnstile\n" +
                    "FSM: TwoCoinTurnstile\n" +
                    "Initial: Locked\n" +
                    "{\n" +
                    "\tLocked {\n" +
                    "\t\tPass\tAlarming\talarmOn\n" +
                    "\t\tCoin\tFirstCoin\t-\n" +
                    "\t\tReset\tLocked\t{lock alarmOff}\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tAlarming\tReset\tLocked {lock alarmOff}\n" +
                    "\t\n" +
                    "\tFirstCoin {\n" +
                    "\t\tPass\tAlarming\t-\n" +
                    "\t\tCoin\tUnlocked\tunlock\n" +
                    "\t\tReset\tLocked\t{lock alarmOff}\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tUnlocked {\n" +
                    "\t\tPass\tLocked\tlock\n" +
                    "\t\tCoin\t-\t\tthankyou\n" +
                    "\t\tReset\tLocked\t\t{lock alarmOff}\n" +
                    "\t}\n" +
                    "}").toString()

            val expected = "" +
                    "Actions: Turnstile\n" +
                    "FSM: TwoCoinTurnstile\n" +
                    "Initial: Locked{\n" +
                    "  Alarming {\n" +
                    "    Reset Locked {lock alarmOff}\n" +
                    "  }\n" +
                    "\n" +
                    "  FirstCoin {\n" +
                    "    Pass Alarming {}\n" +
                    "    Coin Unlocked {unlock}\n" +
                    "    Reset Locked {lock alarmOff}\n" +
                    "  }\n" +
                    "\n" +
                    "  Locked {\n" +
                    "    Pass Alarming {alarmOn}\n" +
                    "    Coin FirstCoin {}\n" +
                    "    Reset Locked {lock alarmOff}\n" +
                    "  }\n" +
                    "\n" +
                    "  Unlocked {\n" +
                    "    Pass Locked {lock}\n" +
                    "    Coin Unlocked {thankyou}\n" +
                    "    Reset Locked {lock alarmOff}\n" +
                    "  }\n" +
                    "}\n"

            assertThat(actual, equalTo(expected))
        }

        @Test
        @DisplayName("Subway Two Coins Turnstile with super states")
        internal fun subwayTurnstileThree() {
            val actual = produceAst("" +
                    "Actions: Turnstile\n" +
                    "FSM: TwoCoinTurnstile\n" +
                    "Initial: Locked\n" +
                    "{\n" +
                    "    (Base)\tReset\tLocked\tlock\n" +
                    "\n" +
                    "\tLocked : Base {\n" +
                    "\t\tPass\tAlarming\t-\n" +
                    "\t\tCoin\tFirstCoin\t-\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tAlarming : Base\t<alarmOn >alarmOff -\t-\t-\n" +
                    "\t\n" +
                    "\tFirstCoin : Base {\n" +
                    "\t\tPass\tAlarming\t-\n" +
                    "\t\tCoin\tUnlocked\tunlock\n" +
                    "\t}\n" +
                    "\t\n" +
                    "\tUnlocked : Base {\n" +
                    "\t\tPass\tLocked\tlock\n" +
                    "\t\tCoin\t-\tthankyou\n" +
                    "\t}\n" +
                    "}").toString()

            val expected = "" +
                    "Actions: Turnstile\n" +
                    "FSM: TwoCoinTurnstile\n" +
                    "Initial: Locked{\n" +
                    "  Alarming :Base <alarmOn >alarmOff {\n" +
                    "    null Alarming {}\n" +
                    "  }\n" +
                    "\n" +
                    "  (Base) {\n" +
                    "    Reset Locked {lock}\n" +
                    "  }\n" +
                    "\n" +
                    "  FirstCoin :Base {\n" +
                    "    Pass Alarming {}\n" +
                    "    Coin Unlocked {unlock}\n" +
                    "  }\n" +
                    "\n" +
                    "  Locked :Base {\n" +
                    "    Pass Alarming {}\n" +
                    "    Coin FirstCoin {}\n" +
                    "  }\n" +
                    "\n" +
                    "  Unlocked :Base {\n" +
                    "    Pass Locked {lock}\n" +
                    "    Coin Unlocked {thankyou}\n" +
                    "  }\n" +
                    "}\n"

            assertThat(actual, equalTo(expected))
        }
    }
}