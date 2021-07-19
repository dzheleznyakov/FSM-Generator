package zh.fsm.smc.implementers

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
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

@DisplayName("Java Nested Switch Case Implementer Tests")
internal class JavaNestedSwitchCaseImplementerTest {
    private val builder = SyntaxBuilder()
    private val parser = Parser(builder)
    private val lexer: Lexer = FsmLexer(parser)
    private val analyzer = SemanticAnalyzer()
    private val optimizer = Optimizer()
    private val generator = NSCGenerator()
    private val emptyFlags = emptyMap<String, String>()

    private fun produceStateMachine(fsmSyntax: String): OptimizedStateMachine {
        lexer.lex(fsmSyntax)
        parser.handleEvent(ParserEvent.EOF, -1, -1)
        val ast = analyzer.analyze(builder.getFsm())
        return optimizer.optimize(ast)
    }

    private fun assertWhitespaceEquivalent(generated: String, expected: String) {
        assertThat(generated.compressWhiteSpace(), equalTo(expected.compressWhiteSpace()))
    }

    fun String.compressWhiteSpace(): String =
        replace("\\n+".toRegex(), "\n").replace("[\t ]+".toRegex(), " ").replace(" *\n *".toRegex(), "\n")

    @Test
    @DisplayName("Test generating class with one transition, actions and with package")
    internal fun oneTransitionWithPackageAndActions() {
        val flags = mapOf("package" to "thePackage")
        val javaImplementer = JavaNestedSwitchCaseImplementer(flags)
        val sm = produceStateMachine("" +
                "Initial: I\n" +
                "FSM: fsm\n" +
                "Actions: acts\n" +
                "{" +
                "  I E I A" +
                "}")

        val generatedFsm = generator.generate(sm)
        generatedFsm.accept(javaImplementer)

        assertWhitespaceEquivalent(javaImplementer.output, "" +
                "package thePackage;\n" +
                "" +
                "public abstract class fsm implements acts {\n" +
                "  public abstract void unhandledTransition(String state, String event);\n" +
                "" +
                "  private enum State {I}\n" +
                "  private enum Event {E}\n" +
                "  private State state = State.I;\n" +
                "" +
                "  private void setState(State s) {state = s;}\n" +
                "" +
                "  public void E() {handleEvent(Event.E);}\n" +
                "" +
                "  private void handleEvent(Event event) {\n" +
                "    switch(state) {\n" +
                "      case I:\n" +
                "        switch(event) {\n" +
                "          case E:\n" +
                "            setState(State.I);\n" +
                "            A();\n" +
                "            break;\n" +
                "          default: unhandledTransition(state.name(), event.name()); break;\n" +
                "        }\n" +
                "        break;\n" +
                "    }\n" +
                "  }\n" +
                "}\n")
    }

    @Test
    @DisplayName("Test generating class with one transition, actions, but no package")
    internal fun oneTransitionWithActionsButNoPackage() {
        val javaImplementer = JavaNestedSwitchCaseImplementer(emptyFlags)
        val sm = produceStateMachine("" +
                "Initial: I\n" +
                "FSM: fsm\n" +
                "Actions: acts\n" +
                "{" +
                "  I E I A" +
                "}")

        val generatedFsm = generator.generate(sm)
        generatedFsm.accept(javaImplementer)

        assertThat(javaImplementer.output, startsWith("public abstract class fsm implements acts {\n"))
    }

    @Test
    @DisplayName("Test generating class with one transition, but no actions and no package")
    internal fun oneTransitionWithNoActionsAndNoPackage() {
        val javaImplementer = JavaNestedSwitchCaseImplementer(emptyFlags)
        val sm = produceStateMachine("" +
                "Initial: I\n" +
                "FSM: fsm\n" +
                "{" +
                "  I E I A" +
                "}")

        val generatedFsm = generator.generate(sm)
        generatedFsm.accept(javaImplementer)

        val output = javaImplementer.output
        assertThat(output, startsWith("public abstract class fsm {\n"))
        assertThat(output, containsString("protected abstract void A();\n"))
    }
}