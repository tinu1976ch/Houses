package com.hektropolis.houses;

import org.bukkit.command.CommandSender;

public class Permissions {

	private static String[] noPermCommands = {"confirm", "cancel", "help"};
	public static boolean hasPerm(CommandSender sender, String[] args) {
		return checkPerm(sender, getPerm(args));
	}
	public static boolean hasPerm(CommandSender sender, String perm) {
		return checkPerm(sender, perm);
	}
	public static String getPerm(String[] args) {
		String perm = "";
		if (args[0].equalsIgnoreCase("all") ||
				args[0].equalsIgnoreCase("me") ||
				args[0].equalsIgnoreCase("class") ||
				args[0].equalsIgnoreCase("number") ||
				args[0].equalsIgnoreCase("player"))
			perm = "show.house";
		else if (args[0].equalsIgnoreCase("expiration"))
			perm = "show.expiration";
		else if (args[0].equalsIgnoreCase("prices"))
			perm = "show.price";
		else if (args[0].equalsIgnoreCase("ranks"))
			perm = "show.rank";
		else if (args[0].equalsIgnoreCase("guests"))
			perm = "show.guest";
		else if ((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) && args.length >= 2) {
			if (args[1].equalsIgnoreCase("owner") ||
					args[1].equalsIgnoreCase("rental") ||
					args[1].equalsIgnoreCase("guest"))
				perm = args[0] + "." + args[1];
		} else if (args[0].equalsIgnoreCase("sync") && args.length >= 2) {
			if (args[1].equalsIgnoreCase("signs") ||
					args[1].equalsIgnoreCase("ranks") ||
					args[1].equalsIgnoreCase("prices"))
				perm = args[0] + "." + args[1];
		} else if (args[0].equalsIgnoreCase("changeclasses") && args.length >= 2) {
			if (args[1].equalsIgnoreCase("increment") ||
					args[1].equalsIgnoreCase("decrement"))
				perm = args[0] + "." + args[1];
		} else {
			perm = args[0];
			for (String cmd : noPermCommands) {
				if (cmd.equalsIgnoreCase(args[0]))
					perm = "";
			}
		}
		//System.out.println(perm.toLowerCase());
		return perm.toLowerCase();
	}
	private static boolean checkPerm(CommandSender sender, String perm) {
		if (Houses.permission != null) {
			if (perm.isEmpty())
				return true;
			else if(Houses.permission.has(sender, "houses." + perm) ||
					Houses.permission.has(sender, "houses." + perm.substring(0, perm.indexOf(".")+1) + "*") ||
					Houses.permission.has(sender, "houses." + perm.substring(0, perm.lastIndexOf(".")+1) + "*") ||
					Houses.permission.has(sender, "houses.*") ||
					sender.isOp()) {
				//System.out.println("Has permission");
				return true;
			} else {
				//System.out.println("Does NOT have permission");
				return false;
			}
		} else
			return false;
	}
}
