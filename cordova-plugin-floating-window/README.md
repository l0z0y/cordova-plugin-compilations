# Cordova Floating Window Plugin

一个用于 Cordova/PhoneGap 的悬浮窗插件，支持后台服务、事件通知、跳转桌面和显示悬浮窗功能。

**特性：** 使用 XXPermissions 库处理权限请求，提供更好的用户体验。

## 功能特性

-   ✅ 启动/停止后台服务
-   ✅ 显示/隐藏悬浮窗（支持图片显示）
-   ✅ 悬浮窗可拖拽移动
-   ✅ 跳转到桌面
-   ✅ 事件监听（显示、隐藏、点击等）
-   ✅ 权限检查和请求

## 安装

```bash
cordova plugin add cordova-plugin-floating-window
```

或者从本地安装：

```bash
cordova plugin add ./cordova-plugin-floating-window
```

## 使用方法

### 1. 检查并请求权限

```javascript
// 检查权限
window.FloatingWindow.checkPermission(
    function (hasPermission) {
        if (hasPermission === 0) {
            // 请求权限
            window.FloatingWindow.requestPermission(
                function () {
                    console.log("权限请求已打开")
                },
                function (error) {
                    console.error("请求权限失败:", error)
                }
            )
        }
    },
    function (error) {
        console.error("检查权限失败:", error)
    }
)
```

### 2. 启动后台服务

```javascript
window.FloatingWindow.startService(
    function () {
        console.log("后台服务已启动")
    },
    function (error) {
        console.error("启动服务失败:", error)
    }
)
```

### 3. 显示悬浮窗

```javascript
// 显示悬浮窗（使用本地图片路径）
window.FloatingWindow.showFloatingWindow(
    "/android_asset/www/img/icon.png", // 图片路径
    150, // 宽度（像素）
    150, // 高度（像素）
    function () {
        console.log("悬浮窗已显示")
    },
    function (error) {
        console.error("显示悬浮窗失败:", error)
    }
)

// 或者使用网络图片
window.FloatingWindow.showFloatingWindow(
    "https://example.com/image.png",
    150,
    150,
    function () {
        console.log("悬浮窗已显示")
    },
    function (error) {
        console.error("显示悬浮窗失败:", error)
    }
)
```

### 4. 隐藏悬浮窗

```javascript
window.FloatingWindow.hideFloatingWindow(
    function () {
        console.log("悬浮窗已隐藏")
    },
    function (error) {
        console.error("隐藏悬浮窗失败:", error)
    }
)
```

### 5. 跳转到桌面

```javascript
window.FloatingWindow.goToHome(
    function () {
        console.log("已跳转到桌面")
    },
    function (error) {
        console.error("跳转失败:", error)
    }
)
```

### 6. 监听事件

```javascript
window.FloatingWindow.setEventListener(function (event) {
    console.log("事件类型:", event.type)
    console.log("事件数据:", event.data)

    switch (event.type) {
        case "show":
            console.log("悬浮窗已显示")
            break
        case "hide":
            console.log("悬浮窗已隐藏")
            break
        case "click":
            console.log("悬浮窗被点击")
            break
    }
})
```

### 7. 停止后台服务

```javascript
window.FloatingWindow.stopService(
    function () {
        console.log("后台服务已停止")
    },
    function (error) {
        console.error("停止服务失败:", error)
    }
)
```

## API 参考

### startService(success, error)

启动后台服务。

**参数：**

-   `success` (Function): 成功回调
-   `error` (Function): 错误回调

### stopService(success, error)

停止后台服务。

**参数：**

-   `success` (Function): 成功回调
-   `error` (Function): 错误回调

### showFloatingWindow(imagePath, width, height, success, error)

显示悬浮窗。

**参数：**

