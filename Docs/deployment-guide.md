# EthioStat Deployment Guide

This guide provides comprehensive instructions for building, deploying, and testing the EthioStat application on Android devices.

## Prerequisites

- Physical Android device with USB debugging enabled
- Android SDK and development tools installed
- Node.js and npm/yarn package manager
- Capacitor CLI installed globally

## Build Process

### 1. Environment Setup
Ensure all environment variables are configured in the `.env` file:
```bash
# Copy example environment file if needed
cp .env.example .env
```

### 2. Install Dependencies
```bash
npm install
```

### 3. Download Required Assets
The build process automatically downloads bank logos and other assets:
```bash
npm run prebuild  # Downloads logos via scripts/download-logos.ts
```

### 4. Build Application
```bash
npm run build    # Builds the React app and downloads assets
```

### 5. Sync with Capacitor
```bash
npx cap sync     # Syncs web assets with native platforms
```

## Deployment

### Automated Deployment Script
Use the provided deployment script for streamlined deployment:

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

The deployment script handles:
- Asset downloading
- Application building
- Capacitor synchronization
- Android deployment to connected device

### Manual Deployment Steps
If you prefer manual deployment:

1. **Build the application**:
   ```bash
   npm run build
   ```

2. **Sync Capacitor**:
   ```bash
   npx cap sync
   ```

3. **Open in Android Studio** (optional):
   ```bash
   npx cap open android
   ```

4. **Deploy to device**:
   ```bash
   npx cap run android
   ```

## Device Configuration

### Automatic Permission Setup
Use the device configuration script to automatically grant required permissions:

```bash
chmod +x scripts/device-config.sh
./scripts/device-config.sh
```

### Manual Permission Setup
If automatic setup fails, manually grant these permissions through Android Settings:

1. **SMS Permissions**:
   - READ_SMS
   - RECEIVE_SMS

2. **Phone Permissions**:
   - CALL_PHONE
   - READ_PHONE_STATE

3. **Accessibility Service**:
   - Navigate to Settings > Accessibility
   - Find "EthioStat USSD Capture"
   - Enable the service

## Testing Native Features

### 1. Background SMS Monitoring
Test the SMS monitoring functionality:

1. Deploy the app to your device using `./scripts/deploy.sh`
2. Send a test SMS to the device from another phone
3. Verify the "EthioStat Monitoring" foreground service notification appears in the system tray
4. Check that SMS messages are being processed and stored

### 2. USSD Capture Testing
Test the USSD accessibility service:

1. Enable the accessibility service in Android Settings
2. Trigger a USSD call from the app (e.g., Check Balance feature)
3. Verify the system popup appears and is automatically processed
4. Confirm the USSD response is captured and displayed in the app

### 3. Historical SMS Scanning
Test the 7-day historical scan feature:

1. Navigate to Settings in the app
2. Add a new transaction source (e.g., "CBE")
3. Verify the app initiates a historical SMS scan
4. Check that found transactions are parsed and added to the database
5. Confirm a new dedicated card appears on the Dashboard

### 4. Room Database Persistence
Verify data persistence:

1. Add transactions through SMS or manual entry
2. Force-close the app
3. Reopen the app
4. Verify all data is preserved and loaded correctly

## Multilingual Support Testing

The app supports three languages with smart parsing:

### Language Detection
- **English**: Standard ASCII characters
- **Amharic**: Unicode range analysis for Ethiopic script
- **Afaan Oromo**: Mixed script detection

### Testing Language Parsing
1. Send SMS messages in different languages
2. Verify correct language detection
3. Confirm accurate transaction parsing for each language
4. Test regex engines for Ethio Telecom and Telebirr messages

## Troubleshooting

### Common Build Issues

**Issue**: Build fails with asset download errors
**Solution**: Check internet connection and run `npm run prebuild` separately

**Issue**: Capacitor sync fails
**Solution**: Clean build directory with `npm run clean` and rebuild

**Issue**: Android deployment fails
**Solution**: Ensure USB debugging is enabled and device is properly connected

### Runtime Issues

**Issue**: SMS monitoring not working
**Solution**: Verify SMS permissions are granted and foreground service is running

**Issue**: USSD capture not functioning
**Solution**: Ensure accessibility service is enabled in Android Settings

**Issue**: Historical scan not finding messages
**Solution**: Check SMS permissions and verify messages exist in the specified timeframe

### Performance Optimization

- **Background Processing**: SMS monitoring runs as a foreground service for reliability
- **Database Operations**: All transactions stored in Room database for offline access
- **Memory Management**: React state synced with native storage for data integrity

## Environment Variables

Key environment variables used in deployment:

```bash
# Feature flags
VITE_ENABLE_SMS_PARSING=true
VITE_ENABLE_USSD=true

# Default configuration
VITE_DEFAULT_LANGUAGE=en
VITE_DEFAULT_THEME=light

# Mock data settings (development)
VITE_USE_MOCK_DATA=false
```

## Security Considerations

- All SMS processing happens on-device (100% offline)
- No internet connection required for core functionality
- Room database provides local data encryption
- Sensitive permissions are requested only when needed

## Next Steps

After successful deployment:

1. **User Testing**: Conduct thorough testing with real SMS messages
2. **Performance Monitoring**: Monitor app performance and battery usage
3. **Feature Validation**: Verify all use cases work as expected
4. **Documentation Updates**: Update user documentation based on testing results

---

**Note**: If you encounter environment restrictions during automated deployment, run `npm run build` and `npx cap sync` manually in your primary terminal before using the deployment script.
