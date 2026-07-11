import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/contact_state.dart';
import '../services/database_service.dart';
import '../models/message.dart';

/// 联系人详情页面
/// 显示对话历史、记忆库、主页分析
class ContactDetailScreen extends StatefulWidget {
  const ContactDetailScreen({super.key});

  @override
  State<ContactDetailScreen> createState() => _ContactDetailScreenState();
}

class _ContactDetailScreenState extends State<ContactDetailScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  List<ChatMessage> _messages = [];
  List<Map<String, dynamic>> _memories = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _loadData();
  }

  Future<void> _loadData() async {
    final contact = context.read<ContactState>().selectedContact;
    if (contact?.id == null) return;
    final messages = await DatabaseService.getMessages(contact!.id!);
    final memories = await DatabaseService.getMemories(contact.id!);
    setState(() {
      _messages = messages;
      _memories = memories;
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final contact = context.watch<ContactState>().selectedContact;
    if (contact == null) return const Scaffold(body: Center(child: Text('未选择联系人')));

    return Scaffold(
      appBar: AppBar(
        title: Text(contact.remark.isNotEmpty ? contact.remark : contact.nickname),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '对话', icon: Icon(Icons.chat)),
            Tab(text: '记忆', icon: Icon(Icons.memory)),
            Tab(text: '主页分析', icon: Icon(Icons.analytics)),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _MessageList(messages: _messages),
          _MemoryList(memories: _memories, contactId: contact.id!),
          _ProfileAnalysisView(contact: contact),
        ],
      ),
    );
  }
}

class _MessageList extends StatelessWidget {
  final List<ChatMessage> messages;
  const _MessageList({required this.messages});

  @override
  Widget build(BuildContext context) {
    if (messages.isEmpty) {
      return const Center(child: Text('暂无对话记录'));
    }
    return ListView.builder(
      itemCount: messages.length,
      itemBuilder: (_, i) {
        final msg = messages[i];
        return ListTile(
          leading: Icon(msg.isFromMe ? Icons.person : Icons.person_outline,
              color: msg.isFromMe ? Colors.green : Colors.grey),
          title: Text(msg.content),
          subtitle: Text(
            '${msg.isFromMe ? "我" : "对方"} · ${msg.timestamp.toString().substring(0, 16)}',
            style: const TextStyle(fontSize: 12),
          ),
        );
      },
    );
  }
}

class _MemoryList extends StatelessWidget {
  final List<Map<String, dynamic>> memories;
  final int contactId;
  const _MemoryList({required this.memories, required this.contactId});

  @override
  Widget build(BuildContext context) {
    if (memories.isEmpty) {
      return const Center(child: Text('暂无记忆'));
    }
    return ListView(
      children: memories.map((m) {
        final type = m['type'] as String;
        final content = m['content'] as String;
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          child: ListTile(
            leading: Icon(
              type == 'summary'
                  ? Icons.summarize
                  : type == 'key_info'
                      ? Icons.info
                      : Icons.label,
            ),
            title: Text(content, maxLines: 3, overflow: TextOverflow.ellipsis),
            subtitle: Text(type),
          ),
        );
      }).toList(),
    );
  }
}

class _ProfileAnalysisView extends StatelessWidget {
  final dynamic contact;
  const _ProfileAnalysisView({required this.contact});

  @override
  Widget build(BuildContext context) {
    final analysis = contact.profileAnalysis as String? ?? '';
    if (analysis.isEmpty) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.analytics_outlined, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text('暂无主页分析', style: TextStyle(color: Colors.grey, fontSize: 16)),
            SizedBox(height: 8),
            Text('截图对方主页后，AI 将自动生成破冰话题和兴趣图谱',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Text(analysis),
    );
  }
}
