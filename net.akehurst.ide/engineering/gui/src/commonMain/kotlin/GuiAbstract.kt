@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package net.akehurst.ide.gui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.akehurst.ide.user.inf.User
import net.akehurst.kotlin.compose.editor.AutocompleteItem
import net.akehurst.kotlin.compose.editor.AutocompleteSuggestion
import net.akehurst.kotlin.compose.editor.CodeEditor
import net.akehurst.kotlin.compose.editor.EditorLineToken
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.scanner.Matchable
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.editor.api.*

expect class Gui() : GuiAbstract {

}

data class AutocompleteItemDefault(
    override val name: String,
    override val text: String
) : AutocompleteItem {
    override fun equalTo(other: AutocompleteItem): Boolean = this.name == other.name
}

abstract class GuiAbstract : User {

    abstract val languageService: LanguageService

    var ready = false

    override suspend fun start() {
        val ep = EndPointIdentity("ide", "")
        languageService.addResponseListener(
            ep, object : LanguageServiceResponse {
                override fun processorCreateResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>, scannerMatchables: List<Matchable>) {
                }

                override fun processorDeleteResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String) {
                }

                override fun processorSetStyleResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String) {
                    ready = true
                }

                override fun sentenceCodeCompleteResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>) {
                }

                override fun sentenceLineTokensResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, startLine: Int, lineTokens: List<List<AglToken>>) {
                    when(status) {
                        MessageStatus.SUCCESS -> {
                            lineTokens.forEachIndexed { index, tokens ->
                                // could get empty tokens for a line from a partial parse
                                if (tokens.isNotEmpty()) {
                                    this@GuiAbstract.tokensByLine[startLine + index] = tokens.map {
                                        EditorLineTokenDef(
                                            start = it.position,
                                            finish = it.position+it.length,
                                            style = it.styles.map {

                                                SpanStyle()
                                            }.reduce { acc, it -> acc.merge(it) }
                                        )
                                    }
                                } else {
                                    // nothing
                                }
                            }
                        }
                        else -> Unit //TODO
                    }
                }

                override fun sentenceParseResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>, tree: Any?) {
                }

                override fun sentenceSemanticAnalysisResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>, asm: Any?) {
                }

                override fun sentenceSyntaxAnalysisResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>, asm: Any?) {
                }

            }
        )

        val grmrStr = FileSystem.read("languages/SysML_2_Std/grammar.agl")
        val styleStr = FileSystem.read("languages/SysML_2_Std/style.agl")
        languageService.request.processorCreateRequest(
            ep,
            "sysml",
            grmrStr,
            null,
            EditorOptionsDefault(
                parse = true,
                parseLineTokens = true,
                lineTokensChunkSize = 1000,
                parseTree = false,
                syntaxAnalysis = false, //TODO
                syntaxAnalysisAsm = false,  //TODO
                semanticAnalysis = false,
                semanticAnalysisAsm = false
            )
        )
        languageService.request.processorSetStyleRequest(
            ep,
            "sysml",
            styleStr
        )

        ready = true
    }

    fun onTextChange(text: String) {
        if (ready) {
            //    lineTokens.clear()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun content() {
        //val longText = (0..30).joinToString(separator = "\n") { "aa" + "$it".repeat(5) + "bbb" }
        //val numbers = (0..50).joinToString(separator = "\n") { "$it" }
        val initText = "hello world"

        val drawerState = rememberDrawerState(DrawerValue.Open)
        val scope = rememberCoroutineScope()
        val tree = remember { mutableStateOf(mutableListOf(TreeNode("<no project>", emptyList()))) }

        return MaterialTheme(
            colorScheme = AppTheme.colors.material,
            typography = AppTheme.typography.material
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        NavigationDrawerItem(
                            label = { Text(text = "Open Project Folder") },
                            selected = false,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Open Project Folder"
                                )
                            },
                            onClick = {
                                scope.launch {
                                    val topLevelItems = openProjectFolder()
                                    tree.value.clear()
                                    tree.value.addAll(topLevelItems)
                                }
                            }
                        )
                        Divider()
                        TreeView(
                            tree.value
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
                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar() { }
                    },
                ) { innerPadding ->

                    CodeEditor(
                        initialText = initText,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
//                        onTextChange = this::onTextChange,
//                        getLineTokens = ::getLineTokens,
//                        requestAutocompleteSuggestions = ::requestAutocompleteSuggestions
                    )
                }
            }
        }
    }

    data class EditorLineTokenDef(
        override val start: Int,
        override val finish: Int,
        override val style: SpanStyle
    ) : EditorLineToken

    val tokensByLine = mutableMapOf<Int, List<EditorLineToken>>()

    abstract suspend fun openProjectFolder(): List<TreeNode>

    fun getLineTokens(lineNumber: Int, lineText: String): List<EditorLineToken> {
        return tokensByLine[lineNumber] ?: emptyList()
    }

    suspend fun requestAutocompleteSuggestions(position: Int, text: String, result: AutocompleteSuggestion) {
        delay(1000)
        result.provide(
            listOf(
                AutocompleteItemDefault(name = "fred", text = "Fred"),
                AutocompleteItemDefault(name = "jim", text = "Jim"),
                AutocompleteItemDefault(name = "jane", text = "Jane"),
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TestEd(
    initialText: String,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf(initialText) }
    var viewText by remember { mutableStateOf(initialText) }

//    OutlinedTextField(
//        value = text,
//        onValueChange = { text = it },
//        modifier = Modifier
//            .padding(innerPadding)
//            .fillMaxSize(),
//    )

    Row(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
                .background(color = Color.Yellow)
        ) {
            itemsIndexed(listOf(1, 2, 3)) { idx, ann ->
                Row(
                ) { }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            CompositionLocalProvider(
                // make selections transparent in the in
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = LocalTextSelectionColors.current.handleColor,
                    backgroundColor = Color.Red
                )
            ) {
                BasicTextField(
                    cursorBrush = SolidColor(Color.Red),
                    textStyle = TextStyle(color = Color.Red),
                    value = viewText,
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 5.dp, vertical = 5.dp)
                )
            }
            CompositionLocalProvider(
                // make selections transparent in the in
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = LocalTextSelectionColors.current.handleColor,
                    backgroundColor = Color.Transparent
                )
            ) {
                BasicTextField(
                    cursorBrush = SolidColor(Color.Blue),
                    textStyle = TextStyle(color = Color.Blue),
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        viewText = it
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}