import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.akehurst.ide.*
import net.akehurst.kotlinx.filesystem.FileSystemFromVfs
import net.akehurst.kotlinx.filesystem.ResourcesFileSystem
import net.akehurst.kotlinx.filesystem.UserFileSystem
import net.akehurst.kotlinx.filesystem.UserHomeFileSystem
import net.akehurst.kotlinx.logging.api.LogFunction
import net.akehurst.kotlinx.logging.common.LoggerCommon
import net.akehurst.kotlinx.logging.common.LoggerConsole
import net.akehurst.language.editor.common.LanguageServiceByJvmThread
import net.akehurst.language.editor.language.service.LanguageServiceDirectExecution
import java.util.concurrent.Executors

suspend fun main() {
    val logFunction: LogFunction = { logLevel, prefix, t, msg ->
        println("$logLevel, $prefix: ${msg()}")
        t?.let { t.printStackTrace() }
    }
    val logger = LoggerCommon("main", logFunction)
    val appFilesystem = if (null != System.getProperty("net.akehurst.ide.app.directory")) {
        FileSystemFromVfs(System.getProperty("net.akehurst.ide.app.directory"))
    } else {
        ResourcesFileSystem
    }
    val userSettingsFileSystem = UserHomeFileSystem(".net-akehurst-ide")
    //val languageService = LanguageServiceByJvmThread(Executors.newSingleThreadExecutor(), logFunction)
    val languageService = LanguageServiceDirectExecution(logFunction)
    val app = IdeApplication(
        logFunction,
        appFilesystem,
        userSettingsFileSystem,
        languageService
    )
    app.start({ gui ->
        GlobalScope.async {
            try {
                gui.start()
                singleWindowApplication(
                    title = "AGL Editor",
                ) {
                    gui.view.content(gui.state)
                }
            } catch (t: Throwable) {
                logger.logError(t) { "Unable to start gui" }
            }
        }
    })
}




