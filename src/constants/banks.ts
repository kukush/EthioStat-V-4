export interface BankInfo {
  name: string;
  abbreviation: string;
  ussd: string;
}

export const ETHIOPIAN_BANKS: BankInfo[] = [
  { name: 'Commercial Bank of Ethiopia', abbreviation: 'CBE', ussd: '*847#' },
  { name: 'Awash Bank', abbreviation: 'Awash', ussd: '*901#' },
  { name: 'Bank of Abyssinia', abbreviation: 'BoA', ussd: '*815#' },
  { name: 'Dashen Bank', abbreviation: 'Dashen', ussd: '*721#' },
  { name: 'Cooperative Bank of Oromia', abbreviation: 'Coopbank', ussd: '*896#' },
  { name: 'Hibret Bank', abbreviation: 'Hibret', ussd: '*844#' },
  { name: 'Wegagen Bank', abbreviation: 'Wegagen', ussd: '*889#' },
  { name: 'Abay Bank', abbreviation: 'Abay', ussd: '*812#' },
  { name: 'Nib International Bank', abbreviation: 'NIB', ussd: '*865#' },
  { name: 'Bunna Bank', abbreviation: 'Bunna', ussd: '*252#' },
  { name: 'Zemen Bank', abbreviation: 'Zemen', ussd: '*710#' },
  { name: 'Berhan Bank', abbreviation: 'Berhan', ussd: '*811#' },
  { name: 'Enat Bank', abbreviation: 'Enat', ussd: '*846#' },
  { name: 'Tsehay Bank', abbreviation: 'Tsehay', ussd: '*921#' },
  { name: 'Siinqee Bank', abbreviation: 'Siinqee', ussd: '*767#' },
  { name: 'Amhara Bank', abbreviation: 'Amhara', ussd: '*946#' },
  { name: 'Lion International Bank', abbreviation: 'Lion', ussd: '*801#' },
  { name: 'Oromia Bank', abbreviation: 'Oromia', ussd: '*804#' },
  { name: 'Global Bank Ethiopia', abbreviation: 'Global', ussd: '*842#' },
  { name: 'Gadaa Bank', abbreviation: 'Gadaa', ussd: '*898#' },
  { name: 'Hijra Bank', abbreviation: 'Hijra', ussd: '*881#' },
  { name: 'Zad Bank', abbreviation: 'Zad', ussd: '*899#' },
  { name: 'Ahadu Bank', abbreviation: 'Ahadu', ussd: '*895#' },
  { name: 'Shabelle Bank', abbreviation: 'Shabelle', ussd: '*808#' },
  { name: 'Amhara Credit and Saving', abbreviation: 'ACSI', ussd: '*810#' },
  { name: 'Telebirr', abbreviation: 'Telebirr', ussd: '*127#' },
];
