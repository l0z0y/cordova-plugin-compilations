/**
 * 宿主环境探测与传输自动选择（仅 Cordova 形态）。
 *
 * 选择顺序：
 *   1. 显式配置的 `transport`（config.transport）
 *   2. 调用方在 configure() 之前用 setTransport 注入的实现
 *   3. 有 `cordova` 全局（或 `cordova.exec`）→ cordova-exec（原生 HttpURLConnection / URLSession）
 *   4. Web / Node → fetch（浏览器调试、单测、Node 侧脚本用）
 *
 * 为什么用「有没有 cordova 全局」而不是只看 `cordova.exec`：
 * `cordova.js` 会先挂出 `cordova` 对象，`exec` 要等 deviceready 之后才补上；
 * 只看 exec 会把「已加载 cordova、但还在 deviceready 之前」的 App 误判成 web，
 * 进而选 fetch —— 而 WebView 里 fetch 会被同源策略拦死（平台不返回 CORS 头）。
 */
import type { Transport } from './transport.js';
import { createFetchTransport } from './transport-fetch.js';
import { createCordovaTransport, hasCordovaGlobal, resolveCordovaExec } from './transport-cordova.js';

export type Host = 'cordova' | 'web' | 'node';

/** 判断当前宿主，不抛错。 */
export function detectHost(): Host {
  if (hasCordovaGlobal() || resolveCordovaExec()) return 'cordova';
  if (typeof (globalThis as { window?: unknown }).window !== 'undefined') return 'web';
  return 'node';
}

/**
 * 按宿主挑选传输实现。
 *
 * @param explicit 配置里显式指定的实现（最高优先级）
 * @param preset   调用方在 configure() 之前用 setTransport 注入的实现；
 *                 传了就直接沿用，避免被宿主探测覆盖
 */
export async function resolveTransport(explicit?: Transport, preset?: Transport): Promise<Transport> {
  if (explicit) return explicit;
  if (preset) return preset;

  return detectHost() === 'cordova' ? createCordovaTransport() : createFetchTransport();
}
