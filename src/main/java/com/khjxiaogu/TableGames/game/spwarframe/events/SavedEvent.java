package com.khjxiaogu.TableGames.game.spwarframe.events;

import com.khjxiaogu.TableGames.game.spwarframe.role.Role;
import com.khjxiaogu.TableGames.game.spwarframe.skill.Skill;

public class SavedEvent extends SkillEvent {
	private DiedEvent saved;
	public DiedEvent getSaved() {
		return saved;
	}
	public void setSaved(DiedEvent saved) {
		this.saved = saved;
	}
	public SavedEvent(Role source, Role target, Skill skill) { super(source, target, skill); }

}
