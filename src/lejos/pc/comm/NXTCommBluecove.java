package lejos.pc.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Implementation of NXTComm using the Bluecove libraries 
 * on Microsoft Windows. 
 * 
 * Should not be used directly - use NXTCommFactory to create
 * an appropriate NXTComm object for your system and the protocol
 * you are using.
 *
 */
public class NXTCommBluecove implements NXTComm {

	public static final String LOGTAG = "LPCCA NXTCommBluecove";
    private static Vector<NXTInfo> nxtInfos;
	private OutputStream os;
	private InputStream is;
	//private NXTInfo nxtInfo;
	private BluetoothDevice connecteddevice=null;
	private BluetoothSocket mmSocket;

	private boolean isopened=false;
	
	public static NXTCommBluecove instance;
	private BluetoothAdapter mBluetoothAdapter;
	
	public NXTCommBluecove(BluetoothAdapter mBluetoothAdapter){
		this.mBluetoothAdapter = mBluetoothAdapter;
	}

	public NXTInfo[] search(String name, int protocol) throws NXTCommException {
		
		nxtInfos = new Vector<NXTInfo>();
		if ((protocol & NXTCommFactory.BLUETOOTH) == 0){
			return new NXTInfo[0];
		}
		
		return nxtInfos.toArray(new NXTInfo[nxtInfos.size()]);
	}

	public BluetoothSocket getMmSocket() {
		return mmSocket;
	}

	//@Override
	public boolean open(NXTInfo nxt, int mode) throws NXTCommException {
		if (mode == RAW) throw new NXTCommException("RAW mode not implemented");
		// Construct URL if not present

		if (nxt.btResourceString == null || nxt.btResourceString.length() < 5
				|| !(nxt.btResourceString.substring(0, 5).equals("btspp"))) {
			nxt.btResourceString = "btspp://"
					+ stripColons(nxt.deviceAddress)
					+ ":1;authenticate=false;encrypt=false";
		}

		try {
			connecteddevice = mBluetoothAdapter.getRemoteDevice(nxt.deviceAddress);
			if(connecteddevice!=null){
				BluetoothSocket tmp =null;
			    // Get a BluetoothSocket to connect with the given BluetoothDevice
	            // MY_UUID is the app's UUID string, also used by the server code
	        	//00001101-0000-1000-8000-00805F9B34FB.
	        	UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	        	tmp = connecteddevice.createRfcommSocketToServiceRecord(uuid);

		    	this.mmSocket=tmp;
		    	Log.d(LOGTAG, "trying BTsocket.connect().");
		    	this.mmSocket.connect();
		    	Log.d(LOGTAG, "finished BTsocket.connect().");
		    	instance=this;
			}
	    	Log.d(LOGTAG, "setting outputstream");
			os = this.mmSocket.getOutputStream();
	    	Log.d(LOGTAG, "setting inputstream");
			is = this.mmSocket.getInputStream();
			nxt.connectionState = (mode == LCP ? NXTConnectionState.LCP_CONNECTED : NXTConnectionState.PACKET_STREAM_CONNECTED);
	    	Log.d(LOGTAG, "setting isopened to true");
	    	isopened=true;
			return true;
		} catch (IOException e) {
	    	Log.d(LOGTAG, "exception during connect: "+e.getMessage());
			nxt.connectionState = NXTConnectionState.DISCONNECTED;
			throw new NXTCommException("Open of " + nxt.name + " failed: " + e.getMessage());
		}
	}

    public boolean open(NXTInfo nxt) throws NXTCommException
    {
        return open(nxt, PACKET);
    }

	public void close() throws IOException {
		if (os != null){
			os.close();
			os=null;
		}
		if (is != null){
			is.close();
			is=null;
		}
		if (mmSocket != null){
			mmSocket.close();
			mmSocket=null;
		}
		this.isopened=false;
	}

	/**
	 * Sends a request to the NXT brick.
	 * 
	 * @param message
	 *            Data to send.
	 */
	public synchronized byte[] sendRequest(byte[] message, int replyLen)
			throws IOException {

		// length of packet (Least and Most significant byte)
		// * NOTE: Bluetooth only. 
		int LSB = message.length;
		int MSB = message.length >>> 8;

		if (os == null)
			return new byte[0];

		// Send length of packet:
		os.write((byte) LSB);
		os.write((byte) MSB);

		os.write(message);
		os.flush();

		if (replyLen == 0)
			return new byte[0];

		byte[] reply = null;
		int length = -1;

		if (is == null)
			return new byte[0];

		do {
			length = is.read(); // First byte specifies length of packet.
		} while (length < 0);

		int lengthMSB = is.read(); // Most Significant Byte value
		length = (0xFF & length) | ((0xFF & lengthMSB) << 8);
		reply = new byte[length];
		int len = is.read(reply);
		if (len != replyLen) throw new IOException("Unexpected reply length");

		return (reply == null) ? new byte[0] : reply;
	}

	public byte[] read() throws IOException {

        int lsb = is.read();
		if (lsb < 0) return null;
		int msb = is.read();
        if (msb < 0) return null;
        int len = lsb | (msb << 8);
		byte[] bb = new byte[len];
		for (int i=0;i<len;i++) bb[i] = (byte) is.read();

		return bb;
	}
	
    public int available() throws IOException {
        return 0;
    }

	public void write(byte[] data) throws IOException {
        os.write((byte)(data.length & 0xff));
        os.write((byte)((data.length >> 8) & 0xff));
		os.write(data);
		os.flush();
	}

	public OutputStream getOutputStream() {
		return new NXTCommOutputStream(this);
	}

	public InputStream getInputStream() {
		return new NXTCommInputStream(this);
	}
	
	public InputStream getRAWInputStream() {
		return is;
	}
	
	public void setInputStream(InputStream is) {
		this.is = is;
	}

	public String stripColons(String s) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c != ':') {
				sb.append(c);
			}
		}

		return sb.toString();
	}
	
	public boolean isOpened() {
		if(this.mmSocket==null){
			return false;
		}
		return isopened; 
	}
}
