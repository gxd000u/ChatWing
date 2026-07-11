/// 用户人物小传（基础 Prompt 数据源）
class UserProfile {
  String age;
  String zodiac;
  String mbti;
  String occupation;
  String income;
  String hobbies;
  String loveView;
  String verbalTics;
  String extraInfo;

  UserProfile({
    this.age = '',
    this.zodiac = '',
    this.mbti = '',
    this.occupation = '',
    this.income = '',
    this.hobbies = '',
    this.loveView = '',
    this.verbalTics = '',
    this.extraInfo = '',
  });

  Map<String, dynamic> toMap() => {
        'age': age,
        'zodiac': zodiac,
        'mbti': mbti,
        'occupation': occupation,
        'income': income,
        'hobbies': hobbies,
        'loveView': loveView,
        'verbalTics': verbalTics,
        'extraInfo': extraInfo,
      };

  factory UserProfile.fromMap(Map<String, dynamic> map) => UserProfile(
        age: map['age'] as String? ?? '',
        zodiac: map['zodiac'] as String? ?? '',
        mbti: map['mbti'] as String? ?? '',
        occupation: map['occupation'] as String? ?? '',
        income: map['income'] as String? ?? '',
        hobbies: map['hobbies'] as String? ?? '',
        loveView: map['loveView'] as String? ?? '',
        verbalTics: map['verbalTics'] as String? ?? '',
        extraInfo: map['extraInfo'] as String? ?? '',
      );

  /// 生成为 system prompt 用的人物设定文本
  String toPromptString() {
    final parts = <String>[];
    if (age.isNotEmpty) parts.add('年龄：$age');
    if (zodiac.isNotEmpty) parts.add('星座：$zodiac');
    if (mbti.isNotEmpty) parts.add('MBTI：$mbti');
    if (occupation.isNotEmpty) parts.add('职业：$occupation');
    if (income.isNotEmpty) parts.add('收入：$income');
    if (hobbies.isNotEmpty) parts.add('兴趣爱好：$hobbies');
    if (loveView.isNotEmpty) parts.add('感情观：$loveView');
    if (verbalTics.isNotEmpty) parts.add('经典口癖：$verbalTics');
    if (extraInfo.isNotEmpty) parts.add('额外信息：$extraInfo');
    return parts.isEmpty ? '未设定' : parts.join('\n');
  }
}

/// 联系人主页分析结果
class ProfileAnalysis {
  final String rawText;
  final List<String> icebreakerTopics;   // 破冰话题
  final Map<String, int> interestGraph;  // 兴趣图谱

  ProfileAnalysis({
    this.rawText = '',
    this.icebreakerTopics = const [],
    this.interestGraph = const {},
  });
}
