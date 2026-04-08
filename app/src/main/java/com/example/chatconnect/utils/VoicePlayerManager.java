package com.example.chatconnect.utils;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VoicePlayerManager {

    private static final String TAG = "VoicePlayerManager";
    private static VoicePlayerManager instance;

    private MediaPlayer mediaPlayer;
    private String currentPlayingId;
    private PlaybackListener currentListener;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Map<String, String> cachedFiles = new HashMap<>();

    public static synchronized VoicePlayerManager getInstance() {
        if (instance == null) {
            instance = new VoicePlayerManager();
        }
        return instance;
    }

    public interface PlaybackListener {
        void onProgress(int currentMs, int totalMs);
        void onComplete();
        void onError(String error);
    }

    public void play(String messageId, String audioUrl, File cacheDir, PlaybackListener listener) {
        // If same message is playing, toggle pause/resume
        if (messageId.equals(currentPlayingId) && mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                stopProgressUpdates();
                return;
            } else {
                mediaPlayer.start();
                startProgressUpdates();
                return;
            }
        }

        // Stop any current playback
        stop();

        currentPlayingId = messageId;
        currentListener = listener;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        try {
            // Check cache first
            String cachedPath = cachedFiles.get(messageId);
            if (cachedPath != null && new File(cachedPath).exists()) {
                mediaPlayer.setDataSource(cachedPath);
            } else {
                mediaPlayer.setDataSource(audioUrl);
            }

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                startProgressUpdates();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                stopProgressUpdates();
                if (currentListener != null) {
                    currentListener.onComplete();
                }
                currentPlayingId = null;
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Playback error: " + what);
                if (currentListener != null) {
                    currentListener.onError("Playback error");
                }
                stop();
                return true;
            });

        } catch (IOException e) {
            Log.e(TAG, "Error setting data source", e);
            if (listener != null) listener.onError("Cannot play audio");
            stop();
        }
    }

    public void stop() {
        stopProgressUpdates();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        currentPlayingId = null;
        currentListener = null;
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public boolean isPlaying(String messageId) {
        return messageId.equals(currentPlayingId) && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public String getCurrentPlayingId() {
        return currentPlayingId;
    }

    public void cacheFile(String messageId, String localPath) {
        cachedFiles.put(messageId, localPath);
    }

    private void startProgressUpdates() {
        handler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        handler.removeCallbacks(progressRunnable);
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && currentListener != null) {
                currentListener.onProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
            }
            handler.postDelayed(this, 100);
        }
    };
}