package edu.bupt.mccdash;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.bupt.mccdash.bluetooth.*;
import edu.bupt.mccdash.cpuutils.CpuManager;
import edu.bupt.mccdash.io.FileFactory;
import edu.bupt.mccdash.io.Logger;
import edu.bupt.mccdash.m3u8.M3u8Resolver;

public class MainActivity extends Activity implements
		OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
		OnVideoSizeChangedListener, SurfaceHolder.Callback {

	private static final String TAG = "MainActivity";

	/**
	 * qq bluetooth
	 * 
	 * 
	 */

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_FINIFSH = 6;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	// private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 7;
	private static final int REQUEST_ENABLE_BT = 8;
	public static final int FINISH_M3U8 = 11;
	public static final int MESSAGE_DISTRIBUTE_TS = 12;

	public static final int FINISH_TS = 13;

	public static final int MESSAGE_REQUEST_NEXT_SEGMENT = 14;

	public static final int MESSAGE_DELIVRY_RATIO = 15;

	public static final int START_PLAY_VIDEO = 16;

	public static int TS_COUNT = 0;

	public static int SEG_NUM = 1;

	public static final String SDDIR = "/sdcard/mccdash/";

	public static ArrayList<String> nameList = new ArrayList<String>();

	public int testCount = 2;

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		Log.v(TAG, "-onPause-");
		releaseMediaPlayer();
		doCleanUp();
		super.onPause();
	}

	public static int TS_TOTAL_COUNT = 0;

	private HashMap<String, Integer> bufferedTs = new HashMap<String, Integer>();

	private HashMap<Integer, String> fileName = new HashMap<Integer, String>();;
	private int bufferingTs = -1;
	private int fileNameTs = 1;
	private int playVideoTs = 1;
	private M3u8Resolver m3u8Resolver;

	// qq bluetooth
	//private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private WirelessConnectionService mChatService = null;
	private String mConnectedDeviceName = null;

	//private Button buttonRequestM3u8;
	//private EditText edittextDestfile;
	//public TextView deliveryText;

	// message types
	public static final int MESSAGE_REQUEST_M3U8 = 9;
	public static final int MESSAGE_DISTRIBUTE_M3U8 = 10;

	public static final int TEST_FILE_TRANSGER = 129;

	// message data
	public static final String DATA = "data";

	public static final String TS_DESTINATION = "http://10.105.39.143/sample_test-1.ts";
	private final int timeLengthForSegment = 10000;

	// preset params
	// private String destfile = "";
	// private String param = "?num=3&key=3212";
	private String param = "?num=2&key=7222";
	public boolean firstStart = true;

	public boolean isFirstStart = true;
	public boolean isPlaying = false;

	/**
	 * 
	 * 
	 * Below here it's the Vitamio setup configuration
	 * 
	 * 
	 */

	private int mVideoWidth;
	private int mVideoHeight;
	private MediaPlayer mMediaPlayer;
	private SurfaceView mPreview;
	private SurfaceHolder holder;
	private String path;
	private Bundle extras;
	private static final String MEDIA = "media";
	private static final int LOCAL_AUDIO = 11;
	private static final int STREAM_AUDIO = 12;
	private static final int RESOURCES_AUDIO = 13;
	private static final int LOCAL_VIDEO = 14;
	private static final int STREAM_VIDEO = 15;
	private boolean mIsVideoSizeKnown = false;
	private boolean mIsVideoReadyToBePlayed = false;

	//Button start;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}


		if (!LibsChecker.checkVitamioLibs(this))
			return;
		setContentView(R.layout.activity_main);

		mPreview = (SurfaceView) findViewById(R.id.surface);
		holder = mPreview.getHolder();
		holder.addCallback(this);
		holder.setFormat(PixelFormat.RGBA_8888);

		// qq bluetooth
		//mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
