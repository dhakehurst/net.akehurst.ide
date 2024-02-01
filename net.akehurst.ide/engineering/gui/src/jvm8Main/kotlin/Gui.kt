package net.akehurst.ide.gui

import androidx.compose.ui.window.singleWindowApplication
import net.akehurst.language.editor.api.LanguageService
import net.akehurst.language.editor.common.LanguageServiceByJvmThread
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.JFileChooser
import kotlin.io.path.readText
import kotlin.io.path.writeText

actual class Gui : GuiAbstract() {

    override suspend fun start() {
        super.start()
        singleWindowApplication(
            title = "IDE",
        ) {
            content()
        }
    }

    override val languageService = LanguageServiceByJvmThread(Executors.newSingleThreadExecutor())

    override fun lineTokens(lineStart: Int, tokens: List<List<Any>>) {
        TODO("not implemented")
    }

    override suspend fun openProjectFolder(): List<TreeNode> {
        val fc = JFileChooser()
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        fc.setAcceptAllFileFilterUsed(false)
        val dir = if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            fc.selectedFile
        } else {
            null
        }
        return dir?.listFiles()?.map { TreeNode(it.name, emptyList(), mapOf(
            "path" to it.absolutePath
        )) } ?: emptyList()
    }

    override suspend fun openFile(filePath: String): String {
        val p = Paths.get(filePath)
        return p.readText()
    }

    override suspend fun saveFile(filePath: String, content: String) {
        val p = Paths.get(filePath)
        p.writeText(content)
    }
}
