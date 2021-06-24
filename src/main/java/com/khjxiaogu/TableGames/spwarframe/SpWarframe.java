package com.khjxiaogu.TableGames.spwarframe;

import com.khjxiaogu.TableGames.platform.AbstractPlayer;
import com.khjxiaogu.TableGames.platform.AbstractRoom;
import com.khjxiaogu.TableGames.spwarframe.role.Role;
import com.khjxiaogu.TableGames.utils.Game;
import com.khjxiaogu.TableGames.utils.Utils;
import com.khjxiaogu.TableGames.utils.WaitThread;

public class SpWarframe extends Game implements Platform {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8817274353799792732L;
	GameManager gm;
	WaitThread wt=new WaitThread();
	@Override
	public void forceShow(AbstractPlayer ct) {
		StringBuilder sb=new StringBuilder();
		for(Role r:gm.roles) {
			sb.append("\n").append(r.getPlayer()).append(" ").append(r.getName()).append(" ").append(r.isAlive()?"存活":"死亡");
			if(r.isBoss()) {
				sb.append(" 是境主");
			}
		}
		sb.append("\n游戏种子：").append(gm.seed);
		ct.sendPrivate(sb.toString());
	}

	@Override
	public boolean takeOverMember(long m, AbstractPlayer o) {
		Role r=gm.roles.get((int) m);
		r.bind(new MiraiPlayer(o));
		if(r.isBoss()) {
			r.getBr().bind(new MiraiPlayer(o));
		}
		return true;
	}

	@Override
	public void forceSkip() {
		wt.stopWait();
	}

	int crc=0;
	public SpWarframe(AbstractRoom group, int cplayer) {
		super(group, cplayer, 2);
		gm=new GameManager(this);
		gm.setMemberCount(cplayer/3*3);
	}

	@Override
	public void waitTime(long time) {
		wt.startWait(time);
	}

	@Override
	public void skipWait() {
		wt.stopWait();
	}

	@Override
	public void sendAll(String s) {
		super.getGroup().sendMessage(s);
	}

	@Override
	public void sendAllLong(String s) {
		super.getGroup().sendMessage(Utils.sendTextAsImage(s,super.getGroup()));
	}

	@Override
	public boolean addMember(AbstractPlayer mem) {
		synchronized(this) {
			if(crc>=gm.roles.size())
				return false;
			MiraiPlayer cur=new MiraiPlayer(mem);
			cur.setNumber(crc);
			Role r=gm.roles.get(crc++);
			r.bind(cur);
			if(r.isBoss()) {
				r.getBr().bind(cur);
			}
			mem.sendPrivate("已经报名");
			if(crc==gm.roles.size()) {
				super.getScheduler().execute(()->gm.start());
			}
			return true;
		}
	}

	@Override
	public void forceStart() {
		super.getScheduler().execute(()->gm.start());
	}

	@Override
	public String getName() {
		return "SP战纪";
	}

	@Override
	public boolean isAlive() {
		return gm.started;
	}

	@Override
	public boolean onReAttach(Long id) {
		return false;
	}

}
