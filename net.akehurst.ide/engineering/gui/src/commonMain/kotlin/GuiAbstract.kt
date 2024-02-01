@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.akehurst.ide.gui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import korlibs.image.color.Colors
import kotlinx.coroutines.launch
import net.akehurst.ide.user.inf.User
import net.akehurst.kotlin.compose.editor.*
import net.akehurst.kotlin.compose.editor.api.*
import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.language.style.asm.AglStyleModelDefault
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.regex.RegexEngineAgl
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.scanner.Matchable
import net.akehurst.language.agl.scanner.ScannerAbstract
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.scanner.Scanner
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.api.style.AglStyleRule
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

val String.toComposeColor: Color
    get() {
        val rgba = when {
            this.startsWith("#") -> Colors[this.lowercase()]
            else -> Colors[CssColours.nameToHex[this.lowercase()]!!]
        }
        return Color(rgba.r, rgba.g, rgba.b, rgba.a)
    }

// Wanted order
// 1 references
// 2 literals words (alphabetical
// 3 literals symbols
// 4 patterns
// 5 segments
val CompletionItem.orderValue
    get() = when (this.kind) {
        CompletionItemKind.REFERRED -> 1
        CompletionItemKind.LITERAL -> when {
            GuiAbstract.REGEX_LETTER.matchesAt(this.text, 0) -> {
                2
            }

            else -> 3
        }

        CompletionItemKind.PATTERN -> 4
        CompletionItemKind.SEGMENT -> 5
    }

abstract class GuiAbstract : User {

    companion object {
        val REGEX_LETTER = Regex("[a-zA-Z]")
    }

    val ep = EndPointIdentity("ide", "")
    val langId = "sysml"
    val aglOptions = Agl.options<Any, Any> { }
    val regexEngineKind = RegexEngineKind.PLATFORM
    var scannerMatchables = listOf<Matchable>()
    val styleHandler = object : AglStyleHandler(langId) {
        override fun <EditorStyleType : Any> convert(rule: AglStyleRule): EditorStyleType {
            val ss = rule.styles.values.map { style ->
                when (style.name) {
                    "foreground" -> SpanStyle(color = style.value.toComposeColor)
                    "background" -> SpanStyle(background = style.value.toComposeColor)
                    "font-style" -> when (style.value) {
                        "bold" -> SpanStyle(fontWeight = FontWeight.Bold)
                        "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
                        else -> SpanStyle() //unsupported
                    }

                    else -> SpanStyle() //unsupported
                }
            }.reduce { acc, it -> it.merge(acc) }
            return ss as EditorStyleType
        }
    }
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
    var openProjectFolderPath: String? = null
    var openFilePath: String? = null

