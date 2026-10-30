package com.rushit.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CategoryManagementScreen(
    categories: MutableList<String>,
    products: MutableList<String>,
    onSave: (String) -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableStateOf("") }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item { ScreenHeader("Categories", "${categories.size} demo categories") }
        item { DemoNotice() }
        itemsIndexed(categories.toList()) { index, item ->
            val name = item.substringBefore('|')
            val visible = item.endsWith("Visible")
            val count = products.count { it.split(" · ").getOrNull(3) == name }
            AdminSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (name.contains("Fruit", true)) "🍎" else if (name.contains("Dairy", true)) "🥛" else if (name.contains("Veget", true)) "🥬" else "▦")
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text("$count products · ${if (visible) "Visible" else "Hidden"}", color = AppColors.muted)
                    }
                    Switch(visible, onCheckedChange = { checked -> categories[index] = "$name|${if (checked) "Visible" else "Hidden"}"; onSave("Category visibility updated locally") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { draft = name; editIndex = index; showEditor = true }) { Text("Edit") }
                    TextButton(onClick = { if (index > 0) { val moved = categories.removeAt(index); categories.add(index - 1, moved); onSave("Category order saved") } }) { Text("↑") }
                    TextButton(onClick = { if (index < categories.lastIndex) { val moved = categories.removeAt(index); categories.add(index + 1, moved); onSave("Category order saved") } }) { Text("↓") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { deleteIndex = index }, enabled = count == 0) { Text(if (count == 0) "Delete" else "In use") }
                }
            }
        }
        item { OutlinedButton(onClick = { draft = ""; editIndex = null; showEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add Category") } }
    }
    if (showEditor) AlertDialog(
        onDismissRequest = { showEditor = false },
        title = { Text(if (editIndex == null) "Add category" else "Edit category") },
        text = { OutlinedTextField(draft, { draft = it }, label = { Text("Category name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            val clean = draft.trim()
            if (clean.isNotBlank() && categories.none { it.substringBefore('|').equals(clean, true) && (editIndex == null || categories.indexOf(it) != editIndex) }) {
                val index = editIndex
                if (index == null) categories.add("$clean|Visible") else {
                    val oldName = categories[index].substringBefore('|')
                    val status = categories[index].substringAfter('|', "Visible")
                    categories[index] = "$clean|$status"
                    products.indices.forEach { productIndex ->
                        val parts = products[productIndex].split(" · ").toMutableList()
                        if (parts.getOrNull(3) == oldName) { parts[3] = clean; products[productIndex] = parts.joinToString(" · ") }
                    }
                }
                onSave("Category saved locally")
                showEditor = false
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
    )
    deleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { deleteIndex = null },
            title = { Text("Delete category?") },
            text = { Text("${categories.getOrNull(index)?.substringBefore('|')} will be removed from this local demo.") },
            confirmButton = { Button(onClick = { if (index in categories.indices && products.none { it.split(" · ").getOrNull(3) == categories[index].substringBefore('|') }) { categories.removeAt(index); onSave("Category deleted locally") }; deleteIndex = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteIndex = null }) { Text("Cancel") } },
        )
    }
}
