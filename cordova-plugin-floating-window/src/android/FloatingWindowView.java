package com.cordova.floatingwindow;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.util.Log;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * FloatingWindowView - 悬浮窗视图管理类
 * 负责创建和管理悬浮窗的显示
 */
public class FloatingWindowView {
    private static final String TAG = "FloatingWindowView";
    private Context context;
    private WindowManager windowManager;
    private View floatingView;
    private ImageView imageView;
    private WindowManager.LayoutParams params;
    private Handler mainHandler;

    public FloatingWindowView(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 显示悬浮窗
     */
    public void show(String imagePath, int width, int height) {
        hide(); // 先隐藏之前的悬浮窗

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // 创建悬浮窗视图
                    floatingView = new ImageView(context);
                    imageView = (ImageView) floatingView;
                    
                    // 设置图片
                    loadImage(imagePath, imageView);
                    
                    // 设置窗口参数
                    params = new WindowManager.LayoutParams(
                        width,
                        height,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                    );

                    // 设置位置（默认右上角）
                    params.gravity = Gravity.TOP | Gravity.END;
                    params.x = 0;
                    params.y = 100;
                    
                    // 设置点击事件
                    floatingView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 发送点击事件
                            sendEvent("click", null);
                        }
                    });

                    // 设置拖拽事件
                    floatingView.setOnTouchListener(new FloatingWindowTouchListener(params, windowManager));

                    // 添加到窗口管理器
                    windowManager.addView(floatingView, params);
                    Log.d(TAG, "Floating window added to window manager");
                    
                    // 发送显示事件
                    sendEvent("show", null);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to show floating window", e);
                }
            }
        });
    }

    /**
     * 隐藏悬浮窗
     */
    public void hide() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (floatingView != null && windowManager != null) {
                        windowManager.removeView(floatingView);
                        floatingView = null;
                        imageView = null;
                        Log.d(TAG, "Floating window removed");
                        
                        // 发送隐藏事件
                        sendEvent("hide", null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to hide floating window", e);
                }
            }
        });
    }

    /**
     * 加载图片
     */
    private void loadImage(String imagePath, ImageView imageView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = null;
                    
                    // 判断是本地路径还是网络URL
                    if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                        // 网络图片
                        URL url = new URL(imagePath);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        bitmap = BitmapFactory.decodeStream(input);
                        input.close();
                    } else {
                        // 本地图片
                        String realPath = imagePath;
                        if (imagePath.startsWith("file://")) {
                            realPath = imagePath.replace("file://", "");
                        }
                        File file = new File(realPath);
                        if (file.exists()) {
                            bitmap = BitmapFactory.decodeFile(realPath);
                        } else {
                            // 尝试从assets或资源加载
                            try {
                                int resId = context.getResources().getIdentifier(
                                    realPath.replace("/", "_").replace(".", "_"),
                                    "drawable",
                                    context.getPackageName()
                                );
                                if (resId != 0) {
                                    bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to load image from resources", e);
                            }
                        }
                    }
                    
                    if (bitmap != null) {
                        final Bitmap finalBitmap = bitmap;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(finalBitmap);
                            }
                        });
                    } else {
                        Log.e(TAG, "Failed to load image: " + imagePath);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image", e);
                }
            }
        }).start();
    }

    /**
     * 发送事件到插件
     */
    private void sendEvent(String eventType, String data) {
        try {
            Intent intent = new Intent("com.cordova.floatingwindow.EVENT");
            intent.putExtra("eventType", eventType);
            if (data != null) {
                intent.putExtra("data", data);
            }
            context.sendBroadcast(intent);
            Log.d(TAG, "Event sent: " + eventType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send event", e);
        }
    }

    /**
     * 更新悬浮窗位置
     */
    public void updatePosition(int x, int y) {
        if (params != null && floatingView != null) {
            params.x = x;
            params.y = y;
            windowManager.updateViewLayout(floatingView, params);
        }
    }
}
