package com.khjxiaogu.TableGames.game.werewolf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.khjxiaogu.TableGames.data.PlayerDatabase.GameData;
import com.khjxiaogu.TableGames.game.werewolf.bots.BearBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.DarkWolfBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.DeadBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.ElderBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.GenericBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.GraveKeeperBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.HunterBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.IdiotBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.SeerBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.WereWolfBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.WitchBot;
import com.khjxiaogu.TableGames.game.werewolf.bots.WolfKillerBot;
import com.khjxiaogu.TableGames.platform.AbstractUser;
import com.khjxiaogu.TableGames.platform.AbstractRoom;
import com.khjxiaogu.TableGames.platform.BotUser;
import com.khjxiaogu.TableGames.platform.GlobalMain;
import com.khjxiaogu.TableGames.platform.message.MessageCompound;
import com.khjxiaogu.TableGames.platform.mirai.MiraiMain;
import com.khjxiaogu.TableGames.utils.Game;
import com.khjxiaogu.TableGames.utils.GameUtils;

import com.khjxiaogu.TableGames.utils.ParamUtils;
import com.khjxiaogu.TableGames.utils.Utils;
import com.khjxiaogu.TableGames.utils.VoteHelper;
import com.khjxiaogu.TableGames.utils.WaitThread;


public class WerewolfGame extends Game implements Serializable{

	private static final long serialVersionUID = 7731234732322205712L;

	public enum DiedReason {
		Vote("被驱逐", true, true, 0), Wolf("被杀死", true, false, 1), Poison("被毒死", false, false, 10),
		Hunter("被射死", false, false, 3), DarkWolf("被狼王杀死", true, false, 3), Knight("被单挑死", false, false, 3),
		Explode("自爆死", false, true, 3), Knight_s("以死谢罪", false, false, 3), Hunt("被猎杀", true, false, 2),
		Shoot("被箭射死", false, false, 2), Reflect("被反伤", false, false, 4), Love("殉情", false, false, 3),
		Shoot_s("射击失败", false, false, 3), Hunt_s("猎杀失败", false, false, 3), Burn("烧死", true, false, 1);

		String desc;
		final boolean canUseSkill;
		final boolean hasDiedWord;
		final int priority;

		private DiedReason(String desc, boolean canUseSkill, boolean hasDiedWord, int priority) {
			this.desc = desc;
			this.canUseSkill = canUseSkill;
			this.hasDiedWord = hasDiedWord;
			this.priority = priority;
		}

		@Override
		public String toString() {
			return desc;
		}

		public static String getString(DiedReason dr) {
			if (dr == null)
				return "存活";
			return dr.toString();
		}

		public boolean canBeReplaced(DiedReason dr) {
			return priority < dr.priority;
		}
	}

	public enum WaitReason {
		Generic(0), DieWord(1), State(2), Vote(3), Other(4);

		private final int id;

