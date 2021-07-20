package zh.fsm.smc.generators

import zh.fsm.smc.OptimizedStateMachine
import zh.fsm.smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor
import zh.fsm.smc.implementers.CNestedSwitchCaseImplementer
import java.io.IOException
import java.nio.file.Files

class CCodeGenerator(
    override val optimizedStateMachine: OptimizedStateMachine,
    outputDirectory: String,
    flags: Map<String, String>
) : CodeGenerator(optimizedStateMachine, outputDirectory, flags) {
    private val implementer = CNestedSwitchCaseImplementer(flags)

    override fun getImplementer(): NSCNodeVisitor = implementer

    @Throws(IOException::class)
    override fun writeFiles() {
        if (implementer.errors.isNotEmpty())
            implementer.errors.forEach { error -> println("Implementation error: ${error.name}") }
        else {
            val fileName = optimizedStateMachine.header.fsm.lowercase()
            Files.write(getOutputPath("$fileName.h"), implementer.fsmHeader.toByteArray())
            Files.write(getOutputPath("$fileName.c"), implementer.fsmImplementation.toByteArray())
        }
    }
}