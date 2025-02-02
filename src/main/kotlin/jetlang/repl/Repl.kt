package jetlang.repl

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jetlang.interpreter.Interpreter
import jetlang.interpreter.Output
import jetlang.parser.parseText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

@Composable
@Preview()
fun Repl() {
    val interpreter = Interpreter()
    val history = remember {
        mutableStateListOf<Pair<String, MutableList<Output>>>()
    }
    var inputFieldText by remember { mutableStateOf(TextFieldValue("")) }
    var isEvaluating by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val inputFocus = remember { FocusRequester() }

    fun updateLast(value: Output) {
        val (input, _) = history.removeLast()
        history.add(input to mutableListOf(value))
    }

    fun appendLast(value: Output) {
        history.last().second.add(value)
    }

    suspend fun evaluate(input: String) {
        isEvaluating = true
        history.add(">>> $input" to mutableListOf())

        parseText(input)
            .getOrElse {
                appendLast(
                    it.message?.let { message ->  Output.Error(message) }
                        ?: Output.Error("something went wrong")
                )
                isEvaluating = false
                return
            }
            .let {
                interpreter.interpret(it)
            }
            .onCompletion { isEvaluating = false }
            .collect {
                appendLast(it)
            }
    }

    val listState = rememberLazyListState()

    fun submit() {
        val input = inputFieldText.text
        if (input.isBlank()) return
        inputFieldText = TextFieldValue()
        job = coroutineScope.launch(Dispatchers.Default) {
            evaluate(input)
        }
    }

    LaunchedEffect(Unit) {
        inputFocus.requestFocus()
    }
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).padding(8.dp).testTag("history")) {
                LazyColumn(state = listState) {
                    items(history.size) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp, bottom = 8.dp)
                                .border(
                                    BorderStroke(Dp.Hairline, Color.LightGray),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            val entry = history[it]
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(entry.first, fontFamily = FontFamily.Monospace)
                                val second = entry.second
                                second.forEach {
                                    Text(
                                        it.value,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (it is Output.Error) Color.Red else Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                    if (isEvaluating) item {
                        CircularProgressIndicator()
                    }
                }
                LaunchedEffect(history.size) {
                    listState.animateScrollToItem(history.size)
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(listState)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(Color.DarkGray).padding(8.dp)
            ) {
                TextField(
                    inputFieldText,
                    onValueChange = { inputFieldText = it },
                    modifier = Modifier.fillMaxWidth().weight(1f).onPreviewKeyEvent {
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyDown && (it.isCtrlPressed || it.isMetaPressed)) {
                            submit()
                            true
                        } else {
                            false
                        }
                    }.focusRequester(inputFocus),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("Enter command") },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                )
                Spacer(Modifier.width(8.dp))
                if (isEvaluating) {
                    ActionButon(
                        "Cancel",
                        secondary = true,
                    ) {
                        // do we want to append this instead of override anything that has already been output
                        updateLast(Output.Error("Canceled"))
                        job?.cancel()
                        isEvaluating = false
                        inputFocus.requestFocus()
                    }
                } else {
                    ActionButon("Evaluate") {
                        submit()
                        inputFocus.requestFocus()
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButon(label: String, secondary: Boolean = false, action: () -> Unit) {
    Button(
        action,
        modifier = Modifier.width(110.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            if (secondary) MaterialTheme.colorScheme.secondary else Color.Unspecified
        )
    ) {
        Text(label)
    }
}
