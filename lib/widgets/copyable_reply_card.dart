import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// 带一键复制功能的回复卡片（同时适用于军师模式5-6条列表）
class CopyableReplyCard extends StatefulWidget {
  final String reply;
  final int index;
  final bool showAnalysis;
  final String? analysis;
  final VoidCallback? onUse;       // 使用这条回复（填入输入框）
  final VoidCallback? onRefresh;   // 换一批

  const CopyableReplyCard({
    super.key,
    required this.reply,
    required this.index,
    this.showAnalysis = false,
    this.analysis,
    this.onUse,
    this.onRefresh,
  });

  @override
  State<CopyableReplyCard> createState() => _CopyableReplyCardState();
}

class _CopyableReplyCardState extends State<CopyableReplyCard> {
  bool _copied = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 头部: 序号 + 标签
            Row(
              children: [
                CircleAvatar(
                  radius: 12,
                  backgroundColor: theme.colorScheme.primaryContainer,
                  child: Text('${widget.index + 1}',
                      style: TextStyle(
                          fontSize: 12,
                          color: theme.colorScheme.onPrimaryContainer)),
                ),
                const SizedBox(width: 8),
                Text(
                  widget.showAnalysis ? '回复候选' : '回复',
                  style: theme.textTheme.labelMedium,
                ),
                const Spacer(),
                // 复制按钮
                IconButton(
                  icon: Icon(
                    _copied ? Icons.check : Icons.copy,
                    size: 18,
                    color: _copied ? Colors.green : null,
                  ),
                  tooltip: '复制',
                  onPressed: () => _copyReply(),
                ),
                if (widget.onUse != null)
                  IconButton(
                    icon: const Icon(Icons.input, size: 18),
                    tooltip: '使用此回复',
                    onPressed: widget.onUse,
                  ),
              ],
            ),
            const SizedBox(height: 8),
            // 回复内容（可点击选中）
            InkWell(
              onLongPress: () => _copyReply(),
              borderRadius: BorderRadius.circular(8),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: theme.colorScheme.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  widget.reply,
                  style: theme.textTheme.bodyMedium?.copyWith(height: 1.5),
                ),
              ),
            ),
            // 心理分析（军师模式）
            if (widget.showAnalysis && widget.analysis != null && widget.analysis!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Row(
                  children: [
                    Icon(Icons.psychology, size: 14, color: theme.colorScheme.tertiary),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(widget.analysis!,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.tertiary,
                          )),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  void _copyReply() {
    Clipboard.setData(ClipboardData(text: widget.reply));
    setState(() => _copied = true);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('已复制到剪贴板'),
          behavior: SnackBarBehavior.floating,
          duration: const Duration(seconds: 2),
        ),
      );
    }
    Future.delayed(const Duration(seconds: 1), () {
      if (mounted) setState(() => _copied = false);
    });
  }
}

/// 复制全部按钮（放在回复列表底部）
class CopyAllButton extends StatelessWidget {
  final List<String> replies;
  const CopyAllButton({super.key, required this.replies});

  @override
  Widget build(BuildContext context) {
    if (replies.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: OutlinedButton.icon(
        onPressed: () {
          final allText = replies.asMap().entries
              .map((e) => '${e.key + 1}. ${e.value}')
              .join('\n\n');
          Clipboard.setData(ClipboardData(text: allText));
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('已复制全部 ${replies.length} 条回复'),
              behavior: SnackBarBehavior.floating,
            ),
          );
        },
        icon: const Icon(Icons.copy_all),
        label: Text('一键复制全部（${replies.length} 条）'),
      ),
    );
  }
}
