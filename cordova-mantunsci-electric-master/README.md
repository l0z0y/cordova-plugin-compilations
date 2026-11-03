# cordova-plugin-mantunsci-electric

面向 **智慧式用电数据开放平台**（Mtsci Open Platform，`open.snd02.com`）的 **Cordova 插件**。

- **OAuth2 全流程**：headless 登录（`authverify2.as`）与重定向登录两种授权方式，`accessToken` / `refreshToken` 自动维护与提前续期，并发请求只刷新一次。
- **签名自动完成**：按平台规范对参数名排序、拼接参数值、追加 `appSecret` 做 MD5，业务代码无需关心。
- **绕开 CORS**：网络走原生 `HttpURLConnection`（Android）/ `URLSession`（iOS），请求不经过 WebView 网络栈，因此平台不返回 `Access-Control-Allow-Origin` 也不影响。
- **类型完整**：全部枚举、字段与 `.d.ts` 声明（`types/electric-open-platform.d.ts`）。
- **错误可编排**：统一 `ElectricOpenPlatformError`，携带平台返回码，可区分「设备离线」「token 过期」「接口未开通」。
- **支持 Android / iOS**：两端原生实现动作一致（`request` / `storageGet` / `storageSet` / `storageRemove`）。

> **只支持 Cordova**。本项目不提供 Capacitor / ESM 形态；如果你在用 Capacitor，需自行适配。

---

## 安装

```bash
cordova plugin add cordova-plugin-mantunsci-electric
```

指定版本：

```bash
cordova plugin add cordova-plugin-mantunsci-electric@1.0.0
```

从本地目录安装（开发调试用）：

```bash
cordova plugin add /path/to/cordova-plugin-mantunsci-electric
```

安装后 `cordova prepare` 会：

- 把 `www/electric-open-platform.js` 注入到 App；
- 编译 `src/android/*.java` 到你的 Android 工程；
- 添加 `INTERNET` / `ACCESS_NETWORK_STATE` 权限；
- 在 `res/xml/config.xml` 注册 `<feature name="ElectricOpenPlatform">`。

## 引入方式

插件通过 `plugin.xml` 的 `clobbers` 挂两个全局对象，**二选一即可**（是同一个对象）：

```js
window.ElectricOpenPlatform
cordova.plugins.electricOpenPlatform
```

插件没有 ESM 导出，**不需要也不能 `import`**：

```js
// ✅ 正确
await ElectricOpenPlatform.configure({ /* ... */ });

// ❌ 不存在这个包
// import { ElectricOpenPlatform } from 'cordova-plugin-mantunsci-electric';
```

### Angular / Vue / TypeScript 项目

插件安装后会在工程根目录生成 `plugins/cordova-plugin-mantunsci-electric/types/electric-open-platform.d.ts`，把它加进 `tsconfig.app.json`（或 `tsconfig.json`）的 `include` 即可获得类型：

```jsonc
{
  "include": [
    "src/**/*.d.ts",
    "plugins/cordova-plugin-mantunsci-electric/types/**/*.d.ts"
  ]
}
```

之后直接用全局变量（声明文件里已有 `declare var ElectricOpenPlatform`，无需再 `declare`）：

```ts
const boxes = await window.ElectricOpenPlatform.box.listOnline();
```

> ⚠️ 不要自己写 `declare const ElectricOpenPlatform: ElectricOpenPlatform.Api;` —— 类型名与变量名同名会把命名空间遮蔽，Angular 的 `ng build` 会报 `Cannot find namespace 'ElectricOpenPlatform'`。

### 等待 deviceready

插件 JS 在 `deviceready` 之后才可用：

```js
document.addEventListener('deviceready', async () => {
  await ElectricOpenPlatform.configure({ /* ... */ });
  await ElectricOpenPlatform.login('账号', '密码');
  console.log(await ElectricOpenPlatform.box.list());
}, false);
```

> **Cordova 工程必须在 `index.html` 里引入 `cordova.js`**：
> ```html
> <body>
>   <app-root></app-root>
>   <script src="cordova.js"></script>
> </body>
> ```
> 少了这一行，`window.ElectricOpenPlatform` 不存在——`cordova.js` 才会加载 `cordova_plugins.js` 并执行各插件。

