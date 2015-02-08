package com.hektropolis.houses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;

import com.hektropolis.houses.commands.Helper;
import com.hektropolis.houses.signs.HouseSign;

public class Utils {

	private static ChatColor dGreen = ChatColor.DARK_GREEN;
	private static ChatColor dAqua = ChatColor.DARK_AQUA;

	public static Block getDoorFromSign(Sign sign) {
		BlockFace signFace = ((org.bukkit.material.Sign) sign.getData()).getAttachedFace();
		Block block = sign.getBlock().getRelative(signFace);
		int ix = block.getX();
		int iy = block.getY();
		int iz = block.getZ();
		if (signFace.equals(BlockFace.NORTH) || signFace.equals(BlockFace.SOUTH)) {
			for (int x = ix - 1; x <= ix + 1; x++ ) {
				for (int y = iy - 2; y <= iy; y++ ) {
					block = block.getWorld().getBlockAt(x, y, iz);
					if (block.getType().equals(Material.IRON_DOOR_BLOCK)) {
						block.getRelative(BlockFace.DOWN);
						if(!block.getType().equals(Material.IRON_DOOR_BLOCK)) {
							block.getRelative(BlockFace.UP);
						}
						return block;
					}
				}
			}
		}
		else if (signFace.equals(BlockFace.EAST) || signFace.equals(BlockFace.WEST)) {
			for (int y = iy - 2; y <= iy; y++ ) {
				for (int z = iz - 1; z <= iz + 1; z++ ) {
					block = block.getWorld().getBlockAt(ix, y, z);
					if (block.getType().equals(Material.IRON_DOOR_BLOCK)) {
						block.getRelative(BlockFace.DOWN);
						if(!block.getType().equals(Material.IRON_DOOR_BLOCK)) {
							block.getRelative(BlockFace.UP);
						}
						return block;
					}
				}
			}
		}
		return null;
	}

	public static HouseSign[] getSignsFromDoor(Block doorBlock) {
		if (doorBlock == null)
			return null;
		List<HouseSign> signList = new ArrayList<HouseSign>();
		Door door = (Door) doorBlock.getState().getData();
		if(!door.isTopHalf())
			doorBlock = doorBlock.getRelative(BlockFace.UP);
		int ix = doorBlock.getX();
		int iy = doorBlock.getY();
		int iz = doorBlock.getZ();
		int r = 1;
		for (int x = ix - r; x <= ix + r; x++ ) {
			for (int y = iy - r; y <= iy + r; y++ ) {
				for (int z = iz - r; z <= iz + r; z++ ) {
					Block block = doorBlock.getWorld().getBlockAt(x, y, z);
					if (block.getType().equals(Material.WALL_SIGN)) {
						HouseSign sign = new HouseSign((Sign) block.getState());
						if (sign.isValid())
							signList.add(sign.getType());
					}
				}
			}
		}
		return signList.toArray(new HouseSign[signList.size()]);
	}
	public static boolean transactionSucces(Houses plugin, EconomyResponse[] withdep, Player player, String action, String builder) {
		int balance = (int) withdep[0].balance;
		int amount = (int) withdep[0].amount;
		String from = "";
		if (!builder.isEmpty())
			from = "from " + dAqua + builder + dGreen + " ";
		if (withdep.length == 2)
			if (!withdep[1].transactionSuccess())
				return false;
		if(withdep[0].transactionSuccess()) {
			player.sendMessage(dGreen  +  "You " + action + " this house " + from + "for $"  +  amount);
			player.sendMessage(ChatColor.GOLD  +  "Your account balance is "  +  dGreen  +  "$"  +  balance);
			return true;
		} else {
			return false;
		}
	}

	public static void broadcastHouse(String playerName, String classNum, String numberNum, String action, String extraInfo) {
		Bukkit.getServer().broadcastMessage(dGreen + "Player " + dAqua + playerName + " " + dGreen + action + " a house at class: " + dAqua + classNum + dGreen + " number: " + dAqua + numberNum + dGreen + " " + extraInfo);
	}

