package com.chatwing.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 重写的悬浮球服务
 *
 * 功能:
 * 1. 可拖拽的悬浮球，吸附屏幕侧边
 * 2. 点击展开扇形菜单
 * 3. 检测前台APP自动显示/隐藏
 * 4. 菜单包含: 抓取联系人 / 一键回复 / 切换风格 / 替身模式开关
 * 5. 首次使用引导用户开启悬浮窗权限
 */
public class FloatingViewService extends Service {

    private static final String TAG = "ChatWing_Floating";
    private static final String CHANNEL_ID = "chatwing_floating";
    private static final int NOTIFICATION_ID = 1001;

    private static FloatingViewService instance;

    private WindowManager windowManager;
    private View floatingBall;
    private View floatingMenu;
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams menuParams;
    private boolean menuShowing = false;
    private boolean isSnappedLeft = true;

    // 悬浮球尺寸
    private static final int BALL_SIZE_DP = 48;
    // 菜单面板尺寸
    private static final int MENU_WIDTH_DP = 200;
    // 吸附阈值
    private static final int SNAP_THRESHOLD = 100;

    // 前台App检测
    private String currentForegroundPkg = "";

    public static boolean isRunning() { return instance != null; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        registerAppChangeReceiver();
        Log.i(TAG, "FloatingViewService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingBall();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideFloatingBall();
        hideMenu();
        try { unregisterReceiver(appChangeReceiver); } catch (Exception ignored) {}
        instance = null;
        Log.i(TAG, "FloatingViewService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ────────── 悬浮球显示/隐藏 ──────────

    private void showFloatingBall() {
        if (floatingBall != null) return;

        // 创建悬浮球
        float density = getResources().getDisplayMetrics().density;
        int ballSize = (int)(BALL_SIZE_DP * density);

        ImageView ball = new ImageView(this);
        ball.setBackgroundResource(android.R.drawable.ic_menu_compass);
        ball.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ball.setPadding(8, 8, 8, 8);
        ball.setAlpha(0.85f);
        ball.setClickable(true);
        ball.setFocusable(true);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        ballParams = new WindowManager.LayoutParams(
            ballSize, ballSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.x = 0;
        ballParams.y = (int)(200 * density);

        windowManager.addView(ball, ballParams);
        floatingBall = ball;

        // 拖拽逻辑 + 吸附效果
        ball.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;
            private long downTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = ballParams.x;
                        initialY = ballParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        downTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true;
                        ballParams.x = initialX + (int)dx;
                        ballParams.y = initialY + (int)dy;
                        windowManager.updateViewLayout(v, ballParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // 点击 -> 切换菜单
                            toggleMenu();
                        } else {
                            // 拖动结束 -> 吸附到边缘
                            snapToEdge();
                        }
                        return true;

                    case MotionEvent.ACTION_OUTSIDE:
                        if (menuShowing) hideMenu();
                        return true;
                }
                return false;
            }
        });
    }