//		if (mBluetoothAdapter == null) {
//			Toast.makeText(this, "Bluetooth is not available",
//					Toast.LENGTH_LONG).show();
//			finish();
//			return;
//		}

		//deliveryText = (TextView) findViewById(R.id.deliveryratio);

		//start = (Button) findViewById(R.id.startPlay);

		//start.setOnClickListener(onClickListener);
		
		//
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		//
		// while (true) {
		//
		// try {
		// Log.v("MainActivity", "CPU:"+CpuManager.processCpu());
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// }
		//
		// }
		//
		// }).start();
		//
		//
		//

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//if (mChatService != null)
			

		releaseMediaPlayer();
		doCleanUp();
		Log.e(TAG, "--- ON DESTROY ---");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getstate() == BluetoothConnectionService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		if (!mBluetoothAdapter.isEnabled()) {
//			Intent enableIntent = new Intent(
//					BluetoothAdapter.ACTION_REQUEST_ENABLE);
//			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//			// Otherwise, setup the chat session
//		} else 
		
		
			//if (mChatService == null)
				setupChat();
		
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		//buttonRequestM3u8 = (Button) findViewById(R.id.bt_request_m3u8);
		//edittextDestfile = (EditText) findViewById(R.id.et_destfile);
		//buttonRequestM3u8.setOnClickListener(onClickListener);

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.e(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {

		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
//			if (resultCode == Activity.RESULT_OK) {
//				connectDevice(data, false);
//			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.e(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.check_group:
			// Launch the DeviceListActivity to see devices and do scan
			ArrayList<String> names = BluetoothConnectionService.connectedNames;
			Toast.makeText(this, names.toString(), Toast.LENGTH_SHORT).show();

			return true;
		case R.id.insecure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
//			serverIntent = new Intent(this, DeviceListActivity.class);
//			startActivityForResult(serverIntent,
//					REQUEST_CONNECT_DEVICE_INSECURE);
			
			
			
			mChatService.connect("10.105.39.100");
			
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		case R.id.clearcache:
			File dir = new File(FileFactory.SDPath + "mccdash");

			FileFactory.delete(dir);

			return true;
		case R.id.startplay:
			Log.e(TAG, "buttonRequestM3u8 clicked");
			Message msg = new Message();
			msg.what = MESSAGE_REQUEST_M3U8;
			String temp=getResources().getString(R.string.default_destfile);
			msg.obj = temp;
			handler.sendMessage(msg);
			return true;
			
		case R.id.local:
			playVideo(LOCAL_VIDEO);
//			Message message = handler.obtainMessage();
//			message.what = TEST_FILE_TRANSGER;
//
//			FileInfo fileInfo = new FileInfo();
//
//			// 这个只是测试之用，以后还得改。只是用来测试 文件拼接问题。
//			if (testCount == 2) {
//				fileInfo = FileInfo.getFile("1/" + testCount + ".ts",
//						"hehe.mp4", 875890);
//			} else {
//				fileInfo = FileInfo.getFile("1/" + testCount + ".ts",
//						"hehe.mp4", 0);
//			}
//			message.obj = fileInfo;
//			message.sendToTarget();
//			testCount--;
			return true;
		}
		return false;
	}

	private OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

			switch (v.getId()) {
//			case R.id.bt_request_m3u8:
//				Log.e(TAG, "buttonRequestM3u8 clicked");
//
//				Message msg = new Message();
//				msg.what = MESSAGE_REQUEST_M3U8;
//				msg.obj = edittextDestfile.getText().toString();
//				handler.sendMessage(msg);
//				break;
//			case R.id.startPlay:
//				// playVideo(LOCAL_VIDEO);
//				Message message = handler.obtainMessage();
//				message.what = TEST_FILE_TRANSGER;
//
//				FileInfo fileInfo = new FileInfo();
//
//				// 这个只是测试之用，以后还得改。只是用来测试 文件拼接问题。
//				if (testCount == 2) {
//					fileInfo = FileInfo.getFile("1/" + testCount + ".ts",
//							"hehe.mp4", 875890);
//				} else {
//					fileInfo = FileInfo.getFile("1/" + testCount + ".ts",
//							"hehe.mp4", 0);
//				}
//				message.obj = fileInfo;
//				message.sendToTarget();
//				testCount--;
//				break;
			
			// File file = new File(FileFactory.SDPath+"110.mp4");

			// try {
			// byte[]fileByte=FileFactory.getByte(file);
			// Logger.i("fileByte.length"+fileByte.length);
			// Log.v(TAG, "110.mp4 byte[]fileByte---"+fileByte.length);
			//
			// //qq一会再改
			// //String test =Base64.encodeToString(fileByte, Base64.DEFAULT);
			// //String test = new
			// String(Base64.encode(fileByte,Base64.DEFAULT));
			// //Log.v(TAG, "110.mp4 String---"+test.length());
			// //Logger.i("test.length()"+test.length());
			// //Logger.i(test);
			//
			// fileInfo.fileContent=fileByte;
			// fileInfo.fileName="outputtest.mp4";
			// fileInfo.fileStartPoint=0;
			// fileInfo.fileLength=fileByte.length;
			// message.obj=fileInfo;
			//
			//
			// } catch (Exception e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			default:
				Log.e(TAG, "default");
				return;
			}
		}
	};

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.e(TAG, "msg.what: "+msg.what);
			switch (msg.what) {
			case MESSAGE_REQUEST_M3U8:
				Log.e(TAG, "MESSAGE_REQUEST_M3U8");
				new HttpDownloader(handler).download((String) msg.obj, param,
						onHttpDownloadCompleteListener);
				break;
			case TEST_FILE_TRANSGER:

				Log.e(TAG, "TEST_FILE_TRANSGER");

				final FileInfo Testmessage = (FileInfo) msg.obj;

				Log.v(TAG, "110.mp4 byte[]Testmessage---"
						+ Testmessage.fileLength);

				Toast.makeText(getApplicationContext(),
						"testMessage.length: " + Testmessage.fileLength,
						Toast.LENGTH_SHORT).show();

				// qq not test it yet

				new Thread(new Runnable() {

					@Override
					public void run() {

						 
						
						sendMessage("START: BYTE" + "NAME:"
								+ Testmessage.fileName + "END NAME" + "LENGTH:"
								+ Testmessage.fileStartPoint + "END LENGTH");
						// sendMessage("NAME:"+"testoutput.mp4");
						// sendMessage("END NAME");
						// sendMessage("LENGTH:0");
						// sendMessage("END LENGTH");

						sendMessage(Testmessage.fileContent);
						sendMessage("END: BYTE");

						// qq 一会再改回来
						// String
						// val=Base64.encodeToString("***".getBytes(),Base64.DEFAULT);

						// Log.v(TAG, "val: "+val);
						// sendMessage(val);
						// Toast.makeText(getApplicationContext(), val,
						// Toast.LENGTH_SHORT).show();

					}

				}).start();

				break;
			case MESSAGE_DISTRIBUTE_M3U8:
				Log.e(TAG, "MESSAGE_DISTRIBUTE_M3U8");

				final String message = (String) msg.obj;
				// Toast.makeText(getApplicationContext(), message,
				// Toast.LENGTH_SHORT).show();

				// qq not test it yet
				new Thread(new Runnable() {

					@Override
					public void run() {

						sendMessage("START: M3U8:");
						sendMessage(message);
						sendMessage("END M3U8");

					}

				}).start();

				// bufferingTs++;

				// qq
				m3u8Resolver = new M3u8Resolver(message);
				donwloadTask();
				Log.e(TAG,
						"m3u8Resolver.getAt(bufferingTs): "
								+ m3u8Resolver.getAt(bufferingTs) + " param: "
								+ param + " ");
				// new
				// HttpDownloader(handler).download(m3u8Resolver.getAt(bufferingTs),
				// param, onHttpDownloadCompleteListener);
				// Toast.makeText(getApplicationContext(),
				// "Finish reading m3u8 list", Toast.LENGTH_SHORT).show();
				// Message msgReq = new Message();
				// msgReq.what = MESSAGE_REQUEST_NEXT_SEGMENT;
				// handler.sendMessageDelayed(msgReq, timeLengthForSegment);

				break;

			case MESSAGE_DISTRIBUTE_TS:
				
				
				
				
				Log.e(TAG, "MESSAGE_DISTRIBUTE_TS");

				final FileInfo messageRecv = (FileInfo) msg.obj;

				Log.v(TAG, "110.mp4 byte[]Testmessage---"
						+ messageRecv.fileLength);

				Toast.makeText(getApplicationContext(),
						"start:"+messageRecv.fileStartPoint+" range:"+messageRecv.fileLength,
						Toast.LENGTH_SHORT).show();

				Log.e(TAG,"start:"+messageRecv.fileStartPoint+" range:"+messageRecv.fileLength);
				// qq not test it yet

				new Thread(new Runnable() {

					@Override
					public void run() {

						sendMessage( messageRecv.fileName + "\n" +  messageRecv.fileStartPoint + "-"+(messageRecv.fileStartPoint+messageRecv.fileLength)+"\n");
						// sendMessage("NAME:"+"testoutput.mp4");
						// sendMessage("END NAME");
						// sendMessage("LENGTH:0");
						// sendMessage("END LENGTH");
//						ByteArrayOutputStream tmp = new ByteArrayOutputStream();
//tmp.write(messageRecv.fileContent, messageRecv.fileStartPoint, messageRecv.fileLength);
//						
						sendMessage(messageRecv.fileContent);
						//sendMessage("END: BYTE");

						// qq 一会再改回来
						// String
						// val=Base64.encodeToString("***".getBytes(),Base64.DEFAULT);

						// Log.v(TAG, "val: "+val);
						// sendMessage(val);
						// Toast.makeText(getApplicationContext(), val,
						// Toast.LENGTH_SHORT).show();

					}

				}).start();

				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				

//				Log.e(TAG, "MESSAGE_DISTRIBUTE_TS");
//
//				final String messageRecv = ((recvedFile) msg.obj).recv;
//				final String messageDestfile = ((recvedFile) msg.obj).destfile;
//
//				new Thread(new Runnable() {
//
//					@Override
//					public void run() {
//
//						sendMessage("START TS:");
//						sendMessage(messageRecv);
//						sendMessage("END TS" + messageDestfile);
//
//					}
//
//				}).start();

				// String strM3u82=TS_DESTINATION;
				//
				// new HttpDownloader().download(strM3u82, param,
				// onHttpDownloadCompleteListener);
				//
				// Toast.makeText(getApplicationContext(),
				// "Finish reading m3u8 list", Toast.LENGTH_SHORT).show();
				//
				//

				break;

			case MESSAGE_STATE_CHANGE:
				Log.e(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothConnectionService.STATE_CONNECTED:

					break;
				case BluetoothConnectionService.STATE_CONNECTING:

					break;
				case BluetoothConnectionService.STATE_LISTEN:
				case BluetoothConnectionService.STATE_NONE:

					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				Toast.makeText(getApplicationContext(), writeMessage,
						Toast.LENGTH_SHORT).show();

				break;
			case MESSAGE_READ:

				String str = (String) msg.obj;

				Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT)
						.show();
				break;

			case MESSAGE_DELIVRY_RATIO:

				String ratio = (String) msg.obj;
				Toast.makeText(getApplicationContext(), ratio,
						Toast.LENGTH_SHORT).show();
				//deliveryText.append("\n" + ratio);
				break;

			case START_PLAY_VIDEO:

				Log.e(TAG,"Enter START_PLAY_VIDEO");
				String name = (String) msg.obj;

				if (bufferedTs.containsKey(name)) {
					int count = bufferedTs.get(name);
					bufferedTs.put(name, count + 1);
					if (bufferedTs.get(name) == WirelessConnectionService.ConnectedThreadPool
							.size() + 1) {

						Log.e(TAG,
								"----------finish  WirelessChatService.connectedNames.size() + 1: "
										+ (WirelessConnectionService.ConnectedThreadPool
												.size() + 1));
						fileName.put(fileNameTs, name);
						fileNameTs++;
						if (isFirstStart) {
							isFirstStart = false;
							isPlaying = true;
							playVideo(LOCAL_VIDEO);
						}

					}

				} else {
					bufferedTs.put(name, 1); 
					
					if (bufferedTs.get(name) == WirelessConnectionService.ConnectedThreadPool
							.size() + 1) {

						Log.e(TAG,
								"----------finish  WirelessChatService.ConnectedThreadPool.size() + 1: "
										+ (WirelessConnectionService.ConnectedThreadPool
												.size() + 1));
						fileName.put(fileNameTs, name);
						fileNameTs++;
						if (isFirstStart) {
							isFirstStart = false;
							isPlaying = true;
							playVideo(LOCAL_VIDEO);
						}

					}

				}

				/*
				 * if (bufferedTs.containsKey()) { int count =
				 * bufferedTs.get(key4Map); bufferedTs.put(key4Map, count + 1);
				 * if (bufferedTs.get(key4Map) ==
				 * BluetoothChatService.connectedNames .size() + 1) { Log.e(TAG,
				 * "----------finish  BluetoothChatService.connectedNames.size() + 1: "
				 * + (BluetoothChatService.connectedNames .size() + 1));
				 * Log.e(TAG, "----------finish " + key4Map); } } else {
				 * bufferedTs.put(key4Map, 1); }
				 */

				// 不在这里进行下载了，因为还有传过来的数据。
				// donwloadTask();

				// String name = (String) msg.obj;
				// Log.e(TAG, "START_PLAY_VIDEO name:" + name + " fileNameTs:"
				// + fileNameTs);
				// fileName.put(fileNameTs, name);
				// fileNameTs++;
				// if (isFirstStart) {
				// isFirstStart = false;
				// isPlaying = true;
				// playVideo(LOCAL_VIDEO);
				// }

				if (!isPlaying) {

					String strPath = "";

					if (fileName.containsKey(playVideoTs)) {

						isPlaying = true;

						Log.d(TAG,
								"fileName.containsKey playVideoTsplayVideoTs: "
										+ playVideoTs);
						strPath = SDDIR + fileName.get(playVideoTs);
						Toast.makeText(getApplicationContext(), playVideoTs+"th video",
								Toast.LENGTH_LONG).show();
						playVideoTs++;

						try {

							mMediaPlayer.reset();
							mMediaPlayer.setDataSource(strPath);
							mMediaPlayer.prepare();
							mMediaPlayer.getMetadata();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}
				
				Log.e(TAG,"Exit START_PLAY_VIDEO");

				break;
			case FINISH_M3U8:
				// String strM3u8=(String) msg.obj;
				Log.e(TAG, "Receive M3U8 list from bluetooth");
				
				


				
				
				
				
				
				final String messageM3u8 = (String) msg.obj;
				// Toast.makeText(getApplicationContext(), message,
				// Toast.LENGTH_SHORT).show();

				// qq not test it yet
//				new Thread(new Runnable() {
//
//					@Override
//					public void run() {
//
//						sendMessage("START: M3U8:");
//						sendMessage(message);
//						sendMessage("END M3U8");
//
//					}
//
//				}).start();

				// bufferingTs++;

				// qq
				
				bufferedTs = new HashMap<String, Integer>();
				fileName= new HashMap<Integer, String>();
				m3u8Resolver = new M3u8Resolver(messageM3u8);
				donwloadTask();
				Log.e(TAG,
						"m3u8Resolver.getAt(bufferingTs): "
								+ m3u8Resolver.getAt(bufferingTs) + " param: "
								+ param + " ");
				
				
				
				
				
				
				
				
				
				

				// qq
				/**
				 * bufferedTs = new HashMap<String, Integer>();
				 * 
				 * m3u8Resolver = new M3u8Resolver((String) msg.obj);
				 * 
				 * // String strM3u8 = m3u8Resolver;
				 * 
				 * bufferingTs++; new HttpDownloader(handler).download(
				 * m3u8Resolver.getAt(bufferingTs), param,
				 * onHttpDownloadCompleteListener);
				 * Toast.makeText(getApplicationContext(),
				 * "Finish reading m3u8 list", Toast.LENGTH_SHORT).show();
				 * Message msgReqFirst = new Message(); msgReqFirst.what =
				 * MESSAGE_REQUEST_NEXT_SEGMENT;
				 * handler.sendMessageDelayed(msgReqFirst,
				 * timeLengthForSegment);
				 **/

				break;

			case FINISH_TS:

				// 在 startPlay里进行bufferedTs操作

				// Log.e(TAG, "FINISH_TS " + (String) msg.obj);
				// String key4Map = ((String) msg.obj).substring(7);
				//
				// Log.e(TAG, "FINISH_TS key4Map " + key4Map);
				//
				// if (bufferedTs.containsKey(key4Map)) {
				// int count = bufferedTs.get(key4Map);
				// bufferedTs.put(key4Map, count + 1);
				// if (bufferedTs.get(key4Map) ==
				// BluetoothChatService.connectedNames
				// .size() + 1) {
				// Log.e(TAG,
				// "----------finish  BluetoothChatService.connectedNames.size() + 1: "
				// + (BluetoothChatService.connectedNames
				// .size() + 1));
				// Log.e(TAG, "----------finish " + key4Map);
				// }
				// } else {
				// bufferedTs.put(key4Map, 1);
				// }

				break;

			case MESSAGE_FINIFSH:
				Toast.makeText(getApplicationContext(), "Finished!!!",
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_DEVICE_NAME:

				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;

			case MESSAGE_REQUEST_NEXT_SEGMENT:

				// donwloadTask();

				// qq 这个十秒下载一次的机制暂且替换成下了就接着下的模式。

				// Timer timer = new Timer();
				// requestTask a = new requestTask();
				//
				// timer.schedule(a,0,10000);
				//

				// long now= System.currentTimeMillis();
				// bufferingTs++;
				// new
				// HttpDownloader().download(m3u8Resolver.getAt(bufferingTs),
				// param, onHttpDownloadCompleteListener);
				// Toast.makeText(getApplicationContext(),
				// "request next segment", Toast.LENGTH_SHORT).show();
				// Log.e(TAG, "request next segment");
				// Message msgReqNext = new Message();
				// msgReqNext.what = MESSAGE_REQUEST_NEXT_SEGMENT;
				// handler.sendMessageDelayed(msgReqNext, timeLengthForSegment);
				break;
			default:
				Log.e(TAG, "default");
				return;

			}
		}

		
		
		//尝试用锁锁住，来防止之前出现的蓝牙堵塞的状况。不过还是会有读数组出现负数的情况。
		private  void sendMessage(String message) {
			// Check that we're actually connected before trying anything
			if (mChatService.getstate() != BluetoothConnectionService.STATE_CONNECTED) {
				// Toast.makeText(this, R.string.not_connected,
				// Toast.LENGTH_SHORT).show();
				return;
			}

			// Check that there's actually something to send
			if (message.length() > 0) {
				// Get the message bytes and tell the BluetoothChatService to
				// write
				// 标准 base64解码
				byte[] send = message.getBytes();
				
				//byte b[]=Base64.decode(message,Base64.DEFAULT);
				mChatService.write(send);
			}
		}

		private void sendMessage(byte[] message) {
			// Check that we're actually connected before trying anything
			if (mChatService.getstate() != BluetoothConnectionService.STATE_CONNECTED) {
				// Toast.makeText(this, R.string.not_connected,
				// Toast.LENGTH_SHORT).show();
				return;
			}

			// Check that there's actually something to send
			if (message.length > 0) {
				// Get the message bytes and tell the BluetoothChatService to
				// write
				// 标准 base64解码
				// byte[] send = message.getBytes();

				// byte b[]=Base64.decode(message,Base64.DEFAULT);
				Log.d(TAG, "write file" );
				mChatService.write(message);
			}
		}

	};

	private OnHttpDownloadCompleteListener onHttpDownloadCompleteListener = new OnHttpDownloadCompleteListener() {
		@Override
		public void OnHttpDownloadComplete(int filetype, String recv,
				String destfile, int startPos,int range) {
			switch (filetype) {
			case HttpDownloader.FILETYPE_M3U8:
				Log.e(TAG, "OnHttpDownloadComplete FILETYPE_M3U8");
				Message msg = new Message();
				msg.what = MESSAGE_DISTRIBUTE_M3U8;
				msg.obj = recv;
				handler.sendMessage(msg);

				//bufferedTs = new HashMap<String, Integer>();
				//fileName = new HashMap<Integer, String>();

				break;
			case HttpDownloader.FILETYPE_TS:

				// Log.e(TAG, "OnHttpDownloadComplete FILETYPE_TS");
				// Message msgTS = new Message();
				// msgTS.what = MESSAGE_DISTRIBUTE_TS;
				// Log.e(TAG, "recv-" + recv + " destfile-" + destfile);
				// msgTS.obj = new recvedFile(recv, destfile);
				// handler.sendMessage(msgTS);
				// // TS_COUNT++;
				//
				// Log.e(TAG, "FINISH_TS recv length: " + recv.length());
				//

				Log.e(TAG, "OnHttpDownloadComplete FILETYPE_TS");
				//Message msgTS = new Message();
				Message msgTS=handler.obtainMessage();
				msgTS.what = MESSAGE_DISTRIBUTE_TS;

				FileInfo fileInfo = new FileInfo();

				// 这个只是测试之用，以后还得改。只是用来测试 文件拼接问题。

				fileInfo = FileInfo.getFile("mccdash" + "/" + destfile,
						destfile, startPos,range);
				Log.e(TAG, " destfile-" + destfile + " startPos: " + startPos);
				msgTS.obj = fileInfo;
				Log.e(TAG, "handler.sendMessage(msgTS): MESSAGE_DISTRIBUTE_TS");
				boolean isSuccess= handler.sendMessage(msgTS);
				Log.e(TAG, "sendMEssage success:"+isSuccess);
				
				donwloadTask();

				// 不在这进行bufferedTs操作，在 Start_play进行操作。
				// if (bufferedTs.containsKey(destfile)) {
				// int count = bufferedTs.get(destfile);
				// bufferedTs.put(destfile, count + 1);
				// Log.e(TAG,
				// "bufferedTs.get(destfile) : "
				// + bufferedTs.get(destfile));
				// Log.e(TAG, "FINISH_TS key4Map " + destfile);
				// if (bufferedTs.get(destfile) ==
				// BluetoothChatService.connectedNames
				// .size() + 1) {
				// Log.e(TAG,
				// "----------finish  BluetoothChatService.connectedNames.size() + 1： "
				// + (BluetoothChatService.connectedNames
				// .size() + 1));
				// Log.e(TAG, "----------finish " + destfile);
				// }
				// } else {
				// bufferedTs.put(destfile, 1);
				// Log.e(TAG,
				// "----------finish " + destfile
				// + " bufferedTs.get(destfile):"
				// + bufferedTs.get(destfile));
				//
				// }

				break;

			default:
				Log.e(TAG, "default");
				return;
			}
		}
	};

	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getstate() != BluetoothConnectionService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);
		}
	}

	private void setupChat() {
		Log.e(TAG, "setupChat()");

		mChatService = new WirelessConnectionService(handler);

	}

	private void ensureDiscoverable() {
		Log.e(TAG, "ensure discoverable");
//		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//			Intent discoverableIntent = new Intent(
//					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//			discoverableIntent.putExtra(
//					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//			startActivity(discoverableIntent);
//		}
	}

