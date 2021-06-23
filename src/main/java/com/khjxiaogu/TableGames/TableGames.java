package com.khjxiaogu.TableGames;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import com.khjxiaogu.TableGames.clue.ClueGame;
import com.khjxiaogu.TableGames.clue.CluePreserve;
import com.khjxiaogu.TableGames.data.CreditTrade;
import com.khjxiaogu.TableGames.data.GenericPlayerData;
import com.khjxiaogu.TableGames.data.PlayerCreditData;
import com.khjxiaogu.TableGames.data.PlayerDatabase;
import com.khjxiaogu.TableGames.fastclue.FastClueGame;
import com.khjxiaogu.TableGames.fastclue.FastCluePreserve;
import com.khjxiaogu.TableGames.platform.message.Message;
import com.khjxiaogu.TableGames.platform.message.Text;
import com.khjxiaogu.TableGames.platform.mirai.MiraiAdapter;
import com.khjxiaogu.TableGames.platform.mirai.MiraiGroup;
import com.khjxiaogu.TableGames.platform.mirai.MiraiHumanPlayer;
import com.khjxiaogu.TableGames.platform.mirai.MiraiListenerUtils;
import com.khjxiaogu.TableGames.spwarframe.SpWarframe;
import com.khjxiaogu.TableGames.spwarframe.SpWarframePreserve;
import com.khjxiaogu.TableGames.undercover.UnderCoverGame;
import com.khjxiaogu.TableGames.undercover.UnderCoverHolder;
import com.khjxiaogu.TableGames.undercover.UnderCoverHolderPreserve;
import com.khjxiaogu.TableGames.undercover.UnderCoverPreserve;
import com.khjxiaogu.TableGames.undercover.UnderCoverTextLibrary;
import com.khjxiaogu.TableGames.utils.GameUtils;
import com.khjxiaogu.TableGames.utils.MessageListener.MsgType;
import com.khjxiaogu.TableGames.utils.PreserveHolder;
import com.khjxiaogu.TableGames.utils.PreserveInfo;
import com.khjxiaogu.TableGames.utils.Utils;
import com.khjxiaogu.TableGames.werewolf.Villager;
import com.khjxiaogu.TableGames.werewolf.WerewolfGame;
import com.khjxiaogu.TableGames.werewolf.WerewolfPreserve;

import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import net.mamoe.mirai.event.events.StrangerMessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageContent;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol;


