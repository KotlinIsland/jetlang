package jetlang.types

import java.math.BigDecimal


sealed class Value {
    abstract fun textContent(): String
}

data class NumberJL(val value: BigDecimal) : Value() {
    constructor(value: Int) : this(value.toBigDecimal())

    override fun textContent() = value.toString()
}

data class SequenceJL(val values: List<Value>) : Value() {
    constructor(values: IntRange) : this(values.toList().map { NumberJL(it) })
    override fun textContent() =
        values.joinToString(" ", prefix = "{", postfix = "}") { it.textContent() }
}
