# EthioStat Architecture

## Overview
EthioStat (EthioBalance) is a hybrid mobile application built with React, Vite, and Tailwind CSS, wrapped in Capacitor to deploy natively on Android and iOS. 

The application tracks a user's telecom balances (internet, voice, SMS, and bonuses), processes transaction histories (income and expenses), and provides a user-friendly dashboard to view financial and telecom data.

## Technology Stack
- **Frontend Framework**: React 19
- **Build Tool**: Vite
- **Styling**: Tailwind CSS, lucide-react (icons), motion (animations)
- **State Management**: React Context / `useReducer` with a centralized store (`src/store.ts`)
- **Native Wrapper**: Capacitor (iOS & Android)
- **Language**: TypeScript

## System Architecture

### 1. Presentation Layer (UI)
The presentation layer is composed of React functional components categorized into complete screens and reusable UI elements.
- **Screens** (`src/screens/`):
  - `HomeScreen`: Main dashboard showing a summary of telecom packages and recent transactions.
  - `TelecomScreen`: Detailed view of telecom balances, active packages, and recommended bundles.
  - `TransactionScreen`: Transaction history, filtering by source and category.
  - `SettingsScreen`: App configuration (theme, language, sim preferences).
- **Components** (`src/components/`):
  - `BottomNav`, Modals, Cards, and other reusable UI elements.

### 2. State Management Layer
Centralized state management is implemented in `src/store.ts` using the `useReducer` hook.
- It manages:
  - App state (Theme, Language, Active Tab)
  - Data collections (SimCards, TelecomPackages, Transactions, Requests)
  - User details & Telecom Balances.

### 3. Service Layer
The business logic and data processing are decoupled into service modules (`src/services/`):
- `smsParser.ts`: Logic to parse SMS messages (e.g., from banks or telecom providers) to extract transaction records and balances.
- `mockDataService.ts`: Provides mock data for development and testing.
- `persistenceService.ts`: Handles saving and loading the application state using `localStorage` (which Capacitor persists on device).

### 4. Native Integration Layer (Capacitor)
Capacitor acts as the bridge between the web application and the native device. It is configured via `capacitor.config.json` and exposes native APIs (which will be configured to read SMS and access Phone State).

## Data Flow
1. **Initialization**: App loads `persistenceService` to retrieve existing state from local storage.
2. **Action Dispatch**: User interactions trigger actions dispatched to the `reducer`.
3. **State Update**: The reducer computes the new state.
4. **Persistence**: `useEffect` hooks listen to state changes and persist them locally.
5. **Native Operations**: Future updates will use Capacitor plugins to read actual SMS messages and execute USSD codes.
