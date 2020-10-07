package com.cristiantorrado.streaming.service

enum class PlayerStatus {
    LOADING, STOPPED, IDLE,PLAYING,PAUSED
}
enum class PlayerTypeError {
    TYPE_SOURCE, TYPE_RENDERER, TYPE_UNEXPECTED, TYPE_REMOTE, TYPE_OUT_OF_MEMORY
}

data class GeneralPlayerStatus(var playerStatus: PlayerStatus,
                               var isPlaying:Boolean,
                               var isLoading:Boolean)

data class GeneralPlayerError(var playerErrorType: PlayerTypeError)