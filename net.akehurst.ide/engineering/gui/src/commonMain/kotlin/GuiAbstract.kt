//@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.akehurst.ide.gui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.akehurst.ide.common.data.fileextensionmap.FileExtensionMap
import net.akehurst.ide.user.inf.User
import net.akehurst.kotlin.compose.editor.ComposableCodeEditor
import net.akehurst.kotlin.compose.editor.ComposableCodeEditor2
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.*
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.editor.api.AglEditor
import net.akehurst.language.editor.api.AglEditorLogger
import net.akehurst.language.editor.api.LanguageService
import net.akehurst.language.editor.api.LogFunction
import net.akehurst.language.editor.common.aglEditorOptions
import net.akehurst.language.editor.compose.attachToComposeEditor
import kotlin.coroutines.coroutineContext

expect class Gui() : GuiAbstract {
    override val logFunction: LogFunction
    override val appFileSystem: AppFilesystem
    override val userSettings: AppFilesystem
    override val languageService: LanguageService
}

abstract class GuiAbstract : User {

    companion object {
        val REGEX_LETTER = Regex("[a-zA-Z]")
    }

    /**
     * directory for application files
     * usually in the app installation directory
     */
    abstract val appFileSystem: AppFilesystem

    /**
     * directory for user settings
     * usually in '~/.net.akehurst.ide'
     */
    abstract val userSettings: AppFilesystem

    abstract val logFunction: LogFunction

    val logger by lazy { AglEditorLogger("GuiAbstract", logFunction) }
    val editorId = "ide-editor"
    val langId = LanguageIdentity("sysml")
    val languageDefinition = Agl.languageDefinitionFromStringSimple(
        langId,
        GrammarString("namespace test grammar Test { S = 'Hello' ; } ")
    )
    val editorOptions = aglEditorOptions()
    var doSaveScheduled = false

    abstract val languageService: LanguageService

    val composeableEditor = ComposableCodeEditor2()
    val aglEditor: AglEditor<Any, Any> by lazy {
        Agl.attachToComposeEditor<Asm, ContextAsmSimple>(
            languageService,
            languageDefinition,
            editorId,
            editorOptions,
            logFunction,
            composeableEditor
        ).also {
            logger.logTrace {"Agl attachToComposeEditor finished"}
        } as AglEditor<Any, Any>
    }

    val guiState = GuiState()

    fun myOnKeyEvent(keyEvent: KeyEvent): Boolean {
        //    (keyEvent.nativeKeyEvent as SkikoKeyboardEvent).platform?.myConsume()
        return true
    }

    override suspend fun start() {

        selectProjectDirectoryFromUserSettings()

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
            guiState.selectedFileIsDirty = true
            // if save not scheduled, schedule save in 5 seconds
            if (doSaveScheduled.not()) {
                doSaveScheduled = true
                scope.launch {
                    delay(5000)
                    guiState.selectedFile?.writeContent(aglEditor.text)
                    guiState.selectedFileIsDirty = false
                    doSaveScheduled = false
                }
            }
        }

