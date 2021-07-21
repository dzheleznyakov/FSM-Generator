package zh.fsm.smc.generators

import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor
import zh.fsm.smc.implementers.CppNestedSwitchCaseImplementer
import java.io.IOException
import java.nio.file.Files

class CppCodeGenerator(
    optimizedStateMachine: OptimizedStateMachine,
    outputDirectory: String,
    flags: Map<String, String>
) : CodeGenerator(optimizedStateMachine, outputDirectory, flags) {
    private val implementer = CppNestedSwitchCaseImplementer(flags)

    override fun getImplementer(): NSCNodeVisitor = implementer

    @Throws(IOException::class)
    override fun writeFiles() {
        val outputFileName = "${optimizedStateMachine.header.fsm}.h"
        Files.write(getOutputPath(outputFileName), implementer.output.toByteArray())
    }

}