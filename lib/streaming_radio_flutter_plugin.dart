import 'dart:async';

import 'package:flutter/services.dart';

class Streaming {
  static const MethodChannel _channel = const MethodChannel('streaming');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
