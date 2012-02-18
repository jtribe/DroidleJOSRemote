package au.com.jtribe.lejos;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import au.com.jtribe.lejos.service.InterfaceLPCCARemoteService;
import au.com.jtribe.lejos.service.LPCCARemoteService;

public class AndroidBTConnectionActivity extends Activity {

	public static final int REQUEST_CHOICE_DEVICE = 200;
	public static final int REQUEST_SETTINGS = 300;
	public static final int REQUEST_CONNECT_DEVICE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	public static final int REQUEST_ENABLE_SCAN = 3;
	public static final int SCANTIME = 10;
	public static final int CONNECTION_ESTABLISHED = 42;
	public static final int CONNECTION_FAILED = 43;
	public static final int REQUEST_INIT = 23;
	public static int SUCCESS = 1;
	public static int CANCEL = 2;
	public static String LOGTAG = "LPCCA AndroidBTConnectionActivity";
	private String deviceKey;
	private Spinner deviceSpinner;
	private TextView textView;
	private Button connectBtn;
	private InterfaceLPCCARemoteService myRemoteService = null;
	private BroadcastReceiver myNXTFoundBCR;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(LOGTAG, "Activity AndroidBTConnection started.");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.deviceselection);
		connectBtn = (Button) findViewById(R.id.ConnectBtn);
		textView = (TextView) findViewById(R.id.TextView01);
		deviceSpinner = (Spinner) findViewById(R.id.Devices);
		connectBtn.setVisibility(View.INVISIBLE);
		deviceSpinner.setVisibility(View.INVISIBLE);
		textView.setText("Requesting bluetooth and populating device list.");
		connectBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onDeviceSelectClick();
			}
		});
		setup();
	}

	private void setup() {
		Log.d(LOGTAG, "Setup called, contacting service.");
		bindService(new Intent(this, LPCCARemoteService.class),
				serviceConnection, Context.BIND_AUTO_CREATE);
		myNXTFoundBCR = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if (LPCCARemoteService.NXT_FOUND.equals(intent.getAction())) {
					createDeviceList();
				}
			}
		};
		
		registerReceiver(myNXTFoundBCR, new IntentFilter(LPCCARemoteService.NXT_FOUND));
	}

	public void onDeviceSelectClick() {
		this.deviceKey = deviceSpinner.getSelectedItem().toString();
		this.setResult(CONNECTION_ESTABLISHED);
		try {
			myRemoteService.establishBTConnection(deviceKey);
		} catch (RemoteException e) {
			e.printStackTrace();
			this.setResult(CONNECTION_FAILED);
		}
		finish();
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		// @Override
		public void onServiceDisconnected(ComponentName name) {
		}

		// @Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			myRemoteService = InterfaceLPCCARemoteService.Stub
					.asInterface(service);
		}
	};

	private void createDeviceList() {
		List<String> devices;
		try {
			devices = myRemoteService.getAvailableDevicesList();
			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, devices.toArray(
							new String[devices.size()]));
			deviceSpinner.setAdapter(spinnerAdapter);
			textView.setText("Please select your NXT from the list below.");
			deviceSpinner.setVisibility(View.VISIBLE);
			connectBtn.setVisibility(View.VISIBLE);
		} catch (RemoteException e) {
			Log.e(LOGTAG, e.getMessage());
			e.printStackTrace();
		}
	}

	// @Override
	public void onDestroy() {
		unbindService(serviceConnection);
		unregisterReceivers();
		super.onDestroy();
	}

	public void unregisterReceivers() {
		if (myNXTFoundBCR != null) {
			unregisterReceiver(myNXTFoundBCR);
		}
	}
}
