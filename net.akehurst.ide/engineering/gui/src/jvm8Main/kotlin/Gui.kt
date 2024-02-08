package net.akehurst.ide.gui

import androidx.compose.ui.window.singleWindowApplication
import net.akehurst.language.editor.api.AglEditorLogger
import net.akehurst.language.editor.api.LogLevel
import net.akehurst.language.editor.common.LanguageServiceByJvmThread
import java.util.concurrent.Executors

actual class Gui : GuiAbstract() {

    override suspend fun start() {
        super.start()
        singleWindowApplication(
            title = "IDE",
        ) {
            content()
        }
    }

    override val logger = AglEditorLogger { logLevel: LogLevel, msg: String, t: Throwable? ->
        when {
            logLevel <= LogLevel.Information -> {
                println("$logLevel: $msg")
                t?.let { println("$logLevel: $t") }
            }
        }
    }

    override val languageService = LanguageServiceByJvmThread(Executors.newSingleThreadExecutor())

    override fun lineTokens(lineStart: Int, tokens: List<List<Any>>) {
        TODO("not implemented")
    }


}