        logger.logTrace{"GUI start finished"}
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun content() {
        var state by remember { mutableStateOf(guiState) }
        val scope = rememberCoroutineScope()
        val treeState by remember { mutableStateOf(TreeViewState()) }
        val drawerState = rememberDrawerState(DrawerValue.Closed) {
            state.openProjectDirectory?.let {
                scope.launch {
                    refreshTreeView(it, treeState)
                }
            }
            true
        }
        var selectedTab by remember { mutableStateOf(0) }

        return MaterialTheme(
            colorScheme = AppTheme.colors.light,
            typography = AppTheme.typography.material
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(text = "Open Project Folder")
                                    Text(
                                        text = "New File", modifier = Modifier.clickable(
                                            onClick = {
                                                scope.launch {
                                                    state.openProjectDirectory?.let {
                                                        actionNewFile(it)
                                                        refreshTreeView(it, treeState)
                                                    }
                                                }
                                            }
                                        ))
                                }
                            },
                            selected = false,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Open Project Folder"
                                )
                            },
                            onClick = {
                                scope.launch {
                                    actionOpenProject() {
                                        refreshTreeView(it, treeState)
                                        state.openProjectDirectory = it
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                        TreeView(
                            state = treeState,
                            onSelectItem = {
                                scope.launch {
                                    val handle = it.data["handle"] as FileSystemObjectHandle?
                                    handle?.let {
                                        when {
                                            handle is FileHandle -> {
                                                actionSelectFile(handle)
                                                drawerState.close()
                                            }

                                            else -> Unit //TODO
                                        }
                                    }
                                }
                            }
                        )
                        // ...other drawer items
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = "SysML v2")
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch {
                                        drawerState.apply {
                                            if (isClosed) open() else close()
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Toggle drawer"
                                    )
                                }
                            },
                            modifier = Modifier
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground)
                        )
                    },
                    bottomBar = {
                        BottomAppBar(
                            modifier = Modifier
                                .padding(0.dp)
                                .height(20.dp)
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(0.dp)
                            ) {
                                Spacer(modifier = Modifier.weight(1f).padding(0.dp))
                                Text("row: ", fontSize = 12.sp, modifier = Modifier.padding(0.dp))
                                Text("col: ", fontSize = 12.sp, modifier = Modifier.padding(0.dp))
                            }
                        }
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    ) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground)
                                .height(30.dp)
                                .fillMaxWidth()
                        ) {
                            when {
                                state.openFiles.isEmpty() -> {
                                    Tab(
                                        selected = true,
                                        onClick = { },
                                        text = { Text(text = "<no file>") },
                                    )
                                }

                                else -> state.openFiles.forEachIndexed { idx, fileHandle ->
                                    Tab(
                                        selected = selectedTab == idx,
                                        onClick = {
                                            selectedTab = idx
                                            scope.launch { selectTab(idx, fileHandle) }
                                        },
                                        content = {
                                            tabContent(text = fileHandle.name, isDirty = state.selectedFileIsDirty, onClose = {
                                                scope.launch { closeTab(idx, fileHandle) }
                                            })
                                        },
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground),
                        ) {
                            composeableEditor.content(
                                modifier = Modifier
                                    //.padding(innerPadding)
                                    .fillMaxWidth()
                                //.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun selectProjectDirectoryFromUserSettings() {
        val appStateFile = userSettings.root.file("app-state.map") ?: let {
            userSettings.root.createFile("app-state.map")
        }
        val lastDir = appStateFile?.readContent()?.let {
            val map = FileExtensionMap.process(it)
            map["last-project"]
        }
        lastDir?.let {
            guiState.openProjectDirectory = UserFileSystem.selectProjectDirectoryFromDialog(true, lastDir)
        }
    }

    suspend fun selectProjectDirectoryFromDialog(): DirectoryHandle? {
        val selected = UserFileSystem.selectProjectDirectoryFromDialog(false, guiState.openProjectDirectory?.path)
        val appStateFile = userSettings.root.file("app-state.map")
        selected?.let {
            appStateFile?.writeContent("last-project : ${it.path}")
        }
        return selected
    }

    suspend fun listFolderContent(directory: DirectoryHandle): List<FileSystemObjectHandle> =
        UserFileSystem.listDirectoryContent(directory)

    suspend fun refreshTreeView(fromDirectory: DirectoryHandle, treeViewState: TreeViewState) {
        val dirEntries = listFolderContent(fromDirectory)
        val topLevelItems = dirEntries.map { entry ->
            TreeNode(entry.name, emptyList(), mapOf("handle" to fromDirectory.entry(entry.name)))
        }
        treeViewState.setNewItems(topLevelItems)
    }

    fun alert(message: String) {
        logger.logError{message}
        //TODO
    }

    suspend fun actionOpenProject(action: suspend (projectDir: DirectoryHandle) -> Unit) {
        logger.logTrace{"actionOpenProject"}
        val projectDir = selectProjectDirectoryFromDialog()
        projectDir?.let { action.invoke(it) }
        logger.logTrace{"actionOpenProject finished"}
    }

    suspend fun actionSelectFile(fileHandle: FileHandle) {
        logger.logTrace{"actionSelectFile ${fileHandle.name}"}
        fileHandle.readContent()?.let {
            setLanguageForExtension(fileHandle.extension)
            composeableEditor.rawText = it
            guiState.selectedFile = fileHandle
            guiState.openFiles.add(fileHandle)
        }
        logger.logTrace{"actionSelectFile finished"}
    }

    suspend fun actionNewFile(parentDirectory: DirectoryHandle): FileHandle? {
        val file = UserFileSystem.selectNewFileFromDialog(parentDirectory)
        file?.let {
            actionSelectFile(it)
        }
        return file
    }

    suspend fun readFileContent(file: FileHandle): String? = file.readContent()
    suspend fun saveFileContent(file: FileHandle, content: String) {
        file.writeContent(content)
    }

    fun createNewTab(handle: FileHandle, fileContent: String) {

    }

    suspend fun selectTab(tabIndex: Int, fileHandle: FileHandle) {
        fileHandle.readContent()?.let {
            setLanguageForExtension(fileHandle.extension)
            composeableEditor.rawText = it
            guiState.selectedFile = fileHandle
        }
    }

    suspend fun closeTab(tabIndex: Int, fileHandle: FileHandle) {
        guiState.openFiles.remove(fileHandle)
        when {
            guiState.openFiles.size > tabIndex -> {

            }
        }
    }

    suspend fun setLanguageForExtension(extension: String) {
        val userFile = userSettings.root.file("file-extensions.map") ?: let {
            userSettings.root.createFile("file-extensions.map")
        }

        val langReference = userFile?.readContent()?.let { content ->
            val map = FileExtensionMap.process(content)
            map[extension]
        } ?: let {
            val appFile: FileHandle? = appFileSystem.getFile("settings/file-extensions.map")
                ?: let {
                    logger.logError{"App settings file 'file-extensions.map' not found!"}
                    null
                }
            appFile?.readContent()?.let { content ->
                val map = FileExtensionMap.process(content)
                map[extension]
            }
        }
        when (langReference) {
            null -> alert("There is no LanguageDefinition set for files with extension '$extension'. Using plain text.")
            else -> {
                val langDef = fetchLanguageDefinition(langReference)
                when (langDef) {
                    null -> alert("The no language definition found for '$langReference'. Using plain text.")
                    else -> {
                        try {
                            aglEditor.updateLanguageDefinition(langDef)
                        } catch (e: Exception) {
                            alert(e.message ?: "An exception occurred")
                        }
                    }
                }
            }
        }
    }

    //TODO: should be in computational
    suspend fun fetchLanguageDefinition(languageReference: String): LanguageDefinition<Any, Any>? =
        when {
            languageReference.startsWith("/") -> {
                val appResourcePath = languageReference
                val langDir = appFileSystem.getDirectory(appResourcePath)
                langDir?.let {
                    val langId = LanguageIdentity(languageReference.substringAfter("/").replace("/", "."))
                    val grammarStr = it.file("grammar.agl-grm")?.readContent()?.let { GrammarString(it) }
                    when (grammarStr) {
                        null -> {
                            alert("The LanguageDefinition directory '$languageReference' must include a 'grammar.agl.grm' file.")
                            null
                        }

                        else -> {
                            val typeDomainStr = it.file("types.agl-typ")?.readContent()?.let { TypeModelString(it) }
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
