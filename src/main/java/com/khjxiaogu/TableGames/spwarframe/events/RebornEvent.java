package com.khjxiaogu.TableGames.spwarframe.events;

import com.khjxiaogu.TableGames.spwarframe.GameManager;
import com.khjxiaogu.TableGames.spwarframe.role.Role;
import com.khjxiaogu.TableGames.spwarframe.skill.Skill;

public class RebornEvent extends SavedEvent {

	public RebornEvent(Role source, Role target, Skill skill) { super(source, target, skill); }

	@Override
	protected void doExecute(GameManager room) {
		super.doExecute(room);
	}

}