import 'dart:convert';
import 'package:flutter/services.dart';

/// Flutter 与 Android 原生之间的桥梁
/// 通过 MethodChannel 调用原生功能
class PlatformBridge {
  static const _channel = MethodChannel('com.chatwing/native');
  static const _eventChannel = MethodChannel('com.chatwing/events');

  static late final PlatformBridge instance;

  /// 事件回调
  Function(String text, String packageName)? onNewMessage;
  Function(String nickname, String packageName)? onContactDetected;
  Function(String path)? onScreenshotTaken;

  PlatformBridge._() {
    _eventChannel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onNewMessage':
          final text = call.arguments['text'] as String? ?? '';
          final pkg = call.arguments['packageName'] as String? ?? '';
          onNewMessage?.call(text, pkg);
          break;
        case 'onContactDetected':
          final name = call.arguments['nickname'] as String? ?? '';
          final pkg = call.arguments['packageName'] as String? ?? '';
          onContactDetected?.call(name, pkg);
          break;
        case 'onScreenshotTaken':
          final path = call.arguments['path'] as String? ?? '';
          onScreenshotTaken?.call(path);
          break;
      }
      return null;
    });
  }

  static void init() => instance = PlatformBridge._();

  // ── 悬浮窗 ──
  static Future<bool> startFloatingWindow() async {
    return _channel.invokeMethod('startFloatingWindow');
  }

  static Future<bool> stopFloatingWindow() async {
    return _channel.invokeMethod('stopFloatingWindow');
  }

  // ── 屏幕捕获 ──
  static Future<bool> startScreenCapture(
      int resultCode, String data) async {
    return _channel
        .invokeMethod('startScreenCapture', {'resultCode': resultCode, 'data': data});
  }

  static Future<bool> stopScreenCapture() async {
    return _channel.invokeMethod('stopScreenCapture');
  }

  // ── 无障碍服务 ──
  static Future<bool> enableAccessibilityService() async {
    return _channel.invokeMethod('enableAccessibilityService');
  }

  // ── 自动化操作 ──
  static Future<bool> sendText(String text) async {
    return _channel.invokeMethod('sendText', {'text': text});
  }

  static Future<bool> clickSend() async {
    return _channel.invokeMethod('clickSend');
  }

  // ── 联系人 ──
  static Future<List<Map<String, dynamic>>> getContacts() async {
    final result = await _channel.invokeMethod('getContacts');
    if (result == null) return [];
    final list = jsonDecode(result as String) as List;
    return list.cast<Map<String, dynamic>>();
  }

  static Future<bool> deleteContact(int id) async {
    return _channel.invokeMethod('deleteContact', {'id': id});
  }

  // ── 引擎控制 ──
  static Future<bool> startAutoPilot() async {
    return _channel.invokeMethod('startAutoPilot');
  }

  static Future<bool> stopAutoPilot() async {
    return _channel.invokeMethod('stopAutoPilot');
  }

  static Future<String> generateReply(
      String engineType, String lastMessage, String context) async {
    return _channel.invokeMethod('generateReply', {
      'engineType': engineType,
      'lastMessage': lastMessage,
      'context': context,
    });
  }

  // ── 设置 ──
  static Future<bool> switchProvider(String provider) async {
    return _channel.invokeMethod('switchProvider', {'provider': provider});
  }

  static Future<bool> updateProfile(Map<String, dynamic> profile) async {
    return _channel.invokeMethod('updateProfile', profile);
  }
}