## 快速开始

```ts
await ElectricOpenPlatform.configure({
  appKey: 'O000093290',
  appSecret: '…',                       // 不要写进源码仓库，见「安全」
  redirectUri: 'http://yourserverurl/receive',
  projectCode: 'P00000104893',
  storage: 'cordova',                    // 原生 SharedPreferences / UserDefaults
});

// 登录：换取 accessToken（有效期 3600s，之后自动续期）
await ElectricOpenPlatform.login('yfdq01', '******');

// 查询
const boxes = await ElectricOpenPlatform.box.listOnline();
const live = await ElectricOpenPlatform.channel.realtimeChannels({ mac: boxes.map((b) => b.mac) });

// 控制（会真实作用于现场设备）
await ElectricOpenPlatform.control.switchCircuit({
  mac: boxes[0].mac,
  action: 'close',                       // 'open' 合闸 | 'close' 分闸
  addr: 1,
  skipWhenAbnormalLock: true,
});
```

## 配置项

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `appKey` | ✅ | 平台分配的 appKey，作为 `client_id` |
| `appSecret` | ✅ | 平台分配的 appSecret，只参与 MD5，不直接上送 |
| `redirectUri` | ✅ | 回调地址，**会参与签名**，必须与平台登记一致 |
| `baseUrl` | | 默认 `https://open.snd02.com` |
| `projectCode` | | 默认项目号，之后调用可不再重复传 |
| `username` / `password` | | 配置后 token 失效会自动重登 |
| `storage` | | `auto`（默认）/ `cordova` / `localStorage` / `memory` |
| `timeoutMs` | | 请求超时，默认 20000 |
| `tokenRefreshSkewMs` | | 提前续期毫秒，默认 60000 |
| `transport` | | 自定义传输实现，一般不需要 |
| `logger` | | 请求/响应日志回调（已脱敏） |

## 认证

### 方式一：headless 登录（推荐给 App）

```ts
const result = await ElectricOpenPlatform.login(username, password);
// → { success, code, expiresIn, expiresAt }
```

内部依次调用 `POST /oauth/authverify2.as`（取 code）与 `POST /oauth/token.as`（换 token）。

### 方式二：重定向登录

```ts
// 打开该 URL 让用户登录，平台会带回 ?code=
const url = ElectricOpenPlatform.getAuthorizeUrl({ state: 'csrf-1' });

// 你的 redirectUri 收到回调后（或用 deep link 接住）
await ElectricOpenPlatform.handleRedirect('http://yourserverurl/receive?code=xxxx');
```

### token 生命周期

| 事项 | 行为 |
| --- | --- |
| 有效期 | 平台返回 `expiresIn`（当前 3600s） |
| 提前续期 | 默认提前 60s 用 `refreshToken` 刷新（`tokenRefreshSkewMs` 可调） |
| 接口返回 `code:"2"` | 自动刷新一次并重放原请求，对调用方透明 |
| refresh 也失效 | 若配置了账号密码则自动重登；否则抛 `E_NOT_CONFIGURED` |
| 并发 | 同一时刻只有一个刷新请求在飞，其余等待复用 |
| 持久化 | `cordova`（原生）→ `localStorage` → 内存 |

## API 一览

所有分组挂在 `ElectricOpenPlatform` 上。

### `project`
| 方法 | 平台接口 |
| --- | --- |
| `getInfo()` | `GET_PROJECT_INFO` |
| `modifyInfo(fields)` | `MODIFY_PROJECT_INFO` |
| `dayStats({year,month,day?})` | `GET_PROJECT_DAY_STATS` |

### `box`
| 方法 | 平台接口 |
| --- | --- |
| `list({equipmentType?,build?,unit?,room?})` | `GET_BOXES` |
| `listOnline()` | `GET_BOXES` + 本地过滤 `online === 1` |
| `get(mac)` | `GET_BOX` |
| `positionTree()` | `GET_BOX_POSITION_TREE` |
| `properties({mac,nodeType?,addr?})` | `GET_BOX_PROPERTIES`（**必须**带 `nodeType=COMM`） |
| `modifyProperties({mac,params})` | `MODIFY_BOX_PROPERTIES` |
| `modifyInfo({mac,...fields})` | `MODIFY_BOX_INFO` |
| `setRealtimeRate({mac,enable,rate})` | `SET_BOX_REALTIME_RATE`（`rate ≥ 5`） |
| `onlineHistory({mac,startTime,endTime})` | `GET_BOX_ONLINE_HISTORY` |

