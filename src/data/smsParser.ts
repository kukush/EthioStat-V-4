import { Transaction, TelecomPackage, PackageType } from '../domain/types';

export interface ParsedData {
  balance?: number;
  packages?: Omit<TelecomPackage, 'simId'>[];
  transaction?: Omit<Transaction, 'simId'>;
  language?: 'en' | 'am' | 'om';
}

/**
 * Smart Language Detection using Unicode Range Analysis
 */
const detectLanguage = (text: string): 'en' | 'am' | 'om' => {
  const amharicRegex = /[\u1200-\u137F]/;
  if (amharicRegex.test(text)) return 'am';
  
  // Oromo patterns (Latin based but uses specific words like 'haala', 'galii', 'herrega')
  const oromoKeywords = ['haala', 'galii', 'herrega', 'kennamee', 'fudhattaniirtu'];
  const lowerText = text.toLowerCase();
  if (oromoKeywords.some(word => lowerText.includes(word))) return 'om';
  
  return 'en';
};

/**
 * Dual-Tracking SMS Parser: Multilingual & Multi-Source
 */
export const parseEthioSMS = (text: string, senderId?: string): ParsedData => {
  const data: ParsedData = {};
  const lang = detectLanguage(text);
  data.language = lang;
  const now = new Date();
  
  const sender = senderId?.toUpperCase() || 'UNKNOWN';

  // 1. Telecom Parsing (Ethio Telecom / Safaricom)
  if (sender === '251994' || sender === 'ETHIOTELECOM' || sender === '994') {
    // Balance
    const balanceRegex = lang === 'am' 
      ? /ቀሪ ሂሳብዎ ([\d,.]+) ብር/ 
      : /balance is ([\d,.]+) ETB/i;
    const balanceMatch = text.match(balanceRegex);
    if (balanceMatch) data.balance = parseFloat(balanceMatch[1].replace(/,/g, ''));

    // Packages
    if (!data.packages) data.packages = [];
    
    // Data (MB/GB)
    const dataMatch = text.match(/([\d,.]+)\s*(MB|GB)\s*(?:data|ኢንተርኔት|Intarneetii)/i);
    if (dataMatch) {
      data.packages.push({
        id: `data-${now.getTime()}`,
        type: 'internet',
        value: parseFloat(dataMatch[1].replace(/,/g, '')),
        total: parseFloat(dataMatch[1].replace(/,/g, '')),
        unit: dataMatch[2].toUpperCase(),
        label: lang === 'am' ? 'የዳታ ጥቅል' : lang === 'om' ? 'Paakeejii Deetaa' : 'Data Package',
        expiryDate: 'N/A',
        daysLeft: 1,
        totalDays: 1
      });
    }

    // Voice (Min)
    const voiceMatch = text.match(/([\d,.]+)\s*(?:Min|ደቂቃ|Daqiiqaa)/i);
    if (voiceMatch) {
      data.packages.push({
        id: `voice-${now.getTime()}`,
        type: 'voice',
        value: parseFloat(voiceMatch[1].replace(/,/g, '')),
        total: parseFloat(voiceMatch[1].replace(/,/g, '')),
        unit: 'Min',
        label: lang === 'am' ? 'የድምፅ ጥቅል' : lang === 'om' ? 'Paakeejii Sagalee' : 'Voice Package',
        expiryDate: 'N/A',
        daysLeft: 1,
        totalDays: 1
      });
    }
  }

  // 2. Mobile Money Parsing (Telebirr 830)
  if (sender === '830' || sender.includes('TELEBIRR')) {
    const amountMatch = text.match(/(?:received|ተቀብለዋል|fudhattaniirtu)\s*(?:ETB|ብር)?\s*([\d,.]+)/i);
    if (amountMatch) {
      data.transaction = {
        id: `tele-${now.getTime()}`,
        type: 'income',
        amount: parseFloat(amountMatch[1].replace(/,/g, '')),
        source: 'Telebirr',
        description: 'Mobile Money Received',
        timestamp: now.toISOString(),
        category: 'Income'
      };
    }
    const balMatch = text.match(/balance is ([\d,.]+) ETB/i);
    if (balMatch) data.balance = parseFloat(balMatch[1].replace(/,/g, ''));
  }

  // 3. Bank Parsing (Generic CR/DR)
  const isBank = sender.includes('BANK') || ['CBE', 'BOA', 'DASHEN', 'AWASH'].includes(sender);
  if (isBank) {
    const crMatch = text.match(/(?:CR|CREDITED|ገቢ|Galii)\s*(?:ETB|ብር)?\s*([\d,.]+)/i);
    const drMatch = text.match(/(?:DR|DEBITED|ወጪ|Kaffaltii)\s*(?:ETB|ብር)?\s*([\d,.]+)/i);
    
    if (crMatch || drMatch) {
      const isIncome = !!crMatch;
      const amount = parseFloat((crMatch || drMatch)![1].replace(/,/g, ''));
      data.transaction = {
        id: `bank-${now.getTime()}`,
        type: isIncome ? 'income' : 'expense',
        amount: amount,
        source: sender,
        description: `${sender} Transaction`,
        timestamp: now.toISOString(),
        category: isIncome ? 'Income' : 'Expense'
      };
    }
  }

  return data;
};
