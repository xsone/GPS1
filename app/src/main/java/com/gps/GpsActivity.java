package com.gps;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.location.*;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class GpsActivity extends Activity {
	private static final String TAG = "GPS";
	
	public TextView msgWindow;
	public TextView Long;
	public TextView Lat;
	public TextView GpsSpeed;
	public TextView txtObdSpeed;
	public TextView txtObdRpm;
	public TextView txtObdCoolant;
	public TextView Alt;
	public TextView Date;
	public TextView Time;
	public TextView Provider;
	public TextView Gpsfile;
	public String Filename;
	public String ProviderName;
	public String[] providers = {"network", "gps", "passive"};
	public static Criteria criteria;
	public Location location;
	public LocationProvider provider;
	public Integer Teller = 10;
	public Integer logIntervalInt = 30000; 
	public String logIntervalString = "30000"; 
	
	public Boolean isGPSEnabled = false;
	public Boolean isNetworkEnabled = false;
	
	public String GpsSpeedValue = "0";
	public String strObdSpeed = "99";
	public String strObdRpm = "1000";
	public String strObdCoolant = "90";
	public String TimeValue = "00:00:00";
	
	public String strObdCmdCoolant = "0105";
	public String strObdCmdSpeed = "010D";
	public String strObdCmdRpm = "010C";
	
	public int intObdRpm = 0;
	public int intObdSpeed = 0;
	public int intObdCoolant = 0;
	
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
 	
 	
    
	@Override
	/** onCreate() is called at start of activity */
    public void onCreate(Bundle savedInstanceState) {
    	LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 
        //getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        msgWindow = (TextView) findViewById(R.id.msgWindow);
        msgWindow.append("TEST");
        msgWindow.setMovementMethod(new ScrollingMovementMethod());
		//this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); //keyboard doesnt popup
		//msgWindow.append("TEST");
        
        SharedPreferences myPreference=PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor myPreferenceEditor = myPreference.edit();
        logIntervalString =  myPreference.getString("loginterval",logIntervalString);
	    logIntervalInt = Integer.parseInt(logIntervalString);
	    btMacAddress = myPreference.getString("btMacAddress","");
	    myPreferenceEditor.putString("btMacAddress", btMacAddress).commit();//test
	    myPreferenceEditor.putString("btMacAddress", "00:12:12:04:10:57").commit();//test
		Toast.makeText(this, "BT-Address: " + btMacAddress, Toast.LENGTH_LONG).show(); //test
		Toast.makeText(this, "Loginterval: " + logIntervalString, Toast.LENGTH_LONG).show(); //test

		
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
		
		
/*		
	    //Voor het weergeven van startwaarden
        Provider = (TextView) findViewById(R.id.Provider);
        Long = (TextView) findViewById(R.id.Lon);
		Lat = (TextView) findViewById(R.id.Lat);
		GpsSpeed = (TextView) findViewById(R.id.GpsSpeed);
		//ObdSpeed = (TextView) findViewById(R.id.ObdSpeed);
		//ObdRpm = (TextView) findViewById(R.id.ObdRpm);
		//ObdCoolant = (TextView) findViewById(R.id.ObdCoolant);
		Alt = (TextView) findViewById(R.id.Alt);
		Date = (TextView) findViewById(R.id.Time);
		Time = (TextView) findViewById(R.id.Time);
		//Gpsfile = (TextView) findViewById(R.id.File);
		//Gpsfile.setText(Filename);
		Provider.setText("ProviderName");
		msgWindow.append("TEST");
   */
   
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
                        	Long.setText(Double.toString(location.getLongitude()));
           	              	Lat.setText(Double.toString(location.getLatitude()));
                        }
                    }
            	// no network provider is enabled
            }
            else {
                //this.canGetLocation = true;
                if (isNetworkEnabled) {
                	//locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
                    Toast.makeText(getBaseContext(), "Network Enabled", Toast.LENGTH_SHORT).show();
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        provider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                //60000, // 1min
                                //30000, // 1/2 min (werkte)
                        		logIntervalInt*1000,
                                //1,   // 10m
                                0,
                                locationListener);
                        if (location != null) {
                        	Long.setText(Double.toString(location.getLongitude()));
           	              	Lat.setText(Double.toString(location.getLatitude()));
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
        String timeStamp = new SimpleDateFormat("ddMMyy_HH:mm").format(new Date());
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        root = Environment.getExternalStorageDirectory();
  	    //Filename = dateFormat.format(dateTime) + timeFormat.format(dateTime)+ ".gpx";
        Filename = timeStamp + ".gpx";
	    File gpxFile = new File(root, Filename);

	    
	    Provider = (TextView) findViewById(R.id.Provider);
        Lat = (TextView) findViewById(R.id.Lat);
        Long = (TextView) findViewById(R.id.Lon);
		GpsSpeed = (TextView) findViewById(R.id.GpsSpeed);
		txtObdSpeed = (TextView) findViewById(R.id.ObdSpeed);
		txtObdRpm = (TextView) findViewById(R.id.ObdRpm);
		txtObdCoolant = (TextView) findViewById(R.id.ObdCoolant);
		Alt = (TextView) findViewById(R.id.Alt);
		Date = (TextView) findViewById(R.id.Date);
		//Time = (TextView) findViewById(R.id.Time);
		//Gpsfile = (TextView) findViewById(R.id.File);
		//msgWindow = (TextView) findViewById(R.id.msgWindow);
		  
		//  Provider.setText(ProviderName);
		//  Gpsfile.setText(Filename);
		//  txtObdSpeed.setText(strObdSpeed + "  KMh");
		//  txtObdRpm.setText(strObdRpm + "  RPM");
		//  txtObdCoolant.setText(strObdCoolant + " �C");
	    
	    
	    
	   
	    //msgWindow.append("TEST");
        //msgWindow.setText("TEST2");
	    
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
    
	/** The LocationListener shows the most recent data with every call of onLocationChanged() */
	private final LocationListener locationListener = new LocationListener() {
    	  public void onLocationChanged(Location location) {
         
    	/*	  
    		  setContentView(R.layout.main);
              Provider = (TextView) findViewById(R.id.Provider);
              Lat = (TextView) findViewById(R.id.Lat);
              Long = (TextView) findViewById(R.id.Lon);
    		  GpsSpeed = (TextView) findViewById(R.id.GpsSpeed);
    		  txtObdSpeed = (TextView) findViewById(R.id.ObdSpeed);
    		  txtObdRpm = (TextView) findViewById(R.id.ObdRpm);
    		  txtObdCoolant = (TextView) findViewById(R.id.ObdCoolant);
    		  Alt = (TextView) findViewById(R.id.Alt);
    		  Date = (TextView) findViewById(R.id.Date);
    		  Time = (TextView) findViewById(R.id.Time);
    		  Gpsfile = (TextView) findViewById(R.id.File);
    		  
    		  Provider.setText(ProviderName);
    		  Gpsfile.setText(Filename);
    		  txtObdSpeed.setText(strObdSpeed + "  KMh");
    		  txtObdRpm.setText(strObdRpm + "  RPM");
    		  txtObdCoolant.setText(strObdCoolant + " �C");
    		*/  
    		      		  
    		  if (location != null)
    		   {	  
	    		  Long.setText(Double.toString(location.getLongitude()));
	              Lat.setText(Double.toString(location.getLatitude()));
	              //Speed.setText(Integer.toString(Teller++));
	              //if(Teller > 200) Teller = 10;
	              GpsSpeedValue =  Integer.toString((int)(location.getSpeed()*3600)/1000); //omwerken van m/s naar km/h
	              //GpsSpeedValue = "55"; //testwaarde
	              GpsSpeed.setText(GpsSpeedValue + "  KMh");
	              //Speed.setText(Float.toString((location.getSpeed()*3600)/1000)); //omwerken van m/s naar km/h
	              //String Altitude = Double.toString(location.getAltitude());
	              Alt.setText(String.format("%.2f", location.getAltitude()));
	              Date date = new Date(location.getTime());
	              SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	              TimeValue = timeFormat.format(date);
	       //       Time.setText(timeFormat.format(date));
	              DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
	       //       Date.setText(dateFormat.format(date));
	              //DateFormat dateFormat = new SimpleDateFormat("YY/MM/DD");
	              
	              //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(location.getTime());
	              //String time = new java.text.SimpleTimeFormat("HH:mm:ss.SSS").format(location.getTime());
	              //Time.setText(dateFormat.format(time));
	           } 
    		  else 
  				{ 
    			  Toast.makeText(getBaseContext(), "Couldn't retrieve user location",
        					Toast.LENGTH_SHORT).show();
    			} 

    		  String state = Environment.getExternalStorageState();
              if(Environment.MEDIA_MOUNTED.equals(state)){
              	try {
              	    if (root.canWrite()){
              	    	out.write(	"<wpt lat='" + Lat.getText() + "' lon='" + Long.getText()	+ "'>\n" +
              	    				"<name>" + GpsSpeedValue + " kmh" + " @ " + TimeValue + "</name>\n" +
              	    				"<desc>" + strObdSpeed + " kmh|" +  strObdRpm + " rpm|" +  strObdCoolant + " �C" + "</desc>\n" +
              	    				"<sym>" + "triangle" + "</sym>\n" +
              	    				//"<desc>" + Lat.=51.295094, Long.=6.790674, Alt.=39.000000m, Speed=3Km/h, Course=45deg. + "</desc>\n" +
              	    				//"<sym>" + Scenic Area + "</sym>\n" +
              	    				//"<name>" + GpsSpeedValue + strObdRpm + ObdCoolant + "</name>\n" +
              	    				//"<ele>" + Alt.getText() + "</ele>\n" +
              	    				//"<time> + Clock.getText() + </time>\n" +//
              	    				"<speed>" + GpsSpeed.getText() + "</speed>\n</wpt>\n");
              	    }
              	    else
              	    {
                  		Toast.makeText(getBaseContext(), "Impossible to write file.",
              					Toast.LENGTH_SHORT).show();
              	    	
              	    }
              	} catch (IOException e) {
              		Toast.makeText(getBaseContext(), "SD card not available.",
          					Toast.LENGTH_SHORT).show();
              		}
              }
          }
    	  public void onProviderDisabled(String provider){}
    	  public void onProviderEnabled(String provider) {}
    	  public void onStatusChanged(String provider, int status, Bundle extras) {}
    };
    
    @Override
	/** The menu with 'Exit' is generated only*/
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        //menu.add(Menu.NONE, 0, 0, "Exit");
        return true;
    }
    
	/** The selected menu item is executed */
    public boolean onOptionsItemSelected(MenuItem item){
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
					//setupChat();
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
		
		SharedPreferences myPreference=PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor myPreferenceEditor = myPreference.edit();
        myPreferenceEditor.putString("btMacAddress", btMacAddress).commit();//test
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(btMacAddress);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}
    
    private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
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
						//btState = 1;
					/*	
						byte[] readBuf = (byte[]) msg.obj;
					 // Test met Thermostaat data
						String readMessage = new String(readBuf, 0, msg.arg1);
						//Toast.makeText(getApplicationContext(), readMessage,Toast.LENGTH_SHORT).show();
						
						
						//Roomcontrol berichten
						readStringValuesTest = readMessage.trim().split(";");
						
						for (int x = 0; x < readStringValuesTest.length; x++) {
							  readStringValues[x] = readStringValuesTest[x];
							  Toast.makeText(getApplicationContext(), x + ": " + readStringValues[x],Toast.LENGTH_SHORT).show();
						 }
						
						strObdSpeed = readStringValues[1];
						strObdRpm = readStringValues[2];
						txtObdSpeed.setText(strObdSpeed + "  KMh");
			    		txtObdRpm.setText(strObdRpm + "  RPM");
			    		msgWindow.append(strObdSpeed + "\n");
			    		msgWindow.append(strObdRpm + "\n");
					*/	
			    		byte[] readBuf = (byte[]) msg.obj;
						readMessage = new String(readBuf, 0, msg.arg1);
						readValue = readValue + readMessage;
						//if(readValue.endsWith(">"))
						if(readValue.endsWith("|"))	
						 {	
						  ObdBytes = readValue.trim().split(" ");
						  msgWindow.append(readValue + "\n"); // test alles uitlezen;
						  ObdDecode();
						  //mObdService.ObdDecode(); //aanroepen ander Java programma
						 }
			    		
			    		
			    		//OBD-berichten
						//readValue = readValue + readMessage;
						//if(readValue.endsWith(">"))
						// {	
						//  ObdBytes = readValue.trim().split(" ");
						//  msgWindow.append(readValue + "\n"); // test alles uitlezen;
						//  ObdDecode();
						// }
						
						readMessage = "";
						readBuf = null;
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
									sendMessage(strObdCmdRpm + "\r"); //RPM
									break;		
							case 10:msgWindow.append("\n" + "SPEED: " + Teller + "\n");
									sendMessage(strObdCmdSpeed + "\r"); //SPEED
									break;		
							case 11:msgWindow.append("\n" + "0100: " + Teller + "\n");
									sendMessage("0100" + "\r"); //SPEED
									break;		
							case 12:msgWindow.append("\n" + "0104: " + Teller + "\n");
									sendMessage("0104" + "\r"); //SPEED
									break;		
							case 13:msgWindow.append("\n" + "COOLANT" + ": " + Teller + "\n");
									sendMessage(strObdCmdCoolant + "\r"); //Coolant
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
		
		//Decodeer de OBD berichten
		public void ObdDecode()
		{
		  ObdBytes = readValue.trim().split(" ");
		  //msgWindow.append("*" + ObdBytes[0].substring(0,4) + "*" + "\n");
		  if(ObdBytes[0].substring(0,4).equals(strObdCmdCoolant))
		   {   
			  //msgWindow.append("$" + ObdBytes[2] + "$" + "\n");
			  intObdCoolant = Integer.parseInt(ObdBytes[2].trim(), 16) - 40; //PID waarde
			  msgWindow.append("OBD_COOLANT" + " | " + ObdBytes[0].substring(0,4) + " | " + String.valueOf(intObdCoolant) + " | " + "\n");
			  txtObdCoolant.setText(String.valueOf(intObdCoolant) + "  KMh");
		    } 
		  else
		   {	  
		    intObdCoolant = 0;
		    //msgWindow.append("|" + String.valueOf(intCoolant) + "|" + "\n");
		   } 

		  if(ObdBytes[0].substring(0,4).equals(strObdCmdSpeed))
		   {
			  //msgWindow.append("$" + ObdBytes[2] + "$" + "\n");
			  intObdSpeed = (Integer.parseInt(ObdBytes[2].trim(), 16)) /4; //PID waarde
			  msgWindow.append("OBD_SPEED" + " | " + ObdBytes[0].substring(0,4) + " | " + String.valueOf(intObdSpeed) + " | " + "\n");
			  txtObdSpeed.setText(String.valueOf(intObdSpeed) + "  KMh");
		   } 
		  else
		   {	  
			intObdSpeed = 0;
			//msgWindow.append("|" + String.valueOf(intSpeed) + "|" + "\n");
		   }	
		
		  if(ObdBytes[0].substring(0,4).equals(strObdCmdRpm))
	       {  
			  //msgWindow.append("$" + ObdBytes[2] + "$" + ObdBytes[3] + "$" + "\n");
			  intObdRpm = Integer.parseInt(ObdBytes[2].trim(), 16)+ Integer.valueOf(ObdBytes[3].trim(), 16); //PID waarde
			  msgWindow.append("OBD_RPM" + "|" + ObdBytes[0].substring(0,4) + "|" + String.valueOf(intObdRpm) + "|" + "\n");
			  txtObdRpm.setText(String.valueOf(intObdRpm) + "  RPM");
		   }	
		  else 
		   {  
			intObdRpm = 0;
	        //msgWindow.append("|" + String.valueOf(intRpm) + "|" + "\n");
		   } 
		 ObdBytes = null;
		 readValue = "";
		}	
    
} // End class GpsActivity