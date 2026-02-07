package com.cordova.floatingwindow;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.util.Log;

/**
 * FloatingWindowTouchListener - 悬浮窗触摸监听器
 * 实现悬浮窗的拖拽功能
 */
public class FloatingWindowTouchListener implements View.OnTouchListener {
    private static final String TAG = "FloatingWindowTouch";
    private WindowManager.LayoutParams params;
    private WindowManager windowManager;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    public FloatingWindowTouchListener(WindowManager.LayoutParams params, WindowManager windowManager) {
        this.params = params;
        this.windowManager = windowManager;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录初始位置
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return false; // 允许点击事件继续传递

            case MotionEvent.ACTION_MOVE:
                // 计算移动距离
                float deltaX = event.getRawX() - initialTouchX;
                float deltaY = event.getRawY() - initialTouchY;
                
                // 更新位置
                params.x = initialX + (int) deltaX;
                params.y = initialY + (int) deltaY;
                
                // 更新视图位置
                windowManager.updateViewLayout(v, params);
                return true; // 消费触摸事件

            case MotionEvent.ACTION_UP:
                // 触摸结束，可以在这里添加点击检测逻辑
                return false;

            default:
                return false;
        }
    }
}
