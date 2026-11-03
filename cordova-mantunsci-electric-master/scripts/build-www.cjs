/**
 * 打包 Cordova 用的单文件 IIFE：src/ts → www/electric-open-platform.js
 *
 * 为什么需要单独一步：Cordova 的 `js-module` 会直接注入 `www/*.js`，
 * 不经过打包器，也没有模块解析，所以必须把所有源码合成一个自包含文件，
 * 并暴露 `window.ElectricOpenPlatform` / `cordova.plugins.electricOpenPlatform`。
 */
const esbuild = require('esbuild');
const fs = require('fs');
const path = require('path');

// 本文件在 scripts/ 下，仓库根是上一级
const here = __dirname;
const root = path.resolve(here, '..');
const entry = path.join(root, 'src/ts/entry-cordova.ts');
const outfile = path.join(root, 'www/electric-open-platform.js');

async function main() {
  await esbuild.build({
    entryPoints: [entry],
    outfile,
    bundle: true,
    format: 'iife',
    // cordova 全局桥在运行时才有，不参与打包
    external: [],
    globalName: '__EOP',
    target: ['es2017'],
    platform: 'browser',
    banner: {
      js: '/*! cordova-plugin-mantunsci-electric — 智慧式用电开放平台。构建产物，请勿手改。 */',
    },
    legalComments: 'none',
    logLevel: 'info',
  });

  // 追加 cordova 全局挂载（IIFE 里 __EOP 已经是命名空间对象）
  const tail = `

// ===================== Cordova 全局挂载 =====================
(function (root, ns) {
  if (!ns) return;
  var api = {
    configure: ns.configure,
    login: ns.login,
    logout: ns.logout,
    getAuthorizeUrl: ns.getAuthorizeUrl,
    handleRedirect: ns.handleRedirect,
    refreshToken: ns.refreshToken,
    getAccessToken: ns.getAccessToken,
    invoke: ns.invoke,
    describe: ns.describe,
    diagnostics: ns.diagnostics,
    storageBackend: ns.storageBackend,
    dispose: ns.dispose,
    setTransport: ns.setTransportImpl,
    detectHost: ns.detectHost,
    // 业务分组：每个方法都转成 Promise，与 SDK 侧返回值一致
    project: ns.project,
    box: ns.box,
    channel: ns.channel,
    control: ns.control,
    alarm: ns.alarm,
    stats: ns.stats,
    scene: ns.scene,
    push: ns.push,
    thirdparty: ns.thirdparty,
    // 工具与错误类型
    md5: ns.md5,
    signParams: ns.signParams,
    formatTimestamp: ns.formatTimestamp,
    ElectricOpenPlatformError: ns.ElectricOpenPlatformError,
    PlatformCodes: ns.PlatformCodes,
    TransportCodes: ns.TransportCodes,
    AlarmType: ns.AlarmType,
    PushType: ns.PushType,
    EquipmentType: ns.EquipmentType,
    ThirdPartyEquipmentType: ns.ThirdPartyEquipmentType
  };

  if (root.cordova) {
    root.cordova.plugins = root.cordova.plugins || {};
    root.cordova.plugins.electricOpenPlatform = api;
  }
  root.ElectricOpenPlatform = api;

  // Cordova deviceready 之前 cordova 可能尚未注入，补一次
  document.addEventListener && document.addEventListener('deviceready', function () {
    if (root.cordova) {
      root.cordova.plugins = root.cordova.plugins || {};
      root.cordova.plugins.electricOpenPlatform = api;
    }
    root.ElectricOpenPlatform = api;
  }, false);
})(typeof window !== 'undefined' ? window : this, __EOP);
`;

  fs.appendFileSync(outfile, tail, 'utf8');
  const size = fs.statSync(outfile).size;
  console.log(`built ${path.relative(root, outfile)} (${(size / 1024).toFixed(1)} KB)`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
