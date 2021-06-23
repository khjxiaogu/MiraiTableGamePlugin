package com.khjxiaogu.TableGames.undercover;

import com.khjxiaogu.TableGames.platform.AbstractRoom;
import com.khjxiaogu.TableGames.utils.PreserveInfo;


public class UnderCoverPreserve extends PreserveInfo<UnderCoverGame> {

	public UnderCoverPreserve(AbstractRoom g) {
		super(g);
	}

	@Override
	protected int getSuitMembers() {
		return 7;
	}

	@Override
	protected int getMinMembers() {
		return 5;
	}

	@Override
	protected int getMaxMembers() {
		return 9;
	}

	@Override
	protected Class<UnderCoverGame> getGameClass() {
		return UnderCoverGame.class;
	}

	@Override
	public String getName() {
		return "谁是卧底";
	}

}
