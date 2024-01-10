import androidx.compose.ui.window.ComposeUIViewController
import io.github.rafambn.templateapp.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
