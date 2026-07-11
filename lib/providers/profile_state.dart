import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dart:convert';
import '../models/profile.dart';

/// 用户人物小传状态管理
/// 数据加密存储在 flutter_secure_storage 中
class ProfileState extends ChangeNotifier {
  static const _storage = FlutterSecureStorage();
  static const _key = 'chatwing_user_profile';

  UserProfile _profile = UserProfile();
  bool _isLoading = false;
  bool _isSaved = false;

  UserProfile get profile => _profile;
  bool get isLoading => _isLoading;
  bool get isSaved => _isSaved;

  /// 从安全存储加载人物小传
  Future<void> loadProfile() async {
    _isLoading = true;
    notifyListeners();
    try {
      final jsonStr = await _storage.read(key: _key);
      if (jsonStr != null && jsonStr.isNotEmpty) {
        final map = jsonDecode(jsonStr) as Map<String, dynamic>;
        _profile = UserProfile.fromMap(map);
        _isSaved = true;
      }
    } catch (e) {
      debugPrint('加载人物小传失败: $e');
    }
    _isLoading = false;
    notifyListeners();
  }

  /// 保存人物小传（加密存储）
  Future<void> saveProfile(UserProfile profile) async {
    _profile = profile;
    _isLoading = true;
    notifyListeners();
    try {
      await _storage.write(key: _key, value: jsonEncode(profile.toMap()));
      _isSaved = true;
    } catch (e) {
      debugPrint('保存人物小传失败: $e');
    }
    _isLoading = false;
    notifyListeners();
  }

  /// 更新单个字段
  void updateField(String field, String value) {
    switch (field) {
      case 'age':
        _profile.age = value;
        break;
      case 'zodiac':
        _profile.zodiac = value;
        break;
      case 'mbti':
        _profile.mbti = value;
        break;
      case 'occupation':
        _profile.occupation = value;
        break;
      case 'income':
        _profile.income = value;
        break;
      case 'hobbies':
        _profile.hobbies = value;
        break;
      case 'loveView':
        _profile.loveView = value;
        break;
      case 'verbalTics':
        _profile.verbalTics = value;
        break;
      case 'extraInfo':
        _profile.extraInfo = value;
        break;
    }
    notifyListeners();
  }

  /// 重置
  Future<void> resetProfile() async {
    _profile = UserProfile();
    _isSaved = false;
    await _storage.delete(key: _key);
    notifyListeners();
  }
}
