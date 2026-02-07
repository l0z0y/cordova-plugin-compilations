var exec = require('cordova/exec');

var yUtils = {

};


yUtils.coolMethod = function (arg0, success, error) {
    exec(success, error, 'cordovaYUtils', 'coolMethod', [arg0]);
};

yUtils.executeCmd = function (arg0, success, error) {
    exec(success, error, 'cordovaYUtils', 'executeCmd', [arg0]);
};

yUtils.writeFileWithGBK = function (filePath, content, success, error) {
    exec(success, error, 'cordovaYUtils', 'writeFileWithGBK', [filePath, content]);
};

module.exports = yUtils;