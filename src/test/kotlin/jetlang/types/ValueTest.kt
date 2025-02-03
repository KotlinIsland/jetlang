package jetlang.types

import java.math.BigDecimal
import kotlin.test.*


class ValueTest {
    @Test
    fun `test number text content`() {
        assertEquals(NumberJL(BigDecimal.ONE).textContent(), "1")
        assertEquals(NumberJL(1.5.toBigDecimal()).textContent(), "1.5")
    }

    @Test
    fun `test sequence text content`() {
        assertEquals(SequenceJL(1..2).textContent(), "{1 2}")
    }
}