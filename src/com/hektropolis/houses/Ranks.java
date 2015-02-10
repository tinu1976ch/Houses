package com.hektropolis.houses;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

public class Ranks {

	private Houses plugin;

	public Ranks(Houses plugin) {
	    this.plugin = plugin;
	}

	public void setRank(String player, String buyClass, boolean buy){
		if(config.getConfig().getBoolean("use-class-ranks")){
			try {
				ResultSet rs = Houses.sqlite.query("SELECT * FROM houses WHERE player='" + player + "' ORDER BY class");
				plugin.reloadConfig();
				String world = null;
				String[] groups = Houses.permission.getPlayerGroups(world, player);
				String rank = plugin.getConfig().getString("classes." + buyClass + ".rank");
				if(rank != null){
					if(buy){
						if(rs.next()) {
							int rsClass = rs.getInt("class");
							if (!buyClass.equalsIgnoreCase("admin") && !buyClass.equalsIgnoreCase("moderator")) {
								if(rsClass > Integer.parseInt(buyClass)){
									removeRanks(world, groups, player);
									addRank(world, rank, player);
								}
							}
						} else {
							removeRanks(world, groups, player);
							addRank(world, rank, player);
						}
					}
					else if(!buy){
						if(rs.next()){
							String rankFromDB = plugin.getConfig().getString("classes." + rs.getString("class") + ".rank");
							if(!rs.getString("class").equalsIgnoreCase("admin") && !rs.getString("class").equalsIgnoreCase("moderator")) {
								removeRanks(world, groups, player);
								addRank(world, rankFromDB, player);
							}
						} else{
							removeRanks(world, groups, player);
							Houses.permission.playerAddGroup(world, player, config.getConfig().getString("homeless"));
							Bukkit.getServer().broadcastMessage(ChatColor.RED + "Player " + player + " is homeless!");
						}
					}
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	private void removeRanks(String world, String[] groups, String player){
		for(String gn: groups){
			Houses.permission.playerRemoveGroup(world, player, gn);
		}
	}
	private void addRank(String world, String rankPath, String player){
		Houses.permission.playerAddGroup(world, player, rankPath);
		OfflinePlayer playerName = Bukkit.getServer().getOfflinePlayer(player);
		Bukkit.getServer().broadcastMessage(ChatColor.DARK_GREEN + "Player "+ChatColor.DARK_AQUA+playerName.getName()+ChatColor.DARK_GREEN+" has become a(n) " + ChatColor.DARK_AQUA+rankPath.replace("_", " "));
	}

}
