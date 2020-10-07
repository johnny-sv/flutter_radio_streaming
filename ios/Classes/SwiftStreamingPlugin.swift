import Flutter
import UIKit

public class SwiftStreamingPlugin: NSObject, FlutterPlugin,StatusChangeDelegate {
    
    public override init() {
        super .init()
        streamingController.statusChangeDelegate = self
    }
    
    public func onChangeStatus(status: StreamingStatus) {
        switch status {
        case .Playing:
            SwiftStreamingPlugin.channel?.invokeMethod(SwiftStreamingPlugin.PLAYING_EVENT_METHOD, arguments: nil)
            break
        case .Stopped:
            SwiftStreamingPlugin.channel?.invokeMethod(SwiftStreamingPlugin.STOPPED_EVENT_METHOD, arguments: nil)
            break
        case .Paused:
            SwiftStreamingPlugin.channel?.invokeMethod(SwiftStreamingPlugin.PAUSED_EVENT_METHOD, arguments: nil)
            break
        case .Loading:
            SwiftStreamingPlugin.channel?.invokeMethod(SwiftStreamingPlugin.LOADING_EVENT_METHOD, arguments: nil)
        default:
            break
        }
    }
    
    private static var channel:FlutterMethodChannel?
 
    private var streamingController = StreamingManager.sharedIntance
    
    private static let PARAM_ERROR_CODE = "Wrong Param"
    private static let PARAM_PARSE_ERROR_MESSAGE = "Incorrect param, please check the values and the name of that"
    private static let RESULT_SUCCESS_MESSAGE = "Success"
    private static let METHOD_CHANNEL = "streaming_channel"
    
    private static let URL_PARAM = "url"
    private static let TITLE_PARAM = "notification_title"
    private static let DESCRIPTION_PARAM = "notification_description"
    private static let COLOR_PARAM = "notification_color"
    private static let STOP_TEXT_PARAM = "notification_stop_button_text"
    private static let PAUSE_TEXT_PARAM = "notification_pause_button_text"
    private static let PLAY_TEXT_PARAM = "notification_play_button_text"
    private static let PLAYING_DESCRIPTION_TEXT = "notification_playing_description_text"
    private static let STOPPED_DESCRIPTION_TEXT = "notification_stopped_description_text"
    
    private static let CONFIG_METHOD = "config"
    private static let PLAY_METHOD = "play"
    private static let PAUSE_METHOD = "pause"
    private static let STOP_METHOD = "stop"
    
    private static let PLAYING_EVENT_METHOD = "playing_event"
    private static let STOPPED_EVENT_METHOD = "stopped_event"
    private static let PAUSED_EVENT_METHOD = "paused_event"
    private static let LOADING_EVENT_METHOD = "loading_event"
    
    private var url = ""
    private var title = ""
    private var notifDescription = ""
    private var playingDescription = ""
    private var stoppedDescription = ""
    
    private var stopText = ""
    private var playText = ""
    private var pauseText = ""
    private var playingText = ""
    private var stoppedText = ""
    private var packageIntentName = ""
  
    

static public func register(with registrar: FlutterPluginRegistrar) {
    UIApplication.shared.applicationIconBadgeNumber = 0
    let channel = FlutterMethodChannel(name: "streaming_channel", binaryMessenger: registrar.messenger())
    self.channel = channel
    let instance = SwiftStreamingPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }
    
  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    var args = call.arguments as! NSDictionary

    switch(call.method){
    case SwiftStreamingPlugin.CONFIG_METHOD:
       
        if (args[SwiftStreamingPlugin.URL_PARAM] != nil ) {
            self.url = args[SwiftStreamingPlugin.URL_PARAM] as! String
        } else { self.url = "" }
        if (args[SwiftStreamingPlugin.TITLE_PARAM] != nil ) {
            self.title = args[SwiftStreamingPlugin.TITLE_PARAM] as! String
        } else { self.title = "" }
        if (args[SwiftStreamingPlugin.DESCRIPTION_PARAM] != nil ) {
            self.notifDescription = args[SwiftStreamingPlugin.DESCRIPTION_PARAM] as! String
        } else { self.notifDescription = "" }
        
        if (args[SwiftStreamingPlugin.PLAYING_DESCRIPTION_TEXT] != nil ) {
            self.playingDescription = args[SwiftStreamingPlugin.PLAYING_DESCRIPTION_TEXT] as! String
        } else { self.playingDescription = "" }
        
        if (args[SwiftStreamingPlugin.STOPPED_DESCRIPTION_TEXT] != nil ) {
            self.stoppedDescription = args[SwiftStreamingPlugin.STOPPED_DESCRIPTION_TEXT] as! String
        } else { self.stoppedDescription = "" }
        
        if (args[SwiftStreamingPlugin.PLAY_TEXT_PARAM] != nil ) {
            self.playText = args[SwiftStreamingPlugin.PLAY_TEXT_PARAM] as! String
        } else { self.playText = "" }
        
        if (args[SwiftStreamingPlugin.PAUSE_TEXT_PARAM] != nil ) {
            self.pauseText = args[SwiftStreamingPlugin.PAUSE_TEXT_PARAM] as! String
        } else { self.pauseText = "" }
        
        if (args[SwiftStreamingPlugin.STOP_TEXT_PARAM] != nil ) {
            self.stopText = args[SwiftStreamingPlugin.STOP_TEXT_PARAM] as! String
        } else { self.stopText = "" }
        
        streamingController.config(url:self.url,title: self.title,description: self.notifDescription,playingDescription: self.playingDescription,stoppedDescription: self.stoppedDescription,playButtonText: self.playText,stopButtonText: stopText,pauseButtonText: pauseText)
            break
        
        case SwiftStreamingPlugin.PLAY_METHOD:
            streamingController.play()
            break
        case SwiftStreamingPlugin.PAUSE_METHOD:
            streamingController.pause()
            break
        case SwiftStreamingPlugin.STOP_METHOD:
            streamingController.stop()
            break
        default:
            break
    }
  }
}
