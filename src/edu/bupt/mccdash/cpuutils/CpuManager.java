package edu.bupt.mccdash.cpuutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class CpuManager {

    // ��ȡCPU���Ƶ�ʣ���λKHZ��
    // "/system/bin/cat" ������
    // "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" �洢���Ƶ�ʵ��ļ���·��
    public static String getMaxCpuFreq() {
        String result = "";
        ProcessBuilder cmd;
        try {
                String[] args = { "/system/bin/cat","/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
                cmd = new ProcessBuilder(args);
                Process process = cmd.start();
                InputStream in = process.getInputStream();
                byte[] re = new byte[24];
                while (in.read(re) != -1) {
                    result = result + new String(re);
                }
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                result = "N/A";
        }
        return result.trim();
    }
    
    // ��ȡCPU��СƵ�ʣ���λKHZ��
    public static String getMinCpuFreq() {
        String result = "";
        ProcessBuilder cmd;
        try {
            String[] args = { "/system/bin/cat","/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq" };
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result = result + new String(re);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
            }
            return result.trim();
    }
    
    // ʵʱ��ȡCPU��ǰƵ�ʣ���λKHZ��
    public static String getCurCpuFreq() {
        String result = "N/A";
        try {
            FileReader fr = new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            result = text.trim();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // ��ȡCPU����
    public static String getCpuName() {
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            String[] array = text.split(":\\s+", 2);
            for (int i = 0; i < array.length; i++) {
            }
            return array[1];
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    //CPU����
    public static int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }      
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
//            Log.d(TAG, "CPU Count: "+files.length);
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Print exception
//            Log.d(TAG, "CPU Count: Failed.");
            e.printStackTrace();
            //Default to return 1 core
            return 1;
        }
    }
    
    
    
    
    public static String processCpu() throws IOException{
    	
    	String Result;
    	Process p=Runtime.getRuntime().exec("top -n 1");

    	BufferedReader br=new BufferedReader(new InputStreamReader   
    	(p.getInputStream ()));
    	StringBuilder tv = new StringBuilder();
    	while((Result=br.readLine())!=null)
    	{
    	 if(Result.trim().length()<1){
    	 continue;
    	 }else{
    	 String[] CPUusr = Result.split("%");
        	 tv.append("USER:"+CPUusr[0]+"\n");
        	 String[] CPUusage = CPUusr[0].split("User");
        	 String[] SYSusage = CPUusr[1].split("System");
        	 tv.append("CPU:"+CPUusage[1].trim()+" length:"+CPUusage[1].trim().length()+"\n");
        	 tv.append("SYS:"+SYSusage[1].trim()+" length:"+SYSusage[1].trim().length()+"\n");
        	 tv.append(Result+"\n");
        	 break;
    	 }
    	}
    	
    	return tv.toString();
    }
    
    
    
}
