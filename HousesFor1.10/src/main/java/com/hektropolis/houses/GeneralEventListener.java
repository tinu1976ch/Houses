package com.hektropolis.houses;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.hektropolis.houses.database.DatabaseQuery;

public class GeneralEventListener implements Listener {

	private Houses plugin;

	public GeneralEventListener(Houses plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onJoinCheck(PlayerJoinEvent e) {
			final Player p = e.getPlayer();
		for (int houseClass : DatabaseQuery.getClasses("rentals")) {
		for (int houseNumber : DatabaseQuery.getNumbers("rentals", houseClass)) {
			DatabaseQuery DBQuery = new DatabaseQuery(p.getWorld().getName(), houseClass, houseNumber);
		if(DBQuery.playerHasRental(p.getName())) {
			int delay = (int) plugin.getConfig().getDouble("expired-check-timer") * 20;
			Bukkit.broadcastMessage(Integer.toString(delay));
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				try {
				Bukkit.broadcastMessage("test");
				ResultSet rs = Houses.sqlite.query("SELECT * FROM rentals WHERE player='" + p.getName() + "'");
				while (rs.next()) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', Houses.prefix + "&6Your rental at class &2" + rs.getInt("class") + "&6 number &2" + rs.getInt("number") + 
							"&6 expires in &2" + Utils.getTimeLeft(rs.getInt("expires"))));
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
							}
						}
					}, delay);
				}
			}
		}
	}
}
