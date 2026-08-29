import re

with open("android/app/src/main/java/com/ethiobalance/app/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    "            airtimeBalance = 0.0 // Set if available\n        )",
    "            airtimeBalance = 0.0, // Set if available\n            isCompact = true\n        )"
)

with open("android/app/src/main/java/com/ethiobalance/app/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)

