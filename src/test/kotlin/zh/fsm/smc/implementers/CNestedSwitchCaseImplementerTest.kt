package zh.fsm.smc.implementers

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
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

@DisplayName("C Nested Switch Case Implementer Tests")
internal class CNestedSwitchCaseImplementerTest {
    private val builder = SyntaxBuilder()
    private val parser = Parser(builder)
    private val lexer: Lexer = FsmLexer(parser)
    private val analyzer = SemanticAnalyzer()
    private val optimizer = Optimizer()
    private val generator = NSCGenerator()
    private val implementer = CNestedSwitchCaseImplementer(emptyMap())

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

        assertThat(implementer.errors, hasSize(1))
        assertThat(implementer.errors[0], `is`(CNestedSwitchCaseImplementer.Error.NO_ACTION))
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

        assertWhitespaceEquivalent(implementer.fsmHeader, "" +
                "#ifndef FSM_H\n" +
                "#define FSM_H\n" +
                "struct acts;\n" +
                "struct fsm;\n" +
                "struct fsm *make_fsm(struct acts*);\n" +
                "void fsm_E(struct fsm*);\n" +
                "#endif\n")

        assertWhitespaceEquivalent(implementer.fsmImplementation, "" +
                "#include <stdlib.h>\n" +
                "#include \"acts.h\"\n" +
                "#include \"fsm.h\"\n" +
                "" +
                "enum Event {E};\n" +
                "enum State {I};\n" +
                "" +
                "struct fsm {\n" +
                "  enum State state;\n" +
                "  struct acts *actions;\n" +
                "};\n" +
                "" +
                "struct fsm *make_fsm(struct acts* actions) {\n" +
                "  struct fsm *fsm = malloc(sizeof(struct fsm));\n" +
                "  fsm->actions = actions;\n" +
                "  fsm->state = I;\n" +
                "  return fsm;\n" +
                "}\n" +
                "" +
                "static void setState(struct fsm *fsm, enum State state) {\n" +
                "  fsm->state = state;\n" +
                "}\n" +
                "" +
                "static void A(struct fsm *fsm) {\n" +
                "  fsm->actions->A();\n" +
                "}\n" +
                "" +
                "static void processEvent(enum State state, enum Event event, struct fsm *fsm, char *event_name) {\n" +
                "  switch (state) {\n" +
                "    case I:\n" +
                "      switch (event) {\n" +
                "        case E:\n" +
                "          setState(fsm, I);\n" +
                "          A(fsm);\n" +
                "          break;\n" +
                "        default:\n" +
                "          (fsm->actions->unexpected_transition)(\"I\", event_name);\n" +
                "          break;\n" +
                "      }\n" +
                "      break;\n" +
                "  }\n" +
                "}\n" +
                "" +
                "void fsm_E(struct fsm* fsm) {\n" +
                "  processEvent(fsm->state, E, fsm, \"E\");\n" +
                "}\n")
    }
}