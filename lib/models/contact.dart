/// 联系人数据模型
class Contact {
  final int? id;
  final String nickname;
  String remark;
  final String sourcePackage;
  String profileBio;
  String profileAnalysis;
  final DateTime lastActiveAt;
  final DateTime createdAt;

  Contact({
    this.id,
    required this.nickname,
    this.remark = '',
    this.sourcePackage = '',
    this.profileBio = '',
    this.profileAnalysis = '',
    DateTime? lastActiveAt,
    DateTime? createdAt,
  })  : lastActiveAt = lastActiveAt ?? DateTime.now(),
        createdAt = createdAt ?? DateTime.now();

  Map<String, dynamic> toMap() => {
        'id': id,
        'nickname': nickname,
        'remark': remark,
        'sourcePackage': sourcePackage,
        'profileBio': profileBio,
        'profileAnalysis': profileAnalysis,
        'lastActiveAt': lastActiveAt.millisecondsSinceEpoch,
        'createdAt': createdAt.millisecondsSinceEpoch,
      };

  factory Contact.fromMap(Map<String, dynamic> map) => Contact(
        id: map['id'] as int?,
        nickname: map['nickname'] as String,
        remark: map['remark'] as String? ?? '',
        sourcePackage: map['sourcePackage'] as String? ?? '',
        profileBio: map['profileBio'] as String? ?? '',
        profileAnalysis: map['profileAnalysis'] as String? ?? '',
        lastActiveAt: map['lastActiveAt'] != null
            ? DateTime.fromMillisecondsSinceEpoch(map['lastActiveAt'] as int)
            : null,
        createdAt: map['createdAt'] != null
            ? DateTime.fromMillisecondsSinceEpoch(map['createdAt'] as int)
            : null,
      );
}
