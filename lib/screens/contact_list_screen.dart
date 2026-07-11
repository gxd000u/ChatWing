import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/contact.dart';
import '../providers/contact_state.dart';
import 'contact_detail_screen.dart';

/// 联系人列表页面
class ContactListScreen extends StatefulWidget {
  const ContactListScreen({super.key});

  @override
  State<ContactListScreen> createState() => _ContactListScreenState();
}

class _ContactListScreenState extends State<ContactListScreen> {
  @override
  void initState() {
    super.initState();
    context.read<ContactState>().loadContacts();
  }

  @override
  Widget build(BuildContext context) {
    final contactState = context.watch<ContactState>();

    return Scaffold(
      appBar: AppBar(title: const Text('联系人管理')),
      body: contactState.isLoading
          ? const Center(child: CircularProgressIndicator())
          : contactState.contacts.isEmpty
              ? const Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.people_outline,
                          size: 64, color: Colors.grey),
                      SizedBox(height: 16),
                      Text('暂无联系人',
                          style: TextStyle(color: Colors.grey, fontSize: 16)),
                      SizedBox(height: 8),
                      Text('打开无障碍服务后，进入微信聊天\n将自动抓取联系人信息',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.grey)),
                    ],
                  ),
                )
              : ListView.builder(
                  itemCount: contactState.contacts.length,
                  itemBuilder: (_, i) {
                    final contact = contactState.contacts[i];
                    return _ContactTile(
                      contact: contact,
                      onTap: () {
                        contactState.selectContact(contact);
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (_) => const ContactDetailScreen()),
                        );
                      },
                      onDelete: () => _confirmDelete(contact),
                    );
                  },
                ),
    );
  }

  void _confirmDelete(Contact contact) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('删除联系人'),
        content: Text('确定删除「${contact.nickname}」及其全部聊天记忆？'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('取消')),
          TextButton(
            onPressed: () {
              context.read<ContactState>().deleteContact(contact.id!);
              Navigator.pop(ctx);
            },
            child:
                Text('删除', style: TextStyle(color: Theme.of(ctx).colorScheme.error)),
          ),
        ],
      ),
    );
  }
}

class _ContactTile extends StatelessWidget {
  final Contact contact;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  const _ContactTile({
    required this.contact,
    required this.onTap,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
        child: Text(
          (contact.remark.isNotEmpty ? contact.remark : contact.nickname)
              .substring(0, 1),
          style: TextStyle(
              color: Theme.of(context).colorScheme.onPrimaryContainer),
        ),
      ),
      title: Text(contact.remark.isNotEmpty ? contact.remark : contact.nickname),
      subtitle: Text(contact.profileBio.isNotEmpty
          ? contact.profileBio
          : contact.sourcePackage),
      trailing: IconButton(
        icon: const Icon(Icons.delete_outline),
        onPressed: onDelete,
      ),
      onTap: onTap,
    );
  }
}
