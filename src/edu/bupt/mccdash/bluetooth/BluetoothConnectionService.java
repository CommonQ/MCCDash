/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.bupt.mccdash.bluetooth;

import io.vov.vitamio.utils.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import edu.bupt.mccdash.MainActivity;
import edu.bupt.mccdash.io.FileFactory;
import edu.bupt.mccdash.io.Logger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */
public class BluetoothConnectionService {
	// Debugging
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";

	// Unique UUID for this application
	private static final UUID MY_UUID_SECURE = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	private static final UUID MY_UUID_INSECURE = UUID
			.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	// private AcceptThread mSecureAcceptThread;
	private AcceptThread mInsecureAcceptThread;
	private ConnectThread mConnectThread;
	// private ArrayList<ConnectedThread> threadPool = new
	// ArrayList<ConnectedThread>();

	public static HashMap<String, ConnectedThread> ConnectedThreadPool = new HashMap<String, ConnectedThread>();

	public static ArrayList<String> connectedNames = new ArrayList<String>();

	// private ConnectedThread mConnectedThread;
	private int mState;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// device

	
	private static int flag=0;//进入条件判断之后选择进入哪条分支
	private static boolean isEnter =false;//决定是否进入条件判断的
	
	/**
	 * Constructor. Prepares a new BluetoothChat session.
	 * 
	 * @param context
	 *            The UI Activity Context
	 * @param handler
	 *            A Handler to send messages back to the UI Activity
	 */
	public BluetoothConnectionService(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1)
				.sendToTarget();
	}

	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume()
	 */
	public synchronized void start() {
		if (D)
			Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// if (mConnectedThread != null) {mConnectedThread.cancel();
		// mConnectedThread = null;}
		if (ConnectedThreadPool != null && ConnectedThreadPool.size() != 0) {
			for (int i = 0; i < connectedNames.size(); i++) {
				ConnectedThreadPool.get(connectedNames.get(i)).cancel();

			}
			ConnectedThreadPool.clear();
			connectedNames.clear();
		}
		setState(STATE_LISTEN);

		// Start the thread to listen on a BluetoothServerSocket

		if (mInsecureAcceptThread == null) {
			mInsecureAcceptThread = new AcceptThread(false);
			mInsecureAcceptThread.start();
		}
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 * @param secure
	 *            Socket Security type - Secure (true) , Insecure (false)
	 */
	public synchronized void connect(BluetoothDevice device, boolean secure) {
		if (D)
			Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		// qq ��Ҫ���и�ģ� ��Ϊ�Ҳ��ܰ��Ѿ����ӵ��̹߳ص�����Ϊ����Ҫ���̡߳�
		// if (mConnectedThread != null) {mConnectedThread.cancel();
		// mConnectedThread = null;}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device, secure);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device, final String socketType) {
		Log.d(TAG, "connected, Socket Type:" + socketType);

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		// qq������и��Ҳ����Ϊ����Ҫ���ж��̲߳��������Բ��ܰ��Ѿ����ӵ��߳�ɾ�����
		// if (mConnectedThread != null) {mConnectedThread.cancel();
		// mConnectedThread = null;}

		// Cancel the accept thread because we only want to connect to one
		// device
		// �����������߳���Ҳ��Ҫ��ֹȡ����Ϊ����ͬʱ��Ҫ���ͻ��������ˡ����?��ȡ����Ϊ����֮�佨������֮���һ����
		// qqȡ����acceptedThread�̵߳���٣���Ϊ������Ҫ����ļ���
		// if (mSecureAcceptThread != null) {
		// mSecureAcceptThread.cancel();
		// mSecureAcceptThread = null;
		// }
		// if (mInsecureAcceptThread != null) {
		// mInsecureAcceptThread.cancel();
		// mInsecureAcceptThread = null;
		// }

		// Start the thread to manage the connection and perform transmissions
		ConnectedThread mConnectedThread = new ConnectedThread(socket,
				socketType, device);

		mConnectedThread.start();

		ConnectedThreadPool.put(device.getAddress(), mConnectedThread);
		connectedNames.add(device.getAddress());
		// Send the name of the connected device back to the UI Activity

		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
		// setState( );
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D)
			Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// if (mConnectedThread != null) {
		// mConnectedThread.cancel();
		// mConnectedThread = null;
		// }

		if (ConnectedThreadPool != null && ConnectedThreadPool.size() != 0) {
			for (int i = 0; i < connectedNames.size(); i++) {
				ConnectedThreadPool.get(connectedNames.get(i)).cancel();

			}
			ConnectedThreadPool.clear();
			connectedNames.clear();
		}

		if (mInsecureAcceptThread != null) {
			mInsecureAcceptThread.cancel();
			mInsecureAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		// ConnectedThread r;
		HashMap<String, ConnectedThread> rr;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {

			rr = ConnectedThreadPool;
			// r = mConnectedThread;
		}

		for (int i = 0; i < connectedNames.size(); i++) {
			ConnectedThreadPool.get(connectedNames.get(i)).write(out);

		}
		// r.write(out);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		BluetoothConnectionService.this.start();
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost(BluetoothDevice device) {
		// Send a failure message back to the Activity

		connectedNames.remove(device.getAddress());
		ConnectedThreadPool.remove(device.getAddress());

		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		BluetoothConnectionService.this.start();
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;
		private String mSocketType;

		public AcceptThread(boolean secure) {
			BluetoothServerSocket tmp = null;
			mSocketType = secure ? "Secure" : "Insecure";

			// Create a new listening server socket
			try {
				if (secure) {
					tmp = mAdapter.listenUsingRfcommWithServiceRecord(
							NAME_SECURE, MY_UUID_SECURE);
				} else {
					tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
							NAME_INSECURE, MY_UUID_INSECURE);
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D)
				Log.d(TAG, "Socket Type: " + mSocketType
						+ "BEGIN mAcceptThread" + this);
			setName("AcceptThread" + mSocketType);

			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (true) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "Socket Type: " + mSocketType
							+ "accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (BluetoothConnectionService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice(),
									mSocketType);
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							connected(socket, socket.getRemoteDevice(),
									mSocketType);

							break;
						}
					}
				}
			}
			if (D)
				Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

		}

		public void cancel() {
			if (D)
				Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Socket Type" + mSocketType
						+ "close() of server failed", e);
			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		private String mSocketType;

		public ConnectThread(BluetoothDevice device, boolean secure) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			mSocketType = secure ? "Secure" : "Insecure";

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				if (secure) {
					tmp = device
							.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
				} else {
					tmp = device
							.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
			setName("ConnectThread" + mSocketType);

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() " + mSocketType
							+ " socket during connection failure", e2);
				}
				connectionFailed();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothConnectionService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice, mSocketType);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect " + mSocketType
						+ " socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private BluetoothDevice mDevice;
		private StringBuilder sb;

		public ConnectedThread(BluetoothSocket socket, String socketType,
				BluetoothDevice device) {
			Log.d(TAG, "create ConnectedThread: " + socketType);
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			mDevice = device;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
			sb = new StringBuilder();
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = null;
			int bytes;

			int totalLength=0;
			int actualLength=0;
			FileInfo fileInfo = null;
			ByteArrayOutputStream bao  = new ByteArrayOutputStream ();
			
			while (true) {
				try {
					int length = mmInStream.available();
					totalLength+=length;
				
					//Log.v(TAG, "available length: "+length+" totalLength: "+totalLength);
					
					
					buffer = new byte[length];
					bytes = mmInStream.read(buffer);
					actualLength+=bytes;
					
					byte[] readBuf = (byte[]) buffer;
					//Log.v(TAG, "available length: "+length+" actualLength: "+actualLength);
					
					String readMessage = new String(readBuf, 0, bytes);
					
					//Log.v(TAG, "readMessage: "+readMessage);
					
					
					if(!isEnter&&readMessage.contains("START: BYTE")){
						
						
						String startString = new String(readBuf, 0, bytes);
						
						
						Log.v(TAG, "Enter ----START:BYTE----");
						
						//obtain the length and the file name from string.
																		
						fileInfo = FileInfo.getFileInfoFromString(startString);
						Log.v(TAG, "Enter ----START:BYTE---- file name: "+fileInfo.fileName+" file start: "+fileInfo.fileStartPoint);
						
						flag=1;
						isEnter=true;
						
						
						// in case that the  after END length segment there are also some bytes remains.
						if(startString.length()!=(startString.indexOf("END LENGTH")+"END LENGTH".length()+1)){
							
							int  endIndex = startString.indexOf("END LENGTH")+"END LENGTH".length();
							
							String tempString = startString.substring(0, endIndex);
							
							Log.v(TAG, "startString.length()!=\"END LENGTH\".length()"+"tempString: "+tempString+" startString:"+startString);
							
							int startPos=tempString.getBytes().length;
							Log.v(TAG, "readMessage.length()!=\"END LENGTH\".length() startPos: "+startPos+" readBuf: "+readBuf.length);
							bao.write(readBuf, startPos, readBuf.length-startPos);
						}
						
						continue;//直接进入下一次循环判断。
						
					}else if(!isEnter&&readMessage.contains("START: M3U8:")){
						
						
						String m3u8String = new String(readBuf, 0, bytes);
						
						Log.v(TAG, "Enter ----START: M3U8:----");
						
						if("START: M3U8:".length()!=m3u8String.length()){

							if(m3u8String.contains("END M3U8")){
							
							String subString = m3u8String.substring("START: M3U8:".length(),m3u8String.indexOf("END M3U8"));
							
							sb.append(subString);
							
							mHandler.obtainMessage(MainActivity.FINISH_M3U8, sb.toString().length(),
									-1, sb.toString()).sendToTarget();
							sb = new StringBuilder();
							}else{
								
								
								String subString = m3u8String.substring("START: M3U8:".length());
								
								sb.append(subString);
								
								flag=2;
								isEnter=true;
								
								
							}
							
						}else{
							
							flag=2;
							isEnter=true;
							
						}
						
						
						continue;//直接进入下一次循环
						
						
						
						//flag=2;
						//isEnter=true;
						
					}
					
					if(isEnter&&flag==2){
						
						String Message = new String(readBuf, 0, bytes);
						
						Log.v(TAG, "Enter ----flag==2---- Message:"+Message);
						
						if(Message.contains("END M3U8")){
							
							Log.v(TAG, "Enter ----END M3U8");
							if(Message.indexOf("END M3U8")>0){
								
								Log.v(TAG, "Enter ----END M3U8 Message.indexOf(\"END: BYTE\"):"+Message.indexOf("END M3U8"));
								
								
								flag=0;
								isEnter=false;
								String subString = Message.substring(0,Message.indexOf("END M3U8"));
								sb.append(subString);
								
								
								Log.v(TAG, "Enter ----END M3U8 final Message:"+sb.toString());
								
								mHandler.obtainMessage(MainActivity.FINISH_M3U8, sb.toString().length(),
										-1, sb.toString()).sendToTarget();
								
								
								sb = new StringBuilder();
								
								
								
								
								
							}else{
								
								
								flag=0;
								isEnter=false;
								
								Log.v(TAG, "Enter ----END M3U8 final Message:"+sb.toString());
								
								mHandler.obtainMessage(MainActivity.FINISH_M3U8, sb.toString().length(),
										-1, sb.toString()).sendToTarget();
								
								sb = new StringBuilder();
								
							}
							
							
							continue;
							
							
						}
						
						sb.append(Message);
						
					}
					
					
					
					
					
					if(isEnter&&flag==1){
						
						
						
						
						
						
						
						
						
						String readMessageEnd = new String(readBuf, 0, bytes);
						Log.v(TAG, "Enter ----flag==1---- readMEssageEnd:"+readMessageEnd);
						if(readMessageEnd.contains("END: BYTE")){
							Log.v(TAG, "Enter ----END: BYTE");
							if(readMessageEnd.indexOf("END: BYTE")>0){
								//int i=
								Log.v(TAG, "Enter ----END: BYTE readMessageEnd.indexOf(\"END: BYTE\"):"+readMessageEnd.indexOf("END: BYTE"));
								
								int endLength = "END: BYTE".getBytes().length;
								
								Log.v(TAG, "Enter ----END: BYTE readBuf.length:"+readBuf.length+" length: "+(readBuf.length-endLength));
								
								bao.write(readBuf,0,readBuf.length-endLength);
							
							
							}
							flag=0;
							isEnter=false;
							bao.flush();
							byte[] endFile =bao.toByteArray();
							InputStream input = new ByteArrayInputStream(endFile);
							FileFactory.RandomAccess("mccdash", "/" + fileInfo.fileName, input, fileInfo.fileStartPoint);
							bao.reset();
							input.close();
							
							
							
							if(readMessageEnd.contains("START: BYTE")){
								
								
								
								fileInfo = FileInfo.getFileInfoFromString(readMessageEnd);
								Log.v(TAG, "Enter ---END BYTE---START:BYTE---- file name: "+fileInfo.fileName+" file start: "+fileInfo.fileStartPoint);
								
								flag=1;
								isEnter=true;
								
								
								// in case that the  after END length segment there are also some bytes remains.
								if(readMessageEnd.length()!=(readMessageEnd.indexOf("END LENGTH")+"END LENGTH".length()+1)){
									
									int  endIndex = readMessageEnd.indexOf("END LENGTH")+"END LENGTH".length();
									
									String tempString = readMessageEnd.substring(0, endIndex);
									
									Log.v(TAG, "startString.length()!=\"END LENGTH\".length()"+" endIndex: "+endIndex+" tempString: "+tempString+" readMessageEnd:"+readMessageEnd);
									
									int startPos=tempString.getBytes().length;
									Log.v(TAG, "readMessageEnd.length()!=\"END LENGTH\".length() startPos: "+startPos+" readBuf: "+readBuf.length);
									bao.write(readBuf, startPos, readBuf.length-startPos);
									
								}
								
								
								
								
								
								
							}
							
							
							
							
							mHandler.obtainMessage(MainActivity.START_PLAY_VIDEO, fileInfo.fileLength,
									-1, fileInfo.fileName).sendToTarget();
							continue;
						}
						
						
						bao.write(readBuf);
					}
					
					
//					String readMessage=Base64.encodeToString(readBuf, Base64.DEFAULT);
					
					
					
					
					//标准base64编码
					//String readMessage = new String(readBuf, 0, bytes);
					//剩下的判断选择也要相应更改
//					Log.v(TAG, "read Message: "+readMessage);
//					if (readMessage.startsWith("END M3U8:")) {
//						mHandler.obtainMessage(MainActivity.FINISH_M3U8, bytes,
//								-1, sb.toString()).sendToTarget();
//						sb = new StringBuilder();
//
//					} else if (readMessage.startsWith("END TS:")) {
//						mHandler.obtainMessage(MainActivity.FINISH_TS, bytes,
//								-1, readMessage).sendToTarget();
//						sb = new StringBuilder();
//
//					} else if (readMessage.contains(Base64.encodeToString("***".getBytes(), Base64.DEFAULT))) {
//						
//						String end_64=Base64.encodeToString("***".getBytes(), Base64.DEFAULT);
//						Log.v(TAG, "readMessage end_64: "+end_64);
//						//end_64=baseTOUtf(end_64);
//						Log.v(TAG, "readMessage end_64: "+end_64);
//						readMessage=readMessage.substring(0, readMessage.indexOf(end_64));
//						Log.v(TAG, "readMessage subString: "+readMessage);
//						sb.append(readMessage);
//						Log.v(TAG, "END TT");
//						
//						String test =sb.toString();
//						//Base64.decode(test, Base64.NO_PADDING);
//						//Logger.i(test);
//						Log.v(TAG, "END FINISH TT----test.length:"+test.length());
//						
//						
//						byte[] testByte=Base64.decode(test, Base64.DEFAULT);
//						//byte[] testByte=test.getBytes();
//						
//						Log.v(TAG, "END FINISH TT----testByte.length:"+testByte.length);
//						
//						InputStream input = new ByteArrayInputStream(testByte);
//						
//						//转码问题导致转换前后不一致，应该用base64解决。
//						FileFactory.RandomAccess(FileFactory.SDPath, "/" + "rceivedfile.mp4", input, 0);
//						
////						mHandler.obtainMessage(MainActivity.FINISH_TS, bytes,
////								-1, readMessage).sendToTarget();
////						sb = new StringBuilder();
//						
//						
//					}

//					else {
//						
////						if(readMessage.length()>0){
//
//						Log.v(TAG, "Start transfer");
//						
//						String base=Base64.encodeToString("***".getBytes(), Base64.DEFAULT);
//						
//						Log.v(TAG, "Start transfer base---"+base);
//						
//						if(readMessage.contains(base)){
//							Log.v(TAG, "readMessage.contains(base)"+readMessage.substring(readMessage.length()-20, readMessage.length()));
//						}
//						
//						
//						
////						else{
////							Log.v(TAG, "not readMessage.contains(base)"+readMessage.substring(readMessage.length()-20, readMessage.length()));
////							if(readMessage.contains("\n")){
////								Log.v(TAG, "readMessage.contains(\"\n\")");
////								
////							}
////						}
//						
//						sb.append(readMessage);
//
////						}
//					}
				} catch (IOException e) {

					Log.e(TAG, "disconnected", e);

					connectionLost(mDevice);
					// Start the service over to restart listening mode
					BluetoothConnectionService.this.start();
					break;
				}

			}

		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public  void write(byte[] buffer) {
			try {
				//加上锁试一下
				
				mmOutStream.write(buffer);
				
				// mmOutStream.write("END:".getBytes());

				// Share the sent message back to the UI Activity
				// mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1,
				// buffer)
				// .sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}
		
		public String baseTOUtf(String str) throws UnsupportedEncodingException{
			
			byte[] data = Base64.decode(str, Base64.DEFAULT);
			String text = new String(data, "UTF-8");
			
			
			return text;
			
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
