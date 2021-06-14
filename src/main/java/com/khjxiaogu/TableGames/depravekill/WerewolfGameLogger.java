package com.khjxiaogu.TableGames.depravekill;

import com.khjxiaogu.TableGames.utils.ImagePrintStream;
import com.khjxiaogu.TableGames.depravekill.WerewolfGame.DiedReason;

import net.mamoe.mirai.contact.Contact;

public class WerewolfGameLogger {
	private ImagePrintStream gamelog=new ImagePrintStream();
	public WerewolfGameLogger() {
	}
	public void logRaw(String s) {
		gamelog.println(s);
	}
	public void logSkill(Villager from,Villager to,String name) {
		logSkill(from.getNameCard(),to,name);
	}
	public void logSkill(String from,Villager to,String name) {
		gamelog.append(from).append(" ").append(name).append("了 ").append(to.getNameCard()).println();
	}
	public void logVote(Villager from,Villager to) {
		gamelog.append(from.getNameCard()).append(" 投票给").append(to.getNameCard()).println();
	}
	public void logDeath(Villager to,DiedReason dr){
		gamelog.append(to.getNameCard()).append(" ").append(dr.desc).println();
	}
	public void title(String name) {
		gamelog.append("========").append(name).append("========").println();
	}
	public void logTurn(int day,String name) {
		gamelog.append("========第").append(day).append("天").append(name).append("========").println();
	}
	public void sendLog(Contact ct) {
		ct.sendMessage(ct.uploadImage(gamelog.asImage()));
	}
}
