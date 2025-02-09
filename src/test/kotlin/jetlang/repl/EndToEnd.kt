package jetlang.repl

import androidx.compose.ui.test.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TestEndToEnd {
    @Test
    fun reduce() = runReplTest {

        val command = "reduce(range, initial, previous next -> previous + next * next)"
        onNodeWithText("Enter command").performTextInput(
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
        onNodeWithText("Evaluate").performClick()
        outputSection.onChildren().apply {
            assertCountEquals(7)
            get(0).assertTextEquals("range =")
            get(1).assertTextEquals("{4 5 6 7 8 9 10}")
            get(2).assertTextEquals("initial =")
            get(3).assertTextEquals("3")
            get(4).assertTextEquals("computing: $command")
            get(5).assertTextEquals("and the result is.....")
            get(6).assertTextEquals("142852004")
        }
    }
}
