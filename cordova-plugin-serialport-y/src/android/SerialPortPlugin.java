package com.yy.serialport;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import com.cl.serialportlibrary.SimpleSerialPortManager;

public class SerialPortPlugin extends CordovaPlugin {
    private SimpleSerialPortManager serialPortManager;
    private CallbackContext readCallback;
    private boolean isInitialized = false;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("init")) {
            String port = args.getString(0);
            int baudRate = args.getInt(1);
            int intervalSleep = args.optInt(2, 50);
            boolean enableLog = args.optBoolean(3, false);
            String logTag = args.optString(4, "SerialPort");
            int databits = args.optInt(5, 8);
            int parity = args.optInt(6, 0);
            int stopbits = args.optInt(7, 1);
            int strategy = args.optInt(8, 0);
            this.initSerialPort(port, baudRate, intervalSleep, enableLog, logTag, databits, parity, stopbits, strategy, callbackContext);
            return true;
        } else if (action.equals("listen")) {
            this.setDataListener(callbackContext);
            return true;
        } else if (action.equals("sendBytes")) {
            String hexData = args.getString(0);
            this.sendBytes(hexData, callbackContext);
            return true;
        } else if (action.equals("sendString")) {
            String data = args.getString(0);
            this.sendString(data, callbackContext);
            return true;
        } else if (action.equals("close")) {
            this.closeSerialPort(callbackContext);
            return true;
        }
        return false;
    }

    private void initSerialPort(String port, int baudRate, int intervalSleep, boolean enableLog, 
                                String logTag, int databits, int parity, int stopbits, int strategy,
                                CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                // 如果已经初始化，先关闭之前的串口
                if (isInitialized && serialPortManager != null) {
                    try {
                        serialPortManager.closeSerialPort();
                    } catch (Exception e) {
                        // 忽略关闭时的错误，继续重新初始化
                    }
                    serialPortManager = null;
                    readCallback = null;
                }

                // 配置串口参数
                try {
                    new SimpleSerialPortManager.QuickConfig()
                        .setIntervalSleep(intervalSleep)
                        .setEnableLog(enableLog)
                        .setLogTag(logTag)
                        .setDatabits(databits)
                        .setParity(parity)
                        .setStopbits(stopbits)
                        .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.values()[strategy])
                        .apply(cordova.getActivity().getApplication());
                } catch (Exception configException) {
                    isInitialized = false;
                    callbackContext.error("Failed to configure serial port: " + configException.getMessage());
                    return;
                }

                // 打开串口
                serialPortManager = SimpleSerialPortManager.getInstance();
                boolean openResult = serialPortManager.openSerialPort(port, baudRate, data -> {
                    // 数据接收回调，如果有监听器则推送数据
                    CallbackContext currentCallback = readCallback;
                    if (currentCallback != null) {
                        String hex = bytesToHex(data);
                        PluginResult dataResult = new PluginResult(PluginResult.Status.OK, hex);
                        dataResult.setKeepCallback(true);
                        currentCallback.sendPluginResult(dataResult);
                    }
                });
                
                if (openResult) {
                    isInitialized = true;
                    callbackContext.success("Serial port initialized and opened successfully");
                } else {
                    isInitialized = false;
                    serialPortManager = null;
                    callbackContext.error("Failed to open serial port: " + port + " at " + baudRate);
                }
            } catch (Exception e) {
                isInitialized = false;
                callbackContext.error(e.getMessage());
            }
        });
    }

    private void setDataListener(CallbackContext callbackContext) {
        // 设置数据接收监听回调
        this.readCallback = callbackContext;
        // 返回成功并保持回调，以便后续持续推送数据
        PluginResult successResult = new PluginResult(PluginResult.Status.OK, "Data listener set");
        successResult.setKeepCallback(true);
        callbackContext.sendPluginResult(successResult);
    }
 
    // 将字节数组转换为大写十六进制字符串（无空格），例如: 0x0A -> "0A"
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // 将十六进制字符串转换为字节数组，例如: "0A1B2C" -> [0x0A, 0x1B, 0x2C]
    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() == 0) {
            return new byte[0];
        }
        // 移除空格和常见分隔符
        hex = hex.replaceAll("\\s+", "").replaceAll("[-:]", "");
        // 确保长度为偶数
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * 发送字节数组（hex 字符串转换为字节数组）
     */
    private void sendBytes(String hexData, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                if (!isInitialized || serialPortManager == null) {
                    callbackContext.error("Serial port is not initialized. Please call init() first.");
                    return;
                }
                // 将 hex 字符串转换为字节数组
                byte[] bytes = hexToBytes(hexData);
                // 使用 sendData(byte[]) 方法发送字节数组
                serialPortManager.sendData(bytes);
                callbackContext.success("Bytes sent successfully");
            } catch (Exception e) {
                callbackContext.error("Failed to send bytes: " + e.getMessage());
            }
        });
    }

    /**
     * 发送字符串
     */
    private void sendString(String data, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                if (!isInitialized || serialPortManager == null) {
                    callbackContext.error("Serial port is not initialized. Please call init() first.");
                    return;
                }
                // 直接使用 sendData(String) 方法发送字符串
                serialPortManager.sendData(data);
                callbackContext.success("String sent successfully");
            } catch (Exception e) {
                callbackContext.error("Failed to send string: " + e.getMessage());
            }
        });
    }

    private void closeSerialPort(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                if (serialPortManager != null) {
                    serialPortManager.closeSerialPort();
                }
                // 关闭时清理状态
                serialPortManager = null;
                readCallback = null;
                isInitialized = false;
                callbackContext.success("Serial port closed successfully");
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
        });
    }
}