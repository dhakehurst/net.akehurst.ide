package net.akehurst.ide.gui

import codemirror.extensions.autocomplete.autocompletion
import codemirror.extensions.commands.defaultKeymap
import codemirror.extensions.commands.history
import codemirror.extensions.commands.historyKeymap
import codemirror.extensions.commands.indentWithTab
import codemirror.extensions.language.bracketMatching
import codemirror.extensions.language.foldGutter
import codemirror.extensions.search.highlightSelectionMatches
import codemirror.extensions.search.searchKeymap
import codemirror.extensions.view.*
import codemirror.view.EditorViewConfig
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.akehurst.kotlin.html5.create
import net.akehurst.kotlin.html5.widgets.TreeView
import net.akehurst.kotlin.html5.widgets.TreeViewFunctions
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.editor.api.*
import net.akehurst.language.editor.browser.codemirror.attachToCodeMirror
import net.akehurst.language.editor.common.objectJS
import net.akehurst.language.editor.common.objectJSTyped
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.coroutines.coroutineContext

@JsModule("@fortawesome/fontawesome-free/css/all.min.css")
@JsNonModule
external val FontAwesome: dynamic

class HtmlGui(
    val gui: Gui,
    val logger: AglEditorLogger,
    val languageService: LanguageService,
) {

    companion object {
        const val EDITOR_DIV_ID = "editor"
        const val documentation = """
Enter SysML v2 Text here.
Or use the side bar to open a local folder and edit *.sysml files.
"""
    }

    lateinit var scope: CoroutineScope
    lateinit var sidebar: HTMLElement
    lateinit var projectTree: TreeView<FileSystemObjectHandle>
    var doSaveTimeout: Int? = null

    suspend fun start() {
        scope = CoroutineScope(coroutineContext)
        FontAwesome
        val appDivSelector = "div#ide"
        val appDiv = document.querySelector(appDivSelector)!!
        while (null != appDiv.firstChild) {
            appDiv.removeChild(appDiv.firstChild!!)
        }

        val logFunction: LogFunction = { lvl, msg, t -> logger.log(lvl, msg, t) }

        appDiv.create().article {
            header {
                button {
                    icon("fa-solid fa-bars")
                    on.click { actionToggleSidebar() }
                }
                p { content = "SysML v2.0" }
            }
            sidebar = section {
                attribute.id = "sidebar"
                article {
                    header {
                        button {
                            icon("fa-solid fa-book-open")
                            on.click {
                                scope.launch {
                                    gui.actionOpenProject() {
                                        projectTree.setRoots(listOf(it))
                                        gui.openProjectDirectory = it
                                    }
                                }
                            }
                        }
                        button {
                            icon("fa-solid fa-file-circle-plus")
                            on.click {
                                gui.openProjectDirectory?.let {
                                    scope.launch {
                                        // save existing file before opening new one, TODO: check for 'dirty'
                                        gui.openFilePath?.writeContent(gui.aglEditor.text)
                                        gui.actionNewFile(it) { newFile ->
                                            gui.actionSelectFile(newFile) {
                                                gui.aglEditor.text = it
                                                gui.openFilePath = newFile
                                                projectTree.refresh()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    section {
                        attribute.id = "projectTreeSection"
                        projectTree = treeview("projectTree", TreeViewFunctions<FileSystemObjectHandle>(
                            label = { root, it -> it.name },
                            hasChildren = { it is DirectoryHandle },
                            children = {
                                when (it) {
                                    is DirectoryHandle -> it.listContent().toTypedArray()
                                    else -> arrayOf()
                                }
                            },
                            onClick = { file ->
                                when (file) {
                                    is FileHandle -> {
                                        // save existing file before opening new one, TODO: check for 'dirty'
                                        gui.openFilePath?.writeContent(gui.aglEditor.text)
                                        gui.actionSelectFile(file) {
                                            gui.aglEditor.text = it
                                            gui.openFilePath = file
                                        }
                                    }

                                    else -> Unit
                                }
                            }
                        ))
                    }
                }
            } as HTMLElement
            section {
                attribute.id = "main"
                div { attribute.id = EDITOR_DIV_ID }
            }
        }

        val editorElement = document.querySelector("div#$EDITOR_DIV_ID") as HTMLDivElement
        gui.aglEditor = createCodeMirror(editorElement, logFunction, languageService)
        //gui.aglEditor = createAce(editorElement, logFunction, languageService)

        gui.aglEditor.editorOptions = EditorOptionsDefault(
            parse = true,
            parseLineTokens = true,
            lineTokensChunkSize = 1000,
            parseTree = false,
            syntaxAnalysis = true, //TODO
            syntaxAnalysisAsm = false,  //TODO
            semanticAnalysis = true,
            semanticAnalysisAsm = false
        )
        gui.aglEditor.onTextChange {
            // if save not scheduled, schedule save in 5 seconds
            if (doSaveTimeout == null) {
                doSaveTimeout = window.setTimeout({
                    scope.launch { gui.openFilePath?.writeContent(gui.aglEditor.text) }
                    doSaveTimeout = null
                }, 5000)
            }
        }
    }

    fun createCodeMirror(editorElement: HTMLDivElement, logFunction: LogFunction, languageService: LanguageService): AglEditor<Any,Any> {
        val editorId = editorElement.id
        val languageId = gui.langId
        val editorOptions = objectJSTyped<EditorViewConfig> {
            doc = ""
            extensions = arrayOf(
                lineNumbers(),
                highlightActiveLineGutter(),
                history(),
                foldGutter(),
                drawSelection(),
                dropCursor(),
                codemirror.state.EditorState.allowMultipleSelections.of(true),
                bracketMatching(),
                autocompletion(objectJS {
                    activateOnTyping = false
                }),
                rectangularSelection(),
                crosshairCursor(),
                highlightActiveLine(),
                highlightSelectionMatches(),
                keymap.of(arrayOf(*defaultKeymap, *searchKeymap, *historyKeymap, indentWithTab)),
                placeholder(documentation)
            )
            parent = editorElement
        }
        val ed = codemirror.extensions.view.EditorView(editorOptions)

       return Agl.attachToCodeMirror<Any, Any>(
            languageService = languageService,
            containerElement = editorElement,
            languageId = languageId,
            editorId = editorId,
            logFunction = logFunction,
            cmEditor = ed,
            codemirror = codemirror.CodeMirror
        )
    }

    /*
    fun createAce(editorElement: HTMLDivElement, logFunction: LogFunction, languageService: LanguageService): AglEditor<Any, Any> {
        val editorId = editorElement.id
        val languageId = editorElement.getAttribute("agl-language")!!
        val ed: ace.Editor = ace.Editor(
            ace.VirtualRenderer(editorElement, null),
            ace.Ace.createEditSession(""),
            objectJS {}
        )
        val aceOptions = objectJS {
            editor = objectJS {
                enableBasicAutocompletion = true
                enableSnippets = true
//            enableLiveAutocompletion = false
            }
            renderer = {

            }
        }
        ed.setOptions(aceOptions.editor)
        ed.renderer.setOptions(aceOptions.renderer)
        val ace = object : IAce {
            override fun createRange(startRow: Int, startColumn: Int, endRow: Int, endColumn: Int): IRange {
                return ace.Range(startRow, startColumn, endRow, endColumn)
            }
        }
        return Agl.attachToAce(
            languageService = languageService,
            containerElement = editorElement,
            languageId = languageId,
            editorId = editorId,
            logFunction = logFunction,
            aceEditor = ed,
            ace = ace
        )
    }
*/
    private fun actionToggleSidebar() {
        if (sidebar.hasAttribute("open")) {
            sidebar.removeAttribute("open")
        } else {
            sidebar.setAttribute("open", "true")
        }
    }

}