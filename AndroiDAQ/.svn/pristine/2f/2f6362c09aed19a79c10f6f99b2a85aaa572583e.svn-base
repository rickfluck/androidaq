package com.androidaq;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

public class StartAndroiDAQ extends Activity {
	//private static final String TAG = "Pics 2 Flix Lite";
	String filename;
	boolean adBased = false;
	boolean isFreeVersion = true;
	boolean firstopen = true;
	String TCPAddress;
	EditText input;
	protected boolean _active = true;
	protected int SPLASH_DISPLAY_TIME  = 1000; // time to display the splash screen in ms

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.splash);
		updateFromPreferences();
		PopIt();
		
	}
	public void PopIt() {
        new AlertDialog.Builder(this)
        .setTitle( "How would you like to connect to AndroiDAQ?" )
        .setMessage( "Please choose a connection type:" )
        .setPositiveButton("Bluetooth", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            	new Handler().postDelayed(new Runnable() {
        			@Override
        			public void run() {
        				//Intent mainIntent = new Intent(StartAndroiDAQ.this,MyActivity.class);
        				Intent mainIntent = new Intent(StartAndroiDAQ.this,AndroiDAQMain.class);
        				mainIntent.putExtra("adbased", adBased);
        				mainIntent.putExtra("isFreeVersion", isFreeVersion);
        				mainIntent.putExtra("firstopen", firstopen);
        				StartAndroiDAQ.this.startActivity(mainIntent);
        				StartAndroiDAQ.this.finish();
        				overridePendingTransition(R.anim.grow_from_bottom,
        						R.anim.shrink_from_bottom);
        			}
        		}, SPLASH_DISPLAY_TIME);
            }
        })
        .setNegativeButton("Wifi", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            	/*new Handler().postDelayed(new Runnable() {
        			@Override
        			public void run() {
        				Intent mainIntent = new Intent(StartAndroiDAQ.this,AndroiDAQTCPMain.class);
        				//Intent mainIntent = new Intent(StartAndroiDAQ.this,AndroiDAQMain.class);
        				mainIntent.putExtra("adbased", adBased);
        				mainIntent.putExtra("isFreeVersion", isFreeVersion);
        				mainIntent.putExtra("firstopen", firstopen);
        				StartAndroiDAQ.this.startActivity(mainIntent);
        				StartAndroiDAQ.this.finish();
        				overridePendingTransition(R.anim.grow_from_bottom,
        						R.anim.shrink_from_bottom);
        			}
        		}, SPLASH_DISPLAY_TIME); */
            	GetTCP();
            }
        }).show();
    }
	public void GetTCP() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Please enter TCP Address");

		// Set up the input
		input = new EditText(this);
		input.setText(TCPAddress);
		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	TCPAddress = input.getText().toString();
		    	saveMyPreferences();
		    	new Handler().postDelayed(new Runnable() {
        			@Override
        			public void run() {
        				Intent mainIntent = new Intent(StartAndroiDAQ.this,AndroiDAQTCPMain.class);
        				//Intent mainIntent = new Intent(StartAndroiDAQ.this,AndroiDAQMain.class);
        				mainIntent.putExtra("adbased", adBased);
        				mainIntent.putExtra("isFreeVersion", isFreeVersion);
        				mainIntent.putExtra("firstopen", firstopen);
        				mainIntent.putExtra("TCPAddress", TCPAddress);
        				StartAndroiDAQ.this.startActivity(mainIntent);
        				StartAndroiDAQ.this.finish();
        				overridePendingTransition(R.anim.grow_from_bottom,
        						R.anim.shrink_from_bottom);
        			}
        		}, SPLASH_DISPLAY_TIME);
		    }
		});
		builder.show();
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			_active = false;
		}
		return true;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0 && resultCode==RESULT_OK) {

			System.gc();
			finish();
		}
	}
	private void updateFromPreferences() {
		//Log.e(TAG, "Update Preferences Fired");
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		TCPAddress = prefs.getString("TCPAddress", "0.0.0.0");
	}
	private void saveMyPreferences() {
		//Log.e(TAG, "Save Preferences Fired");
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor mEditor = prefs.edit();
		mEditor.putString("TCPAddress", TCPAddress);
		mEditor.commit();
	}
}