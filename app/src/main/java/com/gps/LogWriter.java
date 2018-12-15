/*
 * Copyright (C) 2014 Petrolr LLC, a Colorado limited liability company
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

/* 
 * Written by the Petrolr team in 2014. Based on the Android SDK Bluetooth Chat Example... matthew.helm@gmail.com
 */


package com.gps;

import android.annotation.SuppressLint;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//public String timestampOld = "";

public class LogWriter {

	protected static void write_info(final String logmsg) {
		
		// ++++ Fire off a thread to write info to file

		Thread w_thread = new Thread() {
			public void run() {
				File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/gpsdata");
				myFilesDir.mkdirs();


                //Integer timestampDiv = Integer.valueOf(timestamp()) - Integer.valueOf(timestampOld);
				//String dataline = (timestamp() + ";" + logmsg + "\n");
                String dataline = (timestamp() + logmsg);
                //timestampOld = timestamp();


                //File myfile = new File(myFilesDir + "/" + "aicv_Log" + sDate() + ".txt");
				File myfile = new File(myFilesDir + "/" + timestamp() + "trkobd" + ".gpx");
				if(myfile.exists() == true){
					try {
						FileWriter write = new FileWriter(myfile, true);
						write.append(logmsg);
						//read_ct++;
						write.close();		
					}catch (IOException e){
						
					}
				}else{ //make a new file since we apparently need one
					try {
						FileWriter write = new FileWriter(myfile, true);
						//	write.append(header);
						write.append(dataline);
						//read_ct++;
						write.close();		
					}catch (IOException e){
						    		
					}
	
				}
			}
		};
		w_thread.start();
	}
	
	/*
	protected static long timestamp(){
        long timestamp = System.currentTimeMillis();
        return timestamp;
	}
	*/

    protected static String timestamp(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateformat = new SimpleDateFormat("HH:mm:ss");
        String datetime = dateformat.format(c.getTime());
        return datetime;
     }


    @SuppressLint("SimpleDateFormat")
	protected String sDate(){
		//Date date = new Date();
		//Date date = new Date(location.getTime());
		//SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		//TimeValue = timeFormat.format(date);

		//SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		//String timeStamp = new SimpleDateFormat("ddMMyy_HH:mm").format(new Date());
		//SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
		//java.util.Date date= new java.util.Date();
		//String sDate = sdf.format(date.getTime());
		DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String sDate = sdf.format(Calendar.getInstance().getTime());
		//String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
		//String sDate = sdf.format(date);
		//Toast.makeText(getBaseContext(), "SD card not available.",	Toast.LENGTH_SHORT).show();

		return sDate;
	}
	
}
