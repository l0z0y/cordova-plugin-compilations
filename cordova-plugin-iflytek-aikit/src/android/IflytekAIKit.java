package com.iflytek.aikit.plugin;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.BaseLibrary;
import com.iflytek.aikit.core.CoreListener;
import com.iflytek.aikit.core.ErrType;
import com.iflytek.aikit.core.LogLvl;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 讯飞AIKit核心封装类
 * 作为主控制器和事件管理器，统一管理各个功能模块
 */
public class IflytekAIKit {

    private static final String TAG = "IflytekAIKit";

    private Activity activity;
    private Handler mainHandler;
    private boolean isInitialized = false;

    // 各个功能管理器
    private TTSManager ttsManager;
    private WakeUpManager wakeUpManager;
    private CommandRecognitionManager commandManager;

    // 工作目录
    private String workDir;
    private String wakeUpWorkDir;
    private String ttsOutputDir;

    // 回调接口
    public interface ResultCallback {
        void onSuccess(Object result);

        void onError(String error);
    }

    public interface EventCallback {
        void onEvent(String event, JSONObject data);
    }

    private EventCallback eventCallback;

    public IflytekAIKit(Activity activity) {
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 初始化SDK
     */
    public void init(String appId, String apiKey, String apiSecret, String workDir,
            String abilities, ResultCallback callback) {
        if (isInitialized) {
            callback.onSuccess("SDK已经初始化");
            return;
        }

        try {
            // 在后台线程执行资源文件复制和SDK初始化
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 确保工作目录存在且有权限
                        String finalWorkDir = ensureWorkDirExists(workDir);
                        if (finalWorkDir == null) {
                            callback.onError("工作目录创建失败，请检查存储权限");
                            return;
                        }

                        // 保存工作目录
                        IflytekAIKit.this.workDir = finalWorkDir;
                        wakeUpWorkDir = finalWorkDir + "ivw";
                        ttsOutputDir = finalWorkDir + "aisound/output";

                        // 检测并复制资源文件到工作目录
                        copyResourcesIfNeeded(finalWorkDir);

                        // 设置日志
                        String logDir = finalWorkDir + "aikit";
                        new File(logDir).mkdirs();
                        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, logDir + File.separator + "aeeLog.txt");

                        // 构建初始化参数
                        BaseLibrary.Params params = BaseLibrary.Params.builder()
                                .appId(appId)
                                .apiKey(apiKey)
                                .apiSecret(apiSecret)
                                .workDir(finalWorkDir)
                                .ability(abilities)
                                .build();

                        // 注册核心监听器
                        AiHelper.getInst().registerListener(new CoreListener() {
                            @Override
                            public void onAuthStateChange(ErrType type, int code) {
                                Log.i(TAG, "Auth state change: " + type + ", code: " + code);
                                if (type == ErrType.AUTH && code == 0) {
                                    isInitialized = true;

                                    // 初始化各个管理器
                                    initManagers();

                                    sendEvent("authSuccess", null);
                                    callback.onSuccess("SDK初始化成功");
                                } else if (type == ErrType.AUTH) {
                                    sendEvent("authFailed", createErrorData(code, "授权失败"));
                                    callback.onError("SDK授权失败，错误码: " + code);
                                }
                            }
                        });

                        // 初始化SDK
                        AiHelper.getInst().initEntry(activity.getApplicationContext(), params);

                        // 创建必要的目录
                        new File(wakeUpWorkDir).mkdirs();
                        new File(ttsOutputDir).mkdirs();

                    } catch (Exception e) {
                        Log.e(TAG, "Init error: " + e.getMessage(), e);
                        callback.onError("初始化失败: " + e.getMessage());
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Init error: " + e.getMessage(), e);
            callback.onError("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 初始化各个功能管理器
     */
    private void initManagers() {
        // 初始化TTS管理器
        ttsManager = new TTSManager(activity, ttsOutputDir);
        ttsManager.setEventCallback(new TTSManager.TTSEventCallback() {
            @Override
            public void onEvent(String event, JSONObject data) {
                sendEvent(event, data);
            }
        });

        // 初始化唤醒管理器
        wakeUpManager = new WakeUpManager(activity, wakeUpWorkDir);
        wakeUpManager.setEventCallback(new WakeUpManager.WakeUpEventCallback() {
            @Override
            public void onEvent(String event, JSONObject data) {
                sendEvent(event, data);
            }
        });

        // 初始化命令词识别管理器
        commandManager = new CommandRecognitionManager(activity);
        commandManager.setEventCallback(new CommandRecognitionManager.CommandEventCallback() {
            @Override
            public void onEvent(String event, JSONObject data) {
                sendEvent(event, data);
            }
        });
    }

    /**
     * 反初始化SDK
     */
    public void unInit() {
        try {
            if (wakeUpManager != null) {
                wakeUpManager.stopWakeUp(null);
            }
            if (commandManager != null) {
                commandManager.stopCommandRecognition(null);
            }
            AiHelper.getInst().unInit();
            isInitialized = false;
        } catch (Exception e) {
            Log.e(TAG, "UnInit error: " + e.getMessage(), e);
        }
    }

    /**
     * 开始语音唤醒
     */
    public void startWakeUp(String keywords, ResultCallback callback) {
        if (!isInitialized) {
            callback.onError("SDK未初始化");
            return;
        }
        if (wakeUpManager == null) {
            callback.onError("唤醒管理器未初始化");
            return;
        }
        wakeUpManager.startWakeUp(keywords, new WakeUpManager.WakeUpResultCallback() {
            @Override
            public void onSuccess(Object result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 停止语音唤醒
     */
    public void stopWakeUp(ResultCallback callback) {
        if (wakeUpManager == null) {
            if (callback != null) {
                callback.onError("唤醒管理器未初始化");
            }
            return;
        }
        wakeUpManager.stopWakeUp(new WakeUpManager.WakeUpResultCallback() {
            @Override
            public void onSuccess(Object result) {
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * 开始语音合成（轻量版）
     */
    public void startTTS(String text, String vcn, int speed, int pitch, int volume, String fileName,
            ResultCallback callback) {
        if (!isInitialized) {
            callback.onError("SDK未初始化");
            return;
        }
        if (ttsManager == null) {
            callback.onError("TTS管理器未初始化");
            return;
        }
        ttsManager.startTTS(text, vcn, speed, pitch, volume, fileName, new TTSManager.TTSResultCallback() {
            @Override
            public void onSuccess(Object result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 开始语音合成（XTTS版本）
     */
    public void startXTTS(String text, String vcn, int language, int speed, int pitch, int volume,
            ResultCallback callback) {
        if (!isInitialized) {
            callback.onError("SDK未初始化");
            return;
        }
        if (ttsManager == null) {
            callback.onError("TTS管理器未初始化");
            return;
        }
        ttsManager.startXTTS(text, vcn, language, speed, pitch, volume, new TTSManager.TTSResultCallback() {
            @Override
            public void onSuccess(Object result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 开始命令词识别
     * 
     * @param fsaPath        FSA文件路径
     * @param languageType   语种类型 0:中文, 1:英文
     * @param timeoutSeconds 超时时间（秒），默认10秒，0表示不超时
     */
    public void startCommandRecognition(String fsaPath, int languageType, int timeoutSeconds, ResultCallback callback) {
        if (!isInitialized) {
            callback.onError("SDK未初始化");
            return;
        }
        if (commandManager == null) {
            callback.onError("命令词识别管理器未初始化");
            return;
        }
        commandManager.startCommandRecognition(fsaPath, languageType, timeoutSeconds,
                new CommandRecognitionManager.CommandResultCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }

    /**
     * 停止命令词识别
     */
    public void stopCommandRecognition(ResultCallback callback) {
        if (commandManager == null) {
            if (callback != null) {
                callback.onError("命令词识别管理器未初始化");
            }
            return;
        }
        commandManager.stopCommandRecognition(new CommandRecognitionManager.CommandResultCallback() {
            @Override
            public void onSuccess(Object result) {
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * 写入音频数据
     */
    public void writeAudioData(String audioDataBase64, String type, boolean isEnd, ResultCallback callback) {
        try {
            byte[] audioData = Base64.decode(audioDataBase64, Base64.DEFAULT);
            com.iflytek.aikit.core.AiStatus status = isEnd ? com.iflytek.aikit.core.AiStatus.END
                    : com.iflytek.aikit.core.AiStatus.CONTINUE;

            if (type.equals("wakeup")) {
                if (wakeUpManager != null) {
                    wakeUpManager.writeAudioData(audioData, status);
                }
            } else if (type.equals("command")) {
                if (commandManager != null) {
                    commandManager.writeAudioData(audioData, status);
                }
            }

            if (callback != null) {
                callback.onSuccess("音频数据已写入");
            }
        } catch (Exception e) {
            Log.e(TAG, "WriteAudioData error: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("写入音频数据失败: " + e.getMessage());
            }
        }

    }

    /**
     * 播放音频文件
     * 
     * @param filePath   音频文件路径（PCM格式）
     * @param sampleRate 采样率类型：16k 或 24k
     * @param callback   回调
     */
    public void playAudio(String filePath, String sampleRate, ResultCallback callback) {
        try {
            AudioTrackManager.sampleRateType rateType;
            if ("24k".equalsIgnoreCase(sampleRate)) {
                rateType = AudioTrackManager.sampleRateType.SAMPLE_RATE_24k;
            } else {
                rateType = AudioTrackManager.sampleRateType.SAMPLE_RATE_16k;
            }

            AudioTrackManager.getInstance().setSampleRate(rateType);
            AudioTrackManager.getInstance().startPlay(filePath);

            if (callback != null) {
                callback.onSuccess("开始播放音频");
            }
            Log.d(TAG, "开始播放音频: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "播放音频失败: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("播放音频失败: " + e.getMessage());
            }
        }
    }

    /**
     * 停止播放音频
     */
    public void stopPlayAudio(ResultCallback callback) {
        try {
            AudioTrackManager.getInstance().stopPlay();
            if (callback != null) {
                callback.onSuccess("停止播放");
            }
            Log.d(TAG, "停止播放音频");
        } catch (Exception e) {
            Log.e(TAG, "停止播放失败: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("停止播放失败: " + e.getMessage());
            }
        }
    }

    /**
     * 注册事件监听器
     */
    public void registerListener(EventCallback callback) {
        this.eventCallback = callback;
    }

    // ========== 私有方法 ==========

    /**
     * 发送事件
     */
    private void sendEvent(String event, JSONObject data) {
        if (eventCallback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    eventCallback.onEvent(event, data);
                }
            });
        }
    }

    /**
     * 创建错误数据
     */
    private JSONObject createErrorData(int code, String message) {
        try {
            JSONObject data = new JSONObject();
            data.put("code", code);
            data.put("message", message);
            return data;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    /**
     * 确保工作目录存在且有权限
     * 
     * @param workDir 工作目录路径
     * @return 最终的工作目录路径，如果创建失败返回null
     */
    private String ensureWorkDirExists(String workDir) {
        try {
            File workDirFile = new File(workDir);

            // 检查工作目录是否存在，不存在则创建
            if (!workDirFile.exists()) {
                boolean created = workDirFile.mkdirs();
                if (!created) {
                    Log.e(TAG, "创建工作目录失败: " + workDir);
                    // 如果外部存储不可用，尝试使用应用私有目录
                    File externalFilesDir = activity.getExternalFilesDir(null);
                    if (externalFilesDir != null) {
                        File fallbackDir = new File(externalFilesDir, "iflytek");
                        if (fallbackDir.mkdirs() || fallbackDir.exists()) {
                            Log.i(TAG, "使用备用目录: " + fallbackDir.getAbsolutePath());
                            return fallbackDir.getAbsolutePath() + File.separator;
                        }
                    }
                    return null;
                }
                Log.d(TAG, "创建工作目录成功: " + workDir);
            }

            // 检查目录是否有读写权限
            if (!workDirFile.canWrite()) {
                Log.e(TAG, "工作目录无写入权限: " + workDir);
                return null;
            }

            // 确保路径以分隔符结尾
            String finalPath = workDir;
            if (!finalPath.endsWith(File.separator)) {
                finalPath = finalPath + File.separator;
            }

            return finalPath;
        } catch (Exception e) {
            Log.e(TAG, "确保工作目录存在失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检测并复制资源文件到工作目录（如果不存在）
     */
    private void copyResourcesIfNeeded(String workDir) {
        try {
            File workDirFile = new File(workDir);

            // 检查工作目录是否存在，不存在则创建
            if (!workDirFile.exists()) {
                boolean created = workDirFile.mkdirs();
                if (!created) {
                    Log.e(TAG, "复制资源文件时创建工作目录失败: " + workDir);
                    return;
                }
                Log.d(TAG, "创建工作目录: " + workDir);
            }

            // 需要检查的资源目录列表
            String[] resourceDirs = { "ivw", "aisound", "esr", "xtts" };
            boolean needCopy = false;

            // 检查是否有资源文件缺失
            for (String dir : resourceDirs) {
                File targetDir = new File(workDir + File.separator + dir);
                if (!targetDir.exists() || (targetDir.exists()
                        && (targetDir.listFiles() == null || targetDir.listFiles().length == 0))) {
                    needCopy = true;
                    break;
                }
            }

            // 如果需要复制，从assets复制资源文件
            if (needCopy) {
                Log.i(TAG, "检测到资源文件缺失，开始从assets复制资源文件...");
                android.content.res.AssetManager assetManager = activity.getAssets();

                for (String dir : resourceDirs) {
                    try {
                        // 检查assets中是否存在该目录
                        String[] files = assetManager.list("iflytek/" + dir);
                        if (files != null && files.length > 0) {
                            File targetDir = new File(workDir + File.separator + dir);
                            if (!targetDir.exists()) {
                                targetDir.mkdirs();
                                Log.d(TAG, "创建目录: " + targetDir.getAbsolutePath());
                            }

                            // 复制文件
                            copyAssetsDir(assetManager, "iflytek/" + dir, targetDir);
                            Log.d(TAG, "复制资源目录完成: " + dir);
                        }
                    } catch (java.io.IOException e) {
                        Log.w(TAG, "资源目录不存在: iflytek/" + dir + ", 跳过");
                    }
                }

                Log.i(TAG, "资源文件复制完成");
            } else {
                Log.d(TAG, "资源文件已存在，跳过复制");
            }

        } catch (Exception e) {
            Log.e(TAG, "复制资源文件失败: " + e.getMessage(), e);
            // 资源文件复制失败不影响SDK初始化，只记录日志
        }
    }

    /**
     * 递归复制assets目录到目标目录
     */
    private void copyAssetsDir(android.content.res.AssetManager assetManager, String assetsPath, File targetDir)
            throws java.io.IOException {
        String[] files = assetManager.list(assetsPath);

        if (files != null && files.length > 0) {
            for (String file : files) {
                String assetFilePath = assetsPath + "/" + file;
                String[] subFiles = assetManager.list(assetFilePath);

                if (subFiles != null && subFiles.length > 0) {
                    // 是子目录
                    File subDir = new File(targetDir, file);
                    subDir.mkdirs();
                    copyAssetsDir(assetManager, assetFilePath, subDir);
                } else {
                    // 是文件
                    File targetFile = new File(targetDir, file);
                    if (!targetFile.exists()) {
                        copyAssetFile(assetManager, assetFilePath, targetFile);
                    }
                }
            }
        }
    }

    /**
     * 复制单个文件
     */
    private void copyAssetFile(android.content.res.AssetManager assetManager, String assetPath, File targetFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(assetPath);
            out = new java.io.FileOutputStream(targetFile);

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            Log.d(TAG, "复制文件: " + assetPath + " -> " + targetFile.getAbsolutePath());

        } catch (java.io.IOException e) {
            Log.e(TAG, "复制文件失败: " + assetPath, e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (java.io.IOException e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }
}
