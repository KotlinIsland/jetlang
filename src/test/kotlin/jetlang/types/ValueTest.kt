package jetlang.types

import jetlang.fraction.Fraction
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.*

class ValueTest {
    @Test
    fun `number text content`() {
        // no decimal
        assertEquals("1", NumberJL(BigDecimal.ONE).textContent())
        // non-recurring
        assertEquals("1.5", NumberJL("1.5".toBigDecimal()).textContent())
        // tiny value past display cutoff
        assertEquals(
            "0.00000000000000000000000000000000000001",
            NumberJL("0.00000000000000000000000000000000000001".toBigDecimal()).textContent()
        )
        // recurring
        assertEquals("1.3333333333333333333333333333", NumberJL(Fraction(4, 3)).textContent())

        // non 1 recurring period
        assertEquals(
            "0.14285714285714285714285714286",
            NumberJL(Fraction(1, 7)).textContent()
        )

        // combination of fraction the is greater than the display cutoff
        assertEquals(
            "0.33333333336333333333333333333",
            NumberJL(
                Fraction(
                    "0.00000000003".toBigDecimal(),
                    BigDecimal.ONE,
                ) + Fraction(
                    1, 3
                )
            ).textContent(),
        )
        // combination of fraction that is smaller than the display cutoff
        assertEquals(
            "0.333333333333333333333333333338",
            NumberJL(
                Fraction(
                    "0.${"0".repeat(NumberJL.FRACTION_DISPLAY_LENGTH)}5".toBigDecimal(),
                    BigDecimal.ONE
                ) + Fraction(
                    1, 3
                )
            ).textContent(),
        )
    }

    @Test
    fun `sequence text content`() {
        assertEquals(
            SequenceJL(listOf(NumberJL(1), NumberJL(2), NumberJL(3))).textContent(),
            "{1 2 3}"
        )
    }

    @Test
    fun `range sequence text content`() {
        assertEquals(RangeSequenceJL(BigInteger.ONE, 3.toBigInteger()).textContent(), "{1, 3}")
    }
}
