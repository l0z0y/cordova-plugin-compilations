# cordova-plugin-yz

`cordova-plugin-yz` 是一个仅支持 Android 的 Cordova 插件，用于调用供应商提供的 `yztitoapi.aar`。该 AAR 中的核心类为：

```java
com.yztiot.yztitoapi.yztiotManager
```

插件已经将 AAR 放在插件根目录的 `libs` 文件夹中：

```text
cordova-plugin-yz/
├── libs/
│   └── yztitoapi.aar
├── src/
│   └── android/
│       ├── YzPlugin.java
│       └── build.gradle
├── www/
│   └── yz.js
├── plugin.xml
├── package.json
└── README.md
```

## 一、安装到 Cordova 项目

在 Cordova 项目根目录执行：

```bash
cordova plugin add /d/Code/plugin/codova-plugin-yz
```

Windows 命令行也可以使用 Windows 路径：

```bash
cordova plugin add D:\Code\plugin\codova-plugin-yz
```

安装后，插件会把：

```text
libs/yztitoapi.aar
```

复制到 Android 工程的：

```text
platforms/android/app/src/main/libs/yztitoapi.aar
```

然后执行构建：

```bash
cordova platform add android
cordova build android
```

如果之前已经安装过旧版本插件，建议先移除再重新安装：

```bash
cordova plugin rm cordova-plugin-yz
cordova plugin add /d/Code/plugin/codova-plugin-yz
cordova clean android
cordova build android
```

## 二、JavaScript / TypeScript 调用方式

`plugin.xml` 会将 JavaScript 模块暴露为：

```javascript
window.yz
cordova.plugins.yz
```

在 TypeScript 项目中，可以在需要调用的文件中声明：

```typescript
declare var window: any
```

然后使用 `window.yz` 调用：

```typescript
declare var window: any

window.yz.getDeviceVersion(
    (version: string) => {
        console.log("设备型号：", version)
    },
    (error: any) => {
        console.error("获取设备型号失败：", error)
    },
)
```

也可以使用 `cordova.plugins.yz`：

```typescript
declare var cordova: any

cordova.plugins.yz.getDeviceID((deviceId: string) => {
    console.log("设备 ID：", deviceId)
}, console.error)
```

> 建议优先使用 `window.yz`，因为它与插件的 clobber 配置一致，调用方式更简单。

所有方法采用 Cordova 回调形式(详细请查看 [yz.js](www/yz.js) 中的注释)：

```javascript
window.yz.方法名(参数..., 成功回调, 失败回调);
```

## 三、设备和系统信息

```typescript
declare var window: any

// 获取设备型号
window.yz.getDeviceVersion(console.log, console.error)

// 获取 Android 系统版本
window.yz.getAndroidVersion(console.log, console.error)

// 获取 JAR/AAR 版本
window.yz.getJARVersion(console.log, console.error)

// 获取设备唯一 ID
window.yz.getDeviceID(console.log, console.error)

// 获取系统 SDK 版本
window.yz.getSDKVersion(console.log, console.error)

// 获取内部存储总大小，单位为字节
window.yz.getTotalInternalMemorySize(console.log, console.error)

// 获取剩余运行内存，单位为字节
window.yz.getFreeMemorySize(console.log, console.error)

// 获取内部 SD 卡路径
window.yz.getInternalSDCardPath(console.log, console.error)

// 获取外部 SD 卡路径
window.yz.getExternalSDCardPath(console.log, console.error)

// 获取 U 盘路径，没有 U 盘时可能返回 null
window.yz.getUsbStoragePath(console.log, console.error)
```

## 四、文件、应用和页面操作

```typescript
// 判断应用是否安装
window.yz.isAppExist("com.android.settings", (exists: boolean) => console.log("是否存在：", exists), console.error)

// 判断文件或目录是否存在
window.yz.isExist("/sdcard/test.apk", console.log, console.error)

// 打开系统设置
window.yz.startSettings(console.log, console.error)

// 打开 Wi-Fi 设置
window.yz.startWifiSettings(console.log, console.error)

// 启动指定应用的 Activity
window.yz.startActivity("com.android.settings", "com.android.settings.Settings", console.log, console.error)

// 静默安装 APK，通常需要 Root 权限
window.yz.installAppSilent("/sdcard/test.apk", console.log, console.error)

// 卸载应用，通常需要 Root 权限
window.yz.uninstallAppSilent("com.example.app", console.log, console.error)

// 静默安装 APK 后重启系统
window.yz.installAppSilentAndRebootSystem("/sdcard/test.apk", 5, console.log, console.error)

// 安装 APK 后启动指定包名的应用
window.yz.installAppAndStartUp("/sdcard/test.apk", "com.example.app", console.log, console.error)
```

