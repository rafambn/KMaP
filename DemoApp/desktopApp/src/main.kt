import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rafambn.kmap.App
import com.rafambn.kmap.Routes
import java.awt.Dimension

fun main(args: Array<String>) = application {
    Window(
        title = "KMaP Demo",
        state = rememberWindowState(width = 1000.dp, height = 1000.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App(initialRoute = if ("--vector" in args) Routes.VectorTiles else Routes.Start,
            vectorZoom = if ("--vector" in args) 14F else 0F)
    }
}
