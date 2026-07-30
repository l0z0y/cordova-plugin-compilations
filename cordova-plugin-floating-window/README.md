# Cordova Floating Window Plugin

一个用于 Cordova/PhoneGap 的悬浮窗插件，支持后台服务、事件通知、跳转桌面和显示悬浮窗功能。

**特性：** 使用 XXPermissions 库处理权限请求，提供更好的用户体验。

## 最新更新

-   ✨ **点击悬浮窗自动将应用带到前台**：点击悬浮窗时，除了触发点击事件外，还会自动将应用带到前台，提升用户体验
-   🔧 **优化图片加载方式**：本地图片现在直接从 assets 的 `www` 目录加载，路径更简洁（只需相对于 `www` 目录的路径）
-   🔐 **改进权限处理**：权限检查使用 Android 原生方法，更准确可靠；权限被永久拒绝时自动跳转到设置页面
-   📦 **优化插件配置**：Service 配置更加规范，使用标准的 manifest 配置方式

## 功能特性

-   ✅ 启动/停止后台服务
-   ✅ 显示/隐藏悬浮窗（支持图片显示）
-   ✅ 悬浮窗可拖拽移动
-   ✅ 点击悬浮窗自动将应用带到前台
-   ✅ 跳转到桌面
-   ✅ 事件监听（显示、隐藏、点击等）
-   ✅ 权限检查和请求（权限被永久拒绝时自动跳转设置页面）

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
// 显示悬浮窗（使用 assets 中的图片，路径相对于 www 目录）
window.FloatingWindow.showFloatingWindow(
    "img/icon.png", // 图片路径（相对于 www 目录）
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
            console.log("悬浮窗被点击，应用将被带到前台")
            break
    }
})
```

**注意：** 点击悬浮窗时，除了触发 `click` 事件外，还会自动将应用带到前台。

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
-   `error` (Function): 错误回调，当用户拒绝权限时调用。如果用户选择了"不再询问"，错误信息为 "Permission denied permanently"，并且会自动跳转到权限设置页面

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
-   插件使用 **Android 原生方法** `Settings.canDrawOverlays()` 来检查权限
-   插件使用 **XXPermissions** 库来处理权限请求，提供更好的用户体验
-   插件提供了 `checkPermission` 和 `requestPermission` 方法来检查和请求权限
-   `requestPermission` 方法会自动弹出权限请求对话框，用户授权后会自动回调成功或失败
-   如果用户选择了"不再询问"并拒绝权限，插件会自动跳转到系统权限设置页面，方便用户手动开启权限

## 图片路径说明

支持的图片路径格式：

-   **Assets 资源（推荐）**：路径相对于 `www` 目录，例如 `img/icon.png` 对应 `www/img/icon.png`
-   **网络 URL**：`https://example.com/image.png` 或 `http://example.com/image.png`

**重要提示：**

-   本地图片必须放在项目的 `www` 目录下（会被打包到 assets 中）
-   图片路径不需要包含 `/android_asset/www/` 前缀，直接使用相对于 `www` 目录的路径即可
-   例如：如果图片在 `www/img/icon.png`，则路径为 `img/icon.png`
-   网络图片加载是异步的，可能需要一些时间才能显示

## 平台支持

-   ✅ Android

## 注意事项

1. **权限要求**：Android 6.0+ 需要用户手动授予悬浮窗权限，否则无法显示悬浮窗。如果权限被永久拒绝，插件会自动跳转到权限设置页面。
2. **后台服务**：插件使用前台服务来保持后台运行，会在通知栏显示一个持续通知。
3. **悬浮窗位置**：默认显示在屏幕右上角，用户可以通过拖拽移动位置。
4. **图片加载**：
    - 本地图片必须放在 `www` 目录下，路径相对于 `www` 目录
    - 网络图片加载是异步的，可能需要一些时间才能显示
5. **点击悬浮窗**：点击悬浮窗时会自动将应用带到前台，方便用户快速返回应用。

## 示例代码

### 完整示例 - 初始化插件

```javascript
document.addEventListener(
    "deviceready",
    function () {
        // 设置事件监听器（必须在 deviceready 后调用）
        if (window.FloatingWindow) {
            window.FloatingWindow.setEventListener(function (event) {
                console.log("事件类型:", event.type)
                console.log("事件数据:", JSON.stringify(event.data))

                switch (event.type) {
                    case "show":
                        console.log("悬浮窗已显示")
                        break
                    case "hide":
                        console.log("悬浮窗已隐藏")
                        break
                    case "click":
                        console.log("悬浮窗被点击，应用将被带到前台")
                        break
                }
            })
            console.log("事件监听器已设置")
        } else {
            console.error("错误: FloatingWindow 插件未找到")
        }
    },
    false
)
```

### 示例方法 1 - 检查权限

