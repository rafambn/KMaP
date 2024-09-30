import androidx.compose.ui.window.ComposeUIViewController
import com.rafambn.kmapdemo.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
