var exec = require('cordova/exec');

module.exports = {
  start: function (apSsid, apPassword, deviceCountData, successCallback, failCallback) {
    exec(successCallback, failCallback, "esptouch", "start", [apSsid,  apPassword, deviceCountData]);
  },
  stop: function (successCallback, failCallback) {
    exec(successCallback, failCallback, "esptouch", "stop", []);
  }
}
