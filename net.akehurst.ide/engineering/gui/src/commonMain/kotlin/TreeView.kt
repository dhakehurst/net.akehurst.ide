package net.akehurst.ide.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

data class TreeNode(
    val label:String,
    val children:List<TreeNode>
)

@Composable
fun TreeView(nodes: List<TreeNode>) {
    val expandedItems = remember { mutableStateListOf<TreeNode>() }
    LazyColumn {
        nodes(
            nodes,
            isExpanded = {
                expandedItems.contains(it)
            },
            toggleExpanded = {
                if (expandedItems.contains(it)) {
                    expandedItems.remove(it)
                } else {
                    expandedItems.add(it)
                }
            },
        )
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