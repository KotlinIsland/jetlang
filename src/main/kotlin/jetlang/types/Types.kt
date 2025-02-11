package jetlang.types

import jetlang.fraction.Fraction
import java.math.BigDecimal
import java.math.BigInteger


sealed class Value {
    abstract fun textContent(): String
}


data class NumberJL(val value: Fraction) : Value() {
    constructor(value: BigInteger) : this(Fraction(value, BigInteger.ONE))
    constructor(value: BigDecimal) : this(Fraction(value, BigDecimal.ONE))
    constructor(value: Int) : this(value.toBigDecimal())

    override fun textContent() = value.toString(FRACTION_DISPLAY_LENGTH)
    companion object {
        const val FRACTION_DISPLAY_LENGTH = 29
    }
}

sealed class SequenceJL : Value() {
    abstract fun valueIterator(): Iterator<Value>
    abstract fun toList(): List<Value>

}

fun SequenceJL(values: List<NumberJL>) = SequenceJLImpl(values)

data class SequenceJLImpl(val values: List<NumberJL>) : SequenceJL() {
    override fun valueIterator() = values.iterator()
    override fun toList() = values
    override fun textContent() =
        values.joinToString(" ", prefix = "{", postfix = "}") { it.textContent() }
}

data class RangeSequenceJL(val first: BigInteger, val last: BigInteger) : SequenceJL() {
    override fun valueIterator() = iterator {
            var i = first
            while (i <= last) {
                yield(NumberJL(i))
                i++
            }
        }

    override fun toList() = valueIterator().asSequence().toList()
    override fun textContent() = "{$first, $last}"
}
