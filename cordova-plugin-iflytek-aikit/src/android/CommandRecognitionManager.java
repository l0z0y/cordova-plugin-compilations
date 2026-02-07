package com.iflytek.aikit.plugin;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iflytek.aikit.core.AiAudio;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 命令词识别管理器
 * 负责处理命令词识别功能
 */
public class CommandRecognitionManager {
    private static final String TAG = "CommandRecognitionManager";

    // 能力ID定义
    private static final String ABILITY_ESR = "e75f07b62"; // 命令词识别

    private Activity activity;
    private Handler mainHandler;

    private AiHandle commandHandle;
    private AtomicBoolean isCommandEnd = new AtomicBoolean(true);
    private AudioRecord commandAudioRecord;
    private final int BUFFER_SIZE = 1280;
    private AtomicBoolean isCommandRecording = new AtomicBoolean(false);
    private boolean isCommandEngineInit = false;
    private int commandLanguageType = 0;
    private String commandFsaPath;
    private boolean isCommandLoadData = false;
    private int commandIndex = 0;

    // 超时定时器
    private static final int DEFAULT_TIMEOUT_SECONDS = 10; // 默认超时时间10秒
    private Runnable timeoutRunnable;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    // 事件回调接口
    public interface CommandResultCallback {
        void onSuccess(Object result);

        void onError(String error);
    }

    public interface CommandEventCallback {
        void onEvent(String event, JSONObject data);
    }

    private CommandEventCallback eventCallback;

