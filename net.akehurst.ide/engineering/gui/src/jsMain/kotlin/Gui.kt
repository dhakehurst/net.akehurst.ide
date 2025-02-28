package net.akehurst.ide.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import net.akehurst.language.editor.api.LanguageService
import net.akehurst.language.editor.api.LogFunction
import net.akehurst.language.editor.api.LogLevel
import net.akehurst.language.editor.language.service.AglLanguageServiceByWorker
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.MODULE
import org.w3c.dom.SharedWorker
import org.w3c.dom.WorkerOptions
import org.w3c.dom.WorkerType

external var aglScriptBasePath: String = definedExternally
external var resourcesPath: String = definedExternally

actual class Gui : GuiAbstract() {

    actual override val logFunction: LogFunction = { logLevel, prefix, msg, t ->
        when {
            logLevel <= LogLevel.All -> { //LogLevel.Information -> {
                console.log("$logLevel: $prefix - $msg")
                t?.let { console.log("$logLevel: $t") }
            }
        }
    }

    actual override val appFileSystem: AppFilesystem = FileSystemFromPath(resourcesPath)

    val workerScriptName = "${aglScriptBasePath}/js-worker.js"
    actual override val languageService: LanguageService = AglLanguageServiceByWorker(
        SharedWorker(workerScriptName, options = WorkerOptions(type = WorkerType.MODULE)),
        logFunction
    )

    override suspend fun start() = start_html()

    suspend fun start_html() {
        HtmlGui(this, logFunction, languageService).start()
        super.start()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    suspend fun start_compose() {
        super.start()
        onWasmReady {
            CanvasBasedWindow("IDE") {
                Column(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }

}