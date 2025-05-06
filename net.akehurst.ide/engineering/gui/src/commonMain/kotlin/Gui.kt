//@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.akehurst.ide.gui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.akehurst.ide.common.data.fileextensionmap.FileExtensionMap
import net.akehurst.ide.user.inf.User
import net.akehurst.kotlin.compose.editor.ComposableCodeEditor2
import net.akehurst.kotlin.compose.editor.ComposableCodeEditor3
import net.akehurst.kotlinx.filesystem.FileHandle
import net.akehurst.kotlinx.filesystem.FileSystemFromVfs
import net.akehurst.kotlinx.filesystem.ResourcesFileSystem
import net.akehurst.kotlinx.filesystem.UserFileSystem
import net.akehurst.kotlinx.logging.api.LogFunction
import net.akehurst.kotlinx.logging.common.LoggerCommon
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.*
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.editor.api.AglEditor
import net.akehurst.language.editor.api.LanguageService
import net.akehurst.language.editor.common.aglEditorOptions
import net.akehurst.language.editor.compose.attachToComposeEditor
import net.akehurst.language.issues.api.LanguageIssueKind
import kotlin.coroutines.coroutineContext

class Gui(
    val logFunction: LogFunction,
    /**
     * directory for application files
     * usually in the app installation directory
     */
     val appFileSystem: FileSystemFromVfs,
    /**
     * directory for user settings
     * usually in '~/.net.akehurst.ide'
     */
     val userSettingsFileSystem: FileSystemFromVfs,
     val languageService: LanguageService,
) : User {

    companion object {
        val REGEX_LETTER = Regex("[a-zA-Z]")
    }

    val logger = LoggerCommon("Gui",logFunction)

    val editorId = "ide-editor"
    val langId = LanguageIdentity("initial")
    val languageDefinition = Agl.languageDefinitionFromStringSimple(
        identity = langId,
        grammarDefinitionStr = GrammarString("namespace test grammar Test { S = 'Hello' ; } "),
        styleStr = StyleString("namespace test styles Test { 'Hello' { background: pink; } } "),
    )
    val editorOptions = aglEditorOptions()
    var doSaveScheduled = false

    val composeableEditor = ComposableCodeEditor3().also {
        it.editorState.autocompleteState.itemLabelLength = 20
        it.editorState.autocompleteState.itemTextLength = 60
    }
    val aglEditor: AglEditor<Any, ContextAsmSimple> by lazy {
        Agl.attachToComposeEditor<Asm, ContextAsmSimple>(
            languageService,
            languageDefinition,
            { Agl.options {  } },
            editorId,
            editorOptions,
            logFunction,
            composeableEditor
        ).also {
            logger.logTrace { "Agl attachToComposeEditor finished" }
        } as AglEditor<Any, ContextAsmSimple>
    }

    val state = GuiState()
    val actions = GuiHandler(this)
    val view = GuiView(this)

    fun myOnKeyEvent(keyEvent: KeyEvent): Boolean {
        //    (keyEvent.nativeKeyEvent as SkikoKeyboardEvent).platform?.myConsume()
        return true
    }

    override suspend fun start() {

        actions.selectProjectDirectoryFromUserSettings()

        val scope = CoroutineScope(coroutineContext)
        /*
        val grammarStr = appFileSystem.read("languages/SysML_2_Std/grammar.agl")
        val crossReferenceModelStr = appFileSystem.read("languages/SysML_2_Std/references.agl")
        val styleStr = appFileSystem.read("languages/SysML_2_Std/style-light.agl")
        aglEditor.updateLanguageDefinitionWith(
            grammarStr = GrammarString(grammarStr),
            typeModelStr = null,
            asmTransformStr = null,
            crossReferenceStr = CrossReferenceString(crossReferenceModelStr),
            styleStr = StyleString(styleStr)
        )
        */
        aglEditor.onTextChange {
            state.selectedFile?.isDirty = true
            // if save not scheduled, schedule save in 5 seconds
            if (doSaveScheduled.not()) {
                doSaveScheduled = true
                scope.launch {
                    delay(5000)
                    state.selectedFile?.fileHandle?.writeContent(aglEditor.text)
                    state.selectedFile?.isDirty = false
                    doSaveScheduled = false
                }
            }
        }

        aglEditor.onIssues {
            when {
                aglEditor.issues.errors.isNotEmpty() -> state.selectedFile?.issueMarker = LanguageIssueKind.ERROR
                aglEditor.issues.warnings.isNotEmpty() -> state.selectedFile?.issueMarker = LanguageIssueKind.WARNING
                aglEditor.issues.informations.isNotEmpty() -> state.selectedFile?.issueMarker = LanguageIssueKind.INFORMATION
                else -> state.selectedFile?.issueMarker = null
            }
        }

        logger.logTrace { "GUI start finished" }
    }

    suspend fun readFileContent(file: FileHandle): String? = file.readContent()
    suspend fun saveFileContent(file: FileHandle, content: String) {
        file.writeContent(content)
    }

}
