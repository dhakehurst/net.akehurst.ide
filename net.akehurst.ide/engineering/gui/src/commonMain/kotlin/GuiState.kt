package net.akehurst.ide.gui

//import androidx.compose.foundation.text.MyCoreTextField2
import androidx.compose.runtime.*
import net.akehurst.kotlinx.filesystem.DirectoryHandle
import net.akehurst.kotlinx.filesystem.FileHandle
import net.akehurst.language.issues.api.LanguageIssueKind

data class OpenTabState(
    val fileHandle: FileHandle
) {
    var language : String = "<unknown>"
    var isDirty by mutableStateOf(false)
    var issueMarker: LanguageIssueKind? by mutableStateOf(null)
}

@Stable
class GuiState {

    var openProjectDirectory: DirectoryHandle? by mutableStateOf(null)
    val openFiles:MutableSet<OpenTabState> = mutableStateSetOf<OpenTabState>()
    var selectedFile:OpenTabState? by mutableStateOf(null)
    var selectedTab by  mutableStateOf(0)
}