export interface ParsedPackage {
  type: 'internet' | 'voice' | 'sms' | 'airtime' | 'bonus';
  value: number;
  unit: string;
  total?: number;
  expiryDate?: string;
  label?: string;
}

export interface ParsedTransaction {
  type: 'income' | 'expense';
  amount: number;
  source: string;
  category: string;
}

export interface SmsParseResult {
  balance?: number;
  packages: ParsedPackage[];
  transaction?: ParsedTransaction;
}

const TELEBIRR_SENDERS = new Set(['127', 'TELEBIRR', 'telebirr', '830']);
const BALANCE_SENDERS = new Set(['127', '804', '994', '251994', '8994', '810']);
const TRUSTED_SENDERS = new Set([
  '127', 'TELEBIRR', '830',
  '847', 'CBE', 'CBEBirr', 'CBEBIRR',
  '901', 'Awash', 'AwashBank',
  '815', '721', 'Dashen', 'DashenBank',
  '896', 'Coopbank',
  '844', '889', '812', '865', '252',
  '710', '811', '846', '921', '767', '946', '801', '842', '898',
  '881', '899', '895', '808', '810', 'ACSI',
  '994', '251994', '804', '806', '8994',
  '999',
]);

function resolveSource(senderId: string): string {
  const upper = senderId.toUpperCase();
  if (upper.includes('TELEBIRR') || TELEBIRR_SENDERS.has(senderId)) return 'Telebirr';
  if (upper.includes('CBE') || senderId === '847') return 'CBE';
  if (upper.includes('AWASH') || senderId === '901') return 'Awash';
  if (upper.includes('DASHEN') || senderId === '721') return 'Dashen';
  if (upper.includes('COOP') || senderId === '896') return 'Coopbank';
  const ethioTelecomSenders = new Set(['804', '805', '806', '810', '994', '251994', '8994']);
  if (ethioTelecomSenders.has(senderId)) return 'EthioTelecom';
  return senderId;
}

