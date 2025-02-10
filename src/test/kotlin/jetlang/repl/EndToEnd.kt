package jetlang.repl

import androidx.compose.ui.test.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TestEndToEnd {
    @Test
    fun reduce() = runReplTest {

        val command = "reduce(range, initial, previous next -> (previous + previous)*(next + next))"
        evaluate(
            """
            var start = 2
            var end = 10
            var initial = 3
            var range = {start * 2, end}
            print "range ="
            out range
            print "initial ="
            out initial
            
            print "computing: $command"
            var result = $command
            print "and the result is....."
            out result 
            """.trimIndent()
        )
        outputSection.onChildren().apply {
            get(0).assertTextEquals("range =")
            get(1).assertTextEquals("{4 5 6 7 8 9 10}")
            get(2).assertTextEquals("initial =")
            get(3).assertTextEquals("3")
            get(4).assertTextEquals("computing: $command")
            get(5).assertTextEquals("and the result is.....")
            get(6).assertTextEquals("29727129600")
            assertCountEquals(7)
        }
    }

    @Test
    fun pi() = runReplTest {
        evaluate("""
            var n = 5000
            var sequence = map({0, n}, i -> -1^i / (2 * i + 1))
            var pi = 4 * reduce(sequence, 0, x y -> x + y)
            print "pi = "
            out pi
            """.trimIndent())
        waitUntilAtLeastOneExists(hasText("pi = "))
        outputSection.onChildren().apply {
            get(0).assertTextEquals("pi = ")
            get(1).assertTextEquals("3.141792613595792838402639395964")
            assertCountEquals(2)
        }
    }
}