## 五、GPIO 操作

### 5.1 一次性读取 GPIO

供应商 AAR 的实际方法是一次读取，不是硬件监听接口：

```typescript
// 返回 0 或 1；读取失败时供应商实现可能返回 -1
window.yz.getGpioValue(
    "GPIO_A5",
    (value: number) => {
        console.log("GPIO 当前值：", value)
    },
    (error: any) => {
        console.error("读取 GPIO 失败：", error)
    },
)
```

### 5.2 设置 GPIO 输出值

```typescript
// value 传入 0 或 1
window.yz.setGpioValue("GPIO_A5", 1, () => console.log("GPIO 输出设置成功"), console.error)
```

### 5.3 GPIO 持续监听

由于供应商 AAR 没有提供 GPIO 回调监听接口，插件原生层为每个 GPIO 创建一个专用轮询线程，轮询 `/proc/yz_gpio/<GPIO名称>`。

通知规则：

1. 订阅后第一次读取到有效值时通知一次；
2. 值没有变化时不重复通知；
3. 值从 0 变成 1 或从 1 变成 0 时通知；
4. 通知值只会是数字 `0` 或 `1`；
5. 最后一个订阅者取消订阅后，原生轮询线程停止。

```typescript
declare var window: any

const gpioStream = window.yz.observeGpioValue("GPIO_A5", {
    // 单位：毫秒，默认 100，最小 20
    interval: 50,
})

const subscription = gpioStream.subscribe(
    (value: number) => {
        console.log("GPIO_A5 值变化：", value)
    },
    (error: any) => {
        console.error("GPIO 轮询失败：", error)
    },
)

// 页面销毁、组件卸载或不再需要监听时调用
subscription.unsubscribe()
```

也可以添加多个订阅者。多个订阅者会共用同一个 GPIO 原生轮询线程：

```typescript
const stream = window.yz.observeGpioValue("GPIO_A5")

const subscription1 = stream.subscribe((value: number) => {
    console.log("订阅者 1：", value)
})

const subscription2 = stream.subscribe((value: number) => {
    console.log("订阅者 2：", value)
})

subscription1.unsubscribe()
subscription2.unsubscribe()
```

## 六、实体按键（触摸模式）

实体按键 PI4 相关接口：读取 / 切换触摸芯片模式，以及模拟按下实体按键。

```typescript
// 获取当前触摸模式：0=ILITEK，1=FORWARD，其他值表示未知
window.yz.getTouchMode((mode: number) => {
    const name = mode === 1 ? "FORWARD" : mode === 0 ? "ILITEK" : "未知"
    console.log("当前触摸模式：", name)
}, console.error)

// 设置触摸模式：0=ILITEK，1=FORWARD
window.yz.setTouchMode(1, () => console.log("切换触摸模式成功"), console.error)

// 模拟按下实体按键 PI4（默认按压 150ms）
window.yz.simulatePi4Press(() => console.log("已模拟按下 PI4"), console.error)
```

> `setTouchMode` 与 `simulatePi4Press` 依赖设备上的 `/data/local/tmp/pi4_sim` 通道；AAR 还提供带按压时长的重载 `simulatePi4Press(int ms)`，插件当前暴露的是默认 150ms 的无参版本。

## 七、网络操作

```typescript
// 获取网卡 IPv4 地址，例如 eth0、wlan0、ppp0
window.yz.getIPv4("eth0", console.log, console.error)

// 设置静态 IP
window.yz.setStaticIP("static", "192.168.1.100", "24", "192.168.1.1", "8.8.8.8", console.log, console.error)

// 切换为 DHCP 动态获取 IP
window.yz.setStaticIP("dhcp", "0.0.0.0", "0", "0.0.0.0", "0.0.0.0", console.log, console.error)

// 使用 Root 权限执行系统命令
window.yz.execSuCmd("ifconfig eth0 up", console.log, console.error)
```

