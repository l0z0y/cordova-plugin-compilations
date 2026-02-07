var exec = require('cordova/exec');

module.exports = {
    /**
     * 初始化并打开串口
     * @param {String} port - 串口路径，例如 "/dev/ttyS4"
     * @param {Number} baudRate - 波特率，例如 115200
     * @param {Object} options - 可选配置参数
     * @param {Number} options.intervalSleep - 轮询间隔(ms)，默认 50
     * @param {Boolean} options.enableLog - 是否开启底层日志，默认 false
     * @param {String} options.logTag - 日志标签，默认 "SerialPort"
     * @param {Number} options.databits - 数据位：5/6/7/8，默认 8
     * @param {Number} options.parity - 校验位：0-None 1-Odd 2-Even 3-Mark 4-Space，默认 0
     * @param {Number} options.stopbits - 停止位：1/2，默认 1
     * @param {Number} options.strategy - 黏包处理策略索引，默认 0
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    init: function (port, baudRate, options, success, error) {
        options = options || {};
        var intervalSleep = options.intervalSleep != null ? options.intervalSleep : 50;
        var enableLog = options.enableLog != null ? options.enableLog : false;
        var logTag = options.logTag != null ? options.logTag : "SerialPort";
        var databits = options.databits != null ? options.databits : 8;
        var parity = options.parity != null ? options.parity : 0;
        var stopbits = options.stopbits != null ? options.stopbits : 1;
        var strategy = options.strategy != null ? options.strategy : 0;

        exec(success, error, "SerialPort", "init", [port, baudRate, intervalSleep, enableLog, logTag, databits, parity, stopbits, strategy]);
    },
    /**
     * 设置数据接收监听器
     * @param {Function} success - 数据接收回调函数，参数为十六进制字符串
     * @param {Function} error - 错误回调
     */
    listen: function (success, error) {
        exec(success, error, "SerialPort", "listen", []);
    },
    /**
     * 发送数据
     * @param {String} data - 要发送的数据
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    send: function (data, success, error) {
        exec(success, error, "SerialPort", "send", [data]);
    },
    /**
     * 关闭串口
     * @param {Function} success - 成功回调
     * @param {Function} error - 错误回调
     */
    close: function (success, error) {
        exec(success, error, "SerialPort", "close", []);
    }
};