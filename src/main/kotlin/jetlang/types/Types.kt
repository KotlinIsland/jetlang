package jetlang.types

import java.math.BigDecimal


sealed class Type {
    abstract fun textContent(): String
}

data class NumberJL(val value: BigDecimal) : Type() {
    override fun textContent() = value.toString()
}

