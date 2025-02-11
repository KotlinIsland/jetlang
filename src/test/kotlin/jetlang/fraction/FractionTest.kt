package jetlang.fraction

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.*

class FractionTest {

    @Test
    fun testAddition() {
        val f1 = Fraction(1, 2)
        val f2 = Fraction(1, 3)
        assertEquals(Fraction(5, 6), f1 + f2)

        val f3 = Fraction(3, 4)
        val f4 = Fraction(1, 4)
        assertEquals(Fraction(1, 1), f3 + f4)

        val f5 = Fraction(-1, 2)
        val f6 = Fraction(1, 2)
        assertEquals(Fraction(0, 1), f5 + f6)

        val f7 = Fraction(1, 2)
        val f8 = Fraction(-1, 2)
        assertEquals(Fraction(0, 1), f7 + f8)

        val f9 = Fraction(-1, 2)
        val f10 = Fraction(-1, 2)
        assertEquals(Fraction(-1, 1), f9 + f10)


    }

    @Test
    fun testSubtraction() {
        val f1 = Fraction(1, 2)
        val f2 = Fraction(1, 3)
        assertEquals(Fraction(1, 6), f1 - f2)

        val f3 = Fraction(3, 4)
        val f4 = Fraction(1, 4)
        assertEquals(Fraction(1, 2), f3 - f4)

        val f5 = Fraction(-1, 2)
        val f6 = Fraction(1, 2)
        assertEquals(Fraction(-1, 1), f5 - f6)


    }

    @Test
    fun testMultiplication() {
        val f1 = Fraction(1, 2)
        val f2 = Fraction(2, 3)
        assertEquals(Fraction(1, 3), f1 * f2)

        val f3 = Fraction(3, 4)
        val f4 = Fraction(-1, 4)
        assertEquals(Fraction(-3, 16), f3 * f4)
    }

    @Test
    fun testDivision() {
        val f1 = Fraction(1, 2)
        val f2 = Fraction(1, 3)
        assertEquals(Fraction(3, 2), f1 / f2)

        val f3 = Fraction(3, 4)
        val f4 = Fraction(1, 4)
        assertEquals(Fraction(3, 1), f3 / f4)

        val f5 = Fraction(1, 2)
        val f6 = Fraction(-3, 4)
        assertEquals(Fraction(-2, 3), f5 / f6)
    }

    @Test
    fun testDivisionByZero() {
        assertFailsWith<ArithmeticException> {
            val f1 = Fraction(1, 2)
            val f2 = Fraction(0, 1)
            f1 / f2
        }
    }

    @Test
    fun testZeroDenominatorInitialization() {
        assertFailsWith<ArithmeticException> {
            Fraction(1, 0)
        }
    }


    @Test
    fun testPower() {
        val f1 = Fraction(2, 3)
        assertEquals(Fraction(4, 9), f1 pow 2)

        val f2 = Fraction(2, 3)
        assertEquals(Fraction(1, 1), f2 pow 0)

        val f3 = Fraction(3, 2)
        assertEquals(Fraction(4, 9), f3 pow -2)

        val f4 = Fraction(1, 2)
        assertEquals(Fraction(1, 8), f4 pow 3)

        val f5 = Fraction(1, 2)
        assertEquals(Fraction(8, 1), f5 pow -3)
    }

    @Test
    fun testEquals() {
        val f1 = Fraction(1, 2)
        val f2 = Fraction(2, 4)
        val f3 = Fraction(1, 3)

        assertEquals(true, f1 == f2)
        assertEquals(false, f1 == f3)
    }

    @Test
    fun testSimplification() {
        val f1 = Fraction(4, 8)
        assertEquals(BigInteger.ONE, f1.numerator)
        assertEquals(BigInteger.valueOf(2), f1.denominator)

        val f2 = Fraction(-6, 9)
        assertEquals(BigInteger.valueOf(-2), f2.numerator)
        assertEquals(BigInteger.valueOf(3), f2.denominator)

        val f3 = Fraction(5, -10)  //Negative Denominator
        assertEquals(BigInteger.valueOf(-1), f3.numerator)
        assertEquals(BigInteger.valueOf(2), f3.denominator)

        val f4 = Fraction(-5, -15)
        assertEquals(BigInteger.ONE, f4.numerator)
        assertEquals(BigInteger.valueOf(3), f4.denominator)
    }


    @Test
    fun testToString() {
        val f1 = Fraction(1, 2)
        assertEquals("1/2", f1.toString())

        val f2 = Fraction(-3, 4)
        assertEquals("-3/4", f2.toString())

        val f3 = Fraction(5, 1)
        assertEquals("5/1", f3.toString())
    }

    @Test
    fun testConstructor() {
        val f1 = Fraction(2, 4)
        assertEquals(Fraction(1, 2), f1)
    }

    @Test
    fun testBigDecimalConstructor() {
        val f1 = Fraction(BigDecimal("1.5"), BigDecimal("2.5"))
        assertEquals(Fraction(3, 5), f1)  // 1.5/2.5 simplifies to 3/5

        val f2 = Fraction(BigDecimal("0.1"), BigDecimal("0.3"))
        assertEquals(Fraction(1, 3), f2)

        val f3 = Fraction(BigDecimal("1.234"), BigDecimal("5.678"))
        assertEquals(Fraction(617, 2839), f3)

        val f4 = Fraction(BigDecimal("-2.4"), BigDecimal("3.6"))
        assertEquals(Fraction(-2, 3), f4)

        val f5 = Fraction(BigDecimal("1"), BigDecimal("3.00"))
        assertEquals(Fraction(1, 3), f5)
    }

    @Test
    fun testComparison() {
        val f1 = Fraction(1, 2)
        val f2 = Fraction(1, 3)
        val f3 = Fraction(2, 4)
        val f4 = Fraction(5,6)

        assertTrue(f1 > f2)
        assertTrue(f1 >= f2)
        assertTrue(f2 < f1)
        assertTrue(f2 <= f1)
        assertTrue(f1 == f3) // Test equality with comparison
        assertTrue(f1 >= f3)
        assertTrue(f1<= f3)
        assertTrue(f1 < f4)

        val f5 = Fraction(-1,2)
        val f6 = Fraction(1,2)

        assertTrue(f5<f6)
    }

    @Test
    fun isInt() {
        val f1 = Fraction(5, 1)
        val f2 = Fraction(10, 2)
        val f3 = Fraction(7, 3)
        val f4 = Fraction(-4, 1)
        val f5 = Fraction(4,-1) //test negative denominator
        val f6 = Fraction(-4, -1)

        assertTrue(f1.isInt)
        assertTrue(f2.isInt) // Simplified to 5/1
        assertFalse(f3.isInt)
        assertTrue(f4.isInt)
        assertTrue(f5.isInt)
        assertTrue(f6.isInt)
    }

    @Test
    fun toBigInteger() {
        assertEquals(BigInteger.ONE, Fraction(2, 2).toBigInteger())
        assertEquals(BigInteger.TWO, Fraction(2, 1).toBigInteger())
        assertFailsWith<ArithmeticException> {
            Fraction(1, 2).toBigInteger()
        }
    }
}