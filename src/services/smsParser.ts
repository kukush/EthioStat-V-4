import { Transaction, TelecomPackage, PackageType, TransactionType } from '../types';
import { ETHIOPIAN_BANKS } from '../constants/banks';

export interface ParsedData {
  balance?: number;
  packages?: Omit<TelecomPackage, 'simId'>[];
  transaction?: Omit<Transaction, 'simId'>;
}

/**
 * Dual-Tracking SMS Parser
 * Separates "Telecom Assets" (MB, Min) from "Financial Transactions" (ETB)
 */
export const parseEthioSMS = (text: string, senderId?: string): ParsedData => {
  const data: ParsedData = {};
  const now = new Date();

  // Identify bank from senderId if provided
  const bank = senderId ? ETHIOPIAN_BANKS.find(b => 
    b.abbreviation.toLowerCase() === senderId.toLowerCase() || 
    senderId.toLowerCase().includes(b.abbreviation.toLowerCase()) ||
    (senderId === '830' && b.abbreviation === 'Telebirr')
  ) : null;

  const source = bank ? bank.abbreviation : (senderId || 'Unknown');

  // 1. Balance Check (*804#)
  // EN: "Your balance is 145.50 ETB. Valid until 2026-10-30."
  // AM: "ቀሪ ሂሳብዎ 145.50 ብር ነው። እስከ 2026-10-30 ድረስ ያገለግላል።"
  const balanceMatch = text.match(/(?:balance is|ቀሪ ሂሳብዎ|Hafteen keessan|balance\s*:)\s*([\d.]+)\s*(?:ETB|ብር)/i);
  if (balanceMatch) {
    data.balance = parseFloat(balanceMatch[1]);
  }

  // 2. Telecom Asset Gain (251994 / Gifts / Ethio Gebeta)
  // EN: "You have 450MB data remaining until 2026-03-24."
  // Gift: "You have received a gift of 1GB data from telebirr."
  // Ethio Gebeta: "You have successfully bought 1GB Ethio Gebeta data package."
  const dataMatch = text.match(/(?:gift of\s*|bought\s*)?([\d.]+)\s*(MB|GB)\s*(?:data|ኢንተርኔት|Intarneetii|Ethio Gebeta)?\s*(?:remaining|ቀሪ|hafe|gift|ስጦታ|package)?\s*(?:until|እስከ|hanga|from)?\s*([\d-]{10}|telebirr)?/i);
  if (dataMatch && dataMatch[1]) {
    const value = parseFloat(dataMatch[1]);
    const unit = dataMatch[2].toUpperCase() as 'MB' | 'GB';
    const expiryOrSource = dataMatch[3];
    
    if (!data.packages) data.packages = [];
    data.packages.push({
      id: `data-${Date.now()}`,
      type: 'internet',
      value,
      total: unit === 'GB' ? value : 1024,
      unit,
      label: (expiryOrSource === 'telebirr' || text.includes('gift')) ? 'Gifted Data' : (text.includes('Ethio Gebeta') ? 'Ethio Gebeta Data' : 'Data Package'),
      expiryDate: (expiryOrSource && expiryOrSource !== 'telebirr') ? expiryOrSource : 'N/A',
      daysLeft: 1,
      totalDays: 1
    });
  }

  // Voice/Minutes Gain
  // "You have received a gift of 50 Min from telebirr."
  // "You have 45 Min remaining until 2026-03-24."
  const voiceMatch = text.match(/(?:gift of\s*|bought\s*)?([\d.]+)\s*(?:Min|ደቂቃ|Daqiiqaa)\s*(?:remaining|ቀሪ|hafe|gift|ስጦታ|package)?/i);
  if (voiceMatch) {
    if (!data.packages) data.packages = [];
    data.packages.push({
      id: `voice-${Date.now()}`,
      type: 'voice',
      value: parseFloat(voiceMatch[1]),
      total: parseFloat(voiceMatch[1]),
      unit: 'Min',
      label: (text.includes('telebirr') || text.includes('gift')) ? 'Gifted Voice' : (text.includes('Ethio Gebeta') ? 'Ethio Gebeta Voice' : 'Voice Package'),
      expiryDate: 'N/A',
      daysLeft: 1,
      totalDays: 1
    });
  }

  // SMS Gain
  // "You have 100 SMS remaining until 2026-03-24."
  // "You have received a gift of 20 SMS from telebirr."
  const smsMatch = text.match(/(?:gift of\s*|bought\s*)?([\d.]+)\s*(?:SMS|መልዕክት)\s*(?:remaining|ቀሪ|hafe|gift|ስጦታ|package)?/i);
  if (smsMatch) {
    if (!data.packages) data.packages = [];
    data.packages.push({
      id: `sms-${Date.now()}`,
      type: 'sms',
      value: parseFloat(smsMatch[1]),
      total: parseFloat(smsMatch[1]),
      unit: 'SMS',
      label: (text.includes('telebirr') || text.includes('gift')) ? 'Gifted SMS' : (text.includes('Ethio Gebeta') ? 'Ethio Gebeta SMS' : 'SMS Package'),
      expiryDate: 'N/A',
      daysLeft: 1,
      totalDays: 1
    });
  }

  // 3. Gifts Sent / Airtime Transfer (*806# / *999#)
  // "You have transferred 10.00 ETB to 0911..."
  // "You have gifted 1GB data to 0911..."
  const giftSentMatch = text.match(/(?:transferred|gifted|ልከዋል|kennitaniittu)\s*(?:ETB|ብር)?\s*([\d,.]+)\s*(ETB|ብር|MB|GB|Min|SMS)?\s*(?:data|package)?\s*(?:to|ለ|iti)\s*([\d\w\s.-]+)/i);
  if (giftSentMatch) {
    const value = giftSentMatch[1].replace(/,/g, '');
    const unit = giftSentMatch[2] || '';
    const recipient = giftSentMatch[3].trim();
    
    data.transaction = {
      id: `gift-out-${Date.now()}`,
      type: 'expense',
      amount: (unit === 'ETB' || unit === 'ብር') ? parseFloat(value) : 0,
      source: source,
      description: `Gifted ${value}${unit} to ${recipient}`,
      timestamp: now.toISOString(),
      category: 'Gift'
    };
  }

  // 3. Loan Tracking (Airtime / Telebirr Endekise/Enderas)
  
  // Loan Received (Income)
  // "You have taken a loan of 10.00 ETB."
  // "You have received a loan of 500.00 ETB through Endekise."
  const loanReceivedMatch = text.match(/(?:taken|received)\s*(?:a)?\s*loan\s*(?:of)?\s*(?:ETB|ብር)?\s*([\d,.]+)\s*(?:ETB|ብር)?(?:\s*(?:through|from)\s*([\w\s]+))?/i);
  if (loanReceivedMatch) {
    const amount = parseFloat(loanReceivedMatch[1].replace(/,/g, ''));
    const loanSource = loanReceivedMatch[2]?.trim() || source;
    data.transaction = {
      id: `loan-in-${Date.now()}`,
      type: 'income',
      amount: amount,
      source: loanSource,
      description: `Loan received (${loanSource})`,
      timestamp: now.toISOString(),
      category: 'Loan'
    };
  }

  // Loan Repayment (Expense)
  // "You have repaid 510.00 ETB for your Endekise loan."
  // "Your loan of 10.00 ETB has been repaid."
  const loanRepaymentMatch = text.match(/(?:repaid|repayment)\s*(?:of)?\s*(?:ETB|ብር)?\s*([\d,.]+)\s*(?:ETB|ብር)?\s*(?:for|of)?\s*(?:your)?\s*([\w\s]+)?\s*loan/i) || 
                         text.match(/loan\s*(?:of)?\s*(?:ETB|ብር)?\s*([\d,.]+)\s*(?:ETB|ብር)?\s*has\s*been\s*repaid/i);
  if (loanRepaymentMatch && !data.transaction) {
    const amount = parseFloat(loanRepaymentMatch[1].replace(/,/g, ''));
    const loanSource = loanRepaymentMatch[2]?.trim() || source;
    data.transaction = {
      id: `loan-out-${Date.now()}`,
      type: 'expense',
      amount: amount,
      source: loanSource,
      description: `Loan repayment (${loanSource})`,
      timestamp: now.toISOString(),
      category: 'Repayment'
    };
  }

  // Service Fees (Expense)
  // "A service fee of 1.00 ETB will be charged upon recharge."
  // ACS (Airtime Credit Service) typically has a 15% fee if not specified
  const feeMatch = text.match(/(?:service fee|fee)\s*(?:of)?\s*(?:ETB|ብር)?\s*([\d,.]+)\s*(?:ETB|ብር)?/i);
  if (feeMatch && !data.transaction) {
    data.transaction = {
      id: `fee-${Date.now()}`,
      type: 'expense',
      amount: parseFloat(feeMatch[1].replace(/,/g, '')),
      source: source,
      description: 'Loan service fee',
      timestamp: now.toISOString(),
      category: 'Fee'
    };
  } else if (text.toLowerCase().includes('loan') && text.toLowerCase().includes('taken') && !feeMatch && data.transaction?.type === 'income') {
    // ACS 15% automatic tracking if fee not specified
    const estimatedFee = data.transaction.amount * 0.15;
    // We don't add a separate transaction here, but we could add it to description or metadata
    data.transaction.description += ` (Est. 15% Fee: ${estimatedFee.toFixed(2)} ETB)`;
  }

  // 4. Financial Tracking (830 / Banks)
  
  // Generic Income Pattern (Received/Credit)
  // CBE: "Your account 1000... has been credited with ETB 500.00 from ..."
  // Telebirr: "You have received 500.00 ETB from 0911223344."
  const incomeMatch = text.match(/(?:received|credited|ተቀብለዋል|ገቢ|fudhattaniittu)\s*(?:with|of)?\s*(?:ETB|ብር)?\s*([\d,.]+)\s*(?:ETB|ብር)?\s*(?:from|ከ|irraa)\s*([\d\w\s.-]+)/i);
  if (incomeMatch && !data.transaction) {
    data.transaction = {
      id: `tx-${Date.now()}`,
      type: 'income',
      amount: parseFloat(incomeMatch[1].replace(/,/g, '')),
      source: source,
      description: `Received from ${incomeMatch[2].trim()}`,
      timestamp: now.toISOString(),
      category: 'Transfer'
    };
  }

  // Generic Expense Pattern (Paid/Debit)
  // CBE: "Your account 1000... has been debited with ETB 130.00 for ..."
  // Telebirr: "You have paid 130.00 ETB for 1GB package."
  const expenseMatch = text.match(/(?:paid|debited|ከፍለዋል|ወጪ|kaffaltaniittu)\s*(?:with|of)?\s*(?:ETB|ብር)?\s*([\d,.]+)\s*(?:ETB|ብር)?\s*(?:for|ለ|iti|to)\s*([\d\w\s.-]+)/i);
  if (expenseMatch && !data.transaction) {
    data.transaction = {
      id: `tx-${Date.now()}`,
      type: 'expense',
      amount: parseFloat(expenseMatch[1].replace(/,/g, '')),
      source: source,
      description: `Payment for ${expenseMatch[2].trim()}`,
      timestamp: now.toISOString(),
      category: 'Payment'
    };
  }

  // Bank Specific: CBE Transfer
  if (text.includes('credited') && text.includes('CBE') && !data.transaction) {
    const amountMatch = text.match(/ETB\s*([\d,.]+)/);
    if (amountMatch) {
      data.transaction = {
        id: `tx-cbe-${Date.now()}`,
        type: 'income',
        amount: parseFloat(amountMatch[1].replace(/,/g, '')),
        source: 'CBE',
        description: 'Bank Credit',
        timestamp: now.toISOString(),
        category: 'Transfer'
      };
    }
  }

  return data;
};
