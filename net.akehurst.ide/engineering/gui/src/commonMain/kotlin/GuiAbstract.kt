@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.akehurst.ide.gui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.*
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.akehurst.ide.user.inf.User
import net.akehurst.kotlin.compose.editor.*
import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.language.style.asm.AglStyleModelDefault
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.regex.RegexEngineAgl
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.scanner.Matchable
import net.akehurst.language.agl.scanner.ScannerAbstract
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.RegexEngineKind
import net.akehurst.language.api.processor.ScannerKind
import net.akehurst.language.api.scanner.Scanner
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.editor.api.*
import net.akehurst.language.editor.common.AglStyleHandler

expect class Gui() : GuiAbstract {

}

data class AutocompleteItemDefault(
    override val name: String,
    override val text: String
) : AutocompleteItem {
    override fun equalTo(other: AutocompleteItem): Boolean = this.name == other.name
}

abstract class GuiAbstract : User {

    val ep = EndPointIdentity("ide", "")
    val langId = "sysml"
    val aglOptions = Agl.options<Any, Any> { }
    val regexEngineKind = RegexEngineKind.PLATFORM
    var scannerMatchables = listOf<Matchable>()
    val styleHandler = AglStyleHandler(langId)
    val simpleScanner: Scanner by lazy {
        val regexEngine = when (regexEngineKind) {
            RegexEngineKind.PLATFORM -> RegexEnginePlatform
            RegexEngineKind.AGL -> RegexEngineAgl
        }
        object : ScannerAbstract(regexEngine) {
            override val kind: ScannerKind get() = error("Not used")
            override val matchables: List<Matchable> get() = scannerMatchables
            override val validTerminals: List<Rule> get() = error("Not used")
            override fun reset() {}
            override fun isLookingAt(sentence: Sentence, position: Int, terminalRule: Rule): Boolean = error("Not used")
            override fun findOrTryCreateLeaf(sentence: Sentence, position: Int, terminalRule: Rule): CompleteTreeDataNode = error("Not used")
        }
    }

    abstract val languageService: LanguageService
    val autoCompleteResults = mutableListOf<AutocompleteSuggestion>()

    var ready = false

    override suspend fun start() {
        languageService.addResponseListener(
            ep, object : LanguageServiceResponse {
                override fun processorCreateResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>, scannerMatchables: List<Matchable>) {
                    this@GuiAbstract.scannerMatchables = scannerMatchables
                }

                override fun processorDeleteResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String) {
                }

                override fun processorSetStyleResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, styleModel: AglStyleModel?) {
                    this@GuiAbstract.styleHandler.updateStyleModel(styleModel ?: AglStyleModelDefault(emptyList()))
                    ready = true
                }

                override fun sentenceCodeCompleteResponse(
                    endPointIdentity: EndPointIdentity,
                    status: MessageStatus,
                    message: String,
                    issues: List<LanguageIssue>,
                    completionItems: List<CompletionItem>
                ) {
                    val auto = autoCompleteResults.removeFirstOrNull()
                    if (null != auto) {
                        when (status) {
                            MessageStatus.SUCCESS -> {
                                val items = completionItems.map {
                                    object : AutocompleteItem {
                                        //val kind = it.kind
                                        override val name: String get() = it.name
                                        override val text: String get() = it.text
                                        override fun equalTo(other: AutocompleteItem): Boolean = when {
                                            name != other.name -> false
                                            text != other.text -> false
                                            else -> true
                                        }
                                    }
                                }
                                auto.provide(items)
                            }

                            else -> auto.provide(emptyList())
                        }
                    }
                }

