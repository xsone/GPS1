/*
 * Copyright (C) 2017-2018 Jack Cop LLC, St. Nicolaasga

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
 * 
 */

/*
Configure the COM port for 115.2kbps, and connect the interface to your computer and the vehicles diagnostic port. If you did the steps in this order, you will see the device identify itself and print the command prompt:
ELM327 v1.3a
>

Set the Protocol
Lets set the protocol to AUTO, which means that you want the interface to automatically detect the protocol when you send the first OBD request. To do this, enter the �AT SP 0� command:
>AT SP 0
OK

To verify the protocol, enter the AT DP command (Display Protocol):
>AT DP
AUTO

Get RPM
Now it is time to send our first OBD request. Real-time parameters are accessed through Mode 1 (also called Service $01), and each parameter has a Parameter ID, or PID for short. RPMs PID is 0C, so we must tell the interface to send 010C:
>010C
SEARCHING: OK
41 0C 0F A0
The reply contains two bytes that identify it as a response to Mode 1, PID 0C request (41 0C), and two more bytes with the encoded RPM value (1/4 RPM per bit). To get the actual RPM value, convert the hex number to decimal, and divide it by four:
0x0FA0 = 4000
4000 / 4 = 1000 rpm

Get Vehicle Speed
Speeds PID is 0D:
>010D
41 0D FF
To get the speed, simply convert the value to decimal:
0xFF = 255 km/h

Get Engine Load
Engine load is PID 04, and we need to first divide the response by 255, and multiply it by one hundred percent:
>0104
41 04 7F
Translated:
0x7F = 127
(127 / 255) * 100 = 50%

Get Coolant Temperature
Coolant temperature (PID 05) is reported in degrees, and is obtained by subtracting 40 from the value:
>0105
41 05 64
0x64 = 100
100 - 40 = 60C

*/



