package zh.fsm.smc

import zh.args.Args
import zh.args.ArgsException
import zh.fsm.smc.generators.CodeGenerator
import zh.fsm.smc.lexer.FsmLexer
import zh.fsm.smc.lexer.Lexer
import zh.fsm.smc.optimizer.Optimizer
import zh.fsm.smc.parser.FsmSyntax
import zh.fsm.smc.parser.Parser
import zh.fsm.smc.parser.ParserEvent
import zh.fsm.smc.parser.SyntaxBuilder
import zh.fsm.smc.semanticAnalyser.SemanticAnalyzer
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

const val ARG_SCHEMA = "o*,l*,f&"

fun main(args: Array<String>) {
    try {
        val argParser = Args(ARG_SCHEMA, args)
        SmcCompiler(args, argParser).run()
    } catch (e: ArgsException) {
        println("Usage: $ARG_SCHEMA file")
        println(e.message)
        exitProcess(0)
    }
}

class SmcCompiler(private val args: Array<String>, private val argParser: Args) {
    private var outputDirectory: String = ""
    private var language = "Java"
    private val flags = hashMapOf<String, String>()
    private lateinit var syntaxBuilder: SyntaxBuilder
    private lateinit var parser: Parser
    private lateinit var lexer: Lexer

    @Throws(IOException::class)
    fun run() {
        extractCommandLineArguments()

        val sourceCode = getSourceCode()
        val fsm = compile(sourceCode)
        val syntaxErrorCount = reportSyntaxErrors(fsm)

        if (syntaxErrorCount == 0)
            generateCode(optimize(fsm))
    }

    private fun extractCommandLineArguments() {
        if (argParser.has('o'))
            outputDirectory = argParser.getString('o')
        if (argParser.has('l'))
            language = argParser.getString('l')
        if (argParser.has('f'))
            flags.putAll(argParser.getMap('f'))
    }

    private fun getSourceCode(): String {
        val sourceFileName = args[argParser.nextArgument()]
        return Files.readString(Paths.get(sourceFileName))
    }

    private fun compile(smContent: String): FsmSyntax {
        syntaxBuilder = SyntaxBuilder()
        parser = Parser(syntaxBuilder)
        lexer = FsmLexer(parser)
        lexer.lex(smContent)
        parser.handleEvent(ParserEvent.EOF, -1, -1)
        return syntaxBuilder.getFsm()
    }

    private fun reportSyntaxErrors(fsm: FsmSyntax): Int {
        val syntaxErrorCount = fsm.errors.size
        println("Compiled with $syntaxErrorCount error${if (syntaxErrorCount == 1) "" else "s"}")
        fsm.errors.map(FsmSyntax.SyntaxError::toString).forEach(::println)
        return syntaxErrorCount
    }

    private fun optimize(fsm: FsmSyntax): OptimizedStateMachine =
        SemanticAnalyzer().analyze(fsm).run { Optimizer().optimize(this) }

    @Throws(IOException::class)
    private fun generateCode(optimizedStateMachine: OptimizedStateMachine) {
        val generatorClassName = "zh.fsm.smc.generators.${language}CodeGenerator"
        val generator = createGenerator(generatorClassName, optimizedStateMachine)
        generator.generate()
    }

    private fun createGenerator(
        generatorClassName: String,
        optimizedStateMachine: OptimizedStateMachine
    ): CodeGenerator {
        try {
            val generatorClass = Class.forName(generatorClassName)
            val constructor =
                generatorClass.getConstructor(OptimizedStateMachine::class.java, String::class.java, Map::class.java)
            return constructor.newInstance(optimizedStateMachine, outputDirectory, flags) as CodeGenerator
        } catch (e: ClassNotFoundException) {
            println("The class $generatorClassName was not found.\n")
            exitProcess(0)
        } catch (e: NoSuchMethodException) {
            println("Appropriate constructor for $generatorClassName not found.\n")
            exitProcess(0)
        } catch (e: InvocationTargetException) {
            e.printStackTrace();
            exitProcess(0);
        } catch (e: InstantiationException) {
            e.printStackTrace()
            exitProcess(0)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            exitProcess(0)
        }
    }
}
