package edu.bupt.mccdash.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import android.os.Environment;
import android.util.Log;

public class FileFactory {
	
	public static final String SDPath = Environment.getExternalStorageDirectory() + "/";
	private static boolean DEBUG=false;
	
	public static File creatSDDir(String dirName) {  
        File dir = new File(SDPath + dirName);  
        
       
        dir.mkdirs();  
        return dir;  
    }
	
	 public static boolean isFileExist(String fileName){  
	        File file = new File(SDPath + fileName);  
	        return file.exists();  
	    }  
	 
	 
	 public static File creatSDFile(String fileName) throws IOException {  
	        File file = new File(SDPath + fileName);  
	        file.createNewFile();  
	       
	        return file;  
	    }  
	
	 
	 
	 public static File RandomAccess(String path,String fileName,InputStream input,int startPoint){
		
         
		 
		 try {
			 File file = null;  
		    
			 
			 creatSDDir(path);  
	         file = creatSDFile(path + fileName);  
	       
			RandomAccessFile rf = new RandomAccessFile(file, "rw");
			rf.seek(startPoint);
			
			 int length;
	          
	         byte buffer [] = new byte[1024];  
	         while((length=input.read(buffer)) != -1){
	            	if(DEBUG)
	            	 Log.v("FileFactory", "length--"+length);
	            	rf.write(buffer, 0, length);
	                
	            }  
	         
	         rf.close();
	         input.close();
	         
	         return file;
	         
	         
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		 
		
	 }
	 
	 public static File write2SDFromInput(String path,String fileName,InputStream input){  
	        File file = null;  
	        OutputStream output = null;  
	        try{  
	            creatSDDir(path);  
	            file = creatSDFile(path + fileName);  
	            output = new FileOutputStream(file);  
	            int length;
	          
	            byte buffer [] = new byte[1024];  
	            while((length=input.read(buffer)) != -1){
	            	if(DEBUG)
	            	 Log.v("FileFactory", "length--"+length);
	            	output.write(buffer, 0, length);
	                
	            }  
	            output.flush();  
	        }  
	        catch(Exception e){  
	            e.printStackTrace();  
	        }  
	        finally{  
	            try{  
	                output.close();  
	            }  
	            catch(Exception e){  
	                e.printStackTrace();  
	            }  
	        }  
	        return file;  
	    }  
	 
	 
	 
	 public void writeToSDCard(){
		 
		 OutputStream output = null; 
	 }
	 
	 
	 public static void delete(File file) {  
	        if (file.isFile()) {  
	            file.delete();  
	            return;  
	        }  
	  
	        if(file.isDirectory()){  
	            File[] childFiles = file.listFiles();  
	            if (childFiles == null || childFiles.length == 0) {  
	                file.delete();  
	                return;  
	            }  
	      
	            for (int i = 0; i < childFiles.length; i++) {  
	                delete(childFiles[i]);  
	            }  
	            file.delete();  
	        }  
	    } 
	 
	 
	 public static byte[] getByte2(File file,int startPoint,int range) throws Exception{
		
		 byte[] buffer =new byte[range];
		 RandomAccessFile rf = new RandomAccessFile(file,"r");
		 rf.seek(startPoint);
		 rf.read(buffer, 0, range);
		 rf.close();
		 
		 return buffer;
		 
	 }
	 
	 public static byte[] getByte(File file) throws Exception  
	    {  
	        byte[] bytes = null;  
	        if(file!=null)  
	        {  
	            InputStream is = new FileInputStream(file);  
	            int length = (int) file.length();  
	            if(length>Integer.MAX_VALUE)   //当文件的长度超过了int的最大值  
	            {  
	                Log.v("FileFactory","this file is max ");  
	                return null;  
	            }  
	            bytes = new byte[length];  
	            int offset = 0;  
	            int numRead = 0;  
	            while(offset<bytes.length&&(numRead=is.read(bytes,offset,bytes.length-offset))>=0)  
	            {  
	                offset+=numRead;  
	            }  
	            //如果得到的字节长度和file实际的长度不一致就可能出错了  
	            if(offset<bytes.length)  
	            {  
	            	Log.v("FileFactory","file length is error");  
	                return null;  
	            }  
	            is.close();  
	        }  
	        return bytes;  
	    }  
	 

	
	
}
