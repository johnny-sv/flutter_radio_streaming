import 'dart:async';

import 'package:flutter/services.dart';
import 'package:streaming_radio_flutter_plugin/streaming_radio_flutter_plugin.dart';

class StreamingController {
  MethodChannel _channel;
  var streamingController = StreamController<String>();

  StreamingController() {
    _channel = const MethodChannel('streaming_channel');
    _channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'playing_event':
          streamingController.sink.add(call.method);
          Future<void>.delayed(const Duration(milliseconds: 500), () async {
            await Streaming.getCurrentSong;
          });
          return 'Playing, ${call.arguments}';
        case 'paused_event':
          streamingController.sink.add(call.method);
          return 'Paussed, ${call.arguments}';
        case 'stopped_event':
          streamingController.sink.add(call.method);
          return 'Stopped, ${call.arguments}';
        case 'loading_event':
          streamingController.sink.add(call.method);
          return 'Loading, ${call.arguments}';
        case 'song_title_update_event':
          streamingController.sink.add('Playing: ${call.arguments[0]}');
          return 'Playing: ${call.arguments}';
        case 'song_title_update_event':
          final String song = call.arguments[0].toString();
          streamingController.sink.add(song.isNotEmpty ? song : "");
          return 'Now playing, ${call.arguments}';
        default:
          throw MissingPluginException();
      }
    });
  }

  Future<void> config({
    String url,
  }) async {
    try {
      String result = await _channel.invokeMethod('config', <String, dynamic>{
        'url': url,
        'notification_title': "Test ",
        'notification_description': "Description",
        'notification_color': "#FF0000",
        'notification_stop_button_text': "Stop",
        'notification_pause_button_text': "Pause",
        'notification_play_button_text': "Play",
        'notification_playing_description_text': "Playing",
        'notification_stopped_description_text': "Stopped"
      });
      return result;
    } catch (err) {
      throw Exception(err);
    }
  }

  Future<void> play() async {
    try {
      String result = await _channel.invokeMethod('play', <String, dynamic>{});
      return result;
    } catch (err) {
      throw Exception(err);
    }
  }

  Future<void> pause() async {
    try {
      String result = await _channel.invokeMethod('pause', <String, dynamic>{});
      return result;
    } catch (err) {
      throw Exception(err);
    }
  }

  Future<void> stop() async {
    try {
      String result = await _channel.invokeMethod('stop', <String, dynamic>{});
      return result;
    } catch (err) {
      throw Exception(err);
    }
  }

  void dispose() {
    streamingController.close();
  }
}
