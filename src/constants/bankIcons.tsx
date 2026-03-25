import React from 'react';
import { findBank } from './banks';

/**
 * Bank logos strategy (3 layers):
 * 1. Local PNG (downloaded at build time) for CBE, Telebirr, Awash, Dashen
 * 2. Inline SVG icons for major banks
 * 3. Colored abbreviation circle fallback for all others
 */

// Map of banks that have downloaded PNG logos in public/assets/logos/
const PNG_LOGO_BANKS: Record<string, string> = {
  'CBE': '/assets/logos/cbe.png',
  'Telebirr': '/assets/logos/telebirr.png',
  'Awash': '/assets/logos/awash.png',
  'Dashen': '/assets/logos/dashen.png',
};

// --- Inline SVG Icon Components ---

const CBEIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Golden spiral design — trust & national pride */}
    <circle cx="20" cy="20" r="18" fill="#C8982D" />
    <path
      d="M20 6C12.27 6 6 12.27 6 20s6.27 14 14 14"
      stroke="#FFF" strokeWidth="2.5" fill="none" strokeLinecap="round"
    />
    <path
      d="M20 10C14.48 10 10 14.48 10 20s4.48 10 10 10"
      stroke="#FFF" strokeWidth="2" fill="none" strokeLinecap="round" opacity="0.8"
    />
    <path
      d="M20 14C16.69 14 14 16.69 14 20s2.69 6 6 6"
      stroke="#FFF" strokeWidth="1.5" fill="none" strokeLinecap="round" opacity="0.6"
    />
    <circle cx="20" cy="20" r="2.5" fill="#FFF" />
  </svg>
);

const AwashIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Blue/orange wave */}
    <circle cx="20" cy="20" r="18" fill="#0066B3" />
    <path
      d="M6 22c4-4 8 0 12-4s8 0 12-4"
      stroke="#F27D26" strokeWidth="3" fill="none" strokeLinecap="round"
    />
    <path
      d="M6 26c4-4 8 0 12-4s8 0 12-4"
      stroke="#FFF" strokeWidth="2" fill="none" strokeLinecap="round" opacity="0.5"
    />
  </svg>
);

const BoAIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Yellow 5-petal flower (Adey Abeba) */}
    <circle cx="20" cy="20" r="18" fill="#D4A843" />
    {[0, 72, 144, 216, 288].map((angle) => (
      <ellipse
        key={angle}
        cx="20" cy="11"
        rx="4" ry="7"
        fill="#FFF"
        opacity="0.85"
        transform={`rotate(${angle} 20 20)`}
      />
    ))}
    <circle cx="20" cy="20" r="4" fill="#FFF" />
  </svg>
);

const DashenIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Bold "D" in green square */}
    <rect x="2" y="2" width="36" height="36" rx="8" fill="#006838" />
    <text x="20" y="28" textAnchor="middle" fill="#FFF" fontWeight="900" fontSize="24" fontFamily="Arial, sans-serif">D</text>
  </svg>
);

const TelebirrIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Blue "T" logo */}
    <circle cx="20" cy="20" r="18" fill="#005CB9" />
    <text x="20" y="28" textAnchor="middle" fill="#FFF" fontWeight="900" fontSize="22" fontFamily="Arial, sans-serif">T</text>
    <rect x="10" y="10" width="20" height="3" rx="1.5" fill="#FFF" opacity="0.3" />
  </svg>
);

const HibretIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Minimal handshake */}
    <circle cx="20" cy="20" r="18" fill="#1B3A6B" />
    <path
      d="M10 22l5-4 3 2 4-3 4 3 3-2 5 4"
      stroke="#FFF" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"
    />
    <path d="M15 18l5-4 5 4" stroke="#FFF" strokeWidth="1.5" fill="none" strokeLinecap="round" opacity="0.5" />
  </svg>
);

const CoopbankIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Two linked circles */}
    <rect x="2" y="2" width="36" height="36" rx="18" fill="#0054A6" />
    <circle cx="15" cy="20" r="7" stroke="#FFF" strokeWidth="2" fill="none" />
    <circle cx="25" cy="20" r="7" stroke="#FFF" strokeWidth="2" fill="none" />
  </svg>
);

const WegagenIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Sun rays */}
    <circle cx="20" cy="20" r="18" fill="#E8451E" />
    <circle cx="20" cy="20" r="6" fill="#FFF" />
    {[0, 45, 90, 135, 180, 225, 270, 315].map((angle) => (
      <line
        key={angle}
        x1="20" y1="10" x2="20" y2="6"
        stroke="#FFF" strokeWidth="2" strokeLinecap="round"
        transform={`rotate(${angle} 20 20)`}
      />
    ))}
  </svg>
);

const BunnaIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Coffee bean oval */}
    <circle cx="20" cy="20" r="18" fill="#6B3A2A" />
    <ellipse cx="20" cy="20" rx="7" ry="10" fill="#FFF" opacity="0.85" />
    <line x1="20" y1="10" x2="20" y2="30" stroke="#6B3A2A" strokeWidth="1.5" />
  </svg>
);

const OromiaIcon: React.FC<{ size: number }> = ({ size }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Horse head outline */}
    <circle cx="20" cy="20" r="18" fill="#8B0000" />
    <path
      d="M14 30l2-8 3-4 1-6 4-2 4 2 1 6-2 5-1 4 2 3"
      stroke="#FFF" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"
    />
  </svg>
);

// Map abbreviation → SVG component
const SVG_ICON_MAP: Record<string, React.FC<{ size: number }>> = {
  'CBE': CBEIcon,
  'Awash': AwashIcon,
  'BoA': BoAIcon,
  'Dashen': DashenIcon,
  'Telebirr': TelebirrIcon,
  'Hibret': HibretIcon,
  'Coopbank': CoopbankIcon,
  'Wegagen': WegagenIcon,
  'Bunna': BunnaIcon,
  'Oromia': OromiaIcon,
};

/**
 * Colored abbreviation circle fallback
 */
const AbbreviationIcon: React.FC<{ source: string; size: number; color: string }> = ({ source, size, color }) => (
  <div
    style={{
      width: size,
      height: size,
      borderRadius: '50%',
      backgroundColor: color,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
    }}
  >
    <span style={{ color: '#FFF', fontWeight: 900, fontSize: size * 0.35, letterSpacing: '0.05em' }}>
      {source.substring(0, 2).toUpperCase()}
    </span>
  </div>
);

/**
 * PNG logo with SVG fallback on error
 */
const PngWithFallback: React.FC<{ source: string; pngPath: string; size: number }> = ({ source, pngPath, size }) => {
  const [failed, setFailed] = React.useState(false);
  const SvgFallback = SVG_ICON_MAP[source];

  if (failed && SvgFallback) {
    return <SvgFallback size={size} />;
  }

  if (failed) {
    const bank = findBank(source);
    return <AbbreviationIcon source={source} size={size} color={bank?.color || '#6B7280'} />;
  }

  return (
    <img
      src={pngPath}
      alt={source}
      style={{ width: size, height: size, objectFit: 'contain' }}
      onError={() => setFailed(true)}
    />
  );
};

/**
 * Unified bank icon resolver — tries 3 layers:
 * 1. Local PNG (for CBE, Telebirr, Awash, Dashen)
 * 2. Inline SVG component
 * 3. Colored abbreviation circle fallback
 */
export function getBankIcon(source: string, size: number = 32): React.ReactNode {
  // Layer 1: Local PNG with auto-fallback
  const pngPath = PNG_LOGO_BANKS[source];
  if (pngPath) {
    return <PngWithFallback source={source} pngPath={pngPath} size={size} />;
  }

  // Layer 2: Inline SVG
  const SvgIcon = SVG_ICON_MAP[source];
  if (SvgIcon) {
    return <SvgIcon size={size} />;
  }

  // Layer 3: Colored abbreviation fallback
  const bank = findBank(source);
  return <AbbreviationIcon source={source} size={size} color={bank?.color || '#6B7280'} />;
}
