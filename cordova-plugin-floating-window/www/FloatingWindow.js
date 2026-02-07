var exec = require('cordova/exec');

var FloatingWindow = {
    /**
     * 启动后台服务
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    startService: function(success, error) {
        exec(success, error, 'FloatingWindow', 'startService', []);
    },

    /**
     * 停止后台服务
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    stopService: function(success, error) {
        exec(success, error, 'FloatingWindow', 'stopService', []);
    },

    /**
     * 显示悬浮窗
     * @param {String} imagePath - 图片路径（可以是本地路径或网络URL）
     * @param {Number} width - 悬浮窗宽度（可选，默认100）
     * @param {Number} height - 悬浮窗高度（可选，默认100）
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    showFloatingWindow: function(imagePath, width, height, success, error) {
        width = width || 100;
        height = height || 100;
        exec(success, error, 'FloatingWindow', 'showFloatingWindow', [imagePath, width, height]);
    },

    /**
     * 隐藏悬浮窗
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    hideFloatingWindow: function(success, error) {
        exec(success, error, 'FloatingWindow', 'hideFloatingWindow', []);
    },

    /**
     * 跳转到桌面
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    goToHome: function(success, error) {
        exec(success, error, 'FloatingWindow', 'goToHome', []);
    },

    /**
     * 检查悬浮窗权限
     * @param {Function} success - 成功回调，返回是否有权限
     * @param {Function} error - 错误回调
     */
    checkPermission: function(success, error) {
        exec(success, error, 'FloatingWindow', 'checkPermission', []);
    },

    /**
     * 请求悬浮窗权限
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    requestPermission: function(success, error) {
        exec(success, error, 'FloatingWindow', 'requestPermission', []);
    },

    /**
     * 设置事件监听器
     * @param {Function} callback - 事件回调函数，参数为事件类型和事件数据
     */
    setEventListener: function(callback) {
        exec(callback, null, 'FloatingWindow', 'setEventListener', []);
    }
};

module.exports = FloatingWindow;
