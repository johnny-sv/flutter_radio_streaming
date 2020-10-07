# Flutter Streaming Radio Plugin

This is a simple radio streaming plugin, with foreground notification control for Android and iOS. The plugin has several customization options and allows audio streaming using ExoPlayer for Android and AVPlayer for iOS.
The code is fully developed in Swift and Kotlin. You can customize the url and the notification strings for the controls and the feedback.
Any reported problems will be welcome.
Thank you in advance and I hope you enjoy it.

## Setup instructions

### Android
If you use a http protocol you need to add a NetworkConfig to allow http to your domain into the  Manifest file.   

Support Android SDK min version : 16  

### iOS

Like Android if you use http connection, you must change the info.plist of your project to allow it. You need to change the App Transport Security Settings.
For a better functionality in background i recommend to add the Audio Background mode capability in the target of your project.

## Feature included

The plugin can reproduce audio for a streaming source using ExoPlayer for Android and AvPlayer for iOS. 
You can control the play stop and pause action with the plugin methods or from the foreground notification actions.
The plugin have methods to get feedback of the of the player status in the native side to use that in flutter side.
You can customize the foreground notification controls messages and button texts with the config method. 

## How use it in Flutter

```dart
Class StreamingController{

  MethodChannel _channel;
  var  streamingController =  StreamController<String>();

  StreamingController() {
    _channel = const MethodChannel('streaming_channel');
    _channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'playing_event':
          streamingController.sink.add(call.method);
          return 'Playing, ${call.arguments}';
        case 'paused_event':
          streamingController.sink.add(call.method);
          return 'Paused, ${call.arguments}';
        case 'stopped_event':
          streamingController.sink.add(call.method);
          return 'Stopped, ${call.arguments}';
        case 'loading_event':
          streamingController.sink.add(call.method);
          return 'Loading, ${call.arguments}';
        default:
          throw MissingPluginException();
      }
    });
  }

  Future<void> config({String url,}) async {
    try {
      String result =
      await _channel.invokeMethod('config', <String, dynamic>{
        'url': url,
        'notification_title':"Test " ,
        'notification_description': "Description",
        'notification_color':"#FF0000",
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
      String result =
      await _channel.invokeMethod('play',<String, dynamic>{});
      return result;
    } catch (err) {
      throw Exception(err);
    }
  }

  Future<void> pause() async {
    try {
      String result =
      await _channel.invokeMethod('pause', <String, dynamic>{});
      return result;
    } catch (err) {
      throw Exception(err);
    }
  }

  Future<void> stop() async {
    try {
      String result =
      await _channel.invokeMethod('stop', <String, dynamic>{});
      return result;
    } catch (err) {
      throw Exception(err);
    }
  }

  void dispose(){
    streamingController.close();
  }

 }

```






