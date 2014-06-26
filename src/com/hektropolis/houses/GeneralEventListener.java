package com.hektropolis.houses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.hektropolis.houses.signs.HouseSign;
import com.hektropolis.houses.signs.RentSign;

public class GeneralEventListener implements Listener {

	private Houses plugin;

	public GeneralEventListener(Houses plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayJoin(PlayerJoinEvent e) {
		final Player player = e.getPlayer();
		BukkitTask task = new BukkitRunnable() {
			@Override
			public void run() {
				try {
					ResultSet rs = Houses.sqlite.query("SELECT * FROM rentals WHERE player='" + player.getName() + "'");
					while (rs.next()) {
						player.sendMessage(Houses.prefix + "§6Your rental at class §2" + rs.getInt("class") + "§6 number §2" + rs.getInt("number") + 
								"§6 expires in §2" + Utils.getTimeLeft(rs.getInt("expires")));
					}
					rs.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}.runTaskLater(plugin, 2);
	}
}
