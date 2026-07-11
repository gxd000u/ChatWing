import 'package:flutter/material.dart';

/// 回复候选卡片
/// 显示策略军师模式生成的各风格回复
/// 点击可将回复填入输入框
class ReplyCard extends StatelessWidget {
  final String reply;
  final int index;
  final ValueChanged<String> onTap;
  final VoidCallback? onRefresh;

  const ReplyCard({
    super.key,
    required this.reply,
    required this.index,
    required this.onTap,
    this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final colors = [
      colorScheme.primaryContainer,
      colorScheme.secondaryContainer,
      colorScheme.tertiaryContainer,
      colorScheme.errorContainer,
      colorScheme.surfaceContainerHighest,
      colorScheme.surfaceContainer,
    ];

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: colors[index % colors.length],
      child: ListTile(
        leading: CircleAvatar(
          child: Text('${index + 1}',
              style: TextStyle(
                  color: colorScheme.onPrimaryContainer, fontSize: 12)),
        ),
        title: Text(reply, style: const TextStyle(fontSize: 14)),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(Icons.copy, size: 18),
              tooltip: '复制',
              onPressed: () => onTap(reply),
            ),
            if (onRefresh != null && index == 0)
              IconButton(
                icon: const Icon(Icons.refresh, size: 18),
                tooltip: '换一批',
                onPressed: onRefresh,
              ),
          ],
        ),
        onTap: () => onTap(reply),
      ),
    );
  }
}
