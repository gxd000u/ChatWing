import 'package:flutter/material.dart';
import 'dart:math' as math;

/// 可拖拽的悬浮球覆盖层
/// 在应用内模拟系统级悬浮球的效果
class FloatingBallOverlay extends StatefulWidget {
  final Widget child;
  final double ballSize;
  final Color ballColor;

  const FloatingBallOverlay({
    super.key,
    required this.child,
    this.ballSize = 48,
    this.ballColor = const Color(0xFF6C63FF),
  });

  @override
  State<FloatingBallOverlay> createState() => _FloatingBallOverlayState();
}

class _FloatingBallOverlayState extends State<FloatingBallOverlay> {
  bool _isPanelVisible = false;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        widget.child,
        if (_isPanelVisible)
          Positioned(
            right: 16,
            top: MediaQuery.of(context).padding.top + 80,
            child: _buildPanel(),
          ),
        Positioned(
          right: 16,
          bottom: kFloatingActionButtonMargin + 80,
          child: GestureDetector(
            onTap: () => setState(() => _isPanelVisible = !_isPanelVisible),
            child: Container(
              width: widget.ballSize,
              height: widget.ballSize,
              decoration: BoxDecoration(
                color: widget.ballColor.withOpacity(0.9),
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.3),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: const Icon(Icons.auto_awesome, color: Colors.white, size: 24),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPanel() {
    return Material(
      elevation: 8,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        width: 280,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surface,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                const Icon(Icons.auto_awesome, size: 20),
                const SizedBox(width: 8),
                Text('ChatWing 助手',
                    style: Theme.of(context).textTheme.titleSmall),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close, size: 18),
                  onPressed: () => setState(() => _isPanelVisible = false),
                  visualDensity: VisualDensity.compact,
                ),
              ],
            ),
            const Divider(),
            const ListTile(
              leading: Icon(Icons.auto_mode, size: 20),
              title: Text('全托管·赛博替身', style: TextStyle(fontSize: 14)),
              dense: true,
            ),
            const ListTile(
              leading: Icon(Icons.psychology, size: 20),
              title: Text('半自动·策略军师', style: TextStyle(fontSize: 14)),
              dense: true,
            ),
            const ListTile(
              leading: Icon(Icons.lightbulb, size: 20),
              title: Text('纯手动·灵感文案', style: TextStyle(fontSize: 14)),
              dense: true,
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: () {},
                child: const Text('粘贴即回'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
