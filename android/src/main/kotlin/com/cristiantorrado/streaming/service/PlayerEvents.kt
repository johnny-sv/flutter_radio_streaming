package com.cristiantorrado.streaming.service

open class PlayerEvent {
    open var value: String = ""
}

class PlayingEvent: PlayerEvent()
class StoppedEvent: PlayerEvent()
class LoadingEvent: PlayerEvent()
class PausedEvent: PlayerEvent()
class ErrorEvent: PlayerEvent()
class SongTitleUpdateEvent: PlayerEvent()
