package au.com.jtribe.lejos;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import au.com.jtribe.lejos.service.InterfaceLPCCARemoteService;
import au.com.jtribe.lejos.service.LPCCARemoteService;

import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.main)
public class DroidNXTRemoteActivity extends Activity {

	private static final String TAG = DroidNXTRemoteActivity.class.getName();
	
	@ViewById Button connectButton;
	@ViewById Button startButton;
	@ViewById Button bindButton;

	private boolean boundToInterface = false;
	private boolean connectedToNXT = false;
			
	private InterfaceLPCCARemoteService myRemoteService;

	@Click(R.id.startButton)
	void startButtonPressed() {
		startService(new Intent(this, LPCCARemoteService.class));
		startButton.setEnabled(false);
	}
	
	@Click(R.id.bindButton)
	void bindButtonPressed() {
		bindService(new Intent(this, LPCCARemoteService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Click(R.id.connectButton)
	void connectButtonPressed() {
		requestNXTConnection();
	}
	
	@UiThread
	void renderConnectButton() {
		this.connectButton.setEnabled(!this.connectedToNXT);
	}
	
	@UiThread
	void renderBindButton() {
		this.bindButton.setEnabled(!this.boundToInterface);
	}
	
	@Background
	void requestNXTConnection() {
		if (myRemoteService != null) {
			try {
				myRemoteService.requestConnectionToNXT();
				connectedToNXT = true;
			} catch (RemoteException e) {
				Toast.makeText(DroidNXTRemoteActivity.this, "Error Connecting to NXT", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Exception connecting to NXT", e);
			}
		}
		renderConnectButton();
	}

	@Click(R.id.forwardButton)
	void forwardButtonPressed() {
		try {
			myRemoteService.get().forward();
		} catch (RemoteException e) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Forward Command Failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error sending forward command", e);
		}
	}
	
	@Click(R.id.backwardButton)
	void backwardButtonPressed() {
		try {
			myRemoteService.get().backward();
		} catch (RemoteException e) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Backward Command Failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error sending Backward command", e);
		}
	}
	
	@Click(R.id.leftButton)
	void leftButtonPressed() {
		try {
			myRemoteService.get().left();
		} catch (RemoteException e) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Left Command Failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error sending Left command", e);
		}
	}
	
	@Click(R.id.rightButton)
	void rightButtonPressed() {
		try {
			myRemoteService.get().right();
		} catch (RemoteException e) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Right Command Failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error sending Right command", e);
		}
	}

	private final ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Service Disconnected", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			boundToInterface = true;	
			Toast.makeText(DroidNXTRemoteActivity.this, "Service Connected", Toast.LENGTH_SHORT).show();
			myRemoteService = InterfaceLPCCARemoteService.Stub.asInterface(service);
			renderBindButton();
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (boundToInterface) {
			unbindService(serviceConnection);
		}
	};

}