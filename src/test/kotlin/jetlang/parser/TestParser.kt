package jetlang.parser

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

fun parseSingle(input: String) = parseText(input).getOrThrow().nodes.single()
infix fun String.assertParsesAs(node: AstNodeBase) = assertEquals(node, parseSingle(this))

operator fun Expression.plus(other: Expression) =
    Operation(this, Operator.ADD, other)

operator fun Expression.minus(other: Expression) =
    Operation(this, Operator.SUBTRACT, other)

operator fun Expression.times(other: Expression) =
    Operation(this, Operator.MULTIPLY, other)

operator fun Expression.div(other: Expression) =
    Operation(this, Operator.DIVIDE, other)

infix fun Expression.pow(other: Expression) =
    Operation(this, Operator.EXPONENT, other)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestParser {
    @Test
    fun `test program`() {
        assertEquals(
            Program(listOf(Out(NumberLiteral(BigDecimal.ONE)))), parseText("out 1").getOrThrow()
        )
    }

    @Test
    fun `test print`() {
        """print "a"""" assertParsesAs Print("a")
    }

    @Test
    fun `test out`() {
        "out 1" assertParsesAs Out(NumberLiteral(BigDecimal.ONE))
    }

    @Test
    fun `test integer number literal`() {
        "1" assertParsesAs ExpressionStatement(NumberLiteral(BigDecimal.ONE))
    }

    @Test
    fun `test real number literal`() {
        val value = "1.1"
        value assertParsesAs ExpressionStatement(NumberLiteral(value.toBigDecimal()))
    }

    @Test
    fun `test var`() {
        "var a = 1" assertParsesAs Var("a", NumberLiteral(BigDecimal.ONE))
    }

    @Test
    fun `test identifier`() {
        "a" assertParsesAs ExpressionStatement(Identifier("a"))
    }

    @Test
    fun `test sequence`() {
        "{1, 2}" assertParsesAs ExpressionStatement(
            SequenceLiteral(
                NumberLiteral(BigDecimal.ONE),
                NumberLiteral(BigDecimal.TWO)
            )
        )
    }

    fun `test simple operators`() = listOf(
        Arguments.of("+", Operator.ADD),
        Arguments.of("-", Operator.SUBTRACT),
        Arguments.of("*", Operator.MULTIPLY),
        Arguments.of("/", Operator.DIVIDE),
        Arguments.of("^", Operator.EXPONENT),
    )

    @ParameterizedTest
    @MethodSource
    fun `test simple operators`(intputOperator: String, outputOperator: Operator) {
        "1 $intputOperator 2" assertParsesAs ExpressionStatement(
            Operation(
                NumberLiteral(1),
                outputOperator,
                NumberLiteral(2)
            )
        )
    }

    val one = NumberLiteral(BigDecimal.ONE)
    val two = NumberLiteral(BigDecimal.TWO)
    val three = NumberLiteral(3)

    fun `test operator precedence`() = listOf(
        // +
        Arguments.of("1 + 2 + 3", (one + two) + three),

        // -
        Arguments.of("1 - 2 - 3", (one - two) - three),

        // *
        Arguments.of("1 * 2 * 3", (one * two) * three),

        // /
        Arguments.of("1 / 2 / 3", (one / two) / three),

        // ^
        Arguments.of("1 ^ 2 ^ 3", (one pow two) pow three),

        // + and -
        Arguments.of("1 + 2 - 3", (one + two) - three),
        Arguments.of("1 - 2 + 3", (one - two) + three),

        // + and *
        Arguments.of("1 + 2 * 3", one + (two * three)),
        Arguments.of("1 * 2 + 3", (one * two) + three),

        // + and /
        Arguments.of("1 + 2 / 3", one + (two / three)),
        Arguments.of("1 / 2 + 3", (one / two) + three),

        // + and ^
        Arguments.of("1 + 2 ^ 3", one + (two pow three)),
        Arguments.of("1 ^ 2 + 3", (one pow two) + three),

        // - and *
        Arguments.of("1 - 2 * 3", one - (two * three)),
        Arguments.of("1 * 2 - 3", (one * two) - three),

        // - and /
        Arguments.of("1 - 2 / 3", one - (two / three)),
        Arguments.of("1 / 2 - 3", (one / two) - three),

        // - and ^
        Arguments.of("1 - 2 ^ 3", one - (two pow three)),
        Arguments.of("1 ^ 2 - 3", (one pow two) - three),

        // * and /
        Arguments.of("1 * 2 / 3", (one * two) / three),
        Arguments.of("1 / 2 * 3", (one / two) * three),

        // * and ^
        Arguments.of("1 * 2 ^ 3", one * (two pow three)),
        Arguments.of("1 ^ 2 * 3", (one pow two) * three),

        // / and ^
        Arguments.of("1 / 2 ^ 3", one / (two pow three)),
        Arguments.of("1 ^ 2 / 3", (one pow two) / three),
    )

    @ParameterizedTest
    @MethodSource
    fun `test operator precedence`(input: String, expectedAst: Expression) {
        input assertParsesAs ExpressionStatement(expectedAst)
    }

    @Test
    fun `reduce simple`() {
        "reduce(a, b, c d -> e)" assertParsesAs ExpressionStatement(
            Reduce(
                Identifier("a"),
                Identifier("b"),
                "c",
                "d",
                Identifier("e"),
            )
        )
    }

    @Test
    fun `reduce invalid`() {
        val exception = parseText("reduce(a, b, c d e -> e)").exceptionOrNull()!!
        assertEquals(
            """
            Parse error at 1:18 (LiteralTokenParser)

            LiteralTokenParser passed predict(), but failed to parse(). Make sure you're using predict() properly, and that the input text has not been modified during parsing. Expected '->', but got ''.

            1|reduce(a, b, c d e -> e)
            >>>>>>>>>>>>>>>>>>>^
            """.trimIndent(),
            exception.message,
        )
    }

    @Test
    fun map() {
        "map(a, b -> c)" assertParsesAs ExpressionStatement(
            MapJL(
                Identifier("a"),
                "b",
                Identifier("c"),
            )
        )
    }

    @Test
    fun parenthesis() {
        "(1)" assertParsesAs ExpressionStatement(
            NumberLiteral(1),
        )
    }

    @Test
    fun `test multiple lines`() {
        assertEquals(
            Program(listOf(Print("a"), Print("b"))),
            parseText(
                """
                print "a"
                print "b"
                """.trimIndent()
            ).getOrThrow()
        )
    }

    @Test
    fun `test multiple statements on same line`() {
        val exception = parseText("""print "a" print"b""").exceptionOrNull()!!
        assertEquals(
            """
            Parse error at 1:10 (EndOfInputParser)

            Expected end of input, but still had input remaining
            
            1|print "a" print"b
            >>>>>>>>>>>^""".trimIndent(),
            exception.message,
        )
    }

    @Test
    fun `test invalid syntax`() {
        val exception = parseText("""<>""").exceptionOrNull()!!
        assertEquals(
            """
            Parse error at 1:1 (Choice6Parser)

            No inputs matched
            
            1|<>
            >>^""".trimIndent(),
            exception.message,
        )
    }
}
