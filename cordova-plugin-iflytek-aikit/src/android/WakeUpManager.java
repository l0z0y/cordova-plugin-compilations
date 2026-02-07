package com.iflytek.aikit.plugin;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 语音唤醒管理器
 * 负责处理语音唤醒功能
 */
public class WakeUpManager {
    private static final String TAG = "WakeUpManager";

    // 能力ID定义
    private static final String ABILITY_IVW = "e867a88f2"; // 语音唤醒

    private Activity activity;
    private String wakeUpWorkDir;

    private AiHandle wakeUpHandle;
    private AtomicBoolean isWakeUpEnd = new AtomicBoolean(true);
    private AudioRecord wakeUpAudioRecord;
    private final int BUFFER_SIZE = 1280;
    private AtomicBoolean isWakeUpRecording = new AtomicBoolean(false);

    // 事件回调接口
    public interface WakeUpResultCallback {
        void onSuccess(Object result);
        void onError(String error);
    }

    public interface WakeUpEventCallback {
        void onEvent(String event, JSONObject data);
    }

    private WakeUpEventCallback eventCallback;

    public WakeUpManager(Activity activity, String wakeUpWorkDir) {
        this.activity = activity;
        this.wakeUpWorkDir = wakeUpWorkDir;
    }

    /**
     * 设置事件回调
     */
    public void setEventCallback(WakeUpEventCallback callback) {
        this.eventCallback = callback;
    }

    /**
     * 开始语音唤醒
     */
    public void startWakeUp(String keywords, WakeUpResultCallback callback) {
        try {
            // 写入唤醒词文件
            if (!writeKeywordFile(keywords)) {
                callback.onError("唤醒词文件写入失败");
                return;
            }

            // 加载唤醒词数据
            AiRequest.Builder customBuilder = AiRequest.builder();
            customBuilder.customText("key_word", wakeUpWorkDir + "/keyword.txt", 0);
            int ret = AiHelper.getInst().loadData(ABILITY_IVW, customBuilder.build());
            if (ret != 0) {
                callback.onError("加载唤醒词数据失败: " + ret);
                return;
            }

            // 设置数据集
            int[] indexs = { 0 };
            ret = AiHelper.getInst().specifyDataSet(ABILITY_IVW, "key_word", indexs);
            if (ret != 0) {
                callback.onError("设置数据集失败: " + ret);
                return;
            }

            // 注册监听器
            AiHelper.getInst().registerListener(ABILITY_IVW, new AiListener() {
                @Override
                public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {
                    if (outputData != null && outputData.size() > 0) {
                        for (AiResponse response : outputData) {
                            String key = response.getKey();
                            String value = new String(response.getValue());
                            if (key.equals("func_wake_up") || key.equals("func_pre_wakeup")) {
                                sendEvent("wakeUpResult", createResultData(key, value));
                            }
                        }
                    }
                }

                @Override
                public void onEvent(int i, int i1, List<AiResponse> list, Object o) {
                    Log.d(TAG, "WakeUp event: " + i1);
                }

                @Override
                public void onError(int i, int i1, String s, Object o) {
                    sendEvent("wakeUpError", createErrorData(i1, s));
                }
            });

            // 开始唤醒
            AiRequest.Builder paramBuilder = AiRequest.builder();
            paramBuilder.param("wdec_param_nCmThreshold", "0 0:800");
            paramBuilder.param("gramLoad", true);
            isWakeUpEnd.set(false);
            wakeUpHandle = AiHelper.getInst().start(ABILITY_IVW, paramBuilder.build(), null);

            if (wakeUpHandle.getCode() != 0) {
                callback.onError("启动唤醒失败: " + wakeUpHandle.getCode());
                return;
            }

            // 开始录音
            startWakeUpRecording();
            callback.onSuccess("唤醒已启动");

        } catch (Exception e) {
            Log.e(TAG, "StartWakeUp error: " + e.getMessage(), e);
            callback.onError("启动唤醒失败: " + e.getMessage());
        }
    }

    /**
     * 停止语音唤醒
     */
    public void stopWakeUp(WakeUpResultCallback callback) {
        try {
            if (wakeUpAudioRecord != null && isWakeUpRecording.get()) {
                wakeUpAudioRecord.stop();
                isWakeUpRecording.set(false);
            }

            if (wakeUpHandle != null && !isWakeUpEnd.get()) {
                int ret = AiHelper.getInst().end(wakeUpHandle);
                isWakeUpEnd.set(true);
                wakeUpHandle = null;
                if (callback != null) {
                    callback.onSuccess("唤醒已停止");
                }
            } else if (callback != null) {
                callback.onSuccess("唤醒已停止");
            }
        } catch (Exception e) {
            Log.e(TAG, "StopWakeUp error: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("停止唤醒失败: " + e.getMessage());
            }
        }
    }

    /**
     * 写入音频数据
     */
    public void writeAudioData(byte[] audioData, AiStatus status) {
        if (isWakeUpEnd.get() || wakeUpHandle == null) {
            return;
        }

        try {
            AiRequest.Builder dataBuilder = AiRequest.builder();
            AiAudio aiAudio = AiAudio.get("wav").data(audioData).status(status).valid();
            dataBuilder.payload(aiAudio);
            AiHelper.getInst().write(dataBuilder.build(), wakeUpHandle);
        } catch (Exception e) {
            Log.e(TAG, "Write wakeup audio error: " + e.getMessage());
        }
    }

    /**
     * 写入唤醒词文件
     */
    private boolean writeKeywordFile(String keywords) {
        try {
            File keywordFile = new File(wakeUpWorkDir + "/keyword.txt");
            if (keywordFile.exists()) {
                keywordFile.delete();
            }
            keywordFile.getParentFile().mkdirs();
            keywordFile.createNewFile();

            String[] keywordArray = keywords.split(",");
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(keywordFile), "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for (String keyword : keywordArray) {
                bufferedWriter.write(keyword.trim());
                bufferedWriter.write(";");
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Write keyword file error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 开始录音
     */
    private void startWakeUpRecording() {
        if (isWakeUpRecording.get()) {
            return;
        }

        if (wakeUpAudioRecord == null) {
            wakeUpAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE);
        }

        isWakeUpRecording.set(true);
        wakeUpAudioRecord.startRecording();

        // 启动录音线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[BUFFER_SIZE];
                boolean isFirstFrame = true;

                while (isWakeUpRecording.get() && wakeUpHandle != null && !isWakeUpEnd.get()) {
                    int read = wakeUpAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    if (read > 0 && AudioRecord.ERROR_INVALID_OPERATION != read) {
                        AiStatus status = isFirstFrame ? AiStatus.BEGIN : AiStatus.CONTINUE;
                        writeAudioData(buffer, status);
                        isFirstFrame = false;
                    }
                }
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
}

