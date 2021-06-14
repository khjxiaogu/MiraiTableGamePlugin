package com.khjxiaogu.TableGames.werewolf;

import com.khjxiaogu.TableGames.AbstractPlayer;
import com.khjxiaogu.TableGames.utils.ListenerUtils;
import com.khjxiaogu.TableGames.utils.MessageListener.MsgType;
import com.khjxiaogu.TableGames.utils.Utils;

import net.mamoe.mirai.contact.Member;

public class StatueDemon extends Villager {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public StatueDemon(WerewolfGame game, AbstractPlayer p) {
		super(game, p);
	}

	public StatueDemon(WerewolfGame werewolfGame, Member member) {
		super(werewolfGame, member);
	}

	@Override
	public String getJobDescription() {
		return "你属于狼人阵营，你每晚可以查验一个人，不能重复查或者查自己。若场上所有其他狼人死亡，则你可以杀人。";
	}
	@Override
	public boolean canWolfTurn() {
		for (Villager inno : game.playerlist) {
			if (inno instanceof Werewolf && !inno.isDead)
				return false;
		}
		for (Villager inno : game.playerlist) {
			if (inno == this) {
				return true;
			}
			if (inno.getFraction() == Fraction.Wolf && !inno.isDead)
				return false;
		}
		return true;
	}
	@Override
	public void onTurn() {
		super.StartTurn();
		sendPrivate(game.getAliveList());
		super.sendPrivate("石像鬼，你可以查验一个人的身份，请私聊选择查验的人，你有30秒的考虑时间\n格式：“查验 qq号或者游戏号码”\n如：“查验 1”");
		ListenerUtils.registerListener(super.getId(), (msg, type) -> {
			if (type != MsgType.PRIVATE)
				return;
			String content = Utils.getPlainText(msg);
			if (content.startsWith("查验")) {
				try {
					Long qq = Long.parseLong(Utils.removeLeadings("查验", content).replace('号', ' ').trim());
					Villager p = game.getPlayerById(qq);
					if (p == null) {
						super.sendPrivate("选择的qq号或者游戏号码非游戏玩家，请重新输入");
						return;
					}
					if (p.isDead) {
						super.sendPrivate("选择的qq号或者游戏号码已死亡，请重新输入");
						return;
					}
					if (p == this) {
						super.sendPrivate("不能查验自己！");
						return;
					}
					if (p.isDemonChecked) {
						super.sendPrivate("此玩家已经被查验过！");
						return;
					}
					EndTurn();
					ListenerUtils.releaseListener(super.getId());
					game.logger.logSkill(this, p, "石像鬼查验");
					p.isDemonChecked = true;
					super.sendPrivate(p.getMemberString() + "是" + p.getPredictorRole());
				} catch (Throwable t) {
					super.sendPrivate("发生错误，正确格式为：“查验 qq号或者游戏号码”！");
				}
			}
		});
	}

	@Override
	public double onVotedAccuracy() {
		return 1.25;
	}

	@Override
	public double onSkilledAccuracy() {
		return 1.25;
	}

	@Override
	public void onWolfTurn() {
		for (Villager inno : game.playerlist) {
			if (inno instanceof Werewolf && !inno.isDead)
				return;
		}
		for (Villager inno : game.playerlist) {
			if (inno == this) {
				break;
			}
			if (inno.getFraction() == Fraction.Wolf && !inno.isDead)
				return;
		}
		StartTurn();
		sendPrivate(game.getAliveList());
		super.sendPrivate(game.getWolfSentence());
		game.vu.addToVote(this);
		ListenerUtils.registerListener(super.getId(), (msg, type) -> {
			if (type != MsgType.PRIVATE)
				return;
			String content = Utils.getPlainText(msg);
			if (content.startsWith("投票")) {
				try {
					Long qq = Long.parseLong(Utils.removeLeadings("投票", content).replace('号', ' ').trim());
					Villager p = game.getPlayerById(qq);
					if (p == null) {
						super.sendPrivate("选择的qq号或者游戏号码非游戏玩家，请重新输入");
						return;
					}
					if (p.isDead) {
						super.sendPrivate("选择的qq号或者游戏号码已死亡，请重新输入");
						return;
					}
					EndTurn();
					ListenerUtils.releaseListener(super.getId());
					game.WolfVote(this, p);
					game.logger.logSkill(this, p, "狼人投票");
					super.sendPrivate("已投票给 " + p.getMemberString());
				} catch (Throwable t) {
					super.sendPrivate("发生错误，正确格式为：“投票 qq号或者游戏号码”！");
				}
			} else if (content.startsWith("放弃")) {
				EndTurn();
				ListenerUtils.releaseListener(super.getId());
				game.NoVote(this);
				super.sendPrivate("已放弃");
			}
		});
	}

	@Override
	public int getTurn() {
		return 2;
	}

	@Override
	public Fraction getFraction() {
		return Fraction.Wolf;
	}

	@Override
	public String getRole() {
		return "石像鬼";
	}

	@Override
	public String getPredictorRole() {
		return "狼人";
	}

}
