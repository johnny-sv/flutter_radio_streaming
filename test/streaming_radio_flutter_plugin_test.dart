import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:streaming_radio_flutter_plugin/streaming_radio_flutter_plugin.dart';

void main() {
  const MethodChannel channel = MethodChannel('streaming');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await Streaming.platformVersion, '42');
  });
}
