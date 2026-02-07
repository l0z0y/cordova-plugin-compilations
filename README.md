# Cordova Plugins Collection

这是一个 Cordova/PhoneGap 插件集合仓库，包含多个实用的 Android/iOS 插件。

## 📦 插件列表

### 1. cordova-plugin-floating-window
**版本:** 1.0.0  
**平台:** Android  
**描述:** 悬浮窗插件，支持后台服务、事件通知、跳转桌面和显示悬浮窗功能。

**主要功能：**
- ✅ 启动/停止后台服务
- ✅ 显示/隐藏悬浮窗（支持图片显示）
- ✅ 悬浮窗可拖拽移动
- ✅ 跳转到桌面
- ✅ 事件监听（显示、隐藏、点击等）
- ✅ 权限检查和请求（使用 XXPermissions）

**安装：**
```bash
cordova plugin add ./cordova-plugin-floating-window
```

**文档：** [cordova-plugin-floating-window/README.md](./cordova-plugin-floating-window/README.md)

---

### 2. cordova-plugin-serialport-y
**版本:** 1.0.2  
**平台:** Android  
**描述:** 串口通信插件，基于 com.github.cl-6666:serialPort 库实现。

**主要功能：**
- ✅ 串口打开/关闭
- ✅ 数据发送/接收
- ✅ 串口配置（波特率、数据位、停止位等）

**安装：**
```bash
cordova plugin add ./cordova-plugin-serialport-y
```

**文档：** [cordova-plugin-serialport-y/README.md](./cordova-plugin-serialport-y/README.md)

---

### 3. cordova-plugin-iflytek-aikit
**版本:** 1.0.2  
**平台:** Android  
**描述:** 讯飞语音 AI 能力插件，支持语音唤醒、语音合成、命令词识别等功能。

**主要功能：**
- ✅ 语音唤醒（Wake Up）
- ✅ 语音合成（TTS）
- ✅ 命令词识别（ASR）
- ✅ 音频播放管理

**安装：**
```bash
cordova plugin add ./cordova-plugin-iflytek-aikit
```

**文档：** [cordova-plugin-iflytek-aikit/README.md](./cordova-plugin-iflytek-aikit/README.md)

---

### 4. cordova-plugin-y-utils
**版本:** 1.0.3  
**平台:** Android  
**描述:** 工具类插件，提供文件操作、命令执行等功能。

**主要功能：**
- ✅ 文件写入（支持 GBK 编码）
- ✅ 命令执行
- ✅ 工具方法

**安装：**
```bash
cordova plugin add ./cordova-plugin-y-utils
```

---

### 5. cordova-plugin-esptouch
**版本:** 2.0.0  
**平台:** Android, iOS  
**描述:** ESP8266/ESP32 设备配网插件，支持 ESPTouch v1 和 v2 协议。

**主要功能：**
- ✅ ESPTouch v1 协议支持
- ✅ ESPTouch v2 协议支持
- ✅ WiFi 配置
- ✅ 设备发现

**安装：**
```bash
cordova plugin add ./cordova-plugin-esptouch
```

**文档：** [cordova-plugin-esptouch/README.md](./cordova-plugin-esptouch/README.md)

---

## 🚀 快速开始

### 安装单个插件

```bash
# 进入你的 Cordova 项目目录
cd your-cordova-project

# 安装插件（使用本地路径）
cordova plugin add /path/to/plugin/cordova-plugin-floating-window
```

### 安装所有插件

```bash
# 在 Cordova 项目中逐个安装
cordova plugin add /path/to/plugins/cordova-plugin-floating-window
cordova plugin add /path/to/plugins/cordova-plugin-serialport-y
cordova plugin add /path/to/plugins/cordova-plugin-iflytek-aikit
cordova plugin add /path/to/plugins/cordova-plugin-y-utils
cordova plugin add /path/to/plugins/cordova-plugin-esptouch
```

## 📋 系统要求

- **Cordova:** >= 9.0.0
- **cordova-android:** >= 9.0.0
- **Node.js:** >= 12.0.0
- **Android SDK:** API Level 21+

## 🔧 开发环境

### Android 开发环境
- Android Studio
- Android SDK
- Gradle

### iOS 开发环境（仅限 esptouch 插件）
- Xcode
- CocoaPods

## 📝 许可证

各个插件使用不同的许可证，请查看各插件的 README.md 或 LICENSE 文件。

- `cordova-plugin-floating-window`: Apache-2.0
- `cordova-plugin-serialport-y`: ISC
- `cordova-plugin-iflytek-aikit`: MIT
- `cordova-plugin-y-utils`: ISC
- `cordova-plugin-esptouch`: MIT

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题或建议，请通过 GitHub Issues 联系。

## 📚 相关资源

- [Cordova 官方文档](https://cordova.apache.org/docs/en/latest/)
- [Cordova Android 平台文档](https://cordova.apache.org/docs/en/latest/guide/platforms/android/index.html)

---

**注意：** 这些插件主要用于 Android 平台，部分插件支持 iOS。使用前请仔细阅读各插件的 README 文档。
