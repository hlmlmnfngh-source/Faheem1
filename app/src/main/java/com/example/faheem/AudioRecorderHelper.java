package com.example.faheem;

import android.media.MediaRecorder;
import android.os.CountDownTimer;
import java.io.IOException;

public class AudioRecorderHelper {

    private static final int MAX_DURATION_MS = 5 * 60 * 1000; // 5 دقايق كحد أقصى

    private MediaRecorder recorder;
    private String currentFilePath;
    private CountDownTimer autoStopTimer;

    public interface RecordingCallback {
        void onRecordingStarted();
        void onRecordingStopped(String filePath);
        void onRecordingError(String message);
        void onTick(int secondsRemaining); // لتحديث عداد الوقت بالشاشة
    }

    public void startRecording(String outputFilePath, RecordingCallback callback) {
        currentFilePath = outputFilePath;
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(outputFilePath);

        try {
            recorder.prepare();
            recorder.start();
            callback.onRecordingStarted();

            // عداد تنازلي يوقف التسجيل تلقائياً بعد 5 دقايق
            autoStopTimer = new CountDownTimer(MAX_DURATION_MS, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    callback.onTick((int) (millisUntilFinished / 1000));
                }

                @Override
                public void onFinish() {
                    stopRecording(callback);
                }
            };
            autoStopTimer.start();

        } catch (IOException e) {
            callback.onRecordingError("ERR_PREPARE:" + e.getMessage());
        }
    }

    public void stopRecording(RecordingCallback callback) {
        if (autoStopTimer != null) {
            autoStopTimer.cancel();
        }
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (RuntimeException e) {
                // بيصير لو المستخدم وقف التسجيل بسرعة جداً (أقل من ثانية)
                callback.onRecordingError("ERR_TOO_SHORT");
                recorder.release();
                recorder = null;
                return;
            }
            recorder = null;
            callback.onRecordingStopped(currentFilePath);
        }
    }
}