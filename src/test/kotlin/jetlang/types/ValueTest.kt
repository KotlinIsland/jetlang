package jetlang.types

import java.math.BigDecimal
import kotlin.test.*


class ValueTest {
    @Test
    fun `number text content`() {
        assertEquals(NumberJL(BigDecimal.ONE).textContent(), "1")
        assertEquals(NumberJL(1.5.toBigDecimal()).textContent(), "1.5")
    }

    @Test
    fun `sequence text content`() {
        assertEquals(SequenceJL(listOf(NumberJL(1), NumberJL(2), NumberJL(3))).textContent(), "{1 2 3}")
    }
    @Test
    fun `range sequence text content`() {
        assertEquals(RangeSequenceJL(1..3).textContent(), "{1, 3}")
    }
}
