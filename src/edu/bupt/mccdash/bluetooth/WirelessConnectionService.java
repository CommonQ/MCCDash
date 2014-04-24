package edu.bupt.mccdash.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import edu.bupt.mccdash.MainActivity;
import edu.bupt.mccdash.io.FileFactory;
import android.os.Handler;
import android.util.Log;

public class WirelessConnectionService extends Thread {
	private static final String TAG = "WirelessChatService";
	private static final boolean D = true;
	private final int SERVER_HOST_PORT = 19400;
	private final Handler mHandler;
	private static ServerSocket aServerSocket = null;
	public static final int STATE_CONNECTED = 3;
	public static ArrayList<ConnectThread> ConnectedThreadPool = new ArrayList<ConnectThread>();

	public WirelessConnectionService(Handler handler) {
		mHandler = handler;
		this.start();
		if (D)
			Log.d(TAG, "WirelessChatService init");
	}

	public synchronized void connect(String device) {
		ConnectThread tmp = new ConnectThread(device);
		synchronized (ConnectedThreadPool) {
			ConnectedThreadPool.add(tmp);
		}
	}

	public synchronized int getstate() {
		return STATE_CONNECTED;
	}

	public void write(byte[] out) {
		if (D)
			Log.d(TAG, "try to write "
					+ (out.length < 100 ? "\n"+new String(out) : out.length));
		synchronized (ConnectedThreadPool) {
			Log.v(TAG, "ConnectedThreadPool.size() "+ConnectedThreadPool.size());
			for (int i = 0; i < ConnectedThreadPool.size(); i++) {
				ConnectedThreadPool.get(i).write(out);
			}
		}
		if (D)
			Log.d(TAG, "write over");
	}

	public void run() {
		try {
			aServerSocket = new ServerSocket(SERVER_HOST_PORT);
		} catch (Exception e) {
			Log.e(TAG, "Exception during listen", e);
		}
		while (true) {
			try {
				Socket aSessionSoket = aServerSocket.accept();
				ConnectedThreadPool.add(new ConnectThread(aSessionSoket));
			} catch (IOException e) {
				Log.e(TAG, "Exception during accept", e);
			}
		}
	}

	private class ConnectThread extends Thread {
		private Socket mmSocket;
		private String mmDevice;
		private InputStream mmInStream;
		private OutputStream mmOutStream;

		public ConnectThread(String device) {
			try {
				if (D)
					Log.d(TAG, "connect to: " + device);
				mmDevice = device;
				mmSocket = new Socket(mmDevice, SERVER_HOST_PORT);
				mmOutStream = mmSocket.getOutputStream();
				mmInStream = mmSocket.getInputStream();
				this.start();
			} catch (Exception e) {
				Log.e(TAG, "Exception during connect", e);
			}
		}

		public ConnectThread(Socket already) {
			try {
				mmDevice = already.getRemoteSocketAddress().toString();
				if (D)
					Log.d(TAG, "being connect: " + mmDevice + " with: "
							+ already.getLocalAddress().toString());
				mmSocket = already;
				mmOutStream = mmSocket.getOutputStream();
				mmInStream = mmSocket.getInputStream();
				this.start();
			} catch (Exception e) {
				Log.e(TAG, "Exception during being connect", e);
			}
		}

		public void run() {
			while (true) {
				try {
					Log.v(TAG, "do not know msg");
					byte[] buffer = new byte[10240];
					int bytes = mmInStream.read(buffer);// dont known
					String diff = new String(buffer, 0, bytes);// which msg
					int orignalLen=diff.length();

					if (diff.contains("START: M3U8:")) {// m3u8
						Log.v(TAG, "START: M3U8");
						String m3u8String = diff;
						while (true) {
							if (m3u8String.contains("END M3U8")) {
								m3u8String = m3u8String.substring(
										"START: M3U8:".length(),
										m3u8String.indexOf("END M3U8"));
								break;
							} else {
								Log.v(TAG, "READ: M3U8");
								bytes = mmInStream.read(buffer);
								diff = new String(buffer, 0, bytes);
								m3u8String += diff;
							}
						}
						mHandler.obtainMessage(MainActivity.FINISH_M3U8,
								m3u8String.length(), -1, m3u8String)
								.sendToTarget();
						Log.v(TAG, "END: M3U8");
					} else {// file
						Log.v(TAG, "START FILE");
						ByteArrayOutputStream bao = new ByteArrayOutputStream();
						String name = diff.substring(0, diff.indexOf("\n"));
						Log.v(TAG, "FILE INFO " + name);
						diff = diff.substring(diff.indexOf("\n") + 1);
						String lengthStr = diff
								.substring(0, diff.indexOf("\n"));
						diff = diff.substring(diff.indexOf("\n") + 1);
						String[] len = lengthStr.split("-");
						int start = Integer.valueOf(len[0]);
						int thislen = Integer.valueOf(len[1])-start;
						Log.v(TAG,  " start " + start
								+ " len " + thislen);
						bao.write(buffer, orignalLen-diff.length(), bytes-orignalLen+diff.length());
						Log.v(TAG, "BAO SIZE " + bao.size());
						while (true) {
							if (bao.size() < thislen) {
								bytes = mmInStream.read(buffer,0,thislen-bao.size()>buffer.length?buffer.length:thislen-bao.size());
								bao.write(buffer, 0, bytes);
							} else {
								break;
							}
						}
						Log.v(TAG, "BAO SIZE " + bao.size());
						byte[] endFile = bao.toByteArray();
						InputStream input = new ByteArrayInputStream(endFile);
						FileFactory.RandomAccess("mccdash", "/" + name, input,
								start);
						bao.reset();
						input.close();
						mHandler.obtainMessage(MainActivity.START_PLAY_VIDEO,
								thislen, -1, name).sendToTarget();
					}
				} catch (Exception e) {
					Log.e(TAG, "recieve error", e);
					break;
				}
			}
		}

		public void write(byte[] buffer) {
			if (D)
				Log.d(TAG, "we write: " + mmDevice + " len: " + buffer.length);

			try {
				synchronized (this) {
					mmOutStream.write(buffer);
				}
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}
	}
}