## 八、时间操作

```typescript
// 获取系统日期，格式通常为 yyyy/MM/dd
window.yz.getSystemDate(console.log, console.error)

// 获取系统时间，格式通常为 hh:mm:ss
window.yz.getSystemTime(console.log, console.error)

// 判断系统时间是否自动同步
window.yz.isAutoDateTime(console.log, console.error)

// 使用 Date 设置系统时间
window.yz.setSystemTime(new Date(), () => console.log("系统时间设置成功"), console.error)

// 也可以直接传入 Unix 毫秒时间戳
window.yz.setSystemTime(Date.now(), console.log, console.error)
```

## 九、显示、亮度和旋转

```typescript
// 获取屏幕分辨率，例如 1920x1080
window.yz.getDisplayMode(console.log, console.error)

// 获取屏幕宽度和高度
window.yz.getDisplayWidth(console.log, console.error)
window.yz.getDisplayHeight(console.log, console.error)

// 获取和设置屏幕 DPI
window.yz.getDisplayDensity(console.log, console.error)
window.yz.setDisplayDensity(240, console.log, console.error)

// 获取系统最大亮度和当前亮度
window.yz.getSystemMaxBrightness(console.log, console.error)
window.yz.getSystemBrightness(console.log, console.error)

// 调整亮度
window.yz.increaseBrightness(console.log, console.error)
window.yz.decreaseBrightness(console.log, console.error)
window.yz.setBrightness(150, console.log, console.error)

// 设置旋转方向：0、1、2、3
window.yz.setRotation(1, console.log, console.error)

// 模拟按键：Home=3、返回=4、电源=26
window.yz.inputkeyevent(3, console.log, console.error)
```

## 十、音量和桌面

```typescript
// 获取最大音量和当前音量
window.yz.getSystemMaxVolume(console.log, console.error)
window.yz.getSystemCurrenVolume(console.log, console.error)

// 增大或降低音量
window.yz.setRaiseSystemVolume(console.log, console.error)
window.yz.setLowerSystemVolume(console.log, console.error)

// 设置音量值
window.yz.setSystemVolumeIndex(10, console.log, console.error)

// 设置默认桌面应用
window.yz.setDefaultLauncher("com.example.launcher", "com.example.launcher.MainActivity", console.log, console.error)
```

## 十一、设备控制

```typescript
// 关闭设备，通常需要厂商系统权限
window.yz.shutdown(console.log, console.error)

// 重启设备，参数为延迟秒数
window.yz.reboot(5, console.log, console.error)

// 开启或关闭 TCP ADB
window.yz.startTcpAdb(true, console.log, console.error)
```

## 十二、注意事项

1. 本插件只支持 Android。
2. 供应商 AAR 的最低 Android SDK 为 API 29，建议使用 Android API 29 或更高版本构建。
3. GPIO、静态 IP、Root 命令、静默安装/卸载、关机、重启、默认桌面、屏幕控制等功能依赖厂商固件和系统权限。
4. `execSuCmd` 会将命令交给供应商 SDK 的 Root 执行接口，请只传入可信命令。
5. GPIO 端口名称必须使用目标设备支持的名称，例如 `GPIO_A5`；不同硬件型号的 GPIO 列表可能不同。
6. AAR 中 `getGpioValue` 的实际签名是 `getGpioValue(String port)`，供应商文字文档中的双参数声明与实际字节码不一致，以 AAR 实际签名为准。
7. `observeGpioValue` 是文件轮询，不是硬件中断监听。轮询间隔越短，线程唤醒越频繁，建议根据实际响应速度设置合理的间隔。
8. 页面、组件或 WebView 销毁时，应调用 `subscription.unsubscribe()`，避免继续保持业务层订阅。

## 十三、插件导出名称

插件配置文件中的导出名称为：

```xml
<clobbers target="yz" />
<clobbers target="cordova.plugins.yz" />
```

因此以下两种写法都可以：

```typescript
declare var window: any
window.yz.getDeviceVersion(console.log, console.error)
```

```typescript
declare var cordova: any
cordova.plugins.yz.getDeviceVersion(console.log, console.error)
```
