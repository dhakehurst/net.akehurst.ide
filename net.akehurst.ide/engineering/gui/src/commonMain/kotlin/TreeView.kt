package net.akehurst.ide.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

data class TreeNode(
    val label: String,
    val children: List<TreeNode>,
    val data:Map<String,Any> = mapOf()
)

@Composable
fun TreeView(
    modifier: Modifier = Modifier,
    state: TreeViewState,
    onSelectItem: (item:TreeNode)->Unit = {}
) {
    val expandedItems = remember { mutableStateListOf<TreeNode>() }
    LazyColumn(
        state = state.lazyListState
    ) {
        itemsIndexed(state.items) { idx, item ->
            Row () {
                Text(
                    item.label,
                    Modifier.clickable(
                        onClick = {
                            onSelectItem.invoke(item)
                        }
                    )
                )
            }
        }

//        nodes(
//            nodes,
//            isExpanded = {
//                expandedItems.contains(it)
//            },
//            toggleExpanded = {
//                if (expandedItems.contains(it)) {
//                    expandedItems.remove(it)
//                } else {
//                    expandedItems.add(it)
//                }
//            },
//        )
    }
}

fun LazyListScope.nodes(
    nodes: List<TreeNode>,
    isExpanded: (TreeNode) -> Boolean,
    toggleExpanded: (TreeNode) -> Unit,
) {
    nodes.forEach { node ->
        node(
            node,
            isExpanded = isExpanded,
            toggleExpanded = toggleExpanded,
        )
    }
}

fun LazyListScope.node(
    node: TreeNode,
    isExpanded: (TreeNode) -> Boolean,
    toggleExpanded: (TreeNode) -> Unit,
) {
    item {
        Text(
            node.label,
            Modifier.clickable {
                toggleExpanded(node)
            }
        )
    }
    if (isExpanded(node)) {
        nodes(
            node.children,
            isExpanded = isExpanded,
            toggleExpanded = toggleExpanded,
        )
    }
}

@Stable
class TreeViewState {

    var items by mutableStateOf(listOf(TreeNode("<no content>", emptyList())))
    val lazyListState = LazyListState()

    fun setNewItems(newItems:List<TreeNode>) {
        items = newItems
    }
}