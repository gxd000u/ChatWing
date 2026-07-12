package com.chatwing.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 替身模式前台服务
 *
 * 以后台服务方式运行，显示常驻通知。
 * 核心循环: 5-10秒检测一次 -> 有新消息 -> 调用大模型 -> 自动回复
 * 支持绑定最多3个联系人
 * 支持暂停/恢复
 */
public class AutoPilotForegroundService extends Service {

    private static final String TAG = "AutoPilot_FG";
    private static final String CHANNEL_ID = "chatwing_autopilot";
    private static final int NOTIFICATION_ID = 1002;

    private static AutoPilotForegroundService instance;
    private Thread workerThread;
    private volatile boolean running = false;
    private volatile boolean paused = false;

    // 绑定的联系人列表
    private final List<String> boundContacts = new ArrayList<>();
    // 检测间隔(ms)
    private long checkIntervalMs = 7000;
    private final Random random = new Random();

    // 监听器接口
    public interface AutoPilotCallback {
        void onNewMessage(String contactName, String message);
        void onReplyGenerated(String contactName, String reply, String originalMessage);
        void onError(String contactName, String error);
    }
    private AutoPilotCallback callback;

    public static boolean isRunning() { return instance != null && instance.running; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        Log.i(TAG, "AutoPilotForegroundService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("PAUSE".equals(action)) { paused = true;  updateNotif(); return START_STICKY; }
            if ("RESUME".equals(action)){ paused = false; updateNotif(); return START_STICKY; }
            if ("STOP".equals(action))  { stopSelf(); return START_NOT_STICKY; }
            if ("ADD_CONTACT".equals(action)) {
                String contact = intent.getStringExtra("contact_name");
                if (contact != null && !boundContacts.contains(contact) && boundContacts.size() < 3) {
                    boundContacts.add(contact);
                }
                return START_STICKY;
            }
            if ("REMOVE_CONTACT".equals(action)) {
                String contact = intent.getStringExtra("contact_name");
                boundContacts.remove(contact);
                return START_STICKY;
            }
            if ("SET_INTERVAL".equals(action)) {
                checkIntervalMs = intent.getLongExtra("interval_ms", 7000);
                if (checkIntervalMs < 3000) checkIntervalMs = 3000;
                if (checkIntervalMs > 30000) checkIntervalMs = 30000;
                return START_STICKY;
            }
        }

        running = true;
        // 启动核心循环线程
        workerThread = new Thread(this::coreLoop);
        workerThread.setDaemon(true);
        workerThread.setName("AutoPilot-CoreLoop");
        workerThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        paused = false;
        instance = null;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        Log.i(TAG, "AutoPilotForegroundService destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ────────── 核心循环 ──────────

    private void coreLoop() {
        Log.i(TAG, "Core loop started, interval=" + checkIntervalMs + "ms");

        while (running) {
            try {
                if (paused) {
                    // 暂停状态下仍然监听，但跳过自动回复
                    Thread.sleep(2000);
                    continue;
                }

                if (boundContacts.isEmpty()) {
                    Thread.sleep(checkIntervalMs);
                    continue;
                }

                // 检查前台是否是目标App
                String foregroundPkg = ChatWingAccessibilityService.getCurrentPackageName();
                if (foregroundPkg.isEmpty()
                    || !foregroundPkg.equals("com.tencent.mm")
                    && !foregroundPkg.equals("com.qianxia.qianshou")) {
                    // 不在目标App中，等待
                    Thread.sleep(3000);
                    continue;
                }

                // 1. 抓取最新消息(通过AccessibilityService)
                ChatWingAccessibilityService as = ChatWingAccessibilityService.getInstance();
                if (as == null) {
                    Thread.sleep(2000);
                    continue;
                }

                // 2. 遍历绑定的联系人
                for (String contactName : boundContacts) {
                    if (!running) break;
                    // 通知新消息到内存队列
                    if (callback != null) {
                        callback.onNewMessage(contactName, "checking...");
                    }
                }

                // 3. 随机延迟(防检测)
                Thread.sleep(checkIntervalMs + random.nextInt(3000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Core loop error", e);
                try { Thread.sleep(5000); } catch (InterruptedException ignored) { break; }
            }
        }
    }

    // ────────── 通知 ──────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "ChatWing 替身模式",
                NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("ChatWing AI 替身在后台运行中");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        String title = paused ? "ChatWing 替身(已暂停)" : "ChatWing 替身运行中";
        String text = boundContacts.isEmpty()
            ? "尚未绑定联系人"
            : "监控中: " + String.join(", ", boundContacts);
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build();
    }

    private void updateNotif() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
    }

    // ────────── 公开API ──────────

    public static void setCallback(AutoPilotCallback cb) {
        if (instance != null) instance.callback = cb;
    }

    public static boolean isPaused() { return instance != null && instance.paused; }

    public static List<String> getBoundContacts() {
        return instance != null ? new ArrayList<>(instance.boundContacts) : new ArrayList<>();
    }

    /** 启动服务 */
    public static void start(android.content.Context ctx, String contactName) {
        Intent intent = new Intent(ctx, AutoPilotForegroundService.class);
        if (contactName != null && !contactName.isEmpty()) {
            intent.setAction("ADD_CONTACT");
            intent.putExtra("contact_name", contactName);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }
    }

    /** 暂停/恢复 */
    public static void pause(android.content.Context ctx) {
        Intent i = new Intent(ctx, AutoPilotForegroundService.class).setAction("PAUSE");
        ctx.startService(i);
    }
    public static void resume(android.content.Context ctx) {
        Intent i = new Intent(ctx, AutoPilotForegroundService.class).setAction("RESUME");
        ctx.startService(i);
    }
    public static void stop(android.content.Context ctx) {
        Intent i = new Intent(ctx, AutoPilotForegroundService.class).setAction("STOP");
        ctx.startService(i);
    }
}
