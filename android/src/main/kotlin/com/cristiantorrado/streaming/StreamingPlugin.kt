package com.cristiantorrado.streaming

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.NonNull
import com.cristiantorrado.streaming.service.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


/** StreamingPlugin */
public class StreamingPlugin : FlutterPlugin, MethodCallHandler {

    private  var channel : MethodChannel? = null

    private var mFlutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var url = ""
    private var title = ""
    private var description = ""
    private var color = ""
    private var stopText = ""
    private var pauseText = ""
    private var playText = ""
    private var playingText = ""
    private var stopperText = ""


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mFlutterPluginBinding = flutterPluginBinding
        EventBus.getDefault().register(this)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, METHOD_CHANNEL)
        channel?.setMethodCallHandler(this)
    }

    private fun myAppContext(): Context {
        return mFlutterPluginBinding?.applicationContext
            ?: throw Exception("ForegroundServicePlugin application context was null")
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        val args = call.arguments as Map<*, *>

        when (call.method) {
            CONFIG_METHOD -> {
                try {
                    url = if (args[URL_PARAM] != null) args[URL_PARAM] as String else url
                    title = if (args[TITLE_PARAM] != null) args[TITLE_PARAM] as String else title
                    description = if (args[DESCRIPTION_PARAM] != null) args[DESCRIPTION_PARAM] as String else description
                    color = if (args[COLOR_PARAM] != null &&
                        validateHexadecimalColorPattern(args[COLOR_PARAM] as String)) args[COLOR_PARAM] as String else color
                    stopText = if (args[STOP_TEXT_PARAM] != null) args[STOP_TEXT_PARAM] as String else stopText
                    pauseText = if (args[PAUSE_TEXT_PARAM] != null) args[PAUSE_TEXT_PARAM] as String else pauseText
                    playText = if (args[PLAY_TEXT_PARAM] != null) args[PLAY_TEXT_PARAM] as String else playText
                    playingText = if (args[PLAYING_DESCRIPTION_TEXT] != null) args[PLAYING_DESCRIPTION_TEXT] as String else playingText
                    stopperText = if (args[STOPPED_DESCRIPTION_TEXT] != null) args[STOPPED_DESCRIPTION_TEXT] as String else playingText
                    result.success(RESULT_SUCCESS_MESSAGE)
                } catch (e: Exception) {
                    result.error(PARAM_ERROR_CODE, PARAM_PARSE_ERROR_MESSAGE, e)
                }

            }
            PLAY_METHOD -> {
                myAppContext().let { context ->
                    val startServiceIntent = Intent(context, StreamingService::class.java)
                    startServiceIntent.apply {
                        this.action = StreamingService.PLAY_ACTION
                        this.putExtra(StreamingService.URL_EXTRA, url)
                        this.putExtra(StreamingService.TITLE_EXTRA, title)
                        this.putExtra(StreamingService.DESCRIPTION_EXTRA, description)
                        this.putExtra(StreamingService.COLOR_EXTRA, color)
                        this.putExtra(StreamingService.STOP_TEXT_EXTRA, stopText)
                        this.putExtra(StreamingService.PAUSE_TEXT_EXTRA, pauseText)
                        this.putExtra(StreamingService.PLAY_TEXT_EXTRA, playText)
                        this.putExtra(StreamingService.PLAYING_DESCRIPTION_EXTRA, description)
                        this.putExtra(StreamingService.STOPPED_DESCRIPTION_EXTRA, stopperText)
                        this.putExtra(StreamingService.ANDROID_PACKAGE_TAP_INTENT_EXTRA, myAppContext().packageName)
                    }
                    context.startService(startServiceIntent)
                    result.success(RESULT_SUCCESS_MESSAGE)
                }
            }
            PAUSE_METHOD -> {
                myAppContext().let { context ->
                    val startServiceIntent = Intent(context, StreamingService::class.java)
                    startServiceIntent.let {
                        it.action = StreamingService.PAUSE_ACTION
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(startServiceIntent)
                        }else{
                            context.startService(startServiceIntent)
                        }
                    }
                }
                result.success(RESULT_SUCCESS_MESSAGE)
            }
            STOP_METHOD -> {
                myAppContext().let { context ->
                    val startServiceIntent = Intent(context, StreamingService::class.java)
                    startServiceIntent.let {
                        it.action = StreamingService.STOP_ACTION
                        context.startService(startServiceIntent)
                    }
                }
                result.success(RESULT_SUCCESS_MESSAGE)
            }
            GET_CURRENT_SONG_METHOD -> {
                myAppContext().let { context ->
                    val startServiceIntent = Intent(context, StreamingService::class.java)
                    startServiceIntent.apply {
                        this.action = StreamingService.GET_CURRENT_SONG_ACTION
                    }
                    context.startService(startServiceIntent)
                    val currentSong = startServiceIntent.getStringExtra(StreamingService.CURRENT_SONG_TITLE_EXTRA)
                    result.success(currentSong)
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    @Subscribe
    fun onPlayerNewEvent(event: PlayerEvent) {
        when (event) {
            is PlayingEvent -> channel?.invokeMethod(PLAYING_EVENT_METHOD, null)
            is StoppedEvent -> channel?.invokeMethod(STOPPED_EVENT_METHOD, null)
            is PausedEvent -> channel?.invokeMethod(PAUSED_EVENT_METHOD, null)
            is LoadingEvent -> channel?.invokeMethod(LOADING_EVENT_METHOD, null)
            is SongTitleUpdateEvent -> channel?.invokeMethod(SONG_TITLE_UPDATE_METHOD, listOf(event.value))
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        EventBus.getDefault().unregister(this)
    }

    companion object {


        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            val channel = MethodChannel(registrar.messenger(), METHOD_CHANNEL)
            channel.setMethodCallHandler(StreamingPlugin())
        }

        const val PARAM_ERROR_CODE = "Wrong Param"
        const val PARAM_PARSE_ERROR_MESSAGE = "Incorrect param, please check the values and the name of that"
        const val RESULT_SUCCESS_MESSAGE = "Success"
        const val METHOD_CHANNEL = "streaming_channel"

        const val URL_PARAM = "url"
        const val TITLE_PARAM = "notification_title"
        const val DESCRIPTION_PARAM = "notification_description"
        const val COLOR_PARAM = "notification_color"
        const val STOP_TEXT_PARAM = "notification_stop_button_text"
        const val PAUSE_TEXT_PARAM = "notification_pause_button_text"
        const val PLAY_TEXT_PARAM = "notification_play_button_text"
        const val PLAYING_DESCRIPTION_TEXT = "notification_playing_description_text"
        const val STOPPED_DESCRIPTION_TEXT = "notification_stopped_description_text"

        const val CONFIG_METHOD = "config"
        const val PLAY_METHOD = "play"
        const val PAUSE_METHOD = "pause"
        const val STOP_METHOD = "stop"
        const val GET_CURRENT_SONG_METHOD = "getCurrentCong"

        const val PLAYING_EVENT_METHOD = "playing_event"
        const val STOPPED_EVENT_METHOD = "stopped_event"
        const val PAUSED_EVENT_METHOD = "paused_event"
        const val LOADING_EVENT_METHOD = "loading_event"
        const val SONG_TITLE_UPDATE_METHOD = "song_title_update_event"


    }
}


