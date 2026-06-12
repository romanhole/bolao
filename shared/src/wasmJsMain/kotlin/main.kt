import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.bolao.di.networkModule
import com.bolao.di.repositoryModule
import com.bolao.di.viewModelModule
import com.bolao.presentation.App
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(
            networkModule,
            repositoryModule,
            viewModelModule,
        )
    }

    CanvasBasedWindow(title = "Bolão - Copa 2026", canvasElementId = "ComposeTarget") {
        App(appVersionCode = Int.MAX_VALUE)
    }
}
