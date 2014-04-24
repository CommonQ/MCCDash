package edu.bupt.mccdash.m3u8;

import java.util.ArrayList;

import edu.bupt.mccdash.HttpDownloader;
import edu.bupt.mccdash.OnBluetoothRecvCompleteListener;
import edu.bupt.mccdash.OnHttpDownloadCompleteListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FakeStreamPlayer {
    private static final String TAG = "FakeStreamPlayer";
    private M3u8Resolver m3u8Resolver;
    private Handler handler;
    private final int bufferLength = 3;
    private final int timeLengthForSegment = 10000;
    private int currentPlayingSegment = 0;
    private ArrayList<Boolean> currentBufferedSegment;

    public static final int MESSAGE_PLAYER_NEXT_SEGMENT = 1;

    public FakeStreamPlayer(Handler handler) {
        this.handler = handler;
        currentBufferedSegment = new ArrayList<Boolean>();
    }

    public void play(String m3u8String) {
        m3u8Resolver = new M3u8Resolver(m3u8String);
        // prepare
        for (int i = 0; i < bufferLength; i++) {
            Log.v(TAG, "downloading " + m3u8Resolver.getAt(i));
            // TODO
        }

        // start playing

    }

    private OnBluetoothRecvCompleteListener onBluetoothRecvCompleteListener = new OnBluetoothRecvCompleteListener() {
        @Override
        public void OnBluetoothRecvComplete(int segmentNumber) {
            // TODO
        }
    };

    private class PlayingRunnable implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    currentPlayingSegment++;
                    Thread.sleep(timeLengthForSegment);
                    Message message = new Message();
                    message.what = MESSAGE_PLAYER_NEXT_SEGMENT;
                    playHandler.sendMessage(message);
                } catch (Exception e) {
                }
            }
        }
    }

    private Handler playHandler = new Handler() { // handle
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1:
                // TODO
            }
            super.handleMessage(msg);
        }
    };
}
