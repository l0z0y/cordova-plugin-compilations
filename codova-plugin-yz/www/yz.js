/**
 * YZ 物联网 Cordova JavaScript 接口。
 * 仅支持 Android，通过 Cordova exec 调用原生层。
 */
var exec = require('cordova/exec');

/**
 * 调用原生 YZ 插件。
 * @param {String} action 原生方法名。
 * @param {Array} args 传递给原生层的参数。
 * @param {Function} success 成功回调。
 * @param {Function} error 失败回调。
 */
function call(action, args, success, error) {
    exec(success, error, 'YZ', action, args || []);
}

var api = {};

/**
 * 无参数方法列表：用于生成设备信息、系统信息、存储、显示、音量和亮度等查询方法。
 */
[
    'getDeviceVersion', 'getAndroidVersion', 'getIccids', 'getJARVersion', 'getDeviceID',
    'getSDKVersion', 'getTotalInternalMemorySize', 'getFreeMemorySize', 'getInternalSDCardPath',
    'getExternalSDCardPath', 'getUsbStoragePath', 'getSystemDate', 'getSystemTime',
    'getDisplayMode', 'getDisplayHeight', 'getDisplayWidth', 'getDisplayDensity',
    'getSystemMaxVolume', 'getSystemCurrenVolume', 'setRaiseSystemVolume',
    'setLowerSystemVolume', 'getSystemMaxBrightness', 'getSystemBrightness',
    'isAutoDateTime'
].forEach(function (action) {
    api[action] = function (success, error) {
        call(action, [], success, error);
    };
});

/**
 * 判断指定包名的应用是否已经安装。
 * @param {String} pkg 要检查的 Android 应用包名。
 * @param {Function} success 成功回调，参数为 Boolean，true 表示已安装，false 表示未安装。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.isAppExist = function (pkg, success, error) {
    call('isAppExist', [pkg], success, error);
};

/**
 * 判断指定路径的文件或目录是否存在。
 * @param {String} path 要检查的文件或目录路径。
 * @param {Function} success 成功回调，参数为 Boolean。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.isExist = function (path, success, error) {
    call('isExist', [path], success, error);
};

/**
 * 一次性读取 GPIO 当前值。
 * @param {String} port GPIO 端口名称，例如 GPIO_A5。
 * @param {Function} success 成功回调，参数为 Number，通常是 0 或 1，读取失败时可能为 -1。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.getGpioValue = function (port, success, error) {
    call('getGpioValue', [port], success, error);
};

/**
 * 创建 GPIO 值观察对象。
 * 原生层为每个 GPIO 创建专用轮询线程；首次读取到有效值时通知一次，
 * 后续只有值发生变化时才通知。通知值只有数字 0 或 1。
 * @param {String} port GPIO 端口名称，例如 GPIO_A5。
 * @param {Object} [options] 可选配置对象。
 * @param {Number} [options.interval=100] 轮询间隔，单位为毫秒，最小值为 20。
 * @returns {Object} GPIO 观察对象，包含 subscribe 方法。
 */
api.observeGpioValue = function (port, options) {
    options = options || {};
    var interval = Math.max(20, Number(options.interval) || 100);
    var subscribers = [];
    var started = false;
    var stopped = false;

    /**
     * 订阅 GPIO 值变化。
     * @param {Function} next 值变化回调，参数为 Number，只会收到 0 或 1。
     * @param {Function} [error] 错误回调，参数为错误信息。
     * @returns {Object} 订阅对象，包含 unsubscribe 方法。
     */
    function subscribe(next, error) {
        if (typeof next !== 'function') {
            throw new TypeError('subscribe requires a callback');
        }
        var subscriber = {
            next: next,
            error: typeof error === 'function' ? error : function () {}
        };
        subscribers.push(subscriber);

        // 第一个订阅者到来时启动原生轮询；多个订阅者共用一个轮询线程。
        if (!started) {
            started = true;
            call('observeGpioValue', [port, interval], function (value) {
                if (stopped) return;
                subscribers.slice().forEach(function (item) {
                    item.next(Number(value));
                });
            }, function (message) {
                if (stopped) return;
                subscribers.slice().forEach(function (item) {
                    item.error(message);
                });
            });
        }

        return {
            /** 取消当前订阅；最后一个订阅取消后，原生轮询线程也会停止。 */
            unsubscribe: function () {
                var index = subscribers.indexOf(subscriber);
                if (index !== -1) subscribers.splice(index, 1);
                if (!subscribers.length && !stopped) {
                    stopped = true;
                    call('stopObservingGpioValue', [port], function () {}, function () {});
                }
            }
        };
    }

    return { subscribe: subscribe };
};

