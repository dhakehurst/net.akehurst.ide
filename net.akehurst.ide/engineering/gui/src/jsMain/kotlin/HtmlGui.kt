package net.akehurst.ide.gui

import codemirror.extensions.commands.history
import codemirror.extensions.view.highlightActiveLine
import codemirror.extensions.view.highlightActiveLineGutter
import codemirror.extensions.view.lineNumbers
import codemirror.view.EditorViewConfig
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.akehurst.kotlin.html5.create
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.editor.api.AglEditor
import net.akehurst.language.editor.api.AglEditorLogger
import net.akehurst.language.editor.api.LanguageService
import net.akehurst.language.editor.api.LogFunction
import net.akehurst.language.editor.browser.codemirror.attachToCodeMirror
import net.akehurst.language.editor.common.objectJSTyped
import net.akehurst.language.editor.technology.gui.widgets.TreeView
import net.akehurst.language.editor.technology.gui.widgets.TreeViewFunctions
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
    }

    lateinit var scope: CoroutineScope
    lateinit var sidebar: HTMLElement
    lateinit var projectTree: TreeView<FileSystemObjectHandle>
    lateinit var aglEditor: AglEditor<Any, Any>
    var doSaveTimeout:Int?=null

    suspend fun start() {
        scope = CoroutineScope(coroutineContext)
        FontAwesome
        val appDivSelector = "div#ide"
        val appDiv = document.querySelector(appDivSelector)!!
        while (null != appDiv.firstChild) {
            appDiv.removeChild(appDiv.firstChild!!)
        }

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
                                        gui.actionNewFile(it) {newFile ->
                                            gui.actionSelectFile(newFile) {
                                                aglEditor.text = it
                                                gui.openFilePath = newFile
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
                            onClick = {file ->
                                when (file) {
                                    is FileHandle -> gui.actionSelectFile(file) {
                                        aglEditor.text = it
                                        gui.openFilePath = file
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
        val editorId = editorElement.id
        val languageId = "SysMLv2"
        val editorOptions = objectJSTyped<EditorViewConfig> {
            doc = ""
            extensions = arrayOf(
                lineNumbers(),
                highlightActiveLine(),
                highlightActiveLineGutter(),
                history(),
            )
            parent = editorElement
        }
        val ed = codemirror.view.EditorView(editorOptions)

        val logFunction: LogFunction = { lvl, msg, t -> logger.log(lvl, msg, t) }
        aglEditor = Agl.attachToCodeMirror<Any, Any>(
            languageService = languageService,
            containerElement = editorElement,
            languageId = languageId,
            editorId = editorId,
            logFunction = logFunction,
            cmEditor = ed,
            codemirror = codemirror.CodeMirror
        )
        aglEditor.onTextChange {
            // if save not scheduled, schedule save in 5 seconds
            if (doSaveTimeout==null) {
                doSaveTimeout = window.setTimeout({
                    scope.launch { gui.openFilePath?.writeContent(aglEditor.text) }
                    doSaveTimeout = null
                }, 5000)
            }
        }
    }

    private fun actionToggleSidebar() {
        if (sidebar.hasAttribute("open")) {
            sidebar.removeAttribute("open")
        } else {
            sidebar.setAttribute("open", "true")
        }
    }

}