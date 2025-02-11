package jetlang.fraction

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

class Fraction(numerator: BigInteger, denominator: BigInteger) : Comparable<Fraction> {

    val numerator: BigInteger
    val denominator: BigInteger

    init {
        if (denominator == BigInteger.ZERO) {
            throw ArithmeticException("Denominator cannot be zero")
        }

        val gcd = numerator.gcd(denominator)
        val simplifiedNumerator = numerator / gcd
        val simplifiedDenominator = denominator / gcd

        if (simplifiedDenominator < BigInteger.ZERO) {
            this.numerator = -simplifiedNumerator
            this.denominator = -simplifiedDenominator
        } else {
            this.numerator = simplifiedNumerator
            this.denominator = simplifiedDenominator
        }
    }

    constructor(numerator: Int, denominator: Int) : this(
        numerator.toBigInteger(),
        denominator.toBigInteger()
    )

    val isInt: Boolean
        get() = denominator == BigInteger.ONE

    operator fun plus(other: Fraction): Fraction {
        val newNumerator =
            (this.numerator * other.denominator) + (other.numerator * this.denominator)
        val newDenominator = this.denominator * other.denominator
        return Fraction(newNumerator, newDenominator)
    }

    operator fun minus(other: Fraction): Fraction {
        val newNumerator =
            (this.numerator * other.denominator) - (other.numerator * this.denominator)
        val newDenominator = this.denominator * other.denominator
        return Fraction(newNumerator, newDenominator)
    }

    operator fun times(other: Fraction): Fraction {
        val newNumerator = this.numerator * other.numerator
        val newDenominator = this.denominator * other.denominator
        return Fraction(newNumerator, newDenominator)
    }

    operator fun div(other: Fraction): Fraction {
        if (other.numerator == BigInteger.ZERO) {
            throw ArithmeticException("Cannot divide by zero fraction")
        }

        val newNumerator = this.numerator * other.denominator
        val newDenominator = this.denominator * other.numerator
        return Fraction(newNumerator, newDenominator)
    }

    infix fun pow(exponent: Int): Fraction {
        if (exponent == 0) return Fraction(1, 1)

        return if (exponent > 0) {
            Fraction(this.numerator.pow(exponent), this.denominator.pow(exponent))
        } else {
            Fraction(this.denominator.pow(-exponent), this.numerator.pow(-exponent))
        }
    }

    fun toBigInteger(): BigInteger {
        if (denominator != BigInteger.ONE) {
            throw ArithmeticException("Not an integer")
        }
        return numerator
    }

    fun toString(decimalDigits: Int): String {
        val decimalNumerator = numerator.toBigDecimal()
        val decimalDenominator = denominator.toBigDecimal()
        val maxScale = listOf(decimalNumerator.precision(), decimalDenominator.precision(), decimalDigits).max()
        return try {
            decimalNumerator.divide(decimalDenominator)
        } catch (e: ArithmeticException) {
            // when the result is recurring
            decimalNumerator.divide(decimalDenominator, MathContext(maxScale, RoundingMode.HALF_UP))
        }.toPlainString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Fraction) return false

        return numerator * other.denominator == other.numerator * denominator
    }

    override fun hashCode(): Int {
        var result = numerator.hashCode()
        result = 31 * result + denominator.hashCode()
        return result
    }

    override fun toString(): String {
        return "$numerator/$denominator"
    }

    override fun compareTo(other: Fraction): Int {
        // Cross-multiply to compare: (a/b) compareTo (c/d)  =>  (a*d) compareTo (c*b)
        val left = this.numerator * other.denominator
        val right = other.numerator * this.denominator
        return left.compareTo(right)
    }
}

fun Fraction(numerator: BigDecimal, denominator: BigDecimal) : Fraction {
    val maxScale = maxOf(numerator.scale(), denominator.scale())

    // Multiply both by 10^maxScale to remove decimal places
    val num = numerator.movePointRight(maxScale).toBigIntegerExact()
    val den = denominator.movePointRight(maxScale).toBigIntegerExact()
    return Fraction(num, den)
}