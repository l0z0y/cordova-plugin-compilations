#!/usr/bin/env node
/**
 * 版本一致性检查。
 *
 * 本仓库根目录即插件本体，同一份版本号出现在两处，必须保持一致，
 * 否则会出现「npm 上是新版本、Cordova 安装记录还是旧版本」这类难查的问题：
 *   1. package.json  → npm 包版本
 *   2. plugin.xml    → Cordova 安装时记录的版本（cordova_plugins.js metadata）
 *
 * 用法：
 *   node scripts/check-versions.mjs          # 只检查两者是否一致
 *   node scripts/check-versions.mjs 1.0.0    # 额外要求等于指定版本（CI 里传 tag）
 */
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const read = (p) => readFileSync(resolve(root, p), 'utf8');

const pkg = JSON.parse(read('package.json'));
const pluginXml = read('plugin.xml');

// 注意：plugin.xml 第一行的 <?xml version="1.0"?> 也会匹配 version=，
// 所以必须锚定在 <plugin ... id="..." version="..."> 上。
const xmlVersion = pluginXml.match(/<plugin[\s\S]*?\bversion="([^"]+)"/)?.[1];
const xmlId = pluginXml.match(/<plugin[\s\S]*?\bid="([^"]+)"/)?.[1];

const expected = process.argv[2]?.replace(/^v/, '');

const rows = [
  ['package.json', pkg.name, pkg.version],
  ['plugin.xml', xmlId, xmlVersion],
];

let failed = false;
const versions = new Set(rows.map((r) => r[2]));

console.log('版本检查：');
for (const [file, name, version] of rows) {
  console.log(`  ${file.padEnd(16)} ${String(name).padEnd(44)} ${version ?? '(缺失)'}`);
}

if (!xmlVersion) {
  console.error('✗ plugin.xml 里找不到 <plugin version="…">');
  failed = true;
}
if (versions.size !== 1 || !xmlVersion) {
  console.error(`✗ 版本不一致：${[...versions].join(' / ')}`);
  failed = true;
}
// plugin.xml 的 id 必须与 package.json 的 cordova.id 一致（Cordova 按它找原生类）
if (pkg.cordova?.id !== xmlId) {
  console.error(`✗ 插件 id 不一致：package.json=${pkg.cordova?.id} / plugin.xml=${xmlId}`);
  failed = true;
}
if (expected && (versions.size !== 1 || !versions.has(expected))) {
  console.error(`✗ 与期望版本 ${expected}（来自 tag）不一致`);
  failed = true;
}

if (failed) process.exit(1);
console.log(`✓ 两处版本一致：${[...versions][0]}`);