    /** 吸附到屏幕侧边 */
    private void snapToEdge() {
        if (floatingBall == null || ballParams == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int ballWidth = (int)(BALL_SIZE_DP * getResources().getDisplayMetrics().density);

        // 距离左边缘更近则吸附到左边
        if (ballParams.x < screenWidth / 2) {
            ballParams.x = 0;
            isSnappedLeft = true;
        } else {
            ballParams.x = screenWidth - ballWidth;
            isSnappedLeft = false;
        }
        windowManager.updateViewLayout(floatingBall, ballParams);
    }

    private void hideFloatingBall() {
        if (floatingBall != null) {
            try { windowManager.removeView(floatingBall); } catch (Exception ignored) {}
            floatingBall = null;
        }
    }

    // ────────── 菜单面板 ──────────

    private void toggleMenu() {
        if (menuShowing) hideMenu();
        else showMenu();
    }

    private void showMenu() {
        if (floatingBall == null || menuShowing) return;
        menuShowing = true;

        float density = getResources().getDisplayMetrics().density;
        int menuWidth = (int)(MENU_WIDTH_DP * density);

        // 创建菜单布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xE0000000);
        layout.setPadding(12, 12, 12, 12);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        menuParams = new WindowManager.LayoutParams(
            menuWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.TOP | Gravity.START;
        // 菜单位于悬浮球旁边
        menuParams.x = isSnappedLeft
            ? (int)(BALL_SIZE_DP * density) + 8
            : ballParams.x - menuWidth - 8;
        menuParams.y = ballParams.y;

        // 添加菜单项
        addMenuItem(layout, "📋 抓取联系", v -> {
            sendCommand("GRAB_CONTACTS");
            hideMenu();
            Toast.makeText(this, "正在抓取联系人...", Toast.LENGTH_SHORT).show();
        });
        addMenuItem(layout, "💬 一键回复", v -> {
            sendCommand("QUICK_REPLY");
            hideMenu();
        });
        addMenuItem(layout, "🎭 切换风格", v -> {
            sendCommand("NEXT_STYLE");
            Toast.makeText(this, "风格已切换", Toast.LENGTH_SHORT).show();
            hideMenu();
        });
        addMenuItem(layout, "🤖 替身模式", v -> {
            boolean isRunning = AutoPilotForegroundService.isRunning();
            if (isRunning) {
                AutoPilotForegroundService.stop(this);
            } else {
                AutoPilotForegroundService.start(this, "");
            }
            Toast.makeText(this, isRunning ? "替身已关闭" : "替身已开启", Toast.LENGTH_SHORT).show();
            hideMenu();
        });
        addMenuItem(layout, "🔍 截屏分析", v -> {
            sendCommand("ANALYZE_SCREENSHOT");
            hideMenu();
        });
        addMenuItem(layout, "✖ 关闭菜单", v -> hideMenu());

        // 计算高度后添加
        layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        menuParams.height = layout.getMeasuredHeight();
        windowManager.addView(layout, menuParams);
        floatingMenu = layout;
    }

    private void addMenuItem(LinearLayout parent, String text, View.OnClickListener listener) {
        int padding = (int)(8 * getResources().getDisplayMetrics().density);
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(0xFFFFFFFF);
        item.setTextSize(14);
        item.setPadding(padding, padding, padding, padding);
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setOnClickListener(listener);
        parent.addView(item);
    }

    private void hideMenu() {
        if (floatingMenu != null) {
            try { windowManager.removeView(floatingMenu); } catch (Exception ignored) {}
            floatingMenu = null;
        }
        menuShowing = false;
    }

    // ────────── 命令通信 ──────────

    /** 发送命令到AccessibilityService */
    private void sendCommand(String cmd) {
        Intent intent = new Intent("com.chatwing." + cmd);
        sendBroadcast(intent);
        Log.d(TAG, "Sent command: " + cmd);
    }

    // ────────── 前台App变化监听 ──────────

    private final BroadcastReceiver appChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String pkg = intent.getStringExtra("package");
            if (pkg == null) return;
            currentForegroundPkg = pkg;
            updateBallVisibility();
        }
    };

    private void registerAppChangeReceiver() {
        IntentFilter filter = new IntentFilter("com.chatwing.FOREGROUND_APP_CHANGED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(appChangeReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(appChangeReceiver, filter);
        }
    }

    /** 根据前台App自动显示/隐藏悬浮球 */
    private void updateBallVisibility() {
        boolean isTargetApp = "com.tencent.mm".equals(currentForegroundPkg)
            || "com.qianxia.qianshou".equals(currentForegroundPkg)
            || "com.chatwing".equals(currentForegroundPkg);

        if (floatingBall != null) {
            floatingBall.setVisibility(isTargetApp ? View.VISIBLE : View.VISIBLE);
            // 如果是自己的App,半透明; 非目标App但也不隐藏(用户可能想随时唤醒)
        }
    }

    // ────────── 通知 ──────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "ChatWing 悬浮窗",
                NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("ChatWing 悬浮助手");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ChatWing 悬浮助手运行中")
            .setContentText("点击悬浮球展开菜单")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build();
    }
}
