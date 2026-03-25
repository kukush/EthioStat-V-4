/**
 * Local SVG avatar data URIs for profile selection.
 * No external dependencies — works offline.
 */

function svgToDataUri(svg: string): string {
  return `data:image/svg+xml,${encodeURIComponent(svg.trim())}`;
}

// Ethiopian Male — warm brown skin, short hair
const ETH_MALE = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  <circle cx="100" cy="100" r="100" fill="#E8D5B7"/>
  <circle cx="100" cy="85" r="40" fill="#8B6914"/>
  <circle cx="100" cy="90" r="35" fill="#C68642"/>
  <ellipse cx="100" cy="160" rx="50" ry="40" fill="#3B82F6"/>
  <circle cx="88" cy="85" r="3" fill="#1a1a1a"/>
  <circle cx="112" cy="85" r="3" fill="#1a1a1a"/>
  <path d="M92 100 Q100 108 108 100" stroke="#1a1a1a" stroke-width="2" fill="none"/>
  <path d="M60 70 Q80 40 140 70" fill="#1a1a1a"/>
</svg>
`);

// Ethiopian Female — warm brown skin, long hair
const ETH_FEMALE = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  <circle cx="100" cy="100" r="100" fill="#F5E6CC"/>
  <ellipse cx="100" cy="75" rx="50" ry="55" fill="#1a1a1a"/>
  <circle cx="100" cy="90" r="35" fill="#C68642"/>
  <ellipse cx="100" cy="160" rx="50" ry="40" fill="#EC4899"/>
  <circle cx="88" cy="85" r="3" fill="#1a1a1a"/>
  <circle cx="112" cy="85" r="3" fill="#1a1a1a"/>
  <path d="M92 100 Q100 106 108 100" stroke="#1a1a1a" stroke-width="2" fill="none"/>
  <path d="M55 80 Q55 30 100 25 Q145 30 145 80" fill="#1a1a1a"/>
  <circle cx="82" cy="75" r="4" fill="#D4A843"/>
  <circle cx="118" cy="75" r="4" fill="#D4A843"/>
</svg>
`);

// Black Male — darker skin, fade haircut
const BLK_MALE = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  <circle cx="100" cy="100" r="100" fill="#D4B896"/>
  <rect x="68" y="45" width="64" height="25" rx="12" fill="#2D1B00"/>
  <circle cx="100" cy="90" r="35" fill="#8B5E3C"/>
  <ellipse cx="100" cy="160" rx="50" ry="40" fill="#10B981"/>
  <circle cx="88" cy="85" r="3" fill="#1a1a1a"/>
  <circle cx="112" cy="85" r="3" fill="#1a1a1a"/>
  <path d="M92 100 Q100 108 108 100" stroke="#1a1a1a" stroke-width="2" fill="none"/>
  <rect x="86" cy="96" y="94" width="28" height="3" rx="1.5" fill="#8B5E3C"/>
</svg>
`);

// Black Female — darker skin, afro
const BLK_FEMALE = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  <circle cx="100" cy="100" r="100" fill="#E8C9A0"/>
  <circle cx="100" cy="70" r="50" fill="#2D1B00"/>
  <circle cx="100" cy="90" r="35" fill="#8B5E3C"/>
  <ellipse cx="100" cy="160" rx="50" ry="40" fill="#A855F7"/>
  <circle cx="88" cy="85" r="3" fill="#1a1a1a"/>
  <circle cx="112" cy="85" r="3" fill="#1a1a1a"/>
  <path d="M92 100 Q100 106 108 100" stroke="#1a1a1a" stroke-width="2" fill="none"/>
  <circle cx="88" cy="72" r="5" fill="#D4A843"/>
</svg>
`);

// Ethiopian Male 2 — beard variant
const ETH_MALE_2 = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  <circle cx="100" cy="100" r="100" fill="#D9C4A0"/>
  <circle cx="100" cy="90" r="35" fill="#C68642"/>
  <ellipse cx="100" cy="160" rx="50" ry="40" fill="#6366F1"/>
  <circle cx="88" cy="85" r="3" fill="#1a1a1a"/>
  <circle cx="112" cy="85" r="3" fill="#1a1a1a"/>
  <path d="M92 100 Q100 108 108 100" stroke="#1a1a1a" stroke-width="2" fill="none"/>
  <path d="M65 70 Q80 45 135 70" fill="#1a1a1a"/>
  <ellipse cx="100" cy="112" rx="18" ry="8" fill="#1a1a1a" opacity="0.5"/>
</svg>
`);

// Ethiopian Female 2 — headscarf variant
const ETH_FEMALE_2 = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  <circle cx="100" cy="100" r="100" fill="#F0E0C8"/>
  <path d="M55 85 Q55 30 100 25 Q145 30 145 85 L130 85 Q130 50 100 45 Q70 50 70 85Z" fill="#FBBF24"/>
  <circle cx="100" cy="90" r="35" fill="#C68642"/>
  <ellipse cx="100" cy="160" rx="50" ry="40" fill="#FBBF24"/>
  <circle cx="88" cy="85" r="3" fill="#1a1a1a"/>
  <circle cx="112" cy="85" r="3" fill="#1a1a1a"/>
  <path d="M92 100 Q100 106 108 100" stroke="#1a1a1a" stroke-width="2" fill="none"/>
</svg>
`);

export interface AvatarOption {
  id: string;
  url: string;
  label: string;
}

export const DEFAULT_AVATARS: AvatarOption[] = [
  { id: 'eth-m', url: ETH_MALE, label: 'Ethiopian Male' },
  { id: 'eth-f', url: ETH_FEMALE, label: 'Ethiopian Female' },
  { id: 'blk-m', url: BLK_MALE, label: 'Black Male' },
  { id: 'blk-f', url: BLK_FEMALE, label: 'Black Female' },
  { id: 'eth-m2', url: ETH_MALE_2, label: 'Ethiopian Male 2' },
  { id: 'eth-f2', url: ETH_FEMALE_2, label: 'Ethiopian Female 2' },
];

/** Default fallback avatar when none is selected */
export const FALLBACK_AVATAR = ETH_MALE;
