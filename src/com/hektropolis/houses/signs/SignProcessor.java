package com.hektropolis.houses.signs;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.event.block.SignChangeEvent;

import com.hektropolis.houses.Errors;
import com.hektropolis.houses.Houses;
import com.hektropolis.houses.Utils;
import com.hektropolis.houses.config.Config;
import com.hektropolis.houses.database.DatabaseQuery;

public class SignProcessor {

	private Houses plugin;

	private SignType type;
	private String[] line;
	private SignChangeEvent e;
	private Errors error;

	public SignProcessor(Houses plugin, SignChangeEvent event, Errors error) {
		this.plugin = plugin;
		this.line = event.getLines();
		this.error = error;
		this.e = event;
		if (line[0].substring(1, 4).equalsIgnoreCase("buy"))
			type = SignType.BUY;
		else if (line[0].substring(1, 5).equalsIgnoreCase("sell"))
			type = SignType.SELL;
		else if (line[0].substring(1, 5).equalsIgnoreCase("rent"))
			type = SignType.RENT;
		else if (line[0].substring(7, 11).equalsIgnoreCase("info"))
			type = SignType.INFO;
		else if (line[0].substring(1, 6).equalsIgnoreCase("leave"))
			type = SignType.LEAVE;
		else if (line[0].substring(1, 6).equalsIgnoreCase("staff"))
			type = SignType.STAFF;
		else
			type = null;
	}

	public boolean parseSign() {
		if (Utils.getDoorFromSign((Sign) this.e.getBlock().getState()) == null) {
			error.notify("Invalid placement of sign. No door found");
			setRed();
			return false;
		}
		if (type == SignType.SELL || type == SignType.BUY) {
			if (!Utils.isInt(line[1])) {
				error.notify("Error at second line: Class must be a number");
				setRed();
				return false;
			}
			if (!Utils.isInt(line[2])) {
				error.notify("Error at third line: House number must be a number");
				setRed();
				return false;
			}
			if (!line[3].startsWith("$") && !line[3].isEmpty()) {
				error.notify("Error at last line: Price must start with '$'");
				setRed();
				return false;
			}
			else if(line[3].startsWith("$")) {
				if (!Utils.isInt(line[3].substring(1))) {
					error.notify("Error at last line: Price must be a number");
					setRed();
					return false;
				}
			}
			HouseSign[] signs = Utils.getSignsFromDoor(Utils.getDoorFromSign((Sign) this.e.getBlock().getState()));
			boolean buySign = false;
			boolean sellSign = false;
			if (signs != null) {
				if (signs.length > 0) {
					for (HouseSign searchedSign : signs) {
						if (searchedSign instanceof BuySellSign) {
							if (searchedSign.isBuy())
								buySign = true;
							if (searchedSign.isSell())
								sellSign = true;
						}
					}
				}
			}
			DatabaseQuery dbQuery = new DatabaseQuery(e.getBlock().getWorld().getName(), Integer.parseInt(line[1]), Integer.parseInt(line[2]));
			String builder = dbQuery.getBuilder();
			if (type == SignType.BUY && sellSign) {
				if (builder != null && !builder.isEmpty()) {
					error.notify("You can not have both buy and sell signs on a house with an earning builder.");
					error.notify("Remove the sell sign, then re-create this buy sign");
					setRed();
					return false;
				}
			}
			if (type == SignType.SELL && buySign) {
				if (builder != null && !builder.isEmpty()) {
					error.notify("You can not have both buy and sell signs on a house with an earning builder.");
					error.notify("Remove the buy sign, then re-create this sign. Or consider creating a leave sign instead of this");
					setRed();
					return false;
				}
			}
		}
		else if (type == SignType.RENT) {
			RentSign rentSign = new RentSign((Sign) e.getBlock().getState());
			if (rentSign.getHelperSign() != null) {
				if (!Utils.isInt(line[1])) {
					error.notify("Error at second line: Day must be a number");
					setRed();
					return false;
				}
				if (!Utils.isInt(line[2])) {
					error.notify("Error at second line: Hours must be a number");
					setRed();
					return false;

				}
				if (!line[3].startsWith("$") && !line[3].isEmpty()) {
					error.notify("Error at last line: Price must start with '$'");
					setRed();
					return false;
				}
				else if(line[3].startsWith("$")) {
					if (!Utils.isInt(line[3].substring(1))) {
						error.notify("Error at last line: Price must be a number");
						setRed();
						return false;
					}
				}
			} else {
				setRed();
				error.notify("You must put up a buy/sell/leave sign BEFORE creating a rent sign, else the rent sign won't know what class and number it belongs to");
				return false;
			}
		}
		else if (type == SignType.INFO || type == SignType.LEAVE) {
			if (type == SignType.INFO) {
				error.notify("Info sign is deprecated. Converting to leave sign");
				error.notify("Other than providing info, the leave sign allows the player to abandon the rented/bought house");
			}
			if (!Utils.isInt(line[1])) {
				error.notify("Error at second line: Class must be a number");
				setRed();
				return false;
			}
			if (!Utils.isInt(line[2])) {
				error.notify("Error at third line: House number must be a number");
				setRed();
				return false;
			}
		}
		else if (type == SignType.STAFF) {
			if (!line[1].equalsIgnoreCase("admin") && !line[1].equalsIgnoreCase("mod") && !line[1].equalsIgnoreCase("owner")) {
				error.notify("Error at second line: Class must be mod, admin or owner");
				setRed();
				return false;
			}
			if (!Utils.isInt(line[2])) {
				error.notify("Error at third line: House number must be a number");
				setRed();
				return false;
			}
			if (!plugin.getServer().getPlayer(line[3]).hasPlayedBefore()) {
				error.notify("Error at last line: " + line[3] + " has never played on this server");
				setRed();
				return false;
			}
		}
		return true;
	}

