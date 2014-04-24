package edu.bupt.mccdash.m3u8;

import java.util.ArrayList;

import android.util.Log;

public class M3u8Resolver {
    private static final String TAG = "M3u8Resolver";

    private ArrayList<String> tsList;

    public M3u8Resolver(String m3u8String) {
        Log.v(TAG, m3u8String);
        tsList = new ArrayList<String>();
        String[] lines = m3u8String.split("\n");
        Log.v(TAG, lines.toString());
        for (String s : lines) {
            Log.v(TAG, s);
            if (s.startsWith("http")) {
                tsList.add(s);
                
            }
        }
    }

    public String getAt(int index) {
        if (index >= 0 && index < tsList.size()) {
            return tsList.get(index);
        }
        return null;
    }

    public int getCount() {
        return tsList.size();
    }
}