package com.gps;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class OBDscanMainActivity extends Activity {
	private static final String TAG = "OBDII Terminal";
	private ListView mConversationView;
	
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private static StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	static BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private static BluetoothChatService mChatService = null;
	
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	
	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	

	//public String[] readObdValue = new String[16];
	//public int []readHexBytes = new int[4];

	//public byte[] obdByteValue = {(byte) 0x41, (byte) 0x0C, (byte) 0x64, (byte) 0xA0}; //COOLANT 100 C
	
	//public byte[] readBuf = {(byte) 0x41, (byte) 0x0C, (byte) 0x64, (byte) 0xA0}; //COOLANT 100 C
	//public byte[] readBuf = {(byte) 0x41, (byte) 0x0C, (byte) 0x32, (byte) 0x14}; //SPEED 100 KMh
	public byte[] obdByteValue = new byte[5];
	//public String []readStringValues = new String[16]; 
	//public byte[] obdByteValue;
	String readMessageTest = "";
	public String readMessage = "";
	public String readValue = "";
	public String[] ObdBytes;
	public String[] readMessageTemp;
	
	public String btMacAddress = "00:00:00:00:00:00";
	public BluetoothDevice btDevice;
	public BluetoothSocket bluetoothSocket;
	boolean btSecure;
	//public String[] bytes = new String[16]; 
	//public String[] bytes = new String[16];
	//public String[] bytes;
	//test[3] = (byte)  newTest3;
	  
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	
	public enum OBDcommand { rpm, speed, coolant };

	EditText command_line;
	Button send_command;
	Button button_obd_test;
	Button button_obd_rpm;
	Button button_obd_speed;
	Button button_obd_coolant;
	
	public boolean btnObdTest = false;
	
	TextView msgWindow;
	TextView tvObdValue;
	TextView tvObdPid;
	TextView tvObdData;
	TextView tvObdMsg;
	
	public int intRpm = 0;
	public int intSpeed = 0;
	public int intCoolant = 0;
	public int PID = 0;
	public int value = 0;
	public int returnValue = 0;
	
	public String strCoolant = "0105";
	public String strSpeed = "010D";
	public String strRpm = "010C";
	
	public int Teller = 0;
	public int msgTeller = 0;
	public Timer autoUpdate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	//	setContentView(R.layout.terminal_layout);
	//	tvObdValue = (TextView) findViewById(R.id.tvObdValue);
	//	tvObdPid = (TextView) findViewById(R.id.tvObdPid);
	//	tvObdData = (TextView) findViewById(R.id.tvObdData);
	//	msgWindow = (TextView) findViewById(R.id.msgWindow);
	//	msgWindow.setMovementMethod(new ScrollingMovementMethod());
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); //keyboard doesnt popup

		final ActionBar actionBar = getActionBar();
		//  actionBar.setDisplayOptions(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Log.d(TAG, "Adapter: " + mBluetoothAdapter);
		
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}	

	//Delay timer
		public void Delay(int Seconds){
		    long Time = 0;
		    Time = System.currentTimeMillis();
		    while(System.currentTimeMillis() < Time+(Seconds*1000));
		}
	
	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}	

	//Auto run obd commands
	public void ObdRun() {
		autoUpdate = new Timer();
		autoUpdate.schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
					switch(Teller)
					 {
						case 0:	msgWindow.append("\n" + "ATZ: " + Teller + "\n");
								sendMessage("ATZ" + "\r"); //Test geeft OK
								break;
						case 1:	msgWindow.append("\n" + "ATE1: " + Teller + "\n");
								sendMessage("ATE1" + "\r"); //PID
								break;
						case 2:	msgWindow.append("\n" + "AT@1: " + Teller + "\n");
								sendMessage("AT@1" + "\r"); //Protocol geeft AUTO
								break;
						case 3:	msgWindow.append("\n" + "ATSP0: " + Teller + "\n");
								sendMessage("ATSP0" + "\r"); //Test geeft ?
								break;
						case 4:	msgWindow.append("\n" + "ATDP: " + Teller + "\n");
								sendMessage("ATDP" + "\r"); //Buffer Dump
								break;
						case 5:	msgWindow.append("\n" + "ATDPN: " + Teller + "\n");
								sendMessage("ATDPN" + "\r"); //RPM
								break;
						case 6:	msgWindow.append("\n" + "ATBD: " + Teller + "\n");
								sendMessage("ATBD" + "\r"); //SPEED
								break;
						case 7:	msgWindow.append("\n" + "ATRD: " + Teller + "\n");
								sendMessage("ATRD" + "\r"); //SPEED
								break;
						case 8:	msgWindow.append("\n" + "ATRV: " + Teller + "\n");
								sendMessage("ATRV" + "\r"); //SPEED
								break;		
						case 9:	msgWindow.append("\n" + "RPM: " + Teller + "\n");
								sendMessage(strRpm + "\r"); //RPM
								break;		
						case 10:msgWindow.append("\n" + "SPEED: " + Teller + "\n");
								sendMessage(strSpeed + "\r"); //SPEED
								break;		
						case 11:msgWindow.append("\n" + "0100: " + Teller + "\n");
								sendMessage("0100" + "\r"); //SPEED
								break;		
						case 12:msgWindow.append("\n" + "0104: " + Teller + "\n");
								sendMessage("0104" + "\r"); //SPEED
								break;		
						case 13:msgWindow.append("\n" + "COOLANT" + ": " + Teller + "\n");
								sendMessage(strCoolant + "\r"); //Coolant
								break;		
						case 14:msgWindow.append("\n" + "ATCS: " + Teller + "\n");
								sendMessage("ATCS" + "\r"); //SPEED
								break;		
						case 15:msgWindow.append("\n" + "ATCAF1: " + Teller + "\n");
								sendMessage("ATCAF1" + "\r"); //SPEED
								break;		
						case 16:msgWindow.append("\n" + "ATCFC1: " + Teller + "\n");
								sendMessage("ATCFC1" + "\r"); //SPEED
								break;		
						case 17:msgWindow.append("\n" + "ATAL: " + Teller + "\n");
								sendMessage("ATAL" + "\r"); //SPEED
								break;		
						case 18:msgWindow.append("\n" + "ATMA: " + Teller + "\n");
								sendMessage("ATMA" + "\r"); //SPEED
								break;
						default: Teller = 0;
								break;
						}
					ObdBytes = null;
					readValue = "";
					Teller++;
					if (Teller >= 19) Teller = 0;
					}
				});
			}
		}, 0, 5000); // updates each 1 secs
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//stopService(new Intent(this, BluetoothChatService.class)); //stop BT connection
		Toast.makeText(this, "Destroy: BT Service Destroyed", Toast.LENGTH_LONG).show();
		//mChatService.connect(btDevice, true); //BLE
		autoUpdate.cancel();
		finish();
		System.exit(0);
		//if (VERBOSE) Log.v(TAG, "- ON DESTROY -");
	}
	
	
	public void setupChat() {
		Log.d(TAG, "setupChat()");
		mChatService = new BluetoothChatService(this, mHandler);
		mOutStringBuffer = new StringBuffer("");   
	}	
	
	public void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);
			//LogWriter.write_info("\n" + "Cmd: " + message);
			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			//obdByteValue = null;
			//mOutEditText.setText(mOutStringBuffer);
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.d("Terminal", "onActivityResult...");
		
		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)connectDevice(data, true);
				break;
			case REQUEST_CONNECT_DEVICE_INSECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)connectDevice(data, false);
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a chat session
					setupChat();
				} else {
					// User did not enable Bluetooth or an error occurred
					Log.d(TAG, "BT not enabled");
					Toast.makeText(this, "BT NOT ENABLED", Toast.LENGTH_SHORT).show();
					finish();
				}
		}
	}  
	
	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		btMacAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(btMacAddress);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}
	
	private final Handler mHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case MESSAGE_STATE_CHANGE:
					
					switch (msg.arg1) {
						case BluetoothChatService.STATE_CONNECTED:
							setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
							mConversationArrayAdapter.clear();
							//onConnect();
							break;
						case BluetoothChatService.STATE_CONNECTING:
							setStatus(R.string.title_connecting);
							break;
						case BluetoothChatService.STATE_LISTEN:
						case BluetoothChatService.STATE_NONE:
							setStatus(R.string.title_not_connected);
							break;
					}
					break;
				case MESSAGE_WRITE:
					break;
				case MESSAGE_READ:
					//StringBuilder res = new StringBuilder();
					byte[] readBuf = (byte[]) msg.obj;
					//obdByteValue = (byte[]) msg.obj; //maak een bytearray van de valide bytes in de buffer
					//String readMessage = new String(obdByteValue, 0, msg.arg1);
					readMessage = new String(readBuf, 0, msg.arg1);
					//Toast.makeText(getApplicationContext(), readMessage,Toast.LENGTH_SHORT).show();
					//String []bytes = readMessage.trim().split(" ");
					readValue = readValue + readMessage;
					if(readValue.endsWith(">"))
					 {	
					  ObdBytes = readValue.trim().split(" ");
					  msgWindow.append(readValue + "\n"); // test alles uitlezen;
					  ObdDecode();
					 }
					//for (int x = 0; x < bytes.length; x++) {
					//	  msgWindow.append("Bytes " + x + ":" + bytes[x] + "\n");
						  //msgWindow.append("Bytes 1: " + bytes[1] + "\n");
							
						  /*
						  if(bytes[1].length()==2)
							 { 
							   if(bytes[1].trim().equals(strCoolant)) intCoolant = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							   //else intCoolant = 0;
							   tvOBDmsg.setText("OBD_Coolant: " + String.valueOf(intCoolant-40) + " C");
							   //tvCoolant.setText(intCoolant-40 + " C");
							   if(bytes[1].trim().equals(strSpeed)) intSpeed = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							   //else intSpeed = 0;
							   tvOBDmsg.setText("OBD_Speed  : " + String.valueOf(intSpeed/4) + " Kmh");
							   if(bytes[1].trim().equals(strRpm)) intRpm = Integer.valueOf(bytes[2].trim(), 16)+ Integer.valueOf(bytes[3].trim(), 16); //PID waarde
							   //else intRpm = 0;
							   //tvRpm.setText(intRpm + " RPM");
							 }
							*/ 
					//} // end for
			
					//LogWriter.write_info(readMessage);
						//readMessage = new String(obdByteValue, 0, msg.arg1);
						//for (int x = 0; x < readMessage.length(); x++) {
						//	readMessageTest = readMessage.getBytes();
						//}	
						//msgWindow.append(String.valueOf(obdByteValue)+ "\n"); // door 4 delen
					// }
					//	obdByteValue = readBuf;
					//msgWindow.append(String.valueOf(obdByteValue)); // door 4 delen
					//msgWindow.append(obdByteValue); // door 4 delen
					
					//msgWindow.append("\n" + "|" + readBuf + "|" + "\n"); // door 4 delen
					//msgWindow.append("#" + readMessage + "|" + "\n"); // test alles uitlezen
					readMessage = "";
					readBuf = null;
					//LogWriter.write_info(readMessage);
					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(), "Connected to "
							+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
							Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};		  

	
	public int ObdDecode()
	{
	  ObdBytes = readValue.trim().split(" ");
	  //msgWindow.append("*" + ObdBytes[0].substring(0,4) + "*" + "\n");
	  if(ObdBytes[0].substring(0,4).equals(strCoolant))
	   {   
		  //msgWindow.append("$" + ObdBytes[2] + "$" + "\n");
		  intCoolant = Integer.parseInt(ObdBytes[2].trim(), 16) - 40; //PID waarde
		  msgWindow.append("OBD_COOLANT" + " | " + ObdBytes[0].substring(0,4) + " | " + String.valueOf(intCoolant) + " | " + "\n");
		  tvObdValue.setText("OBD_COOLANT");
		  tvObdPid.setText(ObdBytes[0].substring(0,4));
		  tvObdData.setText(String.valueOf(intCoolant));
		  returnValue = intCoolant;
	   } 
	  else
	   {	  
	    intCoolant = 0;
	    //msgWindow.append("|" + String.valueOf(intCoolant) + "|" + "\n");
	   } 

	  if(ObdBytes[0].substring(0,4).equals(strSpeed))
	   {
		  //msgWindow.append("$" + ObdBytes[2] + "$" + "\n");
		  intSpeed = (Integer.parseInt(ObdBytes[2].trim(), 16)) /4; //PID waarde
		  msgWindow.append("OBD_SPEED" + " | " + ObdBytes[0].substring(0,4) + " | " + String.valueOf(intSpeed) + " | " + "\n");
		  tvObdValue.setText("OBD_SPEED");
		  tvObdPid.setText(ObdBytes[0].substring(0,4));
		  tvObdData.setText(String.valueOf(intSpeed));
		  returnValue = intSpeed;
	   } 
	  else
	   {	  
		intSpeed = 0;
		returnValue = intSpeed;
		//msgWindow.append("|" + String.valueOf(intSpeed) + "|" + "\n");
	   }	
	
	  if(ObdBytes[0].substring(0,4).equals(strRpm))
       {  
		  //msgWindow.append("$" + ObdBytes[2] + "$" + ObdBytes[3] + "$" + "\n");
		  intRpm = Integer.parseInt(ObdBytes[2].trim(), 16)+ Integer.valueOf(ObdBytes[3].trim(), 16); //PID waarde
		  msgWindow.append("OBD_RPM" + "|" + ObdBytes[0].substring(0,4) + "|" + String.valueOf(intRpm) + "|" + "\n");
		  tvObdValue.setText("OBD_RPM");
		  tvObdPid.setText(ObdBytes[0].substring(0,4));
		  tvObdData.setText(String.valueOf(intRpm));
		  returnValue = intRpm;
	   }	
	  else 
	   {  
		intRpm = 0;
		returnValue = intRpm;
        //msgWindow.append("|" + String.valueOf(intRpm) + "|" + "\n");
	   }
	 ObdBytes = null;
	 readValue = "";
	 return returnValue;
	}
}