package com.chatwing.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 重写的无障碍服务 - 可以正确解析微信/牵手聊天界面节点
 *
 * 核心能力:
 * 1. 识别前台APP包名
 * 2. 提取聊天列表联系人
 * 3. 提取聊天气泡并区分自己/对方
 * 4. 模拟点击操作(带随机延迟)
 * 5. 支持手动触发抓取(FloatingView发起的Intent)
 */
public class ChatWingAccessibilityService extends AccessibilityService {

    private static final String TAG = "ChatWing_AS";

    // 支持的聊天APP包名
    public static final String[] SUPPORTED_PACKAGES = {
        "com.tencent.mm",                    // 微信
        "com.qianxia.qianshou",              // 牵手
        "com.imo.android.imoim",             // 探探
    };

    // 自定义Action常量(通过Broadcast接收)
    public static final String ACTION_GRAB_CONTACTS  = "com.chatwing.GRAB_CONTACTS";
    public static final String ACTION_GRAB_BUBBLES   = "com.chatwing.GRAB_BUBBLES";
    public static final String ACTION_AUTO_REPLY     = "com.chatwing.AUTO_REPLY";
    public static final String ACTION_SEND_REPLY     = "com.chatwing.SEND_REPLY";
    public static final String EXTRA_REPLY_TEXT      = "extra_reply_text";

    // 抓取结果Broadcast
    public static final String ACTION_GRAB_RESULT    = "com.chatwing.GRAB_RESULT";
    public static final String EXTRA_CONTACTS_JSON   = "extra_contacts_json";
    public static final String EXTRA_BUBBLES_JSON    = "extra_bubbles_json";
    public static final String EXTRA_CURRENT_PKG     = "extra_current_pkg";

    public static ChatWingAccessibilityService instance;

    /** 最近抓取到的API level */
    private static int apiLevel = Build.VERSION.SDK_INT;

    public static boolean isRunning() { return instance != null; }
    public static ChatWingAccessibilityService getInstance() { return instance; }
    public static String getCurrentPackageName() {
        return instance != null ? instance.currentPkg : "";
    }

