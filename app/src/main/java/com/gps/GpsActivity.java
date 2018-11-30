package com.gps;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class GpsActivity extends Activity {
	private static final String TAG = "OBD | GPS ";
	/**
	 * The LocationListener shows the most recent data with every call of onLocationChanged()
	 */
	private final LocationListener locationListener;
	public TextView tvMsgWindow;
	public TextView tvLong;
	public TextView tvLat;
	public TextView tvGpsSpeed;
	public TextView tvObdSpeed;
	public TextView tvObdRpm;
	public TextView tvObdCoolant;
	public TextView tvObdEngineLoad;
	public TextView tvObdTrottlePos;
	public TextView tvObdAbsLoad;
	public TextView tvObdAmbientAir;
	public TextView tvObdOilTemp;
	public TextView tvObdFuelLevel;
	public TextView tvAlt;
	public TextView tvDate;
	public TextView tvTime;
	public TextView tvProvider;
	public String Filename;
	public String ProviderName;
	public String[] providers = {"network", "gps", "passive"};
	public static Criteria criteria;
	public Location location;
	public LocationProvider provider;
	public Integer Teller = 10;
	public TextView tvGpsfile;
	public Integer logIntervalInt = 10;
	
	public Boolean isGPSEnabled = false;
	public Boolean isNetworkEnabled = false;
	
	public String GpsSpeedValue = "0";
	public String logIntervalString = "10";

	public int intObdRpm = 0;
	public int intObdSpeed = 0;
	public int intObdCoolant = 0;
	public String timeStamp = "01-01-2018 00:00:00";
	public int intObdEngineLoad = 0;
	public int intObdTrottlePos = 0;
	public int intObdAbsLoad = 0;
	public int intObdAmbientAir = 0;
	public int intObdOilTemp = 0;
	public int intObdFuelLevel = 0;
	public int intObdPID = 0;
	public int value = 0;
	public String strObdCoolant = "05";
	public String strObdSpeed = "0D";
	public String strObdRpm = "0C";
	public String strObdEngineLoad = "04";
	public String strObdTrottlePos = "11";
	public String strObdAbsLoad = "43";
	public String strObdAmbientAir = "46";
	public String strObdOilTemp = "5C";

	public String[] readStringValuesTest;
	public String []readStringValues = new String[16]; 
	
	String readMessageTest = "";
	public String readMessage = "";
	public String readValue = "";
	public String[] ObdBytes;
	public String[] readMessageTemp;
	
	public Timer autoUpdate;
	
	FileWriter writer;
    BufferedWriter out;
	File root;
	public String strObdFuelLevel = "2F";

    //public String btMacAddress = "00:12:12:04:10:57";
    public String btMacAddress = "00:00:00:00:00:00";
	public BluetoothDevice btDevice;
	public BluetoothSocket bluetoothSocket;
	boolean btSecure;
   
 // Name of the connected device
 	private String mConnectedDeviceName = null;
 	// Array adapter for the conversation thread
 	private ArrayAdapter<String> mConversationArrayAdapter;
 	// String buffer for outgoing messages
 	private static StringBuffer mOutStringBuffer;
	// String buffer for incoming messages
 	private static StringBuffer mInStringBuffer;
 	// Local Bluetooth adapter
 	static BluetoothAdapter mBluetoothAdapter = null;
 	// Member object for the chat services
 	private static BluetoothChatService mChatService = null;
 	static DeviceListActivity mBTMacAddress = null;
 	
 // Intent request codes
 	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
 	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
 	private static final int REQUEST_ENABLE_BT = 3;
 	
 	// Message types sent from the BluetoothChatService Handler
 	public static final int MESSAGE_STATE_CHANGE = 1;
 	public static final int MESSAGE_READ = 2;
 	public static final int MESSAGE_WRITE = 3;
 	public static final int MESSAGE_DEVICE_NAME = 4;
 	public static final int MESSAGE_TOAST = 5;
 	public static final int MESSAGE_ERROR = 6;
 	public static final int MESSAGE_LOST = 7;
 	
 	// Key names received from the BluetoothChatService Handler
 	public static final String DEVICE_NAME = "device_name";
 	public static final String TOAST = "toast";
 	
 	// Member object for the OBDscanMainActivity.java
 	private static OBDscanMainActivity mObdService = null;
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MESSAGE_STATE_CHANGE:

					switch (msg.arg1) {
						case BluetoothChatService.STATE_CONNECTED:
							//setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
							mConversationArrayAdapter.clear();
							//ObdRun();
							break;
						case BluetoothChatService.STATE_CONNECTING:
							setStatus(R.string.title_connecting);
							break;
						case BluetoothChatService.STATE_LISTEN:
						case BluetoothChatService.STATE_NONE:
							setStatus(R.string.title_not_connected);
							//BTCONNECT = 2;
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
					String readMessage = new String(readBuf, 0, msg.arg1);
					tvMsgWindow.append(readMessage);
					String[] bytes = readMessage.trim().split(" ");
					for (int x = 1; x < bytes.length; x++) {
						//msgWindow.append("Bytes " + x + ":" + bytes[x] + "\n");
						//msgWindow.append("Bytes 1: " + bytes[1] + "\n");
						if (bytes[1].length() == 2) {
							if (bytes[1].trim().equals(strObdSpeed))
								intObdSpeed = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							tvObdSpeed.setText(intObdSpeed + " Kmh");

							if (bytes[1].trim().equals(strObdRpm))
								intObdRpm = (Integer.valueOf(bytes[2].trim(), 16) + Integer.valueOf(bytes[3].trim(), 16)) / 4; //PID waarde
							tvObdRpm.setText(intObdRpm + " RPM");

							if (bytes[1].trim().equals(strObdCoolant))
								intObdCoolant = (Integer.valueOf(bytes[2].trim(), 16)) - 40; //PID waarde
							tvObdCoolant.setText(intObdCoolant + " C");

							if (bytes[1].trim().equals(strObdEngineLoad))
								intObdEngineLoad = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							tvObdEngineLoad.setText(intObdEngineLoad + " %");

							if (bytes[1].trim().equals(strObdTrottlePos))
								intObdTrottlePos = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							tvObdTrottlePos.setText(intObdTrottlePos + " %");

							if (bytes[1].trim().equals(strObdAbsLoad))
								intObdAbsLoad = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							tvObdAbsLoad.setText(intObdAbsLoad + " %");

							if (bytes[1].trim().equals(strObdAmbientAir))
								intObdAmbientAir = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							tvObdAmbientAir.setText(intObdAmbientAir + " C");

							if (bytes[1].trim().equals(strObdOilTemp))
								intObdOilTemp = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							tvObdOilTemp.setText(intObdOilTemp + " C");

							if (bytes[1].trim().equals(strObdFuelLevel))
								intObdFuelLevel = Integer.valueOf(bytes[2].trim(), 16); //PID waarde
							tvObdFuelLevel.setText(intObdFuelLevel + " %");
						}
					} // end for
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
				case MESSAGE_ERROR:
					break;
				case MESSAGE_LOST:
					//	    if(Constant.DEBUG)  Log.d(TAG, "Connection lost");
					//	    stopBTService(); //stop the Bluetooth services
					//	    Thread.sleep(500);
					//	       setupBluetoothService(); //re initialize the bluetoothconnection
					break;
			}
		}
	};
	public File gpxFile;

	{
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {

    	 /*
    		  setContentView(R.layout.main);
              Provider = (TextView) findViewById(R.id.Provider);
              Lat = (TextView) findViewById(R.id.Lat);
              Long = (TextView) findViewById(R.id.Lon);
    		  GpsSpeed = (TextView) findViewById(R.id.GpsSpeed);
    		  //txtObdSpeed = (TextView) findViewById(R.id.ObdSpeed);
    		  //txtObdRpm = (TextView) findViewById(R.id.ObdRpm);
    		  //txtObdCoolant = (TextView) findViewById(R.id.ObdCoolant);
    		  Alt = (TextView) findViewById(R.id.Alt);
    		  Date = (TextView) findViewById(R.id.Date);
    		  //Time = (TextView) findViewById(R.id.Time);
    		  //Gpsfile = (TextView) findViewById(R.id.File);

    		  Provider.setText(ProviderName);
    		  Gpsfile.setText(Filename);
    		  //txtObdSpeed.setText(strObdSpeed + "  KMh");
    		  //txtObdRpm.setText(strObdRpm + "  RPM");
    		  //txtObdCoolant.setText(strObdCoolant + " �C");
    	*/

				if (location != null) {
					tvLong.setText(Double.toString(location.getLongitude()));
					tvLat.setText(Double.toString(location.getLatitude()));
					tvAlt.setText(String.format("%.2f", location.getAltitude()));
					GpsSpeedValue = Integer.toString((int) (location.getSpeed() * 3600) / 1000); //omwerken van m/s naar km/h
					tvGpsSpeed.setText(GpsSpeedValue + "  KMh");
					//Speed.setText(Float.toString((location.getSpeed()*3600)/1000)); //omwerken van m/s naar km/h
					tvObdSpeed.setText(intObdSpeed / 4 + " Kmh");
					tvObdRpm.setText(intObdRpm + " RPM");
					tvObdCoolant.setText(intObdCoolant - 40 + " C");
					tvObdEngineLoad.setText(intObdEngineLoad + " %");
					tvObdTrottlePos.setText(intObdTrottlePos + " %");
					tvObdAbsLoad.setText(intObdAbsLoad + " %");
					tvObdAmbientAir.setText(intObdAmbientAir + " C");
					tvObdOilTemp.setText(intObdOilTemp + " C");
					tvObdFuelLevel.setText(intObdFuelLevel + " %");

				} else {
					Toast.makeText(getBaseContext(), "Couldn't retrieve user location",
							Toast.LENGTH_SHORT).show();
				}

				String state = Environment.getExternalStorageState();
				if (Environment.MEDIA_MOUNTED.equals(state)) {
					try {
						if (root.canWrite()) {
							out.write("<wpt lat='" + tvLat.getText() + "' lon='" + tvLong.getText() + "'>\n" +
									"<name>" + GpsSpeedValue + "kmh|" + intObdSpeed + "kmh|" + intObdRpm + "rpm|" + intObdCoolant + "deg|" + intObdTrottlePos + "%" + "</name>\n" +
									"<time>" + timeStamp + "</time>\n" +
									"<sym>" + "triangle" + "</sym>\n" +
									"</wpt>\n");
							//"<desc>" + Lat.=51.295094, Long.=6.790674, Alt.=39.000000m, Speed=3Km/h, Course=45deg. + "</desc>\n" +
							//"<sym>" + Scenic Area + "</sym>\n" +
							//"<name>" + GpsSpeedValue + strObdRpm + ObdCoolant + "</name>\n" +
							//"<ele>" + Alt.getText() + "</ele>\n" +
							//"<time> + Clock.getText() + </time>\n" +//
							//"<speed>" + tvGpsSpeed.getText() + "</speed>\n</wpt>\n");
						} else {
							Toast.makeText(getBaseContext(), "Impossible to write file.",
									Toast.LENGTH_SHORT).show();

						}
					} catch (IOException e) {
						Toast.makeText(getBaseContext(), "SD card not available.",
								Toast.LENGTH_SHORT).show();
					}
				}
			}

			public void onProviderDisabled(String provider) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
		};
    }

	@Override
	/** onCreate() is called at start of activity */
    public void onCreate(Bundle savedInstanceState) {
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		tvMsgWindow = (TextView) findViewById(R.id.msgWindow);
		tvMsgWindow.setMovementMethod(new ScrollingMovementMethod());

        SharedPreferences myPreference=PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor myPreferenceEditor = myPreference.edit();
        logIntervalString =  myPreference.getString("loginterval",logIntervalString);
		//myPreferenceEditor.putString("logInterval", logIntervalString).commit();
	    logIntervalInt = Integer.parseInt(logIntervalString);
		btMacAddress = myPreference.getString("btMacAddress", btMacAddress);
	    myPreferenceEditor.putString("btMacAddress", btMacAddress).commit();//test
		Toast.makeText(this, "Loginterval: " + logIntervalString, Toast.LENGTH_LONG).show(); //test
		Toast.makeText(this, "BT-Address: " + btMacAddress, Toast.LENGTH_LONG).show(); //test

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Log.d(TAG, "Adapter: " + mBluetoothAdapter);
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// If BT is not on, request that it be enabled.
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		// Otherwise, setup the chat session
		} else {
			//Toast.makeText(this, "BT-Address IF Test: " + btMacAddress, Toast.LENGTH_LONG).show(); //test
			//if (mChatService == null) com.gps.OBDscanMainActivity.setupChat();
			if (mChatService == null) setupChat();
		}
		btDevice = mBluetoothAdapter.getRemoteDevice(btMacAddress);
		mChatService.connect(btDevice, btSecure);

		try {
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting Network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isGPSEnabled) {
            	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
                Toast.makeText(getBaseContext(), "GPS Enabled", Toast.LENGTH_SHORT).show();
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                //60000, // 1min
                                //30000, // 1/2min
                                logIntervalInt*1000,
                                //1,   // 10m
                                0,
                                locationListener);
                        if (location != null) {
							tvLong.setText(Double.toString(location.getLongitude()));
							tvLat.setText(Double.toString(location.getLatitude()));
                        }
                    }
            	// no network provider is enabled
            }
            else {
                //this.canGetLocation = true;
                if (isNetworkEnabled) {
					if (location == null) {
						locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
						Toast.makeText(getBaseContext(), "Network Enabled", Toast.LENGTH_SHORT).show();
						if (locationManager != null) {
							location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
							provider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
							locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
									//60000, // 1min
									//30000, // 1/2 min (werkte)
									logIntervalInt * 1000,
									//1,   // 10m
									0,
									locationListener);
							if (location != null) {
								tvLong.setText(Double.toString(location.getLongitude()));
								tvLat.setText(Double.toString(location.getLatitude()));
							}
						}
				  }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        super.onCreate(savedInstanceState); //Met super.onCreate wordt onderstaande code bovenop/naast de eerste onCreate uitgevoerd.
        setContentView(R.layout.main);
        Criteria locationCriteria = new Criteria();
		locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        ProviderName = provider.getName();
        Date dateTime = new Date();
		timeStamp = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss").format(new Date());
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        root = Environment.getExternalStorageDirectory();
		//Filename = dateFormat.format(dateTime) + timeFormat.format(dateTime)+ ".gpx";
		Filename = timeStamp + ".gpx";
		//File gpxFile = new File(root, Filename);
		gpxFile = new File(root, Filename);

		tvDate = (TextView) findViewById(R.id.Date);
		//Time = (TextView) findViewById(R.id.Time);
		//Gpsfile = (TextView) findViewById(R.id.File);
		tvProvider = (TextView) findViewById(R.id.Provider);
		tvLat = (TextView) findViewById(R.id.Lat);
		tvLong = (TextView) findViewById(R.id.Lon);
		tvGpsSpeed = (TextView) findViewById(R.id.GpsSpeed);
		tvObdSpeed = (TextView) findViewById(R.id.ObdSpeed);
		tvObdRpm = (TextView) findViewById(R.id.ObdRpm);
		tvObdCoolant = (TextView) findViewById(R.id.ObdCoolant);
		tvObdEngineLoad = (TextView) findViewById(R.id.ObdEngineLoad);
		tvObdTrottlePos = (TextView) findViewById(R.id.ObdTrottlePos);
		tvObdAbsLoad = (TextView) findViewById(R.id.ObdAbsLoad);
		tvObdAmbientAir = (TextView) findViewById(R.id.ObdAmbientAir);
		tvObdOilTemp = (TextView) findViewById(R.id.ObdOilTemp);
		tvObdFuelLevel = (TextView) findViewById(R.id.ObdFuelLevel);
		tvAlt = (TextView) findViewById(R.id.Alt);
		tvMsgWindow = (TextView) findViewById(R.id.msgWindow);

		//Date date = new Date(location.getTime());
		//SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		//TimeValue = timeFormat.format(date);
		//Time.setText(timeFormat.format(date)); //geeft crash
		//DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
		//DateFormat dateFormat = new SimpleDateFormat("YY/MM/DD");
		//tvDate.setText(dateFormat.format(dateTime));  //geeft crash
		tvDate.setText(timeStamp);  //geeft crash


		//String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(location.getTime());
		//String time = new java.text.SimpleTimeFormat("HH:mm:ss.SSS").format(location.getTime());
		//Time.setText(dateFormat.format(time));
		tvProvider.setText(ProviderName);
		//  Gpsfile.setText(Filename);
		//tvObdSpeed.setText(intObdSpeed/4 + " Kmh");
		//tvObdRpm.setText(intObdRpm + " RPM");
		//tvObdCoolant.setText(intObdCoolant-40 + " C");

	    try {
			writer = new FileWriter(gpxFile);
		    out = new BufferedWriter(writer);
		    out.write("<?xml version='1.0' encoding='ISO-8859-1' standalone='yes'?>\n");
		    out.write("<gpx\n");
		    out.write("xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n");
		    out.write("xmlns='http://www.topografix.com/GPX/1/1' version='1.1' creator='AndroidGPSTrack'\n");
		    out.write("xsi:schemaLocation='http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd'>\n");
		 } catch (IOException e) {
      		Toast.makeText(getBaseContext(), "SD card not available.",
  					Toast.LENGTH_SHORT).show();
		}
    }


    @Override
	/** The menu with 'Exit' is generated only*/
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor myPreferenceEditor = myPreference.edit();
		logIntervalString = myPreference.getString("logInterval", logIntervalString);
		myPreferenceEditor.putString("logInterval", logIntervalString).commit();
		logIntervalInt = Integer.parseInt(logIntervalString);
		btMacAddress = myPreference.getString("btMacAddress", btMacAddress);
		myPreferenceEditor.putString("btMacAddress", btMacAddress).commit();//test
		Toast.makeText(this, "Loginterval: " + logIntervalString, Toast.LENGTH_LONG).show(); //test
		Toast.makeText(this, "BT-Address: " + btMacAddress, Toast.LENGTH_LONG).show(); //test
		obdRun();
	}
    
    /*-------------------------------------------
     OBD gedeelte............................. 
     Verhuizen naar een aparte class maar weet niet hoe dat moet.
     Kan met: import com.gps.OBDscanMainActivity;
    /*
  
    OBDscanMainActivity Test;
    int Test = Test.intCoolant;
    
    public void addDelay(OBDscanMainActivity myDelay)
    {
    	myDelay.Delay(10);
    }
 
    public void addOBDrun(OBDscanMainActivity myOBDrun)
    {
    	myOBDrun.ObdRun();
    	int strObdSpeed = myOBDrun.intSpeed; 
    	ObdSpeed.setText(strObdSpeed + "  KMh");
    }
   */ 

	/** The selected menu item is executed */
    public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId())
      {
       case R.id.secure_connect_scan:
	     // Launch the DeviceListActivity to see devices and do scan
    	   	Toast.makeText(getBaseContext(), "DeviceList", Toast.LENGTH_SHORT).show();
    	   	serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
        case R.id.menu_settings:
        		 	 startActivity(new Intent(this, Prefs.class));
        		 	 break;
       	case R.id.menu_write_file:
    			 try {
    				out.write("</gpx>\n");
    				out.close();
    				Toast.makeText(getBaseContext(), "File written.", Toast.LENGTH_SHORT).show();
    			 } catch (IOException e) {
                 //Toast.makeText(getBaseContext(), "SD card not available.",
          		 //Toast.LENGTH_SHORT).show();
              	 }
			finish();
			break;
        	case R.id.menu_exit:
        			try {
                        //boolean deleted = gpxFile.delete();
                        out.flush();
        				out.close();
						gpxFile.delete();
        				//File gpxFile = new File(root, Filename);
        				//gpxFile.delete();
        				Toast.makeText(getBaseContext(), "File not written.",
             			Toast.LENGTH_SHORT).show();
        			} catch (IOException e) {
                    //Toast.makeText(getBaseContext(), "SD card not available.", Toast.LENGTH_SHORT).show();
                  	}
                   	GpsActivity.this.finish();
        		 	System.exit(0);
        		 	break;
        	default: break;
        }
        return super.onOptionsItemSelected(item);
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
					//setupChat();
				} else {
					// User did not enable Bluetooth or an error occurred
					Log.d(TAG, "BT not enabled");
					Toast.makeText(this, "BT NOT ENABLED", Toast.LENGTH_SHORT).show();
					finish();
				}
		}
	}

	//Chat functie voor het lezen van BT-data
	private void setupChat() {
		Log.d(TAG, "setupChat()");
		// Initialize the array adapter for the conversation thread
		//mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		//mConversationView = (ListView) findViewById(R.id.in);
		//mConversationView.setAdapter(mConversationArrayAdapter);
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
		//Log.d("BT Handler SETUP ", "" +  mChatService.BTmsgHandler);
		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer();
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}	
   
    private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		btMacAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

		SharedPreferences myPreference=PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor myPreferenceEditor = myPreference.edit();
        myPreferenceEditor.putString("btMacAddress", btMacAddress).commit();//test
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(btMacAddress);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
		//obdRun();
	}

	//Auto run obd commands voor testen
	public void obdRun() {
		autoUpdate = new Timer();
		autoUpdate.schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						switch (Teller) {
							case 0:
								tvMsgWindow.append("\n" + "ATZ: reset all " + Teller + "\n");
									sendMessage("ATZ" + "\r"); //Test geeft OK
								break;
							case 1:
								tvMsgWindow.append("\n" + "ATE1: echo on " + Teller + "\n");
								sendMessage("ATE1" + "\r"); //Echo on
								break;
							case 2:
								tvMsgWindow.append("\n" + "AT@1: dev. descr. " + Teller + "\n");
								sendMessage("AT@1" + "\r"); //Protocol geeft?
								break;
							case 3:
								tvMsgWindow.append("\n" + "ATSP0: prot. auto " + Teller + "\n");
								sendMessage("ATSP0" + "\r"); //Protocol geeft AUTO
								break;
							case 4:
								tvMsgWindow.append("\n" + "ATDP: display prot." + Teller + "\n");
									sendMessage("ATDP" + "\r"); //Buffer Dump
								break;
							case 5:
								tvMsgWindow.append("\n" + "ATDPN: prot. number " + Teller + "\n");
								sendMessage("ATDPN" + "\r"); //
								break;
							case 6:
								tvMsgWindow.append("\n" + "ATBD: buffer dump " + Teller + "\n");
								sendMessage("ATBD" + "\r"); //S
								break;
							case 7:
								tvMsgWindow.append("\n" + "ATRD: read stored data " + Teller + "\n");
									sendMessage("ATRD" + "\r"); //SPEED
								break;
							case 8:
								tvMsgWindow.append("\n" + "ATRV: voltage " + Teller + "\n");
									sendMessage("ATRV" + "\r"); //SPEED
								break;
							case 9:
								tvMsgWindow.append("\n" + "RPM: toeren " + Teller + "\n");
								sendMessage("01" + strObdRpm + "\r"); //RPM
								break;
							case 10:
								tvMsgWindow.append("\n" + "SPEED: " + Teller + "\n");
								sendMessage("01" + strObdSpeed + "\r"); //SPEED
								break;
							case 11:
								tvMsgWindow.append("\n" + "COOLANT: " + Teller + "\n");
								sendMessage("01" + strObdCoolant + "\r"); //Coolant
								break;
							case 12:
								tvMsgWindow.append("\n" + "0104: engine load " + Teller + "\n");
								sendMessage("01" + strObdEngineLoad + "\r"); //load
								break;
							case 13:
								tvMsgWindow.append("\n" + "0111: trottlepos " + Teller + "\n");
								sendMessage("01" + strObdTrottlePos + "\r"); //load
								break;
							case 14:
								tvMsgWindow.append("\n" + "0143: absload " + Teller + "\n");
								sendMessage("01" + strObdAbsLoad + "\r"); //load
								break;
							case 15:
								tvMsgWindow.append("\n" + "0146: amb.air " + Teller + "\n");
								sendMessage("01" + strObdAmbientAir + "\r"); //load
								break;
							case 16:
								tvMsgWindow.append("\n" + "0146: oil temp " + Teller + "\n");
								sendMessage("01" + strObdOilTemp + "\r"); //load
								break;
							case 17:
								tvMsgWindow.append("\n" + "012F: fuel level " + Teller + "\n");
								sendMessage("01" + strObdFuelLevel + "\r"); //load
								break;
							case 18:
								tvMsgWindow.append("\n" + "ATCS: CAN status " + Teller + "\n");
									sendMessage("ATCS" + "\r"); //SPEED
								break;
							case 19:
								tvMsgWindow.append("\n" + "ATCAF1: CAN auto form." + Teller + "\n");
									sendMessage("ATCAF1" + "\r"); //SPEED
								break;
							case 20:
								tvMsgWindow.append("\n" + "ATCFC1: CAN flow on " + Teller + "\n");
									sendMessage("ATCFC1" + "\r"); //SPEED
								break;
							case 21:
								tvMsgWindow.append("\n" + "ATAL: long msg." + Teller + "\n");
									sendMessage("ATAL" + "\r"); //SPEED
								break;
							case 22:
								tvMsgWindow.append("\n" + "ATMA: monitor all" + Teller + "\n");
									sendMessage("ATMA" + "\r"); //SPEED
									break;
							default: Teller = 0;
								break;
						}
						//ObdBytes = null;
						//readValue = "";
						Teller++;
						if (Teller >= 23) Teller = 0;
					}
				});
			}
		}, 0, logIntervalInt * 1000); // updates each 1 secs
	}
		//Decodeer de OBD berichten
		public void ObdDecode()
		{
		  ObdBytes = readValue.trim().split(" ");
			//tvMsgWindow.append("*" + ObdBytes[0].substring(0,4) + "*" + "\n");
			if (ObdBytes[0].substring(0, 4).equals(strObdCoolant)) {
				//tvMsgWindow.append("$" + ObdBytes[2] + "$" + "\n");
			  intObdCoolant = Integer.parseInt(ObdBytes[2].trim(), 16) - 40; //PID waarde
				tvMsgWindow.append("OBD_COOLANT" + " | " + ObdBytes[0].substring(0, 4) + " | " + String.valueOf(intObdCoolant) + " | " + "\n");
				tvObdCoolant.setText(String.valueOf(intObdCoolant) + "  KMh");
		    } 
		  else
		   {	  
		    intObdCoolant = 0;
			   //tvMsgWindow.append("|" + String.valueOf(intCoolant) + "|" + "\n");
		   }

			if (ObdBytes[0].substring(0, 4).equals(strObdSpeed)) {
				//tvMsgWindow.append("$" + ObdBytes[2] + "$" + "\n");
			  intObdSpeed = (Integer.parseInt(ObdBytes[2].trim(), 16)) /4; //PID waarde
				tvMsgWindow.append("OBD_SPEED" + " | " + ObdBytes[0].substring(0, 4) + " | " + String.valueOf(intObdSpeed) + " | " + "\n");
				tvObdSpeed.setText(String.valueOf(intObdSpeed) + "  KMh");
		   } 
		  else
		   {	  
			intObdSpeed = 0;
			   //tvMsgWindow.append("|" + String.valueOf(intSpeed) + "|" + "\n");
		   }

			if (ObdBytes[0].substring(0, 4).equals(strObdRpm)) {
				//tvMsgWindow.append("$" + ObdBytes[2] + "$" + ObdBytes[3] + "$" + "\n");
			  intObdRpm = Integer.parseInt(ObdBytes[2].trim(), 16)+ Integer.valueOf(ObdBytes[3].trim(), 16); //PID waarde
				tvMsgWindow.append("OBD_RPM" + "|" + ObdBytes[0].substring(0, 4) + "|" + String.valueOf(intObdRpm) + "|" + "\n");
				tvObdRpm.setText(String.valueOf(intObdRpm) + "  RPM");
		   }	
		  else 
		   {  
			intObdRpm = 0;
			   //tvMsgWindow.append("|" + String.valueOf(intRpm) + "|" + "\n");
		   } 
		 ObdBytes = null;
		 readValue = "";
		}	
    
} // End class GpsActivity