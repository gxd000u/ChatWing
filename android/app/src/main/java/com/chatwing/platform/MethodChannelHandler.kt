package com.chatwing.platform

import android.content.Context
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Flutter 与 Android 原生之间的桥梁
 * 通过 MethodChannel 双向通信
 * Flutter 端调用原生功能（悬浮窗、无障碍、屏幕投射、OCR）
 * 原生端向 Flutter 推送事件（新消息、联系人更新、截图事件）
 */
class MethodChannelHandler(private val context: Context) {

    companion object {
        private const val TAG = "ChatWing_Channel"
        private const val CHANNEL = "com.chatwing/native"
        private const val EVENT_CHANNEL = "com.chatwing/events"

        private var instance: MethodChannelHandler? = null
        fun init(context: Context, flutterEngine: FlutterEngine): MethodChannelHandler {
            val handler = MethodChannelHandler(context)
            handler.setupChannels(flutterEngine)
            instance = handler
            return handler
        }
        fun getInstance(): MethodChannelHandler? = instance
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: MethodChannel
    private var eventSink: MethodChannel.Result? = null

    private fun setupChannels(engine: FlutterEngine) {
        methodChannel = MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
        eventChannel = MethodChannel(engine.dartExecutor.binaryMessenger, EVENT_CHANNEL)

        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "startFloatingWindow" -> handleStartFloatingWindow(result)
                "stopFloatingWindow" -> handleStopFloatingWindow(result)
                "startScreenCapture" -> handleStartScreenCapture(call, result)
                "stopScreenCapture" -> handleStopScreenCapture(result)
                "enableAccessibilityService" -> handleEnableAccessibility(result)
                "sendText" -> handleSendText(call, result)
                "clickSend" -> handleClickSend(result)
                "startAutoPilot" -> handleStartAutoPilot(result)
                "stopAutoPilot" -> handleStopAutoPilot(result)
                "getContacts" -> handleGetContacts(result)
                "deleteContact" -> handleDeleteContact(call, result)
                "updateProfile" -> handleUpdateProfile(call, result)
                "generateReply" -> handleGenerateReply(call, result)
                "switchProvider" -> handleSwitchProvider(call, result)
                else -> result.notImplemented()
            }
        }
    }

    /** 向 Flutter 发送事件 */
    fun sendEvent(event: String, data: Map<String, Any?>) {
        try {
            methodChannel.invokeMethod(event, data)
        } catch (e: Exception) {
            Log.w(TAG, "发送事件失败: ${e.message}")
        }
    }

    fun sendNewMessage(text: String, packageName: String) {
        sendEvent("onNewMessage", mapOf("text" to text, "packageName" to packageName))
    }

    fun sendContactInfo(nickname: String, packageName: String) {
        sendEvent("onContactDetected", mapOf("nickname" to nickname, "packageName" to packageName))
    }

    fun sendScreenshotTaken(path: String) {
        sendEvent("onScreenshotTaken", mapOf("path" to path))
    }

    // ── 方法处理 ─────────────────────────────────────────

    private fun handleStartFloatingWindow(result: MethodChannel.Result) {
        com.chatwing.service.FloatingViewService.start(context)
        result.success(true)
    }

    private fun handleStopFloatingWindow(result: MethodChannel.Result) {
        com.chatwing.service.FloatingViewService.stop(context)
        result.success(true)
    }

    private fun handleStartScreenCapture(call: MethodChannel.MethodCall, result: MethodChannel.Result) {
        val resultCode = call.argument<Int>("resultCode") ?: 0
        val data = call.argument<String>("data") ?: ""
        result.success(true)
    }

    private fun handleStopScreenCapture(result: MethodChannel.Result) {
        result.success(true)
    }

    private fun handleEnableAccessibility(result: MethodChannel.Result) {
        result.success(com.chatwing.service.ChatWingAccessibilityService.isRunning())
    }

    private fun handleSendText(call: MethodChannel.MethodCall, result: MethodChannel.Result) {
        val text = call.argument<String>("text") ?: ""
        com.chatwing.service.ChatWingAccessibilityService.instance?.typeText(text)
        result.success(true)
    }

    private fun handleClickSend(result: MethodChannel.Result) {
        com.chatwing.service.ChatWingAccessibilityService.instance?.clickSendButton()
        result.success(true)
    }

    private fun handleStartAutoPilot(result: MethodChannel.Result) { result.success(true) }
    private fun handleStopAutoPilot(result: MethodChannel.Result) { result.success(true) }
    private fun handleGetContacts(result: MethodChannel.Result) { result.success(JSONArray().toString()) }
    private fun handleDeleteContact(call: MethodChannel.MethodCall, result: MethodChannel.Result) { result.success(true) }
    private fun handleUpdateProfile(call: MethodChannel.MethodCall, result: MethodChannel.Result) { result.success(true) }
    private fun handleGenerateReply(call: MethodChannel.MethodCall, result: MethodChannel.Result) { result.success("") }
    private fun handleSwitchProvider(call: MethodChannel.MethodCall, result: MethodChannel.Result) { result.success(true) }

    fun dispose() { scope.cancel() }
}