	public void setSign() {
		if (type == SignType.SELL || type == SignType.BUY) {
			if (type == SignType.BUY)
				e.setLine(0, SignType.BUY.getHeader());
			else
				e.setLine(0, SignType.SELL.getHeader());
			int price = getPrice();
			e.setLine(1, ChatColor.ITALIC  +  "Class "  +  ChatColor.BLACK  +  e.getLine(1));
			e.setLine(2, ChatColor.ITALIC  +  "Number "  +  ChatColor.BLACK  +  e.getLine(2));
			e.setLine(3, ChatColor.DARK_GREEN  +  "$"  +  price);
		}
		if (type == SignType.RENT) {
			//int price = getPrice();
			int days = Integer.parseInt(line[1]);
			int hours = Integer.parseInt(line[2]);
			while (hours > 24) {
				days++;
				hours -= 24;
			}
			int price = getPrice();
			String dayStr = days == 1 ? ChatColor.ITALIC + " Day" : ChatColor.ITALIC + " Days";
			String hourStr = hours == 1 ? "Â§0 Hour" : "Â§o Hours";
			e.setLine(0, SignType.RENT.getHeader());
			e.setLine(1, days + dayStr);
			e.setLine(2, hours + hourStr);
			e.setLine(3, ChatColor.DARK_GREEN + "$" + price);
		}
		else if (type == SignType.LEAVE || type == SignType.INFO) {
			e.setLine(0, SignType.LEAVE.getHeader());
			e.setLine(1, ChatColor.ITALIC + "Class " + ChatColor.BLACK + e.getLine(1));
			e.setLine(2, ChatColor.ITALIC + "Number " + ChatColor.BLACK + e.getLine(2));
			e.setLine(3, "");
		}
		else if (type == SignType.STAFF) {
			e.setLine(0, SignType.STAFF.getHeader());
			e.setLine(1, ChatColor.ITALIC + "Class " + ChatColor.BLACK + e.getLine(1).substring(0, 1).toUpperCase() + e.getLine(1).substring(1));
			e.setLine(2, ChatColor.ITALIC + "Number " + ChatColor.BLACK + e.getLine(2));
			e.setLine(3, ChatColor.DARK_GREEN + plugin.getServer().getPlayer(e.getLine(3)).getName());
		}
	}

	private int getPrice() {
		int price = 0;
		if (line[3].isEmpty()) {
			if (plugin.getConfig().getBoolean("use-class-prices")) {
				if (type == SignType.BUY || type == SignType.SELL) {
					if(plugin.getConfig().isInt("classes." + line[1] + ".price")) {
						if (type == SignType.BUY)
							price = plugin.getConfig().getInt("classes." + line[1] + ".price");
						else if (type == SignType.SELL)
							price = plugin.getConfig().getInt("classes." + line[1] + ".price")*plugin.getConfig().getInt("sell-percentage")/100;
					} else {
						error.notify("Price for class " + line[1] + " is " + plugin.getConfig().get("classes." + line[1] + ".price"));
						error.warning("Pre-defined price is not a number");
					}
				}
				int houseClass = 0;
				if (type == SignType.RENT) {
					RentSign rentSign = new RentSign((Sign) e.getBlock().getState());
					houseClass = rentSign.getHelperSign().getHouseClass();
					if (plugin.getConfig().isInt("classes." + houseClass + ".per-day-cost")) {
						int dayCost = plugin.getConfig().getInt("classes." + rentSign.getHelperSign().getHouseClass() + ".per-day-cost");
						price = Integer.parseInt(line[1]) * dayCost + (Integer.parseInt(line[2]) * dayCost) / 24;
					} else {
						error.warning("Per day cost for class " + houseClass + " is " + plugin.getConfig().get("classes." + houseClass + ".per-day-cost"));
						error.severe("Pre-defined price is not a number");
					}
				}
			} else
				error.warning("Auto-fill price is not enabled");
		} else
			price = Integer.parseInt(line[3].substring(1));
		return price;
	}

	private void setRed() {
		e.setLine(0, ChatColor.DARK_RED  +  e.getLine(0));
	}
}
