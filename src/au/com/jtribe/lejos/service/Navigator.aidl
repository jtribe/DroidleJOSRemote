package  au.com.jtribe.lejos.service;

interface Navigator {
	void forward();
	void left();
	void right();
	void stop();
	void backward();
	boolean connected();
}