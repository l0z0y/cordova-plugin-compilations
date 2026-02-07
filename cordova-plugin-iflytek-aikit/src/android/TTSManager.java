package com.iflytek.aikit.plugin;

import android.app.Activity;
import android.util.Log;

import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiInput;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 语音合成管理器
 * 负责处理TTS和XTTS语音合成功能
 */
public class TTSManager {
    private static final String TAG = "TTSManager";

    // 能力ID定义
    private static final String ABILITY_AISOUND = "ece9d3c90"; // 语音合成轻量版
    private static final String ABILITY_XTTS = "e2e44feff"; // 语音合成XTTS

    private Activity activity;
    private String ttsOutputDir;

    // TTS相关
    private AiHandle ttsHandle;
    private FileOutputStream ttsFileOutputStream;
    private String currentTtsFilePath;
    private boolean isTTSListenerRegistered = false;

    // XTTS相关
    private AiHandle xttsHandle;
    private FileOutputStream xttsFileOutputStream;
    private String currentXttsFilePath;
    private boolean isXTTSListenerRegistered = false;

    // 事件回调接口
    public interface TTSResultCallback {
        void onSuccess(Object result);

        void onError(String error);
    }

    public interface TTSEventCallback {
        void onEvent(String event, JSONObject data);
    }

    private TTSEventCallback eventCallback;

    public TTSManager(Activity activity, String ttsOutputDir) {
        this.activity = activity;
        this.ttsOutputDir = ttsOutputDir;
    }

    /**
     * 设置事件回调
     */
    public void setEventCallback(TTSEventCallback callback) {
        this.eventCallback = callback;
    }

