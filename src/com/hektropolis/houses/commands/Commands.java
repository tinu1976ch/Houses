package com.hektropolis.houses.commands;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;
import org.bukkit.util.FileUtil;

import com.hektropolis.houses.Errors;
import com.hektropolis.houses.Houses;
import com.hektropolis.houses.Permissions;
import com.hektropolis.houses.Ranks;
import com.hektropolis.houses.Utils;
import com.hektropolis.houses.config.Config;
import com.hektropolis.houses.database.DatabaseQuery;
import com.hektropolis.houses.database.DatabaseSynchronizer;
import com.hektropolis.houses.signs.HouseSign;
import com.hektropolis.houses.signs.RentSign;

public class Commands implements CommandExecutor {

	private Houses plugin;
	private Ranks ranks;
	private Helper helper;
	private Errors error;
	private CommandSender sender;
	private String[] args;
	private YamlConfiguration config;
	private Config housesConfig;
	private HashMap<String, Long> cooldowns;
	public Commands(Houses plugin, Config config) {
		this.plugin = plugin;
		this.ranks = new Ranks(plugin);
		this.housesConfig = config;
		this.cooldowns = new HashMap<String, Long>();
	}

	private ChatColor green = ChatColor.GREEN;
	private ChatColor dAqua = ChatColor.DARK_AQUA;
	private ChatColor dGray = ChatColor.DARK_GRAY;
	private ChatColor gold = ChatColor.GOLD;

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("house")) {
			this.helper = new Helper(sender);
			this.sender = sender;
			this.args = args;
			this.error = new Errors(sender);
			this.config = housesConfig.getConfig();

			if (args.length == 0) {
				helper.showCommands(1);
				return true;
			}

			Method[] methods = this.getClass().getMethods();

			for (Method method : methods) {
				HousesCommand command = method.getAnnotation(HousesCommand.class);
				if (command != null) {
					if (command.name().equalsIgnoreCase(args[0])) {
						try {
							if (Permissions.hasPerm(sender, args)) {
								method.invoke(this);
							} else
								sender.sendMessage("§cYou do not have permission to perform this command");
							return true;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			sender.sendMessage("§cUnknown Houses command. Type §4/house help§c to see a list of commands for Houses");
		}
		return true;
	}

	@HousesCommand(name = "help")
	public void help() {
		if (args.length == 1) {
			helper.showCommands(1);
		} else if (args.length == 2) {
			if (Utils.isInt(args[1]))
				helper.showCommands(Integer.parseInt(args[1]));
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "reload")
	public void reload() {
		housesConfig.reload();
		sender.sendMessage(green + "Houses config reloaded");
	}

	@HousesCommand(name = "all")
	public void all() {
		if (args.length == 1) {
			try {
				ResultSet rsRow = Houses.sqlite.query("SELECT COUNT(*) AS total FROM houses");
				int total = rsRow.getInt("total");
				int pages = (int)Math.ceil((double)total/8);
				sender.sendMessage(dGray + "---- §6Houses: §2" + total + dGray + " -- §6Page §2" + 1 + "§6/§2" + pages + dGray + " ----");
				rsRow.close();
				ResultSet rs = Houses.sqlite.query("SELECT player, class, number, world, '' AS tableName FROM houses " +
						"UNION ALL SELECT player, class, number, world, 'rented' AS tableName FROM rentals ORDER BY class ASC, number DESC");
				Utils.printQuery(plugin, rs, sender, 1, config.getBoolean("worlds.display-worlds"));
				sender.sendMessage("§6Type §2/house all 2§6 to read the next page");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else if (args.length <= 3) {
			World world = null;
			if (args.length == 3) {
				world = plugin.getServer().getWorld(args[2]);
				if (world == null) {
					error.severe("World " + args[2] + " does not exist");
					return;
				}
			}
			if (Utils.isInt(args[1])) {
				try {
					ResultSet rsRow = Houses.sqlite.query("SELECT COUNT(*) AS total FROM houses");
					int pageInput = Integer.parseInt(args[1]);
					int total = rsRow.getInt("total");
					int pages = (int)Math.ceil((double)total/8);
					sender.sendMessage(dGray + "---- §6Houses: §2" + total + dGray + " -- §6Page §2" + args[1] + "§6/§2" + pages + dGray + " ----");
					rsRow.close();
					if (args.length == 2) {
						ResultSet rs = Houses.sqlite.query("SELECT player, class, number, world, '' AS tableName FROM houses " +
								"UNION ALL SELECT player, class, number, world, 'rented' AS tableName FROM rentals ORDER BY class ASC, number DESC");
						Utils.printQuery(plugin, rs, sender, pageInput, config.getBoolean("worlds.display-worlds"));
						sender.sendMessage("§6Type §2/house all " + (pageInput + 1) + "§6 to read the next page");
					}
					if (args.length == 3) {
						ResultSet rs = Houses.sqlite.query("SELECT player, class, number, world, '' AS tableName FROM houses WHERE world='" + world.getName() + "'" +
								"UNION ALL SELECT player, class, number, world, 'rented' AS tableName FROM rentals WHERE world='" + world.getName() + "' ORDER BY class ASC, number DESC");
						Utils.printQuery(plugin, rs, sender, pageInput, false);
						sender.sendMessage("§6Type §2/house all " + (pageInput + 1) + " " + world.getName() + "§6 to read the next page");
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} else
				error.notify("Page must be a number");
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "me")
	public void me() {
		sender.sendMessage("§6Showing houses owned by §2you:");
		sender.sendMessage(dGray + "------------------------------------");
		ResultSet rs;
		try {
			rs = Houses.sqlite.query("SELECT player, class, number, world, '' AS tableName FROM houses WHERE player='" + sender.getName() + "' " +
					"UNION ALL SELECT player, class, number, world, 'rented' AS tableName FROM rentals WHERE player='" + sender.getName() + "' ORDER BY class ASC, number DESC");
			Utils.printQuery(plugin, rs, sender, 0, true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@HousesCommand(name = "expiration")
	public void expiration() {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			try {
				ResultSet rs = Houses.sqlite.query("SELECT * FROM rentals WHERE player='" + player.getName() + "'");
				int results = 0;
				while (rs.next()) {
					results++;
					player.sendMessage("§6Your rental at class §2" + rs.getInt("class") + "§6 number §2" + rs.getInt("number") +
							"§6 expires in §2" + Utils.getTimeLeft(rs.getInt("expires")));
				}
				if (results == 0) {
					error.notify("You don't rent any house");
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else
			sender.sendMessage("§cA player is expected");
	}

	@HousesCommand(name = "info")
	public void info() {
		if (args.length == 1) {
			int houses = 0;
			int classes = 0;
			int owned = 0;
			int lowPrice = 0;
			int highPrice = 0;
			try {
				ResultSet rsHouses = Houses.sqlite.query("SELECT COUNT(*) AS houses FROM signs WHERE type='buy'");
				houses = rsHouses.getInt("houses");
				rsHouses.close();
				ResultSet rsClasses = Houses.sqlite.query("SELECT COUNT(DISTINCT class) AS classes FROM signs");
				classes = rsClasses.getInt("classes");
				rsClasses.close();
				ResultSet rsOwned = Houses.sqlite.query("SELECT COUNT(*) AS owned FROM houses");
				owned = rsOwned.getInt("owned");
				rsOwned.close();
				ResultSet rsLowPrice = Houses.sqlite.query("SELECT MIN(price) AS lowPrice FROM signs");
				lowPrice = rsLowPrice.getInt("lowPrice");
				rsLowPrice.close();
				ResultSet rsHighPrice = Houses.sqlite.query("SELECT MAX(price) AS highPrice FROM signs");
				highPrice = rsHighPrice.getInt("highPrice");
				rsHighPrice.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			sender.sendMessage(dGray + "--- §6General House Info " + dGray + "---");
			sender.sendMessage("§2Houses: " + dAqua + houses);
			sender.sendMessage("§2Classes: " + dAqua + classes);
			sender.sendMessage("§2Owned: " + dAqua + owned);
			sender.sendMessage("§2Avaliable: " + dAqua + (houses - owned));
			sender.sendMessage("§2Price range: " + dAqua + lowPrice + "-" + highPrice);
			sender.sendMessage("§6Type §2/house info [class]§6 to see info for a class");
		} else if (args.length == 2) {
			if (Utils.isClass(sender, args[1])) {
				if (!config.isString("classes." + args[1] + ".info")) {
					sender.sendMessage("§cNo info is available for class " + args[1]);
					return;
				}
				String info = config.getString("classes." + args[1] + ".info");
				String[] lines = info.split("%n");
				sender.sendMessage("§2Class " + args[1] + ": ");
				for (String msg : lines) sender.sendMessage(dAqua + msg);
			}
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "player")
	public void player() {
		if (args.length == 2) {
			OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args[1]);
			if (!player.hasPlayedBefore()) {
				sender.sendMessage("§cPlayer " + args[1] + " has not played on this server");
				return;
			}
			sender.sendMessage("§6Showing houses owned by: §2" + player.getName());
			sender.sendMessage(dGray + "------------------------------------");
			ResultSet rs;
			try {
				rs = Houses.sqlite.query("SELECT player, class, number, world, '' AS tableName FROM houses WHERE player='" + player.getName() + "' " +
						"UNION ALL SELECT player, class, number, world, 'rented' AS tableName FROM rentals WHERE player='" + args[1] + "' ORDER BY class ASC, number DESC");
				Utils.printQuery(plugin, rs, sender, 0, true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "class")
	public void houseClass() {
		if (args.length == 2) {
			if (Utils.isClass(sender, args[1])) {
				sender.sendMessage("§6Showing houses of class: §2" + args[1]);
				sender.sendMessage(dGray + "------------------------------------");
				ResultSet rs;
				try {
					rs = Houses.sqlite.query("SELECT player, class, number, world, '' AS tableName FROM houses WHERE class='" + args[1] + "' " +
							"UNION ALL SELECT player, class, number, world, 'rented' AS tableName FROM rentals WHERE class='" + args[1] + "' ORDER BY class ASC, number DESC");
					Utils.printQuery(plugin, rs, sender, 0, true);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "number")
	public void houseNumber() {
		if (args.length == 2) {
			if (Utils.isNumber(sender, args[1])) {
				sender.sendMessage("§6Showing houses of number: §2" + args[1]);
				sender.sendMessage(dGray + "------------------------------------");
				ResultSet rs;
				try {
					rs = Houses.sqlite.query("SELECT player, class, number, world, '' AS tableName FROM houses WHERE number='" + args[1] + "' " +
							"UNION ALL SELECT player, class, number, world, 'rented' AS tableName FROM rentals WHERE number='" + args[1] + "' ORDER BY class ASC, number DESC");
					Utils.printQuery(plugin, rs, sender, 0, true);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "guests")
	public void guests() {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			int houseClass = 0;
			int houseNumber = 0;
			if (args.length == 1) {
				ResultSet homeRs;
				try {
					homeRs = Houses.sqlite.query("SELECT class, number FROM houses WHERE player='" + player.getName() + "' UNION ALL SELECT class, number FROM rentals WHERE player='" + player.getName() + "' ORDER BY class ASC, number DESC");
					if (homeRs.next()) {
						houseClass = homeRs.getInt("class");
						houseNumber = homeRs.getInt("number");
					}
					homeRs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			else if (args.length == 3) {
				if (Utils.isClass(sender, args[1]) && Utils.isNumber(sender, args[2])) {
					houseClass = Integer.parseInt(args[1]);
					houseNumber = Integer.parseInt(args[2]);
				} else
					return;
			} else {
				helper.showUsage(args[0]);
				return;
			}
			String[] guests = new DatabaseQuery(player.getWorld().getName(), houseClass, houseNumber).getGuests();
			player.sendMessage("§6Players who are guests at class §2" + houseClass + "§6 number §2" + houseNumber + "§6:");
			for (String guest : guests)
				player.sendMessage(dAqua + guest);
		} else
			sender.sendMessage("§cA player is excpected");
	}

	@HousesCommand(name = "backup")
	public void backup() {
		sender.sendMessage("Backing up database...");
		String databasesPath = plugin.getDataFolder().getAbsolutePath() + File.separator + "Databases";
		File backupsPath = new File(databasesPath + File.separator + "Backups" + File.separator);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		if (FileUtil.copy(new File(databasesPath + File.separator + "houses.db"),
				new File(backupsPath + File.separator + "houses_" + dateFormat.format(new Date()) + ".db")))
			sender.sendMessage(green + "Backup successful: houses_" + dateFormat.format(new Date()) + ".db");
		else
			new Errors(sender).severe("Backup failed");
	}

	@HousesCommand(name = "home")
	public void home() {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			//If the player is registered in the hashmap.
			long millis = System.currentTimeMillis();
			if (cooldowns.containsKey(player.getName())) {
				//Check where I put the info in the hashmap, but basically I set the cooldown to current time + extra time. So if the current time catches up to the stored time, then the cooldown is over.
				if (cooldowns.get(player.getName()) > millis) {
					error.notify("You can't perform this command yet!");
					return;
				} else {
					//Cooldown is over
					cooldowns.remove(player.getName());
				}
			}
			int houseClass = 0;
			int houseNumber = 0;
			try {
				ResultSet rsP = Houses.sqlite.query("SELECT class, number FROM houses WHERE player='" + sender.getName() + "' " +
						"UNION ALL SELECT class, number FROM rentals WHERE player='" + sender.getName() + "' ORDER BY class ASC, number DESC");
				if (rsP.next()) {
					houseClass = rsP.getInt("class");
					houseNumber = rsP.getInt("number");
					rsP.close();
				} else
					error.severe("You do not have access to a house");
				rsP.close();
				ResultSet rsS = Houses.sqlite.query("SELECT * FROM signs WHERE class=" + houseClass + " AND number=" + houseNumber);
				Block doorBlock = null;
				if (rsS.next()) {
					sender.sendMessage("§6Teleporting to class §c" + rsS.getInt("class") + "§6 number §c" + rsS.getInt("number") + "...");
					Block sign = player.getWorld().getBlockAt(rsS.getInt("x"), rsS.getInt("y"), rsS.getInt("z"));
					if (sign.getType() == Material.WALL_SIGN)
						doorBlock = Utils.getDoorFromSign((Sign) sign.getState());
					rsS.close();
					if (doorBlock != null) {
						BlockState state = doorBlock.getState();
						Door door = (Door) state.getData();
						Location loc = doorBlock.getRelative(door.getFacing().getOppositeFace()).getLocation();
						player.teleport(loc);
						while (!(player.getLocation().getBlock().getType() == Material.AIR) && player.getLocation().getBlockY() < 253) {
							player.teleport(player.getLocation().add(0, 1, 0));
						}
						double cooldown = config.getDouble("home-teleport-cooldown");
						if (cooldown > 0)
							cooldowns.put(player.getName(), (long) (System.currentTimeMillis() + 1000*config.getDouble("home-teleport-cooldown")));
					} else error.notify("No house door found");
				} else error.notify("No house sign found");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else error.severe("You must be a player to teleport lol!");
	}

	@HousesCommand(name = "tp")
	public void tp() {
		if (args.length == 3) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				ResultSet rs;
				try {
					rs = Houses.sqlite.query("SELECT * FROM signs WHERE class='" + args[1] + "' AND number='" + args[2] + "'");
					if (rs.next()) {
						sender.sendMessage("§6Teleporting to class §c" + rs.getInt("class") + "§6 number §c" + rs.getInt("number") + "§6...");
						Block sign = player.getWorld().getBlockAt(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
						Block doorBlock = Utils.getDoorFromSign((Sign) sign.getState());
						BlockState state = doorBlock.getState();
						Door door = (Door) state.getData();
						Location loc = doorBlock.getRelative(door.getFacing().getOppositeFace()).getLocation().add(0, 1, 0);
						player.teleport(loc);
					} else
						error.notify("No house matches your request");
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@HousesCommand(name = "add")
	public void add() {
		if (args.length == 5 || args.length == 6) {
			if (args[1].equalsIgnoreCase("owner")) {
				World world = Utils.getWorldFromCommand(plugin, sender, args, "add owner", 6);
				if (world != null) {
					if (Utils.isClass(sender, args[3]) && Utils.isNumber(sender, args[4])) {
						if (Utils.isInt(args[3])) {
							DatabaseQuery DBQuery = new DatabaseQuery(world.getName(), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
							if(DBQuery.anyoneHasHouse())
								error.notify("This house is already owned by " + ChatColor.DARK_RED + DBQuery.getHouseOwner());
							else if (DBQuery.anyoneHasRental())
								error.notify("This house is already rented by " + ChatColor.DARK_RED + DBQuery.getRentalOwner());
							else {
								OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args[2]);
								if (player.hasPlayedBefore()) {
									if (!DBQuery.hasTooMany(plugin, player.getName())) {
										sender.sendMessage("§6You added owner:");
										sender.sendMessage(dGray + "------------------------------------");
										sender.sendMessage("§2Player: " + dAqua + player.getName() + "§2 class: " + dAqua + args[3] + "§2 number: " + dAqua + args[4]);
										Utils.broadcastHouse(player.getName(), args[3], args[4], "bought", "built by §3" + DBQuery.getBuilder());
										ranks.setRank(player.getName(), args[3], true);
										try {
											Houses.sqlite.query("INSERT INTO houses(player, class, number, world) VALUES('" + player.getName() + "', '" + args[3] + "', '" + args[4] + "', '" + world.getName() + "')");
										} catch (SQLException e) {
											e.printStackTrace();
										}
									} else
										error.notify(player.getName() + " owns too many houses");
								} else
									error.severe("Player " + player.getName() + " has never played on this server");
							}
						} else {
							OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args[2]);
							if (player.hasPlayedBefore()) {
								sender.sendMessage("§6You added owner:");
								sender.sendMessage(dGray + "------------------------------------");
								sender.sendMessage("§2Player: " + dAqua + player.getName() + "§2 class: " + dAqua + args[3] + "§2 number: " + dAqua + args[4]);
								plugin.getServer().broadcastMessage(dAqua + player.getName() + "§2 bought the rights to the great " + dAqua + args[3] + "§2 house at number " + dAqua + args[4]);
								//Houses.permission.playerAddGroup((Player) player, args[3]);
								ranks.setRank(player.getName(), args[3], true);
								try {
									Houses.sqlite.query("INSERT INTO houses(player, class, number) VALUES('" + player.getName() + "', '" + args[3] + "', '" + args[4] + "')");
								} catch (SQLException e) {
									e.printStackTrace();
								}
							} else error.severe("Player " + player.getName() + " has never played on this server");
						}
					} else
						helper.showUsage(args[0] + " " + args[1]);
				}
			}
			else if (args[1].equalsIgnoreCase("guest")) {
				World world = Utils.getWorldFromCommand(plugin, sender, args, "add guest", 6);
				if (world != null) {
					if (Utils.isClass(sender, args[3]) && Utils.isNumber(sender, args[4])) {
						if (plugin.getServer().getOfflinePlayer(args[2]).hasPlayedBefore()) {
							DatabaseQuery dbQuery = new DatabaseQuery(world.getName(), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
							if (dbQuery.playerHasHouse(sender.getName()) || dbQuery.playerHasRental(sender.getName()) || Permissions.hasPerm(sender, "add.guest.others")) {
								String type = "";
								try {
									if (dbQuery.anyoneHasHouse())
										type = "owned";
									else if (dbQuery.anyoneHasRental())
										type = "rented";
									else {
										error.notify("Nobody owns or rents that house");
										return;
									}
									ResultSet rs = Houses.sqlite.query("SELECT player FROM guests WHERE house_id=" + dbQuery.getHouseId() + " AND type='" + type + "'");
									while (rs.next()) {
										if (args[2].equalsIgnoreCase(rs.getString("player"))) {
											sender.sendMessage("§2Player §3" + args[2] + " §2 is already a guest at that house");
											rs.close();
											return;
										}
									}
									rs.close();
								} catch (SQLException e1) {
									e1.printStackTrace();
								}
								try {
									Houses.sqlite.query("INSERT INTO guests(house_id, player, type) VALUES(" + dbQuery.getHouseId() + ", '" + args[2] + "', '" + type + "')");
									sender.sendMessage("§2Player §3" + args[2] + "§2 is now a guest at class §3" + args[3] + "§2 number §3" + args[4]);
								} catch (SQLException e) {
									e.printStackTrace();
								}
							} else
								error.notify("You don't own or rent that house");
						} else
							error.notify(args[2] + " has never played on this server");
					} else
						helper.showUsage("add guest");
				} else
					helper.showUsage(args[0] + " " + args[1]);
			}
		}
		else if (args.length == 7 || args.length == 8) {
			if (args[1].equalsIgnoreCase("rental")) {
				World world = Utils.getWorldFromCommand(plugin, sender, args, "add rental", 8);
				if (world != null) {
					if (Utils.isClass(sender, args[3]) && Utils.isNumber(sender, args[4])) {
						DatabaseQuery DBQuery = new DatabaseQuery(world.getName(), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
						if(DBQuery.anyoneHasRental())
							error.notify("This house is already rented by " + ChatColor.DARK_RED + DBQuery.getRentalOwner());
						else if(DBQuery.anyoneHasHouse())
							error.notify("This house is already owned by " + ChatColor.DARK_RED + DBQuery.getHouseOwner());
						else {
							OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args[2]);
							if (player.hasPlayedBefore()) {
								if (!DBQuery.hasTooMany(plugin, player.getName())) {
									sender.sendMessage("§6You added rental:");
									sender.sendMessage(dGray + "------------------------------------");
									sender.sendMessage("§2Player: " + dAqua + player.getName() + "§2 class: " + dAqua + args[3] + "§2 number: " + dAqua + args[4] +
											"§2 days: " + dAqua + args[5] + "§2 hours: " + dAqua + args[6]);
									Utils.broadcastHouse(player.getName(), args[3], args[4], "rented", "built by §3" + DBQuery.getBuilder());
									ranks.setRank(player.getName(), args[3], true);
									DBQuery.insertRental(player.getName(), Integer.parseInt(args[5]), Integer.parseInt(args[6]));
								} else
									error.notify(player.getName() + " owns too many houses");
							} else
								error.severe("Player " + player.getName() + " has never played on this server");
						}
					} else
						helper.showUsage(args[0] + " " + args[1]);
				}
			}
		} else {
			if (args.length >= 2) {
				helper.showUsage(args[0] + " " + args[1]);
			} else
				helper.showCommands(2);
		}
	}

	@HousesCommand(name = "remove")
	public void remove() {
		if (args.length == 5 || args.length == 6) {
			if (args[1].equalsIgnoreCase("owner")) {
				World world = Utils.getWorldFromCommand(plugin, sender, args, "remove owner", 6);
				if (world != null) {
					if (Utils.isClass(sender, args[3]) && Utils.isNumber(sender, args[4])) {
						DatabaseQuery DBQuery = new DatabaseQuery(world.getName(), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
						OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args[2]);
						if(DBQuery.playerHasHouse(player.getName())) {
							DBQuery.deleteOwner(player.getName());
							sender.sendMessage("§6You removed owner:");
							sender.sendMessage(dGray + "------------------------------------");
							sender.sendMessage("§2Player: " + dAqua + player.getName() + "§2 class: " + dAqua + args[3] + "§2 number: " + dAqua + args[4]);
							Utils.broadcastHouse(player.getName(), args[3], args[4], "sold", "");
							ranks.setRank(player.getName(), args[3], false);
						} else
							error.warning("Player " + player.getName() + " does not own that house");
					} else
						helper.showUsage(args[0] + " " + args[1]);
				}
			}
			else if (args[1].equalsIgnoreCase("rental")) {
				World world = Utils.getWorldFromCommand(plugin, sender, args, "add owner", 6);
				if (world != null) {
					if (Utils.isClass(sender, args[3]) && Utils.isNumber(sender, args[4])) {
						DatabaseQuery DBQuery = new DatabaseQuery(world.getName(), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
						OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args[2]);
						if(DBQuery.playerHasRental(player.getName())) {
							DBQuery.deleteRental(player.getName());
							try {
								Houses.sqlite.query("DELETE FROM guests WHERE house_id=" + DBQuery.getHouseId() + " AND type='rented'");
							} catch (SQLException e) {
								e.printStackTrace();
							}
							sender.sendMessage("§6You removed rental:");
							sender.sendMessage(dGray + "------------------------------------");
							sender.sendMessage("§2Player: " + dAqua + player.getName() + "§2 class: " + dAqua + args[3] + "§2 number: " + dAqua + args[4]);
							Utils.broadcastHouse(player.getName(), args[3], args[4], "lost access to", "");
							ranks.setRank(player.getName(), args[3], false);
						} else
							error.warning("Player " + player.getName() + " haven't rented that house");
					} else
						helper.showUsage(args[0] + " " + args[1]);
				}
			}
			else if (args[1].equalsIgnoreCase("guest")) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					if (Utils.isClass(sender, args[3]) && Utils.isNumber(sender, args[4])) {
						DatabaseQuery dbQuery = new DatabaseQuery(player.getWorld().getName(), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
						if (dbQuery.playerHasHouse(player.getName()) || dbQuery.playerHasRental(player.getName()) || Permissions.hasPerm(player, "remove.guest.others")) {
							boolean isGuest = false;
							try {
								ResultSet rs = Houses.sqlite.query("SELECT player FROM guests WHERE house_id=" + dbQuery.getHouseId());
								while (rs.next()) {
									if (args[2].equalsIgnoreCase(rs.getString("player")))
										isGuest = true;
								}
								rs.close();
								if (isGuest) {
									dbQuery.deleteGuest(args[2]);
									player.sendMessage("§2You removed player §3" + args[2] + "§2 as a guest at class §3" + args[3] + "§2 number §3" + args[4]);
								} else
									error.notify("Player " + args[2] + " is not a guest at that house");
							} catch (SQLException e) {
								e.printStackTrace();
							}
						} else
							error.notify("Could not remove guest");
					}
				} else
					error.notify("Player excpected");
			}
		} else {
			if (args.length >= 2) {
				helper.showUsage(args[0] + " " + args[1]);
			} else
				helper.showCommands(2);
		}
	}

	@HousesCommand(name = "ranks")
	public void ranks() {
		if (args.length == 1) {
			sender.sendMessage("§6Showing house ranks:");
			sender.sendMessage(dGray + "------------------------------------");
			for (int i=1; i<=100; i++) {
				if (config.isString("classes." + i + ".rank")) {
					String rank = config.getString("classes." + i + ".rank");
					sender.sendMessage("§2Class " + dAqua + i + "§2: " + gold + rank);
				}
			}
		}
		else if (args.length == 2) {
			if (Utils.isClass(sender, args[1]))
				if (config.isString("classes." + args[1] + ".rank")) {
					String rank = config.getString("classes." + args[1] + ".rank");
					sender.sendMessage("§2Class " + dAqua + args[1] + "§2 has rank: " + dAqua + rank);
				} else
					error.severe("Class " + args[1] + " doesn't have a pre-defined rank");
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "prices")
	public void prices() {
		if (args.length == 1) {
			sender.sendMessage("§6Showing house prices:");
			sender.sendMessage(dGray + "------------------------------------");
			for (String i : config.getConfigurationSection("classes").getKeys(false)) {
				if (config.isInt("classes." + i + ".price") || config.isInt("classes." + i + ".per-day-cost")) {
					String price = "";
					String perDayCost = "";
					if (config.isInt("classes." + i + ".price")) {
						price = config.getString("classes." + i + ".price");
					}
					if (config.isInt("classes." + i + ".per-day-cost")) {
						perDayCost = "§2   per day: " + gold + config.getString("classes." + i + ".per-day-cost");
					}
					sender.sendMessage("§2Class " + dAqua + i + "§6: $" + price + perDayCost);
				}
			}
		}
		else if (args.length == 2) {
			if (Utils.isClass(sender, args[1]))
				if (config.isInt("classes." + args[1] + ".price")) {
					int price = config.getInt("classes." + args[1] + ".price");
					String dayCost =  "or $" + config.getInt("classes."  + args[1] + ".per-day-cost") + " per day";
					sender.sendMessage("§2Class " + dAqua + args[1] + "§2 costs $" + price + " to buy " + dayCost);
				} else
					error.severe("Class " + args[1] + " doesn't have a pre-defined price");
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "setprice")
	public void setPrice() {
		if (args.length == 4 || args.length == 5) {
			World world = Utils.getWorldFromCommand(plugin, sender, args, "setprice", 5);
			if (world != null) {
				if (Utils.isInt(args[2]) && Utils.isInt(args[3]) && (args[1].equalsIgnoreCase("day") || args[1].equalsIgnoreCase("buy"))) {
					sender.sendMessage("§2Class " + dAqua + args[2] + "§2 " +  args[1] + " price set to " + dAqua + args[3]);
					try {
						if (args[1].equalsIgnoreCase("buy")) {
							Houses.sqlite.query("UPDATE signs SET price='" + args[3] + "' WHERE class='" + args[2] + "' AND type='buy'");
							Houses.sqlite.query("UPDATE signs SET price='" + ((config.getInt("sell-percentage")*Integer.parseInt(args[3]))/100) + "' WHERE class='" + args[2] + "' AND type='sell'");
						}
						if (args[1].equalsIgnoreCase("day")) {
							ResultSet rs = Houses.sqlite.query("SELECT x, y, z FROM signs WHERE class='" + args[2] + "' AND type='rent'");
							while (rs.next()) {
								int x = rs.getInt("x");
								int y = rs.getInt("y");
								int z = rs.getInt("z");
								RentSign rentSign = new RentSign((Sign) new Location(world, x, y, z).getBlock().getState());
								Houses.sqlite.query("UPDATE signs SET price='" + rentSign.calcPrice(Integer.parseInt(args[3])) + "' WHERE class='" + args[2] + "' AND x='" + x + "' AND y='" + y + "' AND z='" + z + "' AND type='rent'");
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					new DatabaseSynchronizer(plugin).syncPrices(Integer.parseInt(args[2]));
					if (args[1].equalsIgnoreCase("buy"))
						config.set("classes." + args[2] + ".price", Integer.parseInt(args[3]));
					else if (args[1].equalsIgnoreCase("day"))
						config.set("classes." + args[2] + ".per-day-cost", Integer.parseInt(args[3]));
					housesConfig.save();
					housesConfig.reload();
				} else
					helper.showUsage(args[0]);
			}
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "registersigns")
	public void registerSigns() {
		if (sender instanceof Player) {
			if (args.length == 2) {
				Player player = (Player) sender;
				if (Utils.isInt(args[1])) {
					int r = Integer.parseInt(args[1]);
					Location loc = player.getLocation();
					World w = loc.getWorld();
					int px = loc.getBlockX();
					int pz = loc.getBlockZ();
					int scans = 0;
					int signs = 0;
					int registered = 0;
					for (int x = px - r; x <= px + r; x++ )
						for (int y = 0; y <= 255; y++ )
							for (int z = pz - r; z <= pz + r; z++ ) {
								scans++;
								if (w.getBlockAt(x, y, z).getType().equals(Material.WALL_SIGN)) {
									Sign sign = (Sign) w.getBlockAt(x, y, z).getState();
									HouseSign houseSign = new HouseSign(sign);
									signs++;
									if (houseSign.isValid()) {
										if(houseSign.registerSignAt(error, new Location(w, x, y, z), player, false, false))
											registered++;
									}
									/*if (!new DatabaseQuery(houseSign.getHouseClass(), houseSign.getHouseNumber()).matchesDatabase(houseSign)) {
									houseSign.updateSignAt(new Location(w, x, y, z));
									updated++;
								}*/
								}
							}
					sender.sendMessage("§2Scanned " + dAqua + scans + "§2 blocks");
					sender.sendMessage("§2Found " + dAqua  + signs + "§2s signs");
					//sender.sendMessage(dGreen + "Updated " + dAqua + updated + dGreen + " signs");
					sender.sendMessage(gold + "Registered " + registered + gold + " signs");
				}
			} else
				helper.showUsage(args[0]);
		} else
			error.notify("You are not a player");
	}

	@HousesCommand(name = "sync")
	public void sync() {
		if (args.length == 2) {
			DatabaseSynchronizer dbSync = new DatabaseSynchronizer(plugin);
			if (args[1].equalsIgnoreCase("ranks")) {
				if (config.getBoolean("use-class-ranks")) {
					dbSync.syncRanks(DatabaseQuery.getClasses("houses"));
					sender.sendMessage(green + "Synced ranks");
				} else
					new Errors(sender).severe("Class ranks must be enabled");
			}
			else if (args[1].equalsIgnoreCase("signs")) {
				dbSync.syncPrices(DatabaseQuery.getClasses("signs"));
				sender.sendMessage(green + "Synced signs");
			}
			else if (args[1].equalsIgnoreCase("prices")) {
				if (config.getBoolean("use-class-prices")) {
					dbSync.syncPrices(DatabaseQuery.getClasses("signs"));
					sender.sendMessage(green + "Synced prices ");
				} else
					new Errors(sender).severe("Class prices must be enabled");
			}
		} else
			helper.showUsage(args[0]);
	}

	@HousesCommand(name = "changeclasses")
	public void changeClasses() {
		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("increment") || args[1].equalsIgnoreCase("decrement")) {
				World world = Utils.getWorldFromCommand(plugin, sender, args, args[0], 4);
				if (world != null) {
					if (config.getBoolean("use-class-prices")) {
						try {
							//Utils.cleanUpDatabase(w);
							int buyPrice;
							int sellPrice;
							ResultSet maxRs = Houses.sqlite.query("SELECT MAX(class) AS highestClass FROM signs");
							int highestClass = maxRs.getInt("highestClass");
							maxRs.close();
							if (args[1].equalsIgnoreCase("increment")) {
								for (int changeClass = Integer.parseInt(args[2]); changeClass <= highestClass; changeClass++) {
									buyPrice = config.getInt("classes." + (changeClass + 1) + ".price");
									sellPrice = buyPrice*config.getInt("sell-percentage")/100;
									Houses.sqlite.query("UPDATE signs SET price='" + buyPrice + "' WHERE class='" + changeClass + "' AND type='buy'");
									Houses.sqlite.query("UPDATE signs SET price='" + sellPrice + "' WHERE class='" + changeClass + "' AND type='sell'");
									ResultSet rentsRs = Houses.sqlite.query("SELECT x, y, z FROM signs WHERE class='" + changeClass + "' AND type='rent'");
									while (rentsRs.next()) {
										int x = rentsRs.getInt("x");
										int y = rentsRs.getInt("y");
										int z = rentsRs.getInt("z");
										RentSign rentSign = new RentSign((Sign) new Location(world, x, y, z).getBlock().getState());
										//sender.sendMessage("Rent sign at " + x + " " + y + " " + z);
										//sender.sendMessage("Price should become " + rentSign.calcPrice(config.getInt("classes." + (changeClass + 1) + ".per-day-cost")));
										Houses.sqlite.query("UPDATE signs SET price='" + rentSign.calcPrice(config.getInt("classes." + (changeClass + 1) + ".per-day-cost")) + "' WHERE class='" + changeClass + "' AND x='" + x + "' AND y='" + y + "' AND z='" + z + "' AND type='rent'");
									}
									rentsRs.close();
									ResultSet signsRs = Houses.sqlite.query("SELECT COUNT(*) AS signsOnClass FROM signs WHERE class='" + changeClass + "'");
									if (signsRs.getInt("signsOnClass") > 0)
										sender.sendMessage(dAqua + "Changed §2" + signsRs.getInt("signsOnClass") + dAqua + " signs on class §2" + changeClass + "");
									signsRs.close();
								}
								Houses.sqlite.query("UPDATE houses SET class = class + 1 WHERE class>='" + Integer.parseInt(args[2]) + "'");
								Houses.sqlite.query("UPDATE rentals SET class = class + 1 WHERE class>='" + Integer.parseInt(args[2]) + "'");
								Houses.sqlite.query("UPDATE signs SET class= class + 1 WHERE class>='" + Integer.parseInt(args[2]) + "'");
								ResultSet totalSignsRs = Houses.sqlite.query("SELECT COUNT(*) AS totalSignChange FROM signs WHERE class>'" + args[2] + "'");
								sender.sendMessage("§6Changed a total of §2" + totalSignsRs.getInt("totalSignChange") + "§6 signs");
								sender.sendMessage("§2Type §6/house class decrement " + args[2] + "§2 to undo");
								totalSignsRs.close();
							}
							else if (args[1].equalsIgnoreCase("decrement")) {
								for (int changeClass = Integer.parseInt(args[2]); changeClass <= highestClass; changeClass++) {
									buyPrice = config.getInt("classes." + (changeClass - 1) + ".price");
									//System.out.println("loop " + loops);
									//System.out.println("changing class " + (changeClass - 1));
									//System.out.println("Price " + buyPrice);
									sellPrice = buyPrice*config.getInt("sell-percentage")/100;
									Houses.sqlite.query("UPDATE signs SET price='" + buyPrice + "' WHERE class='" + changeClass + "' AND type='buy'");
									Houses.sqlite.query("UPDATE signs SET price='" + sellPrice + "' WHERE class='" + changeClass + "' AND type='sell'");
									ResultSet rentsRs = Houses.sqlite.query("SELECT x, y, z FROM signs WHERE class='" + changeClass + "' AND type='rent'");
									while (rentsRs.next()) {
										int x = rentsRs.getInt("x");
										int y = rentsRs.getInt("y");
										int z = rentsRs.getInt("z");
										RentSign rentSign = new RentSign((Sign) new Location(world, x, y, z).getBlock().getState());
										//sender.sendMessage("Rent sign at " + x + " " + y + " " + z);
										//sender.sendMessage("Price should become " + rentSign.calcPrice(config.getInt("classes." + (changeClass - 1) + ".per-day-cost")));
										Houses.sqlite.query("UPDATE signs SET price='" + rentSign.calcPrice(config.getInt("classes." + (changeClass - 1) + ".per-day-cost")) + "' WHERE class='" + changeClass + "' AND x='" + x + "' AND y='" + y + "' AND z='" + z + "' AND type='rent'");
									}
									rentsRs.close();
									ResultSet signsRs = Houses.sqlite.query("SELECT COUNT(*) AS signsOnClass FROM signs WHERE class='" + changeClass + "'");
									if (signsRs.getInt("signsOnClass") > 0)
										sender.sendMessage(dAqua + "Changed §2" + signsRs.getInt("signsOnClass") + dAqua + " signs on class §2" + changeClass + "");
									signsRs.close();
								}
								Houses.sqlite.query("UPDATE houses SET class = class - 1 WHERE class>='" + Integer.parseInt(args[2]) + "'");
								Houses.sqlite.query("UPDATE rentals SET class = class - 1 WHERE class>='" + Integer.parseInt(args[2]) + "'");
								Houses.sqlite.query("UPDATE signs SET class= class - 1 WHERE class>='" + Integer.parseInt(args[2]) + "'");
								ResultSet totalSignsRs = Houses.sqlite.query("SELECT COUNT(*) AS totalSignChange FROM signs WHERE class>'" + args[2] + "'");
								sender.sendMessage("§6Changed a total of §2" + totalSignsRs.getInt("totalSignChange") + "§6 signs");
								sender.sendMessage("§2Type §6/house class increment " + args[2] + "§2 to undo");
								totalSignsRs.close();
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
						new DatabaseSynchronizer(plugin).syncSigns(DatabaseQuery.getClasses("signs"));
					} else
						error.severe("Auto price must be enabled to use this command");
				}
			} else
				helper.showUsage(args[0]);
		} else
			helper.showUsage(args[0]);
	}
	@HousesCommand(name = "confirm")
	public void confirm() {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			ResultSet rs;
			try {
				rs = Houses.sqlite.query("SELECT * FROM pending WHERE player='" + player.getName() + "'");
				if (rs.next()) {
					int houseNumber = rs.getInt("number");
					int houseClass = rs.getInt("class");
					int price = rs.getInt("price");
					int days = rs.getInt("days");
					int hours = rs.getInt("hours");
					String type = rs.getString("type");
					String world = rs.getString("world");
					DatabaseQuery dbQuery = new DatabaseQuery(world, houseClass, houseNumber);
					String builder = dbQuery.getBuilder();
					if (!config.getBoolean("house-builder-profit"))
						builder = "";
					if (type.equals("buy")) {
						EconomyResponse[] withdep = {Houses.econ.withdrawPlayer(player.getName(), price), Houses.econ.depositPlayer(builder, price)};
						if(Utils.transactionSucces(plugin, withdep, player, "bought", builder)) {
							String extra = "";
							if (builder != null && !builder.isEmpty() && config.getBoolean("house-builder-profit"))
								extra = "built by §3" + builder;
							Utils.broadcastHouse(player.getName(), Integer.toString(houseClass), Integer.toString(houseNumber), "bought", extra);
							ranks.setRank(player.getName(), Integer.toString(houseClass), true);
							try {
								Houses.sqlite.query("INSERT INTO houses(player, class, number, world) VALUES('" + player.getName() + "', '" + houseClass + "', '" + houseNumber + "', '" + world + "')");
								DatabaseQuery.deletePend(player.getName());
							} catch (SQLException e) {
								e.printStackTrace();
							}
						} else
							error.severe("Transaction failed");
					} else if (type.equals("sell")) {
						EconomyResponse[] withdep = {Houses.econ.depositPlayer(player.getName(), price)};
						if(Utils.transactionSucces(plugin, withdep, player, "sold", "")) {
							dbQuery.deleteOwner(player.getName());
							dbQuery.deletePend(player.getName());
							Utils.broadcastHouse(player.getName(), Integer.toString(houseClass), Integer.toString(houseNumber), "sold", "");
							ranks.setRank(player.getName(), Integer.toString(houseClass), false);
						} else
							error.severe("Transaction failed");
					} else if (type.equals("rent")) {
						EconomyResponse[] withdep = {Houses.econ.withdrawPlayer(player.getName(), price), Houses.econ.depositPlayer(builder, price)};
						if(Utils.transactionSucces(plugin, withdep, player, "rented", builder)) {
							String extra = "";
							if (builder != null && !builder.isEmpty() && config.getBoolean("house-builder-profit"))
								extra = "built by §3" + builder;
							Utils.broadcastHouse(player.getName(), Integer.toString(houseClass), Integer.toString(houseNumber), "rented", extra);
							ranks.setRank(player.getName(), Integer.toString(houseClass), true);
							dbQuery.insertRental(player.getName(), days, hours);
							DatabaseQuery.deletePend(player.getName());
						} else
							error.severe("Transaction failed");
					} else if (type.equals("leave")) {
						String extra = "";
						if (builder != null && !builder.isEmpty() && config.getBoolean("house-builder-profit"))
							extra = "built by §3" + builder;
						dbQuery.deleteRental(player.getName());
						dbQuery.deleteOwner(player.getName());
						DatabaseQuery.deletePend(player.getName());
						Utils.broadcastHouse(player.getName(), Integer.toString(houseClass), Integer.toString(houseNumber), "abandoned", extra);
					}
				} else {
					error.notify("You have no pending transaction");
				}
				rs.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} else
			error.notify("Player excpected");
	}

	@HousesCommand(name = "cancel")
	public void cancel() {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			try {
				if (DatabaseQuery.hasPending(player.getName())) {
					Houses.sqlite.query("DELETE FROM pending WHERE player='" + player.getName() + "'");
					player.sendMessage(ChatColor.DARK_GREEN + "Your pending transaction was canceled");
				} else
					error.notify("You have no pending transactions");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@HousesCommand(name = "builder")
	public void builder() {
		World world = Utils.getWorldFromCommand(plugin, sender, args, args[0], 4);
		if (world != null) {
			DatabaseQuery dbQuery = new DatabaseQuery(world.getName(), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
			String builder = dbQuery.getBuilder();
			if (builder != null && !builder.isEmpty()) {
				sender.sendMessage(String.format("§2The builder of class §3%s §2number §3&s §2is: §3%s", args[1], args[2], builder));
			} else {
				error.notify("That house does not exist or does not have a builder");
			}
		}
	}

	@HousesCommand(name = "query")
	public void query() {
		String query = "";
		for (int i=1; i<args.length; i++) {
			query = query + " " + args[i];
		}
		try {
			ResultSet rs = Houses.sqlite.query(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}