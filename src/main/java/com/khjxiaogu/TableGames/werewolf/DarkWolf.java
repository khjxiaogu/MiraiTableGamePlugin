package com.khjxiaogu.TableGames.werewolf;

import com.khjxiaogu.TableGames.MessageListener.MsgType;
import com.khjxiaogu.TableGames.utils.Utils;
import com.khjxiaogu.TableGames.werewolf.WerewolfGame.DiedReason;
import com.khjxiaogu.TableGames.werewolf.WerewolfGame.WaitReason;

import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.PlainText;

public class DarkWolf extends Werewolf {

	public DarkWolf(WerewolfGame werewolfGame, Member member) {
		super(werewolfGame, member);
	}

	@Override
	public String getRole() {
		return super.getRole();
	}

	boolean hasGun=true;
	boolean asked=false;
	@Override
	public boolean onDiePending(DiedReason dir) {
		super.StartTurn();
		dr=dir;
		if(!hasGun&&!asked)return false;
		if(dir!=DiedReason.Poison&&dir!=DiedReason.Explode) {
			this.sendPrivate(game.getAliveList());
			super.sendPrivate("狼王，你死了，你可以选择打死另一个人，你有30秒的考虑时间\n格式：“杀死 qq号或者游戏号码”\n如：“杀死 1”\n也可以放弃，格式：“放弃”");
			asked=true;
			Utils.registerListener(super.mid,(msg,type)->{
				if((dir==DiedReason.Vote||game.isFirstNight)&&type==MsgType.AT) {
					Utils.releaseListener(member.getId());
					game.skipWait(WaitReason.DieWord);
				}
				if(type!=MsgType.PRIVATE)return;
				String content=Utils.getPlainText(msg);
				if(content.startsWith("杀死")) {
					try {
						Long qq=Long.parseLong(Utils.removeLeadings("杀死",content).replace('号', ' ').trim());
						Villager p=game.getPlayerById(qq);
						if(p==null) {
							super.sendPrivate("选择的qq号或者游戏号码非游戏玩家，请重新输入");
							return;
						}
						if(p.isDead) {
							super.sendPrivate("选择的qq号或者游戏号码已死亡，请重新输入");
							return;
						}
						if(p.equals(this)) {
							super.sendPrivate("选择的qq号或者游戏号码是你自己，请重新输入");
							return;
						}
						this.EndTurn();
						Utils.releaseListener(super.member.getId());
						if(dir==DiedReason.Vote||dir==DiedReason.Explode)
							Utils.registerListener(super.member, (msgx,typex)->{
								if(typex==MsgType.AT) {
									Utils.releaseListener(member.getId());
									game.skipWait(WaitReason.DieWord);
								}
							});
						hasGun=false;
						super.sendPrivate("你杀死了"+p.getMemberString());
						super.sendPublic(new PlainText("死亡，同时带走了").plus(p.getAt()));
						if(dir==DiedReason.Vote||dir==DiedReason.Explode)
							game.scheduler.execute(()->p.onDied(DiedReason.DarkWolf));
						else {
							p.isDead=true;
							game.kill(p,DiedReason.DarkWolf);
						}
					}catch(Throwable t) {
						super.sendPrivate("发生错误，正确格式为：“杀死 qq号或者游戏号码”！");
					}
				}else if(content.startsWith("放弃")) {
					super.sendPrivate("你放弃了开枪！");
				}
			});
			return true;
		}
		return false;
	}

}
