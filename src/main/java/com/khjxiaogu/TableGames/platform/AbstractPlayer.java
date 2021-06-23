package com.khjxiaogu.TableGames.platform;

import com.khjxiaogu.TableGames.Game;
import com.khjxiaogu.TableGames.platform.message.IMessage;
import com.khjxiaogu.TableGames.platform.mirai.MiraiListenerUtils;
import com.khjxiaogu.TableGames.utils.MessageListener;


public interface AbstractPlayer {
	void sendPrivate(String str);
	void sendPrivate(IMessage msg);
	void sendPublic(String str);
	void sendPublic(IMessage msg);
	IMessage getAt();
	String getMemberString();
	void setNameCard(String s);
	String getNameCard();
	void tryMute();
	void tryUnmute();
	long getId();
	void bind(Object obj);
	void setGame(Game g);
	void registerListener(MessageListener msgc);
	void releaseListener();
}
