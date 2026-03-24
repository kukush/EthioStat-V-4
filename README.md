# EthioBalance

EthioBalance is a privacy-focused, offline-first financial and telecom asset management application designed for the Ethiopian market. It helps users track their mobile balances, data packages, and financial transactions by automatically parsing SMS messages from telecom providers and banks.

## Features

- **Dual-Tracking Architecture**: Separates telecom assets (Airtime, Data, Voice, SMS) from financial transactions (CBE, Telebirr, etc.).
- **Automatic SMS Parsing**: Scans incoming SMS from Ethio Telecom, Telebirr, and major Ethiopian banks to update balances and transaction history.
- **Privacy First**: All data is processed and stored locally on the device. No data is sent to external servers.
- **Multi-SIM Support**: Track balances and transactions across multiple SIM cards.
- **Transaction Management**: Categorize and filter transactions by source, search through history, and export data to CSV.
- **Telecom Management**: Monitor active packages, expiry dates, and get recommendations for new bundles.
- **Multilingual Support**: Available in English, Amharic, and Afaan Oromoo.

## Architecture

The application follows the **MVI (Model-View-Intent)** pattern for state management:

- **Model**: The `AppState` (defined in `src/types.ts`) represents the entire state of the application.
- **View**: React components (in `src/screens/` and `src/components/`) render the UI based on the current state.
- **Intent**: Actions (defined in `src/store.ts`) represent user intentions to change the state.

### Key Components

- **`smsParser.ts`**: The core logic for extracting structured data from unstructured SMS text.
- **`persistenceService.ts`**: Handles saving and loading the application state to/from local storage.
- **`store.ts`**: Contains the reducer and initial state for the application.

## Usage

1. **Initial Setup**: Grant SMS and Call permissions to allow the app to read messages and perform USSD calls.
2. **Sync Balances**: Tap the "Sync Now" button on the Telecom screen to trigger a USSD call. The app will automatically capture the response SMS.
3. **Add Transaction Sources**: In Settings, add the sender IDs of your banks (e.g., `CBE`, `Telebirr`) to start tracking financial transactions.
4. **Manage SIMs**: Register your phone numbers in Settings to link data to specific SIM cards.

## Development

### Prerequisites

- Node.js (v18+)
- npm

### Installation

```bash
npm install
```

### Running the App

```bash
npm run dev
```

### Building for Production

```bash
npm run build
```

## Exporting Data

You can export your transaction history to a CSV file from the Transactions screen. This allows you to perform further analysis in spreadsheet software like Excel or Google Sheets.

## License

MIT
# EthioStat-V-4
