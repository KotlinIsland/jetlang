package jetlang.parser

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class TestParser {
    @Test
    fun `test print`() {
        assertEquals(
            Program(listOf(Print("a"))), parseText("""print "a"""").getOrThrow()
        )
    }

    @Test @Ignore("`Expression` not implemented, so can't test parsing `Out`")
    fun `test out`() {
        assertEquals(
            Program(listOf(Print("a"))), parseText("""out a""").getOrThrow()
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
        val exception = parseText("""a b""").exceptionOrNull()!!
        assertEquals(
            """
            Parse error at 1:2 (EndOfInputParser)

            Expected end of input, but still had input remaining
            
            1|a b
            >>>^""".trimIndent(),
            exception.message,
        )
    }
}
