package com.khjxiaogu.TableGames;

import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;

public class Player implements AbstractPlayer{
	private AbstractPlayer member;
	public Player(Member member) {
		this.member = new HumanPlayer(member);
	}
	public Player(AbstractPlayer p) {
		this.member = p;
	}
	public void sendPrivate(String str) {
		member.sendPrivate(str);
	}
	public void sendPublic(String str) {
		member.sendPublic(str);
	}
	public void sendPublic(Message msg) {
		member.sendPublic(msg);
	}
	public Message getAt() {
		return member.getAt();
	}
	public String getMemberString() {
		return member.getMemberString();
	}
	public void setNameCard(String s) {
		member.setNameCard(s);
	}
	public String getNameCard() {
		return member.getNameCard();
	}
	public void tryMute() {
		member.tryMute();
	}
	public void tryUnmute() {
		member.tryUnmute();
	}
	public long getId() {
		return member.getId();
	}
}

