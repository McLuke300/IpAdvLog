package me.mcluke300.ipadvlog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ipadvlog  extends JavaPlugin implements Listener{

	private File PlayerFile;
	private File IpFile;
	private FileConfiguration Players;
	private FileConfiguration Ips;
	private HashSet<String> list = new HashSet<String>();
	File subdir = new File("plugins/AdvIpLog/");

	public static ipadvlog plugin;
	
	@Override
	public void onEnable() {
		plugin = this;
		LoadConfiguration();

		//The Ymls
		PlayerFile = new File(getDataFolder(), "players.yml");
		IpFile = new File(getDataFolder(), "ips.yml");

		//Creating the files
		if (!subdir.exists()) {
			subdir.mkdir();
		}
		if (!PlayerFile.exists()) {
			try {
				PlayerFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}
		if (!IpFile.exists()) {
			try {
				IpFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}

		//Yaml configuration
		Players = new YamlConfiguration();
		Ips = new YamlConfiguration();
		loadYamls();

		//Metrics
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit the stats :-(
		}

		//Registering Events
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
	}

	//Config
	public void LoadConfiguration() {
		String path = "IpAdvLog.UseAuthCheck";
		String path2 = "IpAdvLog.Alert.AlertAdmins";
		String path3 = "IpAdvLog.Alert.AlertMessage";
		getConfig().addDefault(path, true);
		getConfig().addDefault(path2, true);
		getConfig().addDefault(path3, "[&cAIL&f]&2%name% &aAlt accounts %alts%");
		getConfig().options().copyDefaults(true);
		saveConfig();

	}

	//Saving The Yamls
	public void saveYamls() {
		try {
			Players.save(PlayerFile);
			Ips.save(IpFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	//Loading the Yamls
	public void loadYamls() {
		try {
			Players.load(PlayerFile);
			Ips.load(IpFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	//On Disable
	@Override
	public void onDisable() {
		list.clear();
	}



	//Join Event
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerEvent(PlayerJoinEvent e) {
		//Adding to list
		Player p = e.getPlayer();
		String playername = p.getName();
		if(plugin.getConfig().getBoolean("IpAdvLog.UseAuthCheck")) {
		list.add(playername);
		} else {
			addYaml(playername, p.getAddress().getAddress().getHostAddress());	
		}
		
		//Alert on alt accounts
		if (plugin.getConfig().getBoolean("IpAdvLog.Alert.AlertAdmins")  && !p.hasPermission("ail.ignorealert")) {
			List<String> alts = new ArrayList<String>();
			if (Players.getStringList(playername.toLowerCase()).isEmpty()) {
			} else {
				for (String b : Players.getStringList(playername.toLowerCase())) {
					b = b.replaceAll("\\.", "_");
					for (String c : Ips.getStringList(b)) {
						if (!alts.contains(c)) {
							alts.add(c);
						}
					}
				}
			}
			if (alts.size() > 1) {
				String msg = plugin.getConfig().getString("IpAdvLog.Alert.AlertMessage");
				msg = msg.replaceAll("%name%", playername);
				msg = msg.replaceAll("%alts%", alts.toString());
				msg = msg.replaceAll("&([0-9a-fA-F])", "§$1");
				for (Player c : Bukkit.getServer().getOnlinePlayers()) {
					if (c.hasPermission("ail.alert")) {
						c.sendMessage("["+ChatColor.RED+"AIL"+ChatColor.WHITE+"]"+ChatColor.DARK_GREEN+playername+ChatColor.GREEN+" Alt Accounts " + alts);
					}
				}
			}
		}

	}




	//On Move event
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent e) {
		if(plugin.getConfig().getBoolean("IpAdvLog.UseAuthCheck")) {
		if (list.contains(e.getPlayer().getName())) {
			list.remove(e.getPlayer().getName());
			if (e.isCancelled() == false) {
				Player p = e.getPlayer();
				String playername = p.getName();
				playername = playername.toLowerCase();
				String ip = p.getAddress().getAddress().getHostAddress();
				list.remove(playername);
				addYaml(playername, ip);
				}}}}
	
	
	//Adding and removing from yaml
	public void addYaml(String playername, String ip) {
		if (Players.getStringList(playername) == null) {
			List<String> list = new ArrayList<String>();
			list.add(ip);
			Players.addDefault(playername, list);
		} else {
			List<String> list2 = Players.getStringList(playername);
			if (!list2.contains(ip)) {
				list2.add(ip);
				Players.set(playername, list2);
			}

		}
		try {
			Players.save(PlayerFile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}



		//Ips.yml
		ip = ip.replaceAll("\\.", "_");
		if (Ips.getStringList(ip) == null) {
			List<String> list3 = new ArrayList<String>();
			list3.add(playername);
			Ips.addDefault(ip, list3);
		} else {
			List<String> list4 = Ips.getStringList(ip);
			Boolean doesit = false;
			for (String b : list4) {
				if (b.equalsIgnoreCase(playername)) {
					doesit = true;
				}}
			if (!doesit) {
				list4.add(playername);
				Ips.set(ip, list4);
			}
			try {
				Ips.save(IpFile);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}}}



	//Join Event
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		String playername = p.getName();
		if (list.contains(playername)) {
			list.remove(playername);
		}}




	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (commandLabel.equalsIgnoreCase("ail")) {

			//Command Guide
			if (args.length < 1) {
				sender.sendMessage(ChatColor.DARK_GREEN+"====="+ChatColor.BLUE+"AdvIpLog"+ChatColor.DARK_GREEN+"=====");
				sender.sendMessage(ChatColor.DARK_GREEN+"/ail player <PlayerName>"+ChatColor.BLUE+"  Lists all the ips the player uses");
				sender.sendMessage(ChatColor.DARK_GREEN+"/ail ip <ip>"+ChatColor.BLUE+"  Lists all players using that ip");
				sender.sendMessage(ChatColor.DARK_GREEN+"/ail check <PlayerName>"+ChatColor.BLUE+"  Will find all players alternate accounts");
				sender.sendMessage(ChatColor.DARK_GREEN+"/ail reload"+ChatColor.BLUE+"  Will reload the plugin");
			} else {

				//Usage If only 1 Arg
				if (args.length == 1) {
					if (args[0].equalsIgnoreCase("player")) {
						if (sender.hasPermission("ail.player")) {
							sender.sendMessage(ChatColor.RED+"USAGE: "+ChatColor.GREEN+"/ail player <PlayerName>");
						}} else {
							if (args[0].equalsIgnoreCase("ip")) {
								if (sender.hasPermission("ail.ip")) {
									sender.sendMessage(ChatColor.RED+"USAGE: "+ChatColor.GREEN+"/ail ip <ip>");
								}} else if (args[0].equalsIgnoreCase("check")) {
									if (sender.hasPermission("ail.ip")) {
										sender.sendMessage(ChatColor.RED+"USAGE: "+ChatColor.GREEN+"/ail check <PlayerName>");
									}}else if (args[0].equalsIgnoreCase("reload")) {
										if (sender.hasPermission("ail.reload")) {
											getConfig();
											reloadConfig();
											getServer().getPluginManager().disablePlugin(plugin);
											getServer().getPluginManager().enablePlugin(plugin);
											sender.sendMessage(ChatColor.GREEN+"AdvIpLog Reloaded");
										}}}}

										//Two Args
										if (args.length == 2) {


											//player command
											if (args[0].equalsIgnoreCase("player")) {
												if (sender.hasPermission("ail.player")) {
													if (Players.getStringList(args[1].toLowerCase()) != null) {
														if (Players.getStringList(args[1].toLowerCase()).isEmpty()) {
															sender.sendMessage(ChatColor.RED+"Cannot find player "+args[1]);
														} else {
															sender.sendMessage(ChatColor.GREEN+"Ip Addresses used by "+ChatColor.BLUE+args[1]);
															for (String a : Players.getStringList(args[1].toLowerCase())) {
																List<String> checker = new ArrayList<String>();
																a = a.replaceAll("\\.", "_");
																for (String c : Ips.getStringList(a)) {
																	if (!checker.contains(c)) {
																		checker.add(c);
																	}
																}
																String alts = "";
																if (checker.size() > 1) {
																	alts = "[" +checker.size() + " Users]";
																} else {
																	alts = "";
																}

																a = a.replaceAll("\\_", ".");
																sender.sendMessage(ChatColor.DARK_GREEN+"- "+a+"  "+ChatColor.RED +alts);
															}}}}}else {

																//Ip Command
																if (args[0].equalsIgnoreCase("ip")) {
																	if (sender.hasPermission("ail.ip")) {
																		if (Ips.getStringList(args[1].replaceAll("\\.", "_")) != null) {
																			if (Ips.getStringList(args[1].replaceAll("\\.", "_")).isEmpty()) {
																				sender.sendMessage(ChatColor.RED+"Cannot find Ip "+args[1]);
																			} else {
																				sender.sendMessage(ChatColor.GREEN+"Players with Ip "+ChatColor.BLUE+args[1]);
																				for (String a : Ips.getStringList(args[1].replaceAll("\\.", "_"))) {
																					sender.sendMessage(ChatColor.DARK_GREEN+"- "+a);



																					//Check Command
																				}}}}} else if (args[0].equalsIgnoreCase("check")) {
																					if (sender.hasPermission("ail.check")) {
																						List<String> checker = new ArrayList<String>();
																						if (Players.getStringList(args[1].toLowerCase()).isEmpty()) {
																							sender.sendMessage(ChatColor.RED+"Cannot find player "+args[1]);
																						} else {
																							for (String b : Players.getStringList(args[1].toLowerCase())) {
																								b = b.replaceAll("\\.", "_");
																								for (String c : Ips.getStringList(b)) {
																									if (!checker.contains(c)) {
																										checker.add(c);
																									}
																								}
																							}
																							sender.sendMessage(ChatColor.GREEN+"Other accounts from user  "+ChatColor.BLUE+args[1]);
																							for (String d : checker) {
																								sender.sendMessage(ChatColor.DARK_GREEN+"- "+d );
																							}

															}}}}}}}
			return false;

		}
}


