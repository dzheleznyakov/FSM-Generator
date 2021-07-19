package zh.fsm.smc.generators

import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor
import zh.fsm.smc.implementers.JavaNestedSwitchCaseImplementer
import java.io.IOException
import java.nio.file.Files

class JavaCodeGenerator(
    override val optimizedStateMachine: OptimizedStateMachine,
    outputDirectory: String?,
    flags: Map<String, String>
) : CodeGenerator(optimizedStateMachine, outputDirectory, flags) {
    private val implementer = JavaNestedSwitchCaseImplementer(flags)

    override fun getImplementer(): NSCNodeVisitor = implementer

    @Throws(IOException::class)
    override fun writeFiles() {
        val outputFileName = "${optimizedStateMachine.header.fsm}.java"
        Files.write(getOutputPath(outputFileName), implementer.output.toByteArray())
    }
}