import 'dart:convert';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as p;
import '../models/contact.dart';
import '../models/message.dart';

/// 本地 SQLite 数据库服务
/// 联系人、对话记录、记忆数据均为本地存储
class DatabaseService {
  static Database? _db;

  static Future<Database> get database async {
    if (_db != null) return _db!;
    _db = await _initDatabase();
    return _db!;
  }

  static Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = p.join(dbPath, 'chatwing.db');
    return openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE contacts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nickname TEXT NOT NULL,
            remark TEXT DEFAULT '',
            sourcePackage TEXT DEFAULT '',
            profileBio TEXT DEFAULT '',
            profileAnalysis TEXT DEFAULT '',
            lastActiveAt INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
          )
        ''');
        await db.execute('''
          CREATE TABLE messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            contactId INTEGER NOT NULL,
            content TEXT NOT NULL,
            isFromMe INTEGER NOT NULL DEFAULT 0,
            timestamp INTEGER NOT NULL,
            style TEXT,
            analysis TEXT,
            FOREIGN KEY (contactId) REFERENCES contacts(id) ON DELETE CASCADE
          )
        ''');
        await db.execute('''
          CREATE TABLE memories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            contactId INTEGER NOT NULL,
            type TEXT NOT NULL DEFAULT 'summary',
            content TEXT NOT NULL,
            tags TEXT DEFAULT '',
            timestamp INTEGER NOT NULL,
            embedding TEXT DEFAULT '',
            FOREIGN KEY (contactId) REFERENCES contacts(id) ON DELETE CASCADE
          )
        ''');
        await db.execute(
            'CREATE INDEX idx_messages_contact ON messages(contactId)');
        await db.execute(
            'CREATE INDEX idx_memories_contact ON memories(contactId)');
      },
    );
  }

  // ── 联系人 CRUD ──

  static Future<List<Contact>> getContacts() async {
    final db = await database;
    final maps = await db.query('contacts', orderBy: 'lastActiveAt DESC');
    return maps.map((m) => Contact.fromMap(m)).toList();
  }

  static Future<Contact?> getContact(int id) async {
    final db = await database;
    final maps = await db.query('contacts', where: 'id = ?', whereArgs: [id]);
    if (maps.isEmpty) return null;
    return Contact.fromMap(maps.first);
  }

  static Future<int> insertContact(Contact contact) async {
    final db = await database;
    return db.insert('contacts', contact.toMap()..remove('id'));
  }

  static Future<void> updateContact(Contact contact) async {
    final db = await database;
    await db.update('contacts', contact.toMap(),
        where: 'id = ?', whereArgs: [contact.id]);
  }

  static Future<void> deleteContact(int id) async {
    final db = await database;
    await db.delete('messages', where: 'contactId = ?', whereArgs: [id]);
    await db.delete('memories', where: 'contactId = ?', whereArgs: [id]);
    await db.delete('contacts', where: 'id = ?', whereArgs: [id]);
  }

  // ── 消息 CRUD ──

  static Future<List<ChatMessage>> getMessages(int contactId,
      {int limit = 50}) async {
    final db = await database;
    final maps = await db.query('messages',
        where: 'contactId = ?',
        whereArgs: [contactId],
        orderBy: 'timestamp DESC',
        limit: limit);
    return maps.reversed.map((m) => ChatMessage.fromMap(m)).toList();
  }

  static Future<void> insertMessage(int contactId, ChatMessage msg) async {
    final db = await database;
    await db.insert('messages', {
      ...msg.toMap(),
      'contactId': contactId,
    });
  }

  // ── 记忆 CRUD ──

  static Future<List<Map<String, dynamic>>> getMemories(int contactId) async {
    final db = await database;
    return db.query('memories',
        where: 'contactId = ?',
        whereArgs: [contactId],
        orderBy: 'timestamp DESC');
  }

  static Future<void> addMemory(
      int contactId, String type, String content) async {
    final db = await database;
    await db.insert('memories', {
      'contactId': contactId,
      'type': type,
      'content': content,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  static Future<void> deleteAllMemories(int contactId) async {
    final db = await database;
    await db.delete('memories', where: 'contactId = ?', whereArgs: [contactId]);
  }
}
