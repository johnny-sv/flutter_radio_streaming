package com.cristiantorrado.streaming.service

open class PlayerEvent;
class PlayingEvent: PlayerEvent()
class StoppedEvent: PlayerEvent()
class LoadingEvent: PlayerEvent()
class PausedEvent: PlayerEvent()
class ErrorEvent: PlayerEvent()
