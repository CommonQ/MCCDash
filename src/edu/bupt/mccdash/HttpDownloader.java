package edu.bupt.mccdash;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import edu.bupt.mccdash.io.FileFactory;
import android.os.Handler;
import android.util.Log;

public class HttpDownloader {
	private static final String TAG = "HttpDownloader";

	private OnHttpDownloadCompleteListener onHttpDownloadCompleteListener;

	public static final int BUFFERSIZE = 1049;

	// file type
	public static final int FILETYPE_UNKNOWN = 0;
	public static final int FILETYPE_M3U8 = 1;
	public static final int FILETYPE_TS = 2;

	public int testHello = 0;

	long current;
	int countLines = 0;
	int totalCountLines = 0;
	Handler mHandler;
	
	public static ArrayList<String> rateList = new ArrayList<String>();
	public  int currentTs=1;

	public HttpDownloader(Handler handler) {
		// TODO Auto-generated constructor stub
		mHandler = handler;
	}

	// public HttpDownloader() {
	// // TODO Auto-generated constructor stub
	//
	// }
	
	
	int decodeRange(HttpResponse httpResponse){
		int end=decodeEndPos(httpResponse);
		int start=decodeStartPos( httpResponse);
		int range=end-start+1;
		return range;
	}
	int decodeEndPos(HttpResponse httpResponse){
		
		Header[] rangeName=httpResponse.getHeaders("Content-Range");
		String rangeValue=rangeName[0].getValue();
		
		int indexStart = rangeValue.indexOf("-");
		int indexEnd   = rangeValue.indexOf("/");
		
		String end=rangeValue.substring(indexStart+1, indexEnd);
		int endInt=Integer.valueOf(end);
		
		
		
		
		
		return endInt;
		
	}
	int decodeStartPos(HttpResponse httpResponse) {

		try {
			Header[] header = httpResponse.getHeaders("Content-Range");
			String startPos = header[0].getValue();
			Log.v(TAG, "startPos:" + startPos);
			int dash = startPos.indexOf("-");
			String subString = startPos.substring(6, dash);
			Log.v(TAG, "startPos:" + subString);

			int start = Integer.valueOf(subString);

			return start;

		} catch (Exception e) {

			return -1;
		}

	}

	public void download(String destfile, String param,
			OnHttpDownloadCompleteListener l) {
		this.onHttpDownloadCompleteListener = l;
		new DownloadThread(destfile, param).start();
	}

	private class DownloadThread extends Thread {
		private int filetype;
		private String destfile;
		private String param;
		private String fileName;

		public DownloadThread(String destfile, String param) {
			this.filetype = getFileType(destfile);
			this.destfile = destfile;
			this.param = param;
			this.fileName = getFileName(destfile);
		}