	public static boolean isInt(String i) {
		try {
			Integer.parseInt(i);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	public static void printQuery(Houses plugin, ResultSet rs, CommandSender sender,int page, boolean displayWorlds) {
		try {
			if(rs!=null) {
				if (rs.next()) {
					do {
						String world = displayWorlds ? " world: " + dGreen + plugin.getConfig().getString("worlds." + rs.getString("world") + ".display-name") : "";
						String message = dGreen + rs.getString("player")  +
								dAqua + ": class: " + dGreen + rs.getString("class") +
								dAqua + " number: " + dGreen + rs.getString("number") +
								dAqua + world +
								ChatColor.RED + " " + rs.getString("tableName");
						if (page > 0) {
							if(rs.getRow() <= page*8 && rs.getRow() > page*8-8)
								sender.sendMessage(message);
						}
						if (page == 0)
							sender.sendMessage(message);
					} while (rs.next());
				} else
					new Errors(sender).notify("No house matches your request");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public static void cleanUpDatabase(World world) {
		if (Houses.sqlite != null) {
			boolean invalid;
			boolean hadInvalid = false;
			do {
				invalid = false;
				try {
					ResultSet rs = Houses.sqlite.query("SELECT * FROM signs");
					while(rs.next()) {
						int x = rs.getInt("x");
						int y = rs.getInt("y");
						int z = rs.getInt("z");
						Block sign = world.getBlockAt(x, y, z);
						String signAt = "Sign at x=" + x + " y=" + y + " z=" + z;
						String delete = "DELETE FROM signs WHERE x='" + x + "' AND y='" + y + "' AND z='" + z + "'";
						if (sign.getType().equals(Material.WALL_SIGN)) {
							HouseSign houseSign = new HouseSign((Sign) sign.getState());
							if (houseSign.getHouseClass() != rs.getInt("class") ||
									houseSign.getHouseNumber() != rs.getInt("number") ||
									houseSign.getPrice() != rs.getInt("price")){
								Houses.log.warning(Houses.prefix + "Data from  " + signAt + " does not match database data ");
								Houses.log.warning(Houses.prefix + "Re-register signs to fix this");
								hadInvalid = true;
							}
							if (rs.getString("world") == null) {
								rs.close();
								Houses.log.warning(Houses.prefix + signAt + " has no world data");
								Houses.log.warning(Houses.prefix + "Setting world to: " + world.getName());
								Houses.sqlite.query("UPDATE signs SET world='" + world.getName() + "' WHERE x='" + x + "' AND y='" + y + "' AND z='" + z + "'");
								invalid = true;
								hadInvalid = true;
							}
							if (!houseSign.isValid()) {
								rs.close();
								Houses.log.warning(Houses.prefix + signAt + " was deleted because: ");
								Houses.log.warning(Houses.prefix + "Sign is not valid");
								Houses.sqlite.query(delete);
								invalid = true;
								hadInvalid = true;
							}
						} else {
							rs.close();
							Houses.log.warning(Houses.prefix + signAt + " was deleted because: ");
							Houses.log.warning(Houses.prefix + "Block is not a sign");
							Houses.sqlite.query(delete);
							invalid = true;
							hadInvalid = true;
						}
					}
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} while (invalid);
			if(hadInvalid)
				Houses.log.info(Houses.prefix + "Finished cleaning up database");
			else {
				Houses.log.info(Houses.prefix + "No database errors found");
			}
		}
	}
	public static boolean isClass(CommandSender sender, String classNum) {
		if(!classNum.equalsIgnoreCase("admin") && !classNum.equalsIgnoreCase("moderator") && !classNum.equalsIgnoreCase("owner")) {
			if(!isInt(classNum)) {
				new Errors(sender).severe("Class must be a number or owner/admin/moderator");
				return false;
			}
		}
		return true;
	}

	public static boolean isNumber(CommandSender sender, String houseNum) {
		if(!isInt(houseNum)) {
			new Errors(sender).severe("House number must be a number");
			return false;
		}
		return true;
	}

	public static String getTimeLeft(int expires) {
		int days = 0;
		int hours = 0;
		int minutes = 0;
		long secsLeft = expires - System.currentTimeMillis() / 1000;
		for ( ; secsLeft >= 86400; secsLeft -= 86400)
			days++;
		for ( ; secsLeft >= 3600; secsLeft -= 3600)
			hours++;
		for ( ; secsLeft >= 60; secsLeft -= 60)
			minutes++;
		String dayStr;
		String hourStr;
		String minuteStr;
		String secondStr;
		if (days > 0) {
			if (days == 1)
				dayStr = days + " day ";
			else
				dayStr = days + " days ";
		} else
			dayStr = "";
		if (hours > 0) {
			if (hours == 1)
				hourStr = hours + " hour ";
			else
				hourStr = hours + " hours ";
		} else
			hourStr = "";
		if (minutes > 0) {
			if (minutes == 1)
				minuteStr = minutes + " minute and ";
			else
				minuteStr = minutes + " minutes and ";
		} else
			minuteStr = "";
		secondStr = secsLeft + " seconds";
		return dayStr + hourStr + minuteStr + secondStr;
	}
	public static World getWorldFromCommand(Houses plugin, CommandSender sender, String[] args, String cmdName, int argsLength) {
		Errors error = new Errors(sender);
		Helper help = new Helper(sender);
		World world = null;
		if (args.length == (argsLength -1)) {
			if (sender instanceof Player) {
				Player senderPlayer = (Player) sender;
				world = senderPlayer.getWorld();
			} else {
				error.notify("You must choose a world when sending from console");
				help.showUsage(cmdName);
			}
		}
		if (args.length == argsLength) {
			world = plugin.getServer().getWorld(args[argsLength - 1]);
			if (world == null) {
				error.severe("World " + args[argsLength - 1] + " does not exist");
			}
		}
		return world;
	}

	public String getClassStringFromId(int id) {
		if (id == 1)
			return "Owner";
		else if (id == 2)
			return "Admin";
		else if (id == 3)
			return "Moderator";
		else
			return "unknown";
	}

}
