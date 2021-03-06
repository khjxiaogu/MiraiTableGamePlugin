package com.khjxiaogu.TableGames.platform;

import com.khjxiaogu.TableGames.platform.message.IMessageCompound;

@FunctionalInterface
public interface MessageListener{
	public enum MsgType{
		AT,
		PRIVATE,
		PUBLIC,
	}
	void handle(IMessageCompound msg,MsgType type);
}