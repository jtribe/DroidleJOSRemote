package au.com.jtribe.lejos.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import lejos.nxt.Motor;
import lejos.pc.comm.NXTCommBluecove;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import au.com.jtribe.lejos.AndroidBTConnectionActivity;

public class LPCCARemoteService extends Service {

	public static final String NXT_FOUND = "org.raesch.java.lpcca.service.NXT_FOUND";

	protected static final String LEGO_NXT_ID = "00:16:53";
	
	static String LOGTAG = "LPCCA RemoteService";
	private BluetoothAdapter mBluetoothAdapter;
	public static NXTCommBluecove myNXTCommBluecove;
	private BroadcastReceiver bluetoothDeviceFoundBroadcastReceiver = null;
	private HashSet<BluetoothDevice> bluetoothDevices = new HashSet<BluetoothDevice>();

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOGTAG, "Service created.");
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(LOGTAG, "No bluetooth adapter found.");
		} else {
			Log.d(LOGTAG, "Bluetooth adapter found.");
			myNXTCommBluecove = new NXTCommBluecove(mBluetoothAdapter);
		}

		if (bluetoothDeviceFoundBroadcastReceiver == null) {
			// Create a BroadcastReceiver for ACTION_FOUND
			bluetoothDeviceFoundBroadcastReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					// When discovery finds a device
					if (BluetoothDevice.ACTION_FOUND.equals(action)) {
						// Get the BluetoothDevice object from the Intent
						BluetoothDevice device = intent
								.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						// Add the name and address to an array adapter to show
						// in a ListView
						if (device.getAddress().startsWith(LEGO_NXT_ID)) {
							bluetoothDevices.add(device);
							Log.d(LOGTAG,
									"Added discovered NXT: "
											+ device.getName() + ", "
											+ device.getAddress());
							broadcastNXTFound();
						}
					}
				}
			};
			// Register the BroadcastReceiver
			registerReceiver(bluetoothDeviceFoundBroadcastReceiver,
					new IntentFilter(BluetoothDevice.ACTION_FOUND));
		}
	}

	protected void broadcastNXTFound() {
		Intent nXTFoundIntent = new Intent();
		nXTFoundIntent.setAction(NXT_FOUND);
		sendBroadcast(nXTFoundIntent);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(LOGTAG, "Received bind request: " + arg0.toString());
		return myRemoteService;
	}

	private final InterfaceLPCCARemoteService.Stub myRemoteService = new InterfaceLPCCARemoteService.Stub() {

		// @Override
		public List<String> getAvailableDevicesList() throws RemoteException {
			// TODO activate bluetooth, get list, display paired devices (or
			// others?)
			List<String> bluetoothDeviceNames = new ArrayList<String>();
			for (BluetoothDevice btd : bluetoothDevices) {
				bluetoothDeviceNames.add(btd.getName());
			}
			return bluetoothDeviceNames;
		}

		/*
		 * (non-Javadoc) This will establish the bluetooth connection to the
		 * specified device given by key & mac.
		 * 
		 * @see org.raesch.java.lpcca.service.InterfaceLPCCARemoteService#
		 * establishBTConnection(java.lang.String, java.lang.String)
		 */
		public void establishBTConnection(String deviceKey) {
			if (deviceKey != null) {
				mBluetoothAdapter.cancelDiscovery();
				String deviceMac;
				for (BluetoothDevice btd : bluetoothDevices) {
					if(deviceKey.equals(btd.getName())){
						deviceMac = btd.getAddress();
						Log.d(LOGTAG, "Trying to establish connection with: " + btd.getName());
						NXTInfo nxtInfo = new NXTInfo(NXTCommFactory.BLUETOOTH,
								deviceKey, deviceMac);
						try {
							myNXTCommBluecove.open(nxtInfo);
						} catch (NXTCommException e) {
						}
						break;
					}
				}
			}
		}

		// @Override
		/*
		 * (non-Javadoc) This will open the activity so the user can choose a
		 * device he wants to connect to.
		 * 
		 * @see org.raesch.java.lpcca.service.InterfaceLPCCARemoteService#
		 * requestConnectionToNXT()
		 */
		public void requestConnectionToNXT() throws RemoteException {
			Log.d(LOGTAG, "Starting Activity for choice of NXTs.");
			Intent remoteIntent = new Intent(getBaseContext(),
					AndroidBTConnectionActivity.class);
			remoteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplication().startActivity(remoteIntent);
			startDiscovery();
		}

		// @Override
		public Navigator get() throws RemoteException {
			// only allow use of the navigator if the connection is fully
			// established.
			if (myNXTCommBluecove != null && myNXTCommBluecove.isOpened()) {
				Log.d(LOGTAG,
						"Returning navigator, connection fully established.");
				return myRemoteNavigator;
			} else {
				Log.d(LOGTAG,
						"Not returning navigator, connection not fully established.");
				String error = "Something wrong happened";
				if (myNXTCommBluecove == null) {
					error = "NXTCommBluecove == null";
				} else if (!myNXTCommBluecove.isOpened()) {
					error = "NXTCommBluecove not open";
				}
				Log.d(LOGTAG, error);
				throw new RemoteException();
			}
		}

		// @Override
		public void startDiscovery() throws RemoteException {
			if (!mBluetoothAdapter.isEnabled()) {
				Log.d(LOGTAG,
						"Bluetooth adapter not enabled, requesting enabling.");
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(enableBtIntent);
			} else {
				Log.d(LOGTAG, "BTAdapter found and enabled.");
			}
			Log.d(LOGTAG, "starting discovery");
			mBluetoothAdapter.startDiscovery();
		}
	};

	Navigator.Stub myRemoteNavigator = new Navigator.Stub() {
		int count = 250;

		// @Override
		public void forward() throws RemoteException {
			if (myNXTCommBluecove != null && myNXTCommBluecove.isOpened()) {
				Log.d(LOGTAG, "Moving motor A and B forward");
				Motor.A.forward();
				Motor.B.forward();
				
				//Motor.A.rotate(count, true);
				//Motor.B.rotate(count, true);
			}
		}

		// @Override
		public void left() throws RemoteException {
			if (myNXTCommBluecove != null && myNXTCommBluecove.isOpened()) {
				// Motor.A.backward();
				// Motor.B.forward();
				Motor.A.rotate(-count, true);
				Motor.B.rotate(count, true);
			}
		}

		// @Override
		public void right() throws RemoteException {
			if (myNXTCommBluecove != null && myNXTCommBluecove.isOpened()) {
				// Motor.A.forward();
				// Motor.B.backward();
				Motor.A.rotate(count, true);
				Motor.B.rotate(-count, true);
			}
		}

		// @Override
		public void stop() throws RemoteException {
			if (myNXTCommBluecove != null && myNXTCommBluecove.isOpened()) {
				Motor.A.stop();
				Motor.B.stop();
			}
		}

		// @Override
		public void backward() throws RemoteException {
			if (myNXTCommBluecove != null && myNXTCommBluecove.isOpened()) {
				// Motor.A.backward();
				// Motor.B.backward();
				Motor.A.rotate(-count, true);
				Motor.B.rotate(-count, true);
			}
		}

		// @Override
		public boolean connected() throws RemoteException {
			if (myNXTCommBluecove != null && myNXTCommBluecove.isOpened()) {
				return true;
			} else {
				return false;
			}
		}
	};

	@Override
	public void onDestroy() {
		unregisterReceivers();
		super.onDestroy();
	}

	public void unregisterReceivers() {
		if (bluetoothDeviceFoundBroadcastReceiver != null) {
			unregisterReceiver(bluetoothDeviceFoundBroadcastReceiver);
		}
	}
}
