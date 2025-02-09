package jetlang.interpreter

import jetlang.parser.Expression
import jetlang.parser.Identifier
import jetlang.parser.NumberLiteral
import jetlang.parser.Reduce
import jetlang.parser.SequenceLiteral
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test
import jetlang.utility.*
import kotlin.test.Ignore

suspend fun assertAssociative(expression: Expression) {
    assertTrue(isAssociative(expression, "y", "z"))
}

suspend fun assertDisassociative(expression: Expression) {
    assertFalse(isAssociative(expression, "y", "z"))
}

suspend fun simplify(expression: Expression) = expression.accept(AssociativeSimplifier("y", "z"))

class IsAssociativeTest {
    val a = Identifier("y")
    val b = Identifier("z")
    val c = NumberLiteral(2)

    // constants
    @Test
    fun constant() = runTest {
        assertAssociative(NumberLiteral(1))
    }

    @Test
    fun `constant addition`() = runTest {
        assertAssociative(NumberLiteral(1) + NumberLiteral(2))
    }

    @Test
    fun `constant subtraction`() = runTest {
        assertAssociative(NumberLiteral(1) - NumberLiteral(2))
    }

    @Test
    fun `constant multiplication`() = runTest {
        assertAssociative(NumberLiteral(2) * NumberLiteral(3))
    }


    @Test
    fun `constant division`() = runTest {
        assertAssociative(NumberLiteral(2) / NumberLiteral(3))
    }

    @Test
    fun `constant exponentiation`() = runTest {
        assertAssociative(NumberLiteral(2) pow NumberLiteral(3))
    }

    // simple positive cases
    @Test
    fun `a b addition`() = runTest {
        assertAssociative(a + b)
    }

    @Test
    fun `a b multiplication`() = runTest {
        assertAssociative(a * b)
    }

    @Test
    fun `a + b + c`() = runTest {
        assertAssociative(a + b + NumberLiteral(3))
    }

    @Test
    fun `a b c with multiplication`() = runTest {
        assertAssociative(a * b * NumberLiteral(3))
    }

    // simple negative cases
    @Test
    fun subtraction() = runTest {
        assertDisassociative(a - b)
        assertDisassociative(b - a)
    }

    @Test
    fun division() = runTest {
        assertDisassociative(a/b)
        assertDisassociative(b/a)
    }

    @Test
    fun exponentiation() = runTest {
        assertDisassociative(a pow b)
        assertDisassociative(b pow a)
    }

    @Test
    fun `only a`() = runTest {
        assertDisassociative(a)
        assertDisassociative(a + NumberLiteral(3))
    }

    @Test
    fun `only b`() = runTest {
        assertDisassociative(b)
        assertDisassociative(b + NumberLiteral(3))
    }

    // negative mixed operator
    @Test
    fun `a + b times c`() = runTest {
        assertDisassociative((a + b) * NumberLiteral(3))
    }

    @Test
    fun `a times b + c`() = runTest {
        assertDisassociative((a * b) + NumberLiteral(3))
    }

    // negative unbalanced
    @Test
    fun `a + b + b`() = runTest {
        assertDisassociative(a + b + b)
    }

    // positive via simplification
    @Test
    fun `minus 0`() = runTest {
        assertAssociative((a - NumberLiteral(0)) * b)
    }

    @Test
    fun `times 0`() = runTest {
        assertAssociative(a * NumberLiteral(0))
    }

    @Test
    fun `times 1`() = runTest {
        assertAssociative((a * NumberLiteral(1)) + b)
    }

    @Test
    fun `divide 1`() = runTest {
        assertAssociative(a / NumberLiteral(1) + b)
    }

    @Test
    fun `pow 0`() = runTest {
        assertAssociative(a pow NumberLiteral(0))
    }

    @Test
    fun `pow 1`() = runTest {
        assertAssociative((a pow NumberLiteral(1)) + b)
    }

    @Test
    fun `simplify a + a`() = runTest {
        assertEquals(a * c, simplify(a + a))
        assertEquals(b * c, simplify(b + b))
    }

    @Test
    fun `simplify a + c`() = runTest {
        assertEquals(a, simplify(a + c))
        assertEquals(a, simplify(c + a))
        assertEquals(b, simplify(b + c))
        assertEquals(b, simplify(c + b))
    }

    @Test
    fun `simplify c + c`() = runTest {
        assertEquals(NumberLiteral(3), simplify(NumberLiteral(3) + c))
    }

    @Test
    fun `simplify c times c`() = runTest {
        assertEquals(NumberLiteral(3), simplify(NumberLiteral(3) * c))
    }

    // positive complex
    @Test
    fun `nested reduction via addition normalisation to multiplication`() = runTest {
        assertAssociative((a + a) * (b + b) * NumberLiteral(7))
    }

    @Test
    fun `n added operand`() = runTest {
        assertAssociative(a * (b + b + b))
    }

    @Test
    fun `add then sub`() = runTest {
        assertAssociative(a + (b + b - b))
    }

    @Test
    fun reduce() = runTest  {
        assertDisassociative(Reduce(SequenceLiteral(a, b), NumberLiteral(1), "a", "b", Identifier("a") + Identifier("b")))
    }

    @Test @Ignore("not implemented yet")
    fun `positive reduce`() = runTest  {
        assertAssociative(Reduce(NumberLiteral(1), NumberLiteral(1), "a", "b", NumberLiteral(1)))
    }
}