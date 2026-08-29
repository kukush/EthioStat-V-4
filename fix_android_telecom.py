import re

with open("android/app/src/main/java/com/ethiobalance/app/ui/screens/TelecomScreen.kt", "r") as f:
    content = f.read()

# Replace TelecomAssetCard invocation to include onOpenUssd
replacement = """        TelecomAssetCard(
            language = language,
            dataVol = dataVol,
            voiceVol = voiceVol,
            smsVol = smsVol,
            onOpenUssd = { action ->
                if (action == "recharge") {
                    showRechargeSheet = true
                } else if (action == "transfer") {
                    showTransferSheet = true
                }
            }
        )"""
content = re.sub(
    r'TelecomAssetCard\(\s*language = language,\s*dataVol = dataVol,\s*voiceVol = voiceVol,\s*smsVol = smsVol\s*\)',
    replacement,
    content
)

with open("android/app/src/main/java/com/ethiobalance/app/ui/screens/TelecomScreen.kt", "w") as f:
    f.write(content)

