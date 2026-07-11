import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import '../models/profile.dart';

class ProfileState extends ChangeNotifier {
  static const _key = 'chatwing_user_profile';

  UserProfile _profile = UserProfile();
  bool _isLoading = false;
  bool _isSaved = false;

  UserProfile get profile => _profile;
  bool get isLoading => _isLoading;
  bool get isSaved => _isSaved;

  Future<void> loadProfile() async {
    _isLoading = true;
    notifyListeners();
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonStr = prefs.getString(_key);
      if (jsonStr != null && jsonStr.isNotEmpty) {
        final map = jsonDecode(jsonStr) as Map<String, dynamic>;
        _profile = UserProfile.fromMap(map);
        _isSaved = true;
      }
    } catch (e) {
      debugPrint('loadProfile failed: ');
    }
    _isLoading = false;
    notifyListeners();
  }

  Future<void> saveProfile(UserProfile profile) async {
    _profile = profile;
    _isLoading = true;
    notifyListeners();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, jsonEncode(profile.toMap()));
      _isSaved = true;
    } catch (e) {
      debugPrint('saveProfile failed: ');
    }
    _isLoading = false;
    notifyListeners();
  }

  void updateField(String field, String value) {
    switch (field) {
      case 'age': _profile.age = value; break;
      case 'zodiac': _profile.zodiac = value; break;
      case 'mbti': _profile.mbti = value; break;
      case 'occupation': _profile.occupation = value; break;
      case 'income': _profile.income = value; break;
      case 'hobbies': _profile.hobbies = value; break;
      case 'loveView': _profile.loveView = value; break;
      case 'verbalTics': _profile.verbalTics = value; break;
      case 'extraInfo': _profile.extraInfo = value; break;
    }
    notifyListeners();
  }

  Future<void> resetProfile() async {
    _profile = UserProfile();
    _isSaved = false;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_key);
    notifyListeners();
  }
}