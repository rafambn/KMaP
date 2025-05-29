import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import com.rafambn.kmap.App

fun main() = application {
    Window(
        title = "KMaP Demo",
        state = rememberWindowState(width = 1000.dp, height = 1000.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App()
    }
}
