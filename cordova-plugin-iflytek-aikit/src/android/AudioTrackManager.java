package com.iflytek.aikit.plugin;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * 音频播放管理器
 * 用于播放PCM格式的音频文件
 * 参考原生代码 AudioTrackManager.java
 */
public class AudioTrackManager {
    private static final String TAG = "AudioTrackManager";

    private AudioTrack mAudioTrack;
    private DataInputStream mDis; // 播放文件的数据流
    private Thread mRecordThread;
    private boolean isStart = false;
    private volatile static AudioTrackManager mInstance;

    // 音频流类型
    private static final int mStreamType = AudioManager.STREAM_MUSIC;
    // 指定采样率
    public static final int mSampleRateIn16KHz = 16000;
    public static final int mSampleRateIn24KHz = 24000;
    // 指定捕获音频的声道数目。在AudioFormat类中指定用于此的常量
    private static final int mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO; // 单声道
    // 指定音频量化位数
    private static final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    // 指定缓冲区大小
    private int mMinBufferSize;
    // STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中
    private static int mMode = AudioTrack.MODE_STREAM;
    private int mSampleRate = 16000;

    public enum sampleRateType {
        SAMPLE_RATE_16k,
        SAMPLE_RATE_24k
    }

    public AudioTrackManager() {
        initData();
    }

    private void initData() {
        Log.d(TAG, "AudioTrackManager:sampleRate=" + mSampleRate);
        // 根据采样率，采样精度，单双声道来得到frame的大小
        mMinBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat); // 计算最小缓冲区
        // 创建AudioTrack
        mAudioTrack = new AudioTrack(mStreamType, mSampleRate, mChannelConfig,
                mAudioFormat, mMinBufferSize, mMode);
    }

    public void setSampleRate(sampleRateType sampleRate) {
        int newSampleRate = mSampleRate;
        switch (sampleRate) {
            case SAMPLE_RATE_16k:
                newSampleRate = mSampleRateIn16KHz;
                break;
            case SAMPLE_RATE_24k:
                newSampleRate = mSampleRateIn24KHz;
                break;
        }
        // 如果采样率改变，重新初始化数据
        if (newSampleRate != mSampleRate) {
            mSampleRate = newSampleRate;
            // 停止当前播放
            stopPlay();
            // 重新初始化数据
            initData();
        }
    }

    /**
     * 获取单例引用
     *
     * @return
     */
    public static AudioTrackManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioTrackManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioTrackManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 销毁线程方法
     */
    private void destroyThread() {
        try {
            isStart = false;
            if (null != mRecordThread && Thread.State.RUNNABLE == mRecordThread.getState()) {
                try {
                    Thread.sleep(100);
                    mRecordThread.interrupt();
                } catch (Exception e) {
                    mRecordThread = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mRecordThread = null;
        }
    }

    /**
     * 启动线程方法
     */
    private void startThread() {
        destroyThread();
        isStart = true;
        if (mRecordThread == null) {
            mRecordThread = new Thread(playRunnable);
            mRecordThread.start();
        }
    }

    /**
     * 播放线程
     */
    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // 设置线程的优先级
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                byte[] tempBuffer = new byte[mMinBufferSize];
                int readCount = 0;
                while (mDis.available() > 0) {
                    readCount = mDis.read(tempBuffer);
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                        continue;
                    }
                    if (readCount != 0 && readCount != -1) { // 一边播放一边写入语音数据
                        // 判断AudioTrack未初始化，停止播放的时候释放了，状态就为STATE_UNINITIALIZED
                        if (mAudioTrack.getState() == mAudioTrack.STATE_UNINITIALIZED) {
                            initData();
                        }
                        mAudioTrack.play();
                        mAudioTrack.write(tempBuffer, 0, readCount);
                    }
                }
                stopPlay(); // 播放完就停止播放
            } catch (Exception e) {
                Log.e(TAG, "播放线程异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
    };

    /**
     * 播放文件
     *
     * @param path
     * @throws Exception
     */
    private void setPath(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            throw new Exception("音频文件不存在: " + path);
        }
        mDis = new DataInputStream(new FileInputStream(file));
    }

    /**
     * 启动播放
     *
     * @param path
     */
    public void startPlay(String path) {
        try {
            setPath(path);
            startThread();
        } catch (Exception e) {
            Log.e(TAG, "启动播放失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止播放
     */
    public void stopPlay() {
        try {
            destroyThread(); // 销毁线程
            if (mAudioTrack != null) {
                if (mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) { // 初始化成功
                    mAudioTrack.stop(); // 停止播放
                }
                if (mAudioTrack != null) {
                    mAudioTrack.release(); // 释放audioTrack资源
                }
            }
            if (mDis != null) {
                mDis.close(); // 关闭数据输入流
            }
            Log.d(TAG, "停止播放");
        } catch (Exception e) {
            Log.e(TAG, "停止播放异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取播放状态
     *
     * @return
     */
    public int getPlayState() {
        if (mAudioTrack != null) {
            return mAudioTrack.getPlayState();
        }
        return AudioTrack.PLAYSTATE_STOPPED;
    }
}