/**
 * 查询指定网卡的 IPv4 地址。
 * @param {String} nettype 网卡名称，例如 eth0、wlan0 或 ppp0。
 * @param {Function} success 成功回调，参数为 String 类型的 IPv4 地址。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.getIPv4 = function (nettype, success, error) {
    call('getIPv4', [nettype], success, error);
};

/**
 * 关闭 Android 设备。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.shutdown = function (success, error) {
    call('shutdown', [], success, error);
};

/**
 * 重启 Android 设备。
 * @param {Number} delay 重启前的延迟时间，单位为秒，默认 0。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.reboot = function (delay, success, error) {
    call('reboot', [delay || 0], success, error);
};

/**
 * 打开 Android 系统设置页面。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.startSettings = function (success, error) {
    call('startSettings', [], success, error);
};

/**
 * 打开 Android Wi-Fi 设置页面。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.startWifiSettings = function (success, error) {
    call('startWifiSettings', [], success, error);
};

/**
 * 设置 GPIO 输出电平。
 * @param {String} port GPIO 端口名称，例如 GPIO_A5。
 * @param {Number} value 输出值，只支持 0（低电平）或 1（高电平）。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setGpioValue = function (port, value, success, error) {
    call('setGpioValue', [port, value], success, error);
};

/**
 * 启动指定应用的 Activity。
 * @param {String} pkg Android 应用包名。
 * @param {String} activity Activity 完整类名。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.startActivity = function (pkg, activity, success, error) {
    call('startActivity', [pkg, activity], success, error);
};

/**
 * 使用 Root 权限执行系统命令。
 * @param {String} cmd 要执行的系统命令。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.execSuCmd = function (cmd, success, error) {
    call('execSuCmd', [cmd], success, error);
};

/**
 * 静默安装 APK。
 * @param {String} path APK 文件的绝对路径。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.installAppSilent = function (path, success, error) {
    call('installAppSilent', [path], success, error);
};

/**
 * 静默卸载应用。
 * @param {String} pkg 要卸载的 Android 应用包名。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.uninstallAppSilent = function (pkg, success, error) {
    call('uninstallAppSilent', [pkg], success, error);
};

/**
 * 静默安装 APK，并在安装后重启设备。
 * @param {String} path APK 文件的绝对路径。
 * @param {Number} seconds 安装完成后等待多少秒再重启。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.installAppSilentAndRebootSystem = function (path, seconds, success, error) {
    call('installAppSilentAndRebootSystem', [path, seconds], success, error);
};

/**
 * 安装 APK，并启动安装后的应用。
 * @param {String} path APK 文件的绝对路径。
 * @param {String} pkg 安装后的应用包名。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.installAppAndStartUp = function (path, pkg, success, error) {
    call('installAppAndStartUp', [path, pkg], success, error);
};

/**
 * 设置静态 IP，或切换到 DHCP 自动获取 IP。
 * @param {String} mode 模式：static 表示静态 IP，dhcp 表示自动获取。
 * @param {String} ip IP 地址。
 * @param {String} prefix 子网前缀长度，例如 24。
 * @param {String} gateway 网关地址。
 * @param {String} dns DNS 地址。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setStaticIP = function (mode, ip, prefix, gateway, dns, success, error) {
    call('setStaticIP', [mode, ip, prefix, gateway, dns], success, error);
};

/**
 * 设置屏幕旋转方向。
 * @param {Number} rotation 旋转值：0、1、2、3，分别表示不同方向。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setRotation = function (rotation, success, error) {
    call('setRotation', [rotation], success, error);
};

/**
 * 模拟 Android 按键输入。
 * @param {Number} keyCode Android 按键编码，例如 Home=3、返回=4、电源=26。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.inputkeyevent = function (keyCode, success, error) {
    call('inputkeyevent', [keyCode], success, error);
};

/**
 * 设置屏幕显示密度。
 * @param {Number} dpi 显示密度，单位为 DPI。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setDisplayDensity = function (dpi, success, error) {
    call('setDisplayDensity', [dpi], success, error);
};

/**
 * 设置系统媒体音量。
 * @param {Number} index 音量索引，范围取决于设备的最大音量。
 * @param {Function} success 成功回调，参数通常为 Number，0 表示成功，-1 表示失败。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setSystemVolumeIndex = function (index, success, error) {
    call('setSystemVolumeIndex', [index], success, error);
};

/**
 * 将系统亮度增加一个单位。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.increaseBrightness = function (success, error) {
    call('increaseBrightness', [], success, error);
};

/**
 * 将系统亮度减少一个单位。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.decreaseBrightness = function (success, error) {
    call('decreaseBrightness', [], success, error);
};

/**
 * 设置系统亮度。
 * @param {Number} value 亮度值，通常范围为 0 到系统最大亮度。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setBrightness = function (value, success, error) {
    call('setBrightness', [value], success, error);
};

/**
 * 设置系统默认桌面应用。
 * @param {String} pkg 默认桌面应用包名。
 * @param {String} activity 默认桌面 Activity 完整类名。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setDefaultLauncher = function (pkg, activity, success, error) {
    call('setDefaultLauncher', [pkg, activity], success, error);
};

/**
 * 开启或关闭 TCP ADB。
 * @param {Boolean} enabled true 表示开启，false 表示关闭。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.startTcpAdb = function (enabled, success, error) {
    call('startTcpAdb', [enabled], success, error);
};

/**
 * 设置系统时间。
 * @param {Date|Number} dateOrMillis Date 对象或 Unix 毫秒时间戳。
 * @param {Function} success 成功回调，无返回值。
 * @param {Function} error 失败回调，参数为错误信息。
 */
api.setSystemTime = function (dateOrMillis, success, error) {
    var millis = dateOrMillis instanceof Date ? dateOrMillis.getTime() : dateOrMillis;
    call('setSystemTime', [millis], success, error);
};

// plugin.xml 会将本模块暴露为 window.yz 和 cordova.plugins.yz。
module.exports = api;
