package com.khjxiaogu.TableGames.spwarframe;

public interface Platform {
	void waitTime(long time);
	void skipWait();
	void sendAll(String s);
	void sendAllLong(String s);
}
