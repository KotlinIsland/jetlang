package jetlang.repl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
    val interpreter = remember { Interpreter() }
    val history = remember {
        mutableStateListOf<Pair<String, SnapshotStateList<Output>>>()
    }
    var showScrollButton by remember { mutableStateOf(false) }
    var temporarilyHideScrollButton by remember { mutableStateOf(false) }
    var inputFieldText by remember { mutableStateOf(TextFieldValue("")) }

    var isEvaluating by remember { mutableStateOf(false) }
    var interpreterWorker by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val inputFocus = remember { FocusRequester() }
    val historyListState = rememberLazyListState()
    var tempHistory by remember { mutableStateOf(TextFieldValue("")) }

    fun appendLast(value: Output) {
        history.last().second.add(value)
    }

    var historySelection by remember { mutableIntStateOf(history.size) }
    suspend fun evaluate(input: String) {
        isEvaluating = true
        history.add(input to mutableStateListOf())
        historySelection = history.size
        parseText(input)
            .getOrElse {
                appendLast(
                    it.message?.let { message -> Output.Error(message) }
                        ?: Output.Error("something went wrong")
                )
                isEvaluating = false
                return
            }
            .let { interpreter.interpret(it) }
            .onCompletion { isEvaluating = false }
            .collect { appendLast(it) }
    }


    fun submit() {
        val input = inputFieldText.text
        if (input.isBlank()) return
        inputFieldText = TextFieldValue()
        interpreterWorker = coroutineScope.launch(Dispatchers.Default) {
            evaluate(input)
        }
    }

    LaunchedEffect(Unit) {
        inputFocus.requestFocus()
    }
    LaunchedEffect(
        historyListState.firstVisibleItemIndex,
        historyListState.firstVisibleItemScrollOffset
    ) {
        showScrollButton =
            history.isNotEmpty() && historyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index != historyListState.layoutInfo.totalItemsCount - 1
    }
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).testTag("history")) {
                LazyColumn(
                    Modifier.testTag("history_list"),
                    state = historyListState,
                    contentPadding = PaddingValues(8.dp, 8.dp, 24.dp, 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(history) {
                        HistoryEntry(it)
                    }
                    if (isEvaluating) item {
                        CircularProgressIndicator()
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter)) {
                    AnimatedVisibility(
                        visible = showScrollButton && !temporarilyHideScrollButton,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    historyListState.animateScrollToItem(history.lastIndex)
                                }
                            },
                            Modifier.testTag("scroll_to_bottom_button")
                        ) {
                            Icon(
                                Icons.Filled.ArrowDownward,
                                contentDescription = "Scroll to Bottom"
                            )
                            Text("Scroll to Bottom")
                        }
                    }
                }
                LaunchedEffect(history.size) {
                    temporarilyHideScrollButton = true
                    historyListState.animateScrollToItem(history.size)
                    temporarilyHideScrollButton = false
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)
                        .testTag("scroll_bar"),
                    adapter = rememberScrollbarAdapter(historyListState)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(Color.DarkGray).padding(8.dp)
            ) {
                TextField(
                    inputFieldText,
                    onValueChange = { inputFieldText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(inputFocus)
                        .testTag("input_field")
                        .onPreviewKeyEvent { keyEvent ->
                        fun navigateHistory(amount: Int): Boolean {
                            if (amount == 0) {
                                return false
                            }
                            val selection = inputFieldText.selection.start
                            if ((selection != inputFieldText.selection.end)
                                || if (amount < 0)
                                    selection > (inputFieldText.text.indexOf("\n")
                                        .takeUnless { it == -1 } ?: selection)
                                else selection < inputFieldText.text.lastIndexOf("\n") + 1
                            ) {
                                return false
                            }
                            val newSelection =
                                (history + (tempHistory.text to null)).getOrNull(historySelection + amount) ?: return false
                            if (historySelection == history.size) {
                                tempHistory = inputFieldText
                            }
                            historySelection += amount
                            inputFieldText = TextFieldValue(newSelection.first)
                            return true
                        }
                        if (keyEvent.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }
                        when {
                            keyEvent.key == Key.Enter && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) -> {
                                submit()
                                true
                            }

                            keyEvent.key == Key.DirectionUp -> navigateHistory(
                                -1
                            )

                            keyEvent.key == Key.DirectionDown -> navigateHistory(
                                +1
                            )

                            else -> false
                        }
                    },
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
                        appendLast(Output.Error("Canceled"))
                        interpreterWorker?.cancel()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryEntry(history: Pair<String, SnapshotStateList<Output>>) {
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null // remove ripple effect.  Use rememberRipple() for more control
            ) {} // Empty click listener for hover to work
    ) {

        Column(
            Modifier

                // Here we need to draw a border for the output section, but not obscure the border of the
                // input section
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val stroke = 1.dp.toPx()
                        val radius = 8f
                        val paint = Paint().apply {
                            color = Color.LightGray
                            strokeWidth = 1f
                        }

                        canvas.drawLine(
                            Offset(0f, 15f),
                            Offset(0f, size.height - radius),
                            paint = paint,
                        )
                        drawArc(
                            color = Color.LightGray,
                            startAngle = 90f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(0f, size.height - radius * 2),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(stroke)
                        )
                        canvas.drawLine(
                            Offset(radius, size.height),
                            Offset(size.width - radius, size.height),
                            paint = paint,
                        )
                        drawArc(
                            color = Color.LightGray,
                            startAngle = 90f,
                            sweepAngle = -90f,
                            useCenter = false,
                            topLeft = Offset(size.width - 2 * radius, size.height - radius * 2),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(stroke)
                        )
                        canvas.drawLine(
                            Offset(size.width, size.height - radius),
                            Offset(size.width, 15f),
                            paint = paint,
                        )
                    }
                }

        ) {
            Text(
                history.first.split('\n').joinToString("\n... ", prefix = ">>> "),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(Dp.Hairline, Color(0xa0, 0xa0, 0xa0), RoundedCornerShape(8.dp))
                    .background(Color(0xf0, 0xf0, 0xf0), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(50.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)

                ) {
                    SelectionContainer(Modifier.testTag("output_section")) {
                        history.second.forEach {
                            Text(
                                it.value,
                                fontFamily = FontFamily.Monospace,
                                color = if (it is Output.Error) Color.Red else Color.Unspecified,
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = isHovered,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TooltipArea(
                        tooltip = {
                            Surface(
                                modifier = Modifier.shadow(4.dp),
                                color = Color.White
                            ) {
                                Text(
                                    text = "Copy to Clipboard",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        },
                        delayMillis = 500
                    ) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(
                                    AnnotatedString(history.second.joinToString("\n") { it.value })
                                )
                            },
                            Modifier.testTag("copy_button"),
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copy to clipboard",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium),
                            )
                        }
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
