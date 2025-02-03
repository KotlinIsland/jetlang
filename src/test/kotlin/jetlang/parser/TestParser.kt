package jetlang.parser

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

fun parseSingle(input: String) = parseText(input).getOrThrow().nodes.single()
infix fun String.assertParsesAs(node: AstNodeBase) = assertEquals(node, parseSingle(this))

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
        value assertParsesAs ExpressionStatement(NumberLiteral(BigDecimal(value)))
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
        "{1, 2}" assertParsesAs ExpressionStatement(SequenceLiteral(NumberLiteral(BigDecimal.ONE), NumberLiteral(BigDecimal.TWO)))
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
            Parse error at 1:1 (Choice4Parser)

            No inputs matched
            
            1|<>
            >>^""".trimIndent(),
            exception.message,
        )
    }
}
