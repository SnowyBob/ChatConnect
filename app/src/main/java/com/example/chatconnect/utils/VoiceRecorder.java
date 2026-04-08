package com.example.chatconnect.utils;

import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class VoiceRecorder {

    private static final String TAG = "VoiceRecorder";
    private MediaRecorder recorder;
    private String filePath;
    private long startTime;
    private boolean isRecording = false;

    public void startRecording(File outputFile) throws IOException {
        filePath = outputFile.getAbsolutePath();
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(filePath);

        try {
            recorder.prepare();
            recorder.start();
            startTime = System.currentTimeMillis();
            isRecording = true;
        } catch (IOException e) {
            Log.e(TAG, "Recording failed to start", e);
            releaseRecorder();
            throw e;
        }
    }

    public RecordingResult stopRecording() {
        if (!isRecording) return null;
        long duration = System.currentTimeMillis() - startTime;
        try {
            recorder.stop();
        } catch (RuntimeException e) {
            Log.e(TAG, "Stop failed", e);
            // File may be invalid if recording was too short
            new File(filePath).delete();
            releaseRecorder();
            return null;
        }
        releaseRecorder();
        isRecording = false;
        return new RecordingResult(filePath, duration);
    }

    public void cancelRecording() {
        if (!isRecording) return;
        try {
            recorder.stop();
        } catch (RuntimeException ignored) {}
        releaseRecorder();
        isRecording = false;
        if (filePath != null) {
            new File(filePath).delete();
        }
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.release();
            } catch (Exception ignored) {}
            recorder = null;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public int getAmplitude() {
        if (recorder != null && isRecording) {
            try {
                return recorder.getMaxAmplitude();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public static class RecordingResult {
        public final String filePath;
        public final long durationMs;

        public RecordingResult(String filePath, long durationMs) {
            this.filePath = filePath;
            this.durationMs = durationMs;
        }
    }
}