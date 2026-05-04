import androidx.compose.ui.window.ComposeUIViewController
import com.rafambn.kmap.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
