import 'package:flutter/material.dart';

/// 新手引导覆盖层组件（3-4页）
class OnboardingOverlay extends StatefulWidget {
  final VoidCallback onComplete;
  const OnboardingOverlay({super.key, required this.onComplete});

  @override
  State<OnboardingOverlay> createState() => _OnboardingOverlayState();
}

class _OnboardingOverlayState extends State<OnboardingOverlay> {
  final _controller = PageController();
  int _currentPage = 0;

  final _pages = [
    _OnboardPage(
      icon: Icons.accessibility_new,
      title: '开启无障碍服务',
      desc: 'ChatWing 通过无障碍服务监听聊天界面，\n自动抓取联系人和消息。\n请在设置页开启。',
      color: Colors.blue,
    ),
    _OnboardPage(
      icon: Icons.blur_on,
      title: '开启悬浮窗权限',
      desc: '悬浮球让你在任意界面快速使用 ChatWing。\n点击「设置 → 悬浮窗」跳转授权。',
      color: Colors.purple,
    ),
    _OnboardPage(
      icon: Icons.people,
      title: '抓取第一个联系人',
      desc: '打开微信，切换回 ChatWing 点击「抓取联系人」。\n系统会自动识别聊天列表。',
      color: Colors.green,
    ),
    _OnboardPage(
      icon: Icons.psychology,
      title: '开始使用',
      desc: '选择模式：\n• 替身 — 全自动托管\n• 军师 — 生成回复候选\n• 灵感 — 粘贴即回\n\n设置完成，开始你的社交之旅！',
      color: Colors.orange,
    ),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.black87,
      child: Stack(
        children: [
          PageView(
            controller: _controller,
            onPageChanged: (i) => setState(() => _currentPage = i),
            children: _pages,
          ),
          // 底部控制
          Positioned(
            bottom: 60,
            left: 0,
            right: 0,
            child: Column(
              children: [
                // 小圆点指示器
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(
                    _pages.length,
                    (i) => AnimatedContainer(
                      duration: const Duration(milliseconds: 300),
                      margin: const EdgeInsets.symmetric(horizontal: 4),
                      width: _currentPage == i ? 24 : 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: _currentPage == i
                            ? Colors.white
                            : Colors.white38,
                        borderRadius: BorderRadius.circular(4),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                // 按钮
                TextButton(
                  onPressed: () {
                    if (_currentPage < _pages.length - 1) {
                      _controller.nextPage(
                        duration: const Duration(milliseconds: 400),
                        curve: Curves.easeInOut,
                      );
                    } else {
                      widget.onComplete();
                    }
                  },
                  style: TextButton.styleFrom(
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(
                        horizontal: 32, vertical: 12),
                  ),
                  child: Text(
                    _currentPage < _pages.length - 1 ? '下一步' : '开始使用',
                    style: const TextStyle(fontSize: 18),
                  ),
                ),
              ],
            ),
          ),
          // 跳过按钮
          Positioned(
            top: 40,
            right: 16,
            child: TextButton(
              onPressed: widget.onComplete,
              child: const Text('跳过', style: TextStyle(color: Colors.white54)),
            ),
          ),
        ],
      ),
    );
  }
}

class _OnboardPage extends StatelessWidget {
  final IconData icon;
  final String title;
  final String desc;
  final Color color;

  const _OnboardPage({
    required this.icon,
    required this.title,
    required this.desc,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: color.withOpacity(0.2),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 64, color: Colors.white),
          ),
          const SizedBox(height: 32),
          Text(
            title,
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            desc,
            style: const TextStyle(
              fontSize: 16,
              color: Colors.white70,
              height: 1.5,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
