import kotlinx.browser.document
import net.akehurst.ide.IdeApplication

suspend fun main() {
    IdeApplication().start({ gui ->
        onWasmReady {
            CanvasBasedWindow(canvasElementId = "ComposeTarget") {
                LaunchedEffect(Unit) {
                    document.getElementById("loading-indicator")?.remove()
                }
                gui.content()
            }
        }
    })
}