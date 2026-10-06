import androidx.compose.ui.window.ComposeUIViewController
import com.rafambn.kmap.App
import com.rafambn.kmap.Routes
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val vectorDemo = "--vector" in NSProcessInfo.processInfo.arguments
    App(initialRoute = if (vectorDemo) Routes.VectorTiles else Routes.Start, vectorZoom = if (vectorDemo) 14F else 0F)
}
