package au.com.jtribe.lejos;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;
import au.com.jtribe.lejos.service.InterfaceLPCCARemoteService;
import au.com.jtribe.lejos.service.LPCCARemoteService;

import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Touch;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.main)
public class DroidNXTRemoteActivity extends Activity {

	private static final String TAG = DroidNXTRemoteActivity.class.getName();

	@ViewById
	Button connectButton;
	@ViewById
	Button startButton;
	@ViewById
	Button bindButton;

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

	@Touch(R.id.forwardButton)
	void forwardButtonPressed(MotionEvent event) {

		try {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				myRemoteService.get().forward();
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				myRemoteService.get().stop();
			}

		} catch (RemoteException e) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Forward Command Failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error sending forward command", e);
		}
	}

	@Touch(R.id.backwardButton)
	void backwardButtonPressed(MotionEvent event) {
		try {
			
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				myRemoteService.get().backward();
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				myRemoteService.get().stop();
			}

		} catch (RemoteException e) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Backward Command Failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error sending Backward command", e);
		}
	}

	@Touch(R.id.leftButton)
	void leftButtonPressed(MotionEvent event) {
		try {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				myRemoteService.get().left();
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				myRemoteService.get().stop();
			}

		} catch (RemoteException e) {
			Toast.makeText(DroidNXTRemoteActivity.this, "Left Command Failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error sending Left command", e);
		}
	}

	@Touch(R.id.rightButton)
	void rightButtonPressed(MotionEvent event) {
		try {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				myRemoteService.get().right();
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				myRemoteService.get().stop();
			}
			
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