#!/usr/bin/env tsx
/**
 * Build-time script: Downloads bank logo images from URLs defined in .env (VITE_LOGO_*)
 * to public/assets/logos/ so they can be served as local static assets.
 *
 * Usage: tsx scripts/download-logos.ts
 * Integrated via package.json "prebuild" script.
 */

import * as fs from 'fs';
import * as path from 'path';
import * as https from 'https';
import * as http from 'http';
import { config } from 'dotenv';

// Load .env from project root (ES module compatible)
const projectRoot = path.resolve(new URL('.', import.meta.url).pathname, '..');
config({ path: path.join(projectRoot, '.env') });

const LOGOS_DIR = path.join(projectRoot, 'public', 'assets', 'logos');

interface LogoEntry {
  key: string;
  url: string;
  filename: string;
  svgContent?: string; // SVG fallback content
}

function getLogoEntries(): LogoEntry[] {
  const entries: LogoEntry[] = [];
  for (const [key, value] of Object.entries(process.env)) {
    if (key.startsWith('VITE_LOGO_') && value) {
      const bankKey = key.replace('VITE_LOGO_', '').toLowerCase();
      entries.push({
        key: bankKey,
        url: value,
        filename: `${bankKey}.png`,
        svgContent: getSvgFallback(bankKey),
      });
    }
  }
  return entries;
}

function getSvgFallback(bankKey: string): string {
  // Generate minimal SVG fallback with bank abbreviation
  const colors: Record<string, string> = {
    cbe: '#C8982D',
    telebirr: '#005CB9', 
    awash: '#F27D26',
    dashen: '#006838'
  };
  const color = colors[bankKey] || '#6B7280';
  const abbr = bankKey.toUpperCase();
  
  return `<svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="20" cy="20" r="18" fill="${color}" />
    <text x="20" y="20" text-anchor="middle" dominant-baseline="middle" fill="white" font-family="Arial, sans-serif" font-size="10" font-weight="bold">${abbr}</text>
  </svg>`;
}

function writeSvgFallback(dest: string, svgContent: string): void {
  const svgPath = dest.replace('.png', '.svg');
  fs.writeFileSync(svgPath, svgContent);
  console.log(`  📝 ${path.basename(dest, '.png')} — SVG fallback created`);
}

function downloadFile(url: string, dest: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const protocol = url.startsWith('https') ? https : http;

    const request = protocol.get(url, { headers: { 'User-Agent': 'EthioStat-Build/1.0' } }, (response) => {
      // Follow redirects
      if (response.statusCode && response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        downloadFile(response.headers.location, dest).then(resolve).catch(reject);
        return;
      }

      if (response.statusCode !== 200) {
        reject(new Error(`HTTP ${response.statusCode} for ${url}`));
        return;
      }

      const contentType = response.headers['content-type'] || '';
      if (!contentType.includes('image') && !contentType.includes('octet-stream')) {
        console.warn(`  ⚠️  Unexpected content-type "${contentType}" for ${url} — downloading anyway`);
      }

      const file = fs.createWriteStream(dest);
      response.pipe(file);
      file.on('finish', () => {
        file.close();
        // Validate file is not empty
        const stats = fs.statSync(dest);
        if (stats.size < 100) {
          fs.unlinkSync(dest);
          reject(new Error(`Downloaded file too small (${stats.size} bytes) for ${url}`));
          return;
        }
        resolve();
      });
      file.on('error', (err) => {
        fs.unlinkSync(dest);
        reject(err);
      });
    });

    request.on('error', reject);
    request.setTimeout(15000, () => {
      request.destroy();
      reject(new Error(`Timeout downloading ${url}`));
    });
  });
}

async function main() {
  const entries = getLogoEntries();

  if (entries.length === 0) {
    console.log('ℹ️  No VITE_LOGO_* variables found in .env — skipping logo download.');
    return;
  }

  // Ensure output directory exists
  fs.mkdirSync(LOGOS_DIR, { recursive: true });

  console.log(`📦 Downloading ${entries.length} bank logos to ${LOGOS_DIR}...`);

  let failures = 0;

  for (const entry of entries) {
    const dest = path.join(LOGOS_DIR, entry.filename);

    // Skip if already downloaded and recent (within 24h)
    if (fs.existsSync(dest)) {
      const stats = fs.statSync(dest);
      const ageMs = Date.now() - stats.mtimeMs;
      if (ageMs < 24 * 60 * 60 * 1000 && stats.size > 100) {
        console.log(`  ✅ ${entry.key} — cached (${(stats.size / 1024).toFixed(1)}KB)`);
        continue;
      }
    }

    try {
      await downloadFile(entry.url, dest);
      const stats = fs.statSync(dest);
      console.log(`  ✅ ${entry.key} — downloaded (${(stats.size / 1024).toFixed(1)}KB)`);
    } catch (err: any) {
      console.error(`  ❌ ${entry.key} — FAILED: ${err.message}`);
      failures++;
      
      // Create SVG fallback when PNG fails
      if (entry.svgContent) {
        writeSvgFallback(dest, entry.svgContent);
      }
    }
  }

  if (failures > 0) {
    console.log(`\n⚠️  ${failures} PNG logo(s) failed to download. SVG fallbacks created as backup.`);
  } else {
    console.log('\n✅ All PNG logos downloaded successfully.');
  }
  
  // Check if we have any SVG fallbacks and update bankIcons.tsx mapping
  const svgFiles = fs.readdirSync(LOGOS_DIR).filter(f => f.endsWith('.svg'));
  if (svgFiles.length > 0) {
    console.log(`\n📝 Created ${svgFiles.length} SVG fallback files for failed downloads.`);
  }
}

main().catch((err) => {
  console.error('Logo download script failed:', err);
  // Non-fatal
});
