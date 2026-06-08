# EthioStat — Play Store Creative Briefs

---

## 1. App Icon (512x512 PNG)

### Design Spec

| Property | Value |
|---|---|
| Size | 512 x 512 px |
| Format | PNG-32 (transparent background NOT allowed by Play Store — use solid bg) |
| Shape | Google Play auto-masks to rounded square; design within safe zone |
| Safe Zone | Central 66% (approximately 338x338 px) for key elements |

### Concept

- **Background**: Gradient from deep teal (`#0D9488`, Emerald600) to dark slate (`#1E293B`, Slate800) — conveys trust and modernity
- **Foreground Symbol**: A stylized Ethiopian Birr sign (ብር) combined with a subtle upward-trending line graph
  - The Birr symbol is bold white, slightly offset left
  - A small ascending line graph (3 data points) in Emerald400 (`#34D399`) arcs from the bottom-right of the Birr sign
- **Accent**: A tiny SIM card or signal icon (4px, white, 30% opacity) in the top-right corner to hint at telecom tracking
- **No text** on the icon — the app name appears below it in the Play Store
- **Style**: Flat/material design, no 3D effects, no gradients on the symbol itself

### Color Palette

| Element | Color | Hex |
|---|---|---|
| Background start | Emerald600 | `#0D9488` |
| Background end | Slate800 | `#1E293B` |
| Birr symbol | White | `#FFFFFF` |
| Graph line | Emerald400 | `#34D399` |
| SIM accent | White @ 30% | `#FFFFFF4D` |

---

## 2. Feature Graphic (1024x500 PNG)

### Design Spec

| Property | Value |
|---|---|
| Size | 1024 x 500 px |
| Format | PNG or JPEG |
| Text safe zone | Central 800 x 400 px (avoid edges — cropped on some devices) |

### Concept

- **Background**: Same gradient as icon (Emerald600 → Slate800), angled 135 degrees
- **Left side (60%)**: Text content
  - Line 1: "EthioStat" in white, bold, 48px (Manrope font)
  - Line 2: "Track Your Birr & Telecom" in Emerald300, 24px
  - Line 3: "100% Offline • 25+ Banks • 3 Languages" in white @ 70%, 16px
  - Small Ethiopian flag emoji 🇪🇹 next to "EthioStat"
- **Right side (40%)**: Phone mockup
  - A tilted (10°) smartphone frame showing the Home Dashboard screen
  - Visible elements: SummaryCard (net balance), TelecomAssetCard (data/voice/SMS), one TransactionItem
- **Bottom-left corner**: Subtle bank logo strip (CBE, Awash, Telebirr icons faded at 20% opacity)

---

## 3. Screenshot Plan (8 screenshots)

### Technical Requirements

| Property | Value |
|---|---|
| Minimum | 4 screenshots |
| Maximum | 8 screenshots |
| Recommended size | 1080 x 1920 px (phone) |
| Format | PNG or JPEG |
| Frame | Use device frame (Pixel 7 or similar) |

### Screenshot Sequence

Each screenshot should have a **caption bar** at the top (120px height, gradient background) with bold white text.

---

#### Screenshot 1 — Home Dashboard

**Caption**: "Your Complete Financial Picture"

**UI Scene**: Home screen showing:
- SummaryCard with net balance (e.g., ETB +12,500.50), income/expense trends
- Per-source breakdown with bank icons (CBE, Telebirr, Awash)
- Eye toggle for amount hiding

**Key Selling Point**: First impression — shows the core value proposition.

---

#### Screenshot 2 — Telecom Assets

**Caption**: "Track Your Data, Voice & SMS"

**UI Scene**: Telecom tab showing:
- TelecomAssetCard (dark card with data GB, voice minutes, SMS count)
- PackageCards below (internet package with circular progress, voice package, SMS package)
- Expiry dates and usage percentages visible

**Key Selling Point**: Differentiator — no other Ethiopian app does this.

---

