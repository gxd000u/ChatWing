import 'dart:convert';
import 'package:flutter/services.dart';

/// Flutter ? Android ???????
/// ?? MethodChannel ??????
class PlatformBridge {
  static const _channel = MethodChannel('com.chatwing/native');
  static const _eventChannel = MethodChannel('com.chatwing/events');

  static late final PlatformBridge instance;

  /// ????
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

  // ?? ??? ??
  static Future<bool> startFloatingWindow() async {
    final r = await _channel.invokeMethod('startFloatingWindow');
    return r as bool;
  }

  static Future<bool> stopFloatingWindow() async {
    final r = await _channel.invokeMethod('stopFloatingWindow');
    return r as bool;
  }

  // ?? ???? ??
  static Future<bool> startScreenCapture(int resultCode, String data) async {
    final r = await _channel.invokeMethod('startScreenCapture', {
      'resultCode': resultCode,
      'data': data,
    });
    return r as bool;
  }

  static Future<bool> stopScreenCapture() async {
    final r = await _channel.invokeMethod('stopScreenCapture');
    return r as bool;
  }

  // ?? ????? ??
  static Future<bool> enableAccessibilityService() async {
    final r = await _channel.invokeMethod('enableAccessibilityService');
    return r as bool;
  }

  // ?? ????? ??
  static Future<bool> sendText(String text) async {
    final r = await _channel.invokeMethod('sendText', {'text': text});
    return r as bool;
  }

  static Future<bool> clickSend() async {
    final r = await _channel.invokeMethod('clickSend');
    return r as bool;
  }

  // ?? ??? ??
  static Future<List<Map<String, dynamic>>> getContacts() async {
    final result = await _channel.invokeMethod('getContacts');
    if (result == null) return [];
    final s = result as String;
    final list = jsonDecode(s) as List;
    return list.cast<Map<String, dynamic>>();
  }

  static Future<bool> deleteContact(int id) async {
    final r = await _channel.invokeMethod('deleteContact', {'id': id});
    return r as bool;
  }

  // ?? ???? ??
  static Future<bool> startAutoPilot() async {
    final r = await _channel.invokeMethod('startAutoPilot');
    return r as bool;
  }

  static Future<bool> stopAutoPilot() async {
    final r = await _channel.invokeMethod('stopAutoPilot');
    return r as bool;
  }

  static Future<String> generateReply(
      String engineType, String lastMessage, String context) async {
    final result = await _channel.invokeMethod('generateReply', {
      'engineType': engineType,
      'lastMessage': lastMessage,
      'context': context,
    });
    return (result ?? '') as String;
  }

  // ?? ?? ??
  static Future<bool> switchProvider(String provider) async {
    final r = await _channel.invokeMethod('switchProvider', {'provider': provider});
    return r as bool;
  }

  static Future<bool> updateProfile(Map<String, dynamic> profile) async {
    final r = await _channel.invokeMethod('updateProfile', profile);
    return r as bool;
  }
}
