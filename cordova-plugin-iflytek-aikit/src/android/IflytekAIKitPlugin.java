package com.iflytek.aikit.plugin;

import android.util.Log;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

/**
 * 讯飞AIKit Cordova插件主类
 * 负责处理来自JavaScript的调用请求
 */
public class IflytekAIKitPlugin extends CordovaPlugin {

    private static final String TAG = "IflytekAIKitPlugin";

    private IflytekAIKit aiKit;
    private CallbackContext initCallbackContext;
    private JSONArray initArgs;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            if (aiKit == null) {
                aiKit = new IflytekAIKit(cordova.getActivity());
            }

            switch (action) {
                case "init":
                    init(args, callbackContext);
                    return true;

                case "unInit":
                    unInit(callbackContext);
                    return true;

                case "startWakeUp":
                    startWakeUp(args, callbackContext);
                    return true;

                case "stopWakeUp":
                    stopWakeUp(callbackContext);
                    return true;

                case "startTTS":
                    startTTS(args, callbackContext);
                    return true;

                case "startXTTS":
                    startXTTS(args, callbackContext);
                    return true;

                case "startCommandRecognition":
                    startCommandRecognition(args, callbackContext);
                    return true;

                case "stopCommandRecognition":
                    stopCommandRecognition(callbackContext);
                    return true;

                case "writeAudioData":
                    writeAudioData(args, callbackContext);
                    return true;

                case "registerListener":
                    registerListener(callbackContext);
                    return true;

                case "playAudio":
                    playAudio(args, callbackContext);
                    return true;

                case "stopPlayAudio":
                    stopPlayAudio(callbackContext);
                    return true;

                default:
                    Log.e(TAG, "Unknown action: " + action);
                    callbackContext.error("Unknown action: " + action);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Execute error: " + e.getMessage(), e);
            callbackContext.error("Execute error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 初始化SDK
     */
    private void init(JSONArray args, CallbackContext callbackContext) throws JSONException {
        // 检查参数
        if (args == null || args.length() == 0) {
            Log.e(TAG, "初始化参数为空");
            callbackContext.error("初始化参数为空");
            return;
        }

        // 保存参数和回调，用于权限获取后继续初始化
        this.initArgs = args;
        this.initCallbackContext = callbackContext;

        Log.d(TAG, "收到初始化请求，参数数量: " + args.length());

        // 先请求权限
        requestPermissions();
    }

    /**
     * 请求必要的权限（使用XXPermissions库，与原生代码保持一致）
     */
    private void requestPermissions() {
        /**
         * 获取相关权限。
         * 本地存储的读写权限:用于SDK从workdir中读取资源文件或者测试音频，日志存放到workdir以及引擎运行结果的保存目录
         * 网络权限:首次运行鉴权时需要联网拉去协议，鉴权成功后，可在无网环境下运行，此时则可以不需要网络权限
         */
        XXPermissions.with(cordova.getActivity())
                .permission("android.permission.WRITE_EXTERNAL_STORAGE")
                .permission("android.permission.READ_EXTERNAL_STORAGE")
                .permission("android.permission.INTERNET")
                .permission("android.permission.MANAGE_EXTERNAL_STORAGE")
                .permission("android.permission.RECORD_AUDIO")
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean all) {
                        Log.d(TAG, "SDK获取系统权限成功:" + all);
                        for (int i = 0; i < granted.size(); i++) {
                            Log.d(TAG, "获取到的权限有：" + granted.get(i));
                        }
                        if (all) {
                            // 所有权限都已授予，开始初始化SDK
                            Log.d(TAG, "所有权限已授予，开始初始化SDK");
                            doInit();
                        } else {
                            // 部分权限被拒绝
                            String errorMsg = "部分权限获取失败，请确保授予所有必要权限";
                            Log.e(TAG, errorMsg);
                            if (initCallbackContext != null) {
                                initCallbackContext.error(errorMsg);
                            }
                            // 清理状态
                            initCallbackContext = null;
                            initArgs = null;
                        }
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        if (quick) {
                            Log.e(TAG, "onDenied:被永久拒绝授权，请手动授予权限");
                            // 跳转到权限设置页面
                            XXPermissions.startPermissionActivity(cordova.getActivity(), denied);
                            String errorMsg = "权限被永久拒绝，请手动授予权限";
                            if (initCallbackContext != null) {
                                initCallbackContext.error(errorMsg);
                            }
                        } else {
                            Log.e(TAG, "onDenied:权限获取失败");
                            String errorMsg = "权限获取失败，请授予必要权限";
                            if (initCallbackContext != null) {
                                initCallbackContext.error(errorMsg);
                            }
                        }
                        // 清理状态
                        initCallbackContext = null;
                        initArgs = null;
                    }
                });
    }

    /**
     * 执行实际的初始化操作
     */
    private void doInit() {
        if (initArgs == null || initCallbackContext == null) {
            Log.e(TAG, "初始化参数或回调为空");
            return;
        }

        try {
            // 检查参数数组长度
            if (initArgs.length() == 0) {
                Log.e(TAG, "初始化参数数组为空");
                initCallbackContext.error("初始化参数数组为空");
                return;
            }

            // 获取配置对象
            JSONObject config = initArgs.getJSONObject(0);
            if (config == null) {
                Log.e(TAG, "配置对象为null");
                initCallbackContext.error("配置对象为null");
                return;
            }

            Log.d(TAG, "解析配置参数，config keys: " + config.toString());

            String appId = config.getString("appId");
            String apiKey = config.getString("apiKey");
            String apiSecret = config.getString("apiSecret");
            String workDir = config.optString("workDir", "/sdcard/iflytek/");
            String abilities = config.optString("abilities", "e867a88f2;ece9d3c90;e75f07b62");

            final CallbackContext callback = initCallbackContext;

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        aiKit.init(appId, apiKey, apiSecret, workDir, abilities, new IflytekAIKit.ResultCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                callback.success(result != null ? result.toString() : "");
                            }

                            @Override
                            public void onError(String error) {
                                callback.error(error);
                            }
                        });
                    } catch (Exception e) {
                        callback.error("Init error: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "解析配置参数失败: " + e.getMessage());
            if (initCallbackContext != null) {
                initCallbackContext.error("配置参数错误: " + e.getMessage());
            }
        } finally {
            // 清理状态
            initCallbackContext = null;
            initArgs = null;
        }
    }

    /**
     * 反初始化SDK
     */
    private void unInit(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    aiKit.unInit();
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error("UnInit error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 开始语音唤醒
     */
    private void startWakeUp(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String keywords = options.getString("keywords");

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.startWakeUp(keywords, new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 停止语音唤醒
     */
    private void stopWakeUp(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.stopWakeUp(new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 开始语音合成（轻量版）
     */
    private void startTTS(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String text = options.getString("text");
        String vcn = options.optString("vcn", "xiaoyan");
        int speed = options.optInt("speed", 50);
        int pitch = options.optInt("pitch", 50);
        int volume = options.optInt("volume", 50);
        String fileName = options.optString("fileName", "");

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.startTTS(text, vcn, speed, pitch, volume, fileName, new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 开始语音合成（XTTS版本）
     */
    private void startXTTS(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String text = options.getString("text");
        String vcn = options.optString("vcn", "xiaoyan");
        int language = options.optInt("language", 1);
        int speed = options.optInt("speed", 50);
        int pitch = options.optInt("pitch", 50);
        int volume = options.optInt("volume", 50);

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.startXTTS(text, vcn, language, speed, pitch, volume, new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 开始命令词识别
     */
    private void startCommandRecognition(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String fsaPath = options.getString("fsaPath");
        int languageType = options.optInt("languageType", 0);
        // 超时时间（秒），默认10秒，0表示不超时
        int timeoutSeconds = options.optInt("timeoutSeconds", 10);

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.startCommandRecognition(fsaPath, languageType, timeoutSeconds, new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 停止命令词识别
     */
    private void stopCommandRecognition(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.stopCommandRecognition(new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 写入音频数据
     */
    private void writeAudioData(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String audioDataBase64 = options.getString("audioData");
        String type = options.getString("type");
        boolean isEnd = options.optBoolean("isEnd", false);

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.writeAudioData(audioDataBase64, type, isEnd, new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 注册监听器
     */
    private void registerListener(CallbackContext callbackContext) {
        aiKit.registerListener(new IflytekAIKit.EventCallback() {
            @Override
            public void onEvent(String event, JSONObject data) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("event", event != null ? event : "");
                    // 如果data为null，创建一个空的JSONObject
                    if (data != null) {
                        result.put("data", data);
                    } else {
                        result.put("data", new JSONObject());
                    }
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                } catch (JSONException e) {
                    Log.e(TAG, "Error sending event: " + e.getMessage());
                }
            }
        });
        // 不要调用 success()，而是发送一个初始结果并保持回调
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "监听器已注册");
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    /**
     * 播放音频文件
     */
    private void playAudio(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String filePath = options.getString("filePath");
        String sampleRate = options.optString("sampleRate", "16k");

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.playAudio(filePath, sampleRate, new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    /**
     * 停止播放音频
     */
    private void stopPlayAudio(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                aiKit.stopPlayAudio(new IflytekAIKit.ResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callbackContext.success(result != null ? result.toString() : "");
                    }

                    @Override
                    public void onError(String error) {
                        callbackContext.error(error);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aiKit != null) {
            aiKit.unInit();
        }
    }
}