#### Screenshot 3 — Transaction List

**Caption**: "Every Transaction, Automatically"

**UI Scene**: Transactions screen showing:
- SummaryCard at top (transaction count, income/expense totals)
- 4-5 TransactionItems with:
  - Category icons (shopping bag, phone, trending up/down)
  - Party names ("From: Abebe Kebede", "To: Ethio Telecom")
  - Source labels (TELEBIRR, CBE)
  - Ethiopian calendar dates

**Key Selling Point**: Automatic parsing — zero manual entry.

---

#### Screenshot 4 — Multi-Bank Support

**Caption**: "25+ Ethiopian Banks Supported"

**UI Scene**: Settings screen → Transaction Sources section showing:
- List of configured banks with their icons
- "Add Source" button
- Visible banks: CBE, Telebirr, Awash, Dashen, BOA

**Key Selling Point**: Breadth of coverage — works with almost every Ethiopian bank.

---

#### Screenshot 5 — Package Details

**Caption**: "Never Lose Track of Your Packages"

**UI Scene**: Close-up of PackageCards:
- Internet package: 3.5 GB remaining / 5 GB total, circular progress at 68%, 15 days left
- Voice package: 120 min remaining, green theme
- Bonus fund card with Amber theme
- Expiry date visible in Ethiopian calendar

**Key Selling Point**: Visual, at-a-glance package monitoring.

---

#### Screenshot 6 — Amharic Language

**Caption**: "በአማርኛ ይጠቀሙ — Use in Amharic"

**UI Scene**: Same Home Dashboard but in Amharic:
- All labels in Amharic script (ገቢ, ወጪ, ቀሪ ሂሳብ)
- Ethiopian calendar dates (e.g., ሚያዝ 21, 2018)
- Navigation tabs in Amharic

**Key Selling Point**: True localization — not just translated labels, but Ethiopian calendar.

---

#### Screenshot 7 — Privacy & Offline

**Caption**: "100% Offline — Your Data Stays Yours"

**UI Scene**: A split-screen concept:
- **Top half**: The app working normally (showing real transactions)
- **Bottom half**: A "No Internet" icon with airplane mode enabled
- Overlay badge: "🔒 Zero Data Sent"

**Key Selling Point**: Trust and privacy — critical for financial apps.

---

#### Screenshot 8 — Smart Categories

**Caption**: "Smart Parsing — Income, Expenses & More"

**UI Scene**: Transaction detail or filtered view showing:
- Income transaction (green, trending up icon, "From: Almaz Tadesse")
- Expense transaction (red, shopping cart icon, "To: Ethio Telecom")
- Category chips: PURCHASE, TRANSFER, LOAN, RECHARGE
- Amount formatting with ETB prefix

**Key Selling Point**: Intelligent categorization without manual tagging.

---

## Screenshot Caption Styles

| Element | Style |
|---|---|
| Caption background | Gradient: Emerald600 → Slate800 (match feature graphic) |
| Caption text | White, Manrope Bold, 32px |
| Caption height | 120px at top of 1920px screenshot |
| Device frame | Pixel 7 or Samsung Galaxy S23 |
| App content | Real data (use realistic Ethiopian names, amounts, dates) |

---

## Additional Assets (Optional)

### Promotional Video (30 seconds)

**Storyboard**:
1. (0-5s) — Phone receives SMS notification from "127" (Telebirr)
2. (5-12s) — App auto-parses: transaction appears in the list with party name and amount
3. (12-18s) — Dashboard updates: net balance changes, income/expense chart adjusts
4. (18-24s) — Swipe to Telecom tab: data packages visible with usage progress
5. (24-28s) — Language switch: Amharic → dates change to Ethiopian calendar
6. (28-30s) — Logo + "EthioStat — Know where your birr goes" + 🇪🇹

### Tablet Screenshots (optional, 7-inch and 10-inch)

Same content as phone screenshots, adapted for wider layouts. Material 3 adaptive layouts should handle this naturally.