### `channel`
| 方法 | 平台接口 |
| --- | --- |
| `realtime({mac})` | `GET_BOX_CHANNELS_REALTIME`（≤20 台，自动归一化单/多设备返回结构） |
| `realtimeChannels({mac})` | 同上，拍平为带 `mac` 的线路数组 |
| `list({mac\|param})` | `GET_BOX_CHANNELS` |
| `channelsOc({mac})` | `GET_BOX_CHANNELS_OC` |
| `history({mac,date,addr?})` | `GET_BOX_CHANNELS_REALTIME_HISTORY`（**仅按天**，`date` 格式 `yyyy-MM-dd`） |

### `control`
| 方法 | 命令 |
| --- | --- |
| `switchCircuit({mac,action,addr})` | `OCSWITCH` |
| `batchSwitch({items,action})` | `BATCH_BOX_CONTROL` |
| `updateCircuit({mac,addr,title,maxPowerW})` | `SWITCHSET` |
| `remoteLock({mac,addr,locked})` | `REMOTE_SWITCH_LOCK` |
| `unlockAbnormal({mac,addr})` | `ABNORMAL_SWITCH_UNLOCK` |
| `setVisibility` / `setControl` | `SETVISIBILITY` / `SETCONTROL` |
| `setAutoLeakage({enable,day,hour,minute})` | `SETAUTOLEAK` |
| `getTimers({mac})` / `getLeakageCheck({mac})` | `GET_CFG_TIMER` / `GET_CFG_LKICHK` |
| `control({mac,cmd,options})` | `PUT_BOX_CONTROL`（任意命令，类型安全） |

### `alarm`
| 方法 | 说明 |
| --- | --- |
| `list({start,end,mac?,page?})` | `GET_BOX_ALARM`，返回 `{total,page,totalPage,datas}` |
| `listAll(opts)` | 自动翻页 |
| `byType({...,typeNumbers})` | 本地按告警类型过滤（配合 `AlarmType` 枚举） |
| `maintenance({alarmId})` | `GET_ALARM_MAINTENANCE`（支持数组，最多 20 个） |
| `urged({mac,alarmId})` | `ALARM_URGED`（`mac` 为**必填**） |
| `boxMaintenanceHistory({mac?,start,end})` | `GET_BOX_MAINTENANCE_HISTORY`（`start`/`end` 必填，`pageSize` ≤100） |

> ⚠️ 告警窗口是**存储时间**：`start` 需在最近一个月内，`end - start ≤ 48h`。

### `stats`
`day()` / `hour()` / `month()` 对应 **`GET_BOXES_DAY_STATS`** / `_HOUR_` / `_MON_STATS`，
另有 `totalElectricity(rows)` 帮你从 `addr=0` 那一行取设备总电量。

> ⚠️ 方法名是 `GET_BOXES_*`（BOX + **ES**），不是 `GET_BOXS_*`。
> 少一个 E 时平台返回「接口名称(X)无效！」，很容易被误判成「接口未开通」。

### `thirdparty` — 第三方设备
烟感 / 燃气 / 水表 / 温湿度 / 光照等（`equipmentType ≠ 1/2`）走独立接口：

| 方法 | 平台接口 |
| --- | --- |
| `status({mac,equipmentType,dataType?,startDate?,endDate?})` | `GET_THR_DEVICE_STATUS` |
| `statusParsed(...)` | 同上，并把 `data` 的 JSON 字符串解析成对象 |
| `listDevices({equipmentType})` | `GET_BOXES` 按类型过滤 |