		@Override
		public void run() {

//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			// HttpParams params = new BasicHttpParams();
			// HttpConnectionParams.setSoTimeout(params, 2000);
			//
			//
			countLines = 0;
			totalCountLines = 0;

			HttpClient httpClient = new DefaultHttpClient();
			HttpGet get = new HttpGet(destfile + param);
			// HttpConnectionParams.setSoTimeout(params, timeout);
			StringBuilder recv = new StringBuilder();
			// InputStream istream = null;
			try {
				Log.i(TAG, "HttpGet destfile: " + destfile);
				Log.i(TAG, "HttpGet param: " + param);
				HttpResponse httpResponse = httpClient.execute(get);

				Log.v(TAG, "waiting for responce");

				
				
				StatusLine stateLine = httpResponse.getStatusLine();
				int code = stateLine.getStatusCode();
				
				
				
				int range = decodeRange(httpResponse);
				
				Log.v(TAG, "TS RANGE--"+range);
				
				

				int startPoint=0;
				
				
				
				
				
				
				Header[] contentRange = httpResponse.getHeaders("Content-Range");

				String stringRange=contentRange[0].getValue();
				String[] contentLength=stringRange.split("/");
				long totalLength=Long.valueOf(contentLength[1]);
				long bitrate = totalLength / 1250;
				
				
				
				
				
				
				

				HttpEntity entity = httpResponse.getEntity();

				Log.v(TAG,
						"Entity getContentLength: " + entity.getContentLength()
								+ " code:" + code);

				if (entity != null) {
					Log.d(TAG, "recv from server");
					

					if (FILETYPE_TS == filetype) {
						Log.d(TAG, "FILETYPE_TS==filetype");
						Log.d(TAG, "filename-" + fileName);
						startPoint = decodeStartPos(httpResponse);

						Log.d(TAG, "startPoint-" + startPoint);
						if (startPoint == -1) {
							startPoint = 0;
						}
						
						InputStream istream = entity.getContent();

						FileFactory.RandomAccess("mccdash", "/" + fileName,
								istream, startPoint);

						// FileFactory.write2SDFromInput("/mccdash", ",
						// istream);

						istream.close();

						mHandler.obtainMessage(MainActivity.START_PLAY_VIDEO,
								0, 0, fileName).sendToTarget();

					} else {

						BufferedReader br = new BufferedReader(
								new InputStreamReader(entity.getContent()));
						String line = null;
						Log.d(TAG, "get start to read line!");

						// Timer timer = new Timer();
						// TimeOutTask task = new TimeOutTask(httpClient,
						// mHandler);
						// current= System.currentTimeMillis();
						// timer.schedule(task, 10000);

						while ((line = br.readLine()) != null) {

							recv.append(line);
							recv.append("\n");
						}

					}
					// task.cancel();
					// timer.cancel();
					// task=null;
					// timer=null;
				}
				
				rateList.add(bitrate+"");
				Log.d(TAG, "bitrate:"+bitrate+"kbps");
				// complete
				onHttpDownloadCompleteListener.OnHttpDownloadComplete(filetype,
						recv.toString(), fileName,startPoint,range);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			} finally {
				httpClient.getConnectionManager().shutdown();

			}

		}

		private int getFileType(String destfile) {
			if (destfile.toUpperCase().endsWith("MP4")) {
				Log.v(TAG, "filetype: " + "FILETYPE_TS");
				return FILETYPE_TS;
			} else if (destfile.toUpperCase().endsWith("M3U8")) {
				Log.v(TAG, "filetype: " + "M3U8");
				return FILETYPE_M3U8;
			} else {
				Log.v(TAG, "filetype: " + "UNKNOWN");
				return FILETYPE_UNKNOWN;
			}
		}

		private String getFileName(String destfile) {

			int index = destfile.lastIndexOf('/');
			String name = destfile.substring(index + 1);

			return name;

		}

//		class TimeOutTask extends TimerTask {
//
//			HttpClient mHttpClient;
//			Handler mHandler;
//
//			public TimeOutTask(HttpClient httpClient, Handler mHandler) {
//				// TODO Auto-generated constructor stub
//
//				mHttpClient = httpClient;
//				this.mHandler = mHandler;
//			}
//
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				long a = System.currentTimeMillis() - current;
//				mHttpClient.getConnectionManager().shutdown();
//				Log.v(TAG, "TIME OUT!! SHUT DOWN CONNECTION time: " + a
//						+ " current lines: " + countLines);
//
//				double ratio = (double) (countLines - 3)
//						/ (double) totalCountLines;
//				Log.w(TAG, "TIME OUT!! ratio:" + ratio);
//
//				mHandler.obtainMessage(MainActivity.MESSAGE_DELIVRY_RATIO, 0,
//						0, "ratio:" + ratio).sendToTarget();
//
//				// Message msg =
//				// mHandler.obtainMessage(MainActivity.MESSAGE_DELIVRY_RATIO);
//				// Bundle bundle = new Bundle();
//				// bundle.putString(MainActivity.TOAST,
//				// "Device connection was lost");
//				// msg.setData(bundle);
//				// mHandler.sendMessage(msg);
//
//			}
//
//		}
	}
}