    public CommandRecognitionManager(Activity activity) {
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 设置事件回调
     */
    public void setEventCallback(CommandEventCallback callback) {
        this.eventCallback = callback;
    }

    /**
     * 开始命令词识别
     * 
     * @param fsaPath        FSA文件路径
     * @param languageType   语种类型 0:中文, 1:英文
     * @param timeoutSeconds 超时时间（秒），默认10秒，0表示不超时
     * @param callback       回调
     */
    public void startCommandRecognition(String fsaPath, int languageType, int timeoutSeconds,
            CommandResultCallback callback) {
        try {
            // 设置超时时间（如果为0或负数，使用默认值10秒）
            this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;

            Log.d(TAG, "========== 开始命令词识别 ==========");
            Log.d(TAG, "FSA路径: " + fsaPath);
            Log.d(TAG, "语种类型: " + languageType);
            Log.d(TAG, "超时时间: " + this.timeoutSeconds + "秒");
            Log.d(TAG, "当前状态 - isCommandEnd: " + isCommandEnd.get() + ", isCommandRecording: "
                    + isCommandRecording.get() + ", commandHandle: " + (commandHandle != null));

            // 如果已经在运行，先停止
            if (commandHandle != null && !isCommandEnd.get()) {
                Log.w(TAG, "检测到识别正在运行，先停止之前的识别");
                stopCommandRecognition(null);
                // 等待一下确保停止完成
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 取消之前的超时定时器（如果有）
            cancelTimeoutTimer();

            commandLanguageType = languageType;
            commandFsaPath = fsaPath;

            // 初始化引擎（如果需要）
            if (!isCommandEngineInit) {
                Log.d(TAG, "步骤1: 初始化命令词引擎");
                initCommandEngine();
                Log.d(TAG, "引擎初始化结果: " + isCommandEngineInit);
            } else {
                Log.d(TAG, "步骤1: 引擎已初始化，跳过");
            }

            // 加载FSA数据
            if (!isCommandLoadData) {
                Log.d(TAG, "步骤2: 加载FSA数据");
                AiRequest.Builder customBuilder = AiRequest.builder();
                customBuilder.customText("FSA", fsaPath, commandIndex);
                int ret = AiHelper.getInst().loadData(ABILITY_ESR, customBuilder.build());
                Log.d(TAG, "加载FSA数据结果: " + ret);
                if (ret != 0) {
                    Log.e(TAG, "加载FSA数据失败: " + ret);
                    callback.onError("加载FSA数据失败: " + ret);
                    return;
                }
                isCommandLoadData = true;
            } else {
                Log.d(TAG, "步骤2: FSA数据已加载，跳过");
            }

            // 设置数据集
            Log.d(TAG, "步骤3: 设置数据集");
            int[] indexs = { commandIndex };
            int ret = AiHelper.getInst().specifyDataSet(ABILITY_ESR, "FSA", indexs);
            Log.d(TAG, "设置数据集结果: " + ret);
            if (ret != 0) {
                Log.e(TAG, "设置数据集失败: " + ret);
                callback.onError("设置数据集失败: " + ret);
                return;
            }

            // 注册监听器（只注册一次）
            Log.d(TAG, "步骤4: 注册监听器");
            AiHelper.getInst().registerListener(ABILITY_ESR, new AiListener() {
                @Override
                public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {
                    Log.d(TAG, "========== 收到识别结果 ==========");
                    Log.d(TAG, "handleID: " + handleID);
                    Log.d(TAG, "outputData数量: " + (outputData != null ? outputData.size() : 0));

                    if (outputData != null && outputData.size() > 0) {
                        /**
                         * key的取值以及含义
                         * pgs:progressive格式的结果，即可以实时刷屏
                         * htk:带有分词信息的结果，每一个分词结果占一行
                         * plain:类比于htk，把一句话结果中的所有分词拼成完整一句，若有后处理，则也含有后处理的结果信息，plain是每一段话的最终结果
                         * vad:语音端点检测结果(需要打开vad功能才会返回)bg:前端点，ed:后端点。单位:帧(10ms)
                         * readable:json格式的结果。
                         */

                        // 统计信息
                        boolean hasVad = false;
                        boolean hasPgs = false;
                        boolean hasPlain = false;
                        String lastPgsResult = null;
                        String plainResult = null;

                        // 用于pgs结果去重（用于实时进度显示）
                        Set<String> sentPgsResults = new HashSet<>();

                        // 第一遍遍历：统计所有结果类型
                        for (AiResponse response : outputData) {
                            String key = response.getKey();
                            byte[] valueBytes = response.getValue();

                            Log.d(TAG, "处理响应 - key: " + key + ", status: " + response.getStatus() + ", value长度: "
                                    + (valueBytes != null ? valueBytes.length : 0));

                            if (key == null)
                                continue;

                            // 统计VAD事件
                            if (key.contains("vad")) {
                                hasVad = true;
                                Log.d(TAG, "检测到VAD事件: " + key);
                                continue; // VAD事件不参与后续处理
                            }

                            // 处理pgs结果（渐进式结果）
                            if (key.contains("pgs") && valueBytes != null) {
                                String result = parseResult(valueBytes);
                                if (result != null && !result.trim().isEmpty()) {
                                    hasPgs = true;
                                    lastPgsResult = result;
                                    // 实时发送进度事件（去重）
                                    if (!sentPgsResults.contains(result)) {
                                        sentPgsResults.add(result);
                                        Log.d(TAG, "发送识别进度（渐进式）: " + result);
                                        sendEvent("commandProgress", createResultData("value", result));
                                    }
                                }
                            }

                            // 处理plain结果（最终结果）
                            if (key.contains("plain") && valueBytes != null) {
                                String result = parseResult(valueBytes);
                                if (result != null && !result.trim().isEmpty()) {
                                    hasPlain = true;
                                    plainResult = result;
                                }
                            }
                        }

                        // 判断识别是否完成（与官方demo一致：检查第一个结果的status）
                        if (outputData.size() > 0 && outputData.get(0).getStatus() == 2) {
                            Log.d(TAG, "识别完成（status=2），准备停止识别");
                            Log.d(TAG, "统计结果 - hasVad: " + hasVad + ", hasPgs: " + hasPgs + ", hasPlain: " + hasPlain);

                            try {
                                // 统一处理逻辑：根据统计结果决定发送什么事件
                                if (hasPlain) {
                                    // 有最终结果，发送成功事件
                                    Log.d(TAG, "发送最终识别结果: " + plainResult);
                                    sendEvent("commandResult", createResultData("value", plainResult));
                                } else if (hasPgs) {
                                    // 有渐进式结果但没有最终结果，说明识别到了但不在命令词列表中
                                    Log.w(TAG, "识别到语音但不在命令词列表中: " + lastPgsResult);
                                    JSONObject unsupportedData = new JSONObject();
                                    unsupportedData.put("value", lastPgsResult != null ? lastPgsResult : "");
                                    unsupportedData.put("message", "暂时不支持此操作");
                                    sendEvent("commandUnsupported", unsupportedData);
                                } else if (hasVad) {
                                    // 只有VAD事件，说明未识别到有效语音
                                    Log.w(TAG, "只有VAD事件，未识别到有效语音");
                                    JSONObject noMatchData = new JSONObject();
                                    noMatchData.put("value", "");
                                    noMatchData.put("message", "未识别到有效命令词");
                                    sendEvent("commandNoMatch", noMatchData);
                                } else {
                                    // 其他情况，也视为未识别到
                                    Log.w(TAG, "识别完成但无有效结果");
                                    JSONObject noMatchData = new JSONObject();
                                    noMatchData.put("value", "");
                                    noMatchData.put("message", "未识别到有效命令词");
                                    sendEvent("commandNoMatch", noMatchData);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "创建事件数据失败: " + e.getMessage());
                            }

                            // 取消超时定时器
                            cancelTimeoutTimer();
                            // 停止识别，但不重置所有状态，以便可以再次启动
                            stopCommandRecognitionForNext(null);
                        }
                    } else {
                        Log.w(TAG, "识别结果为空");
                    }
                }

                @Override
                public void onEvent(int handleID, int eventType, List<AiResponse> eventData, Object usrContext) {
                    Log.d(TAG, "========== 收到识别事件 ==========");
                    Log.d(TAG, "handleID: " + handleID + ", eventType: " + eventType);
                    if (eventData != null) {
                        Log.d(TAG, "事件数据数量: " + eventData.size());
                    }
                }

                @Override
                public void onError(int handleID, int errCode, String errMsg, Object usrContext) {
                    Log.e(TAG, "========== 识别错误 ==========");
                    Log.e(TAG, "handleID: " + handleID + ", errCode: " + errCode + ", errMsg: " + errMsg);
                    sendEvent("commandError", createErrorData(errCode, errMsg));
                }
            });

            // 构建参数
            Log.d(TAG, "步骤5: 构建识别参数");
            AiRequest.Builder paramBuilder = AiRequest.builder();
            paramBuilder.param("languageType", languageType);
            paramBuilder.param("vadEndGap", 60);
            paramBuilder.param("vadOn", true);
            paramBuilder.param("beamThreshold", 20);
            paramBuilder.param("hisGramThreshold", 3000);
            paramBuilder.param("vadLinkOn", false);
            paramBuilder.param("vadSpeechEnd", 80);
            paramBuilder.param("vadResponsetime", 1000);
            paramBuilder.param("postprocOn", false);

            // 重置状态
            isCommandEnd.set(false);
            Log.d(TAG, "步骤6: 启动识别引擎");
            commandHandle = AiHelper.getInst().start(ABILITY_ESR, paramBuilder.build(), null);
            Log.d(TAG, "启动识别引擎结果 - code: " + commandHandle.getCode() + ", handleID: " + commandHandle.getI() + ", id: "
                    + commandHandle.getId());

            if (commandHandle.getCode() != 0) {
                Log.e(TAG, "启动识别失败: " + commandHandle.getCode());
                callback.onError("启动识别失败: " + commandHandle.getCode());
                return;
            }

            // 开始录音
            Log.d(TAG, "步骤7: 开始录音");
            startCommandRecording();

            // 启动超时定时器
            startTimeoutTimer();

            Log.d(TAG, "========== 命令词识别启动完成 ==========");
            callback.onSuccess("识别已启动");

        } catch (Exception e) {
            Log.e(TAG, "StartCommandRecognition error: " + e.getMessage(), e);
            callback.onError("启动识别失败: " + e.getMessage());
        }
    }

    /**
     * 停止命令词识别（用于识别完成后，保持状态以便再次启动）
     * 注意：这里不完全释放资源，以便可以快速再次启动
     */
    private void stopCommandRecognitionForNext(CommandResultCallback callback) {
        Log.d(TAG, "========== 停止识别（准备下次启动）==========");
        // 取消超时定时器
        cancelTimeoutTimer();

        try {
            // 先设置标志位，让录音线程自然退出
            if (isCommandRecording.get()) {
                Log.d(TAG, "设置停止标志，等待录音线程退出");
                isCommandRecording.set(false);

                // 等待一小段时间，让录音线程检测到标志位变化并退出
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 然后停止AudioRecord
            if (commandAudioRecord != null) {
                try {
                    int state = commandAudioRecord.getRecordingState();
                    if (state == AudioRecord.RECORDSTATE_RECORDING) {
                        Log.d(TAG, "停止AudioRecord");
                        commandAudioRecord.stop();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "停止AudioRecord异常: " + e.getMessage());
                }
            }

            // 结束识别句柄
            if (commandHandle != null && !isCommandEnd.get()) {
                Log.d(TAG, "结束识别句柄");
                try {
                    int ret = AiHelper.getInst().end(commandHandle);
                    Log.d(TAG, "结束识别句柄结果: " + ret);
                } catch (Exception e) {
                    Log.e(TAG, "结束识别句柄异常: " + e.getMessage());
                }
                isCommandEnd.set(true);
                commandHandle = null;
            }

            // 注意：这里不释放 AudioRecord，也不重置 isCommandLoadData，以便下次启动时可以直接使用
            // 下次启动时会检查状态，如果 AudioRecord 存在但状态不对，会重新创建
            Log.d(TAG, "识别已停止，可以再次启动（资源已保留）");
            if (callback != null) {
                callback.onSuccess("识别已停止");
            }
        } catch (Exception e) {
            Log.e(TAG, "StopCommandRecognitionForNext error: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("停止识别失败: " + e.getMessage());
            }
        }
    }

    /**
     * 停止命令词识别（完全停止）
     */
    public void stopCommandRecognition(CommandResultCallback callback) {
        Log.d(TAG, "========== 停止识别（完全停止）==========");
        // 取消超时定时器
        cancelTimeoutTimer();

        try {
            // 先设置标志位，让录音线程自然退出
            if (isCommandRecording.get()) {
                Log.d(TAG, "设置停止标志，等待录音线程退出");
                isCommandRecording.set(false);

                // 等待一小段时间，让录音线程检测到标志位变化并退出
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 然后停止AudioRecord
            if (commandAudioRecord != null) {
                try {
                    int state = commandAudioRecord.getRecordingState();
                    if (state == AudioRecord.RECORDSTATE_RECORDING) {
                        Log.d(TAG, "停止AudioRecord");
                        commandAudioRecord.stop();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "停止AudioRecord异常: " + e.getMessage());
                }
            }

            // 释放AudioRecord资源
            if (commandAudioRecord != null) {
                Log.d(TAG, "释放AudioRecord资源");
                commandAudioRecord.release();
                commandAudioRecord = null;
            }

            // 结束识别句柄
            if (commandHandle != null && !isCommandEnd.get()) {
                Log.d(TAG, "结束识别句柄");
                int ret = AiHelper.getInst().end(commandHandle);
                Log.d(TAG, "结束识别句柄结果: " + ret);
                isCommandEnd.set(true);
                commandHandle = null;
            }

            Log.d(TAG, "识别已完全停止");
            if (callback != null) {
                callback.onSuccess("识别已停止");
            }
        } catch (Exception e) {
            Log.e(TAG, "StopCommandRecognition error: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("停止识别失败: " + e.getMessage());
            }
        }
    }

    /**
     * 写入音频数据
     */
    public void writeAudioData(byte[] audioData, AiStatus status) {
        if (isCommandEnd.get() || commandHandle == null) {
            Log.d(TAG,
                    "跳过写入音频数据 - isCommandEnd: " + isCommandEnd.get() + ", commandHandle: " + (commandHandle != null));
            return;
        }

        try {
            AiRequest.Builder dataBuilder = AiRequest.builder();
            AiAudio aiAudio = AiAudio.get("audio").data(audioData).status(status).valid();
            dataBuilder.payload(aiAudio);
            int ret = AiHelper.getInst().write(dataBuilder.build(), commandHandle);
            if (ret != 0) {
                Log.w(TAG, "写入音频数据返回非0: " + ret);
            }
        } catch (Exception e) {
            Log.e(TAG, "Write command audio error: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化命令词引擎
     */
    private void initCommandEngine() {
        try {
            Log.d(TAG, "初始化命令词引擎 - decNetType: fsa, punishCoefficient: 0.0, wfst_addType: " + commandLanguageType);
            AiRequest.Builder engineBuilder = AiRequest.builder();
            engineBuilder.param("decNetType", "fsa");
            engineBuilder.param("punishCoefficient", 0.0);
            engineBuilder.param("wfst_addType", commandLanguageType);
            int ret = AiHelper.getInst().engineInit(ABILITY_ESR, engineBuilder.build());
            Log.d(TAG, "引擎初始化结果: " + ret);
            if (ret == 0) {
                isCommandEngineInit = true;
                Log.d(TAG, "引擎初始化成功");
            } else {
                Log.e(TAG, "引擎初始化失败: " + ret);
            }
        } catch (Exception e) {
            Log.e(TAG, "Init command engine error: " + e.getMessage(), e);
        }
    }

    /**
     * 开始录音
     */
    private void startCommandRecording() {
        if (isCommandRecording.get()) {
            Log.w(TAG, "录音已在运行中，跳过启动");
            return;
        }

        // 如果AudioRecord已存在但状态不对，先释放
        if (commandAudioRecord != null) {
            try {
                if (commandAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    Log.w(TAG, "检测到AudioRecord正在录音，先停止");
                    commandAudioRecord.stop();
                }
                commandAudioRecord.release();
                commandAudioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "释放旧AudioRecord失败: " + e.getMessage());
            }
        }

        Log.d(TAG, "创建AudioRecord - 采样率: 16000, 声道: MONO, 编码: PCM_16BIT, 缓冲区: " + BUFFER_SIZE);
        commandAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE);

        if (commandAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord初始化失败");
            return;
        }

        isCommandRecording.set(true);
        commandAudioRecord.startRecording();
        Log.d(TAG, "录音已启动");

        // 启动录音线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "录音线程已启动");
                byte[] buffer = new byte[BUFFER_SIZE];
                boolean isFirstFrame = true;
                int frameCount = 0;

                while (isCommandRecording.get() && commandHandle != null && !isCommandEnd.get()) {
                    // 检查AudioRecord状态，如果已停止则退出
                    if (commandAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                        Log.d(TAG, "AudioRecord已停止，退出录音线程");
                        break;
                    }

                    int read = commandAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    if (read > 0 && AudioRecord.ERROR_INVALID_OPERATION != read) {
                        AiStatus status = isFirstFrame ? AiStatus.BEGIN : AiStatus.CONTINUE;
                        writeAudioData(buffer, status);
                        isFirstFrame = false;
                        frameCount++;

                        // 每100帧读取一次识别结果（避免过于频繁）
                        if (frameCount % 100 == 0 && commandHandle != null) {
                            try {
                                AiHelper.getInst().read(ABILITY_ESR, commandHandle);
                            } catch (Exception e) {
                                Log.e(TAG, "读取识别结果失败: " + e.getMessage());
                            }
                        }
                    } else if (read < 0) {
                        // 如果读取失败且不是正常的停止操作，记录警告
                        // -38 (ERROR_INVALID_OPERATION) 通常表示AudioRecord已停止，这是正常的
                        if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                            Log.w(TAG, "读取音频数据失败: " + read);
                        }
                        // 如果AudioRecord已停止，退出循环
                        if (!isCommandRecording.get()
                                || commandAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                            break;
                        }
                    }
                }
                Log.d(TAG, "录音线程已退出 - frameCount: " + frameCount);
            }
        }).start();
    }

    /**
     * 发送事件
     */
    private void sendEvent(String event, JSONObject data) {
        if (eventCallback != null) {
            eventCallback.onEvent(event, data);
        }
    }

    /**
     * 创建结果数据
     */
    private JSONObject createResultData(String key, String value) {
        try {
            JSONObject data = new JSONObject();
            data.put(key, value);
            return data;
        } catch (JSONException e) {
            return new JSONObject();
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
     * 解析识别结果（支持GBK和UTF-8编码）
     */
    private String parseResult(byte[] valueBytes) {
        if (valueBytes == null) {
            return null;
        }
        try {
            // 优先使用GBK编码（与官方demo一致）
            return new String(valueBytes, "GBK");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "使用GBK编码解析失败，尝试UTF-8: " + e.getMessage());
            try {
                return new String(valueBytes, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Log.e(TAG, "使用UTF-8编码也失败，使用系统默认编码");
                return new String(valueBytes);
            }
        }
    }

    /**
     * 启动超时定时器
     */
    private void startTimeoutTimer() {
        // 取消之前的定时器（如果有）
        cancelTimeoutTimer();

        Log.d(TAG, "启动超时定时器，超时时间: " + timeoutSeconds + "秒");

        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCommandEnd.get() && commandHandle != null) {
                    Log.w(TAG, "========== 识别超时，自动停止 ==========");
                    Log.w(TAG, "超时时间: " + timeoutSeconds + "秒");
                    // 发送超时事件
                    try {
                        JSONObject timeoutData = new JSONObject();
                        timeoutData.put("message", "识别超时，已自动停止");
                        timeoutData.put("timeoutSeconds", timeoutSeconds);
                        sendEvent("commandTimeout", timeoutData);
                    } catch (JSONException e) {
                        Log.e(TAG, "创建超时事件数据失败: " + e.getMessage());
                    }
                    // 自动停止识别
                    stopCommandRecognitionForNext(null);
                }
            }
        };

        // 在主线程中延迟执行
        mainHandler.postDelayed(timeoutRunnable, timeoutSeconds * 1000L);
    }

    /**
     * 取消超时定时器
     */
    private void cancelTimeoutTimer() {
        if (timeoutRunnable != null) {
            Log.d(TAG, "取消超时定时器");
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }
}
