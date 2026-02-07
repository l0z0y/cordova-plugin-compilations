package com.cordova.floatingwindow;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * FloatingWindowService - 后台服务
 * 用于管理悬浮窗的生命周期
 */
public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    public static final String ACTION_SHOW_FLOATING_WINDOW = "com.cordova.floatingwindow.SHOW";
    public static final String ACTION_HIDE_FLOATING_WINDOW = "com.cordova.floatingwindow.HIDE";
    private static final String CHANNEL_ID = "FloatingWindowServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private FloatingWindowView floatingWindowView;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            
            if (ACTION_SHOW_FLOATING_WINDOW.equals(action)) {
                String imagePath = intent.getStringExtra("imagePath");
                int width = intent.getIntExtra("width", 100);
                int height = intent.getIntExtra("height", 100);
                showFloatingWindow(imagePath, width, height);
            } else if (ACTION_HIDE_FLOATING_WINDOW.equals(action)) {
                hideFloatingWindow();
            }
        }
        
        return START_STICKY; // 服务被杀死后自动重启
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideFloatingWindow();
        Log.d(TAG, "Service destroyed");
    }

    /**
     * 显示悬浮窗
     */
    private void showFloatingWindow(String imagePath, int width, int height) {
        if (floatingWindowView == null) {
            floatingWindowView = new FloatingWindowView(this);
        }
        floatingWindowView.show(imagePath, width, height);
        Log.d(TAG, "Floating window shown");
    }

    /**
     * 隐藏悬浮窗
     */
    private void hideFloatingWindow() {
        if (floatingWindowView != null) {
            floatingWindowView.hide();
            floatingWindowView = null;
        }
        Log.d(TAG, "Floating window hidden");
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Floating Window Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for floating window");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮窗服务")
            .setContentText("后台服务正在运行")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
}
