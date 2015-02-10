package com.hektropolis.houses.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Helper {

	private CommandSender sender;

	public Helper(CommandSender sender) {
		this.sender = sender;
	}

	private String[][] help = {
			{"help", "[page]", "Shows the help menu"},
			{"info", "[class]", "Shows general info or about a chosen class"},
			{"reload", "", "Reloads the Houses config"},
			{"all", "[page]", "[world]"},
			{"me", "", "Shows your houses"},
			{"expiration", "", "Shows the time to expiration of your rentals"},
			{"player", "<name>", "Shows the houses of a player"},
			{"class", "<class number>", "Shows the houses of a class"},
			{"number", "<house number>", "Shows the houses of a number"},
			{"guests", "[class] [number]", "Shows the guests of your best house or a chosen house"},
			{"home", "", "Teleports you home"},
			{"tp", "<class> <number>", "Teleports to the chosen house"},
			{"backup", "", "Makes a backup of the current database"},
			{"add owner", "<player> <class> <number>", "Adds an owner of a house"},
			{"add rental", "<player> <class> <number> <days> <hours>", "Adds a rental of a house"},
			{"add guest", "<player> <class> <number>", "Adds a guest to a house"},
			{"remove owner", "<player> <class> <number> [world]", "Removes an owner of a house"},
			{"remove rental", "<player> <class> <number> [world]", "Removes a rental of a house"},
			{"remove guest", "<player> <class> <number>", "Removes a guest of a house"},
			{"ranks", "[class]", "Shows ranks for all classes or chosen class"},
			{"prices", "[class]", "Shows prices for all classes or chosen class"},
			{"setprice", "<buy|day> <class> <price> [world]", "Sets the buy/per-day price of a class"},
			{"confirm", "", "Confirms your pending transaction"},
			{"cancel", "", "Cancels you pending transaction"},
			{"registersigns", "<radius>", "Registers all signs within the radius and top to bottom"},
			{"sync", "<signs|prices|ranks> [world]", "Syncs signs/prices/ranks with what's in the database"},
			{"changeclasses", "<increment|decrement> <class> [world]", "Increments or decrements the class on each signs that is greater or equal to the class to make room for a new class"}
	};

	public void showCommands(int page) {
		int totalPages = (int) Math.ceil((double) help.length / 7);
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8--- " + "&6Houses &2Help &8-- &6Page &2" + page + "&6/&2" + totalPages + "&8 ---"));
		for (int i = page * 7 - 7; i < page * 7 && i < help.length; i++) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/house &2" + help[i][0] + " &3"
					+ help[i][1]));
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Type &2/houses help "
				+ (page + 1) + " " + "&6to read the next page"));
	}

	public void showUsage(String cmd) {
		for (int i = 0; i < help.length; i++) {
			if (cmd.equalsIgnoreCase(help[i][0])) {
				sender.sendMessage(ChatColor.GOLD + "Description: " + ChatColor.DARK_GREEN + help[i][2]);
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/house &2" + help[i][0] + " &3" + help[i][1]));
				//sender.sendMessage(gold+"Permision: " + ChatColor.DARK_AQUA + help[i]);
				break;
			}
		}
	}
}
