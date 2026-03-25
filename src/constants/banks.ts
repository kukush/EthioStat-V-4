export interface BankInfo {
  name: string;
  abbreviation: string;
  ussd: string;
  color: string;
}

export const ETHIOPIAN_BANKS: BankInfo[] = [
  { name: 'Commercial Bank of Ethiopia', abbreviation: 'CBE', ussd: '*847#', color: '#C8982D' },
  { name: 'Awash Bank', abbreviation: 'Awash', ussd: '*901#', color: '#F27D26' },
  { name: 'Bank of Abyssinia', abbreviation: 'BoA', ussd: '*815#', color: '#D4A843' },
  { name: 'Dashen Bank', abbreviation: 'Dashen', ussd: '*721#', color: '#006838' },
  { name: 'Cooperative Bank of Oromia', abbreviation: 'Coopbank', ussd: '*896#', color: '#0054A6' },
  { name: 'Hibret Bank', abbreviation: 'Hibret', ussd: '*844#', color: '#1B3A6B' },
  { name: 'Wegagen Bank', abbreviation: 'Wegagen', ussd: '*889#', color: '#E8451E' },
  { name: 'Abay Bank', abbreviation: 'Abay', ussd: '*812#', color: '#0072BC' },
  { name: 'Nib International Bank', abbreviation: 'NIB', ussd: '*865#', color: '#8B1A2B' },
  { name: 'Bunna Bank', abbreviation: 'Bunna', ussd: '*252#', color: '#6B3A2A' },
  { name: 'Zemen Bank', abbreviation: 'Zemen', ussd: '*710#', color: '#2E3092' },
  { name: 'Berhan Bank', abbreviation: 'Berhan', ussd: '*811#', color: '#009B4D' },
  { name: 'Enat Bank', abbreviation: 'Enat', ussd: '*846#', color: '#D4145A' },
  { name: 'Tsehay Bank', abbreviation: 'Tsehay', ussd: '*921#', color: '#FF8C00' },
  { name: 'Siinqee Bank', abbreviation: 'Siinqee', ussd: '*767#', color: '#4CAF50' },
  { name: 'Amhara Bank', abbreviation: 'Amhara', ussd: '*946#', color: '#1565C0' },
  { name: 'Lion International Bank', abbreviation: 'Lion', ussd: '*801#', color: '#B8860B' },
  { name: 'Oromia Bank', abbreviation: 'Oromia', ussd: '*804#', color: '#8B0000' },
  { name: 'Global Bank Ethiopia', abbreviation: 'Global', ussd: '*842#', color: '#003366' },
  { name: 'Gadaa Bank', abbreviation: 'Gadaa', ussd: '*898#', color: '#228B22' },
  { name: 'Hijra Bank', abbreviation: 'Hijra', ussd: '*881#', color: '#2E7D32' },
  { name: 'Zad Bank', abbreviation: 'Zad', ussd: '*899#', color: '#5C6BC0' },
  { name: 'Ahadu Bank', abbreviation: 'Ahadu', ussd: '*895#', color: '#7B1FA2' },
  { name: 'Shabelle Bank', abbreviation: 'Shabelle', ussd: '*808#', color: '#00838F' },
  { name: 'Amhara Credit and Saving', abbreviation: 'ACSI', ussd: '*810#', color: '#33691E' },
  { name: 'Telebirr', abbreviation: 'Telebirr', ussd: '*127#', color: '#005CB9' },
];

/** Lookup a bank by abbreviation (case-insensitive) */
export function findBank(abbreviation: string): BankInfo | undefined {
  return ETHIOPIAN_BANKS.find(
    b => b.abbreviation.toLowerCase() === abbreviation.toLowerCase()
  );
}
