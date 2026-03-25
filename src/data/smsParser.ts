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

  // Universal Balance Parsing (works for any sender)
  const universalBalanceRegex = lang === 'am' 
    ? /ቀሪ ሂሳብዎ ([\d,.]+) ብር/ 
    : /(?:balance is|remaining|ቀሪ)\s*([\d,.]+)\s*(?:ETB|ብር)?/i;
  const balanceMatch = text.match(universalBalanceRegex);
  if (balanceMatch) data.balance = parseFloat(balanceMatch[1].replace(/,/g, ''));

    // Universal Package Parsing (works for any sender)
  if (!data.packages) data.packages = [];
  
  // Data packages (MB/GB) - enhanced pattern
  const dataMatch = text.match(/([\d,.]+)\s*(MB|GB)\s*(?:data|remaining|ኢንተርኔት|Intarneetii)/i);
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

  // Special case for Ethio Gebeta Data
  if (text.toLowerCase().includes('ethio gebeta') && text.toLowerCase().includes('data')) {
    const gebetaMatch = text.match(/([\d,.]+)\s*(MB|GB)\s*(?:ethio gebeta)?/i);
    if (gebetaMatch) {
      data.packages.push({
        id: `gebeta-${now.getTime()}`,
        type: 'internet',
        value: parseFloat(gebetaMatch[1].replace(/,/g, '')),
        total: parseFloat(gebetaMatch[1].replace(/,/g, '')),
        unit: gebetaMatch[2].toUpperCase(),
        label: 'Ethio Gebeta Data',
        expiryDate: 'N/A',
        daysLeft: 1,
        totalDays: 1
      });
    }
  }

  // Voice packages (Min)
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

  // SMS packages
  const smsMatch = text.match(/([\d,.]+)\s*(?:SMS|ኤስኤምኤስ)/i);
  if (smsMatch) {
    data.packages.push({
      id: `sms-${now.getTime()}`,
      type: 'sms',
      value: parseFloat(smsMatch[1].replace(/,/g, '')),
      total: parseFloat(smsMatch[1].replace(/,/g, '')),
      unit: 'SMS',
      label: 'SMS Package',
      expiryDate: 'N/A',
      daysLeft: 1,
      totalDays: 1
    });
  }

  // Gift patterns
  if (text.toLowerCase().includes('gift') || text.toLowerCase().includes('received a gift')) {
    // Gifted data
    const giftDataMatch = text.match(/([\d,.]+)\s*(MB|GB)\s*(?:data|ኢንተርኔት)/i);
    if (giftDataMatch) {
      data.packages.push({
        id: `gift-data-${now.getTime()}`,
        type: 'internet',
        value: parseFloat(giftDataMatch[1].replace(/,/g, '')),
        total: parseFloat(giftDataMatch[1].replace(/,/g, '')),
        unit: giftDataMatch[2].toUpperCase(),
        label: 'Gifted Data',
        expiryDate: 'N/A',
        daysLeft: 1,
        totalDays: 1
      });
    }
    
    // Gifted voice
    const giftVoiceMatch = text.match(/([\d,.]+)\s*(?:Min|ደቂቃ)/i);
    if (giftVoiceMatch) {
      data.packages.push({
        id: `gift-voice-${now.getTime()}`,
        type: 'voice',
        value: parseFloat(giftVoiceMatch[1].replace(/,/g, '')),
        total: parseFloat(giftVoiceMatch[1].replace(/,/g, '')),
        unit: 'Min',
        label: 'Gifted Voice',
        expiryDate: 'N/A',
        daysLeft: 1,
        totalDays: 1
      });
    }
    
    // Gifted SMS
    const giftSmsMatch = text.match(/([\d,.]+)\s*(?:SMS|ኤስኤምኤስ)/i);
    if (giftSmsMatch) {
      data.packages.push({
        id: `gift-sms-${now.getTime()}`,
        type: 'sms',
        value: parseFloat(giftSmsMatch[1].replace(/,/g, '')),
        total: parseFloat(giftSmsMatch[1].replace(/,/g, '')),
        unit: 'SMS',
        label: 'Gifted SMS',
        expiryDate: 'N/A',
        daysLeft: 1,
        totalDays: 1
      });
    }
  }

  // Transaction Parsing
  // Bank transactions (CBE, Awash, etc.)
  const isBank = sender.includes('BANK') || ['CBE', 'BOA', 'DASHEN', 'AWASH'].includes(sender);
  if (isBank) {
    // Credit transactions
    const creditMatch = text.match(/(?:credited|credited with|ገቢ|Galii)\s*(?:ETB|ብር)?\s*([\d,.]+)/i);
    if (creditMatch) {
      data.transaction = {
        id: `bank-${now.getTime()}`,
        type: 'income',
        amount: parseFloat(creditMatch[1].replace(/,/g, '')),
        source: sender === 'CBE' ? 'CBE' : sender === 'AWASH' ? 'Awash' : sender,
        description: `${sender} Transaction`,
        timestamp: now.toISOString(),
        category: 'Income'
      };
    }
    
    // Debit transactions
    const debitMatch = text.match(/(?:debited|debited with|ወጪ|Kaffaltii)\s*(?:ETB|ብር)?\s*([\d,.]+)/i);
    if (debitMatch) {
      data.transaction = {
        id: `bank-${now.getTime()}`,
        type: 'expense',
        amount: parseFloat(debitMatch[1].replace(/,/g, '')),
        source: sender === 'CBE' ? 'CBE' : sender === 'AWASH' ? 'Awash' : sender,
        description: `${sender} Transaction`,
        timestamp: now.toISOString(),
        category: 'Expense'
      };
    }
  }
  
  // Telebirr and Mobile Money transactions
  if (sender.includes('TELEBIRR') || ['830', '806', '999', '810'].includes(sender)) {
    const source = sender.includes('TELEBIRR') || ['830', '806', '999'].includes(sender) ? 'Telebirr' : sender;
    
    // Loan repayment (highest priority to avoid conflicts with loan received)
    const loanRepaymentMatch = text.match(/(?:loan of)\s*([\d,.]+)\s*(?:ETB|ብር)?\s*(?:has been repaid)/i);
    if (loanRepaymentMatch) {
      data.transaction = {
        id: `repayment-${now.getTime()}`,
        type: 'expense',
        amount: parseFloat(loanRepaymentMatch[1].replace(/,/g, '')),
        source: source,
        description: 'Loan Repayment',
        timestamp: now.toISOString(),
        category: 'Repayment'
      };
    }
    
    // Other loan repayment patterns
    const otherRepaymentMatch = text.match(/(?:repaid|repayment of)\s*([\d,.]+)\s*(?:ETB|ብር)?/i);
    if (otherRepaymentMatch && !loanRepaymentMatch) {
      data.transaction = {
        id: `repayment-${now.getTime()}`,
        type: 'expense',
        amount: parseFloat(otherRepaymentMatch[1].replace(/,/g, '')),
        source: source,
        description: 'Loan Repayment',
        timestamp: now.toISOString(),
        category: 'Repayment'
      };
    }
    
    // Loan received (lower priority)
    const loanReceivedMatch = text.match(/(?:loan of|taken a loan of|received a loan of)\s*([\d,.]+)\s*(?:ETB|ብር)?/i);
    if (loanReceivedMatch && !loanRepaymentMatch && !otherRepaymentMatch) {
      data.transaction = {
        id: `loan-${now.getTime()}`,
        type: 'income',
        amount: parseFloat(loanReceivedMatch[1].replace(/,/g, '')),
        source: source,
        description: 'Loan Received',
        timestamp: now.toISOString(),
        category: 'Loan'
      };
    }
    
    // Payment/Purchase transactions
    const paymentMatch = text.match(/(?:paid|pay|payment|purchase)\s*([\d,.]+)\s*(?:ETB|ብር)?/i);
    if (paymentMatch && !loanReceivedMatch && !loanRepaymentMatch && !otherRepaymentMatch) {
      data.transaction = {
        id: `payment-${now.getTime()}`,
        type: 'expense',
        amount: parseFloat(paymentMatch[1].replace(/,/g, '')),
        source: source,
        description: 'Payment/Purchase',
        timestamp: now.toISOString(),
        category: 'Expense'
      };
    }
    
    // Service fee (lowest priority)
    const serviceFeeMatch = text.match(/(?:service fee|fee of)\s*([\d,.]+)\s*(?:ETB|ብር)?/i);
    if (serviceFeeMatch && !loanReceivedMatch && !loanRepaymentMatch && !otherRepaymentMatch && !paymentMatch) {
      data.transaction = {
        id: `fee-${now.getTime()}`,
        type: 'expense',
        amount: parseFloat(serviceFeeMatch[1].replace(/,/g, '')),
        source: source,
        description: 'Service Fee',
        timestamp: now.toISOString(),
        category: 'Fee'
      };
    }
    
    // Transfer/Gift sent
    const transferMatch = text.match(/(?:transferred|gifted)\s*([\d,.]+)\s*(?:ETB|ብር)?/i);
    if (transferMatch) {
      data.transaction = {
        id: `transfer-${now.getTime()}`,
        type: 'expense',
        amount: parseFloat(transferMatch[1].replace(/,/g, '')),
        source: source,
        description: 'Transfer/Gift Sent',
        timestamp: now.toISOString(),
        category: 'Gift'
      };
    }
    
    // Package gifted (without amount)
    const packageGiftMatch = text.match(/(?:gifted|transferred)\s*[\d,.]*\s*(MB|GB|SMS)/i);
    if (packageGiftMatch && !transferMatch) {
      data.transaction = {
        id: `package-gift-${now.getTime()}`,
        type: 'expense',
        amount: 0,
        source: source,
        description: 'Package Gifted',
        timestamp: now.toISOString(),
        category: 'Gift'
      };
    }
  }

  return data;
};
