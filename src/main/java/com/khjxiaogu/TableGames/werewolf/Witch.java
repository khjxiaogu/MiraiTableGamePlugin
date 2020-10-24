package com.khjxiaogu.TableGames.werewolf;

import com.khjxiaogu.TableGames.MessageListener.MsgType;
import com.khjxiaogu.TableGames.utils.ListenerUtils;
import com.khjxiaogu.TableGames.utils.Utils;
import com.khjxiaogu.TableGames.werewolf.WerewolfGame.DiedReason;

import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.PlainText;

public class Witch extends Villager {
	boolean hasPoison=true;
	boolean hasHeal=true;
	public Witch(WerewolfGame werewolfGame, Member member) {
		super(werewolfGame, member);
	}
	
	@Override
	public void onTurn() {
		super.StartTurn();
		if(!hasHeal&&!hasPoison) {
			super.sendPrivate("女巫，你没有药了。");
			return;
		}
		this.sendPrivate(game.getAliveList());
		StringBuilder sb=new StringBuilder("女巫，你有");
		if(hasPoison) {
			sb.append("一瓶毒药，可以杀死一个人，格式：“毒 qq号或者游戏号码”\n");
		}
		if(hasHeal) {
			sb.append("一瓶解药，可以救活一个人，格式：“救 qq号或者游戏号码”\n");
			sb.append("今晚死亡情况是：\n");
			if(game.tokill.isEmpty()) {
				sb.append("今晚没有人死亡");
			}else {
				for(Villager p:game.tokill.keySet()) {
					sb.append(p.getMemberString());
					sb.append("\n");
				}
			}
		}
		sb.append("你可以使用其中一瓶\n如：“救 1”，\n");
		sb.append("你有一分钟的考虑时间。\n如果不需要使用药，无需发送任何内容，等待时间结束即可。");
		super.sendPrivate(sb.toString());
		ListenerUtils.registerListener(super.member,(msg,type)->{
			if(type!=MsgType.PRIVATE)return;
			String content=Utils.getPlainText(msg);
			if(hasPoison&&content.startsWith("毒")) {
				try {
					Long qq=Long.parseLong(Utils.removeLeadings("毒",content).replace('号', ' ').trim());
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
					ListenerUtils.releaseListener(super.member.getId());
					if(p instanceof NightmareKnight) {
						NightmareKnight nk=(NightmareKnight) p;
						if(!nk.isSkillUsed) {
							nk.isSkillUsed=true;
							game.kill(this, DiedReason.Reflect);
						}
					}
					game.kill(p,DiedReason.Poison);
					game.logger.logSkill(this,p,"女巫毒");
					hasPoison=false;
					super.sendPrivate("毒死了"+p.getMemberString());
				}catch(Throwable t) {
					super.sendPrivate("发生错误，正确格式为：“毒 qq号或者游戏号码”！");
				}
			}else if(hasHeal&&content.startsWith("救")) {
				try {
					Long qq=Long.parseLong(Utils.removeLeadings("救",content).replace('号', ' ').trim());
					Villager p=game.getPlayerById(qq);
					if(p==null) {
						super.sendPrivate("选择的qq号或者游戏号码非游戏玩家，请重新输入");
						return;
					}
					if(p.isDead) {
						super.sendPrivate("选择的qq号或者游戏号码已死亡，请重新输入");
						return;
					}
					if(!game.tokill.containsKey(p)||game.tokill.get(p)!=DiedReason.Wolf) {
						super.sendPrivate("选择的qq号或者游戏号码没有死亡危险，请重新输入。");
						return;
					}
					if((!game.isFirstNight)&&p==this) {
						super.sendPrivate("你今晚无法自救！");
						return;
					}
					this.EndTurn();
					ListenerUtils.releaseListener(super.member.getId());
					p.isSavedByWitch=true;
					hasHeal=false;
					game.logger.logSkill(this,p,"女巫救");
					super.sendPrivate("救活了"+p.getMemberString());
				}catch(Throwable t) {
					super.sendPrivate("发生错误，正确格式为：“救 qq号或者游戏号码”！");
				}
			}
		});
		return;
	}
	@Override
	public Fraction getFraction() {
		return Fraction.God;
	}
	@Override
	public int getTurn() {
		return 2;
	}
	@Override
	public String getRole() {
		return "女巫";
	}
}
