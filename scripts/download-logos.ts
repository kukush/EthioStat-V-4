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

// Load .env from project root
const projectRoot = path.resolve(__dirname, '..');
config({ path: path.join(projectRoot, '.env') });

const LOGOS_DIR = path.join(projectRoot, 'public', 'assets', 'logos');

interface LogoEntry {
  key: string;
  url: string;
  filename: string;
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
      });
    }
  }
  return entries;
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
    }
  }

  if (failures > 0) {
    console.error(`\n❌ ${failures} logo(s) failed to download. SVG fallbacks will be used at runtime.`);
    // Non-fatal: don't block the build, SVG fallbacks exist
    // To make it fatal, uncomment: process.exit(1);
  } else {
    console.log('\n✅ All logos downloaded successfully.');
  }
}

main().catch((err) => {
  console.error('Logo download script failed:', err);
  // Non-fatal
});
