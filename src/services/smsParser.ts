import { BalancePackageEntity, ParsedSmsResult, TransactionCategory, TransactionType } from '../types';

export function parseIncomingSms(sender: string, body: string, timestamp: number = Date.now()): ParsedSmsResult {
  const cleanSender = sender.trim().toUpperCase();
  const lowerBody = body.toLowerCase();
  
  let source = 'TELEBIRR';
  if (['889', '847', 'CBE'].some(s => cleanSender.includes(s))) source = 'CBE';
  else if (['CBEBIRR'].some(s => cleanSender.includes(s))) source = 'CBEBIRR';
  else if (['127', 'TELEBIRR'].some(s => cleanSender.includes(s))) source = 'TELEBIRR';
  else if (['901', 'AWASH'].some(s => cleanSender.includes(s))) source = 'AWASH';
  else if (['996', '675', 'DASHEN'].some(s => cleanSender.includes(s))) source = 'DASHEN';
  else if (['815', '999', 'BOA', 'ABYSSINIA'].some(s => cleanSender.includes(s))) source = 'BOA';
  else if (['841', '896', 'COOP'].some(s => cleanSender.includes(s))) source = 'COOP';
  else if (['804', '805', '806', '994', '251994'].some(s => cleanSender.includes(s))) source = 'AIRTIME';
  else source = cleanSender || 'UNKNOWN';

  const packages: BalancePackageEntity[] = [];
  let scenario = 'GENERAL_TRANSACTION';
  let confidence = 0.8;
  let type: TransactionType | undefined = undefined;
  let amount: number | undefined = undefined;
  let fee: number | undefined = undefined;
  let balanceAfter: number | undefined = undefined;
  let reference: string | undefined = undefined;
  let partyName: string | undefined = undefined;
  let category: TransactionCategory = 'GENERAL';
  let airtimeBalance: number | undefined = undefined;
  let isRecharge = false;

  // 1. Check for EthioTelecom Multi-Package Status (*804# or *999# response)
  // e.g. "Your balance is 45.20 ETB. Valid until 15/09/2026; from 5GB Monthly Internet package is 3.4GB, expiry date on 10/09/2026; from 100 Min Voice package is 42 Min, expiry date on 05/09/2026"
  const balanceMatch = body.match(/(?:balance|haft\w+|ቀሪ\s*ሂሳብ).*?(?:is|:\s*|ነው\s*)?(?:ETB|ብር)?\s*([\d,]+\.?\d*)\s*(?:ETB|ብር)?/i);
  if (balanceMatch && balanceMatch[1]) {
    const rawVal = parseFloat(balanceMatch[1].replace(/,/g, ''));
    if (!isNaN(rawVal)) {
      if (source === 'AIRTIME' || cleanSender === '804' || cleanSender === '805') {
        airtimeBalance = rawVal;
      }
    }
  }

  // Multi-package or segment detection
  const internetMatch = body.match(/(?:is|remaining:?)\s*(\d+(?:\.\d+)?)\s*(GB|MB)/i) ||
                        body.match(/(\d+(?:\.\d+)?)\s*(GB|MB)\s*(?:Monthly|Daily|Weekly|Night)?\s*(?:Internet|Data)/i);
  if (internetMatch) {
    const val = parseFloat(internetMatch[1]);
    const unit = internetMatch[2].toUpperCase() as 'GB' | 'MB';
    packages.push({
      id: `pkg-internet-${Date.now()}`,
      type: 'internet',
      subType: lowerBody.includes('night') ? 'Night' : lowerBody.includes('daily') ? 'Daily' : lowerBody.includes('weekly') ? 'Weekly' : 'Monthly',
      totalAmount: unit === 'GB' ? val : val / 1024,
      remainingAmount: unit === 'GB' ? val : val / 1024,
      unit: 'GB',
      expiryDate: Date.now() + 15 * 24 * 60 * 60 * 1000,
      isActive: true,
      source: 'SMS',
      simId: 'SIM1',
    });
  }

  const voiceMatch = body.match(/(?:is|remaining:?)\s*(\d+)\s*Min/i) ||
                     body.match(/(\d+)\s*Min(?:ute)?s?\s*(?:Weekly|Monthly|Daily)?\s*(?:Voice|Package)?/i);
  if (voiceMatch) {
    const minVal = parseInt(voiceMatch[1], 10);
    packages.push({
      id: `pkg-voice-${Date.now()}`,
      type: 'voice',
      subType: lowerBody.includes('monthly') ? 'Monthly' : lowerBody.includes('daily') ? 'Daily' : 'Weekly',
      totalAmount: minVal,
      remainingAmount: minVal,
      unit: 'MIN',
      expiryDate: Date.now() + 7 * 24 * 60 * 60 * 1000,
      isActive: true,
      source: 'SMS',
      simId: 'SIM1',
    });
  }

  const smsPackMatch = body.match(/(\d+)\s*SMS/i);
  if (smsPackMatch && !lowerBody.includes('sms package is not')) {
    const smsVal = parseInt(smsPackMatch[1], 10);
    packages.push({
      id: `pkg-sms-${Date.now()}`,
      type: 'sms',
      subType: 'Weekly',
      totalAmount: smsVal,
      remainingAmount: smsVal,
      unit: 'SMS',
      expiryDate: Date.now() + 7 * 24 * 60 * 60 * 1000,
      isActive: true,
      source: 'SMS',
      simId: 'SIM1',
    });
  }

  // 2. Airtime recharge detection (e.g. "Your account has been recharged with 50.00 ETB")
  if (lowerBody.includes('recharged with') || lowerBody.includes('ሂሳብዎ ተሞልቷል') || cleanSender === '805') {
    isRecharge = true;
    type = 'INCOME';
    category = 'RECHARGE';
    scenario = 'RECHARGE';
    const rechargeAmount = body.match(/recharged\s+with\s+([\d,]+\.?\d*)\s*ETB/i) ||
                           body.match(/([\d,]+\.?\d*)\s*ETB/i);
    if (rechargeAmount && rechargeAmount[1]) {
      amount = parseFloat(rechargeAmount[1].replace(/,/g, ''));
    }
  }

  // 3. Telebirr / Bank transactions
  // Income patterns (Credited, received, deposit)
  else if (
    lowerBody.includes('credited with') ||
    lowerBody.includes('received') ||
    lowerBody.includes('deposited') ||
    lowerBody.includes('ገቢ ሆኗል') ||
    lowerBody.includes('ተቀብለዋል')
  ) {
    type = 'INCOME';
    category = lowerBody.includes('salary') ? 'SALARY' : 'TRANSFER';
    scenario = 'INCOME_DEPOSIT';
    const amtMatch = body.match(/(?:credited with|received|amount of|ETB)\s*[:]?\s*(?:ETB|ብር)?\s*([\d,]+\.?\d*)/i) ||
                     body.match(/([\d,]+\.?\d*)\s*(?:ETB|ብር)/i);
    if (amtMatch && amtMatch[1]) {
      amount = parseFloat(amtMatch[1].replace(/,/g, ''));
    }
  }
  // Expense / Payment patterns (Paid, sent, transferred, debited, withdrawn)
  else if (
    lowerBody.includes('paid') ||
    lowerBody.includes('sent') ||
    lowerBody.includes('transferred') ||
    lowerBody.includes('transfered') ||
    lowerBody.includes('transfer of') ||
    lowerBody.includes('debited') ||
    lowerBody.includes('withdrawn') ||
    lowerBody.includes('ከፍለዋል') ||
    lowerBody.includes('ተልኳል') ||
    lowerBody.includes('ወጪ ሆኗል')
  ) {
    if (lowerBody.includes('transferred to') || lowerBody.includes('transfer of') || lowerBody.includes('sent') || lowerBody.includes('ተልኳል')) {
      type = 'TRANSFER';
      category = 'TRANSFER';
      scenario = 'MONEY_TRANSFER';
    } else {
      type = 'EXPENSE';
      category = lowerBody.includes('electric') || lowerBody.includes('water') || lowerBody.includes('utility') || lowerBody.includes('bill')
        ? 'UTILITY'
        : lowerBody.includes('merchant') || lowerBody.includes('store') || lowerBody.includes('shop')
        ? 'SHOPPING'
        : lowerBody.includes('restaurant') || lowerBody.includes('cafe') || lowerBody.includes('food')
        ? 'DINING'
        : lowerBody.includes('package') || lowerBody.includes('airtime') || lowerBody.includes('internet')
        ? 'TELECOM'
        : 'GENERAL';
      scenario = 'PAYMENT_EXPENSE';
    }

    const amtMatch = body.match(/(?:paid|sent|transferred|transfered|transfer of|debited with|amount)\s*[:]?\s*(?:ETB|ብር)?\s*([\d,]+\.?\d*)/i) ||
                     body.match(/([\d,]+\.?\d*)\s*(?:ETB|ብር)/i);
    if (amtMatch && amtMatch[1]) {
      amount = parseFloat(amtMatch[1].replace(/,/g, ''));
    }
  }

  // Extract reference number
  const refMatch = body.match(/(?:Ref(?:erence)?|Transaction (?:ID|number)|Trans No|ID|Trx|ቁጥር)\s*[:]?\s*([A-Za-z0-9\-_]+)/i);
  if (refMatch && refMatch[1]) {
    reference = refMatch[1];
  }

  // Extract service fee
  const feeMatch = body.match(/(?:fee|charge|VAT|S\.charge).*?(?:is|of)?\s*(?:ETB)?\s*([\d,]+\.?\d*)/i);
  if (feeMatch && feeMatch[1]) {
    fee = parseFloat(feeMatch[1].replace(/,/g, ''));
  }

  // Extract new balance after transaction
  const newBalMatch = body.match(/(?:current balance|new (?:account )?balance|available balance|balance|ቀሪ ሂሳብ)\s*(?:is|:|ነው)?\s*(?:ETB|ብር)?\s*([\d,]+\.?\d*)/i);
  if (newBalMatch && newBalMatch[1]) {
    balanceAfter = parseFloat(newBalMatch[1].replace(/,/g, ''));
  }

  // Extract party name or recipient
  const partyMatch = body.match(/(?:to|from|by)\s+([A-Za-z0-9\s._]+?)(?:\s*\(|\s+on|\s+for|\s+at|\s+via|\.|\,|$)/i);
  if (partyMatch && partyMatch[1] && partyMatch[1].trim().length > 2 && partyMatch[1].trim().length < 40) {
    partyName = partyMatch[1].trim();
  }

  return {
    scenario,
    confidence,
    source,
    type,
    amount: amount || 0,
    fee,
    balanceAfter,
    reference: reference || `REF-${Math.floor(100000 + Math.random() * 900000)}`,
    partyName,
    category,
    airtimeBalance,
    packages,
    isRecharge,
  };
}
