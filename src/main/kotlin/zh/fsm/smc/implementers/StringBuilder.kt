package zh.fsm.smc.implementers

private const val INDENT_SIZE = 4
private var indentationLevel = 0
private var indentOn = true

operator fun StringBuilder.inc(): StringBuilder {
    ++indentationLevel
    return this
}

operator fun StringBuilder.dec(): StringBuilder {
    --indentationLevel
    return this
}

fun indentOn() {
  indentOn = true
}

fun indentOff() {
    indentOn = false
}

operator fun StringBuilder.plusAssign(str: String) {
    if (indentOn)
        repeat(indentationLevel * INDENT_SIZE) { append(' ') }
    append(str)
}