    /**
     * 开始语音合成（轻量版）
     */
    public void startTTS(String text, String vcn, int speed, int pitch, int volume, String fileName,
            TTSResultCallback callback) {
        try {
            // 关闭之前的文件流（如果存在）
            closeTTSFileStream();

            // 生成输出文件名
            String finalFileName = (fileName != null && !fileName.isEmpty()) ? fileName
                    : "OutPut_" + System.currentTimeMillis() + ".pcm";
            if (!finalFileName.endsWith(".pcm")) {
                finalFileName += ".pcm";
            }

            currentTtsFilePath = ttsOutputDir + File.separator + finalFileName;
            File outputFile = new File(currentTtsFilePath);
            outputFile.getParentFile().mkdirs();

            // 创建文件输出流
            try {
                ttsFileOutputStream = new FileOutputStream(outputFile, false);
            } catch (IOException e) {
                Log.e(TAG, "创建TTS输出文件失败: " + e.getMessage());
                callback.onError("创建输出文件失败: " + e.getMessage());
                return;
            }

            // 注册监听器（只在第一次调用时注册，避免重复注册）
            if (!isTTSListenerRegistered) {
                AiHelper.getInst().registerListener(ABILITY_AISOUND, new AiListener() {
                    @Override
                    public void onResult(int handleID, List<AiResponse> list, Object usrContext) {
                        if (list != null && list.size() > 0) {
                            // 每次收到数据就追加写入文件
                            for (AiResponse response : list) {
                                byte[] bytes = response.getValue();
                                if (bytes != null && bytes.length > 0) {
                                    try {
                                        if (ttsFileOutputStream != null) {
                                            ttsFileOutputStream.write(bytes);
                                            ttsFileOutputStream.flush();
                                        }
                                    } catch (IOException e) {
                                        Log.e(TAG, "写入TTS文件失败: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onEvent(int handleID, int event, List<AiResponse> eventData, Object usrContext) {
                        if (event == com.iflytek.aikit.core.AeeEvent.AEE_EVENT_END.getValue()) {
                            // 合成结束，关闭文件流并发送结果事件
                            closeTTSFileStream();
                            if (ttsHandle != null) {
                                AiHelper.getInst().end(ttsHandle);
                                ttsHandle = null;
                            }
                            // 发送合成完成事件
                            JSONObject data = createResultData("filePath", currentTtsFilePath);
                            try {
                                if (fileName != null && !fileName.isEmpty()) {
                                    data.put("fileName", fileName);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "添加fileName到结果失败: " + e.getMessage());
                            }
                            sendEvent("ttsResult", data);
                            Log.d(TAG, "TTS合成完成，文件路径: " + currentTtsFilePath);
                        } else if (event == com.iflytek.aikit.core.AeeEvent.AEE_EVENT_PROGRESS.getValue()) {
                            // 处理进度事件
                            int pos = -1;
                            int len = -1;
                            if (eventData != null) {
                                for (AiResponse aiOutput : eventData) {
                                    if (aiOutput.getKey().equals("progress_pos")) {
                                        byte[] posBytes = aiOutput.getValue();
                                        if (posBytes != null && posBytes.length == 4) {
                                            pos = bytesToInt(posBytes);
                                        }
                                    } else if (aiOutput.getKey().equals("progress_len")) {
                                        byte[] lenBytes = aiOutput.getValue();
                                        if (lenBytes != null && lenBytes.length == 4) {
                                            len = bytesToInt(lenBytes);
                                        }
                                    }
                                }
                            }
                            if (pos >= 0 && len > 0) {
                                JSONObject progressData = new JSONObject();
                                try {
                                    progressData.put("pos", pos);
                                    progressData.put("len", len);
                                    progressData.put("progress", (int) (pos * 100.0 / len));
                                } catch (JSONException e) {
                                    Log.e(TAG, "创建进度数据失败: " + e.getMessage());
                                }
                                sendEvent("ttsProgress", progressData);
                            }
                        }
                    }

                    @Override
                    public void onError(int handleID, int err, String msg, Object usrContext) {
                        // 错误时关闭文件流
                        closeTTSFileStream();
                        sendEvent("ttsError", createErrorData(err, msg));
                        Log.e(TAG, "TTS合成错误: handleID=" + handleID + ", err=" + err + ", msg=" + msg);
                    }
                });
                isTTSListenerRegistered = true;
                Log.d(TAG, "TTS监听器已注册");
            }

            // 构建参数
            AiInput.Builder paramBuilder = AiInput.builder();
            paramBuilder.param("vcn", vcn); // 必填参数，发音人：xiaoyan(中文 女 晓燕)、xiaofeng(中文 男 晓峰)、catherine(英文 女)
            paramBuilder.param("textEncoding", "UTF-8"); // 非必填，文本编码：GBK或UTF-8
            paramBuilder.param("pitch", pitch); // 非必填，语调：最小值0，最大值100
            paramBuilder.param("volume", volume); // 非必填，音量：最小值0，最大值100
            paramBuilder.param("speed", speed); // 非必填，语速：最小值0，最大值100

            // 开始合成
            ttsHandle = AiHelper.getInst().start(ABILITY_AISOUND, paramBuilder.build(), null);
            if (ttsHandle.getCode() != 0) {
                closeTTSFileStream();
                callback.onError("启动合成失败: " + ttsHandle.getCode());
                return;
            }

            // 写入文本数据
            AiRequest.Builder dataBuilder = AiRequest.builder();
            AiText input = AiText.get("text").data(text).valid();
            dataBuilder.payload(input);

            int ret = AiHelper.getInst().write(dataBuilder.build(), ttsHandle);
            if (ret != 0) {
                closeTTSFileStream();
                if (ttsHandle != null) {
                    AiHelper.getInst().end(ttsHandle);
                    ttsHandle = null;
                }
                callback.onError("写入文本失败: " + ret);
                return;
            }

            callback.onSuccess("合成已启动");

        } catch (Exception e) {
            Log.e(TAG, "StartTTS error: " + e.getMessage(), e);
            closeTTSFileStream();
            callback.onError("启动合成失败: " + e.getMessage());
        }
    }

    /**
     * 开始语音合成（XTTS版本）
     */
    public void startXTTS(String text, String vcn, int language, int speed, int pitch, int volume,
            TTSResultCallback callback) {
        try {
            // 关闭之前的文件流（如果存在）
            closeXTTSFileStream();

            // 生成输出文件名（使用时间戳）
            String fileName = "OutPut_" + System.currentTimeMillis() + ".pcm";
            currentXttsFilePath = ttsOutputDir + File.separator + fileName;
            File outputFile = new File(currentXttsFilePath);
            outputFile.getParentFile().mkdirs();

            // 创建文件输出流
            try {
                xttsFileOutputStream = new FileOutputStream(outputFile, false);
            } catch (IOException e) {
                Log.e(TAG, "创建XTTS输出文件失败: " + e.getMessage());
                callback.onError("创建输出文件失败: " + e.getMessage());
                return;
            }

            // 注册监听器（只在第一次调用时注册，避免重复注册）
            if (!isXTTSListenerRegistered) {
                AiHelper.getInst().registerListener(ABILITY_XTTS, new AiListener() {
                    @Override
                    public void onResult(int handleID, List<AiResponse> list, Object usrContext) {
                        if (list != null && list.size() > 0) {
                            // 每次收到数据就追加写入文件
                            for (AiResponse response : list) {
                                byte[] bytes = response.getValue();
                                if (bytes != null && bytes.length > 0) {
                                    try {
                                        if (xttsFileOutputStream != null) {
                                            xttsFileOutputStream.write(bytes);
                                            xttsFileOutputStream.flush();
                                            Log.d(TAG, "写入XTTS数据: " + bytes.length + " bytes, handleID: " + handleID);
                                        }
                                    } catch (IOException e) {
                                        Log.e(TAG, "写入XTTS文件失败: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onEvent(int handleID, int event, List<AiResponse> eventData, Object usrContext) {
                        if (event == com.iflytek.aikit.core.AeeEvent.AEE_EVENT_END.getValue()) {
                            // 合成结束，关闭文件流并发送结果事件
                            closeXTTSFileStream();
                            if (xttsHandle != null) {
                                AiHelper.getInst().end(xttsHandle);
                                xttsHandle = null;
                            }
                            // 发送合成完成事件
                            JSONObject data = createResultData("filePath", currentXttsFilePath);
                            // 这里XTTS没有传fileName参数，但我们可以根据路径获取或以后扩展
                            sendEvent("xttsResult", data);
                            Log.d(TAG, "XTTS合成完成，文件路径: " + currentXttsFilePath);
                        } else if (event == com.iflytek.aikit.core.AeeEvent.AEE_EVENT_PROGRESS.getValue()) {
                            // 处理进度事件
                            int pos = -1;
                            int len = -1;
                            if (eventData != null) {
                                for (AiResponse aiOutput : eventData) {
                                    if (aiOutput.getKey().equals("progress_pos")) {
                                        byte[] posBytes = aiOutput.getValue();
                                        if (posBytes != null && posBytes.length == 4) {
                                            pos = bytesToInt(posBytes);
                                        }
                                    } else if (aiOutput.getKey().equals("progress_len")) {
                                        byte[] lenBytes = aiOutput.getValue();
                                        if (lenBytes != null && lenBytes.length == 4) {
                                            len = bytesToInt(lenBytes);
                                        }
                                    }
                                }
                            }
                            if (pos >= 0 && len > 0) {
                                JSONObject progressData = new JSONObject();
                                try {
                                    progressData.put("pos", pos);
                                    progressData.put("len", len);
                                    progressData.put("progress", (int) (pos * 100.0 / len));
                                } catch (JSONException e) {
                                    Log.e(TAG, "创建进度数据失败: " + e.getMessage());
                                }
                                sendEvent("xttsProgress", progressData);
                                Log.d(TAG, "XTTS合成进度: " + pos + "/" + len);
                            }
                        }
                    }

                    @Override
                    public void onError(int handleID, int err, String msg, Object usrContext) {
                        // 错误时关闭文件流
                        closeXTTSFileStream();
                        sendEvent("xttsError", createErrorData(err, msg));
                        Log.e(TAG, "XTTS合成错误: handleID=" + handleID + ", err=" + err + ", msg=" + msg);
                    }
                });
                isXTTSListenerRegistered = true;
                Log.d(TAG, "XTTS监听器已注册");
            }

            // 构建参数
            AiInput.Builder paramBuilder = AiInput.builder();
            paramBuilder.param("vcn", vcn); // 必填参数，发音人
            paramBuilder.param("language", language); // 必填参数，语种：1-中文, 2-英文, 3-法语等
            paramBuilder.param("textEncoding", "UTF-8"); // 非必填，文本编码
            paramBuilder.param("pitch", pitch); // 非必填，语调：0-100
            paramBuilder.param("volume", volume); // 非必填，音量：0-100
            paramBuilder.param("speed", speed); // 非必填，语速：0-100

            // 开始合成
            xttsHandle = AiHelper.getInst().start(ABILITY_XTTS, paramBuilder.build(), null);
            if (xttsHandle.getCode() != 0) {
                closeXTTSFileStream();
                callback.onError("启动合成失败: " + xttsHandle.getCode());
                return;
            }
            Log.d(TAG, "XTTS启动成功: handleID=" + xttsHandle.getI() + ", id=" + xttsHandle.getId());

            // 写入文本数据
            AiRequest.Builder dataBuilder = AiRequest.builder();
            AiText input = AiText.get("text").data(text).valid();
            dataBuilder.payload(input);

            int ret = AiHelper.getInst().write(dataBuilder.build(), xttsHandle);
            if (ret != 0) {
                closeXTTSFileStream();
                if (xttsHandle != null) {
                    AiHelper.getInst().end(xttsHandle);
                    xttsHandle = null;
                }
                callback.onError("写入文本失败: " + ret);
                return;
            }

            callback.onSuccess("合成已启动");

        } catch (Exception e) {
            Log.e(TAG, "StartXTTS error: " + e.getMessage(), e);
            closeXTTSFileStream();
            callback.onError("启动合成失败: " + e.getMessage());
        }
    }

    /**
     * 关闭TTS文件输出流
     */
    private void closeTTSFileStream() {
        if (ttsFileOutputStream != null) {
            try {
                ttsFileOutputStream.flush();
                ttsFileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭TTS文件流失败: " + e.getMessage());
            } finally {
                ttsFileOutputStream = null;
            }
        }
    }

    /**
     * 关闭XTTS文件输出流
     */
    private void closeXTTSFileStream() {
        if (xttsFileOutputStream != null) {
            try {
                xttsFileOutputStream.flush();
                xttsFileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭XTTS文件流失败: " + e.getMessage());
            } finally {
                xttsFileOutputStream = null;
            }
        }
    }

    /**
     * 将字节数组转换为int
     */
    private int bytesToInt(byte[] bytes) {
        if (bytes == null || bytes.length != 4) {
            return 0;
        }
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) |
                ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
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
