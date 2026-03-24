# EthioStat Use Cases

EthioStat is designed to help users track their telecom packages, manage their airtime balances, and monitor financial transactions. Below are the primary use cases of the application.

## 1. Monitor Telecom Balances
**Actor**: Mobile User
**Description**: The user opens the app to view their current telecom balances, including main airtime, internet data, voice minutes, and SMS limits.
**Flow**:
1. User launches the application.
2. The `HomeScreen` displays a summary card with the total airtime balance.
3. The user navigates to the `TelecomScreen` to view detailed breakdowns of specific packages (e.g., Internet, Voice) and their expiry dates.

## 2. Track Financial Transactions
**Actor**: Mobile User
**Description**: The user wants to see a history of their income and expenses parsed from mobile money or banking SMS messages (e.g., Telebirr, CBE).
**Flow**:
1. User receives an SMS regarding a transaction.
2. (Future Native Integration) The app reads the SMS and parses it using `smsParser.ts`.
3. The parsed transaction is added to the application state.
4. The user navigates to the `TransactionScreen` to view a categorized list of all transactions, seeing the net flow (Income vs. Expense) from various sources.

## 3. Top-Up and Buy Packages
**Actor**: Mobile User
**Description**: The user views recommended bundles and initiates a recharge or package purchase.
**Flow**:
1. User navigates to the `TelecomScreen`.
2. App displays `RecommendedBundles` based on past usage or provider defaults.
3. User selects a bundle to purchase.
4. (Future Native Integration) The app executes the required USSD code to fulfill the purchase.

## 4. Customize Application Experience
**Actor**: Mobile User
**Description**: The user changes application preferences such as language (English, Amharic, Oromiffa) or theme (Dark, Light, Vibrant, Midnight, Forest).
**Flow**:
1. User navigates to the `SettingsScreen`.
2. User selects a new theme or language.
3. The `store.ts` updates the global state, instantly re-rendering the UI with the newly selected preferences.
4. `persistenceService.ts` saves the preferences to device storage.

## 5. Handle Gift Requests
**Actor**: Mobile User
**Description**: The user receives a request from a contact to send airtime or a data package.
**Flow**:
1. The app registers a `GiftRequest` (e.g., via SMS parsing).
2. The user sees a notification badge on the `BottomNav`.
3. The user opens the pending requests in the `TelecomScreen` and chooses to accept or reject the transmission of funds.
