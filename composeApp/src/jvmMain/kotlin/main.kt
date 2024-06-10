import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import io.github.rafambn.templateapp.App

fun main() = application {
    Window(
        title = "Template library with App",
        state = rememberWindowState(width = 800.dp, height = 800.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App()
    }
}