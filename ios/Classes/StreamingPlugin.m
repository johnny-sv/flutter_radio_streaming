#import "StreamingPlugin.h"
#import <streaming_radio_flutter_plugin/streaming_radio_flutter_plugin-Swift.h>

@implementation StreamingPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftStreamingPlugin registerWithRegistrar:registrar];
}
@end
