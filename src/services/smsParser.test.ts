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

  console.log(`\nTests Completed: ${passed} Passed, ${failed} Failed`);
  if (failed > 0) process.exit(1);
};

runTests();