                override fun sentenceLineTokensResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, startLine: Int, lineTokens: List<List<AglToken>>) {
                    when (status) {
                        MessageStatus.SUCCESS -> {
                            lineTokens.forEachIndexed { index, tokens ->
                                // could get empty tokens for a line from a partial parse
                                val lineStart = tokens.firstOrNull()?.position ?: 0 // if not tokens then no conversion so line start pos does not matter
                                //this@GuiAbstract.tokensByLine[startLine + index] = convertTokens(tokens, lineStart)
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
            langId,
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
            langId,
            styleStr
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun content() {
        //val longText = (0..30).joinToString(separator = "\n") { "aa" + "$it".repeat(5) + "bbb" }
        val numbers = (0..50).joinToString(separator = "\n") { "$it" }
        val initText = "" //"hello world\nhello bill!"

        val drawerState = rememberDrawerState(DrawerValue.Closed)
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

//                    TestEd(
//                        initialText = initText,
//                        modifier = Modifier
//                            .padding(innerPadding)
//                            .fillMaxSize(),
//                    )

                    CodeEditor(
                        initialText = initText,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        onTextChange = this::onTextChange,
                        getLineTokens = ::getLineTokens,
                        requestAutocompleteSuggestions = ::requestAutocompleteSuggestions
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

    fun onTextChange(text: String) {
        if (ready) {
            tokensByLine.clear()
            println("textChange")
            this.languageService.request.sentenceProcessRequest(ep, langId, text, aglOptions)
        }
    }

    fun getLineTokens(lineNumber: Int, lineOffset: Int, lineText: String): List<EditorLineToken> {
        println("getLineTokens")
        return tokensByLine[lineNumber] ?: getTokensByScan(lineNumber, lineOffset, lineText)
    }

    fun getTokensByScan(lineNumber: Int, lineOffset: Int, lineText: String): List<EditorLineToken> {
        val sr = simpleScanner.scan(SentenceDefault(lineText), 0, lineOffset)
        val aglTokens = this.styleHandler.transformToTokens(sr.tokens)
        val edTokens = convertTokens(aglTokens, lineOffset)
        this.tokensByLine[lineNumber] = edTokens
        return edTokens
    }

    fun convertTokens(tokens: List<AglToken>, lineOffset: Int) = tokens.map { tok ->
        val s = tok.position - lineOffset
        EditorLineTokenDef(
            start = s,
            finish = s + tok.length,
            style = tok.styles.map {
                when (it) {
                    AglStyleHandler.EDITOR_NO_STYLE -> SpanStyle(color = Color.Green)
                    else -> SpanStyle(color = Color.Blue)
                }

            }.reduce { acc, it -> acc.merge(it) }
        )
    }

    suspend fun requestAutocompleteSuggestions(position: Int, text: String, result: AutocompleteSuggestion) {
        this.autoCompleteResults.add(result)
        languageService.request.sentenceCodeCompleteRequest(ep, langId, text, position, aglOptions)
    }
}

val rx = Regex("\n")
val String.lineEndsAt get() = rx.findAll(this + "\n")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TestEd(
    initialText: String,
    modifier: Modifier = Modifier
) {
    println("TestEd")
    // var inputText by remember { mutableStateOf(initialText) }
    // var viewText by remember { mutableStateOf(initialText) }

    val scope = rememberCoroutineScope()
    val defaultTextStyle = SpanStyle(color = Color.White) //MaterialTheme.colorScheme.onBackground)
    // val state by remember { mutableStateOf(EditorState(initialText, { _, _, _ -> })) }
    val viewTextState by remember { mutableStateOf(TextFieldState()) }
    val inputTextState by remember { mutableStateOf(TextFieldState()) }
    val inputScrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }
    val inputScrollerPosition = rememberSaveable(Orientation.Vertical, saver = TextFieldScrollerPosition.Saver) { TextFieldScrollerPosition(Orientation.Vertical) }
    val lineEndsAt = remember { mutableStateListOf<MatchResult>(*initialText.lineEndsAt.toList().toTypedArray()) }

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
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = LocalTextSelectionColors.current.handleColor,
                    backgroundColor = Color.Red
                )
            ) {
                // Visible Viewport
                // A CoreTextField that displays the styled text
                // just the subsection of text that is visible is formatted
                // The 'input' CoreTextField is transparent and sits on top of this.
                BasicTextField2(
                    //    MyCoreTextField(
                    cursorBrush = SolidColor(Color.Red),
                    textStyle = TextStyle(color = Color.Red),
                    //readOnly = false,
                    //enabled = true,
                    state = viewTextState,
//                        onValueChange = {}, //{ state.viewTextValue = it },
//                    onTextLayout = {},
//                    onScroll = {
//                        // update the drawn cursor position
//                        if (it != null) {
//                            state.viewCursorRect = it.getCursorRect(state.viewTextValue.selection.start)
//                            val cr = state.viewCursorRect
//                            state.viewCursors[0].update(cr.topCenter, cr.height)
//                        }
//                    },
                    modifier = Modifier
                        //                       .background(color = Color.Green)
                        .fillMaxSize()
                        .padding(5.dp, 5.dp)
//                        .drawWithContent {
//                            drawContent()
//                            // draw the cursors
//                            // (can't see how to make the actual cursor visible unless the control has focus)
//                            state.viewCursors.forEach {
//                                drawLine(
//                                    strokeWidth = 3f,
//                                    brush = it.brush,
//                                    start = it.start,
//                                    end = it.end
//                                )
//                            }
//                        },
                    //cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                )
            }
            CompositionLocalProvider(
                // make selections transparent in the in
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = LocalTextSelectionColors.current.handleColor,
                    backgroundColor = Color.Transparent
                )
            ) {
                BasicTextField2(
                    cursorBrush = SolidColor(Color.Transparent),
                    textStyle = TextStyle(color = Color.Blue),
                    state = inputTextState,
//                    onValueChange = {
////                        onTextChange(it.text)
//                        state.inputTextValue = it
//                        //FIXME slow - workaround because getLineEnd does tno work on JS
//                        lineEndsAt.clear()
//                        lineEndsAt.addAll(it.text.lineEndsAt)
//                    },
                    onTextLayout = {
                        //state.viewTextValue = state.viewTextValue.copy(selection = state.viewSelection)
                        viewTextState.text
                    },
                    modifier = Modifier
                        .fillMaxSize(),
                    //                       .onPreviewKeyEvent { ev -> handlePreviewKeyEvent(ev) },
                    //textScrollerPosition = inputScrollerPosition,
//                    onScroll = { textLayoutResult ->
//                        if (textLayoutResult != null) {
//                            val st = inputScrollerPosition.offset
//                            println("st = $st")
//                            val len = inputScrollerPosition.viewportSize
//                            println("len = $len")
//                            val firstLine = textLayoutResult.MygetLineForVerticalPosition(st)
//                            println("firstLine = $firstLine")
//                            val lastLine = textLayoutResult.MygetLineForVerticalPosition(st + len)//-1
//                            println("lastLine = $lastLine")
//                            val firstPos = textLayoutResult.getLineStart(firstLine)
//                            state.viewFirstLinePos = firstPos
//                            val lastPos = textLayoutResult.getLineEnd(lastLine, true)
//
//                            //FIXME: using JS workaround
//                            val fp = if (firstLine==0) {
//                                0
//                            } else {
//                                lineEndsAt.getOrNull(firstLine-1)?.range?.last ?: -1
//                            }
//                            val lp = lineEndsAt.getOrNull(lastLine)?.range?.first ?: -1
//                            println("fp/lp = [$fp-$lp]")
//                            val viewText = state.inputTextValue.text.substring(firstPos, lastPos)
//                            val annotated = buildAnnotatedString {
//                                for (lineNum in firstLine..lastLine) {
//                                    //val lineStartPos = textLayoutResult.getLineStart(lineNum)
//                                    //val lineFinishPos = textLayoutResult.getLineEnd(lineNum)
//                                    //FIXME: bug on JS getLineEnd does not work - workaround
//                                    val lineStartPos = if (firstLine==0) {
//                                        0
//                                    } else {
//                                        lineEndsAt.getOrNull(lineNum-1)?.range?.last ?: -1
//                                    }
//                                    val lineFinishPos = lineEndsAt.getOrNull(lineNum)?.range?.first ?: -1
//                                    val lineText = state.inputTextValue.text.substring(lineStartPos, lineFinishPos)
//                                    if (lineNum != firstLine) {
//                                        append("\n")
//                                    }
//                                    append(lineText)
//                                    addStyle(
//                                        defaultTextStyle,
//                                        lineStartPos - firstPos,
//                                        lineFinishPos - firstPos
//                                    )
//                                    val toks = try {
////                                        getLineTokens(lineNum, lineText)
//                                        emptyList<EditorLineToken>()
//                                    } catch (t: Throwable) {
//                                        //TODO: log error!
//                                        emptyList<EditorLineToken>()
//                                    }
//                                    for (tk in toks) {
//                                        val offsetStart = tk.start - firstPos
//                                        val offsetFinish = tk.finish - firstPos
//                                        addStyle(tk.style, offsetStart, offsetFinish)
//                                    }
//                                }
//                            }
//                            val sel = state.inputTextValue.selection //.toView(textLayoutResult)
//                            println("set viewTextValue [$firstPos-$lastPos], '$viewText'")
//                            //state.viewTextValue = state.inputTextValue.copy(annotatedString = annotated, selection = sel)
//                            state.viewTextValue = state.inputTextValue.copy(text=viewText, selection = sel)
//                        }
//                    }
                )
            }
        }
    }
}