public class TableGames extends JavaPlugin {
	public TableGames(JvmPluginDescription description) {
		super(description);
	}
	public static TableGames plugin;
	public static PlayerDatabase db;
	public static PlayerCreditData credit;
	public static Map<String,BiConsumer<GroupMessageEvent,String[]>> normcmd=new ConcurrentHashMap<>();
	public static Map<String,BiConsumer<GroupMessageEvent,String[]>> privcmd=new ConcurrentHashMap<>();
	public static ExecutorService dispatchexec=Executors.newCachedThreadPool();
	public static <T extends Game> void makeGame(String name,Class<? extends PreserveInfo<T>> preserver,Class<T> gameClass) {

		TableGames.normcmd.put("预定"+name, (event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).addPreserver(new MiraiHumanPlayer((NormalMember) event.getSender()));
		});
		TableGames.normcmd.put(name+"预定列表", (event,command)->{
			event.getGroup().sendMessage(PreserveHolder.getPreserve(event.getGroup(),preserver).getPreserveList());
		});
		TableGames.normcmd.put(name+"统计", (event,command)->{
			if(command.length==1) {
				event.getGroup().sendMessage(new At(event.getSender().getId()).plus(TableGames.db.getPlayer(event.getSender().getId(),name).toString()));
			} else {
				long id=Long.parseLong(command[1]);
				event.getGroup().sendMessage(event.getGroup().get(id).getNameCard()+"的"+TableGames.db.getPlayer(id,name).toString());
			}
		});
		TableGames.normcmd.put("取消预定"+name, (event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).removePreserver(new MiraiHumanPlayer((NormalMember) event.getSender()));
		});
		TableGames.normcmd.put("查询"+name+"参数", (event,command)->{
			event.getGroup().sendMessage(PreserveHolder.getPreserve(event.getGroup(),preserver).getArgs());
		});
		TableGames.privcmd.put("设置"+name+"参数", (event,command)->{
			String[] args=Arrays.copyOfRange(command,1,command.length);
			PreserveHolder.getPreserve(event.getGroup(),preserver).setArgs(args);
			event.getGroup().sendMessage(PreserveHolder.getPreserve(event.getGroup(),preserver).getArgs());
			event.getGroup().sendMessage("特殊场已经设置，欢迎 @我 预定狼人杀 参与。");
		});
		TableGames.privcmd.put("清除"+name+"参数", (event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).clearArgs();
			event.getGroup().sendMessage("狼人杀已经重置为普通场。");
		});
		TableGames.privcmd.put("立即开始"+name, (event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).startNow();
		});
		TableGames.privcmd.put("强制开始"+name, (event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).startForce();
		});
		TableGames.privcmd.put("清空"+name+"预定", (event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).removeAll();
		});
		TableGames.privcmd.put(name+"提醒", (event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).notifyPreserver();
			event.getGroup().sendMessage("已经提醒所有预定玩家");
		});

		TableGames.privcmd.put("强制预定"+name,(event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).addPreserver(new MiraiHumanPlayer(event.getGroup().get(Long.parseLong(command[1]))));
		});
		TableGames.privcmd.put("强制取消预定"+name,(event,command)->{
			PreserveHolder.getPreserve(event.getGroup(),preserver).removePreserver(new MiraiHumanPlayer(event.getGroup().get(Long.parseLong(command[1]))),true);
		});
		TableGames.privcmd.put(name+"统计", (event,command)->{
			event.getGroup().sendMessage(new At(event.getSender().getId()).plus(TableGames.db.getGame(name).getPlayer(event.getSender().getId(),PlayerDatabase.datacls.get(name)).toString()));
		});
		TableGames.privcmd.put("b"+name, (event,command)->{
			PreserveInfo<?> pi=PreserveHolder.getPreserve(event.getGroup(),preserver);
			pi.enablefake=!pi.enablefake;
		});
		TableGames.privcmd.put(name+"全局统计", (event,command)->{
			GenericPlayerData<? extends GenericPlayerData<?>>[] ds=TableGames.db.getPlayers(name);
			for(int i=1;i<ds.length;i++) {
				ds[0].plusa(ds[i]);
			}
			event.getGroup().sendMessage("全局"+ds[0].toString());
		});
		TableGames.privcmd.put("开始"+name, (event,command)->{
			GameUtils.createGame(gameClass,new MiraiGroup(event.getGroup()),Integer.parseInt(command[1]));
			event.getGroup().sendMessage(name+"游戏已经创建，请 @我 报名 来报名。");
		});
		TableGames.privcmd.put("定制"+name, (event,command)->{
			GameUtils.createGame(gameClass,new MiraiGroup(event.getGroup()),Arrays.copyOfRange(command,1,command.length));
			event.getGroup().sendMessage(name+"游戏已经创建，请 @我 报名 来报名。");
		});

	}

	static {
		Random ckr=new SecureRandom();
		List<String> cards=new ArrayList<>();
		cards.add("平民");
		cards.add("平民");
		cards.add("平民");
		cards.add("平民");
		cards.add("平民");
		cards.add("平民");
		cards.add("平民");
		cards.add("平民");
		cards.add("长老");
		cards.add("老流氓");
		cards.add("狼人");
		cards.add("狼人");
		cards.add("狼人");
		cards.add("狼人");
		cards.add("狼人");
		cards.add("狼人");
		cards.add("狼人");
		cards.add("狼人");
		cards.add("石像鬼");
		cards.add("白狼王");
		cards.add("白痴");
		cards.add("预言家");
		cards.add("猎人");
		cards.add("女巫");
		cards.add("守卫");
		cards.add("乌鸦");
		cards.add("骑士");
		cards.add("守墓人");
		cards.add("守卫");
		cards.add("猎魔人");
		TableGames.normcmd.put("狼人杀抽卡",(event,args)->{
			event.getGroup().sendMessage(new At(event.getSender().getId()).plus(cards.get(ckr.nextInt(cards.size()))));
		});
		TableGames.privcmd.put("狼人杀摇号",(event,args)->{
			List<Class<? extends Villager>> larr=WerewolfGame.fairRollRole(Integer.parseInt(args[1]));
			double pts=WerewolfGame.calculateRolePoint(larr);
			StringBuilder sb=new StringBuilder();
			for(Class<? extends Villager> cls:larr) {
				sb.append(WerewolfGame.getName(cls)).append(" ");
			}
			sb.append("得分：").append(pts);
			event.getGroup().sendMessage(sb.toString());
		});
		TableGames.privcmd.put("揭示",(event,args)->{
			Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
			if(g!=null&&g.isAlive()) {
				g.forceShow(new MiraiHumanPlayer((NormalMember) event.getSender()));
			}
		});
		TableGames.privcmd.put("跳过",(event,args)->{
			Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
			if(g!=null&&g.isAlive()) {
				g.forceSkip();
			}
		});
		TableGames.privcmd.put("接管",(event,args)->{
			Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
			if(g!=null&&g.isAlive()) {
				long m1=Long.parseLong(args[1]);
				Member m2=null;
				if(args.length>2) {
					m2=event.getGroup().get(Long.parseLong(args[2]));
				}
				if(g.takeOverMember(m1,new MiraiHumanPlayer((NormalMember) m2))) {
					event.getGroup().sendMessage("接管成功");
				} else {
					event.getGroup().sendMessage("接管失败");
				}
			}
		});
		TableGames.privcmd.put("CMD",(event,args)->{
			Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
			if(g!=null) {
				g.specialCommand(new MiraiHumanPlayer((NormalMember) event.getSender()),Arrays.copyOfRange(args,1,args.length));
			}
		});
		TableGames.privcmd.put("TTI",(event,args)->{
			event.getGroup().sendMessage(Utils.sendTextAsImage(String.join(" ",args), event.getGroup()));
		});
		TableGames.normcmd.put("查询积分",(event,args)->{
			event.getGroup().sendMessage(new At(event.getSender().getId()).plus(TableGames.credit.get(event.getSender().getId()).toString()));
		});
		TableGames.normcmd.put("积分商城",(event,args)->{
			event.getGroup().sendMessage(event.getGroup().uploadImage(Utils.ImageResource(CreditTrade.getList())));
		});
		TableGames.normcmd.put("购买",(event,args)->{
			int is=Integer.parseInt(args[1]);
			if(is>CreditTrade.trades.size()+1||is<1) {
				event.getGroup().sendMessage(new At(event.getSender().getId()).plus("非法商品序号"));
				return;
			}
			if(CreditTrade.trades.get(is-1).execute(event.getSender().getId())) {
				event.getGroup().sendMessage(new At(event.getSender().getId()).plus("购买成功"));
			} else {
				event.getGroup().sendMessage(new At(event.getSender().getId()).plus("购买失败"));
			}
		});
		TableGames.privcmd.put("给积分",(event,args)->{
			double crp=TableGames.credit.get(Long.parseLong(args[1])).givePT(Integer.parseInt(args[2]));
			event.getGroup().sendMessage("添加成功，现有"+crp+"积分");
		});
		TableGames.privcmd.put("扣积分",(event,args)->{
			double crp=TableGames.credit.get(Long.parseLong(args[1])).removePT(Integer.parseInt(args[2]));
			event.getGroup().sendMessage("扣除成功，还剩"+crp+"积分");
		});

		TableGames.privcmd.put("使用积分",(event,args)->{
			double crp;
			if((crp=TableGames.credit.get(Long.parseLong(args[1])).withdrawPT(Integer.parseInt(args[2])))<0) {
				event.getGroup().sendMessage("扣除失败，积分还差"+-crp+"点");
			}
			event.getGroup().sendMessage("扣除成功，还剩"+crp+"积分");
		});
		TableGames.privcmd.put("给物品",(event,args)->{
			int cnt=args.length>3?Integer.parseInt(args[3]):1;
			int crp=TableGames.credit.get(Long.parseLong(args[1])).giveItem(args[2],cnt);
			event.getGroup().sendMessage("添加成功，现有"+crp+"个"+args[2]);
		});
		TableGames.privcmd.put("扣物品",(event,args)->{
			int cnt=args.length>3?Integer.parseInt(args[3]):1;
			int crp=TableGames.credit.get(Long.parseLong(args[1])).removeItem(args[2],cnt);
			event.getGroup().sendMessage("扣除成功，还剩"+crp+"个"+args[2]);
		});
		TableGames.privcmd.put("使用物品",(event,args)->{
			int crp;
			int cnt=args.length>3?Integer.parseInt(args[3]):1;
			if((crp=TableGames.credit.get(Long.parseLong(args[1])).withdrawItem(args[2],cnt))<0) {
				event.getGroup().sendMessage("扣除失败，"+args[2]+"还差"+-crp+"个");
			}
			event.getGroup().sendMessage("扣除成功，还剩"+crp+"个"+args[2]);
		});
		TableGames.makeGame("狼人杀",WerewolfPreserve.class,WerewolfGame.class);
		TableGames.makeGame("谁是卧底",UnderCoverPreserve.class,UnderCoverGame.class);
		TableGames.makeGame("谁是卧底发词",UnderCoverHolderPreserve.class,UnderCoverHolder.class);
		TableGames.makeGame("妙探寻凶",CluePreserve.class,ClueGame.class);
		TableGames.makeGame("妙探寻凶X",FastCluePreserve.class,FastClueGame.class);
		TableGames.makeGame("SP战纪",SpWarframePreserve.class,SpWarframe.class);
	}
	public static void transfer(InputStream i,OutputStream o) throws IOException {
		int nRead;
		byte[] data = new byte[4096];

		try {
			while ((nRead = i.read(data, 0, data.length)) != -1) {
				o.write(data, 0, nRead);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	@Override
	public void onEnable() {
		BotConfiguration.getDefault().setProtocol(MiraiProtocol.ANDROID_PHONE);
		TableGames.plugin=this;
		TableGames.db=new PlayerDatabase(super.getDataFolder());
		TableGames.credit=new PlayerCreditData(super.getDataFolder());
		try {
			File f=new File(super.getDataFolder(),"undtext.txt");
			if(!f.exists()) {
				f.createNewFile();
				FileOutputStream fos=new FileOutputStream(f);
				TableGames.transfer(getResourceAsStream("undtext.txt"),fos);
				fos.close();
			}

			getLogger().info("已载入"+UnderCoverTextLibrary.read(new FileInputStream(new File(super.getDataFolder(),"undtext.txt")))+"词条");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GlobalEventChannel.INSTANCE.registerListenerHost(new SimpleListenerHost(getCoroutineContext()) {
			@EventHandler
			public void onGroup(GroupMessageEvent event) {
				if(event.getGroup().getBotAsMember().getPermission()==MemberPermission.MEMBER)return;

				At at = (At) event.getMessage().get(MessageContent.Key);

				if (at!=null&&at.getTarget() == event.getBot().getId()) {
					MiraiListenerUtils.dispatch(event.getSender().getId(),event.getGroup(),MsgType.AT,(Message) MiraiAdapter.INSTANCE.toUnified(event.getMessage(),event.getBot()));
					{
						String command=Utils.getPlainText(event.getMessage());
						String[] args=command.split(" ");
						BiConsumer<GroupMessageEvent,String[]> bae=TableGames.normcmd.get(args[0]);
						if(bae!=null) {
							bae.accept(event,args);
						}else if(args[0].startsWith("报名")) {
							Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
							if(g!=null&&g.isAlive()) {
								g.addMember(new MiraiHumanPlayer((NormalMember) event.getSender()));
							}
						}else if(event.getSender().getPermission().getLevel()>0) {
							BiConsumer<GroupMessageEvent,String[]> bce=TableGames.privcmd.get(args[0]);
							if(bce!=null) {
								bce.accept(event,args);
							}else if(command.startsWith("强制开始")) {
								Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
								if(g!=null&&g.isAlive()) {
									g.forceStart();
								}
							}else if(command.startsWith("停止游戏")) {
								Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
								if(g!=null) {
									g.forceStop();
								}
								event.getGroup().sendMessage("已经停止正在进行的游戏！");
								event.getSender().sendMessage("已经停止正在进行的游戏！");
							}else if(command.startsWith("暂停游戏")) {
								Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
								if(g!=null) {
									g.forceInterrupt();
								}
								event.getSender().sendMessage("已经暂停正在进行的游戏！");
							}else if(command.startsWith("继续游戏")) {
								Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
								if(g!=null&&g.isAlive()) {
									event.getGroup().sendMessage("因为有其他的游戏正在运行，无法继续。");
									return;
								}
								try(FileInputStream fileOut = new FileInputStream(new File(TableGames.plugin.getDataFolder(),""+event.getGroup()+".game"));ObjectInputStream out = new ObjectInputStream(fileOut)){
									
									GameUtils.getGames().put(new MiraiGroup(event.getGroup()),(Game) out.readObject());
								} catch (IOException | ClassNotFoundException e) {
									// TODO Auto-generated catch block
									event.getGroup().sendMessage("继续游戏失败！");
									e.printStackTrace();
								}

							}else if(args[0].startsWith("强制报名")) {
								Game g=GameUtils.getGames().get(new MiraiGroup(event.getGroup()));
								if(g!=null&&g.isAlive()) {
									g.addMember(new MiraiHumanPlayer(event.getGroup().get(Long.parseLong(args[1]))));
								}
							}else if(args[0].startsWith("执行")) {
								TableGames.dispatchexec.execute(()->MiraiListenerUtils.dispatch(Long.parseLong(args[1]),MsgType.valueOf(args[2]),new Text(args[3]).asMessage()));
							}
						}
					}
				}else {
					TableGames.dispatchexec.execute(()->MiraiListenerUtils.dispatch(event.getSender().getId(),event.getGroup(),MsgType.PUBLIC,(Message) MiraiAdapter.INSTANCE.toUnified(event.getMessage(),event.getBot())));
				}
			}
			@EventHandler
			public void onNewFriend(NewFriendRequestEvent ev) {
				ev.accept();
			}
			@EventHandler
			public void onFriend(FriendMessageEvent event) {
				TableGames.dispatchexec.execute(()->MiraiListenerUtils.dispatch(event.getSender().getId(),MsgType.PRIVATE,(Message) MiraiAdapter.INSTANCE.toUnified(event.getMessage(),event.getBot())));
			}

			@EventHandler
			public void onTemp(StrangerMessageEvent event) {
				TableGames.dispatchexec.execute(()->MiraiListenerUtils.dispatch(event.getSender().getId(),MsgType.PRIVATE,(Message) MiraiAdapter.INSTANCE.toUnified(event.getMessage(),event.getBot())));
			}
		});
	}

}
