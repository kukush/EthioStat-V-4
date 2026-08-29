import re

with open("src/components/TelecomAssetCard.tsx", "r") as f:
    content = f.read()

# Add isCompact to props
content = content.replace("interface TelecomAssetCardProps {", "interface TelecomAssetCardProps {\n  isCompact?: boolean;")
content = content.replace("}) => {", "  isCompact = false,\n}) => {")

# Remove quick action buttons if isCompact
buttons_regex = r"(\{/\* Quick USSD Action Buttons \*/\}.*?</div>\s*</div>)"
# Wait, let's just do a string replace for the buttons container.