```javascript
function checkPermission() {
    if (!window.FloatingWindow) {
        console.error("错误: 插件未加载")
        return
    }

    window.FloatingWindow.checkPermission(
        function (hasPermission) {
            if (hasPermission === 1) {
                console.log("✓ 已有悬浮窗权限")
                // 可以继续使用悬浮窗功能
            } else {
                console.log("✗ 没有悬浮窗权限")
                // 需要请求权限
                requestPermission()
            }
        },
        function (error) {
            console.error("检查权限失败: " + error)
        }
    )
}
```

### 示例方法 2 - 请求权限

```javascript
function requestPermission() {
    if (!window.FloatingWindow) {
        console.error("错误: 插件未加载")
        return
    }

    window.FloatingWindow.requestPermission(
        function () {
            console.log("✓ 权限已授予")
            // 权限获取成功，可以继续使用悬浮窗功能
        },
        function (error) {
            if (error === "Permission denied permanently") {
                console.log("✗ 权限被永久拒绝，请前往系统设置手动开启")
                // 插件已自动跳转到权限设置页面
            } else {
                console.log("✗ 权限请求失败: " + error)
            }
        }
    )
}
```

### 示例方法 3 - 显示悬浮窗

```javascript
function showFloatingWindow(imagePath, width, height) {
    if (!window.FloatingWindow) {
        console.error("错误: 插件未加载")
        return
    }

    // 参数验证和默认值
    imagePath = imagePath || "img/icon.png" // 默认图片路径（相对于 www 目录）
    width = width || 150 // 默认宽度
    height = height || 150 // 默认高度

    console.log("显示悬浮窗: " + imagePath + " (" + width + "x" + height + ")")

    window.FloatingWindow.showFloatingWindow(
        imagePath,
        width,
        height,
        function () {
            console.log("✓ 悬浮窗已显示")
        },
        function (error) {
            console.error("✗ 显示悬浮窗失败: " + error)
            // 常见错误：权限未授予
            if (error.indexOf("Permission denied") !== -1) {
                console.log("提示: 请先请求悬浮窗权限")
            }
        }
    )
}

// 使用示例：
// 显示本地图片（相对于 www 目录）
showFloatingWindow("img/icon.png", 150, 150)

// 显示网络图片
showFloatingWindow("https://example.com/image.png", 200, 200)
```

### 示例方法 4 - 隐藏悬浮窗

```javascript
function hideFloatingWindow() {
    if (!window.FloatingWindow) {
        console.error("错误: 插件未加载")
        return
    }

    window.FloatingWindow.hideFloatingWindow(
        function () {
            console.log("✓ 悬浮窗已隐藏")
        },
        function (error) {
            console.error("✗ 隐藏悬浮窗失败: " + error)
        }
    )
}
```

### 示例方法 5 - 启动后台服务

```javascript
function startService() {
    if (!window.FloatingWindow) {
        console.error("错误: 插件未加载")
        return
    }

    window.FloatingWindow.startService(
        function () {
            console.log("✓ 后台服务已启动")
            // 服务启动后，悬浮窗可以在后台继续显示
        },
        function (error) {
            console.error("✗ 启动服务失败: " + error)
        }
    )
}
```

### 示例方法 6 - 停止后台服务

```javascript
function stopService() {
    if (!window.FloatingWindow) {
        console.error("错误: 插件未加载")
        return
    }

    window.FloatingWindow.stopService(
        function () {
            console.log("✓ 后台服务已停止")
            // 服务停止后，悬浮窗也会被隐藏
        },
        function (error) {
            console.error("✗ 停止服务失败: " + error)
        }
    )
}
```

### 示例方法 7 - 跳转到桌面

```javascript
function goToHome() {
    if (!window.FloatingWindow) {
        console.error("错误: 插件未加载")
        return
    }

    window.FloatingWindow.goToHome(
        function () {
            console.log("✓ 已跳转到桌面")
        },
        function (error) {
            console.error("✗ 跳转失败: " + error)
        }
    )
}
```

### 完整使用流程示例

```javascript
document.addEventListener(
    "deviceready",
    function () {
        // 1. 设置事件监听器
        window.FloatingWindow.setEventListener(function (event) {
            console.log("事件:", event.type)
        })

        // 2. 检查权限
        window.FloatingWindow.checkPermission(function (hasPermission) {
            if (hasPermission === 0) {
                // 3. 如果没有权限，请求权限
                window.FloatingWindow.requestPermission(
                    function () {
                        // 4. 权限获取成功，初始化悬浮窗
                        initFloatingWindow()
                    },
                    function (error) {
                        if (error === "Permission denied permanently") {
                            console.log("权限被永久拒绝，请前往系统设置手动开启")
                        } else {
                            console.log("权限请求失败:", error)
                        }
                    }
                )
            } else {
                // 5. 已有权限，直接初始化悬浮窗
                initFloatingWindow()
            }
        })

        function initFloatingWindow() {
            // 6. 启动后台服务
            window.FloatingWindow.startService(function () {
                console.log("服务已启动")

                // 7. 显示悬浮窗（图片路径相对于 www 目录）
                window.FloatingWindow.showFloatingWindow("img/icon.png", 150, 150, function () {
                    console.log("悬浮窗已显示")
                })
            })
        }
    },
    false
)
```

