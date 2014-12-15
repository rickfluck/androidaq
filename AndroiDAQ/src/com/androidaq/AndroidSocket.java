package com.androidaq;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.androidaq.AndroiDAQTCPMain.MyTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
 
public class AndroidSocket {
	
	private static final String TAG = "TCPChatService";
    private static final boolean D = true;
    private int mState;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;

    //public static String SERVERIP = "192.168.0.9"; //your computer IP address
    public static String SERVERIP = null;
    public static final int SERVERPORT = 2000; //this is default port from RN171 radio

    private Socket mAdapter = null;
    PrintWriter out;
    BufferedReader in;
    
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

 
    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public AndroidSocket(MyTask myTask, Handler handler, String TCPAddress) {
        mState = STATE_NONE;
        mHandler = handler;
        SERVERIP = TCPAddress;
    }
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.e(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(AndroiDAQTCPMain.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.e(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }
    public synchronized void connect(Socket socket) {
        if (D) Log.e(TAG, "connect to: " + socket);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(socket);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(Socket socket) {
        if (D) Log.e(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mAdapter);
        mConnectedThread.start();
        setState(STATE_CONNECTED);
     
    }
    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.e(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(AndroiDAQTCPMain.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(AndroiDAQTCPMain.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(AndroiDAQTCPMain.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(AndroiDAQTCPMain.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        //private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
 
            // Create a new listening server socket
	        InetAddress serverAddr = null;
	        Log.e(TAG, "AcceptThread InetAddress.getByName(SERVERIP) is: " + SERVERIP);
			try {
				serverAddr = InetAddress.getByName(SERVERIP);
			} catch (UnknownHostException e) {
				connectionFailed();
			}  
	        //create a socket to make the connection with the server
	        try {
				mAdapter = new Socket(serverAddr, SERVERPORT);
			} catch (IOException e) {
				connectionFailed();
	            // Close the socket
	            try {
	            	if (mAdapter != null) {
	            		mAdapter.close();
	            	}
	            } catch (IOException e2) {
	                Log.e(TAG, "unable to close() socket during connection failure", e2);
	            }
			}
        }

        public void run() {
            if (D) Log.e(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            Boolean connected = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                	if (mAdapter != null) {
                		connected = mAdapter.isConnected();
                	} else {
                		connected = false;
                	}
                } catch (NullPointerException e) {
                	Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (connected != false) {
                    synchronized (AndroidSocket.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(mAdapter);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                            	mAdapter.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                } else {
                	setState(STATE_NONE);
                	break;
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.e(TAG, "cancel " + this);
            try {
            	mAdapter.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            } catch (NullPointerException e) {
            	
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {

        public ConnectThread(Socket socket) {

        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            // Make a connection to the Socket
            InetAddress serverAddr = null;
            Log.e(TAG, "ConnectThread InetAddress.getByName(SERVERIP) is: " + SERVERIP);
			try {
				serverAddr = InetAddress.getByName(SERVERIP);
			} catch (UnknownHostException e) {
				connectionFailed();
			}  
           
            //create a socket to make the connection with the server
            try {
				mAdapter = new Socket(serverAddr, SERVERPORT);
			} catch (IOException e) {
				connectionFailed();
                // Close the socket
                try {
                	mAdapter.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                AndroidSocket.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (AndroidSocket.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mAdapter);
        }

        public void cancel() {
            try {
            	mAdapter.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        //private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(Socket socket) {
            Log.e(TAG, "create ConnectedThread");
            mAdapter = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            InetAddress serverAddr = null;
            Log.e(TAG, "ConnectedThread InetAddress.getByName(SERVERIP) is: " + SERVERIP);
			try {
				serverAddr = InetAddress.getByName(SERVERIP);
			} catch (UnknownHostException e) {
				Log.e(TAG, "TCP Client connectionFailed UnknownHost");
				connectionFailed();
			}  
	        //Log.e("TCP Client", "C: Connecting...");
	        
	        //create a socket to make the connection with the server
	        try {
				mAdapter = new Socket(serverAddr, SERVERPORT);
			} catch (IOException e) {
				Log.e(TAG, "TCP Client connectionFailed IOException");
				connectionFailed();
	            // Close the socket
	            try {
	            	mAdapter.close();
	            } catch (IOException e2) {
	                Log.e(TAG, "unable to close() socket during connection failure", e2);
	            }
			}
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mAdapter.getInputStream();
                tmpOut = mAdapter.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    //Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(AndroiDAQTCPMain.MESSAGE_READ, readMessage)
                    .sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
        	
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(AndroiDAQTCPMain.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
            	mAdapter.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}