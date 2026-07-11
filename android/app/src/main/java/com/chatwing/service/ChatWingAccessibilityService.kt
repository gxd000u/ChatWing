package com.chatwing.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

/**
 * ChatWing 无障碍服务
 * 负责：
 * 1. 监听微信/牵手App等聊天界面的 UI 变化
 * 2. 自动抓取联系人信息（昵称、头像、主页简介）
 * 3. 模拟点击和文本输入实现自动回复
 * 4. 触发截图分析事件
 */
class ChatWingAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ChatWing_Assist"
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        private const val QIANSHOU_PACKAGE = "com.qianxia.qianshou"

        var instance: ChatWingAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentAppPackage: String? = null
    private var lastProcessedText: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AccessibilityService 已连接")

        val info = accessibilityServiceInfo
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 100
        this.serviceInfo = info
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        scope.cancel()
        Log.i(TAG, "AccessibilityService 已销毁")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowChange(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextChange(event)
            AccessibilityEvent.TYPE_VIEW_CLICKED -> handleViewClick(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleContentChange(event)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService 被中断")
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        currentAppPackage = pkg
        if (pkg in listOf(WECHAT_PACKAGE, QIANSHOU_PACKAGE)) {
            Log.d(TAG, "检测到目标应用窗口变化: $pkg")
            notifyChatWindowOpened(pkg)
            scope.launch {
                delay(500)
                extractContactInfo()
            }
        }
    }

    private fun handleTextChange(event: AccessibilityEvent) {
        val text = event.text?.joinToString("") ?: return
        if (text == lastProcessedText) return
        lastProcessedText = text
        Log.d(TAG, "文本变化: $text")
        notifyNewMessageReceived(text, event.packageName?.toString() ?: "")
    }

    private fun handleViewClick(event: AccessibilityEvent) {}
    private fun handleContentChange(event: AccessibilityEvent) {}

    private fun extractContactInfo() {
        val root = rootInActiveWindow ?: return
        val contactInfo = extractInfoFromNode(root)
        root.recycle()
        if (contactInfo != null) {
            Log.i(TAG, "抓取到联系人信息: $contactInfo")
            broadcastContactInfo(contactInfo)
        }
    }

    private fun extractInfoFromNode(node: AccessibilityNodeInfo?): ContactInfo? {
        if (node == null) return null
        var nickName: String? = null
        if (node.className?.contains("TextView") == true) {
            val text = node.text?.toString()
            if (!text.isNullOrBlank() && text.length in 1..20) {
                nickName = text
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = extractInfoFromNode(child)
            child.recycle()
            if (result != null) return result
        }
        return if (nickName != null) ContactInfo(nickName = nickName, packageName = currentAppPackage ?: "") else null
    }

    fun performTapOnNode(nodeId: String) {
        val root = rootInActiveWindow ?: return
        val target = findNodeById(root, nodeId)
        target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        root.recycle()
    }

    fun typeText(text: String) {
        val root = rootInActiveWindow ?: return
        val editField = findEditableNode(root)
        editField?.let { field ->
            field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val args = android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
        root.recycle()
    }

    fun clickSendButton() {
        val root = rootInActiveWindow ?: return
        val sendBtn = findSendButton(root)
        sendBtn?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        root.recycle()
    }

    private fun findNodeById(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        if (root.viewIdResourceName == id) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findNodeById(child, id)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun findEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable) return root
        if (root.className?.contains("EditText") == true) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findEditableNode(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = root.contentDescription?.toString() ?: ""
        val text = root.text?.toString() ?: ""
        if (desc.contains("send", ignoreCase = true) || text == "发送" || text == "Send") return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findSendButton(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun notifyChatWindowOpened(packageName: String) {
        sendBroadcast(Intent(ACTION_CHAT_WINDOW_OPENED).apply { putExtra("package_name", packageName) })
    }

    private fun notifyNewMessageReceived(text: String, packageName: String) {
        sendBroadcast(Intent(ACTION_NEW_MESSAGE).apply {
            putExtra("text", text)
            putExtra("package_name", packageName)
        })
    }

    private fun broadcastContactInfo(info: ContactInfo) {
        sendBroadcast(Intent(ACTION_CONTACT_INFO).apply {
            putExtra("nickname", info.nickName)
            putExtra("package_name", info.packageName)
        })
    }

    data class ContactInfo(val nickName: String, val packageName: String, val avatarBase64: String? = null, val bio: String? = null)

    companion object Actions {
        const val ACTION_CHAT_WINDOW_OPENED = "com.chatwing.CHAT_WINDOW_OPENED"
        const val ACTION_NEW_MESSAGE = "com.chatwing.NEW_MESSAGE"
        const val ACTION_CONTACT_INFO = "com.chatwing.CONTACT_INFO"
        const val ACTION_AUTO_REPLY = "com.chatwing.AUTO_REPLY"
        const val ACTION_SEND_TEXT = "com.chatwing.SEND_TEXT"
        const val ACTION_CLICK_SEND = "com.chatwing.CLICK_SEND"
    }
}
