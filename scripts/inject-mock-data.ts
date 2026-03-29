import * as fs from 'fs';
import * as path from 'path';

/**
 * This script demonstrates how mock data could be injected or prepared.
 * In a real application, you might use this to overwrite the localStorage state
 * when running in a specialized 'demo' mode or via a development bridge.
 */

import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const MOCK_DATA_PATH = path.join(__dirname, 'sample-mock-data.json');
const STORAGE_KEY = 'ethio_balance_state';

function injectMockData() {
  try {
    const rawData = fs.readFileSync(MOCK_DATA_PATH, 'utf8');
    const mockData = JSON.parse(rawData);
    
    console.log('--- Mock Data Injection Script ---');
    console.log(`Loaded mock state for: ${mockData.userProfile.name}`);
    console.log(`Total Transactions: ${mockData.transactions.length}`);
    
    // Note: This script runs in Node.js, so it can't directly access browser 'localStorage'.
    // However, it can be used to generate a script segment that could be executed in the browser console
    // or as part of a development build step.
    
    const injectionCode = `localStorage.setItem('${STORAGE_KEY}', '${JSON.stringify(mockData)}'); location.reload();`;
    
    console.log('\nTo inject this data in your browser console, run:');
    console.log('-------------------------------------------');
    console.log(injectionCode);
    console.log('-------------------------------------------');
    
  } catch (error) {
    console.error('Error reading mock data:', error);
  }
}

injectMockData();
