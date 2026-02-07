/**
 * 讯飞AIKit插件JavaScript接口
 * 提供语音唤醒、语音合成、命令词识别等功能
 */

var exec = require('cordova/exec');

/**
 * 讯飞AIKit插件主类
 */
var IflytekAIKit = {
    /**
     * 初始化SDK
     * @param {Object} config 配置对象
     * @param {string} config.appId 应用ID
     * @param {string} config.apiKey API密钥
     * @param {string} config.apiSecret API密钥
     * @param {string} config.workDir 工作目录，默认为 /sdcard/iflytek/
     * @param {string} config.abilities 能力ID列表，用分号分隔，如 "e867a88f2;ece9d3c90;e75f07b62"
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    init: function (config, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'init', [config]);
    },

    /**
     * 反初始化SDK
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    unInit: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'unInit', []);
    },

    /**
     * 开始语音唤醒
     * @param {Object} options 唤醒配置
     * @param {string} options.keywords 唤醒词，多个用逗号分隔，如 "你好小迪,小迪小迪"
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    startWakeUp: function (options, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'startWakeUp', [options]);
    },

    /**
     * 停止语音唤醒
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    stopWakeUp: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'stopWakeUp', []);
    },

    /**
     * 开始语音合成（轻量版Aisound）
     * @param {Object} options 合成配置
     * @param {string} options.text 要合成的文本
     * @param {string} options.vcn 发音人，可选值: xiaoyan(晓燕), xiaofeng(晓峰), catherine(英文)
     * @param {number} options.speed 语速 0-100，默认50
     * @param {number} options.pitch 语调 0-100，默认50
     * @param {number} options.volume 音量 0-100，默认50
     * @param {string} options.fileName 预设文件名（可选），如 "hello.pcm"，指定后可避免重复合成
     * @param {Function} successCallback 成功回调，返回音频文件路径
     * @param {Function} errorCallback 失败回调
     */
    startTTS: function (options, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'startTTS', [options]);
    },

    /**
     * 开始语音合成（XTTS版本）
     * @param {Object} options 合成配置
     * @param {string} options.text 要合成的文本
     * @param {string} options.vcn 发音人
     * @param {number} options.language 语种 1:中文, 2:英文, 3:法语等
     * @param {number} options.speed 语速 0-100
     * @param {number} options.pitch 语调 0-100
     * @param {number} options.volume 音量 0-100
     * @param {Function} successCallback 成功回调，返回音频文件路径
     * @param {Function} errorCallback 失败回调
     */
    startXTTS: function (options, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'startXTTS', [options]);
    },

    /**
     * 开始命令词识别
     * @param {Object} options 识别配置
     * @param {string} options.fsaPath FSA命令词文件路径
     * @param {number} options.languageType 语种 0:中文, 1:英文
     * @param {number} options.timeoutSeconds 超时时间（秒），默认10秒，0表示不超时
     * @param {Function} successCallback 成功回调，返回识别结果
     * @param {Function} errorCallback 失败回调
     */
    startCommandRecognition: function (options, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'startCommandRecognition', [options]);
    },

    /**
     * 停止命令词识别
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    stopCommandRecognition: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'stopCommandRecognition', []);
    },

    /**
     * 写入音频数据（用于唤醒或命令词识别）
     * @param {Object} options 音频数据配置
     * @param {string} options.audioData Base64编码的音频数据
     * @param {string} options.type 类型: 'wakeup' 或 'command'
     * @param {boolean} options.isEnd 是否为最后一帧
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    writeAudioData: function (options, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'writeAudioData', [options]);
    },

    /**
     * 注册监听器
     * @param {Function} callback 回调函数，接收事件通知
     */
    registerListener: function (callback) {
        exec(callback, null, 'IflytekAIKit', 'registerListener', []);
    },

    /**
     * 播放音频文件
     * @param {Object} options 播放配置
     * @param {string} options.filePath 音频文件路径（PCM格式）
     * @param {string} options.sampleRate 采样率类型：'16k' 或 '24k'，默认 '16k'
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    playAudio: function (options, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'playAudio', [options]);
    },

    /**
     * 停止播放音频
     * @param {Function} successCallback 成功回调
     * @param {Function} errorCallback 失败回调
     */
    stopPlayAudio: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'IflytekAIKit', 'stopPlayAudio', []);
    }
};

module.exports = IflytekAIKit;