    // 当前前台包名
    private volatile String currentPkg = "";
    // 最后处理的文本(防重复)
    private String lastProcessedText = "";
    // 随机延迟生成器
    private final Random random = new Random();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "AccessibilityService connected");

        // 注册BroadcastReceiver接收来自Flutter/FloatingView的命令
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(ACTION_GRAB_CONTACTS);
        filter.addAction(ACTION_GRAB_BUBBLES);
        filter.addAction(ACTION_AUTO_REPLY);
        filter.addAction(ACTION_SEND_REPLY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    handleCommand(intent);
                }
            }, filter, android.content.Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    handleCommand(intent);
                }
            }, filter);
        }

        // 配置ServiceInfo——监听所有包名，不限制
        configureServiceInfo();
    }

    private void configureServiceInfo() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                   | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                   | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {
            currentPkg = event.getPackageName().toString();
        }
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                onWindowStateChanged(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                onContentChanged(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                onTextChanged(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                // 不处理，避免干扰
                break;
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        Log.i(TAG, "AccessibilityService destroyed");
    }

    // ────────── 事件处理 ──────────

    private void onWindowStateChanged(AccessibilityEvent event) {
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (pkg.isEmpty()) return;
        Log.d(TAG, "Window changed: " + pkg + " class=" + event.getClassName());

        // 通知FloatViewService：前台窗口变化
        Intent notify = new Intent("com.chatwing.FOREGROUND_APP_CHANGED");
        notify.putExtra("package", pkg);
        sendBroadcast(notify);
    }

    private void onContentChanged(AccessibilityEvent event) {
        // 在目标App中，内容变化可能表示新消息出现
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (!isSupported(pkg)) return;
        // 延迟一小段时间让UI稳定，然后尝试抓取新消息
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            grabBubblesIfInChat();
        }, 500);
    }

    private void onTextChanged(AccessibilityEvent event) {
        // 文本变化——主要是输入框内容变化，我们记录但不重复处理
    }

    // ────────── 命令处理(接收来自FloatingView/Flutter的命令) ──────────

    private android.content.BroadcastReceiver receiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            handleCommand(intent);
        }
    };

    private void handleCommand(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "Received command: " + action);

        new Thread(() -> {
            try {
                switch (action) {
                    case ACTION_GRAB_CONTACTS:
                        grabContacts();
                        break;
                    case ACTION_GRAB_BUBBLES:
                        grabBubbles();
                        break;
                    case ACTION_AUTO_REPLY:
                        // TODO: 调用AutoPilotEngine处理
                        break;
                    case ACTION_SEND_REPLY:
                        String replyText = intent.getStringExtra(EXTRA_REPLY_TEXT);
                        if (replyText != null && !replyText.isEmpty()) {
                            typeTextAndSend(replyText);
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Command failed: " + action, e);
            }
        }).start();
    }

    // ────────── 核心功能1: 抓取联系人列表 ──────────

    public void grabContacts() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.w(TAG, "No active window root");
            broadcastResult("grab_contacts", "[]");
            return;
        }

        List<ContactItem> contacts = new ArrayList<>();
        // 遍历节点树，查找聊天列表中的联系人
        findContactsInList(root, contacts);
        root.recycle();

        // 将结果转为JSON广播出去
        org.json.JSONArray arr = new org.json.JSONArray();
        for (ContactItem c : contacts) {
            arr.put(c.toJson());
        }
        broadcastResult("grab_contacts", arr.toString());
        Log.i(TAG, "Found " + contacts.size() + " contacts");
    }

    private void findContactsInList(AccessibilityNodeInfo node, List<ContactItem> results) {
        if (node == null) return;

        // 微信联系人列表特征:
        // - ImageView (头像) + TextView (昵称) 在同一个父Layout中
        // - 昵称文本长度 1~20 字符
        // - 通常在 ListView/RecyclerView 内

        CharSequence className = node.getClassName();
        CharSequence text = node.getText();
        CharSequence contentDesc = node.getContentDescription();

        // 判断是否为联系人项
        if (text != null && text.length() > 0 && text.length() <= 20
            && className != null && className.toString().contains("Text")) {

            String name = text.toString().trim();
            // 过滤无用文本
            if (!name.startsWith("http") && !name.contains("搜索")
                && !name.contains("设置") && !name.contains("发现")
                && android.text.TextUtils.isDigitsOnly(name) == false) {

                ContactItem item = new ContactItem();
                item.nickname = name;
                item.packageName = currentPkg;
                // 尝试找头像
                item.avatarViewId = findSiblingAvatar(node);
                results.add(item);
            }
        }

        // 递归遍历子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findContactsInList(child, results);
                child.recycle();
            }
        }
    }

    private String findSiblingAvatar(AccessibilityNodeInfo textNode) {
        // 在当前节点的父节点中查找 ImageView
        AccessibilityNodeInfo parent = textNode.getParent();
        if (parent == null) return "";
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo sibling = parent.getChild(i);
            if (sibling != null) {
                if (sibling.getClassName() != null
                    && sibling.getClassName().toString().contains("Image")) {
                    String viewId = sibling.getViewIdResourceName();
                    sibling.recycle();
                    return viewId != null ? viewId : "";
                }
                sibling.recycle();
            }
        }
        return "";
    }

    // ────────── 核心功能2: 抓取聊天气泡 ──────────

    private boolean grabBubblesIfInChat() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<BubbleItem> bubbles = new ArrayList<>();
        extractBubbles(root, bubbles);
        root.recycle();

        if (!bubbles.isEmpty()) {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (BubbleItem b : bubbles) { arr.put(b.toJson()); }
            broadcastResult("grab_bubbles", arr.toString());
            return true;
        }
        return false;
    }

    public void grabBubbles() {
        grabBubblesIfInChat();
    }

    private void extractBubbles(AccessibilityNodeInfo node, List<BubbleItem> results) {
        if (node == null) return;

        CharSequence className = node.getClassName();
        CharSequence text = node.getText();
        String viewId = node.getViewIdResourceName();

        // 微信气泡特征:
        // - 自己发的气泡: android:id 包含 "right" 或 "send"
        // - 对方发的气泡: android:id 包含 "left" 或 "receive"
        // - 内容在 TextView 中

        if (text != null && text.length() > 0
            && className != null && className.toString().contains("TextView")) {

            String content = text.toString().trim();
            if (content.length() > 0 && !content.equals(lastProcessedText)) {
                BubbleItem bubble = new BubbleItem();
                bubble.content = content;
                bubble.viewId = viewId != null ? viewId : "";

                // 判断是自己还是对方（根据id或布局方向）
                if (viewId != null && (viewId.contains("right") || viewId.contains("send"))) {
                    bubble.isFromMe = true;
                } else if (viewId != null && (viewId.contains("left") || viewId.contains("receive"))) {
                    bubble.isFromMe = false;
                } else {
                    // 通过父布局判断
                    AccessibilityNodeInfo parent = node.getParent();
                    if (parent != null) {
                        String parentId = parent.getViewIdResourceName();
                        bubble.isFromMe = parentId != null
                            && (parentId.contains("right") || parentId.contains("send"));
                        parent.recycle();
                    }
                }
                results.add(bubble);
                lastProcessedText = content;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                extractBubbles(child, results);
                child.recycle();
            }
        }
    }

    // ────────── 核心功能3: 自动化操作(模拟点击+输入+发送) ──────────

    /** 模拟点击某个节点 */
    public boolean performTapOnNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        addRandomDelay(300, 800);
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    /** 按resource id查找并点击 */
    public boolean tapNodeById(String viewId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo target = findNodeByViewId(root, viewId);
        if (target != null) {
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            target.recycle();
            root.recycle();
            return true;
        }
        root.recycle();
        return false;
    }

    /** 按文本内容查找并点击 */
    public boolean tapNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            for (AccessibilityNodeInfo n : nodes) n.recycle();
            root.recycle();
            return true;
        }
        root.recycle();
        return false;
    }

    /** 在输入框中输入文本（支持中英文） */
    public void typeText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo editField = findEditableNode(root);
        if (editField != null) {
            addRandomDelay(300, 1000);
            // 先聚焦
            editField.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            addRandomDelay(100, 300);
            // 清空已有内容
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle argsClear = new Bundle();
                argsClear.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
                editField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, argsClear);
            }
            addRandomDelay(200, 500);
            // 逐字符输入(模拟真人打字)
            for (char c : text.toCharArray()) {
                Bundle args = new Bundle();
                args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    String.valueOf(c));
                // 每次追加一个字符
                editField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                addRandomDelay(50, 200); // 打字间隔
            }
        }
        if (root != null) root.recycle();
    }

    /** 输入文本并自动发送 */
    public void typeTextAndSend(String text) {
        typeText(text);
        addRandomDelay(500, 1500);
        clickSendButton();
    }

    /** 查找并点击发送按钮 */
    public boolean clickSendButton() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        // 按多种特征查找发送按钮
        // 微信发送按钮: android:id 包含 "send" 或 text 为 "发送" / "Send"
        List<AccessibilityNodeInfo> sendNodes = root.findAccessibilityNodeInfosByText("发送");
        if (sendNodes == null || sendNodes.isEmpty()) {
            sendNodes = root.findAccessibilityNodeInfosByText("Send");
        }
        if (sendNodes != null && !sendNodes.isEmpty()) {
            for (AccessibilityNodeInfo n : sendNodes) {
                if (n.isClickable()) {
                    addRandomDelay(300, 800);
                    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    for (AccessibilityNodeInfo node : sendNodes) node.recycle();
                    root.recycle();
                    return true;
                }
            }
            for (AccessibilityNodeInfo n : sendNodes) n.recycle();
        }

        // 回退：查找class包含"Button"且viewId包含"send"的节点
        AccessibilityNodeInfo btn = findSendButton(root);
        if (btn != null) {
            addRandomDelay(300, 800);
            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            btn.recycle();
            root.recycle();
            return true;
        }
        root.recycle();
        return false;
    }

    // ────────── 工具方法 ──────────

    private AccessibilityNodeInfo findNodeByViewId(AccessibilityNodeInfo root, String viewId) {
        if (root == null) return null;
        if (viewId.equals(root.getViewIdResourceName())) return root;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByViewId(child, viewId);
                child.recycle();
                if (result != null) return result;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        if (root.isEditable()) return root;
        if (root.getClassName() != null
            && root.getClassName().toString().contains("EditText")) return root;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findEditableNode(child);
                child.recycle();
                if (result != null) return result;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findSendButton(AccessibilityNodeInfo root) {
        if (root == null) return null;
        String desc = root.getContentDescription() != null
            ? root.getContentDescription().toString().toLowerCase() : "";
        String text = root.getText() != null ? root.getText().toString() : "";
        if (desc.contains("send") || text.contains("发送") || text.equals("Send")) {
            return root;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findSendButton(child);
                child.recycle();
                if (result != null) return result;
            }
        }
        return null;
    }

    private boolean isSupported(String pkg) {
        for (String supported : SUPPORTED_PACKAGES) {
            if (supported.equals(pkg)) return true;
        }
        return false;
    }

    private void addRandomDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + random.nextInt(maxMs - minMs));
        } catch (InterruptedException ignored) {}
    }

    /** 发送抓取结果广播（由FloatingView/Flutter接收） */
    private void broadcastResult(String type, String jsonData) {
        Intent intent = new Intent(ACTION_GRAB_RESULT);
        intent.putExtra("result_type", type);
        intent.putExtra("result_json", jsonData);
        intent.putExtra(EXTRA_CURRENT_PKG, currentPkg);
        sendBroadcast(intent);
        Log.d(TAG, "Broadcast result: " + type + " data=" + jsonData);
    }

    // ────────── 数据类 ──────────

    public static class ContactItem {
        public String nickname = "";
        public String packageName = "";
        public String avatarViewId = "";
        public String bio = "";

        public org.json.JSONObject toJson() {
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("nickname", nickname);
                obj.put("packageName", packageName);
                obj.put("avatarViewId", avatarViewId);
                obj.put("bio", bio);
            } catch (Exception ignored) {}
            return obj;
        }
    }

    public static class BubbleItem {
        public String content = "";
        public boolean isFromMe = false;
        public String viewId = "";
        public long timestamp = System.currentTimeMillis();

        public org.json.JSONObject toJson() {
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("content", content);
                obj.put("isFromMe", isFromMe);
                obj.put("viewId", viewId);
                obj.put("timestamp", timestamp);
            } catch (Exception ignored) {}
            return obj;
        }
    }
}
