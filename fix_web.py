import re

with open("src/components/TelecomAssetCard.tsx", "r") as f:
    content = f.read()

# Let's fix the inner card classNames manually
content = content.replace('className={`flex flex-col justify-between rounded-2xl border ${isCompact ? "p-2" : "p-3.5"} rounded-2xl bg-gradient-to-b from-blue-950/40 to-slate-900 border border-blue-500/20 flex flex-col justify-between">', 'className={`rounded-2xl bg-gradient-to-b from-blue-950/40 to-slate-900 border border-blue-500/20 flex flex-col justify-between ${isCompact ? "p-2" : "p-3.5"}`}">')
content = content.replace('className={`flex flex-col justify-between rounded-2xl border ${isCompact ? "p-2" : "p-3.5"} rounded-2xl bg-gradient-to-b from-purple-950/40 to-slate-900 border border-purple-500/20 flex flex-col justify-between">', 'className={`rounded-2xl bg-gradient-to-b from-purple-950/40 to-slate-900 border border-purple-500/20 flex flex-col justify-between ${isCompact ? "p-2" : "p-3.5"}`}">')
content = content.replace('className={`flex flex-col justify-between rounded-2xl border ${isCompact ? "p-2" : "p-3.5"} rounded-2xl bg-gradient-to-b from-amber-950/40 to-slate-900 border border-amber-500/20 flex flex-col justify-between">', 'className={`rounded-2xl bg-gradient-to-b from-amber-950/40 to-slate-900 border border-amber-500/20 flex flex-col justify-between ${isCompact ? "p-2" : "p-3.5"}`}">')
content = content.replace('className={`flex flex-col justify-between rounded-2xl border ${isCompact ? "p-2" : "p-3.5"} rounded-2xl bg-gradient-to-b from-emerald-950/40 to-slate-900 border border-emerald-500/20 flex flex-col justify-between">', 'className={`rounded-2xl bg-gradient-to-b from-emerald-950/40 to-slate-900 border border-emerald-500/20 flex flex-col justify-between ${isCompact ? "p-2" : "p-3.5"}`}">')

# Wait, the closing `">` should just be `">` but let's check what I replaced it with.
# Currently it is `className={`flex flex-col justify-between rounded-2xl border ${isCompact ? "p-2" : "p-3.5"} rounded-2xl bg-gradient-to-b from-blue-950/40 to-slate-900 border border-blue-500/20 flex flex-col justify-between`}`
# No it has `"` at the end? Let me just write the correct content.
