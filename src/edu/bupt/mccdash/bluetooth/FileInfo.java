package edu.bupt.mccdash.bluetooth;

import java.io.File;

import android.util.Log;
import edu.bupt.mccdash.io.FileFactory;
import edu.bupt.mccdash.io.Logger;

public class FileInfo {
	public String fileName;
	public int fileStartPoint;
	public int fileLength;
	public String filePath;
	public byte[] fileContent;

	private static final String TAG = "MainActivity";

	public static FileInfo getFileInfoFromString(String readMessage) {
		int nameIndex = readMessage.indexOf("NAME:");
		int nameEndIndex = readMessage.indexOf("END NAME");
		String fileName = readMessage.substring(nameIndex + "NAME:".length(),
				nameEndIndex);

		// obtain the length:
		int lengthIndex = readMessage.indexOf("LENGTH:");
		int lengthEndIndex = readMessage.indexOf("END LENGTH");
		String fileStartLengthString = readMessage.substring(lengthIndex
				+ "LENGTH:".length(), lengthEndIndex);
		int fileStartLength = Integer.valueOf(fileStartLengthString);

		FileInfo fileInfo = new FileInfo();

		fileInfo = new FileInfo();
		fileInfo.fileName = fileName;
		fileInfo.fileStartPoint = fileStartLength;

		return fileInfo;

	}

	public static FileInfo getFile(String filePath, String outputFileName,int startPoint,int range) {
		FileInfo fileInfo = null;

		File file = new File(FileFactory.SDPath + filePath);
		try {
			byte[] fileByte = FileFactory.getByte2(file,startPoint,range);
			//Logger.i("fileByte.length" + fileByte.length);
			Log.v(TAG, "110.mp4 byte[]fileByte---" + fileByte.length);

			// qq一会再改
			// String test =Base64.encodeToString(fileByte, Base64.DEFAULT);
			// String test = new String(Base64.encode(fileByte,Base64.DEFAULT));
			// Log.v(TAG, "110.mp4 String---"+test.length());
			// Logger.i("test.length()"+test.length());
			// Logger.i(test);
			fileInfo = new FileInfo();
			fileInfo.fileContent = fileByte;
			fileInfo.fileName = outputFileName;
			fileInfo.fileStartPoint = startPoint;
			fileInfo.fileLength = range;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return fileInfo;
		}

	}

}
