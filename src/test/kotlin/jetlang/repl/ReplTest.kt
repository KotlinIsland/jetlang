@file:OptIn(ExperimentalTestApi::class)

package jetlang.repl

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import kotlin.test.Ignore
import kotlin.test.Test

fun ComposeUiTest.setup() {
    setContent {
        Repl()
    }
}

class ReplTest {

    @Test
    fun `test initial state`() = runComposeUiTest {
        setup()

        onNodeWithTag("history").assertExists()
        onNodeWithText("Enter command").apply {
            assertIsDisplayed()
            assertIsFocused()
        }
        onNodeWithText("Evaluate").assertIsDisplayed()
    }

    @Test
    fun `test submit button`() = runComposeUiTest {
        setup()

        val command = """print "a""""
        onNodeWithText("Enter command").performTextInput(command)
        onNodeWithText("Evaluate").performClick()
        onNodeWithText(""">>> $command""").assertExists()
        onNodeWithText("Enter command").assertIsFocused()
        onNodeWithText("Evaluate").assertExists()
    }

    @Test
    fun `test ctrl enter`() = runComposeUiTest {
        setup()

        val command = "ctrl enter test"
        onNodeWithText("Enter command").performTextInput(command)

        onNodeWithText(command)
            .performKeyInput {
                withKeyDown(Key.CtrlLeft) { pressKey(Key.Enter) }
            }
        waitUntilExactlyOneExists(hasText(">>> $command"))
        onNodeWithText("Enter command").assertIsFocused()
    }

    @Test  @Ignore("cancel button disappears before we can click it")
    fun `test cancel button`() = runComposeUiTest {
        setup()

        mainClock.autoAdvance = false
        onNodeWithText("Enter command").performTextInput("test")
        onNodeWithText("Evaluate").performClick()
        waitUntilDoesNotExist(hasText("Evaluate"))
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Evaluate").assertExists()
        onNodeWithText("Canceled").assertExists()
    }
}
