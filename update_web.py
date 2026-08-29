import re

with open("src/components/TelecomAssetCard.tsx", "r") as f:
    content = f.read()

if "isCompact?: boolean" not in content:
    content = content.replace("interface TelecomAssetCardProps {", "interface TelecomAssetCardProps {\n  isCompact?: boolean;")
    content = content.replace("  onOpenUssd,", "  onOpenUssd,\n  isCompact = false,")

# Hide action buttons when compact
content = content.replace("{/* Quick USSD Action Buttons */}", "{/* Quick USSD Action Buttons */}\n        {!isCompact && (")
content = content.replace("          </button>\n        </div>", "          </button>\n        </div>\n        )}")

# Compact styles for padding and text size
# Change p-5 to p-4 or p-3 if compact
content = content.replace('p-5 shadow-xl', '${isCompact ? "p-3" : "p-5"} shadow-xl')
content = content.replace('className="rounded-3xl', 'className={`rounded-3xl ${isCompact ? "bg-slate-900 border border-slate-800" : "bg-slate-900 border border-slate-800"}`}')
# wait, better to use string formatting for the main container class
content = re.sub(r'className="rounded-3xl bg-slate-900 border border-slate-800 (p-5)? shadow-xl"', r'className={`rounded-3xl bg-slate-900 border border-slate-800 shadow-xl ${isCompact ? "p-3" : "p-5"}`}', content)

# Inner grid
content = content.replace('gap-3', '${isCompact ? "gap-2" : "gap-3"}')
content = re.sub(r'className="grid grid-cols-2 sm:grid-cols-4 (gap-3)?"', r'className={`grid grid-cols-2 sm:grid-cols-4 ${isCompact ? "gap-2" : "gap-3"}`}', content)

# p-3.5 to p-2 for inner cards
content = content.replace('p-3.5', '${isCompact ? "p-2" : "p-3.5"}')
# Since it's inside className="", we need to change it to className={``}
def replace_inner_card(match):
    cls = match.group(1)
    cls = cls.replace('p-3.5', '${isCompact ? "p-2" : "p-3.5"}')
    return f'className={{`{cls}`}}'

content = re.sub(r'className="(p-3\.5 rounded-2xl bg-gradient-to-b.*?)"', replace_inner_card, content)

# Change text-2xl to text-lg if compact
def replace_text_size(match):
    cls = match.group(1)
    cls = cls.replace('text-2xl', '${isCompact ? "text-lg" : "text-2xl"}')
    return f'className={{`{cls}`}}'

content = re.sub(r'className="(text-2xl font-black text-white tracking-tight)"', replace_text_size, content)

with open("src/components/TelecomAssetCard.tsx", "w") as f:
    f.write(content)

