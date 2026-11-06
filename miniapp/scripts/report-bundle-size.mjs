import { gzipSync } from "node:zlib";
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

const assetsDir = join(process.cwd(), "dist", "assets");
const budgetBytes = 200 * 1024;

let totalGzipBytes = 0;
for (const file of readdirSync(assetsDir)) {
  if (!file.endsWith(".js")) continue;
  const raw = readFileSync(join(assetsDir, file));
  const gzipBytes = gzipSync(raw).length;
  totalGzipBytes += gzipBytes;
  console.log(`${file}: ${(gzipBytes / 1024).toFixed(1)} KB gzip`);
}

const totalKb = (totalGzipBytes / 1024).toFixed(1);
const budgetKb = (budgetBytes / 1024).toFixed(0);
console.log(`\nTotal JS (gzip): ${totalKb} KB (budget: ${budgetKb} KB)`);
if (totalGzipBytes > budgetBytes) {
  console.log("Over budget.");
}
