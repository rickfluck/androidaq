/*
 * Copyright (C) 2012 Controlled Capture Systems, LLC
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

package com.androidaq;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.viewpagerindicator.TitlePageIndicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * This is the main Activity that displays the current chat session.
 */
//public class AndroiDAQTCPMain extends Activity implements OnPageChangeListener {
	public class AndroiDAQTCPMain extends Activity {
    // Debugging
    private static final String TAG = "TCPChat";
    private static final boolean D = true;
    private MenuItem mMenuItemConnect;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    private static final int CLEAR_SCREEN = 6;
    private static final int DELETE = 7;
    private static final int TOO_MANY_PULSED = 8;
    // Key names received from the AndroidSocket Handler
    public static final String DEVICE_NAME = "TCP_connection";
    public static final String TOAST = "toast";

    // Intent request codes
    //private static final int REQUEST_CONNECT_DEVICE = 9;
    //private static final int REQUEST_ENABLE_BT = 10;

    // Layout Views
    private TextView mTitle;
    //private TextView inText;
    private TextView logText;
    //private LayoutInflater mInflater = null;
    View logView;
    String TCPAddress;
    
    private boolean fromADCSample = false;
    boolean fromMenu = false;
    private boolean fromVoltsRead = false;
    private boolean fromInputsRead = false;
    private boolean fromContInputsRead = false;
    
    private String year;
   	private String month ;
   	private String date;
   	private String day;
   	private String hour;
   	private String minute;
   	private String seconds;
   	private String readBuffer = "";
    // Name of the connected device
    private String mConnectedDeviceName = "TCP";
    private  StringBuffer mOutStringBuffer;

    // Member object for the chat services
    private AndroidSocket mSerialService = null;
	int focusedPage = 0;
	TitlePageIndicator indicator;
	ViewPager pager;
	public WaveFormView mWaveform = null;
	int[] wfdata = new int[448];
	int lastlength = 0;
	
	String Bufferedmessage = "";
	//private AndroiDAQTCPAdapter vpa;
	
	boolean firstopen;
	boolean fromOScope;
	
	static final String SD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
	static final String ADC_FILE_NAME = "log.txt";
	File fileToSave;
	String myFile;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        if (savedInstanceState!=null) {
            onRestoreInstanceState(savedInstanceState);
        }
        //updateFromPreferences();
        onNewIntent(getIntent());		
	}
    @Override
    public void onNewIntent(Intent intent) {
        if(D) Log.e(TAG, "+++ NEW INTENT +++");
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.right_title);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
        Intent intent1 = getIntent();
        TCPAddress = intent1.getStringExtra("TCPAddress");
		
        
	    AndroiDAQTCPAdapter adapter = new AndroiDAQTCPAdapter( this );
    	pager = (ViewPager)findViewById( R.id.viewpager );
	    pager.setOffscreenPageLimit(6);
	    indicator =  (TitlePageIndicator)findViewById( R.id.indicator );
	    pager.setAdapter( adapter );
	    indicator.setViewPager( pager );
	    mWaveform = (WaveFormView)findViewById(R.id.WaveformArea);
	    //indicator.setOnPageChangeListener(this);
    }
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        //Open socket with setupChat()
        if (mSerialService == null) setupChat();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        Intent intent = getIntent();
        intent.getBooleanExtra("firstopen", firstopen);
        updateFromPreferences();
       /* if (mSerialService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
        	 Log.d(TAG, "mSerialService.getState() = " + mSerialService.getState());
        	if (mSerialService.getState() == AndroidSocket.STATE_NONE) {
              // Start the Bluetooth chat services
              mSerialService.start();
            }
        } */
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        //mSerialService = new AndroidSocket(this, mHandler);
        new MyTask().execute();
        // Initialize the buffer for outgoing messages
        //mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
        
    }

    @Override
    public void onStop() {
        super.onStop();
        saveMyPreferences();
        if(D) Log.e(TAG, "-- ON STOP --");
        System.gc();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the TCP services
        if (mSerialService != null) mSerialService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
     public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mSerialService.getState() != AndroidSocket.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the AndroidSocket to write
            byte[] send = message.getBytes();
            mSerialService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }
     public int btState() {
    	 int state = mSerialService.getState();
    	 return state;
     }
    // The Handler that gets information back from the AndroidSocket
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case AndroidSocket.STATE_CONNECTED:
                	if (mMenuItemConnect != null) {
                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
                		mMenuItemConnect.setTitle(R.string.disconnect);
                	}
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    //messageArrayAdapter.clear();
                    break;
                case AndroidSocket.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case AndroidSocket.STATE_LISTEN:
                case AndroidSocket.STATE_NONE:
                	if (mMenuItemConnect != null) {
                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
                		mMenuItemConnect.setTitle(R.string.connect);
                	}
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_READ:
            	//String theMessage = (String) msg.obj;
        		//Log.e("Test", "fromContInputs theMessage is: " + theMessage);
        		//Log.e("Test", "fromMenu is: " + fromMenu);
        		//Log.e("Test", "fromInputsRead is: " + fromInputsRead);
        		//Log.e("Test", "fromContInputsRead is: " + fromContInputsRead);
        		//Log.e("Test", "fromVoltsRead is: " + fromVoltsRead);   
            	
            	if (fromContInputsRead) {
            		final ScrollView sv = (ScrollView)findViewById(R.id.scrollViewInputs);
            		String readMessage = (String) msg.obj;
            		//Log.e("Test", "fromContInputs readMessage is: " + readMessage);
                	int length = readBuffer.length();
                	
                	if (length <32) {
                		if (length == 0) {
                			readBuffer = readMessage;
                		} else {
                			readBuffer = readBuffer + readMessage;
                		}
                		length = readBuffer.length();
                		//Log.e("Test", "fromContInputsRead readBuffer.length1 is: " + length1);
                		if (length >= 32) {
                			readBuffer = readBuffer.replace("\n", "");
                			Log.e("Test", "fromContInputs sending this readBuffer: " + readBuffer);
                			((AndroiDAQTCPAdapter)pager.getAdapter()).setInputsText(readBuffer);
                    		readBuffer = "";
                    		//readMessage = null;
                		}
                	}
                	/*sv.post(new Runnable() { 
                	    public void run() { 
                	        sv.fullScroll(ScrollView.FOCUS_DOWN); 
                	    } 
                	}); */
            	}
            	if (fromInputsRead) {
            		final ScrollView sv = (ScrollView)findViewById(R.id.scrollViewInputs);
            		String readMessage = (String) msg.obj;
            		//Log.e("Test", "fromInputsRead readMessage is: " + readMessage);
                	int length = readBuffer.length();
                	//Log.e("Test", "fromInputsRead readBuffer.length is: " + length);
                	if (length <32) {
                		if (length == 0) {
                			readBuffer = readMessage;
                		} else {
                			readBuffer = readBuffer + readMessage;
                		}
                		int length1 = readBuffer.length();
                		if (length1 >= 32) {
                			Log.e("Test", "fromInputs sending this readBuffer: " + readBuffer);
                			((AndroiDAQTCPAdapter)pager.getAdapter()).setInputsText(readBuffer);
                    		readBuffer = "";
                    		//readMessage = null;
                		}
                	}
               	sv.post(new Runnable() { 
                	    public void run() { 
                	        sv.fullScroll(ScrollView.FOCUS_DOWN); 
                	    } 
                	});
            	}
            	if (fromVoltsRead) {
            		final ScrollView sv = (ScrollView)findViewById(R.id.scrollViewVolts);
            		String readMessage = (String) msg.obj;
            		//Log.e("Test", "fromVoltsRead readMessage is: " + readMessage);
                	int length = readBuffer.length();
                	//Log.e("Test", "fromVoltsRead readBuffer.length is: " + length);
                	if (length <56) {
                		if (length == 0) {
                			readBuffer = readMessage;
                		} else {
                			readBuffer = readBuffer + readMessage;
                		}
                		//Log.e("Test", "fromVoltsRead readBuffer is: " + readBuffer);
                		int length1 = readBuffer.length();
                		//Log.e("Test", "fromVoltsRead readBuffer.length1 is: " + length1);
                		if (length1 >= 56) {
                			Log.e("Test", "fromVolts sending this readBuffer: " + readBuffer);
                			((AndroiDAQTCPAdapter)pager.getAdapter()).setVoltText(readBuffer);
                    		readBuffer = "";
                    		//readMessage = null;
                		}
                	}
                	/*sv.post(new Runnable() { 
                	    public void run() { 
                	        sv.fullScroll(ScrollView.FOCUS_DOWN); 
                	    } 
                	}); */
	            } if (fromMenu) {
	           		final ScrollView sv = (ScrollView)findViewById(R.id.scrollViewLog);
	            	String readMessage = (String) msg.obj;
	            	//Log.e("Test", "readMessage from normal view is: " + readMessage);
	            	((AndroiDAQTCPAdapter)pager.getAdapter()).setText(readMessage);
	            	sv.post(new Runnable() { 
	            	    public void run() { 
	            	        sv.fullScroll(ScrollView.FOCUS_DOWN); 
	            	    } 
	            	});
	            	/* if (readMessage.contains("Readings:")) {
	            		String[] timeStampSplit = readMessage.split("Readings:");
	            		Log.e("Test", "timeStampSplit[1]: " + timeStampSplit[1]);
	            		String[] data = timeStampSplit[1].split(",");
	            		Log.e("Test", "data size: " + data.length);
 	            	} */
	            	//fromMenu = false;
	            } if (fromADCSample) {
	           		final ScrollView sv = (ScrollView)findViewById(R.id.scrollViewLog);
	            	String readMessage = (String) msg.obj;
	            	//Log.e("Test", "readMessage from normal view is: " + readMessage);
	            	((AndroiDAQTCPAdapter)pager.getAdapter()).setText(readMessage);
	            	sv.post(new Runnable() { 
	            	    public void run() { 
	            	        sv.fullScroll(ScrollView.FOCUS_DOWN); 
	            	    } 
	            	});
	            	int length = readMessage.length();
	            	Log.e(TAG, "readMessage length is: " + length);
	            	lastlength = lastlength + length;
	            	Log.e(TAG, "readMessage lastlength is: " + lastlength);
	            	if (lastlength >= 2698 && fromOScope) {
	            		getOscopeReadings();
		            	fromADCSample = false;
	            	}
	            }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = "TCP";
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

   /* public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    } */
    public void writeToFile(String data) {
    	
    	File myFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/log.txt");
        
        try {
			if (!myFile.exists()) {
				myFile.createNewFile();
				Toast.makeText(getBaseContext(),
	                    "Created new 'log.txt'",
	                    Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			Toast.makeText(getBaseContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
		}
    	
    	try {
            
            FileOutputStream fOut = new FileOutputStream(myFile, true);
            OutputStreamWriter myOutWriter = 
                                    new OutputStreamWriter(fOut);
            myOutWriter.append(data);
            myOutWriter.close();
            fOut.close();
            Toast.makeText(getBaseContext(),
                    "Done writing to SD 'log.txt'",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }

    }
    public int getConnectionState() {
		return mSerialService.getState();
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        int state = getConnectionState();
        if (state == 0) {
	        mMenuItemConnect = menu.getItem(0);
	        if (mMenuItemConnect != null) {
	        	mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
        		mMenuItemConnect.setTitle(R.string.connect);
	    	}
        } else {
	        mMenuItemConnect = menu.getItem(0);
	        if (mMenuItemConnect != null) {
	    		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
	    		mMenuItemConnect.setTitle(R.string.disconnect);
	    	}
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect:
        	int state = getConnectionState();
        	Log.e(TAG, "getConnectionState" + state);
        	if (state == 0) {
        		//mSerialService = new AndroidSocket(this, mHandler);
        		new MyTask().execute();
        		mSerialService.start();
        	}
        	if (state == 1) {
        		mSerialService.start();
        	} else {
        		if (state == 3) {
        			mSerialService.stop();
        		}
        	}
            return true;
		case R.id.clock:
		    // Read Real Time Clock on AndroiDAQ
			if (pager.getCurrentItem() != 5) {
				pager.setCurrentItem(5);
			}
			if (mSerialService.getState() != AndroidSocket.STATE_CONNECTED) {
				Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
				return true;
			} else { 
				if (mMenuItemConnect != null) {
            		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            		mMenuItemConnect.setTitle(R.string.disconnect);
            	}
				mTitle.setText(R.string.title_connected_to);
	            mTitle.append(mConnectedDeviceName);
	            fromADCSample = false;
				fromMenu = true;
				String message2 = "05\r";
				Bufferedmessage = "";
			    sendMessage(message2);
			    return true;
			}
		case R.id.autosetRTC:
		    // Read DIR of SDcard on AndroiDAQ
			if (pager.getCurrentItem() != 5) {
				pager.setCurrentItem(5);
			}
			if (mSerialService.getState() != AndroidSocket.STATE_CONNECTED) {
				Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
				return true;
			} else { 
				if (mMenuItemConnect != null) {
            		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            		mMenuItemConnect.setTitle(R.string.disconnect);
            	}
				mTitle.setText(R.string.title_connected_to);
				mTitle.append(mConnectedDeviceName);
				fromADCSample = false;
				fromMenu = true;
				String message7 = "06\r";
				sendMessage(message7);
				getTime();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
				String message9 = "0" + year + "\r" 
					+ "0" + month + "\r"
					+ "0" + date + "\r"
					+ "0" + day + "\r"
					+ "0" + hour + "\r"
					+ "0" + minute + "\r"
					+ "0" + seconds + "\r";
				Log.i(TAG, "message9 " + message9);
				Bufferedmessage = "";
				sendMessage(message9);
				return true;
			}
		case R.id.deletelog:
		    // Delete log file SDcard on AndroiDAQ
			if (pager.getCurrentItem() != 5) {
				pager.setCurrentItem(5);
			}
			if (mSerialService.getState() != AndroidSocket.STATE_CONNECTED) {
				Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
				return true;
			} else { 
				showDialog(DELETE);
				return true; 
			}
		case R.id.clearscreen:
		    // Clear inText screen
			if (mSerialService.getState() != AndroidSocket.STATE_CONNECTED) {
				Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
				return true;
			} else {
				showDialog(CLEAR_SCREEN);
			    return true;
			}
			}
        return false;
    }
	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case CLEAR_SCREEN:
			return new AlertDialog.Builder(AndroiDAQTCPMain.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage("Do you really want to clear the screen and its data?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					clearScreen();	
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			})
			.create();
		case DELETE:
			return new AlertDialog.Builder(AndroiDAQTCPMain.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage("Do you really want to delete the log file and its data?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					fromADCSample = false;
					fromMenu = true;
					String message8 = "08\r";
					sendMessage(message8);	
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			})
			.create();	
		case TOO_MANY_PULSED:
			return new AlertDialog.Builder(AndroiDAQTCPMain.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage("You can only set 8 channels for frequency output! Please check your settings.")
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			})
			.create();
		}
		return null;
	}
	private void clearScreen() {
		 logText = (TextView)findViewById(R.id.logtext);
		 logText.setText("");
		 Bufferedmessage = "";
	}
	@Override
	public void onBackPressed() {
		int state = getConnectionState();
    	Log.i(TAG, "getConnectionState" + state);
   		if (state == 3) {
   			mSerialService.stop();
    		//mSerialService.start();
   		}
		finish();
	}
	public void setPage(int page) {
		pager.setCurrentItem(page);
	}
	public void getAll(boolean setting) {
		fromVoltsRead = setting;
		if (fromVoltsRead) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
			sendMessage("04\r");
		} else {
			getInputs(true);
		}
	}
	public void getADCSample(boolean setting, String sample, String rate) {
		fromVoltsRead = false;
		fromMenu = false;
		fromInputsRead = false;
		fromContInputsRead = false;
		fromADCSample = setting;
		fromOScope = false;
		if (fromADCSample) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
	    	//Log.v("Command will be: ", "016\r" + "0" + sample +"\r" + "0" + rate + "\r");
			sendMessage("016\r" + "0" + sample +"\r" + "0" + rate + "\r");
			if (pager.getCurrentItem() != 5) {
				pager.setCurrentItem(5);
			}
		} 		
	}
	public void getOscopeSample(boolean setting, String rate) {
		fromVoltsRead = false;
		fromMenu = false;
		fromInputsRead = false;
		fromContInputsRead = false;
		fromADCSample = setting;
		lastlength = 0;
		fromOScope = true;
		if (fromADCSample) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
	    	//Log.v("Command will be: ", "016\r" + "0" + sample +"\r" + "0" + rate + "\r");
			sendMessage("017\r"+ "0" + rate + "\r");
		} 		
	}
	public void getOscopeReadings() {
		logText = (TextView)findViewById(R.id.logtext);
		mWaveform = (WaveFormView)findViewById(R.id.WaveformArea);
		String logsText = logText.getText().toString();
		String[] timeStampSplit = logsText.split("Readings:");
		int tsSize = timeStampSplit.length;
		
		Log.e(TAG, "timeStampSplit size: " + tsSize);
		String[] data = timeStampSplit[tsSize - 1].split(",");
		Log.e("Test", "data size: " + data.length);
		if (data.length != 0) {
			for(int x = 0; x < data.length; x++){
				//data[x] =data[x].replace(" ", ""); // fill array with value of ch1_pos scaled for 1-4095 (ADC values)
				data[x] =data[x].trim();
				//ch2_data[x] = ch2_pos;
			} 
		}
		for(int x=0; x < data.length; x++){
        	wfdata[x] = Integer.parseInt(data[x]); // fill array with value of ch1_pos for testing
			//ch2_data[x] = ch2_pos;
		} 
		wfdata =Arrays.copyOfRange(wfdata, 0, data.length);
        Toast.makeText(this, "Updated Waveform", Toast.LENGTH_SHORT).show();
        mWaveform.set_data(wfdata); 
        if (pager.getCurrentItem() != 7) {
			pager.setCurrentItem(7);
		}
	}
	public void getVolts(boolean setting) {
		fromVoltsRead = setting;
		fromMenu = false;
		fromInputsRead = false;
		fromContInputsRead = false;
		fromADCSample = false;
		if (fromVoltsRead) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
			sendMessage("04\r");
		} 
	}
	public void getVoltsCont(boolean setting) {
		fromVoltsRead = setting;
		fromMenu = false;
		fromInputsRead = false;
		fromContInputsRead = false;
		fromADCSample = false;
		if (fromVoltsRead) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
			sendMessage("011\r");
		} 
	}
	public void stopContVolts() {
		fromVoltsRead = false;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		sendMessage("012\r");
	}
	public void getInputs(boolean setting) {
		fromInputsRead = setting;
		fromMenu = false;
		fromContInputsRead = false;
		fromVoltsRead = false;
		fromADCSample = false;
		if (fromInputsRead) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
			sendMessage("02\r");
		}
	}
	public void getInputsCont(boolean setting) {
		fromContInputsRead = setting;
		fromMenu = false;
		fromInputsRead = false;
		fromVoltsRead = false;
		fromADCSample = false;
		if (fromContInputsRead) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
			sendMessage("010\r");
		}
	}
	public void stopContInput() {
		fromContInputsRead = false;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		sendMessage("09\r");
	}
	public void setOutputs() {
		Log.i(TAG, "setOutputs" + "Setting Output values");
 		sendMessage("01\r");
	}
    public void setFromMenu(boolean fromLog) {
    	fromMenu = fromLog;
    	fromInputsRead = false;
    	fromContInputsRead = false;
    	fromVoltsRead = false;
    	fromADCSample = false;
    }
    private void getTime() {
    	Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yy_MM_dd_E_kk_mm_ss");
		String timeStamp = sdf.format(now);
		//Log.i(TAG, "timeStamp " + timeStamp);
		String[] arrayString = ((String) timeStamp).split("_");

		year = arrayString[0];
		//Log.i(TAG, "year " + year);
		month = arrayString[1];
		date = arrayString[2];
		if (arrayString[3].contains("Mon")) day = "01";
		if (arrayString[3].contains("Tue")) day = "02";
		if (arrayString[3].contains("Wed")) day = "03";
		if (arrayString[3].contains("Thu")) day = "04";
		if (arrayString[3].contains( "Fri")) day = "05";
		if (arrayString[3].contains( "Sat")) day = "06";
		if (arrayString[3].contains("Sun")) day = "07";
		//Log.i(TAG, "day " + day);
		hour = arrayString[4];
		minute = arrayString[5];
		seconds = arrayString[6];
    }
	public void openMenu() {
		openOptionsMenu();
	}
	@Override
	public void openOptionsMenu() {

	    Configuration config = getResources().getConfiguration();

	    if((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) 
	            > Configuration.SCREENLAYOUT_SIZE_LARGE) {

	        int originalScreenLayout = config.screenLayout;
	        config.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE;
	        super.openOptionsMenu();
	        config.screenLayout = originalScreenLayout;

	    } else {
	        super.openOptionsMenu();
	    }
	}
	private void updateFromPreferences() {
		//Log.e(TAG, "Update Preferences Fired");
		Context context = getApplicationContext();
		boolean[] isOutputCh = loadBooleanArray("isInput", context);
		boolean[] isDigCh = loadBooleanArray("isDig", context);
		boolean[] outputState = loadBooleanArray("outputState", context);
		boolean fromFreq = true;
		String[] desiredFreq = loadStringArray("desiredFreqs", context, fromFreq);
		fromFreq = false;
		String[] desiredDuty = loadStringArray("desiredDutys", context, fromFreq);
		Bundle myBundle = new Bundle();
		myBundle.putBooleanArray("isInput", isOutputCh);
		myBundle.putBooleanArray("isDig", isDigCh);
		myBundle.putBooleanArray("outputState", outputState);
		myBundle.putStringArray("desiredFreqs", desiredFreq);
		myBundle.putStringArray("desiredDutys", desiredDuty);
		((AndroiDAQTCPAdapter)pager.getAdapter()).setUIStates(myBundle);
		/*Example
		countSecs = prefs.getInt("setTime", 5000);
		timeIsSet = prefs.getBoolean("timeSet", true);
		project = prefs.getString("project", "Project01");*/
	}
	private void saveMyPreferences() {
		//Log.e(TAG, "Save Preferences Fired");
		Context context = getApplicationContext();
		Bundle myBundle = ((AndroiDAQTCPAdapter)pager.getAdapter()).getUIStates();
		boolean[] isOutputCh = myBundle.getBooleanArray("isInput");
		boolean[] isDigCh = myBundle.getBooleanArray("isDig");
		boolean[] outputState = myBundle.getBooleanArray("outputState");
		String[] desiredFreq = myBundle.getStringArray("desiredFreqs");
		String[] desiredDuty = myBundle.getStringArray("desiredDutys");
		saveBooleanArray(isOutputCh, "isInput", context);
		saveBooleanArray(isDigCh, "isDig", context);
		saveBooleanArray(outputState, "outputState", context);
		saveStringArray(desiredFreq, "desiredFreqs", context);
		saveStringArray(desiredDuty, "desiredDutys", context);
		
		/*Example
		mEditor.putInt("setTime", countSecs);
		mEditor.putBoolean("timeSet", timeIsSet);
		mEditor.putString("project", project);
		mEditor.commit(); */
	}
	public boolean saveBooleanArray(boolean[] array, String arrayName, Context mContext) { 
		  SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
		  SharedPreferences.Editor editor = prefs.edit();
		  editor.putInt(arrayName +"_size", array.length);
		  for(int i=0;i<array.length;i++)
		    editor.putBoolean(arrayName + "_" + i, array[i]);
		  return editor.commit();
	}
	public boolean saveStringArray(String[] array, String arrayName, Context mContext) { 
		  SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
		  SharedPreferences.Editor editor = prefs.edit();
		  editor.putInt(arrayName +"_size", array.length);
		  for(int i=0;i<array.length;i++)
		    editor.putString(arrayName + "_" + i, array[i]);
		  return editor.commit();
	}
	public boolean[] loadBooleanArray(String arrayName, Context mContext) {
		  SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
		  int size = prefs.getInt(arrayName + "_size", 16);
		  boolean array[] = new boolean[size];
		  for(int i=0;i<size;i++)
		    array[i] = prefs.getBoolean(arrayName + "_" + i, false);
		  return array;
	}
	public String[] loadStringArray(String arrayName, Context mContext, boolean fromFreq) {
		  SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
		  int size = prefs.getInt(arrayName + "_size", 16);
		  String array[] = new String[size];
		  if (fromFreq) {
			  for(int i=0;i<size;i++)
				  array[i] = prefs.getString(arrayName + "_" + i, "0");
		  } else {
			  for(int i=0;i<size;i++)
				  array[i] = prefs.getString(arrayName + "_" + i, "50");
		  }
		  return array;
		}
	public class MyTask extends AsyncTask<Void, Void, Void>{
	    @Override
	    protected Void doInBackground(Void... params) {
	    	 mSerialService = new AndroidSocket(this, mHandler, TCPAddress);
	         // Initialize the buffer for outgoing messages
	         mOutStringBuffer = new StringBuffer("");
	         if (mSerialService != null) {
		            // Only if the state is STATE_NONE, do we know that we haven't started already
		        	 Log.d(TAG, "mSerialService.getState() = " + mSerialService.getState());
		        	if (mSerialService.getState() == AndroidSocket.STATE_NONE) {
		              // Start the Bluetooth chat services
		              mSerialService.start();
		            }
		        }
	     return null;
	    }
	    @Override
	    protected void onPostExecute(Void result) {
	     super.onPostExecute(result);   
	    }

	/*public void showSetupToast() {
		Toast.makeText(this, "Swipe up for MENU", Toast.LENGTH_LONG).show();
	} */
	/* @Override
	public void onPageScrollStateChanged(int state) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		
	}
	@Override
	public void onPageSelected(int position) {
		if (position == 0) {
			Toast.makeText(this, "Swipe up for MENU", Toast.LENGTH_SHORT).show();
		}		
	} */
	}
}