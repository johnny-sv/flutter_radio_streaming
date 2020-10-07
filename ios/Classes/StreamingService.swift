//
//  StreamingServi.swift
//  streaming
//
//  Created by Cristian Torrado on 25/04/2020.
//
import Foundation
import AVKit
import AVFoundation
import UserNotifications


public protocol StreamingDelegate {
    func onChangeStatus( status: StreamingStatus)
    func onUpdateSongTitle( title: String )
}

public final class StreamingManager: NSObject, UNUserNotificationCenterDelegate, AVPlayerItemMetadataOutputPushDelegate {
    
    static let sharedIntance = StreamingManager()
    private static let PLAY_NOTIF_ID = "playButton"
    private static let STOP_NOTIF_ID = "stopButton"
    private static let PAUSE_NOTIF_ID = "pauseButton"
    private static let NOTIF_ID = "streaming.notification"
    private static let NOTIF_CATEGORY_ID = "streaming.control"
    
    override private init() {
        
        let session = AVAudioSession.sharedInstance()
        do {
            if #available(iOS 10.0, *) {
                try session.setCategory(AVAudioSession.Category.playback, mode: AVAudioSession.Mode.default, options: [.interruptSpokenAudioAndMixWithOthers])
            }
        } catch let error as NSError {
            print("Failed to set the audio session category and mode: \(error.localizedDescription)")
        }

    }
    
    private var isInitialized = false
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
    private var timeObserverToken: Any?
    private var isPLaying = false
    private var status = StreamingStatus.Stopped
    private var statusChangeFlag = false
    private var currentSong = ""

    public var streamingDelegate: StreamingDelegate?
    
    var player:AVPlayer?
    var playerItem:AVPlayerItem?
 
    public func config(
        url:String,
        title: String,
        description: String,
        playingDescription: String,
        stoppedDescription: String,
        playButtonText: String,
        stopButtonText: String,
        pauseButtonText: String
    ) {
        if(!url.isEmpty) { self.url = url }
        if(!title.isEmpty) { self.title = title }
        if(!description.isEmpty) { self.notifDescription = description }
        if(!playingDescription.isEmpty) { self.playingDescription = playingDescription }
        if(!stoppedDescription.isEmpty) { self.stoppedDescription = stoppedDescription }
        if(!playButtonText.isEmpty){ self.playText = playButtonText }
        if(!stopButtonText.isEmpty){ self.stopText = stopButtonText }
        if(!pauseButtonText.isEmpty){ self.pauseText = pauseButtonText }

        let metadataOutput = AVPlayerItemMetadataOutput(identifiers: nil)
        let u = URL(string: url)
        playerItem = AVPlayerItem(url: u!)
        metadataOutput.setDelegate(self, queue: DispatchQueue.main)
        playerItem?.add(metadataOutput)

        player = AVPlayer(playerItem: playerItem)
        
        let interval = CMTime(seconds: 0.5,
                             preferredTimescale: CMTimeScale(NSEC_PER_SEC))
       timeObserverToken =
       self.player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) {
               [weak self] time in
        
            if self?.player?.rate != 0.0 && self?.status == StreamingStatus.Playing
                && self?.statusChangeFlag == true {
                
                self?.streamingDelegate?.onChangeStatus(status: self!.status)
                self?.statusChangeFlag = false
                
            } else if self?.player?.rate == 0.0 && self?.status == StreamingStatus.Playing
                && self?.statusChangeFlag == false {
                self?.streamingDelegate?.onChangeStatus(status: StreamingStatus.Loading)
                
            } else if self?.player?.rate == 0.0 && self?.status == StreamingStatus.Paused
                && self?.statusChangeFlag == true {
                self?.streamingDelegate?.onChangeStatus(status: self!.status)
                self?.statusChangeFlag = false
            } else if self?.player?.rate == 0.0 && self?.status == StreamingStatus.Stopped
                && self?.statusChangeFlag == true {
                self?.streamingDelegate?.onChangeStatus(status: self!.status)
                self?.statusChangeFlag = false
            }
        }
    }

    
    public func play() {
        if status == StreamingStatus.Stopped || status == StreamingStatus.Paused {
            player?.play()
            status = StreamingStatus.Playing
            statusChangeFlag = true
        }
    }

    public func stop() {
        if status == StreamingStatus.Playing{
            player?.pause()
            status = StreamingStatus.Stopped
            statusChangeFlag = true
        }
    }

    public func pause() {
        if status == StreamingStatus.Playing{
            player?.pause()
            status = StreamingStatus.Paused
            statusChangeFlag = true
        }
    }

    public func getCurrentSong() -> String {
        return currentSong
    }
    
    public func metadataOutput(_ output: AVPlayerItemMetadataOutput, didOutputTimedMetadataGroups groups: [AVTimedMetadataGroup], from track: AVPlayerItemTrack?) {
        if let item = groups.first?.items.first {
            item.value(forKeyPath: "value")
            currentSong = (item.value(forKeyPath: "value")!) as! String
            print("Now Playing: \n \(String(describing: currentSong))")
            self.streamingDelegate?.onUpdateSongTitle(title: currentSong)
        } else {
            print("No Metadata or Could not read")
            currentSong = ""
            self.streamingDelegate?.onUpdateSongTitle(title: "")
        }
    }
  
    
    private func removeNotificationTrigger() {
        if #available(iOS 10.0, *) {
            let center = UNUserNotificationCenter.current()
            center.removePendingNotificationRequests(withIdentifiers: [StreamingManager.NOTIF_ID])
            center.removeDeliveredNotifications(withIdentifiers: [StreamingManager.NOTIF_ID])
        }

    }
    
    private func showNotification(
        title: String,
        description: String,
        playingDescription: String,
        stoppedDescription: String,
        playButtonText: String,
        stopButtonText: String,
        pauseButtonText: String,
        packageIntentName: String
    ) {

        let application = UIApplication.shared
        
        if #available(iOS 10.0, *) {
            let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
            UNUserNotificationCenter.current().requestAuthorization(options: authOptions,
                                                                    completionHandler: {_, _ in })
           

            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 60,
                                                            repeats: true)
            
            let notificationContent = UNMutableNotificationContent()
            notificationContent.categoryIdentifier = StreamingManager.NOTIF_CATEGORY_ID
            
            var statusDescription = ""
            if(player != nil && player?.rate != 0){
                statusDescription = playingDescription
            }else{
                statusDescription = stoppedDescription
            }
            
            let finalDescriptionText = description + " - " + statusDescription
            notificationContent.title = title
            notificationContent.body = finalDescriptionText

            let request = UNNotificationRequest(identifier: StreamingManager.NOTIF_ID,
                             content: notificationContent,
                             trigger: trigger)
            

            UNUserNotificationCenter.current().add(request)
            UNUserNotificationCenter.current().delegate = self
            
        }
        application.registerForRemoteNotifications()
    }
    
    @available(iOS 10.0, *)
    public func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        switch response.actionIdentifier {
            case StreamingManager.PLAY_NOTIF_ID:
                self.play()
                break
            case StreamingManager.STOP_NOTIF_ID:
                self.stop()
                break
            case StreamingManager.PAUSE_NOTIF_ID:
                self.pause()
                break
            default:
                break
        }
        
    }
    
}


