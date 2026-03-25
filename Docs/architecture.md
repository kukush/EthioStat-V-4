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
- `smsParser.ts`: Logic to parse SMS messages (e.g., from banks or telecom providers) to extract transaction records and balances. Supports English, Amharic, and Afaan Oromo.
- `mockDataService.ts`: Provides mock data for development and testing.
- `persistenceService.ts`: Standardizes state sync between React memory and native Room DB.

### 4. Native Integration Layer (Capacitor & Room)
Native Android components provide high-reliability background processing:
- **Room Database**: The **primary source of truth** for all transactions and balances. 100% offline and local.
- **SmsForegroundService**: A background-eligible service that monitors incoming SMS messages in real-time without polling.
- **UssdAccessibilityService**: A fallback mechanism to capture USSD response text when standard callbacks are suppressed.
- **SmsMonitorPlugin**: The Capacitor bridge that syncs Room data to the React layer and triggers historical scans.

## Data Flow
1. **Initialization**: App loads `persistenceService` to retrieve existing state from local storage.
2. **Action Dispatch**: User interactions trigger actions dispatched to the `reducer`.
3. **State Update**: The reducer computes the new state.
4. **Persistence**: `useEffect` hooks listen to state changes and persist them locally.
5. **Native Operations**: Capacitor plugins read SMS messages and execute USSD codes through native Android services.

## Project Status & Implementation

### Completed Features
- **MVI Architecture**: Project refactored to align with Model-View-Intent architecture pattern
- **Native Integration**: SMS monitoring and USSD capture implemented via Capacitor plugins
- **Multilingual Support**: Smart parsing for English, Amharic, and Afaan Oromo languages
- **Background Processing**: Foreground service for reliable SMS monitoring
- **Historical Scanning**: 7-day SMS history scan when adding new transaction sources
- **Room Database**: Native SQLite storage for 100% offline functionality

### Build Fixes Applied
- **Duplicate Switch Case**: Resolved Vite build warning in `src/store.ts`
- **Gradle Optimization**: Updated Android build configuration for better metadata support
- **Permissions**: Added required SMS and phone permissions to AndroidManifest.xml

### Native Capabilities
- **SMS Monitoring**: Real-time background SMS processing via SmsForegroundService
- **USSD Capture**: Accessibility service for capturing USSD responses
- **Room Persistence**: Primary source of truth for all transactions and balances
- **Capacitor Bridge**: SmsMonitorPlugin syncs native data with React layer

For detailed deployment instructions, see [deployment-guide.md](./deployment-guide.md).