export function parseEthioSMS(text: string, senderId?: string): SmsParseResult {
  const result: SmsParseResult = { packages: [] };
  const lower = text.toLowerCase();
  const sender = senderId ?? '';

  // Helper: parse "with expiry date on YYYY-MM-DD" → ISO date string or undefined
  function parseExpiryDate(seg: string): string | undefined {
    const m = seg.match(/with expiry date on (\d{4}-\d{2}-\d{2})/i);
    return m ? m[1] : undefined;
  }

  // 1-3. Package parsing: detect multi-segment Telebirr remaining-balance format
  // Separator can be ";  from " (2+ spaces) so use regex, not literal string match.
  const isMultiSegment = /;\s+from\s/i.test(text) && /expiry date on/i.test(text);

  // C7: Add or replace package using "largest total wins" strategy.
  const addOrReplace = (pkg: ParsedPackage) => {
    const existingIdx = result.packages.findIndex(p => p.type === pkg.type);
    if (existingIdx < 0) {
      result.packages.push(pkg);
    } else if ((pkg.total ?? 0) > (result.packages[existingIdx].total ?? 0)) {
      result.packages[existingIdx] = pkg;
    }
    // else: keep existing (first-seen wins when totals are equal)
  };

  if (isMultiSegment) {
    const segments = text.split(/;\s*/);
    segments.forEach((seg, segIdx) => {
      const segLower = seg.toLowerCase();
      const expiryDate = parseExpiryDate(seg);

      if (segLower.includes('min') || segLower.includes('voice') || segLower.includes('ደቂቃ')) {
        // Voice segment
        const isIdx = seg.toLowerCase().indexOf('is ');
        const beforeIs = isIdx > 0 ? seg.substring(0, isIdx) : seg;
        const totalMatch = beforeIs.match(/(\d[\d,.]*)\s*Min(?!ute)/i);
        const totalVal = totalMatch ? parseFloat(totalMatch[1].replace(',', '')) : 0;
        const remainMatch = seg.match(/is\s+(\d[\d,.]*)\s*(?:Min(?:ute)?|ደቂቃ)/i);
        const remainVal = remainMatch ? parseFloat(remainMatch[1].replace(',', '')) : totalVal;
        if (remainVal > 0) {
          addOrReplace({
            type: 'voice', value: remainVal, unit: 'Min',
            total: totalVal > 0 ? totalVal : remainVal,
            expiryDate,
          });
        }
      } else if (segLower.includes('mb') || segLower.includes('gb') || segLower.includes('internet')) {
        // Data segment
        const isIdx = seg.toLowerCase().indexOf('is ');
        const beforeIs = isIdx > 0 ? seg.substring(0, isIdx) : seg;
        const totalGb = beforeIs.match(/(\d[\d,.]*)\s*GB/i);
        const totalMb = beforeIs.match(/(\d[\d,.]*)\s*MB/i);
        const totalValMB = totalGb
          ? parseFloat(totalGb[1].replace(',', '')) * 1024
          : totalMb ? parseFloat(totalMb[1].replace(',', '')) : 0;
        const remainGb = seg.match(/is\s+(\d[\d,.]*)\s*GB/i);
        const remainMb = seg.match(/is\s+(\d[\d,.]*)\s*MB/i);
        const remainVal = remainGb
          ? parseFloat(remainGb[1].replace(',', '')) * 1024
          : remainMb ? parseFloat(remainMb[1].replace(',', '')) : totalValMB;
        if (remainVal > 0) {
          addOrReplace({
            type: 'internet', value: remainVal, unit: 'MB',
            total: totalValMB > 0 ? totalValMB : remainVal,
            expiryDate,
          });
        }
      } else if (segLower.includes('sms') || segLower.includes('ኤስኤምኤስ')) {
        // SMS segment
        const remainSms = seg.match(/is\s+(\d[\d,.]*)\s*(?:SMS|ኤስኤምኤስ)/i)
          ?? seg.match(/(\d[\d,.]*)\s*(?:SMS|ኤስኤምኤስ)/i);
        const remainVal = remainSms ? parseFloat(remainSms[1].replace(',', '')) : 0;
        if (remainVal > 0) {
          addOrReplace({
            type: 'sms', value: remainVal, unit: 'SMS',
            total: remainVal,
            expiryDate,
          });
        }
      } else if (segLower.includes('bonus')) {
        // Bonus Fund segment — "Bonus Fund is 7.50 Birr"
        const bonusMatch = seg.match(/is\s+(\d[\d,.]*)\s*(?:Birr|ETB|ብር)/i);
        const bonusVal = bonusMatch ? parseFloat(bonusMatch[1].replace(',', '')) : 0;
        if (bonusVal > 0) {
          addOrReplace({
            type: 'bonus', value: bonusVal, unit: 'ETB',
            total: bonusVal,
            expiryDate,
          });
        }
      }
    });
  } else {
    // Single-match fallback for simple purchase/gift/recharge messages
    const dataMatch = text.match(/([\d,.]+)\s*(MB|GB)(?:\s+\w+)*\s*(?:data|internet|remaining|ኢንተርኔት|Intarneetii)/i);
    if (dataMatch) {
      const value = parseFloat(dataMatch[1].replace(',', ''));
      const unit = dataMatch[2].toUpperCase();
      const isGifted = lower.includes('gift') || lower.includes('received a gift');
      result.packages.push({ type: 'internet', value, unit, label: isGifted ? 'Gifted Data' : undefined });
    }

    const voiceMatch = text.match(/([\d,.]+)\s*(?:Min(?:ute)?|ደቂቃ|Daqiiqaa)/i);
    if (voiceMatch) {
      const value = parseFloat(voiceMatch[1].replace(',', ''));
      const isGifted = lower.includes('gift') || lower.includes('received a gift');
      result.packages.push({ type: 'voice', value, unit: 'Min', label: isGifted ? 'Gifted Voice' : undefined });
    }

    const smsMatch = text.match(/(\d[\d,]*)\s*(?:SMS|ኤስኤምኤስ)/i);
    if (smsMatch) {
      const value = parseFloat(smsMatch[1].replace(',', ''));
      const isGifted = lower.includes('gift') || lower.includes('received a gift');
      result.packages.push({ type: 'sms', value, unit: 'SMS', label: isGifted ? 'Gifted SMS' : undefined });
    }

    // Standalone bonus: "awarded an ETB 7.50 bonus" or "Bonus Fund is 7.50 Birr"
    const bonusMatch = text.match(/(?:awarded\s+(?:an\s+)?(?:ETB\s*)?([\d,.]+)\s*bonus|Bonus\s+Fund\s+is\s+([\d,.]+)\s*(?:Birr|ETB|ብር))/i);
    if (bonusMatch) {
      const value = parseFloat((bonusMatch[1] || bonusMatch[2]).replace(',', ''));
      if (value > 0) {
        result.packages.push({ type: 'bonus', value, unit: 'ETB' });
      }
    }
  }

  // 4. Airtime / ETB balance — always attempt for known balance senders, but also
  //    run unconditionally when the text unambiguously contains a balance phrase
  const isBalanceSender = !sender || BALANCE_SENDERS.has(sender) || sender.toUpperCase().includes('TELEBIRR');
  const balMatch = text.match(
    /(?:your\s+(?:telebirr\s+)?(?:account\s+)?(?:new\s+)?balance\s+(?:after\s+\S+\s+)?(?:is|:)|(?:new\s+)?balance[:\s]+|ቀሪ\s*(?:ሒሳ\S*|ብዛ)?|current\s+balance)[\s:]*(?:ETB\s*)?(\d[\d,]*\.?\d*)\s*(?:ETB|ብር)?/i
  );
  if (balMatch) {
    const value = parseFloat(balMatch[1].replace(',', ''));
    result.balance = value;
    if (isBalanceSender) {
      result.packages.push({ type: 'airtime', value, unit: 'ETB' });
    }
  }

  // 5. Financial transactions (trusted senders only)
  const isTrusted = TRUSTED_SENDERS.has(sender) || sender.toUpperCase().includes('TELEBIRR');
  if (isTrusted) {
    const source = resolveSource(sender);

    // Income: credit / received / recharge
    const creditMatch = text.match(/(?:credited|received|credit of)\s*(?:with\s+)?(?:ETB\s*)?([\d,]+\.?\d*)/i);
    const isRepaymentText = /has been repaid|repaid\s+[\d]/i.test(text);
    const loanMatch = !isRepaymentText
      ? text.match(/(?:loan of|taken a loan of|received a loan of)\s*(?:ETB\s*)?([\d,.]+)\s*(?:ETB|ብር)?/i)
      : null;

    if (loanMatch) {
      result.transaction = {
        type: 'income',
        amount: parseFloat(loanMatch[1].replace(',', '')),
        source,
        category: 'Loan',
      };
    } else if (creditMatch && !lower.includes('debited')) {
      result.transaction = {
        type: 'income',
        amount: parseFloat(creditMatch[1].replace(',', '')),
        source,
        category: 'Income',
      };
    }

    // Expense: paid / debit / transferred / repaid / service fee
    // repayMatch only runs when no loan-income was detected (prevents override)
    const repayMatch = !loanMatch
      ? (text.match(/(?:repayment of|repaid|loan of)\s*(?:ETB\s*)?([\d,.]+)\s*(?:ETB|ብር)?(?:.*has been repaid)?/i) ??
         text.match(/([\d,.]+)\s*(?:ETB|ብር)?\s+has been repaid/i))
      : null;
    const debitMatch = text.match(/(?:debited|debit of|deducted)\s*(?:with\s+)?(?:ETB\s*)?(\d[\d,]*\.?\d*)/i);
    // Negative lookbehind ensures 'repaid' is not matched as 'paid'
    const paidMatch = !repayMatch
      ? text.match(/(?<!re)(?:paid|payment|purchase)\s*(?:ETB\s*)?([\d,.]+)\s*(?:ETB|ብር)?/i)
      : null;
    const transferMatch = text.match(/(?:transferred|gifted)\s*(?:ETB\s*)?([\d,.]+)\s*(?:ETB|ብር)?/i);
    const feeMatch = !loanMatch && !repayMatch
      ? text.match(/(?:service fee of|fee\s+of)\s*(?:ETB\s*)?([\d,.]+)\s*(?:ETB|ብር)?/i)
      : null;

    if (repayMatch) {
      result.transaction = {
        type: 'expense',
        amount: parseFloat(repayMatch[1].replace(',', '')),
        source,
        category: 'Repayment',
      };
    } else if (debitMatch) {
      result.transaction = {
        type: 'expense',
        amount: parseFloat(debitMatch[1].replace(',', '')),
        source,
        category: 'Expense',
      };
    } else if (paidMatch && !loanMatch) {
      result.transaction = {
        type: 'expense',
        amount: parseFloat(paidMatch[1].replace(',', '')),
        source,
        category: 'Purchase',
      };
    } else if (transferMatch) {
      result.transaction = {
        type: 'expense',
        amount: parseFloat(transferMatch[1].replace(',', '')),
        source,
        category: 'Gift',
      };
    } else if (feeMatch) {
      result.transaction = {
        type: 'expense',
        amount: parseFloat(feeMatch[1].replace(',', '')),
        source,
        category: 'Fee',
      };
    }
  }

  return result;
}
