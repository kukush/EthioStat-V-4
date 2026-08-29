import re

with open("android/app/src/main/java/com/ethiobalance/app/ui/viewmodel/TransactionViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "        .map { sources ->\n            sources.map { it.abbreviation to AppConstants.displaySource(it.abbreviation) }",
    "        .map { sources ->\n            sources.filter { it.isEnabled }.map { it.abbreviation to AppConstants.displaySource(it.abbreviation) }"
)

with open("android/app/src/main/java/com/ethiobalance/app/ui/viewmodel/TransactionViewModel.kt", "w") as f:
    f.write(content)
