package zh.fsm.smc.generators

import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCGenerator
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path

abstract class CodeGenerator(
    protected val optimizedStateMachine: OptimizedStateMachine,
    protected val outputDirectory: String?,
    protected val flags: Map<String, String>
) {
    protected fun getOutputPath(outputFileName: String): Path =
        if (outputDirectory == null) FileSystems.getDefault().getPath(outputFileName)
        else FileSystems.getDefault().getPath(outputDirectory, outputFileName)

    @Throws(IOException::class)
    fun generate(): Unit {
        val nscGenerator = NSCGenerator()
        nscGenerator.generate(optimizedStateMachine)
        writeFiles();
    }

    protected abstract fun getImplementer(): NSCNodeVisitor

    @Throws(IOException::class)
    protected abstract fun writeFiles()
}