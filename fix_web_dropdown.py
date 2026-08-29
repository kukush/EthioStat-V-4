import re

with open("src/screens/TransactionScreen.tsx", "r") as f:
    content = f.read()

# Replace the banks.map with banks.filter(b => b.enabled).map
content = content.replace(
    "{banks.map((b) => (\\n                <option key={b.id} value={b.abbreviation}>",
    "{banks.filter((b) => b.enabled).map((b) => (\\n                <option key={b.id} value={b.abbreviation}>"
)

# wait, the exact text is:
#              {banks.map((b) => (
#                <option key={b.id} value={b.abbreviation}>
#                  {b.abbreviation}
#                </option>
#              ))}
content = re.sub(
    r'\{banks\.map\(\(b\) => \(',
    r'{banks.filter((b) => b.enabled).map((b) => (',
    content
)

with open("src/screens/TransactionScreen.tsx", "w") as f:
    f.write(content)
