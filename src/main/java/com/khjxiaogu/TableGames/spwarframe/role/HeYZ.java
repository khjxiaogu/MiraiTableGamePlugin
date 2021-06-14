package com.khjxiaogu.TableGames.spwarframe.role;

import java.util.List;

import com.khjxiaogu.TableGames.spwarframe.GameManager;
import com.khjxiaogu.TableGames.spwarframe.Exceptions.InvalidSkillParamException;
import com.khjxiaogu.TableGames.spwarframe.Exceptions.SkillException;
import com.khjxiaogu.TableGames.spwarframe.events.DiedEvent;
import com.khjxiaogu.TableGames.spwarframe.events.KillEvent;
import com.khjxiaogu.TableGames.spwarframe.events.SkillEvent;
import com.khjxiaogu.TableGames.spwarframe.role.WangXH.Pathing;
import com.khjxiaogu.TableGames.spwarframe.skill.Skill;
import com.khjxiaogu.TableGames.spwarframe.skill.SkillType;

public class HeYZ extends Role {
	static class Pathless extends Skill {

		public Pathless(GameManager game, Role owner) {
			super(game, owner);
			owner.listen(SkillEvent.class, event -> {
				if (event.getSkill() instanceof Pathing) {
					event.reject();
				}
			});
		}

		@Override
		public String getName() {
			return "无法指路";
		}

		@Override
		public String getDesc() {
			return "无法被指路";
		}

		@Override
		protected boolean onSkillUse(List<String> params) {
			return false;
		}

		@Override
		public SkillType getType() {
			return SkillType.PASSIVE;
		}

		@Override
		public int getMaxRemain() {
			return 0;
		}

	}

	static class Calm extends Skill {
		@Override
		public String[] getSpecialParamType() {
			return new String[] { "游戏号码" };
		}

		public Calm(GameManager game, Role owner) {
			super(game, owner);
		}

		@Override
		public String getName() {
			return "镇定剂";
		}

		@Override
		public String getDesc() {
			return "让一个玩家下个回合无法使用技能";
		}

		@Override
		protected boolean onSkillUse(List<String> params) throws SkillException {
			if (params.size() < 1)
				throw new InvalidSkillParamException(this);
			Role target = game.getSkillPlayerByName(params.get(0));
			target.ensureAlive();
			return super.fireEvent(new SkillEvent(owner, target, this).setMainEvent(true).andThen((room, ev) -> {
				((SkillEvent) ev).getTarget().getAllSkills().forEach(sk -> sk.addLocked(1));
			}));
		}

		@Override
		public SkillType getType() {
			return SkillType.SPECIAL;
		}

		@Override
		public int getMaxRemain() {
			return 1;
		}

	}

	static class Guard extends Skill {
		@Override
		public String[] getSpecialParamType() {
			return new String[] { "游戏号码" };
		}

		public Guard(GameManager game, Role owner) {
			super(game, owner);
		}

		@Override
		public String getName() {
			return "守护";
		}

		@Override
		public String getDesc() {
			return "保护一个女性角色，在接下来的游戏中代替她死亡";
		}

		@Override
		protected boolean onSkillUse(List<String> params) throws SkillException {
			if (params.size() < 1)
				throw new InvalidSkillParamException(this);
			Role target = game.getSkillPlayerByName(params.get(0));
			target.ensureAlive();
			return super.fireEvent(
					new SkillEvent(owner, target, this).setMainEvent(true).setPriority(5).andThen((room, ev) -> {
						((SkillEvent) ev).getTarget().listen(DiedEvent.class, evt -> {
							if (((SkillEvent) ev).getSource().isAlive()) {
								DiedEvent evx = new DiedEvent(((SkillEvent) ev).getSource());
								for (KillEvent evk : evt.getKillBy()) {
									evx.populateKill(new KillEvent(evk.getSource(), evk.getTarget(), evk.getSkill()));
									evk.cancel();
								}
								super.fireEvent(evx);
							}
						});

					}));
		}

		@Override
		public SkillType getType() {
			return SkillType.SPECIAL;
		}

		@Override
		public int getMaxRemain() {
			return 1;
		}

	}

	public HeYZ(GameManager room) {
		super(room);
		new Pathless(room, this);
		new Calm(room, this);
		new Guard(room, this);
	}

	@Override
	public String getName() {
		return "贺云舟";
	}

	@Override
	public boolean isMale() {
		return true;
	}

}
