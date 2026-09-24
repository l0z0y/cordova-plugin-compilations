package com.yztiot.cordova;

import com.yztiot.yztitoapi.yztiotManager;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class YzPlugin extends CordovaPlugin {
    /** GPIO 节点基路径：设备实际为 /proc/yz_gpio（新 AAR 的 Gpio 类基路径亦为此），插件直接读写节点文件以支持 observeGpioValue 轮询 */
    private static final String GPIO_BASE_PATH = "/proc/yz_gpio";
    private yztiotManager manager;
    private final ExecutorService gpioExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "yz-gpio-poller");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, GpioSubscription> gpioSubscriptions = new HashMap<>();
    private final Object gpioLock = new Object();

    private static final class GpioSubscription {
        final CallbackContext callback;
        Future<?> future;
        volatile boolean running;
        boolean hasValue;
        int lastValue = -1;

        GpioSubscription(CallbackContext callback) {
            this.callback = callback;
        }
    }

    @Override public void pluginInitialize() {
        manager = yztiotManager.GetInstance();
        manager.setContext(cordova.getActivity().getApplicationContext());
    }

    @Override public boolean execute(String action, JSONArray args, CallbackContext cb) throws JSONException {
        try {
            if (manager == null) pluginInitialize();
            if (action.equals("getDeviceVersion")) return result(cb, yztiotManager.getDeviceVersion());
            if (action.equals("getAndroidVersion")) return result(cb, yztiotManager.getAndroidVersion());
            if (action.equals("observeGpioValue")) return observeGpioValue(args, cb);
            if (action.equals("stopObservingGpioValue")) return stopObservingGpioValue(args, cb);
            if (action.equals("observeTouchMode")) return observeTouchMode(args, cb);
            if (action.equals("stopObservingTouchMode")) return stopObservingTouchMode(args, cb);
            return executeManager(action, args, cb);
        } catch (Exception e) { cb.error(message(e)); return true; }
    }

    private boolean executeManager(String a, JSONArray x, CallbackContext cb) throws Exception {
        if (a.equals("getJARVersion")) return result(cb, manager.getJARVersion());
        if (a.equals("getDeviceID")) return result(cb, manager.getDeviceID());
        if (a.equals("getSDKVersion")) return result(cb, manager.getSDKVersion());
        if (a.equals("getTotalInternalMemorySize")) return result(cb, manager.getTotalInternalMemorySize());
        if (a.equals("getFreeMemorySize")) return result(cb, manager.getFreeMemorySize());
        if (a.equals("getInternalSDCardPath")) return result(cb, manager.getInternalSDCardPath());
        if (a.equals("getExternalSDCardPath")) return result(cb, manager.getExternalSDCardPath());
        if (a.equals("getUsbStoragePath")) return result(cb, manager.getUsbStoragePath());
        if (a.equals("isAppExist")) return result(cb, manager.isAppExist(x.getString(0)));
        if (a.equals("isExist")) return result(cb, manager.isExist(x.getString(0)));
        if (a.equals("getGpioValue")) return result(cb, readGpioFile(x.getString(0)));
        if (a.equals("getTouchMode")) return result(cb, manager.getTouchMode());
        if (a.equals("getSystemDate")) return result(cb, manager.getSystemDate());
        if (a.equals("getSystemTime")) return result(cb, manager.getSystemTime());
        if (a.equals("getDisplayMode")) return result(cb, manager.getDisplayMode());
        if (a.equals("getDisplayHeight")) return result(cb, manager.getDisplayHeight());
        if (a.equals("getDisplayWidth")) return result(cb, manager.getDisplayWidth());
        if (a.equals("getDisplayDensity")) return result(cb, manager.getDisplayDensity());
        if (a.equals("getSystemMaxVolume")) return result(cb, manager.getSystemMaxVolume());
        if (a.equals("getSystemCurrenVolume")) return result(cb, manager.getSystemCurrenVolume());
        if (a.equals("setRaiseSystemVolume")) return result(cb, manager.setRaiseSystemVolume());
        if (a.equals("setLowerSystemVolume")) return result(cb, manager.setLowerSystemVolume());
        if (a.equals("setSystemVolumeIndex")) return result(cb, manager.setSystemVolumeIndex(x.getInt(0)));
        if (a.equals("getSystemMaxBrightness")) return result(cb, manager.getSystemMaxBrightness());
        if (a.equals("getSystemBrightness")) return result(cb, manager.getSystemBrightness());
        if (a.equals("isAutoDateTime")) return result(cb, manager.isAutoDateTime());
        if (a.equals("getIPv4")) return result(cb, manager.getIPv4(x.getString(0)));
        if (a.equals("shutdown")) manager.shutdown();
        else if (a.equals("reboot")) manager.reboot(x.optInt(0, 0));
        else if (a.equals("startSettings")) manager.startSettings();
        else if (a.equals("startWifiSettings")) manager.startWifiSettings();
        else if (a.equals("setGpioValue")) writeGpioFile(x.getString(0), x.getInt(1));
        else if (a.equals("setTouchMode")) manager.setTouchMode(x.getInt(0));
        else if (a.equals("simulatePi4Press")) manager.simulatePi4Press();
        else if (a.equals("startActivity")) manager.startActivity(x.getString(0), x.getString(1));
        else if (a.equals("execSuCmd")) manager.execSuCmd(x.getString(0));
        else if (a.equals("installAppSilent")) manager.installAppSilent(x.getString(0));
        else if (a.equals("uninstallAppSilent")) manager.uninstallAppSilent(x.getString(0));
        else if (a.equals("installAppSilentAndRebootSystem")) manager.installAppSilentAndRebootSystem(x.getString(0), x.getInt(1));
        else if (a.equals("installAppAndStartUp")) manager.installAppAndStartUp(x.getString(0), x.getString(1));
        else if (a.equals("setStaticIP")) manager.setStaticIP(x.getString(0), x.getString(1), x.getString(2), x.getString(3), x.getString(4));
        else if (a.equals("setRotation")) manager.setRotation(x.getInt(0));
        else if (a.equals("inputkeyevent")) manager.inputkeyevent(x.getInt(0));
        else if (a.equals("setDisplayDensity")) manager.setDisplayDensity(x.getInt(0));
        else if (a.equals("increaseBrightness")) manager.increaseBrightness();
        else if (a.equals("decreaseBrightness")) manager.decreaseBrightness();
        else if (a.equals("setBrightness")) manager.setBrightness(x.getInt(0));
        else if (a.equals("setDefaultLauncher")) manager.setDefaultLauncher(x.getString(0), x.getString(1));
        else if (a.equals("startTcpAdb")) manager.startTcpAdb(x.getBoolean(0));
        else if (a.equals("setSystemTime")) manager.setSystemTime(calendar(x));
        else return false;
        cb.success(); return true;
    }

    private boolean observeGpioValue(JSONArray args, CallbackContext callback) throws JSONException {
        String port = args.getString(0);
        long interval = args.optLong(1, 100L);
        if (interval < 20L) interval = 20L;

        GpioSubscription subscription = new GpioSubscription(callback);
        subscription.running = true;
        synchronized (gpioLock) {
            stopGpioLocked(port);
            gpioSubscriptions.put(port, subscription);
            final long period = interval;
            subscription.future = gpioExecutor.submit(() -> {
                while (subscription.running && !Thread.currentThread().isInterrupted()) {
                    pollGpio(port, subscription);
                    try {
                        Thread.sleep(period);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        return true;
    }

    private boolean stopObservingGpioValue(JSONArray args, CallbackContext callback) throws JSONException {
        String port = args.getString(0);
        synchronized (gpioLock) {
            stopGpioLocked(port);
        }
        callback.success();
        return true;
    }

    private void pollGpio(String port, GpioSubscription subscription) {
        int value;
        try {
            value = readGpioFile(port);
        } catch (Exception e) {
            if (!subscription.hasValue) {
                subscription.running = false;
                synchronized (gpioLock) {
                    if (gpioSubscriptions.get(port) == subscription) gpioSubscriptions.remove(port);
                }
                subscription.callback.error("Unable to read GPIO '" + port + "': " + message(e));
            }
            return;
        }
        if (value != 0 && value != 1) {
            if (!subscription.hasValue) {
                subscription.running = false;
                synchronized (gpioLock) {
                    if (gpioSubscriptions.get(port) == subscription) {
                        gpioSubscriptions.remove(port);
                    }
                }
                subscription.callback.error("Unable to read GPIO '" + port + "' (value: " + value + ")");
            }
            return;
        }
        synchronized (gpioLock) {
            if (gpioSubscriptions.get(port) != subscription) return;
            if (subscription.hasValue && subscription.lastValue == value) return;
            subscription.lastValue = value;
            subscription.hasValue = true;
        }
        org.apache.cordova.PluginResult result = new org.apache.cordova.PluginResult(
                org.apache.cordova.PluginResult.Status.OK, value);
        result.setKeepCallback(true);
        subscription.callback.sendPluginResult(result);
    }

    private void stopGpioLocked(String port) {
        GpioSubscription subscription = gpioSubscriptions.remove(port);
        if (subscription != null) {
            subscription.running = false;
            if (subscription.future != null) {
                subscription.future.cancel(true);
            }
        }
    }

    /** 触摸模式监听的固定 key（触摸模式是全局单值，无需端口参数） */
    private static final String TOUCH_MODE_KEY = "__touch_mode__";

    /**
     * 持续监听触摸模式变化（轮询 manager.getTouchMode()）
     * 通知规则：首次读到有效值（0/1）通知一次，之后仅 0↔1 变化时通知；取消订阅后轮询停止
     * @param args [interval] 轮询间隔（毫秒），默认 100，最小 20
     */
    private boolean observeTouchMode(JSONArray args, CallbackContext callback) throws JSONException {
        long interval = args.optLong(0, 100L);
        if (interval < 20L) interval = 20L;

        GpioSubscription subscription = new GpioSubscription(callback);
        subscription.running = true;
        synchronized (gpioLock) {
            stopGpioLocked(TOUCH_MODE_KEY);
            gpioSubscriptions.put(TOUCH_MODE_KEY, subscription);
            final long period = interval;
            subscription.future = gpioExecutor.submit(() -> {
                while (subscription.running && !Thread.currentThread().isInterrupted()) {
                    pollTouchMode(subscription);
                    try {
                        Thread.sleep(period);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        return true;
    }

    private boolean stopObservingTouchMode(JSONArray args, CallbackContext callback) throws JSONException {
        synchronized (gpioLock) {
            stopGpioLocked(TOUCH_MODE_KEY);
        }
        callback.success();
        return true;
    }

    private void pollTouchMode(GpioSubscription subscription) {
        int value;
        try {
            value = manager.getTouchMode();
        } catch (Exception e) {
            if (!subscription.hasValue) {
                subscription.running = false;
                synchronized (gpioLock) {
                    if (gpioSubscriptions.get(TOUCH_MODE_KEY) == subscription) gpioSubscriptions.remove(TOUCH_MODE_KEY);
                }
                subscription.callback.error("Unable to read touch mode: " + message(e));
            }
            return;
        }
        if (value != 0 && value != 1) {
            if (!subscription.hasValue) {
                subscription.running = false;
                synchronized (gpioLock) {
                    if (gpioSubscriptions.get(TOUCH_MODE_KEY) == subscription) {
                        gpioSubscriptions.remove(TOUCH_MODE_KEY);
                    }
                }
                subscription.callback.error("Unable to read touch mode (value: " + value + ")");
            }
            return;
        }
        synchronized (gpioLock) {
            if (gpioSubscriptions.get(TOUCH_MODE_KEY) != subscription) return;
            if (subscription.hasValue && subscription.lastValue == value) return;
            subscription.lastValue = value;
            subscription.hasValue = true;
        }
        org.apache.cordova.PluginResult result = new org.apache.cordova.PluginResult(
                org.apache.cordova.PluginResult.Status.OK, value);
        result.setKeepCallback(true);
        subscription.callback.sendPluginResult(result);
    }

    @Override public void onReset() {
        stopAllGpioObservers();
        super.onReset();
    }

    @Override public void onDestroy() {
        stopAllGpioObservers();
        gpioExecutor.shutdownNow();
        super.onDestroy();
    }

    private void stopAllGpioObservers() {
        synchronized (gpioLock) {
            for (GpioSubscription subscription : gpioSubscriptions.values()) {
                subscription.running = false;
                if (subscription.future != null) subscription.future.cancel(true);
            }
            gpioSubscriptions.clear();
        }
    }

    private Calendar calendar(JSONArray x) throws JSONException {
        Calendar c = Calendar.getInstance();
        if (x.length() > 0 && !x.isNull(0)) c.setTimeInMillis(x.getLong(0));
        return c;
    }

    /**
     * 读取 GPIO 节点值——直接读 /proc/yz_gpio 下对应节点文件
     * @return 0/1；节点不存在/读取失败返回 -1
     */
    private int readGpioFile(String port) {
        java.io.File f = new java.io.File(GPIO_BASE_PATH, port);
        if (!f.exists()) return -1;
        String line = null;
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(f));
            try {
                line = reader.readLine();
            } finally {
                reader.close();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return -1;
        }
        if (line == null) return -1;
        try {
            if ("0".equals(line)) return 0;
            if ("1".equals(line)) return 1;
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 写入 GPIO 节点值——直接写 /proc/yz_gpio 下对应节点文件
     */
    private void writeGpioFile(String port, int value) throws Exception {
        java.io.File f = new java.io.File(GPIO_BASE_PATH, port);
        if (!f.exists()) throw new Exception("GPIO port not found: " + port);
        java.io.FileOutputStream out = new java.io.FileOutputStream(f);
        try {
            byte[] bytes = String.valueOf(value).getBytes();
            out.write(bytes, 0, bytes.length);
            out.flush();
        } finally {
            out.close();
        }
    }

    private boolean result(CallbackContext cb, Object value) { if (value instanceof Boolean) cb.sendPluginResult(new org.apache.cordova.PluginResult(org.apache.cordova.PluginResult.Status.OK, (Boolean)value)); else if (value instanceof Integer) cb.success((Integer)value); else if (value instanceof Long) cb.success(String.valueOf(value)); else if (value instanceof JSONArray) cb.success((JSONArray)value); else cb.success(value == null ? JSONObject.NULL.toString() : String.valueOf(value)); return true; }
    private String message(Exception e) { return e.getMessage() == null ? e.toString() : e.getMessage(); }
}