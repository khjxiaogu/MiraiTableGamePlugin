package com.khjxiaogu.TableGames.game.werewolf;

import com.khjxiaogu.TableGames.platform.AbstractUser;

public class Coroner extends Villager {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	boolean hasKnown=false;
	public Coroner(WerewolfGame game, AbstractUser p) {
		super(game, p);
	}

	@Override
	public Fraction getRealFraction() {
		return Fraction.God;
	}

	@Override
	public void onTurn() {
		StringBuilder sb = new StringBuilder("昨晚死者名单：");
		if (!game.getTokill().isEmpty()) {
			for (Villager me : game.getTokill()) {
				game.logger.logSkill(this, me, "验尸官查验");
				sb.append("\n").append(me.getMemberString()).append(" 是")
				.append(me.getPredictorFraction() == Fraction.Wolf ? "狼人" : "好人").append("，死于")
				.append(me.getEffectiveDiedReason().desc);
				hasKnown=true;
			}
		} else {
			sb.append("昨晚无死者");
		}
		sendPrivate(sb.toString());
	}

	@Override
	public double onVotedAccuracy() {
		if(hasKnown)
			return 0.4;
		return super.onVotedAccuracy();
	}

	@Override
	public double onSkilledAccuracy() {
		if(hasKnown)
			return 0.45;
		return super.onSkilledAccuracy();
	}

	@Override
	public String getJobDescription() {
		return "你属于神阵营，你每天白天前可以知道每晚所有死者的最终死因和阵营。";
	}

	@Override
	public String getRole() {
		return "验尸官";
	}

	@Override
	public int getTurn() {
		return 4;
	}
}