```js
// 感烟（6）
const rows = await ElectricOpenPlatform.thirdparty.statusParsed({
  mac: '866971033551412',
  equipmentType: ElectricOpenPlatform.ThirdPartyEquipmentType.SmokeForSafe,
});

// 智能水表（16）必须指定 dataType
await ElectricOpenPlatform.thirdparty.status({
  mac: 'W1',
  equipmentType: ElectricOpenPlatform.ThirdPartyEquipmentType.SmartWaterMeter,
  dataType: ElectricOpenPlatform.MeterQueryType.Range,
  startDate: '2026-11-01',
  endDate: '2026-11-02',
});
```

平台约束（插件已内置校验）：
- `equipmentType` 传 1（微断）或 2（电气火灾）会被拒绝 → 改用 `channel.realtime()`
- 智能水表(16) 必须带 `dataType`；`dataType=1` 时必须带 `startDate`/`endDate`

### `scene` / `push`
`scene.list()` / `scene.execute()`；`push.query/subscribe/unsubscribe/failedCheck/failedReset`。

### 逃生舱
```js
// 平台新增接口、插件还没包时直接用：
const data = await ElectricOpenPlatform.invoke('NEW_METHOD', { foo: 'bar' });
```
同样自动签名、自动续期、自动解包。

## 错误处理

```js
try {
  await ElectricOpenPlatform.control.switchCircuit({ mac, action: 'open', addr: 1 });
} catch (err) {
  if (err instanceof ElectricOpenPlatform.ElectricOpenPlatformError) {
    switch (err.code) {
      case '1001':
        return showToast('设备不在线');
      case ElectricOpenPlatform.TransportCodes.NOT_PROVISIONED:
        return showToast('该接口未开通，请联系平台商务');
      case ElectricOpenPlatform.TransportCodes.NETWORK:
        return showToast('网络异常，请稍后重试');
      default:
        return showToast(err.message);   // 已含平台中文提示
    }
  }
  throw err;
}
```

`err` 上还有 `isTokenExpired`、`isRetryable`、`method`、`details`、`toJSON()`。

## 安全

1. **不要硬编码 `appSecret`**。Cordova 没有构建期环境变量，建议从后端运行时下发，或至少确保构建产物不提交仓库。
2. 已打包进 App 的密钥**无法真正保密**。若威胁模型要求保密，应让自建后端代持密钥并转发签名请求（可用 `transport` 切到自家代理）。
3. token 默认存 `SharedPreferences` / `UserDefaults`。需要更高安全级别时，可把 `TokenStorage.java` 换成 `EncryptedSharedPreferences`，接口不变。
4. 控制类接口（分合闸）会真实操作现场设备，务必二次确认。
5. 插件**从不**把 `accessToken` / `appSecret` 写进日志；`logger` 回调里已做脱敏。

## 平台支持

| 平台 | 状态 |
| --- | --- |
| Android | ✅ `HttpURLConnection` + `SharedPreferences`（`cordova-android` 10 / 15 均验证过） |
| iOS | ✅ `URLSession` + `UserDefaults`（需 Cordova iOS ≥ 6） |
| Browser | ⚠️ 仅调试用，需自备代理（平台不返回 CORS 头） |

## 已验证行为

以下结论来自对真实平台（`projectCode=P00000104893`）的联调：

| 接口 | 结果 |
| --- | --- |
| `GET_PROJECT_INFO` | ✅ |
| `GET_BOXES` | ✅ 11 台设备，在线 2 台 |
| `GET_BOX_POSITION_TREE` | ✅ |
| `GET_BOX_CHANNELS_REALTIME` | ✅ 线路实时电压/电流/温度/漏电 |
| `GET_BOX_CHANNELS` / `_OC` | ✅ 28 个有效节点 |
| `GET_BOX_CHANNELS_REALTIME_HISTORY` | ✅（参数是 `date`，**不是**区间） |
| `GET_BOX_ALARM` | ✅ 近 24h 告警 |
| `GET_BOX_PROPERTIES` | ✅（**必须**带 `nodeType=COMM`） |
| `GET_BOX_ONLINE_HISTORY` | ✅（参数是 `startTime`/`endTime`） |
| `GET_BOX_MAINTENANCE_HISTORY` | ✅（`start`/`end` 必填） |
| `GET_CFG_TIMER` / `GET_CFG_LKICHK` | ✅ |
| `SCENE_SERVICE` / `SUBSCRIBE_PUSH_SERVICE` | ✅ |
| `GET_BOXES_DAY_STATS` / `_HOUR_STATS` / `_MON_STATS` | ✅ |
| `oauth/refresh.as` | ✅ |