### 带 UI 交互的完整示例

```javascript
// HTML 部分
// <input type="text" id="imagePath" value="img/icon.png" placeholder="输入图片路径或URL">
// <input type="number" id="width" value="150" min="50" max="500">
// <input type="number" id="height" value="150" min="50" max="500">
// <button onclick="showFloatingWindow()">显示悬浮窗</button>
// <button onclick="hideFloatingWindow()">隐藏悬浮窗</button>

var logElement = document.getElementById("log")
var statusElement = document.getElementById("status")

function log(message) {
    var time = new Date().toLocaleTimeString()
    logElement.innerHTML += "[" + time + "] " + message + "<br>"
    logElement.scrollTop = logElement.scrollHeight
    console.log(message)
}

function updateStatus(message) {
    statusElement.textContent = message
}

document.addEventListener(
    "deviceready",
    function () {
        updateStatus("设备已就绪")
        log("设备已就绪")

        // 设置事件监听器
        if (window.FloatingWindow) {
            window.FloatingWindow.setEventListener(function (event) {
                log("事件: " + event.type + " - " + JSON.stringify(event.data))
            })
            log("事件监听器已设置")
        } else {
            log("错误: FloatingWindow 插件未找到")
        }
    },
    false
)

function checkPermission() {
    if (!window.FloatingWindow) {
        log("错误: 插件未加载")
        return
    }

    window.FloatingWindow.checkPermission(
        function (hasPermission) {
            if (hasPermission === 1) {
                log("✓ 已有悬浮窗权限")
                updateStatus("已有悬浮窗权限")
            } else {
                log("✗ 没有悬浮窗权限")
                updateStatus('没有悬浮窗权限，请点击"请求权限"')
            }
        },
        function (error) {
            log("检查权限失败: " + error)
        }
    )
}

function requestPermission() {
    if (!window.FloatingWindow) {
        log("错误: 插件未加载")
        return
    }

    window.FloatingWindow.requestPermission(
        function () {
            log("✓ 权限已授予")
            updateStatus("权限已授予")
        },
        function (error) {
            if (error === "Permission denied permanently") {
                log("✗ 权限被永久拒绝，请前往系统设置手动开启")
                updateStatus("权限被永久拒绝，请前往系统设置")
            } else {
                log("✗ 权限请求失败: " + error)
                updateStatus("权限请求失败")
            }
        }
    )
}

function showFloatingWindow() {
    if (!window.FloatingWindow) {
        log("错误: 插件未加载")
        return
    }

    var imagePath = document.getElementById("imagePath").value
    var width = parseInt(document.getElementById("width").value) || 150
    var height = parseInt(document.getElementById("height").value) || 150

    log("显示悬浮窗: " + imagePath + " (" + width + "x" + height + ")")

    window.FloatingWindow.showFloatingWindow(
        imagePath,
        width,
        height,
        function () {
            log("✓ 悬浮窗已显示")
            updateStatus("悬浮窗已显示")
        },
        function (error) {
            log("✗ 显示悬浮窗失败: " + error)
            updateStatus("显示失败: " + error)
        }
    )
}

function hideFloatingWindow() {
    if (!window.FloatingWindow) {
        log("错误: 插件未加载")
        return
    }

    window.FloatingWindow.hideFloatingWindow(
        function () {
            log("✓ 悬浮窗已隐藏")
            updateStatus("悬浮窗已隐藏")
        },
        function (error) {
            log("✗ 隐藏悬浮窗失败: " + error)
        }
    )
}

function startService() {
    if (!window.FloatingWindow) {
        log("错误: 插件未加载")
        return
    }

    window.FloatingWindow.startService(
        function () {
            log("✓ 后台服务已启动")
            updateStatus("后台服务运行中")
        },
        function (error) {
            log("✗ 启动服务失败: " + error)
        }
    )
}

function stopService() {
    if (!window.FloatingWindow) {
        log("错误: 插件未加载")
        return
    }

    window.FloatingWindow.stopService(
        function () {
            log("✓ 后台服务已停止")
            updateStatus("后台服务已停止")
        },
        function (error) {
            log("✗ 停止服务失败: " + error)
        }
    )
}

function goToHome() {
    if (!window.FloatingWindow) {
        log("错误: 插件未加载")
        return
    }

    window.FloatingWindow.goToHome(
        function () {
            log("✓ 已跳转到桌面")
        },
        function (error) {
            log("✗ 跳转失败: " + error)
        }
    )
}
```

## 许可证

Apache-2.0

## 作者

Created for Cordova/PhoneGap applications.
