package net.akehurst.ide.gui

//import androidx.compose.foundation.text.MyCoreTextField2
import androidx.compose.runtime.*

@Stable
class GuiState {

    var openProjectDirectory:DirectoryHandle? by mutableStateOf(null)
    val openFiles:MutableSet<FileHandle> = mutableStateSetOf<FileHandle>()
    var selectedFile:FileHandle? by mutableStateOf(null)
    var selectedFileIsDirty by mutableStateOf(false)
    var selectedTab by  mutableStateOf(0)
}