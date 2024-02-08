package net.akehurst.ide.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import net.akehurst.language.editor.api.AglEditorLogger
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

    override val logger = AglEditorLogger { logLevel: LogLevel, msg: String, t: Throwable? ->
        when {
            logLevel <= LogLevel.Information -> {
                console.log("$logLevel: $msg")
                t?.let { console.log("$logLevel: $t") }
            }
        }
    }

    val workerScriptName = "${aglScriptBasePath}/js-worker.js"
    override val languageService = AglLanguageServiceByWorker(
        SharedWorker(workerScriptName, options = WorkerOptions(type = WorkerType.MODULE)),
        logger
    )

    override suspend fun start() = start_html()

    suspend fun start_html() {
        HtmlGui(this, logger,languageService).start()
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

    override fun lineTokens(lineStart: Int, tokens: List<List<Any>>) {

    }

}