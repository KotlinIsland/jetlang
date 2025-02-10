@file:OptIn(ExperimentalTestApi::class)

package jetlang.repl

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assert
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

fun ComposeUiTest.setup() {
    setContent {
        Repl()
    }
}

fun runReplTest(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
    setup()
    block()
}

fun ComposeUiTest.evaluate(command: String) {
    inputField.performTextInput(command)
    onNodeWithText("Evaluate").performClick()
}

val ComposeUiTest.outputSection
    get() = onNodeWithTag("output_section", useUnmergedTree = true)

val ComposeUiTest.inputField
    get() = onNodeWithTag("input_field")

fun hasTextSelectionRange(value: TextRange) = SemanticsMatcher.expectValue(
    SemanticsProperties.TextSelectionRange, value
)

class ReplTest {
    @Test
    fun `test initial state`() = runReplTest {
        onNodeWithTag("history").assertExists()
        onNodeWithText("Enter command").apply {
            assertIsDisplayed()
            assertIsFocused()
        }
        onNodeWithText("Evaluate").assertIsDisplayed()
    }

    @Test
    fun `evaluate button`() = runReplTest {
        val line1 = "out 1"
        val line2 = "out 2"
        evaluate("$line1\n$line2")
        onNodeWithText(
            """
            >>> $line1
            ... $line2
            """.trimIndent()
        ).assertExists()
        onNodeWithText("Enter command").assertIsFocused()
        onNodeWithText("Evaluate").assertExists()
    }

    @Test
    fun `test ctrl enter`() = runReplTest {
        val command = "ctrl enter test"
        onNodeWithText("Enter command").performTextInput(command)

        onNodeWithText(command)
            .performKeyInput {
                withKeyDown(Key.CtrlLeft) { pressKey(Key.Enter) }
            }
        waitUntilExactlyOneExists(hasText(">>> $command"))
        onNodeWithText("Enter command").assertIsFocused()
    }

    @Test
    fun `cancel button`() = runReplTest {
        mainClock.autoAdvance = false
        // an operation that will take a non-trivial amount of time
        evaluate("""
            print "starting"
            var sequence = map({0, 5000}, i -> 1^i / i)
            """.trimIndent())
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Evaluate").assertExists()
        onNodeWithText("starting").assertExists()
        onNodeWithText("Canceled").assertExists()
    }

    @Test
    fun `implicit expression`() = runReplTest {
        evaluate(
            """
            1
            out 2
            3
            4
            """.trimIndent()
        )

        outputSection.onChildren().apply {
            get(0).assertTextEquals("2")
            get(1).assertTextEquals("4")
            assertCountEquals(2)
        }
    }

    @Test
    fun `error shows in red`() = runReplTest {
        evaluate("test")
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        outputSection.onChildren().fetchSemanticsNodes()
            .first()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(textLayoutResults)
        assertEquals(Color.Red, textLayoutResults.first().layoutInput.style.color)
    }

    @Test
    fun `scroll to bottom button`() = runReplTest {
        repeat(10) {
            // fill the history till it is scrollable
            evaluate("$it")
        }
        onNodeWithTag("history_list").performScrollToIndex(0)
        val scrollButton = "scroll_to_bottom_button"
        waitUntilExactlyOneExists(hasTestTag(scrollButton))
        onNodeWithTag(scrollButton).performClick()
        onNodeWithTag(scrollButton).assertDoesNotExist()
        onNodeWithText("9").assertExists()
    }

    @Test
    fun `copy button`() = runReplTest {
        evaluate("1")
        waitForIdle()
        val copyButton = onNodeWithTag("copy_button")
        copyButton.assertDoesNotExist()

        outputSection.performMouseInput {
            enter(center)
        }
        copyButton.assertIsDisplayed()

        copyButton.performMouseInput {
            exit()
            enter(center)
        }
        onNodeWithText("Copy to Clipboard").assertExists()
    }

    @Test
    fun `history navigation`() = runReplTest {
        evaluate("1")
        evaluate("2")
        evaluate("3")
        inputField.assertTextEquals(
            "Enter command",
            includeEditableText = false
        )
        fun up() = inputField.performKeyInput {
            pressKey(Key.DirectionUp)
        }

        fun down() = inputField.performKeyInput {
            pressKey(Key.DirectionDown)
        }
        inputField.performKeyInput {
            pressKey(Key.DirectionUp)
        }
        inputField.assertTextEquals("3")
        up()
        inputField.assertTextEquals("2")
        down()
        inputField.assertTextEquals("3")
        down()
        inputField.assertTextEquals(
            "Enter command",
            includeEditableText = false
        )
        val multilineText = "4\n5"
        inputField.performTextInput(multilineText)
        up()
        inputField.assertTextEquals(multilineText)

        // Assert that the cursor is at the end of the first line
        inputField.assert(hasTextSelectionRange(TextRange(1)))
        up()
        val thirdMultilineText = "3\nlast"
        inputField.performTextClearance()
        inputField.performTextInput(thirdMultilineText)
        up()
        inputField.assertTextEquals(thirdMultilineText)
        down()
        // ensure down from a not-last line will not navigate
        inputField.assertTextEquals(thirdMultilineText)
        down()
        inputField.performTextInput(multilineText)
    }

    @Test
    fun `state is preserved across evaluations`() = runReplTest {
        evaluate("var a = 1")
        evaluate("out a")
        onNodeWithText("1").assertExists()
    }
}
