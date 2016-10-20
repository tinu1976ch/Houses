package com.hektropolis.houses;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Errors {
	
	private CommandSender sender;
	
	private Player player;
	
	public Errors(CommandSender sender) {
		this.sender = sender;
	}
	
	public Errors(Player player) {
		this.player = player;
	}
	
	public void notify(String message) {
		if (sender != null) {
		sender.sendMessage(ChatColor.RED + message);
		}
		if (player != null) {
			player.sendMessage(ChatColor.RED + message);
		}
	}
	
	public void warning(String message) {
		if (sender != null) {
		sender.sendMessage(ChatColor.DARK_RED + "Warning: " + ChatColor.RED + message);
		}
		if (player != null) {
			player.sendMessage(ChatColor.DARK_RED + "Warning: " + ChatColor.RED + message);
		}
	}
	
	public void severe(String message) {
		if (sender != null) {
		sender.sendMessage(ChatColor.DARK_RED + "Error: " + message);
		}
		if (player != null) {
			player.sendMessage(ChatColor.DARK_RED + "Error: " + message);
		}
	}
}