		private WaitReason(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

	public static enum Role{
		DARKWOLF("狼王",DarkWolf.class,-1.5,DarkWolfBot.class,Fraction.Wolf),
		ARSONER("纵火者",Arsoner.class,1.25,GenericBot.class,Fraction.God),
		WHITEWOLF("白狼王",WhiteWolf.class,-1.5,WereWolfBot.class,Fraction.Wolf),
		BEAR("熊",Bear.class,1.0,BearBot.class,Fraction.God),
		STATUEDEMON("石像鬼",StatueDemon.class,-0.5,WereWolfBot.class,Fraction.Wolf),
		CROW("乌鸦",Crow.class,1.0,GenericBot.class,Fraction.God),
		DEMON("恶魔",Demon.class,-1.35,WereWolfBot.class,Fraction.Wolf),
		WITCH("女巫",Witch.class,1.0,WitchBot.class,Fraction.God),
		MUTER("禁言长老",Muter.class,0.75,GenericBot.class,Fraction.God),
		WOLFBEAUTY("狼美人",WolfBeauty.class,-1.5,WereWolfBot.class,Fraction.Wolf),
		MIRACLEARCHER("奇迹弓手",MiracleArcher.class,1.0,GenericBot.class,Fraction.God),
		IDIOT("白痴",Idiot.class,0.5,IdiotBot.class,Fraction.God),
		TRAMP("老流氓",Tramp.class,0.0,GenericBot.class,Fraction.Innocent),
		HARDWOLF("巨狼",HardWolf.class,-1.25,WereWolfBot.class,Fraction.Wolf),
		HUNTER("猎人",Hunter.class,1.0,HunterBot.class,Fraction.God),
		CORONER("验尸官",Coroner.class,0.5,GenericBot.class,Fraction.God),
		FOX("狐狸",Fox.class,1.15,GenericBot.class,Fraction.God),
		DEFENDER("守卫",Defender.class,1.0,GenericBot.class,Fraction.God),
		KNIGHT("骑士",Knight.class,1.5,GenericBot.class,Fraction.God),
		NIGHTMAREKNIGHT("恶灵骑士",NightmareKnight.class,-1.5,WereWolfBot.class,Fraction.Wolf),
		GRAVEKEEPER("守墓人",GraveKeeper.class,0.5,GraveKeeperBot.class,Fraction.God),
		ELDER("长老",Elder.class,0.0,ElderBot.class,Fraction.Innocent),
		WEREWOLF("狼人",Werewolf.class,-1.0,WereWolfBot.class,Fraction.Wolf),
		VILLAGER("平民",Villager.class,0.0,GenericBot.class,Fraction.Innocent),
		SEER("预言家",Seer.class,1.0,SeerBot.class,Fraction.God),
		WOLFKILLER("猎魔人",WolfKiller.class,1.5,WolfKillerBot.class,Fraction.God),
		HIDDENWOLF("隐狼",HiddenWolf.class,-0.55,WereWolfBot.class,Fraction.Wolf);
		private final String name;
		private final Class<? extends Villager> roleClass;
		private final Class<? extends GenericBot> botClass;
		private final double rolePoint;
		private final Fraction fraction;
		private static Map<Class<? extends Villager>,Role> caram = new HashMap<>();
		private static Map<String,Role> namem = new HashMap<>();
		static {
			for(Role r:Role.values()) {
				caram.put(r.getRoleClass(),r);
				namem.put(r.getName(),r);
			}
		}
		private Role(String name, Class<? extends Villager> cls, double rolePoint, Class<? extends GenericBot> bots,Fraction fraction) {
			this.name = name;
			this.roleClass = cls;
			this.botClass = bots;
			this.rolePoint = rolePoint;

			this.fraction=fraction;
		}
		public static Role getByName(String name) {
			return namem.getOrDefault(name,VILLAGER);
		}
		public static Role getRole(Villager v) {
			return caram.get(v.getClass());
		}
		public String getName() {
			return name;
		}
		public Class<? extends Villager> getRoleClass() {
			return roleClass;
		}
		public Class<? extends GenericBot> getBotClass() {
			return botClass;
		}
		public double getRolePoint() {
			return rolePoint;
		}
		public Fraction getFraction() {
			return fraction;
		}
	}

	public static void main(String[] args) {
		/*PriorityQueue<Role> pq=new PriorityQueue<Role>(new Comparator<Role>() {
			@Override
			public int compare(Role o1, Role o2) {
				double obj1=f2n(o1.fraction)+o1.RolePoint;
				double obj2=f2n(o2.fraction)+o2.RolePoint;
				return (int)((obj2-obj1)*100);
			}

		} );
		for(Role r:Role.values()) {
			pq.add(r);
		}
		
		while(!pq.isEmpty()) {
			Role r=pq.poll();
			System.out.println(r.name+","+r.fraction.name+","+r.RolePoint);
		}*/
		//System.out.println(caram.size());
		while(true)
			fairRollRole(9,0.3);
	}
	@FunctionalInterface
	public interface RoleRoller {
		List<Role> roll(int cplayer, double flag);
	}

	private static Map<String, RoleRoller> patterns = new HashMap<>();
	
	static {
		WerewolfGame.patterns.put("默认", (cp, obj) -> WerewolfGame.fairRollRole(cp));
		WerewolfGame.patterns.put("标准", (cp, obj) -> WerewolfGame.StandardRollRole(cp));
		WerewolfGame.patterns.put("随机", (cp, obj) -> WerewolfGame.rollRole(cp));
		WerewolfGame.patterns.put("诸神", (cn, obj) -> WerewolfGame.godFightRollRole(cn));
		WerewolfGame.patterns.put("猎人", (cp, obj) -> WerewolfGame.hunterRollRole(cp));
	}
	transient Set<Villager> tokill = Collections.newSetFromMap(new ConcurrentHashMap<>());
	int[] tokillIds;
	transient List<Villager> sherifflist = Collections.synchronizedList(new ArrayList<>());
	public List<Villager> playerlist = Collections.synchronizedList(new ArrayList<>());
	transient List<Villager> canVote = Collections.synchronizedList(new ArrayList<>());
	transient List<Villager> canTalk=Collections.synchronizedList(new ArrayList<>());
	double winrate = 0;
	WerewolfGameLogger logger = new WerewolfGameLogger();
	boolean isDayTime = false;
	boolean isFirstNight = true;
	boolean canDayVote = false;
	boolean isEnded = false;
	boolean sameTurn = false;
	boolean canNoKill = false;
	boolean hunterMustShoot = false;
	boolean doStat = true;
	boolean hasTramp = false;
	boolean hasElder = false;
	boolean hasSheriff=false;
	boolean hasWolfGod=false;
	boolean isSheriffSelection=false;
	boolean isSkippedDay=false;
	int day = 0;
	int lastDeathCount = 0;
	
	transient Villager lastVoteOut;
	int lastVoteOutId;

	transient Villager cursed = null;
	int cursedId;

	transient Villager lastCursed = null;
	int lastCursedId;

	transient Villager lastwolfkill = null;
	int lastwolfkillId;
	transient Function<WerewolfGame,Wininfo> victoryinfo=killall;
	boolean vikillall=true;
	transient Object waitLock = new Object();
	transient VoteHelper<Villager> vu = new VoteHelper<>();
	int num = 0;
	int pointpool = -1;
	double curskillRate;
	transient WaitThread[] wt = new WaitThread[5];
	transient public List<Role> roles;
	@Override
	public boolean specialCommand(AbstractUser m,String[] cmds) {
		if(cmds.length==1) {
			if(cmds[0].equals("game")) {
				m.sendPrivate(String.join(",",ParamUtils.loadParams(this)));
				return true;
			}
		}
		if(cmds.length==2) {
			if(cmds[0].equals("game")) {
				m.sendPrivate(ParamUtils.getValue(this,cmds[1]));
				return true;
			}else if(cmds[0].equals("role")) {
				m.sendPrivate(String.join(",",ParamUtils.loadParams(getPlayerById(Long.parseLong(cmds[1])))));
				return true;
			}
		}
		if(cmds.length==3) {
			if(cmds[0].equals("game")) {
				ParamUtils.setValue(this,cmds[1],cmds[2]);
				m.sendPrivate(ParamUtils.getValue(this,cmds[1]));
				return true;
			}else if(cmds[0].equals("role")) {
				m.sendPrivate(ParamUtils.getValue(getPlayerById(Long.parseLong(cmds[1])),cmds[2]));
				return true;
			}
		}
		if(cmds.length==4) {
			if(cmds[0].equals("role")) {
				ParamUtils.setValue(getPlayerById(Long.parseLong(cmds[1])),cmds[2],cmds[3]);
				m.sendPrivate(ParamUtils.getValue(getPlayerById(Long.parseLong(cmds[1])),cmds[2]));
				return true;
			}
		}
		return false;
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException
	{
		// perform the default de-serialization first
		aInputStream.defaultReadObject();
		wt = new WaitThread[5];
		vu = new VoteHelper<>();
		waitLock = new Object();
		tokill = Collections.newSetFromMap(new ConcurrentHashMap<>());
		for (int i = 0; i < wt.length; i++) {
			wt[i] = new WaitThread();
		}
		Villager prev=playerlist.get(playerlist.size()-1);
		int min=0;
		for(Villager v:playerlist) {
			prev.next=v;
			v.prev=prev;
			prev=v;
			v.game=this;
			v.retake();
			String nc = v.getNameCard();
			if (nc.indexOf('|') != -1) {
				nc = nc.split("\\|")[1];
			}
			v.setNameCard(min++ + "号 |" + nc);
		}
		lastVoteOut=getPlayerById(lastVoteOutId);
		cursed=getPlayerById(cursedId);
		lastCursed=getPlayerById(lastCursedId);
		lastwolfkill=getPlayerById(lastwolfkillId);
		tokillIds=new int[tokill.size()];
		canTalk=Collections.synchronizedList(new ArrayList<>());
		sherifflist = Collections.synchronizedList(new ArrayList<>());
		canVote = Collections.synchronizedList(new ArrayList<>());
		if(vikillall)
			victoryinfo=killall;
		else
			victoryinfo=killside;
		for(int tok:tokillIds) {
			tokill.add(getPlayerById(tok));
		}
		this.sendPublicMessage("狼人杀游戏将在20秒后继续，请各位做好准备");
		getScheduler().execute(()->{
			try {
				Thread.sleep(20000);
			}catch(InterruptedException ie){
			}
			onDawnNoSe();
		});
		// ensure that object state has not been corrupted or tampered with malicious code
		//validateUserInfo();
	}

	/**
	 * This is the default implementation of writeObject. Customize as necessary.
	 */
	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		lastVoteOutId=getIdByPlayer(lastVoteOut);
		cursedId=getIdByPlayer(cursed);
		lastCursedId=getIdByPlayer(lastCursed);
		lastwolfkillId=getIdByPlayer(lastwolfkill);
		tokillIds=new int[tokill.size()];
		int i=0;
		for(Villager tok:tokill) {
			tokillIds[i++]=getIdByPlayer(tok);
		}
		aOutputStream.defaultWriteObject();
	}
	public WerewolfGame(AbstractRoom g, int cplayer) {
		super(g, cplayer, cplayer * 2);
		for (int i = 0; i < wt.length; i++) {
			wt[i] = new WaitThread();
		}
		roles = Collections.synchronizedList(WerewolfGame.fairRollRole(cplayer));
		winrate = WerewolfGame.calculateRolePoint(roles);
		if(cplayer>=9){
			hasSheriff=true;
		}
		if(cplayer>=8) {
			hasWolfGod=true;
		}
		for (Role role : roles) {
			if (role==Role.TRAMP) {
				hasTramp = true;
				continue;
			}
			if (role==Role.ELDER) {
				hasElder = true;
				continue;
			}
		}
	}

	public WerewolfGame(AbstractRoom g, String... args) {
		super(g, args.length, args.length * 2);
		for (int i = 0; i < wt.length; i++) {
			wt[i] = new WaitThread();
		}
		roles = Collections.synchronizedList(new ArrayList<>());
		for (String s : args) {
			roles.add(Role.getByName(s));
		}
		Collections.shuffle(roles);
		if(roles.size()>=9){
			hasSheriff=true;
		}
		winrate = WerewolfGame.calculateRolePoint(roles);
		for (Role role : roles) {
			if (role==Role.TRAMP) {
				hasTramp = true;
				continue;
			}
			if (role==Role.ELDER) {
				hasElder = true;
				continue;
			}
			if(role!=Role.WEREWOLF&&role.getFraction()==Fraction.Wolf) {
				hasWolfGod=true;
				continue;
			}
		}
	}

	public WerewolfGame(AbstractRoom g, int cplayer, Map<String, String> sets) {
		super(g, cplayer, cplayer * 2);
		int botnm = 0;
		for (int i = 0; i < wt.length; i++) {
			wt[i] = new WaitThread();
		}
		if (sets.containsKey("机器人")) {
			cplayer += botnm = Integer.parseInt(sets.get("机器人"));
		} else if (sets.containsKey("人数")) {
			int tplayer = Integer.parseInt(sets.get("人数"));
			botnm = tplayer - cplayer;
			cplayer = tplayer;
		}
		double cpoint = Double.parseDouble(sets.getOrDefault("评分", "0.3"));
		String type=sets.getOrDefault("板", "默认");
		roles = Collections.synchronizedList(WerewolfGame.patterns
				.getOrDefault(type, (cp, cps) -> WerewolfGame.fairRollRole(cp, cps))
				.roll(cplayer, cpoint));
		canNoKill = sets.getOrDefault("空刀", "false").equals("true");
		hunterMustShoot = sets.getOrDefault("压枪", "false").equals("true");
		doStat = sets.getOrDefault("统计", "true").equals("true");
		isFirstNight = sets.getOrDefault("首夜发言", "true").equals("true");
		pointpool = Integer.parseInt(sets.getOrDefault("积分奖池", "-1"));
		boolean isDeadBot=sets.getOrDefault("不接管", "false").equals("true");
		Collections.shuffle(roles);
		winrate = WerewolfGame.calculateRolePoint(roles);
		hasSheriff= sets.getOrDefault("警长",String.valueOf(cplayer>=9)).equals("true");
		vikillall=sets.getOrDefault("屠城","true").equals("true");
		if(vikillall)
			victoryinfo=killall;
		else
			victoryinfo=killside;
		if(cplayer>=8&&!type.equals("默认")&&!type.equals("随机")) {
			hasWolfGod=true;
		}
		for (Role role : roles) {
			if (role==Role.TRAMP) {
				hasTramp = true;
				continue;
			}
			if (role==Role.ELDER) {
				hasElder = true;
				continue;
			}
			if(role!=Role.WEREWOLF&&role.getFraction()==Fraction.Wolf) {
				hasWolfGod=true;
				continue;
			}
		}
		if (botnm > 0) {
			while (botnm-- > 0) {
				synchronized (playerlist) {
					Villager cp = null;
					int min = playerlist.size();
					Role role = roles.remove(0);
					Class<? extends BotUser> bot = role.getBotClass();
					try {
						if(isDeadBot) {
							bot=DeadBot.class;
						}playerlist.add(cp = role.getRoleClass().getConstructor(WerewolfGame.class, AbstractUser.class).newInstance(
								this,GlobalMain.createBot(min,bot,this)));
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					}

					cp.sendPrivate("已经报名");
					String nc = cp.getNameCard();
					if (nc.indexOf('|') != -1) {
						nc = nc.split("\\|")[1];
					}
					if (min != 0) {
						cp.prev = playerlist.get(min - 1);
						cp.prev.next = cp;
					}
					cp.setNameCard(min + "号 |" + nc);
					if (roles.size() == 0) {
						cp.next = playerlist.get(0);
						cp.next.prev = cp;
						this.sendPublicMessage("狼人杀已满人，游戏即将开始。");
						getScheduler().execute(() -> gameStart());
					}
				}
			}
		}
	}
	public void calcCSR() {
		int total=0;
		int wolfs=0;
		for (Villager p : playerlist) {
			if (p.isDead()) {
				continue;
			}
			total++;
			if (p.getRealFraction()==Fraction.Wolf) {
				wolfs++;
				continue;
			}
		}
		curskillRate=wolfs*1.0/total;
	}
	public double getCSR() {
		return curskillRate;
	}
	public String getGameRules() {
		StringBuilder sb = new StringBuilder("游戏规则设定：");
		if (canNoKill) {
			sb.append("\n允许狼人空刀");
		} else {
			sb.append("\n狼人必须杀人");
		}
		if (hasTramp) {
			sb.append("\n有老流氓");
		}
		if (hasElder) {
			sb.append("\n有长老");
		}
		if (roles.size() + playerlist.size() >= 8) {
			sb.append("\n允许狼神");
		}
		if (hunterMustShoot) {
			sb.append("\n猎人不能压枪");
		}
		if(hasSheriff) {
			sb.append("\n首日产生警长");
		}
		sb.append("\n人数：").append(playerlist.size());
		int inno=0,wolf=0,god=0;
		for(Villager v:playerlist) {
			if(v.getRealFraction()==Fraction.Wolf) {
				wolf++;
			} else if(v.getRealFraction()==Fraction.God) {
				god++;
			} else {
				inno++;
			}
		}
		sb.append("\n神/狼/民：").append(god).append("/").append(wolf).append("/").append(inno);
		if (doStat && roles.size() + playerlist.size() >= 6) {
			sb.append("\n记录统计数据");
		} else {
			sb.append("\n不记录统计数据");
		}
		if (!isFirstNight()) {
			sb.append("\n第一晚死亡不能发言");
		}else
			sb.append("\n第一晚死亡有遗言");
		if (doStat && pointpool == -1) {
			int cplayer = playerlist.size();
			if (cplayer >= 6) {
				pointpool = cplayer - (int) Math.ceil((cplayer - (int) Math.ceil(cplayer / 3.0)) / 2.0)
						+ playerlist.size() - 6;
			} else {
				pointpool = 0;
			}
		}
		if (pointpool > 0) {
			sb.append("\n获胜奖池：").append(pointpool);
		}

		return sb.toString();
	}

	public static String getName(Class<? extends Villager> vcls) {
		for (Role me : Role.values())
			if (me.getRoleClass().equals(vcls))
				return me.name;
		return "错误角色";
	}

	public static List<Role> hunterRollRole(int cplayer) {
		List<Role> roles = new ArrayList<>();
		int cwolf = (int) Math.ceil(cplayer * 1 / 3D);
		int chunter = cplayer - cwolf;
		while (--cwolf >= 0) {
			roles.add(Role.WEREWOLF);
		}
		while (--chunter >= 0) {
			roles.add(Role.HUNTER);
		}
		Collections.shuffle(roles);
		return roles;
	}

	public static List<Role> godFightRollRole(int cplayer) {
		List<Role> roles = new ArrayList<>();
		roles.add(Role.WITCH);
		roles.add(Role.SEER);
		roles.add(Role.IDIOT);
		roles.add(Role.DEFENDER);
		roles.add(Role.WHITEWOLF);
		roles.add(Role.NIGHTMAREKNIGHT);
		switch (cplayer) {
		case 6:
			break;
		case 7:
			roles.add(Role.GRAVEKEEPER);
			break;
		case 14:
			roles.add(Role.ELDER);
		case 13:
			roles.add(Role.TRAMP);
		case 12:
			roles.add(Role.GRAVEKEEPER);
		case 11:
			roles.add(Role.HUNTER);
			roles.add(Role.DARKWOLF);
			roles.add(Role.STATUEDEMON);
			roles.add(Role.WOLFKILLER);
			roles.add(Role.HIDDENWOLF);
			break;
		case 10:
			roles.add(Role.STATUEDEMON);
		case 9:
			roles.add(Role.GRAVEKEEPER);
		case 8:
			roles.add(Role.HUNTER);
			roles.add(Role.DARKWOLF);
			break;
		}
		Collections.shuffle(roles);
		return roles;
	}

	public static List<Role> fairRollRole(int cplayer) {
		return WerewolfGame.fairRollRole(cplayer, 0.3);
	}

	public static List<Role> fairRollRole(int cplayer, double cps) {
		List<Role> rslt = null;
		double rsltpoint = 100;
		for (int i = 0; i < 3; i++) {
			List<Role> cur = WerewolfGame.rollRole(cplayer);
			double curpoint = Math.abs(WerewolfGame.calculateRolePoint(cur) - cps);
			if (curpoint < rsltpoint) {
				rslt = cur;
				rsltpoint = curpoint;
			}
		}
		return rslt;
	}
	/**
	 * @param cp  
	 */
	public static List<Role> StandardRollRole(int cp) {
		List<Role> li=new ArrayList<>();
		li.add(Role.HUNTER);
		li.add(Role.WITCH);
		li.add(Role.SEER);
		if(cp>=12)
			li.add(Role.DEFENDER);
		li.add(Role.VILLAGER);
		li.add(Role.VILLAGER);
		li.add(Role.VILLAGER);
		if(cp>=12)
			li.add(Role.VILLAGER);
		li.add(Role.WEREWOLF);
		li.add(Role.WEREWOLF);
		li.add(Role.WEREWOLF);
		if(cp>=12)
			li.add(Role.WEREWOLF);
		Collections.shuffle(li);
		return li;
	}
	public static double calculateRolePoint(List<Role> larr) {
		double rslt = 0;
		
		for (Role cls : larr) {
			rslt += cls.getRolePoint();
		}
		return rslt;
	}

	public static List<Role> rollRole(int cplayer) {
		List<Role> roles = new ArrayList<>();
		int godcount = (int) Math.ceil(cplayer / 3.0);
		int wolfcount = (int) Math.ceil((cplayer - godcount) / 2.0);
		int innocount = cplayer - godcount - wolfcount;
		if (innocount < wolfcount) {
			--innocount;
			roles.add(Role.ELDER);
		}
		List<Role> exwroles = new ArrayList<>();

		if (cplayer >= 8) {
			exwroles.add(Role.STATUEDEMON);
			exwroles.add(Role.WHITEWOLF);
			exwroles.add(Role.DARKWOLF);
			exwroles.add(Role.HIDDENWOLF);
			exwroles.add(Role.WOLFBEAUTY);
			exwroles.add(Role.DEMON);
			exwroles.add(Role.HARDWOLF);
			int cs=exwroles.size();
			for(int i=0;i<cs;i++)
				exwroles.add(Role.WEREWOLF);
		}else
			for (int i = 0; i < wolfcount; i++) {
				exwroles.add(Role.WEREWOLF);
			}
		Collections.shuffle(exwroles);
		while (--wolfcount >= 0) {
			roles.add(exwroles.remove(0));
		}
		if (innocount >= 3) {
			roles.add(Role.TRAMP);
			--innocount;
		}
		while (--innocount >= 0) {
			roles.add(Role.VILLAGER);
		}
		Collections.shuffle(roles);
		List<Role> exroles = new ArrayList<>();
		for(Role r:Role.values())
			if(r.getFraction()==Fraction.God)
				exroles.add(r);
		Collections.shuffle(exroles);
		while (--godcount >= 0) {
			roles.add(exroles.remove(0));
		}
		Collections.shuffle(roles);
		return roles;
	}

	public String getWolfSentence() {
		if (canNoKill)
			return "请私聊选择要杀的人，你有2分钟的考虑时间\n也可以通过“#要说的话”来给所有在场狼人发送信息\n格式：“投票 qq号或者游戏号码”\n如：“投票 1”\n如果想空刀，请发送“放弃”";
		return "请私聊选择要杀的人，你有2分钟的考虑时间\n也可以通过“#要说的话”来给所有在场狼人发送信息\n格式：“投票 qq号或者游戏号码”\n如：“投票 1”\n如果想系统随机选择，请发送“放弃”";
	}



	// game control
	@Override
	protected void doFinalize() {
		vu.clear();
		for (Villager p : playerlist) {
			p.releaseListener();
			GameUtils.RemoveMember(p.getId());

		}
		super.doFinalize();

	}

	@Override
	public void forceStop() {
		terminateWait(WaitReason.State);
		terminateWait(WaitReason.Vote);
		terminateWait(WaitReason.DieWord);
		terminateWait(WaitReason.Generic);
		StringBuilder mc = new StringBuilder("游戏已中断\n");
		mc.append("游戏身份：");
		for (Villager p : playerlist) {
			p.releaseListener();
			GameUtils.RemoveMember(p.getId());
			mc.append("\n").append(p.getMemberString()).append("的身份为 ").append(p.getRole()).append(" ")
			.append(DiedReason.getString(p.getEffectiveDiedReason()));
			String nc = p.getNameCard();
			try {
			if (nc.indexOf('|') != -1) {
				nc = nc.split("\\|")[1];
			}
			}catch(Exception e){
				nc="";
			}
			p.setNameCard(nc);
			try {
				p.tryUnmute();
			} catch (Throwable t) {
			}

		}
		// muteAll(false);
		mc.append("\n角色评分：").append(winrate);
		try {
			Thread.sleep(1000);// sbtx好像有频率限制，先等他个1秒再说
		} catch (InterruptedException e) {
		}
		this.sendPublicMessage(mc.toString());
		isEnded = true;
		logger.sendLog(getGroup());
		super.forceStop();
	}
	@Override
	public void forceInterrupt() {
		terminateWait(WaitReason.State);
		terminateWait(WaitReason.Vote);
		terminateWait(WaitReason.DieWord);
		terminateWait(WaitReason.Generic);
		for (Villager p : playerlist) {
			p.releaseListener();
			GameUtils.RemoveMember(p.getId());
			String nc = p.getNameCard();
			try {
			if (nc.indexOf('|') != -1) {
				nc = nc.split("\\|")[1];
			}
			}catch(Exception e){
				nc="";
			}
			p.setNameCard(nc);
			try {
				p.tryUnmute();
			} catch (Throwable t) {
			}

		}
		try {
			Thread.sleep(1000);// sbtx好像有频率限制，先等他个1秒再说
		} catch (InterruptedException e) {
		}
		this.sendPublicMessage("游戏已暂停，请等待恢复");
		isEnded = true;
		super.forceStop();
	}
	@Override
	public void forceSkip() {
		skipWait(WaitReason.State);
		skipWait(WaitReason.Vote);
		skipWait(WaitReason.DieWord);
		skipWait(WaitReason.Generic);
	}

	@Override
	public String getName() {
		return "狼人杀";
	}

	@Override
	public boolean isAlive() {
		return !isEnded;
	}

	@Override
	public boolean onReAttach(Long c) {
		for (Villager in : playerlist) {
			if (in.onReattach(c))
				return true;
		}
		return false;
	}

	public String getAliveList() {
		StringBuilder sb = new StringBuilder("存活：\n");
		for (Villager p : playerlist) {
			if (!p.isDead()) {
				sb.append(p.getMemberString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public int getAliveCount() {
		int cnt = 0;
		for (Villager p : playerlist) {
			if (!p.isDead()) {
				cnt++;
			}
		}
		return cnt;
	}
	Map<Long,Fraction> requested=new HashMap<>();
	int cvg=0;
	int cvw=0;
	int cvi=0;
	@Override
	public void userSettings(AbstractUser ar,String name,String set) {
		//System.out.println(name+set);
		if(name.equals("vip"))
			switch(set) {
			case "神":requested.put(ar.getId(),Fraction.God);cvg++;break;
			case "狼":requested.put(ar.getId(),Fraction.Wolf);cvw++;break;
			case "民":requested.put(ar.getId(),Fraction.Innocent);cvi++;break;
			}
	}
	@Override
	public boolean addMember(AbstractUser mem) {
		if (getPlayerById(mem.getId()) != null) {
			mem.sendPublic("你已经报名了！");
			return false;
		}
		if (!GameUtils.tryAddMember(mem.getId())) {
			mem.sendPublic("你已参加其他游戏！");
		}
		if (roles.size() > 0) {
			try {
				synchronized (playerlist) {
					Villager cp;
					int min = playerlist.size();
					Fraction f=requested.get(mem.getId());
					Role to = null;
					if(f!=null) {
						//GlobalMain.getLogger().debug("vip"+mem.getId());
						int tot=0;
						switch(f) {
						case God:tot=cvg;break;
						case Wolf:tot=cvw;break;
						case Innocent:tot=cvi;break;
						}
						int tpcount=min+roles.size();
						if(Math.random()<Math.ceil(tpcount/9.0*2.0)/tot) {
							for(int i=0;i<roles.size();i++) {
								Role r=roles.get(i);
								if(r.getFraction()==f) {
									if(GlobalMain.credit.get(mem.getId()).withdrawItem("狼人杀vip券",1)>=0) {
										roles.remove(i);
										to=r;
									}
									break;
								}
							}
						}else {
							for(int i=0;i<roles.size();i++) {
								Role r=roles.get(i);
								if(r.getFraction()!=f) {
									roles.remove(i);
									to=r;
									break;
								}
							}
						}
					}
					if(to==null)
						to=roles.remove(0);
					playerlist.add(cp = to.getRoleClass().getConstructor(WerewolfGame.class, AbstractUser.class)
							.newInstance(this, mem));

					cp.sendPrivate("已经报名");
					String nc = cp.getNameCard();
					try {
						if (nc.indexOf('|') != -1) {
							nc = nc.split("\\|")[1];
						}
						}catch(Exception e){
							nc="";
						}
					if (min != 0) {
						cp.prev = playerlist.get(min - 1);
						cp.prev.next = cp;
					}
					cp.setNameCard(min + "号 |" + nc);
					if (roles.size() == 0) {
						cp.next = playerlist.get(0);
						cp.next.prev = cp;
						this.sendPublicMessage("狼人杀已满人，游戏即将开始。");
						getScheduler().execute(() -> gameStart());
					}
				}
				return true;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public void forceStart() {
		roles.clear();
		Villager cp = playerlist.get(playerlist.size() - 1);
		cp.next = playerlist.get(0);
		cp.next.prev = cp;
		getScheduler().execute(() -> gameStart());
	}

	@Override
	public void forceShow(AbstractUser ct) {
		StringBuilder mc = new StringBuilder("游戏身份：");
		for (Villager p : playerlist) {
			mc.append("\n").append(p.getMemberString()).append("的身份为 ").append(p.getRole()).append(" ")
			.append(DiedReason.getString(p.getEffectiveDiedReason()));
		}
		mc.append("\n角色评分：").append(winrate);
		try {
			Thread.sleep(1000);// sbtx好像有频率限制，先等他个1秒再说
		} catch (InterruptedException e) {
		}
		ct.sendPrivate(mc.toString());
	}

	@Override
	public boolean takeOverMember(long id, AbstractUser o) {
		Villager p = getPlayerById(id);
		try {
			int n = playerlist.indexOf(p);
			String nc = p.getNameCard();
			try {
			if (nc.indexOf('|') != -1) {
				nc = nc.split("\\|")[1];
			}
			}catch(Exception e){
				nc="";
			}
			p.setNameCard(nc);
			if (o == null) {
				p.doTakeOver(GlobalMain.createBot(n,Role.getRole(p).getBotClass(),this));
			} else {
				p.doTakeOver(o);
			}
			p.setNameCard(n + "号 |" + p.getNameCard());
			return true;
		} catch (IllegalArgumentException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

	// wait utils
	public void startWait(long millis, WaitReason lr) {
		wt[lr.getId()].startWait(millis);
	}

	public void skipWait(WaitReason lr) {
		wt[lr.getId()].stopWait();
	}

	public void terminateWait(WaitReason lr) {
		wt[lr.getId()].terminateWait();
	}

	public void endWait(WaitReason lr) throws InterruptedException {
		wt[lr.getId()].endWait();
	}

	// game logic
	void removeAllListeners() {
		for (Villager p : playerlist) {
			p.EndTurn();
			p.releaseListener();
		}
	}
	public int getIdByPlayer(Villager v) {
		int i=0;
		for (Villager p : playerlist) {
			if (p==v)
				return i;
			i++;
		}
		return -1;
	}
	public Villager getPlayerById(long id) {
		int i = 0;
		for (Villager p : playerlist) {
			if (p.getId() == id || i == id)
				return p;
			i++;
		}
		return null;
	}

	public void WolfVote(Villager src, Villager id) {
		if (vu.vote(src, id)) {
			skipWait(WaitReason.Vote);
		}
	}

	public void DayVote(Villager src, Villager id) {
		if (canDayVote)
			if (vu.vote(src, id,src.getTicketCount())) {
				skipWait(WaitReason.Vote);
			}
	}

	public boolean checkCanVote(Villager id) {
		if (getCanVote() == null)
			return true;
		return getCanVote().contains(id);
	}

	public void NoVote(Villager src) {
		vu.giveUp(src);
		if (vu.finished()) {
			skipWait(WaitReason.Vote);
		}
	}

	void kill(Villager p, DiedReason r) {
		synchronized(tokill) {
			tokill.add(p);
			p.populateDiedReason(r);
		}
	}

	/**
	 * @param isMute
	 */
	@SuppressWarnings("unused")
	private void muteAll(boolean isMute) {
		getGroup().setMuteAll(isMute);
	}

	// 开始游戏流程
	public void gameStart() {
		logger.title("游戏开始");
		// muteAll(true);
		this.sendPublicMessage(getGameRules());
		StringBuilder sb = new StringBuilder("玩家列表：\n");
		for (Villager p : playerlist) {
			p.onGameStart();
			sb.append(p.getMemberString());
			sb.append("\n");
			p.onFinishTalk();
		}
		this.sendPublicMessage(sb.toString());

		onDawn();
	}

	// 混合循环
	public void nextOnDawn() {
		isFirstNight=false;
		lastCursed = cursed;
		cursed = null;
		if (VictoryPending())
			return;
		onDawn();
	}
	public void onDawn() {
		try (FileOutputStream fileOut = new FileOutputStream(new File(MiraiMain.plugin.getDataFolder(),""+getGroup()+".game"));
				ObjectOutputStream out = new ObjectOutputStream(fileOut);){
			
			out.writeObject(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		onDawnNoSe();
	}
	public void onDawnNoSe() {
		day++;
		isDayTime = false;
		vu.clear();
		removeAllListeners();
		logger.logTurn(day, "狼人回合");
		onWolfTurn();
	}

	/*
	 * public void onUpperNightTurn() {
	 * this.sendPublicMessage("天黑了，所有人闭眼，有上半夜技能的玩家请睁眼，请私聊决定技能……");
	 * for(Innocent p2:playerlist) {
	 * if(p2.isDead)continue;
	 * p2.onTurn(4);
	 * }
	 * startWait(30000);
	 * removeAllListeners();
	 * scheduler.execute(()->onWolfTurn());
	 * }
	 */
	public void onWolfTurn() {
		sendPublicMessage("天黑了，所有人闭眼，狼人请睁眼，请私聊投票选择你们要杀的人。");
		vu.skipHalf = false;
		// muteAll(true);
		for (Villager p : playerlist) {
			if (p.isDead()) {
				continue;
			}
			p.lastIsMuted=false;
			if(p.isMuted) {
				p.isMuted=false;
				p.lastIsMuted=true;
			}
			p.onWolfTurn();
		}
		startWait(120000, WaitReason.Vote);
		removeAllListeners();
		List<Villager> il = vu.getForceMostVoted();
		if (il.size() > 0) {

			wolfKill(il.get(0));
		} else {
			if (canNoKill && vu.finished()) {
				logger.logRaw("狼人空刀");
				lastwolfkill = null;
				getScheduler().execute(() -> afterWolf());
				return;
			}
			Villager rd;
			if(isFirstNight) {
				List<Villager> toselect=new ArrayList<>(playerlist);
				toselect.removeIf(r->(r.isDead||r.getRealFraction()==Fraction.Wolf));
				Collections.shuffle(toselect);
				List<Villager> toSelectNoGod=new ArrayList<>(toselect);
				if(this.playerlist.size()<9)
					toSelectNoGod.removeIf(r->r.onSkilledAccuracy()<0.25);
				else
					toSelectNoGod.removeIf(r->r.onSkilledAccuracy()<0.2);
				if(toSelectNoGod.size()>0)
					rd=toSelectNoGod.get(0);
				else
					rd=toselect.get(0);
			}
			else
				do {
					rd = playerlist.get((int) (Math.random() * playerlist.size()));
				} while (rd.isDead() || rd instanceof Werewolf);
				logger.logSkill("系统", rd, "随机杀死");
			wolfKill(rd);
		}
	}

	public void wolfKill(Villager p) {
		logger.logSkill("狼人",p, "杀死");
		vu.clear();
		lastwolfkill = p;
		for(Villager px:playerlist) {
			if(!px.isDead()&&px.canWolfTurn()) {
				px.sendPrivate("昨晚最终决定杀死"+p.getMemberString());
			}
		}
		if (p instanceof Elder && !((Elder) p).lifeUsed) {
			logger.logRaw("长老生命减少");
			((Elder) p).lifeUsed = true;
		} else if (p != null) {
			kill(p, DiedReason.Wolf);
		}
		afterWolf();
	}

	public void afterWolf() {
		logger.logTurn(day, "技能回合");
		this.sendPublicMessage("狼人请闭眼，有夜间技能的玩家请睁眼，请私聊选择你们的技能。");
		for (Villager p2 : playerlist) {
			if (p2.isDead()) {
				continue;
			}
			p2.onTurn(2);
		}
		startWait(60000, WaitReason.Generic);
		removeAllListeners();
		if(isFirstNight()&&hasSheriff) {
			getScheduler().execute(() -> onSheriffSelect());
			return;
		}
		getScheduler().execute(() -> onDiePending());
	}
	public void onSheriffSelect() {
		logger.logTurn(day, "警长竞选");
		isSheriffSelection=true;
		this.sendPublicMessage("所有人睁眼，警长竞选回合开始。");
		for(Villager v:playerlist) {
			v.onSelectSheriff();
		}
		startWait(60000,WaitReason.Generic);
		this.removeAllListeners();
		if(sherifflist.size()==0) {
			this.sendPublicMessage("无人竞选，跳过环节。");
			getScheduler().execute(() -> onDiePending());
			return;
		}
		List<Villager> restToVote=new ArrayList<>(playerlist);
		Collections.shuffle(sherifflist);
		StringBuilder sb = new StringBuilder("警长竞选列表：\n");
		for(Villager p:playerlist) {
			if(p instanceof Werewolf) {
				p.sendPrivate(p.getRole()+"，你可以在投票前随时翻牌自爆并且立即进入黑夜，格式：“自爆”");
				p.addDaySkillListener();
			}
		}
		if(restToVote.isEmpty()) {
			this.sendPublicMessage("无人可以投票，若投票前有多于一人竞选，则无人可以得到警徽。");
		}
		for (Villager p : sherifflist) {
			restToVote.remove(p);
			p.onBeforeSheriffState();
			sb.append(p.getMemberString());
			sb.append("\n");
		}
		this.sendPublicMessage(sb.toString());
		for(Villager v:new ArrayList<>(sherifflist)) {
			if(!sherifflist.contains(v)) {
				continue;
			}
			v.onSheriffState();
		}
		this.sendPublicMessage("你们有15秒思考时间，15秒后开始投票。");
		if(sherifflist.size()>1) {
			if(restToVote.isEmpty()) {
				this.sendPublicMessage("无人可以投票，跳过环节。");
				getScheduler().execute(() -> onDiePending());
				return;
			}

			startWait(15000,WaitReason.Generic);
			vu.clear();
			restToVote.forEach(v->v.onSheriffVote());
			this.sendPublicMessage("请在两分钟内在私聊中完成投票！");
			vu.hintVote(getScheduler());
			startWait(120000,WaitReason.Vote);
			voteSheriff(vu.getForceMostVoted(),restToVote);
			return;
		}else if(sherifflist.size()==1){
			Villager slt=sherifflist.get(0);
			this.sendPublicMessage("仅一人竞选，"+slt.getMemberString()+"直接当选！");
			slt.isSheriff=true;

		}else if(sherifflist.size()==0) {
			this.sendPublicMessage("无人竞选，跳过环节。");
		}
		for(Villager p:playerlist) {
			p.releaseListener();
		}

		getScheduler().execute(() -> onDiePending());
	}
	public void voteSheriff(List<Villager> ps,List<Villager> vtb) {
		vu.clear();
		if (ps.size() > 1) {
			if (!sameTurn) {
				sherifflist.clear();
				sherifflist.addAll(ps);
				logger.logTurn(day, "警长同票PK");
				sameTurn = true;
				this.sendPublicMessage("同票，请做最终陈述。");
				MessageCompound mcb = new MessageCompound();
				mcb.append("开始投票，请在两分钟内投给以下人物其中之一：\n");
				getCanVote().clear();
				getCanVote().addAll(ps);
				for (Villager p : ps) {
					mcb.append(p.getAt());
					mcb.append("\n");
					p.onSheriffState();
				}
				if(sherifflist.size()==1){
					Villager slt=sherifflist.get(0);
					this.sendPublicMessage("仅一人竞选，"+slt.getMemberString()+"直接当选！");
					slt.isSheriff=true;
					getScheduler().execute(() -> onDiePending());
					return;
				}else if(sherifflist.size()==0) {
					this.sendPublicMessage("无人竞选，跳过环节。");
					getScheduler().execute(() -> onDiePending());
					return;
				}
				mcb.append("请在两分钟内在私聊中完成投票！");
				this.sendPublicMessage(mcb);
				// muteAll(true);
				vtb.forEach(v->v.onSheriffVote());
				vu.hintVote(getScheduler());
				getScheduler().execute(() -> {
					startWait(120000, WaitReason.Vote);
					removeAllListeners();
					voteSheriff(vu.getForceMostVoted(),vtb);
				});
				return;
			}
			this.sendPublicMessage("再次同票，警徽流失。");
			ps.clear();
		}
		getCanVote().clear();
		sameTurn = false;
		if (ps.size() == 0) {
			this.sendPublicMessage("无人当选");
		} else {
			Villager p = ps.get(0);
			this.sendPublicMessage(p.getMemberString()+"当选！");
			p.isSheriff=true;
		}
		getScheduler().execute(() -> onDiePending());
	}
	public void SheriffVote(Villager src, Villager id) {
		if (vu.vote(src, id)) {
			skipWait(WaitReason.Vote);
		}
	}
	public void onDiePending() {
		isSheriffSelection=false;
		logger.logTurn(day, "死亡技能回合");
		this.sendPublicMessage("有夜间技能的玩家请闭眼，有死亡技能的玩家请睁眼，你的技能状态是……");
		if (lastwolfkill.isBurned) {
			Villager firstWolf = lastwolfkill.getPrevWolf();
			if(firstWolf!=null) {
				kill(firstWolf, DiedReason.Burn);
				firstWolf.isDead=true;
			}
		}
		if (lastwolfkill.isArcherProtected && !lastwolfkill.isGuarded) {
			Villager firstWolf = lastwolfkill.getPrevWolf();
			if(firstWolf!=null) {
				kill(firstWolf, DiedReason.Shoot);
				firstWolf.isDead=true;
			}
		}
		/*if (lastwolfkill.isBurned) {
			lastwolfkill.isBurned = false;
			Villager firstWolf = lastwolfkill.prev;
			while (firstWolf != lastwolfkill) {
				if ((!firstWolf.isDead())&&firstWolf instanceof Werewolf) {
					break;
				}
				firstWolf=firstWolf.prev;
			}
			if (firstWolf == lastwolfkill) {
				firstWolf = lastwolfkill.prev;
				while (firstWolf != lastwolfkill) {
					if ((!firstWolf.isDead())&&firstWolf.getRealFraction() == Fraction.Wolf) {
						break;
					}
					firstWolf=firstWolf.prev;
				}
			}
			if (firstWolf.getRealFraction() == Fraction.Wolf) {
				kill(firstWolf, DiedReason.Burn);
				firstWolf.isDead=true;
			}
		}
		if (lastwolfkill.isArcherProtected && !lastwolfkill.isGuarded) {
			Villager firstWolf = lastwolfkill.prev;
			while (firstWolf != lastwolfkill) {
				if (!firstWolf.isDead()&&firstWolf instanceof Werewolf) {
					break;
				}
				firstWolf=firstWolf.prev;
			}
			if (firstWolf == lastwolfkill) {
				firstWolf = lastwolfkill.prev;
				while (firstWolf != lastwolfkill) {
					if (!firstWolf.isDead()&&firstWolf.getRealFraction() == Fraction.Wolf) {
						break;
					}
					firstWolf=firstWolf.prev;
				}
			}
			if (firstWolf.getRealFraction() == Fraction.Wolf && firstWolf != lastwolfkill) {
				kill(firstWolf, DiedReason.Shoot);
				firstWolf.isDead=true;
			}
		}*/
		tokill.removeIf(in -> in.shouldSurvive());
		for (Villager px : tokill) {
			px.isDead = true;
		}
		for (Villager p2 : playerlist) {
			if (p2 instanceof Bear && p2.isDead()) {
				sendPublicMessage("昨晚熊没有咆哮。");
			}
			if (p2.isDead()) {
				continue;
			}
			if(p2.getEffectiveDiedReason()!=null) {
				if(!p2.shouldSurvive())
					tokill.add(p2);
			}
			p2.onTurn(4);
		}

		Set<Villager> tks = new HashSet<>(tokill);
		tokill.clear();
		boolean shouldWait = false;
		for (Villager p : tks) {
			shouldWait |= p.onDiePending(p.getEffectiveDiedReason());
		}
		if (!shouldWait && tokill.isEmpty() && VictoryPending())
			return;
		startWait(30000, WaitReason.Generic);
		removeAllListeners();
		while (!tokill.isEmpty()) {
			tks.addAll(tokill);
			tokill.clear();
			boolean haswait = false;
			for (Villager p : tks) {
				haswait |= p.onDiePending(p.getEffectiveDiedReason());
			}
			if (haswait) {
				startWait(30000, WaitReason.Generic);
			}
			removeAllListeners();
		}
		tokill.addAll(tks);
		if (VictoryPending())
			return;
		getScheduler().execute(() -> onDayTime());
	}



	public void skipDay() {
		terminateWait(WaitReason.State);
		terminateWait(WaitReason.Vote);
		terminateWait(WaitReason.DieWord);
		terminateWait(WaitReason.Generic);
		nextOnDawn();
	}

	public void preSkipDay() {
		terminateWait(WaitReason.State);
		terminateWait(WaitReason.Vote);
		terminateWait(WaitReason.DieWord);
		terminateWait(WaitReason.Generic);
	}

	public void onDayTime() {
		lastVoteOut = null;
		// muteAll(false);
		logger.logTurn(day, "宣布死者");
		Villager lastdeath=null;
		if (!tokill.isEmpty()) {
			lastDeathCount = 0;
			lastdeath=tokill.iterator().next();
			StringBuilder sb = new StringBuilder("天亮了，昨晚的死者是：\n");
			for (Villager p : tokill) {
				p.isDead = true;
				lastDeathCount = getLastDeathCount() + 1;
				sb.append(p.getNameCard());
				sb.append("\n");
			}
			if (VictoryPending())
				return;
			this.sendPublicMessage(sb.toString());
			for (Villager p : tokill) {
				p.onDied(p.getEffectiveDiedReason());
				logger.logDeath(p, p.getEffectiveDiedReason());
			}
		} else {
			this.sendPublicMessage("昨夜无死者。");
		}
		this.sendPublicMessage(getAliveList());
		tokill.clear();
		isFirstNight = false;
		int aliv = 0;
		int tot = 0;
		for (Villager p : playerlist) {
			tot++;
			if (!p.isDead()) {
				aliv++;
			}
		}
		this.sendPublicMessage("剩余人数：" + aliv + "/" + tot);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Villager pb : playerlist) {
			if (pb.isBurned) {
				pb.isBurned = false;
				sendPublicMessage("昨晚，" + pb.getMemberString() + "燃起来了，他的身份是" + pb.getRole() + "。");
			}
		}
		canTalk.clear();
		boolean orderSelected=false;
		for (Villager p : playerlist) {
			if (!p.isDead()) {
				p.onTurnStart();
				orderSelected|=p.onSelectOrder(lastdeath);

			}
		}
		if(!orderSelected) {
			canTalk.addAll(playerlist);
		}
		if(isSkippedDay) {
			isSkippedDay=false;
			nextOnDawn();
		}
		isDayTime = true;
		logger.logTurn(day, "白天陈述");
		for (Villager p :canTalk) {
			if (!p.isDead()&&!p.isMuted) {
				p.onDayTime();
			}
		}
		vu.skipHalf = true;
		getCanVote().addAll(playerlist);
		this.sendPublicMessage("你们有15秒思考时间，15秒后开始投票。");
		startWait(15000, WaitReason.Generic);
		// muteAll(true);
		this.sendPublicMessage("请在两分钟内在私聊中完成投票！");
		logger.logTurn(day, "白天投票");
		this.calcCSR();
		for (Villager p : playerlist) {
			if (!p.isDead()) {
				p.vote();
			}
		}
		if (cursed != null) {
			vu.vote(cursed);
		}
		vu.hintVote(getScheduler());
		canDayVote = true;
		startWait(120000, WaitReason.Vote);
		removeAllListeners();
		if (cursed != null) {
			this.sendPublicMessage(cursed.getMemberString() + "被乌鸦诅咒了。");
		}
		voteKill(vu.getMostVoted());
	}


	public void voteKill(List<Villager> ps) {
		vu.clear();
		if (ps.size() > 1) {
			if (!sameTurn) {
				logger.logTurn(day, "同票PK");
				sameTurn = true;
				this.sendPublicMessage("同票，请做最终陈述。");
				MessageCompound mcb = new MessageCompound();
				mcb.append("开始投票，请在两分钟内投给以下人物其中之一：\n");
				// muteAll(false);
				getCanVote().addAll(ps);
				for (Villager p : ps) {
					mcb.append(p.getAt());
					mcb.append("\n");
					p.onDayTime();
				}
				mcb.append("请在两分钟内在私聊中完成投票！");
				this.sendPublicMessage(mcb);
				// muteAll(true);
				for (Villager p : playerlist) {
					if (!p.isDead()) {
						p.vote();
					}
				}
				if (cursed != null) {
					vu.vote(cursed);
				}
				vu.hintVote(getScheduler());
				getScheduler().execute(() -> {
					startWait(120000, WaitReason.Vote);
					removeAllListeners();
					voteKill(vu.getMostVoted());
				});
				return;
			}
			this.sendPublicMessage("再次同票，跳过回合。");
			ps.clear();
		}
		getCanVote().clear();
		sameTurn = false;
		if (ps.size() == 0) {
			this.sendPublicMessage("无人出局");
		} else {
			Villager p = ps.get(0);
			lastVoteOut = p;
			kill(p, DiedReason.Vote);
			// muteAll(false);
			while(tokill.size()>0) {
				List<Villager> lv=new ArrayList<>(tokill);
				tokill.clear();
				for (Villager pe : lv) {
					logger.logDeath(pe, pe.getEffectiveDiedReason());
					if (pe.canDeathSkill(pe.getEffectiveDiedReason())) {
						if (pe.shouldWaitDeathSkill()) {
							pe.onDied(pe.getEffectiveDiedReason());
							continue;
						}
						pe.onDieSkill(pe.getEffectiveDiedReason());
					}
					pe.onSheriffSkill();
					pe.isDead = true;
					if (VictoryPending())
						return;
					pe.onDied(pe.getEffectiveDiedReason(), false);
				}
			}
		}
		tokill.clear();
		this.calcCSR();
		canDayVote = false;
		nextOnDawn();
	}
	static class Wininfo {
		public Wininfo(String status, Fraction winfrac) {
			this.status = status;
			this.winfrac = winfrac;
		}
		String status = null;
		Fraction winfrac = null;
	}
	
	static Function<WerewolfGame,Wininfo> killall=(game)->{
		int total = 0;
		float innos = 0;
		float wolfs = 0;
		float activewolfs=0;
		if (!game.canDayVote && game.cursed != null) {
			innos++;
		}
		for (Villager p : game.playerlist) {
			if (p.isDead()) {
				continue;
			}
			total++;
			if (p.getRealFraction()==Fraction.Wolf) {
				if(p instanceof Werewolf) {
					activewolfs++;
					if(p.isSheriff)
						activewolfs+=0.5;
				}
				wolfs++;
				if(p.isSheriff)
					wolfs+=0.5;
				continue;
			}
			innos++;
			if(p.isSheriff)
				innos+=0.5;
			if (p instanceof Hunter) {
				innos++;
			} else if (p instanceof Witch && ((Witch) p).hasPoison) {
				innos++;
			} else if (p instanceof Knight && ((Knight) p).hasSkill) {
				innos++;
			} else if (p instanceof Idiot && !((Idiot) p).canVote) {
				innos--;
			} else if (p instanceof MiracleArcher&&((MiracleArcher) p).hasArrow) {
				innos++;
			} else if (p instanceof Arsoner&&!((Arsoner) p).isSkillUsed) {
				innos++;
			} else if (p instanceof WolfKiller) {
				innos += 2;
			}
		}
		if(activewolfs==0&&wolfs>0)
			activewolfs=wolfs;
		if (innos <= 0.5 && wolfs > 0) {
			return new Wininfo("游戏结束！狼人获胜\n",Fraction.Wolf);
		} else if (wolfs <= 0.5 && innos > 0) {
			return new Wininfo("游戏结束！好人获胜\n",Fraction.Innocent);
		} else if (total == 0) {
			return new Wininfo("游戏结束！同归于尽\n",null);
		} else if (activewolfs >= innos) {
			return new Wininfo("游戏结束！狼人获胜\n",Fraction.Wolf);
		}
		return null;
	};
	static Function<WerewolfGame,Wininfo> killside=(game)->{
		int total = 0;
		int innos = 0;
		int wolfs = 0;
		int gods=0;
		if (!game.canDayVote && game.cursed != null) {
			innos++;
		}
		for (Villager p : game.playerlist) {
			if(!p.isDead) {
				total++;
				if(p.getRealFraction()==Fraction.God) 
					gods++;
				else if(p.getRealFraction()==Fraction.Wolf)
					wolfs++;
				else
				innos++;
				
			}
		}
		if ((innos == 0||gods == 0) && wolfs > 0) {
			return new Wininfo("游戏结束！狼人获胜\n",Fraction.Wolf);
		} else if (total == 0) {
			return new Wininfo("游戏结束！同归于尽\n",null);
		} else if(wolfs==0){
			return new Wininfo("游戏结束！好人获胜\n",Fraction.Innocent);
		}
		return null;
	};
	// 结束回合循环
	public boolean VictoryPending() {
		Wininfo wi=victoryinfo.apply(this);
		if (wi!=null) {
			logger.title(wi.status);
			GameData gd = null;
			if (doStat && playerlist.size() >= 6) {
				gd = GlobalMain.db.getGame(getName());
			}
			removeAllListeners();
			StringBuilder mc = new StringBuilder();
			mc.append(wi.status);
			mc.append("游戏身份：");
			List<Villager> winpls = new ArrayList<>();
			for (Villager p : playerlist) {
				mc.append("\n").append(p.getMemberString()).append("的身份为 ").append(p.getRole()).append(" ")
				.append(DiedReason.getString(p.getEffectiveDiedReason()));
				if(!(p.getRealFraction()==Fraction.Wolf)) {
					mc.append(" 准确率：").append(Utils.percent(p.skillAccuracy * 2 +p.voteAccuracy,p.skilled * 2 + p.voted));
				}
				String nc = p.getNameCard();
				try {
				if (nc.indexOf('|') != -1) {
					nc = nc.split("\\|")[1];
				}
				}catch(Exception e){
					nc="";
				}
				p.setNameCard(nc);
				if (gd != null) {
					WerewolfPlayerData wpd = gd.getPlayer(p.getId(), WerewolfPlayerData.class);
					if (wpd.log(p, wi.winfrac, !p.isDead())) {
						winpls.add(p);
					}
					gd.setPlayer(p.getId(), wpd);
				}
				try {
					p.tryUnmute();
				} catch (Throwable t) {
				}
			}
			if (pointpool > 0&&winpls.size()>0) {
				int ppp = pointpool / winpls.size();
				for (Villager p : winpls) {
					GlobalMain.credit.get(p.getId()).givePT(ppp);
				}

			}
			// muteAll(false);
			mc.append("\n角色评分：").append(winrate);
			try {
				Thread.sleep(10000);// sbtx好像有频率限制，先等他个10秒再说
			} catch (InterruptedException e) {
			}
			this.sendPublicMessage(mc.toString());
			logger.sendLog(getGroup());
			doFinalize();

		}
		isEnded = wi!=null;
		return isEnded;
	}
	public int getLastDeathCount() {
		return lastDeathCount;
	}
	public List<Villager> getCanVote() {
		return canVote;
	}
	public Set<Villager> getTokill() {
		return tokill;
	}
	public boolean isFirstNight() {
		return isFirstNight;
	}

}
