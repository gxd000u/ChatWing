package com.chatwing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ChatWing_ScreenCap"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "chatwing_screen_capture"
        private const val VIRTUAL_DISPLAY_NAME = "ChatWingScreenCapture"
        private const val SCREEN_WIDTH = 1080
        private const val SCREEN_HEIGHT = 1920
        private const val DPI = 420

        var isRunning = false; private set
        var mediaProjection: MediaProjection? = null; private set

        fun start(context: Context, resultCode: Int, data: Intent) {
            val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mgr.getMediaProjection(resultCode, data)
            context.startForegroundService(Intent(context, ScreenCaptureService::class.java))
        }
    }

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var captureCallback: ((Bitmap) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startScreenCapture()
        Log.i(TAG, "屏幕捕获服务已启动")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        mediaProjection?.stop(); mediaProjection = null
        isRunning = false
        Log.i(TAG, "屏幕捕获服务已销毁")
    }

    private fun startScreenCapture() {
        val projection = mediaProjection ?: return
        imageReader = ImageReader.newInstance(SCREEN_WIDTH, SCREEN_HEIGHT, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME, SCREEN_WIDTH, SCREEN_HEIGHT, DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, mainHandler
        )
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val planes = image.planes
            val buffer = planes[0].buffer
            val rowStride = planes[0].rowStride
            val pixelStride = planes[0].pixelStride
            val rowPadding = rowStride - pixelStride * SCREEN_WIDTH
            val bitmap = Bitmap.createBitmap(SCREEN_WIDTH + rowPadding / pixelStride, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)
            captureCallback?.invoke(cropped)
            image.close()
        }, mainHandler)
    }

    fun captureOnce(callback: (Bitmap) -> Unit) {
        captureCallback = callback
        mainHandler.postDelayed({ captureCallback = null }, 3000)
    }

    private fun stopScreenCapture() {
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close(); imageReader = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "ChatWing 屏幕捕获", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "用于实时截取聊天屏幕进行OCR识别" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("ChatWing 屏幕监控中")
        .setContentText("正在捕获屏幕用于聊天分析")
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
}
