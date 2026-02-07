package com.cordova.floatingwindow;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;
import android.Manifest;

import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.Permission;
import com.hjq.permissions.OnPermissionCallback;
import java.util.List;

/**
 * FloatingWindowPlugin - Cordova插件主类
 * 提供悬浮窗、后台服务和跳转桌面功能
 */
public class FloatingWindowPlugin extends CordovaPlugin {
    private static final String TAG = "FloatingWindowPlugin";
    private FloatingWindowService floatingWindowService;
    private CallbackContext eventCallbackContext;
    private BroadcastReceiver eventReceiver;
    private CallbackContext permissionCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "FloatingWindowPlugin initialized");
        
        // 注册事件接收器
        eventReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.cordova.floatingwindow.EVENT".equals(intent.getAction())) {
                    String eventType = intent.getStringExtra("eventType");
                    String data = intent.getStringExtra("data");
                    
                    try {
                        JSONObject eventData = new JSONObject();
                        if (data != null) {
                            eventData.put("message", data);
                        }
                        sendEvent(eventType, eventData);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to create event data", e);
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.cordova.floatingwindow.EVENT");
        cordova.getActivity().registerReceiver(eventReceiver, filter);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Context context = this.cordova.getActivity().getApplicationContext();

        if (action.equals("startService")) {
            startService(context, callbackContext);
            return true;
        } else if (action.equals("stopService")) {
            stopService(context, callbackContext);
            return true;
        } else if (action.equals("showFloatingWindow")) {
            String imagePath = args.getString(0);
            int width = args.optInt(1, 100);
            int height = args.optInt(2, 100);
            showFloatingWindow(context, imagePath, width, height, callbackContext);
            return true;
        } else if (action.equals("hideFloatingWindow")) {
            hideFloatingWindow(context, callbackContext);
            return true;
        } else if (action.equals("goToHome")) {
            goToHome(callbackContext);
            return true;
        } else if (action.equals("checkPermission")) {
            checkPermission(context, callbackContext);
            return true;
        } else if (action.equals("requestPermission")) {
            requestPermission(callbackContext);
            return true;
        } else if (action.equals("setEventListener")) {
            setEventListener(callbackContext);
            return true;
        }

        return false;
    }

    /**
     * 启动后台服务
     */
    private void startService(Context context, CallbackContext callbackContext) {
        try {
            Intent serviceIntent = new Intent(context, FloatingWindowService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            callbackContext.success("Service started");
            Log.d(TAG, "Service started");
        } catch (Exception e) {
            callbackContext.error("Failed to start service: " + e.getMessage());
            Log.e(TAG, "Failed to start service", e);
        }
    }

    /**
     * 停止后台服务
     */
    private void stopService(Context context, CallbackContext callbackContext) {
        try {
            Intent serviceIntent = new Intent(context, FloatingWindowService.class);
            context.stopService(serviceIntent);
            callbackContext.success("Service stopped");
            Log.d(TAG, "Service stopped");
        } catch (Exception e) {
            callbackContext.error("Failed to stop service: " + e.getMessage());
            Log.e(TAG, "Failed to stop service", e);
        }
    }

    /**
     * 显示悬浮窗
     */
    private void showFloatingWindow(Context context, String imagePath, int width, int height, CallbackContext callbackContext) {
        try {
            // 使用 XXPermissions 检查权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!XXPermissions.isGranted(cordova.getActivity(), Permission.SYSTEM_ALERT_WINDOW)) {
                    callbackContext.error("Permission denied: SYSTEM_ALERT_WINDOW. Please request permission first.");
                    return;
                }
            }

            Intent serviceIntent = new Intent(context, FloatingWindowService.class);
            serviceIntent.setAction(FloatingWindowService.ACTION_SHOW_FLOATING_WINDOW);
            serviceIntent.putExtra("imagePath", imagePath);
            serviceIntent.putExtra("width", width);
            serviceIntent.putExtra("height", height);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            callbackContext.success("Floating window shown");
            Log.d(TAG, "Floating window shown");
        } catch (Exception e) {
            callbackContext.error("Failed to show floating window: " + e.getMessage());
            Log.e(TAG, "Failed to show floating window", e);
        }
    }

    /**
     * 隐藏悬浮窗
     */
    private void hideFloatingWindow(Context context, CallbackContext callbackContext) {
        try {
            Intent serviceIntent = new Intent(context, FloatingWindowService.class);
            serviceIntent.setAction(FloatingWindowService.ACTION_HIDE_FLOATING_WINDOW);
            context.startService(serviceIntent);
            callbackContext.success("Floating window hidden");
            Log.d(TAG, "Floating window hidden");
        } catch (Exception e) {
            callbackContext.error("Failed to hide floating window: " + e.getMessage());
            Log.e(TAG, "Failed to hide floating window", e);
        }
    }

    /**
     * 跳转到桌面
     */
    private void goToHome(CallbackContext callbackContext) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.cordova.getActivity().startActivity(intent);
            callbackContext.success("Navigated to home");
            Log.d(TAG, "Navigated to home");
        } catch (Exception e) {
            callbackContext.error("Failed to go to home: " + e.getMessage());
            Log.e(TAG, "Failed to go to home", e);
        }
    }

    /**
     * 检查悬浮窗权限
     */
    private void checkPermission(Context context, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean hasPermission = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // 使用 XXPermissions 检查悬浮窗权限
                        hasPermission = XXPermissions.isGranted(cordova.getActivity(), Permission.SYSTEM_ALERT_WINDOW);
                    } else {
                        hasPermission = true;
                    }
                    
                    final boolean finalHasPermission = hasPermission;
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callbackContext.success(finalHasPermission ? 1 : 0);
                            Log.d(TAG, "Permission check: " + finalHasPermission);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to check permission", e);
                    callbackContext.error("Failed to check permission: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 请求悬浮窗权限（使用 XXPermissions）
     */
    private void requestPermission(CallbackContext callbackContext) {
        this.permissionCallbackContext = callbackContext;
        
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // 使用 XXPermissions 请求悬浮窗权限
                        XXPermissions.with(cordova.getActivity())
                            .permission(Permission.SYSTEM_ALERT_WINDOW)
                            .request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(List<String> permissions, boolean all) {
                                    Log.d(TAG, "Permission granted");
                                    if (permissionCallbackContext != null) {
                                        permissionCallbackContext.success("Permission granted");
                                        permissionCallbackContext = null;
                                    }
                                }

                                @Override
                                public void onDenied(List<String> permissions, boolean never) {
                                    Log.d(TAG, "Permission denied, never: " + never);
                                    if (permissionCallbackContext != null) {
                                        if (never) {
                                            // 用户选择了"不再询问"
                                            permissionCallbackContext.error("Permission denied permanently");
                                        } else {
                                            permissionCallbackContext.error("Permission denied");
                                        }
                                        permissionCallbackContext = null;
                                    }
                                }
                            });
                    } else {
                        // Android 6.0 以下不需要权限
                        callbackContext.success("Permission not required for this Android version");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to request permission", e);
                    callbackContext.error("Failed to request permission: " + e.getMessage());
                    permissionCallbackContext = null;
                }
            }
        });
    }

    /**
     * 设置事件监听器
     */
    private void setEventListener(CallbackContext callbackContext) {
        this.eventCallbackContext = callbackContext;
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
        Log.d(TAG, "Event listener set");
    }

    /**
     * 发送事件到JavaScript
     */
    public void sendEvent(String eventType, JSONObject data) {
        if (eventCallbackContext != null) {
            try {
                JSONObject event = new JSONObject();
                event.put("type", eventType);
                event.put("data", data);
                
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, event);
                pluginResult.setKeepCallback(true);
                eventCallbackContext.sendPluginResult(pluginResult);
                Log.d(TAG, "Event sent: " + eventType);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to send event", e);
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (eventReceiver != null) {
            try {
                cordova.getActivity().unregisterReceiver(eventReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister receiver", e);
            }
            eventReceiver = null;
        }
        if (eventCallbackContext != null) {
            eventCallbackContext = null;
        }
    }
}
