package net.akehurst.ide.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.window
import kotlinx.coroutines.await
import net.akehurst.ide.gui.fs.FileSystemDirectoryHandle
import net.akehurst.language.editor.api.AglEditorLogger
import net.akehurst.language.editor.api.LogLevel
import net.akehurst.language.editor.common.AglLanguageServiceByWorker
import net.akehurst.language.editor.common.objectJS
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.MODULE
import org.w3c.dom.SharedWorker
import org.w3c.dom.WorkerOptions
import org.w3c.dom.WorkerType
import kotlin.js.Promise

external var aglScriptBasePath: String = definedExternally
external var resourcesPath: String = definedExternally

actual class Gui : GuiAbstract() {

    val workerScriptName = "${aglScriptBasePath}/js-worker.js"
    override val languageService = AglLanguageServiceByWorker(
        SharedWorker(workerScriptName, options = WorkerOptions(type = WorkerType.MODULE)),
        AglEditorLogger { logLevel: LogLevel, msg: String, t: Throwable? ->
            console.log("$logLevel: $msg")
            t?.let { console.log("$logLevel: $t") }
        }
    )


     @OptIn(ExperimentalComposeUiApi::class)
     override suspend fun start() {
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

    override suspend fun openProjectFolder(): List<TreeNode> {
        val w:dynamic = window
        val p:Promise<dynamic> = w.showDirectoryPicker(
            objectJS {
                mode = "readwrite"
            }
        )
        val fileSystemDirectoryHandle:FileSystemDirectoryHandle = p.await()
        val list = mutableListOf<TreeNode>()
        for(v in fileSystemDirectoryHandle.values) {
            list.add(TreeNode(v.name, emptyList()))
        }
        return list
    }
}