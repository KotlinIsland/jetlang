package jetlang.repl

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilDoesNotExist
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TestEndToEnd {
    @Test
    fun reduce() = runComposeUiTest {
        setup()

        val command = "computing: reduce(start, initial, previous next -> previous + next * next)"
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
    }
}
