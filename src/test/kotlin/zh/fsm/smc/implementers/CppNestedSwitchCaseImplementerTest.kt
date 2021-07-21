package zh.fsm.smc.implementers

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCGenerator
import zh.fsm.smc.lexer.FsmLexer
import zh.fsm.smc.lexer.Lexer
import zh.fsm.smc.optimizer.Optimizer
import zh.fsm.smc.parser.Parser
import zh.fsm.smc.parser.ParserEvent
import zh.fsm.smc.parser.SyntaxBuilder
import zh.fsm.smc.semanticAnalyser.SemanticAnalyzer

@DisplayName("C++ Nested Switch Case Implementer Tests")
internal class CppNestedSwitchCaseImplementerTest {
    private val builder = SyntaxBuilder()
    private val parser = Parser(builder)
    private val lexer: Lexer = FsmLexer(parser)
    private val analyzer = SemanticAnalyzer()
    private val optimizer = Optimizer()
    private val generator = NSCGenerator()
    private val implementer = CppNestedSwitchCaseImplementer(emptyMap())

    private fun produceStateMachine(fsmSyntax: String): OptimizedStateMachine {
        lexer.lex(fsmSyntax)
        parser.handleEvent(ParserEvent.EOF, -1, -1)
        val ast = analyzer.analyze(builder.getFsm())
        return optimizer.optimize(ast)
    }

    @Test
    @DisplayName("No action yields an error")
    internal fun noAction_shouldBeError() {
        val sm = produceStateMachine("" +
                "Initial: I\n" +
                "FSM: fsm\n" +
                "{" +
                "  I E I A" +
                "}")

        val generatedFsm = generator.generate(sm)
        generatedFsm.accept(implementer)

        assertThat(implementer.errors, Matchers.hasSize(1))
        assertThat(implementer.errors[0], `is`(CppNestedSwitchCaseImplementer.Error.NO_ACTIONS))
    }

    @Test
    @DisplayName("Test generating a C file for a single transition FSM")
    internal fun oneTransition() {
        val sm = produceStateMachine("" +
                "Initial: I\n" +
                "FSM: fsm\n" +
                "Actions: acts\n" +
                "{" +
                "  I E I A" +
                "}")

        val generatedFsm = generator.generate(sm)
        generatedFsm.accept(implementer)

        assertWhitespaceEquivalent(implementer.output, "" +
                "#ifndef FSM_H\n" +
                "#define FSM_H\n" +
                "#include \"acts.h\"\n" +
                "" +
                "class fsm : public acts {\n" +
                "public:\n" +
                "  fsm()\n" +
                "  : state(State_I)\n" +
                "  {}\n" +
                "" +
                "  void E() {processEvent(Event_E, \"E\");}\n" +
                "" +
                "private:\n" +
                "  enum State {State_I};\n" +
                "  State state;\n" +
                "" +
                "  void setState(State s) {state=s;}\n" +
                "" +
                "  enum Event {Event_E};\n" +
                "" +
                "  void processEvent(Event event, const char* eventName) {\n" +
                "    switch (state) {\n" +
                "      case State_I:\n" +
                "        switch (event) {\n" +
                "          case Event_E:\n" +
                "            setState(State_I);\n" +
                "            A();\n" +
                "            break;\n" +
                "" +
                "          default:\n" +
                "            unexpected_transition(\"I\", eventName);\n" +
                "            break;\n" +
                "        }\n" +
                "        break;\n" +
                "    }\n" +
                "  }\n" +
                "};\n" +
                "#endif\n")
    }
}