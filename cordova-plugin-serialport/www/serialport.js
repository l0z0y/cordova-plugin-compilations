var exec = require('cordova/exec');

module.exports = {
    init: function (success, error, options) {
        options = options || {};
        var intervalSleep = options.intervalSleep != null ? options.intervalSleep : 50;
        var enableLog = options.enableLog != null ? options.enableLog : false;
        var logTag = options.logTag != null ? options.logTag : "SerialPort";
        var databits = options.databits != null ? options.databits : 8;
        var parity = options.parity != null ? options.parity : 0;
        var stopbits = options.stopbits != null ? options.stopbits : 1;
        var strategy = options.strategy != null ? options.strategy : 0;

        exec(success, error, "SerialPort", "init", [intervalSleep, enableLog, logTag, databits, parity, stopbits, strategy]);
    },
    open: function (port, baudRate, success, error) {
        exec(success, error, "SerialPort", "open", [port, baudRate]);
    },
    send: function (data, success, error) {
        exec(success, error, "SerialPort", "send", [data]);
    },
    close: function (success, error) {
        exec(success, error, "SerialPort", "close", []);
    }
};