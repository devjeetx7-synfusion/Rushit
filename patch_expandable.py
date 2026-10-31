import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'r') as f:
    content = f.read()

new_expandable = """@Composable
fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium, color = Coral)
        }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Box(Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
        HorizontalDivider(color = Border)
    }
}"""

content = re.sub(
    r'@Composable\nfun ExpandableSection.*?(?=\n\n@Composable\nfun ProductOffers)',
    new_expandable,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'w') as f:
    f.write(content)
