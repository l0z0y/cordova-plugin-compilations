package com.cordova.YUtils;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class echoes a string called from JavaScript.
 */
public class cordovaYUtils extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        } else if (action.equals("executeCmd")) {
            String cmd = args.getString(0);
            this.executeCmd(cmd, callbackContext);
            return true;
        } else if (action.equals("writeFileWithGBK")) {
            String filePath = args.getString(0);
            String content = args.getString(1);
            this.writeFileWithGBK(filePath, content, callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void executeCmd(String cmd, CallbackContext callbackContext) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader br = null;
        InputStreamReader reader = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            cmd = cmd + "\n";

            os.write(cmd.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            reader = new InputStreamReader(process.getInputStream());
            br = new BufferedReader(reader);
            StringBuilder buffer = new StringBuilder();

            StringBuilder result = null;
            String line;
            while ((line = br.readLine()) != null) {
                result = (result == null ? new StringBuilder() : result).append(line).append("\n");
            }
            String[] arr = {};
            if (result != null) {
                arr = result.toString().split("\\s+");
            }
            for (String value : arr) {
                buffer.append(value).append("\n");
            }
            callbackContext.success(buffer.toString());

        } catch (Exception e) {
            callbackContext.error(e.toString());
        } finally {
            if (process != null) {
                process.destroy();
            }

            try {
                if (os != null) {
                    os.close();
                }

                if (reader != null) {
                    reader.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                callbackContext.error("Failed to close");

            }

        }

    }

     private void writeFileWithGBK(String filePath, String content, CallbackContext callbackContext) {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        try {
            // 日志：开始写入
            android.util.Log.d("YUtils", "开始写入文件 GBK 编码: " + filePath);
            android.util.Log.d("YUtils", "内容长度: " + content.length());
            
            // 处理 file:// 协议的路径
            String realPath = filePath;
            if (filePath.startsWith("file://")) {
                realPath = filePath.replace("file://", "");
                android.util.Log.d("YUtils", "转换后的路径: " + realPath);
            }
            
            File file = new File(realPath);
            
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                if (!parentDir.exists()) {
                    android.util.Log.d("YUtils", "父目录不存在，正在创建: " + parentDir.getAbsolutePath());
                    boolean mkdirResult = parentDir.mkdirs();
                    android.util.Log.d("YUtils", "创建目录结果: " + mkdirResult);
                    
                    if (!parentDir.exists()) {
                        callbackContext.error("无法创建目录: " + parentDir.getAbsolutePath());
                        return;
                    }
                } else {
                    android.util.Log.d("YUtils", "父目录已存在: " + parentDir.getAbsolutePath());
                }
            }
            
            // 使用 GBK 编码写入文件
            android.util.Log.d("YUtils", "开始创建 FileOutputStream");
            fos = new FileOutputStream(file, false); // false 表示覆盖写入
            osw = new OutputStreamWriter(fos, Charset.forName("GBK"));
            
            android.util.Log.d("YUtils", "开始写入内容");
            osw.write(content);
            osw.flush();
            
            android.util.Log.d("YUtils", "文件写入成功");
            callbackContext.success("文件写入成功（GBK编码）: " + filePath);
        } catch (Exception e) {
            android.util.Log.e("YUtils", "文件写入失败: " + e.getMessage(), e);
            callbackContext.error("文件写入失败: " + e.getMessage() + "\n路径: " + filePath);
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                android.util.Log.e("YUtils", "关闭流失败: " + e.getMessage());
            }
        }
    }

}
