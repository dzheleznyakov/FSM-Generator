package zh.fsm.smc.generators.nestedSwitchCaseGenerator

interface NSCNode {
    fun accept(visitor: NSCNodeVisitor)

    class SwitchCaseNode(val variableName: String) : NSCNode {
        val caseNodes = arrayListOf<NSCNode>()
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
        fun generateCases(visitor: NSCNodeVisitor) = caseNodes.forEach { c -> c.accept(visitor) }
    }

    class CaseNode(val switchName: String, val caseName: String) : NSCNode {
        lateinit var caseActionNode: NSCNode
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class FunctionCallNode(val functionName: String, val argument: NSCNode? = null) : NSCNode {
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class CompositeNode : NSCNode {
        private val nodes = arrayListOf<NSCNode>()
        override fun accept(visitor: NSCNodeVisitor) = nodes.forEach { node -> node.accept(visitor) }
        fun add(node: NSCNode) = nodes.add(node)
    }

    class EnumNode(val name: String, val enumerators: List<String>) : NSCNode {
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class StatePropertyNode(val initialState: String) : NSCNode {
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class EventDelegatorsNode(val events: List<String>) : NSCNode {
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class FSMClassNode : NSCNode {
        lateinit var delegators: EventDelegatorsNode
        lateinit var eventEnum: EnumNode
        lateinit var stateEnum: EnumNode
        lateinit var stateProperty: StatePropertyNode
        lateinit var handleEvent: HandleEventNode
        lateinit var className: String
        lateinit var actionsName: String
        lateinit var actions: ArrayList<String>

        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class HandleEventNode(val switchCase: SwitchCaseNode) : NSCNode {
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class EnumeratorNode(val enumeration: String, val enumerator: String) : NSCNode {
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }

    class DefaultCaseNode(val state: String) : NSCNode {
        override fun accept(visitor: NSCNodeVisitor) = visitor.visit(this)
    }
}