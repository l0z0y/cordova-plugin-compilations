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

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("init")) {
            int intervalSleep = args.optInt(0, 50);
            boolean enableLog = args.optBoolean(1, false);
            String logTag = args.optString(2, "SerialPort");
            int databits = args.optInt(3, 8);
            int parity = args.optInt(4, 0);
            int stopbits = args.optInt(5, 1);
            int strategy = args.optInt(6, 0);

             new SimpleSerialPortManager.QuickConfig()
                .setIntervalSleep(intervalSleep)
                .setEnableLog(enableLog)
                .setLogTag(logTag)
                .setDatabits(databits)
                .setParity(parity)
                .setStopbits(stopbits)
                .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.values()[strategy]).apply(cordova.getActivity().getApplication());

            callbackContext.success("Serial port config initialized");
        } else if (action.equals("open")) {
            String port = args.getString(0);
            int baudRate = args.getInt(1);
            this.openSerialPort(port, baudRate, callbackContext);
            return true;
        } else if (action.equals("send")) {
            String data = args.getString(0);
            this.sendData(data, callbackContext);
            return true;
        } else if (action.equals("close")) {
            this.closeSerialPort(callbackContext);
            return true;
        }
        return false;
    }

    private void openSerialPort(String port, int baudRate, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                serialPortManager = SimpleSerialPortManager.getInstance();
                serialPortManager.openSerialPort(port, baudRate, data -> {
                    CallbackContext currentCallback = readCallback;
                    if (currentCallback != null) {
                        String hex = bytesToHex(data);
                        PluginResult dataResult = new PluginResult(PluginResult.Status.OK, hex);
                        dataResult.setKeepCallback(true);
                        currentCallback.sendPluginResult(dataResult);
                    }
                });
                // 保存回调用于后续持续推送数据
                this.readCallback = callbackContext;
                // 初始返回一个占位结果并保持回调
                PluginResult initResult = new PluginResult(PluginResult.Status.NO_RESULT);
                initResult.setKeepCallback(true);
                callbackContext.sendPluginResult(initResult);
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
        });
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

    private void sendData(String data, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                serialPortManager.sendData(data);
                callbackContext.success("Data sent successfully");
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
        });
    }

    private void closeSerialPort(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                serialPortManager.closeSerialPort();
                // 关闭时清理回调
                readCallback = null;
                callbackContext.success("Serial port closed successfully");
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
        });
    }
}