//	private void connectDevice(Intent data, boolean secure) {
//		// Get the device MAC address
//		String address = data.getExtras().getString(
//				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
//		// Get the BluetoothDevice object
//		//BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//		// Attempt to connect to the device
//		mChatService.connect("");
//	}

	private class recvedFile {
		public String recv;
		public String destfile;

		public recvedFile(String recv, String destfile) {
			this.recv = recv;
			this.destfile = destfile;
		}
	}

	public class requestTask extends TimerTask {
		long previous;
		long now;

		@Override
		public void run() {
			// TODO Auto-generated method stub

			bufferingTs++;
			new HttpDownloader(handler).download(
					m3u8Resolver.getAt(bufferingTs), param,
					onHttpDownloadCompleteListener);
			// Toast.makeText(getApplicationContext(), "request next segment",
			// Toast.LENGTH_SHORT).show();
			if (firstStart) {
				previous = 0;
				now = System.currentTimeMillis();
				firstStart = false;
			} else {

				previous = now;
				now = System.currentTimeMillis();
			}

			Log.e(TAG, "request next segment now-previous: " + (now - previous));

		}

	}

	public void donwloadTask() {

		long previous;
		long now = 0;

		bufferingTs++;
		if (m3u8Resolver.getAt(bufferingTs) == null) {

			Log.e(TAG, "Downloaded all the segment");
			return;
		}
		new HttpDownloader(handler).download(m3u8Resolver.getAt(bufferingTs),
				param, onHttpDownloadCompleteListener);
		// Toast.makeText(getApplicationContext(), "request next segment",
		// Toast.LENGTH_SHORT).show();
		if (firstStart) {
			previous = 0;
			now = System.currentTimeMillis();
			firstStart = false;
		} else {

			previous = now;
			now = System.currentTimeMillis();
		}

		Log.e(TAG, "request next segment now-previous: " + (now - previous));

	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.v(TAG, "--surfaceCreated--");
		// playVideo(LOCAL_VIDEO);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onVideoSizeChanged called");
		if (width == 0 || height == 0) {
			Log.e(TAG, "invalid video width(" + width + ") or height(" + height
					+ ")");
			return;
		}
		mIsVideoSizeKnown = true;
		mVideoWidth = width;
		mVideoHeight = height;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub

		mIsVideoReadyToBePlayed = true;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onCompletion called");

		// String strPath = "/sdcard/2-1.mp4.ts";
		String strPath = "";
		SEG_NUM++;

		if (fileName.containsKey(playVideoTs)) {

			Log.d(TAG, "fileName.containsKey playVideoTsplayVideoTs: "
					+ playVideoTs);
			strPath = SDDIR + fileName.get(playVideoTs);
			Toast.makeText(getApplicationContext(), playVideoTs+"th video",
					Toast.LENGTH_LONG).show();
			playVideoTs++;

			try {
				mMediaPlayer.reset();
				mMediaPlayer.setDataSource(strPath);
				mMediaPlayer.prepare();
				mMediaPlayer.getMetadata();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {

			Log.d(TAG, "isPlaying:" + isPlaying);

			isPlaying = false;

		}

	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub

	}

	private void doCleanUp() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;
	}

	private void playVideo(Integer Media) {
		doCleanUp();
		try {

			switch (Media) {
			case LOCAL_VIDEO:
				/*
				 * TODO: Set the path variable to a local media file path.
				 */
				
				// path = "/sdcard/2.mp4.ts";
				// path="/sdcard/mccdash/segment-"+SEG_NUM+".ts";
				SEG_NUM++;

				if (fileName.containsKey(playVideoTs)) {
					path = SDDIR + fileName.get(playVideoTs);
					Toast.makeText(getApplicationContext(), playVideoTs+"th video",
							Toast.LENGTH_LONG).show();
					playVideoTs++;
					Log.v(TAG, "LOCAL_VIDEO:"+path);
				}
				
				
				//path="/sdcard/mccdash/00001_1.ts";

				if (path == "") {
					// Tell the user to provide a media file URL.
					Toast.makeText(
							MainActivity.this,
							"Please edit MediaPlayerDemo_Video Activity, "
									+ "and set the path variable to your media file path."
									+ " Your media file must be stored on sdcard.",
							Toast.LENGTH_LONG).show();
					return;
				}
				break;
			case STREAM_VIDEO:
				/*
				 * TODO: Set path variable to progressive streamable mp4 or 3gpp
				 * format URL. Http protocol should be used. Mediaplayer can
				 * only play "progressive streamable contents" which basically
				 * means: 1. the movie atom has to precede all the media data
				 * atoms. 2. The clip has to be reasonably interleaved.
				 */
				path = "http://10.105.39.110/hls-server/output.m3u8";
				if (path == "") {
					// Tell the user to provide a media file URL.
					Toast.makeText(
							MainActivity.this,
							"Please edit MediaPlayerDemo_Video Activity,"
									+ " and set the path variable to your media file URL.",
							Toast.LENGTH_LONG).show();
					return;
				}

				break;

			}

			// Create a new media player and set the listeners
			mMediaPlayer = new MediaPlayer(this);
			mMediaPlayer.setDataSource(path);
			mMediaPlayer.setDisplay(holder);
			mMediaPlayer.prepare();

			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnVideoSizeChangedListener(this);
			mMediaPlayer.getMetadata();
			setVolumeControlStream(AudioManager.STREAM_MUSIC);

		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
	}

	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	private void startVideoPlayback() {
		Log.v(TAG, "startVideoPlayback");
		holder.setFixedSize(mVideoWidth, mVideoHeight);
		mMediaPlayer.start();
	}

}