    override suspend fun start() {
        languageService.addResponseListener(
            ep, object : LanguageServiceResponse {
                override fun processorCreateResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>, scannerMatchables: List<Matchable>) {
                    scannerMatchables.forEach {
                        it.using(RegexEnginePlatform)
                    }
                    this@GuiAbstract.scannerMatchables = scannerMatchables
                }

                override fun processorDeleteResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String) {
                }

                override fun processorSetStyleResponse(endPointIdentity: EndPointIdentity, status: MessageStatus, message: String, issues: List<LanguageIssue>, styleModel: AglStyleModel?) {
                    when (status) {
                        MessageStatus.START -> Unit
                        MessageStatus.SUCCESS -> {
                            this@GuiAbstract.styleHandler.updateStyleModel(styleModel ?: AglStyleModelDefault(emptyList()))
                            ready = true
                        }

                        MessageStatus.FAILURE -> {
                            issues.forEach {
                                println(it) //TODO
                            }
                        }
                    }
                }

                override fun sentenceCodeCompleteResponse(
                    endPointIdentity: EndPointIdentity,
                    status: MessageStatus,
                    message: String,
                    issues: List<LanguageIssue>,
                    completionItems: List<CompletionItem>
                ) {
                    val auto = autoCompleteResults.removeFirstOrNull()

                    val sorted = completionItems.sortedWith { a, b ->
                        val av = a.orderValue
                        val bv = b.orderValue
                        when {
                            av > bv -> 1
                            av < bv -> -1
                            else -> {
                                a.text.compareTo(b.text)
                            }
                        }
                    }
                    if (null != auto) {
                        when (status) {
                            MessageStatus.SUCCESS -> {
                                val items = sorted.map {
                                    object : AutocompleteItem {
                                        //val kind = it.kind
                                        override val name: String?
                                            get() = when (it.kind) {
                                                CompletionItemKind.LITERAL -> null
                                                else -> it.name
                                            }
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
                                this@GuiAbstract.tokensByLine[startLine + index] = convertTokens(tokens, lineStart)
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
        val styleStr = FileSystem.read("languages/SysML_2_Std/style-light.agl")
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
    fun content1() {
        var text by remember { mutableStateOf(TextFieldValue("")) }
        TextField(
            value = text,
            label = { Text(text = "Enter Your Name") },
            onValueChange = {
                text = it
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun content2() {
        val state by remember { mutableStateOf(TextFieldState("")) }
        BasicTextField2(
            state = state
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun content() {
        //val longText = (0..30).joinToString(separator = "\n") { "aa" + "$it".repeat(5) + "bbb" }
        val numbers = (0..50).joinToString(separator = "\n") { "$it" }
        val initText = "" //"hello world\nhello bill!"

        val defaultTextStyle = SpanStyle(color = MaterialTheme.colorScheme.onBackground, background = MaterialTheme.colorScheme.background)
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val treeState by remember { mutableStateOf(TreeViewState()) }
        val editorState by remember {
            mutableStateOf(
                EditorState(
                    initialText = initText,
                    onTextChange = { txt ->
                        scope.launch {
                            this@GuiAbstract.onTextChange(txt)
                        }
                    },
                    defaultTextStyle = defaultTextStyle,
                    getLineTokens = ::getLineTokens,
                    requestAutocompleteSuggestions = ::requestAutocompleteSuggestions
                )
            )
        }

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
                                    Text(text = "New File", modifier = Modifier.clickable(
                                        onClick = {
                                            openProjectFolderPath?.let { newFile(it) }
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
                                    openProjectFolderPath = openProjectFolder()
                                    openProjectFolderPath?.let {
                                        val topLevelItems = listFolderContent(it)
                                        treeState.setNewItems(topLevelItems)
                                    }

                                }
                            }
                        )
                        Divider()
                        TreeView(
                            state = treeState,
                            onSelectItem = {
                                scope.launch {
                                    val path = it.data["path"] as String?
                                    path?.let {
                                        val fileContent = openFile(it)
                                        editorState.setNewText(fileContent)
                                        openFilePath = it
                                        drawerState.close()
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
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        editorState = editorState,
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

    abstract suspend fun openProjectFolder(): String
    abstract suspend fun listFolderContent(filePath: String): List<TreeNode>
    abstract suspend fun newFile(parentPath: String): String
    abstract suspend fun openFile(filePath: String): String
    abstract suspend fun saveFile(filePath: String, content: String)

    suspend fun onTextChange(text: String) {
        if (ready) {
            tokensByLine.clear()
//            println("textChange")
            this.languageService.request.sentenceProcessRequest(ep, langId, text, aglOptions)
            openFilePath?.let { path ->
                saveFile(path, text)
            }
        }
    }

    fun getLineTokens(lineNumber: Int, lineOffset: Int, lineText: String): List<EditorLineToken> {
//        println("getLineTokens")
        return tokensByLine[lineNumber] ?: getTokensByScan(lineNumber, lineOffset, lineText)
    }

    fun getTokensByScan(lineNumber: Int, lineOffset: Int, lineText: String): List<EditorLineToken> {
//        println("getTokensByScan($lineNumber,$lineOffset,'${lineText.substring(0, min(5, lineText.length))}...')")
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
            style = tok.styles
                .map { styleHandler.editorStyleFor<SpanStyle>(it) ?: SpanStyle() }
                .reduce { acc, it -> it.merge(acc) }
        )
    }

    suspend fun requestAutocompleteSuggestions(position: Int, text: String, result: AutocompleteSuggestion) {
        this.autoCompleteResults.add(result)
        languageService.request.sentenceCodeCompleteRequest(ep, langId, text, position, aglOptions)
    }
}

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
