import { parseEthioSMS } from './smsParser.js';

const runTests = () => {
  console.log('Running SMS Parser Tests...');
  let passed = 0;
  let failed = 0;

  const test = (name: string, text: string, expected: any, senderId?: string) => {
    const result = parseEthioSMS(text, senderId);
    const resultStr = JSON.stringify(result);
    const expectedStr = JSON.stringify(expected);

    // Basic check if result contains expected properties
    const isMatch = Object.entries(expected).every(([key, value]) => {
      if (Array.isArray(value)) {
        return Array.isArray((result as any)[key]) && (result as any)[key].length >= value.length;
      }
      if (typeof value === 'object' && value !== null) {
        const resultVal = (result as any)[key];
        if (!resultVal) return false;
        return Object.entries(value).every(([subKey, subValue]) => resultVal[subKey] === subValue);
      }
      return (result as any)[key] === value;
    });

    if (isMatch) {
      console.log(`✅ PASS: ${name}`);
      passed++;
    } else {
      console.log(`❌ FAIL: ${name}`);
      console.log(`   Expected: ${expectedStr}`);
      console.log(`   Actual: ${resultStr}`);
      failed++;
    }
  };

  test(
    'Balance Check',
    'Your balance is 145.50 ETB. Valid until 2026-10-30.',
    { balance: 145.5 }
  );

  test(
    'Data Package Remaining',
    '450MB data remaining until 2026-03-24.',
    { packages: [{ type: 'internet', value: 450, unit: 'MB' }] }
  );

  test(
    'CBE Credit Transaction',
    'Your account 1000123456789 has been credited with ETB 1,500.00 from John Doe.',
    { transaction: { type: 'income', amount: 1500, source: 'CBE' } },
    'CBE'
  );

  test(
    'Awash Bank Debit',
    'Your account has been debited with ETB 250.00 for Utility Payment.',
    { transaction: { type: 'expense', amount: 250, source: 'Awash' } },
    'Awash'
  );

  test(
    'Telebirr Payment',
    'You have paid 130.00 ETB for 1GB package.',
    { transaction: { type: 'expense', amount: 130, source: 'Telebirr' } },
    'Telebirr'
  );

  test(
    'Airtime Loan Received',
    'You have taken a loan of 10.00 ETB. A service fee of 1.00 ETB will be charged upon recharge.',
    { transaction: { type: 'income', amount: 10, category: 'Loan' } },
    '810'
  );

  test(
    'Telebirr Endekise Loan Received',
    'You have received a loan of 500.00 ETB through Endekise.',
    { transaction: { type: 'income', amount: 500, category: 'Loan' } },
    'Telebirr'
  );

  test(
    'Telebirr Endekise Loan Repayment',
    'You have repaid 510.00 ETB for your Endekise loan.',
    { transaction: { type: 'expense', amount: 510, category: 'Repayment' } },
    'Telebirr'
  );

  test(
    'Airtime Loan Repayment',
    'Your loan of 10.00 ETB has been repaid.',
    { transaction: { type: 'expense', amount: 10, category: 'Repayment' } },
    '810'
  );

  test(
    'Airtime Loan Service Fee',
    'A service fee of 1.00 ETB will be charged upon recharge.',
    { transaction: { type: 'expense', amount: 1, category: 'Fee' } },
    '810'
  );

  test(
    'Gift Received (Data)',
    'You have received a gift of 1GB data from telebirr.',
    { packages: [{ type: 'internet', value: 1, unit: 'GB', label: 'Gifted Data' }] },
    '251994'
  );

  test(
    'Gift Received (Voice)',
    'You have received a gift of 50 Min from telebirr.',
    { packages: [{ type: 'voice', value: 50, unit: 'Min', label: 'Gifted Voice' }] },
    '251994'
  );

  test(
    'Airtime Transfer (Sent)',
    'You have transferred 10.00 ETB to 0911223344.',
    { transaction: { type: 'expense', amount: 10, category: 'Gift' } },
    '806'
  );

  test(
    'Package Gifted (Sent)',
    'You have gifted 1GB data to 0911223344.',
    { transaction: { type: 'expense', category: 'Gift' } },
    '999'
  );

  test(
    'Telebirr Purchase (830)',
    'You have paid 130.00 ETB for 1GB package.',
    { transaction: { type: 'expense', amount: 130, source: 'Telebirr' } },
    '830'
  );

  test(
    'Ethio Gebeta Data Package',
    'You have successfully bought 1GB Ethio Gebeta data package.',
    { packages: [{ type: 'internet', value: 1, unit: 'GB', label: 'Ethio Gebeta Data' }] },
    '999'
  );

  test(
    'SMS Package Remaining',
    'You have 100 SMS remaining until 2026-03-24.',
    { packages: [{ type: 'sms', value: 100, unit: 'SMS' }] },
    '251994'
  );

  test(
    'Gift Received (SMS)',
    'You have received a gift of 20 SMS from telebirr.',
    { packages: [{ type: 'sms', value: 20, unit: 'SMS', label: 'Gifted SMS' }] },
    '251994'
  );

  test(
    'Complex Telebirr Multi-Package Balance (double-space separator)',
    'Dear Customer, your remaining amount  from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 11927.084 MB with expiry date on 2026-04-30 at 02:41:08;  from Monthly Recurring 125 Min and 63Min night package bonus is 63 minute and 0 second with expiry date on 2026-04-30 at 16:02:16;   from Monthly Recurring 125 Min and 63Min night package bonus is 125 minute and 0 second with expiry date on 2026-04-30 at 16:02:16;    from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 12288.000 MB with expiry date on 2026-04-30 at 10:16:09;  from Monthly voice 150 Min from telebirr to be expired after 30 days and 76 Min night package bonus valid for 30 days is 65 minute and 14 second with expiry date on 2026-04-10 at 11:08:07;     from Create Your Own Package Monthly is 149 SMS with expiry date on 2026-04-19 at 00:22:19;  Enjoy 10% additional rewards by downloading telebirr SuperApp https://bit.ly/telebirr_SuperApp.Ethio telecom',
    { packages: [{ type: 'internet' }, { type: 'voice' }, { type: 'sms' }] },
    '127'
  );

  // ── New cases ─────────────────────────────────────────────────────────────

  test(
    'Telebirr Payment + Internet Package (dual: expense + asset)',
    'You have paid 130.00 ETB for 1GB internet package. Your telebirr account balance is 370.00 ETB.',
    { transaction: { type: 'expense', amount: 130, source: 'Telebirr' }, packages: [{ type: 'internet' }] },
    '127'
  );

  test(
    'Amharic Airtime Balance (sender 804)',
    'ቀሪ ሒሳቡ 450.00 ብር',
    { balance: 450, packages: [{ type: 'airtime', value: 450, unit: 'ETB' }] },
    '804'
  );

  test(
    'Amharic Airtime Balance - alternative phrase (ቀሪ ብዛ)',
    'ቀሪ ብዛ 210.50 ብር',
    { balance: 210.5 },
    '804'
  );

  test(
    'CBE Debit Transaction',
    'Your account has been debited with ETB 500.00 for utility payment.',
    { transaction: { type: 'expense', amount: 500, source: 'CBE' } },
    'CBE'
  );

  test(
    'Awash Bank Credit (Income)',
    'Your Awash Bank account has been credited with ETB 10,000.00 from John Doe.',
    { transaction: { type: 'income', amount: 10000, source: 'Awash' } },
    'Awash'
  );

  test(
    'Telebirr Airtime Balance Only (no packages)',
    'Your telebirr account balance is 250.75 ETB. Thank you for using Telebirr.',
    { balance: 250.75, packages: [{ type: 'airtime', value: 250.75, unit: 'ETB' }] },
    '127'
  );

  test(
    'Multi-Segment Voice + SMS Only (no internet)',
    'Dear Customer, your remaining amount  from Monthly Recurring 125 Min is 80 minute and 0 second with expiry date on 2026-04-30 at 10:00:00;  from Create Your Own Package Monthly is 50 SMS with expiry date on 2026-04-30 at 10:00:00;  Ethio telecom',
    { packages: [{ type: 'voice' }, { type: 'sms' }] },
    '251994'
  );

  test(
    'Airtime Transfer Service Fee (Expense)',
    'A service fee of 1.50 ETB will be charged upon recharge.',
    { transaction: { type: 'expense', amount: 1.5, category: 'Fee' } },
    '810'
  );

  test(
    'Telebirr Debit (non-loan, non-package payment)',
    'You have paid 500.00 ETB to Ethiopian Electric Utility.',
    { transaction: { type: 'expense', amount: 500, source: 'Telebirr' } },
    '127'
  );

  test(
    'Bank Income — Dashen Credit',
    'Your Dashen Bank account has been credited with ETB 3,200.00.',
    { transaction: { type: 'income', amount: 3200, source: 'Dashen' } },
    'Dashen'
  );

  test(
    'Voice-Only Multi-Segment (largest total wins: 150 Min replaces 125 Min)',
    'Dear Customer, remaining  from Monthly Recurring 125 Min is 90 minute with expiry date on 2026-04-30 at 10:00:00;  from Monthly voice 150 Min is 65 minute and 0 second with expiry date on 2026-04-15 at 10:00:00;  Ethio telecom',
    { packages: [{ type: 'voice', value: 65 }] },
    '251994'
  );

  // ── Real Telebirr format: ETB BEFORE amount ──────────────────────────────
  test(
    'Telebirr Transfer — ETB before amount (real format)',
    'Dear Tesfaye You have transferred ETB 220.00 to Worku Mengistu (2519****3881) on 31/03/2026 10:59. Your new balance is ETB 1,234.56. Transaction ID: TBR123456',
    { transaction: { type: 'expense', amount: 220 }, balance: 1234.56 },
    '127'
  );

  test(
    'Telebirr Payment — ETB before amount (real format)',
    'Dear Tesfaye You have paid ETB 550.00 for Monthly Internet Package 12GB from telebirr on 31/03/2026. Your telebirr account balance is ETB 684.25.',
    { transaction: { type: 'expense', amount: 550 }, balance: 684.25 },
    '127'
  );

  test(
    'Telebirr Airtime Received — ETB before amount (real format)',
    'Dear Customer You have received ETB 50.00 airtime from 251970824468 on 31/03/2026 15:50.',
    { transaction: { type: 'income', amount: 50 } },
    '127'
  );

  console.log(`\nTests Completed: ${passed} Passed, ${failed} Failed`);
  if (failed > 0) process.exit(1);
};

runTests();
