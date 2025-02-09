package jetlang

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jetlang.repl.Repl

fun main() = application {
    @Suppress("DEPRECATION")
    Window(title="JetLang", onCloseRequest = ::exitApplication, icon = painterResource("icon.ico")) {
        Repl()
    }
}
