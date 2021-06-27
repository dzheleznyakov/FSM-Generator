package zh.fsm.smc.lexer

interface TokenCollector {
    fun openBrace(line: Int, pos: Int)
    fun closedBrace(line: Int, pos: Int)
    fun openParen(line: Int, pos: Int)
    fun closedParen(line: Int, pos: Int)
    fun openAngle(line: Int, pos: Int)
    fun closedAngle(line: Int, pos: Int)
    fun dash(line: Int, pos: Int)
    fun colon(line: Int, pos: Int)
    fun name(name: String, line: Int, pos: Int)
    fun error(line: Int, pos: Int)
}