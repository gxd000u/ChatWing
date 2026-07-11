# ChatWing ProGuard 规则
# Keep Room entities
-keep class com.chatwing.db.entity.** { *; }
# Keep JSON serialization
-keep class org.json.** { *; }
# Keep method channel
-keep class com.chatwing.platform.** { *; }
# Keep Flutter embedding
-keep class io.flutter.** { *; }
dontwarn io.flutter.**
dontwarn com.chatwing.**
