import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

/// 大模型 API 封装
/// 默认使用 DeepSeek，支持切换至通义千问、智谱 GLM
class LLMService {
  static const _storage = FlutterSecureStorage();
  static const _keyApiKey = 'chatwing_api_key';
  static const _keyProvider = 'chatwing_provider';

  static String _apiKey = '';
  static String _provider = 'deepseek';

  // 供应商配置
  static const _providers = {
    'deepseek': {
      'baseUrl': 'https://api.deepseek.com/v1/chat/completions',
      'model': 'deepseek-chat',
    },
    'zhipu': {
      'baseUrl': 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
      'model': 'glm-4-flash',
    },
    'qwen': {
      'baseUrl':
          'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
      'model': 'qwen-turbo',
    },
  };

  /// 初始化：从安全存储加载 API Key
  static Future<void> init() async {
    _apiKey = await _storage.read(key: _keyApiKey) ?? '';
    _provider = await _storage.read(key: _keyProvider) ?? 'deepseek';
  }

  /// 设置 API Key（加密存储）
  static Future<void> setApiKey(String key) async {
    _apiKey = key;
    await _storage.write(key: _keyApiKey, value: key);
  }

  /// 获取 API Key
  static Future<String> getApiKey() async {
    if (_apiKey.isEmpty) _apiKey = await _storage.read(key: _keyApiKey) ?? '';
    return _apiKey;
  }

  /// 切换供应商
  static Future<void> switchProvider(String provider) async {
    if (_providers.containsKey(provider)) {
      _provider = provider;
      await _storage.write(key: _keyProvider, value: provider);
    }
  }

  /// 获取当前供应商名称
  static String get currentProvider => _provider;

  /// 发送聊天请求
  static Future<String> chat({
    required String systemPrompt,
    required String userPrompt,
    double temperature = 0.8,
    int maxTokens = 1024,
  }) async {
    if (_apiKey.isEmpty) throw Exception('请先设置 API Key');

    final config = _providers[_provider]!;
    final headers = <String, String>{
      'Content-Type': 'application/json',
      if (_provider == 'deepseek' || _provider == 'zhipu')
        'Authorization': 'Bearer $_apiKey',
    };

    Map<String, dynamic> body;
    if (_provider == 'qwen') {
      body = {
        'model': config['model'],
        'input': {
          'messages': [
            {'role': 'system', 'content': systemPrompt},
            {'role': 'user', 'content': userPrompt},
          ]
        },
        'parameters': {
          'temperature': temperature,
          'max_tokens': maxTokens,
          'result_format': 'message',
        },
      };
    } else {
      body = {
        'model': config['model'],
        'messages': [
          {'role': 'system', 'content': systemPrompt},
          {'role': 'user', 'content': userPrompt},
        ],
        'temperature': temperature,
        'max_tokens': maxTokens,
      };
    }

    final response = await http.post(
      Uri.parse(config['baseUrl']!),
      headers: headers,
      body: jsonEncode(body),
    );

    if (response.statusCode != 200) {
      throw Exception('API 请求失败: ${response.statusCode} ${response.body}');
    }

    final json = jsonDecode(response.body);

    // 解析不同供应商的返回格式
    if (_provider == 'qwen') {
      return json['output']['choices']['message']['content']?.toString() ?? '';
    } else {
      return json['choices'][0]['message']['content']?.toString() ?? '';
    }
  }
}