**唯一未开通的接口：`GET_BOX`**（对照实验确认是接口级未授权，不是签名问题），需要请联系平台商务。

### 接口命名陷阱

| 现象 | 真实原因 |
| --- | --- |
| `接口名称(X)无效！` | 方法名写错 **或** 该 appKey 未开通 |
| `无效的接口方法参数签名！` | 既可能是签名错，**也可能是接口未开通**——平台复用同一文案 |

典型踩坑：`GET_BOXES_DAY_STATS` 写成 `GET_BOXS_DAY_STATS`（少一个 `E`），返回「接口名称无效」，极易误判为“未开通”。

## 关于 CORS

平台 `open.snd02.com` **不返回 `Access-Control-Allow-Origin`**，`OPTIONS` 预检返回 `403`。
所以在 WebView 里用 `fetch` / `XMLHttpRequest` 直连必然失败（表现为 `status 0` / `Failed to fetch`）。

本插件因此**不走 WebView 网络栈**：

```
业务代码 → RouterClient → postForm() → cordova-exec
                                          ↓
                              cordova.exec('ElectricOpenPlatform', 'request', ...)
                                          ↓
                          原生 HttpURLConnection / URLSession → 平台
```

请求不发 `Origin`、不做预检，因此不存在 CORS 问题。这也是本项目必须同时提供原生层、而不是纯 JS 的原因。

## 传输选择

`configure()` 时按以下优先级选传输实现：

| 优先级 | 条件 | 实现 |
| --- | --- | --- |
| 1 | `configure({ transport })` 显式指定 | 调用方给的实现 |
| 2 | configure 之前 `setTransport()` 注入 | 调用方给的实现 |
| 3 | 有 `cordova` 全局 / `cordova.exec` | `cordova-exec`（原生桥） |
| 4 | 浏览器 / Node | `fetch` |

排查时可看 `ElectricOpenPlatform.describe()` 里的 `host` 与 `transport` 字段。

> 判定用「有没有 `cordova` 全局」而不是只看 `cordova.exec`：`cordova.js` 先挂出 `cordova` 对象，`exec` 要 `deviceready` 之后才补上。只看 `exec` 会把「已加载 cordova、仍在 deviceready 之前」的 App 误判成 web → 选 fetch → 被 CORS 拦死。

## 已知兼容性

`cordova-android` 的 `CordovaPlugin` 只提供三种 `execute` 重载：

```java
execute(String action, String rawArgs, CallbackContext ctx)
execute(String action, JSONArray args, CallbackContext ctx)
execute(String action, CordovaArgs args, CallbackContext ctx)
```

**没有 `JSONObject` 版本**（10.x / 15.x 均已确认）。本插件的原生实现按 `JSONArray` 签名实现，取第 0 个元素作为参数对象，因此对 10.x 与 15.x 都兼容。

## 本地开发

```bash
npm install
npm run build     # 源码 src/ts → www/electric-open-platform.js（单文件 IIFE）
npm run lint      # tsc --noEmit
```

修改 `src/ts/` 后必须重新 `npm run build`，否则 `www/` 里的产物是旧的。原生代码改动需要重新 `cordova plugin rm` + `cordova plugin add` 才会同步到 platform。

## 平台接口速查

| 项 | 值 |
| --- | --- |
| 基础地址 | `https://open.snd02.com` |
| 取 code | `POST /oauth/authverify2.as` |
| 取 token | `POST /oauth/token.as` |
| 刷新 token | `POST /oauth/refresh.as` |
| 业务接口 | `POST /invoke/router.as`（`application/x-www-form-urlencoded`，UTF-8） |
| 公共参数 | `client_id`(=appKey)、`method`、`access_token`、`timestamp`(`yyyyMMddHHmmss` 本地时间)、`sign` |
| `client_secret` | `MD5(appKey + grant_type + redirect_uri + code\|refreshToken + appSecret)` |
| 签名 | 除 `sign` 外所有参数按**参数名升序**拼接**参数值** + `appSecret`，再 MD5 |

## License

MIT
