package net.akehurst.ide.gui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.akehurst.kotlinx.filesystem.DirectoryHandle
import net.akehurst.kotlinx.filesystem.FileHandle
import net.akehurst.kotlinx.filesystem.FileSystemObjectHandle
import net.akehurst.kotlinx.filesystem.UserFileSystem

class GuiView(
    val gui: Gui
) {

    val actions = gui.actions

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun content(guiState: GuiState) {
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

        MaterialTheme(
            colorScheme = AppTheme.colors.light,
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
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = "Open Project Folder"
                                )
                            },
                            onClick = {
                                scope.launch {
                                    actions.actionOpenProject() {
                                        state.openProjectDirectory = it
                                        refreshTreeView(it, treeState)
                                    }
                                }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text(text = "New File") },
                            selected = false,
                            icon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = "New File") },
                            onClick = {
                                scope.launch {
                                    state.openProjectDirectory?.let {
                                        actions.actionNewFile(it)
                                        refreshTreeView(it, treeState)
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
                                                actions.actionSelectFile(handle)
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
                                Text(text = "Agl Editor")
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
                                .height(30.dp)
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(0.dp)
                            ) {
                                Text("language: ${state.selectedFile?.language}", fontSize = 12.sp, modifier = Modifier.padding(0.dp))
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
                                .border(width = Dp.Hairline, color = MaterialTheme.colorScheme.onBackground)
                                .height(30.dp)
                                .fillMaxWidth()
                        ) {
                            when {
                                state.openFiles.isEmpty() -> {
                                    Tab(
                                        selected = true,
                                        onClick = { },
                                        text = { Text(text = "<no file>") },
                                        modifier = Modifier
                                            .border(width = Dp.Hairline, color = MaterialTheme.colorScheme.onBackground)
                                            .padding(10.dp),
                                    )
                                }

                                else -> state.openFiles.forEachIndexed { idx, tab ->
                                    Tab(
                                        selected = selectedTab == idx,
                                        onClick = {
                                            selectedTab = idx
                                            scope.launch { actions.selectTab(idx, tab.fileHandle) }
                                        },
                                        content = {
                                            tabContent(text = tab.fileHandle.name, isDirty = tab.isDirty, issueMarker = tab.issueMarker, onClose = {
                                                scope.launch { actions.closeTab(idx, tab.fileHandle) }
                                            })
                                        },
                                        modifier = Modifier
                                            .border(width = Dp.Hairline, color = MaterialTheme.colorScheme.onBackground)
                                            .padding(10.dp),
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground),
                        ) {
                            gui.composeableEditor.content(
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

    private suspend fun refreshTreeView(fromDirectory: DirectoryHandle, treeViewState: TreeViewState) {
        val dirEntries = listFolderContent(fromDirectory)
        val topLevelItems = dirEntries.map { entry ->
            TreeNode(entry.name, emptyList(), mapOf("handle" to fromDirectory.entry(entry.name)))
        }
        treeViewState.setNewItems(topLevelItems)
    }

    private suspend fun listFolderContent(directory: DirectoryHandle): List<FileSystemObjectHandle> =
        UserFileSystem.listDirectoryContent(directory)

}