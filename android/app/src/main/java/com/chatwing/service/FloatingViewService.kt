package com.chatwing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageView

class FloatingViewService : Service() {

    companion object {
        private const val TAG = "ChatWing_Floating"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "chatwing_floating"
        var isRunning = false; private set
        fun start(context: Context) { context.startForegroundService(Intent(context, FloatingViewService::class.java)) }
        fun stop(context: Context) { context.stopService(Intent(context, FloatingViewService::class.java)) }
    }

    private lateinit var windowManager: WindowManager
    private var floatingBall: View? = null
    private var floatingPanel: View? = null
    private var paramsBall: WindowManager.LayoutParams? = null
    private var paramsPanel: WindowManager.LayoutParams? = null
    private var isPanelShowing = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        showFloatingBall()
        Log.i(TAG, "悬浮窗服务已启动")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingBall(); removeFloatingPanel()
        isRunning = false
        Log.i(TAG, "悬浮窗服务已销毁")
    }

    private fun showFloatingBall() {
        if (floatingBall != null) return
        val ball = ImageView(this).apply {
            setBackgroundResource(android.R.drawable.ic_dialog_info)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(12, 12, 12, 12)
        }
        floatingBall = ball

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val sizePx = (56 * resources.displayMetrics.density).toInt()
        paramsBall = WindowManager.LayoutParams(
            sizePx, sizePx, flags,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 200 }

        windowManager.addView(ball, paramsBall)

        ball.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0; private var initialY = 0
            private var initialTouchX = 0f; private var initialTouchY = 0f
            private var isDragging = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = paramsBall?.x ?: 0; initialY = paramsBall?.y ?: 0
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        isDragging = false; return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt(); val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) isDragging = true
                        paramsBall?.let { it.x = initialX + dx; it.y = initialY + dy; windowManager.updateViewLayout(v, it) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> { if (!isDragging) togglePanel(); return true }
                }
                return false
            }
        })
    }

    private fun removeFloatingBall() {
        floatingBall?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }; floatingBall = null
    }

    private fun togglePanel() { if (isPanelShowing) removeFloatingPanel() else showFloatingPanel() }

    private fun showFloatingPanel() {
        if (floatingPanel != null) return; isPanelShowing = true
        val density = resources.displayMetrics.density
        val widthPx = (320 * density).toInt(); val heightPx = (480 * density).toInt()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

        val textView = TextView(this).apply {
            text = "ChatWing 助手面板\n[模式] 全托管/策略军师/灵感文案\n点击外部关闭此面板"
            setTextColor(-0x1000000); textSize = 14f
            gravity = Gravity.CENTER; setPadding(16, 16, 16, 16)
            setBackgroundColor(-0x34000000)
        }
        floatingPanel = textView

        paramsPanel = WindowManager.LayoutParams(
            widthPx, heightPx, flags,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (paramsBall?.x ?: 0) - widthPx / 2
            y = (paramsBall?.y ?: 0) + (60 * density).toInt()
        }
        windowManager.addView(textView, paramsPanel)
        textView.setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_OUTSIDE) { removeFloatingPanel() }; false }
    }

    private fun removeFloatingPanel() {
        floatingPanel?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }; floatingPanel = null; isPanelShowing = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "ChatWing 悬浮窗", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "ChatWing AI 僚机悬浮助手" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ChatWing 助手运行中")
            .setContentText("点击展开悬浮球")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
        return builder.build()
    }
}
