package net.akehurst.ide.gui

import androidx.compose.foundation.layout.size
import net.akehurst.ide.common.data.fileextensionmap.FileExtensionMap
import net.akehurst.kotlinx.filesystem.DirectoryHandle
import net.akehurst.kotlinx.filesystem.FileAccessMode
import net.akehurst.kotlinx.filesystem.FileHandle
import net.akehurst.kotlinx.filesystem.UserFileSystem
import net.akehurst.kotlinx.text.toRegexFromGlob
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.processor.TransformString
import net.akehurst.language.api.processor.TypesString

class GuiHandler(
    private val gui: Gui,
) {
    val logger = gui.logger

    fun alert(message: String) {
        logger.logError { message }
        //TODO
    }

    suspend fun actionOpenProject(action: suspend (projectDir: DirectoryHandle) -> Unit) {
        logger.logTrace { "actionOpenProject" }
        val projectDir = selectProjectDirectoryFromDialog()
        projectDir?.let { action.invoke(it) }
        gui.projectContext = ContextAsmSimple()
        logger.logTrace { "actionOpenProject finished" }
    }

    suspend fun actionSelectFile(fileHandle: FileHandle) {
        logger.logTrace { "actionSelectFile ${fileHandle.name}" }
        fileHandle.readContent()?.let {
            val lang = setLanguageForExtension(fileHandle.name)
            gui.composeableEditor.rawText = it
            val ts = gui.state.openFiles.firstOrNull { it.fileHandle == fileHandle } ?: OpenTabState(fileHandle)
            ts.language = lang
            gui.state.openFiles.add(ts)
            gui.state.selectedFile = ts
        }
        logger.logTrace { "actionSelectFile finished" }
    }

    suspend fun actionNewFile(parentDirectory: DirectoryHandle): FileHandle? {
        val file = UserFileSystem.selectNewFileFromDialog(parentDirectory)
        file?.let {
            actionSelectFile(it)
        }
        return file
    }

    private suspend fun selectProjectDirectoryFromDialog(): DirectoryHandle? {
        val selected = UserFileSystem.selectDirectoryFromDialog(gui.state.openProjectDirectory,FileAccessMode.READ_WRITE)
        val appStateFile = gui.appFileSystem.root.file("app-state.map")
        selected?.let {
            appStateFile?.writeContent("last-project : ${it.path}")
        }
        return selected
    }

    suspend fun selectProjectDirectoryFromUserSettings() {
        val appStateFile = gui.userSettingsFileSystem.root.file("app-state.map") ?: let {
            gui.userSettingsFileSystem.root.createFile("app-state.map")
        }
        val lastDir = appStateFile?.readContent()?.let {
            val map = FileExtensionMap.process(it)
            map["last-project"]
        }
        lastDir?.let {
            //TODO: gui.state.openProjectDirectory = UserFileSystem.selectDirectoryFromDialog(lastDir,FileAccessMode.READ_WRITE)
            gui.state.openProjectDirectory = UserFileSystem.selectDirectoryFromDialog(null,FileAccessMode.READ_WRITE)
        }
    }

    fun createNewTab(handle: FileHandle, fileContent: String) {

    }

    suspend fun selectTab(tabIndex: Int, fileHandle: FileHandle) {
        fileHandle.readContent()?.let {
            setLanguageForExtension(fileHandle.name)
            gui.composeableEditor.rawText = it
            gui.state.selectedFile = gui.state.openFiles.firstOrNull { it.fileHandle == fileHandle }
        }
    }

    suspend fun closeTab(tabIndex: Int, fileHandle: FileHandle) {
        val ts = gui.state.openFiles.firstOrNull { it.fileHandle == fileHandle }
        gui.state.openFiles.remove(ts)
        when {
            gui.state.openFiles.isEmpty() -> {
                setLanguageForExtension("")
                gui.composeableEditor.rawText = ""
                gui.state.selectedFile = null
            }
            gui.state.openFiles.size > tabIndex -> {
                actionSelectFile(gui.state.openFiles.first().fileHandle)
            }
        }
    }

    private suspend fun setLanguageForExtension(fileName: String): String {
        val userFile = gui.userSettingsFileSystem.root.file("file-extensions.map") ?: let {
            gui.userSettingsFileSystem.root.createFile("file-extensions.map")
        }

        val langReference = userFile?.readContent()?.let { content ->
            val map = FileExtensionMap.process(content)
            map.entries.firstOrNull { (k,v) ->
                k.toRegexFromGlob('/').matches(fileName)
            }?.value
        } ?: let {
            val appFile: FileHandle? = gui.appFileSystem.getFile("settings/file-extensions.map")
                ?: let {
                    logger.logError { "App settings file 'file-extensions.map' not found!" }
                    null
                }
            appFile?.readContent()?.let { content ->
                val map = FileExtensionMap.process(content)
                map.entries.firstOrNull { (k,v) ->
                    k.toRegexFromGlob('/').matches(fileName)
                }?.value
            }
        }
        when (langReference) {
            null -> alert("There is no LanguageDefinition set for files named '$fileName'. Using plain text.")
            else -> {
                val langDef = fetchLanguageDefinition(langReference)
                when (langDef) {
                    null -> alert("The no language definition found for '$langReference'. Using plain text.")
                    else -> {
                        try {
                            //aglEditor.updateLanguageDefinition(langDef)
                            gui.aglEditor.updateLanguageDefinitionWith(
                                grammarStr = langDef.grammarString,
                                typeModelStr = langDef.typesString,
                                asmTransformStr = langDef.transformString,
                                crossReferenceStr = langDef.crossReferenceString,
                                styleStr = langDef.styleString
                            )
                        } catch (e: Exception) {
                            alert(e.message ?: "An exception occurred")
                        }
                    }
                }
            }
        }
        return langReference ?: "<unknown>"
    }

    //TODO: should be in computational
    suspend fun fetchLanguageDefinition(languageReference: String): LanguageDefinition<Any, Any>? =
        when {
            languageReference.startsWith("/") -> {
                val appResourcePath = languageReference
                val langDir = gui.appFileSystem.getDirectory(appResourcePath)
                langDir?.let {
                    val langId = LanguageIdentity(languageReference.substringAfter("/").replace("/", "."))
                    val grammarStr = it.file("grammar.agl-grm")?.readContent()?.let { GrammarString(it) }
                    when (grammarStr) {
                        null -> {
                            alert("The LanguageDefinition directory '$languageReference' must include a 'grammar.agl.grm' file.")
                            null
                        }

                        else -> {
                            val typeDomainStr = it.file("types.agl-typ")?.readContent()?.let { TypesString(it) }
                            val transformStr = it.file("transform.agl-trf")?.readContent()?.let { TransformString(it) }
                            val referenceStr = it.file("reference.agl-ref")?.readContent()?.let { CrossReferenceString(it) }
                            val styleStr = it.file("style.agl-sty")?.readContent()?.let { StyleString(it) }
                            val formatStr = it.file("format.agl-fmt")?.readContent()?.let { FormatString(it) }
                            Agl.languageDefinitionFromStringSimple(
                                identity = langId,
                                grammarDefinitionStr = grammarStr,
                                typeStr = typeDomainStr,
                                transformStr = transformStr,
                                referenceStr = referenceStr,
                                styleStr = styleStr,
                                formatterModelStr = formatStr
                            ) as LanguageDefinition<Any, Any>
                        }
                    }
                }
            }

            else -> {
                val registeredLangId = LanguageIdentity(languageReference)
                Agl.registry.findOrNull(registeredLangId) ?: let {
                    alert("The LanguageIdentity '$languageReference' cannot be found in the Agl.registry.")
                    null
                }
            }
        }

}