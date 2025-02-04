package jetlang.types

import java.math.BigDecimal


sealed class Value {
    abstract fun textContent(): String
}

data class NumberJL(val value: BigDecimal) : Value() {
    constructor(value: Int) : this(value.toBigDecimal())
    override fun textContent() = value.toString()
}

data class SequenceJL(val start: Int, val end: Int) : Value() {
    override fun textContent() =  "{$start, $end}"
}

