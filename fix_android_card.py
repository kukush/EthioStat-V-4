import re

with open("android/app/src/main/java/com/ethiobalance/app/ui/components/TelecomAssetCard.kt", "r") as f:
    content = f.read()

content = content.replace(
    "    airtimeBalance: Double = 0.0,\n    onOpenUssd: ((action: String) -> Unit)? = null\n) {",
    "    airtimeBalance: Double = 0.0,\n    onOpenUssd: ((action: String) -> Unit)? = null,\n    isCompact: Boolean = false\n) {"
)

# Hide buttons if isCompact
content = content.replace("if (onOpenUssd != null) {", "if (onOpenUssd != null && !isCompact) {")

# Paddings and sizes
content = content.replace("Column(modifier = Modifier.padding(20.dp)) {", "Column(modifier = Modifier.padding(if (isCompact) 14.dp else 20.dp)) {")
content = content.replace("Arrangement.spacedBy(12.dp)", "Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)")
content = content.replace(".padding(14.dp)", ".padding(if (isCompact) 10.dp else 14.dp)")

content = re.sub(
    r'fontSize = 24.sp, fontWeight = FontWeight.Black',
    r'fontSize = if (isCompact) 18.sp else 24.sp, fontWeight = FontWeight.Black',
    content
)

with open("android/app/src/main/java/com/ethiobalance/app/ui/components/TelecomAssetCard.kt", "w") as f:
    f.write(content)

