package io.github.mkmanishkandel.paisaaayo.service

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import io.github.mkmanishkandel.paisaaayo.parser.Direction
import io.github.mkmanishkandel.paisaaayo.parser.Speech
import io.github.mkmanishkandel.paisaaayo.parser.SpeechLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Speaks payments. Announcements are serialised (one at a time, in order).
 *
 * Nepali uses the recorded voice pack in `assets/voice/ne/` when every needed clip is present,
 * otherwise the phone's text-to-speech engine. English always uses text-to-speech.
 */
class Announcer(context: Context) {

    private val app = context.applicationContext
    private val mutex = Mutex()
    private val audioManager = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var tts: TextToSpeech? = null
    private var ttsReady: CompletableDeferred<Boolean>? = null
    private val pendingUtterances = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /** clip name → asset path, e.g. "ne_5" → "voice/ne/ne_5.ogg". */
    private val clipFiles: Map<String, String> by lazy {
        runCatching { app.assets.list(VOICE_DIR).orEmpty() }
            .getOrDefault(emptyArray())
            .associate { file -> file.substringBeforeLast('.') to "$VOICE_DIR/$file" }
    }

    val hasVoicePack: Boolean get() = Speech.ALL_CLIPS.keys.all { it in clipFiles }

    suspend fun announce(
        amountPaisa: Long,
        direction: Direction,
        source: String?,
        language: SpeechLanguage,
        useRecordedVoice: Boolean,
        raiseVolume: Boolean,
    ) = mutex.withLock {
        withTimeoutOrNull(ANNOUNCE_TIMEOUT_MS) {
            withAudioFocus(raiseVolume) {
                val clips = Speech.nepaliClips(amountPaisa, direction)
                val playedClips = language == SpeechLanguage.NEPALI && useRecordedVoice &&
                    clips.all { it in clipFiles } && playClips(clips.map { clipFiles.getValue(it) })
                if (!playedClips) speak(amountPaisa, direction, source, language)
            }
        } ?: Log.w(TAG, "Announcement timed out")
    }

    // ---------------- audio focus & volume ----------------

    private suspend fun <T> withAudioFocus(raiseVolume: Boolean, block: suspend () -> T): T {
        val am = audioManager ?: return block()
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
        am.requestAudioFocus(focus)
        val restoreTo = if (raiseVolume) raiseMediaVolume(am) else null
        try {
            return block()
        } finally {
            restoreTo?.let { runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) } }
            am.abandonAudioFocusRequest(focus)
        }
    }

    /** Raises media volume to 80% if it is lower; returns the previous level to restore. */
    private fun raiseMediaVolume(am: AudioManager): Int? = runCatching {
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = (am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.8f).roundToInt()
        if (current < target) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            current
        } else {
            null
        }
    }.getOrNull() // Do Not Disturb can forbid volume changes.

    // ---------------- recorded clips ----------------

    /** Plays clips back-to-back without gaps. Returns false if anything failed (caller falls back to TTS). */
    private suspend fun playClips(paths: List<String>): Boolean = withContext(Dispatchers.Main) {
        val players = mutableListOf<MediaPlayer>()
        try {
            for (path in paths) {
                val player = MediaPlayer()
                players += player
                app.assets.openFd(path).use { fd: AssetFileDescriptor ->
                    player.setAudioAttributes(attributes)
                    player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
                player.prepare()
            }
            players.zipWithNext { a, b -> a.setNextMediaPlayer(b) }
            suspendCancellableCoroutine { cont ->
                players.last().setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                players.forEach { p ->
                    p.setOnErrorListener { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                }
                players.first().start()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Voice pack playback failed", e)
            false
        } finally {
            players.forEach { runCatching { it.release() } }
        }
    }

    // ---------------- text-to-speech ----------------

    private suspend fun speak(amountPaisa: Long, direction: Direction, source: String?, language: SpeechLanguage) {
        val engine = ensureTts() ?: return
        var spokenLanguage = language
        if (!selectVoice(engine, language)) {
            // No Nepali (or Hindi) voice on this phone: English is better than silence.
            spokenLanguage = SpeechLanguage.ENGLISH
            selectVoice(engine, SpeechLanguage.ENGLISH)
        }
        val text = Speech.sentence(spokenLanguage, amountPaisa, direction, source)
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        pendingUtterances[id] = done
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f) }
        if (engine.speak(text, TextToSpeech.QUEUE_ADD, params, id) != TextToSpeech.SUCCESS) {
            pendingUtterances.remove(id)
            return
        }
        withTimeoutOrNull(UTTERANCE_TIMEOUT_MS) { done.await() }
        pendingUtterances.remove(id)
    }

    private fun selectVoice(engine: TextToSpeech, language: SpeechLanguage): Boolean {
        val candidates = when (language) {
            // Hindi voices read Devanagari Nepali well enough when no Nepali voice is installed.
            SpeechLanguage.NEPALI -> listOf(Locale("ne", "NP"), Locale("ne"), Locale("hi", "IN"))
            SpeechLanguage.ENGLISH -> listOf(Locale("en", "IN"), Locale.UK, Locale.US)
        }
        for (locale in candidates) {
            val availability = runCatching { engine.isLanguageAvailable(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (availability >= TextToSpeech.LANG_AVAILABLE && engine.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE) {
                return true
            }
        }
        return false
    }

    private suspend fun ensureTts(): TextToSpeech? {
        val existing = tts
        val ready = ttsReady
        if (existing != null && ready != null) return if (ready.await()) existing else null

        val deferred = CompletableDeferred<Boolean>()
        val engine = withContext(Dispatchers.Main) {
            TextToSpeech(app) { status -> deferred.complete(status == TextToSpeech.SUCCESS) }
        }
        engine.setAudioAttributes(attributes)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { pendingUtterances[it]?.complete(Unit) }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { pendingUtterances[it]?.complete(Unit) }
            }
        })
        tts = engine
        ttsReady = deferred
        val ok = withTimeoutOrNull(TTS_INIT_TIMEOUT_MS) { deferred.await() } ?: false
        if (!ok) {
            Log.w(TAG, "Text-to-speech engine unavailable")
            runCatching { engine.shutdown() }
            tts = null
            ttsReady = null
            return null
        }
        return engine
    }

    private companion object {
        const val TAG = "Announcer"
        const val VOICE_DIR = "voice/ne"
        const val ANNOUNCE_TIMEOUT_MS = 30_000L
        const val UTTERANCE_TIMEOUT_MS = 20_000L
        const val TTS_INIT_TIMEOUT_MS = 8_000L
    }
}
