package com.khjxiaogu.TableGames.werewolf;

import com.khjxiaogu.TableGames.MessageListener.MsgType;
import com.khjxiaogu.TableGames.Utils;

import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.PlainText;

public class Seer extends Villager{

	public Seer(WerewolfGame werewolfGame, Member member) {
		super(werewolfGame, member);
	}

	@Override
	public void onTurn() {
		super.StartTurn();
		this.sendPrivate(game.getAliveList());
		super.sendPrivate("预言家，你可以查验一个人是否为狼人，请私聊选择查验的人，你有一分钟的考虑时间\n格式：“查验 qq号或者游戏号码”\n如：“查验 1”");
		Utils.registerListener(super.member,(msg,type)->{
			if(type!=MsgType.PRIVATE)return;
			String content=Utils.getPlainText(msg);
			if(content.startsWith("查验")) {
				try {
					Long qq=Long.parseLong(Utils.removeLeadings("查验",content).replace('号', ' ').trim());
					Villager p=game.getPlayerById(qq);
					if(p==null) {
						super.sendPrivate("选择的qq号或者游戏号码非游戏玩家，请重新输入");
						return;
					}
					if(p.isDead) {
						super.sendPrivate("选择的qq号或者游戏号码已死亡，请重新输入");
						return;
					}
					this.EndTurn();
					Utils.releaseListener(super.member.getId());
					super.sendPrivate(p.getMemberString()+"是"+p.getPredictorRole());
				}catch(Throwable t) {
					super.sendPrivate("发生错误，正确格式为：“查验 qq号或者游戏号码”！");
				}
			}
		});
	}
	@Override
	public int getTurn() {
		return 2;
	}
	@Override
	public Fraction getFraction() {
		return Fraction.God;
	}
	@Override
	public String getRole() {
		return "预言家";
	}
}