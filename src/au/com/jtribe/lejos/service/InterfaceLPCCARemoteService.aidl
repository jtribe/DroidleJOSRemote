package  au.com.jtribe.lejos.service;
import  au.com.jtribe.lejos.service.Navigator;

interface InterfaceLPCCARemoteService {
	List<String> getAvailableDevicesList();
	void requestConnectionToNXT();
	void establishBTConnection(String deviceKey);
	void startDiscovery();
	Navigator get();
}