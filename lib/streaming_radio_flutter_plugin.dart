import 'dart:async';

import 'package:flutter/services.dart';

class Streaming {
  static const MethodChannel _channel = const MethodChannel('streaming_channel');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> get getCurrentSong async {
    final String title = await _channel.invokeMethod('getCurrentSong');
    return title;
  }
}
