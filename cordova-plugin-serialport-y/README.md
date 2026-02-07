# cordova-plugin-serialport-y

一个用于在 Cordova 应用中访问 Android 串口的插件，支持持续监听串口数据，并将接收的原始字节以「大写十六进制字符串」形式回调给 JS。

## 特性

-   串口打开后持续监听（非一次性回调），内部使用 keepCallback 实现。
-   接收数据统一以大写十六进制字符串回调，例如 `0A1B2C`。
-   JS 侧导出两个全局名：`window.SerialPort` 与 `cordova.plugins.serialPort`。
-   初始化可配置：波特率策略、校验位、数据位、停止位、日志等。

## 安装

```bash
cordova plugin add cordova-plugin-serialport-y
```

或从你的 Git 仓库安装：

```bash
cordova plugin add https://github.com/l0z0y/cordova-plugin-compilations.git
```

## 快速开始

```javascript
// 可选：初始化配置
SerialPort.init(
    {
        intervalSleep: 50, // 轮询间隔(ms)
        enableLog: false, // 是否开启底层日志
        logTag: "SerialPort",
        databits: 8, // 数据位：5/6/7/8
        parity: 0, // 校验位：0-None 1-Odd 2-Even 3-Mark 4-Space（随底层库）
        stopbits: 1, // 停止位：1/2（随底层库）
        strategy: 0, // 黏包处理策略索引（随底层库）
    },
    function () {
        console.log("Serial init ok")
    },
    function (err) {
        console.error("Serial init error", err)
    }
)

// 打开串口，并持续接收数据（十六进制字符串）
SerialPort.open(
    "/dev/ttyS4",
    115200,
    function (hex) {
        // 每当串口收到数据都会回调一次，例如："0A1B2C"
        console.log("RX:", hex)
    },
    function (err) {
        console.error("Open error", err)
    }
)

// 发送数据（按你的设备协议准备字符串）
SerialPort.send(
    "AT+CMD\r\n",
    function () {
        console.log("TX ok")
    },
    function (err) {
        console.error("TX error", err)
    }
)

// 关闭串口（停止监听）
SerialPort.close(
    function () {
        console.log("Closed")
    },
    function (err) {
        console.error("Close error", err)
    }
)
```

你也可以通过 `cordova.plugins.serialPort` 使用相同 API：

```javascript
cordova.plugins.serialPort.open("/dev/ttyS4", 115200, onData, onError)
```

## API 说明

所有方法都遵循 Cordova `exec(success, error, ...)` 的风格。

-   `SerialPort.init(options, success, error)`

    -   options（可选）：
        -   `intervalSleep` number 默认 50
        -   `enableLog` boolean 默认 false
        -   `logTag` string 默认 "SerialPort"
        -   `databits` number 默认 8
        -   `parity` number 默认 0
        -   `stopbits` number 默认 1
        -   `strategy` number 默认 0（对应底层 SimpleSerialPortManager 的策略枚举索引）
    -   success：初始化成功回调
    -   error：失败回调

-   `SerialPort.open(port, baudRate, success, error)`

    -   `port` string 例如 `/dev/ttyS4`
    -   `baudRate` number 例如 `115200`
    -   `success(hex: string)`：持续回调接收的数据（大写十六进制、无空格，例如 "0A1B2C"）。
    -   `error(err)`：出错回调

-   `SerialPort.send(data, success, error)`

    -   `data` string：要发送的数据（按你的设备协议拼接，如命令、换行等）。

-   `SerialPort.close(success, error)`
    -   关闭串口并停止后续回调。

## 数据格式

-   串口物理层传输的是字节流（byte[]），插件在 Android 端接收到字节后会把其转换为「大写十六进制字符串」再回调 JS。
-   示例：字节 `0x0A, 0x1B, 0x2C` -> `"0A1B2C"`。
-   如需空格分隔或小写形式，可在 Android 端或 JS 端自行转换；如你需要插件内置不同格式开关，可提 issue 或 PR。

## 平台支持

-   Android（基于 `com.cl.serialportlibrary.SimpleSerialPortManager`）

## 引用的库 / 依赖

-   Android 串口实现基于第三方库：`com.cl.serialportlibrary.SimpleSerialPortManager`
    -   Gradle 依赖（已在本插件 `src/android/build.gradle` 中声明）：
        ```gradle
        repositories {
            maven { url 'https://jitpack.io' }
        }
        dependencies {
            implementation 'com.github.cl-6666:serialPort:v5.0.7'
        }
        ```
    -   主要用途：负责底层串口打开、发送、接收与黏包处理策略，本插件在其回调上封装为 Cordova 的持续监听回调（keepCallback）。

## 注意事项

-   Android 权限与清单由插件自动注入。
-   若你自定义包名或目录（例如 `com.yy.serialport`），请确认 `plugin.xml` 中 `<feature>` 与 `source-file target-dir` 一致。
-   某些设备节点可能需要 root 或特定权限。

## 常见问题

1. 为什么数据是十六进制字符串？  
   为了避免字符集误判及提升通用性，插件统一以十六进制返回原始字节。你可以在 JS 侧自行解析为数值或字符串。

2. 能否改为返回 ArrayBuffer 或 Uint8Array？  
   可以。当前实现已在 Android 侧做了 hex 规范返回。如你更偏好二进制回调，可提需求，我可以添加配置开关。

3. 如何确认串口节点与波特率？  
   请参考硬件设备说明书，或通过 adb/shell 查看设备 `/dev/tty*` 列表。

## 版本变更

-   v1.0.3
    -   统一作者信息
    -   更新仓库地址

-   v1.0.0
    -   首次发布：持续监听、十六进制回调、JS 全局导出与 init 可配置。

## 许可证

Apache-2.0
\*\*\* End Patch
