package com.chatwing.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log

/**
 * 屏幕投射管理器
 * 封装 MediaProjection API 的请求和生命周期管理
 */
object ScreenProjectionManager {

    private const val TAG = "ScreenProjection"
    const val REQUEST_CODE = 10001

    private var mediaProjectionManager: MediaProjectionManager? = null

    /**
     * 发起屏幕捕获请求（会弹出系统确认对话框）
     */
    fun requestScreenCapture(activity: Activity) {
        mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager?.createScreenCaptureIntent()
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    /**
     * 处理 onActivityResult 回调
     */
    fun handleActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Log.i(TAG, "屏幕投射授权成功，启动 ScreenCaptureService")
            // 启动捕获服务（由调用方传入 ApplicationContext）
        } else {
            Log.w(TAG, "屏幕投射授权被拒绝")
        }
    }

    fun isProjectionRunning(): Boolean = com.chatwing.service.ScreenCaptureService.isRunning
}