-   `imagePath` (String): 图片路径（本地路径或网络 URL）
-   `width` (Number, 可选): 悬浮窗宽度，默认 100
-   `height` (Number, 可选): 悬浮窗高度，默认 100
-   `success` (Function): 成功回调
-   `error` (Function): 错误回调

### hideFloatingWindow(success, error)

隐藏悬浮窗。

**参数：**

-   `success` (Function): 成功回调
-   `error` (Function): 错误回调

### goToHome(success, error)

跳转到桌面。

**参数：**

-   `success` (Function): 成功回调
-   `error` (Function): 错误回调

### checkPermission(success, error)

检查悬浮窗权限。

**参数：**

-   `success` (Function): 成功回调，返回 1 表示有权限，0 表示无权限
-   `error` (Function): 错误回调

### requestPermission(success, error)

请求悬浮窗权限（使用 XXPermissions 库，会自动弹出权限请求对话框）。

**参数：**

-   `success` (Function): 成功回调，当用户授予权限时调用
-   `error` (Function): 错误回调，当用户拒绝权限时调用。如果用户选择了"不再询问"，错误信息为 "Permission denied permanently"

### setEventListener(callback)

设置事件监听器。

**参数：**

-   `callback` (Function): 事件回调函数，接收事件对象 `{type: String, data: Object}`

**事件类型：**

-   `show`: 悬浮窗显示时触发
-   `hide`: 悬浮窗隐藏时触发
-   `click`: 悬浮窗被点击时触发

## 权限说明

### Android

插件需要以下权限：

-   `SYSTEM_ALERT_WINDOW`: 显示悬浮窗权限（需要用户手动授权）
-   `FOREGROUND_SERVICE`: 前台服务权限
-   `WAKE_LOCK`: 保持设备唤醒权限
-   `INTERNET`: 网络访问权限（用于加载网络图片）

**重要提示：**

-   Android 6.0+ 需要用户手动授予悬浮窗权限
-   插件使用 **XXPermissions** 库来处理权限请求，提供更好的用户体验
-   插件提供了 `checkPermission` 和 `requestPermission` 方法来检查和请求权限
-   `requestPermission` 方法会自动弹出权限请求对话框，用户授权后会自动回调成功或失败

## 图片路径说明

支持的图片路径格式：

-   本地文件路径：`/storage/emulated/0/Pictures/image.png`
-   file:// 协议：`file:///storage/emulated/0/Pictures/image.png`
-   assets 资源：`/android_asset/www/img/icon.png`
-   网络 URL：`https://example.com/image.png`

## 平台支持

-   ✅ Android

## 注意事项

1. **权限要求**：Android 6.0+ 需要用户手动授予悬浮窗权限，否则无法显示悬浮窗。
2. **后台服务**：插件使用前台服务来保持后台运行，会在通知栏显示一个持续通知。
3. **悬浮窗位置**：默认显示在屏幕右上角，用户可以通过拖拽移动位置。
4. **图片加载**：网络图片加载是异步的，可能需要一些时间才能显示。

## 示例代码

完整的使用示例：

```javascript
document.addEventListener(
    "deviceready",
    function () {
        // 检查权限
        window.FloatingWindow.checkPermission(function (hasPermission) {
            if (hasPermission === 0) {
                // 请求权限
                window.FloatingWindow.requestPermission(function () {
                    initFloatingWindow()
                })
            } else {
                initFloatingWindow()
            }
        })

        function initFloatingWindow() {
            // 设置事件监听
            window.FloatingWindow.setEventListener(function (event) {
                console.log("事件:", event.type)
            })

            // 启动服务
            window.FloatingWindow.startService(function () {
                console.log("服务已启动")

                // 显示悬浮窗
                window.FloatingWindow.showFloatingWindow("/android_asset/www/img/icon.png", 150, 150, function () {
                    console.log("悬浮窗已显示")
                })
            })
        }
    },
    false
)
```

## 许可证

Apache-2.0

## 作者

Created for Cordova/PhoneGap applications.
