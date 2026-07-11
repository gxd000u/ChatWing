import 'package:flutter/foundation.dart';
import '../models/contact.dart';
import '../services/database_service.dart';

/// 联系人状态管理
class ContactState extends ChangeNotifier {
  List<Contact> _contacts = [];
  Contact? _selectedContact;
  bool _isLoading = false;

  List<Contact> get contacts => _contacts;
  Contact? get selectedContact => _selectedContact;
  bool get isLoading => _isLoading;

  /// 加载所有联系人
  Future<void> loadContacts() async {
    _isLoading = true;
    notifyListeners();
    try {
      _contacts = await DatabaseService.getContacts();
    } catch (e) {
      debugPrint('加载联系人失败: $e');
    }
    _isLoading = false;
    notifyListeners();
  }

  /// 选择联系人
  void selectContact(Contact contact) {
    _selectedContact = contact;
    notifyListeners();
  }

  /// 添加联系人
  Future<void> addContact(Contact contact) async {
    final id = await DatabaseService.insertContact(contact);
    _contacts.insert(0, contact..id.hashCode); // placeholder
    await loadContacts(); // reload
  }

  /// 删除联系人
  Future<void> deleteContact(int id) async {
    await DatabaseService.deleteContact(id);
    _contacts.removeWhere((c) => c.id == id);
    if (_selectedContact?.id == id) _selectedContact = null;
    notifyListeners();
  }

  /// 更新联系人备注
  Future<void> updateRemark(int id, String remark) async {
    final contact = _contacts.firstWhere((c) => c.id == id);
    contact.remark = remark;
    await DatabaseService.updateContact(contact);
    notifyListeners();
  }

  /// 更新联系人主页分析
  Future<void> updateProfileAnalysis(int id, String analysis) async {
    final contact = _contacts.firstWhere((c) => c.id == id);
    contact.profileAnalysis = analysis;
    await DatabaseService.updateContact(contact);
    notifyListeners();
  }

  void clearSelection() {
    _selectedContact = null;
    notifyListeners();
  }
}
