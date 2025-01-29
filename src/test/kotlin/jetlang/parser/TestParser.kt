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
            Program(listOf(Out(NumberLiteral(BigDecimal(1))))), parseText("out 1").getOrThrow()
        )
    }

    @Test
    fun `test print`() {
        """print "a"""" assertParsesAs Print("a")
    }

    @Test
    fun `test out`() {
        "out 1" assertParsesAs Out(NumberLiteral(BigDecimal(1)))
    }

    @Test
    fun `test integer number literal`() {
        "1" assertParsesAs ExpressionStatement(NumberLiteral(BigDecimal(1)))
    }

    @Test
    fun `test real number literal`() {
        "1.1" assertParsesAs ExpressionStatement(NumberLiteral(BigDecimal("1.1")))
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
