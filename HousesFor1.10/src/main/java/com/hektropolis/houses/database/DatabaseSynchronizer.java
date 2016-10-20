package com.hektropolis.houses.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;

import com.hektropolis.houses.Houses;
import com.hektropolis.houses.config.Config;
import com.hektropolis.houses.signs.BuySellSign;
import com.hektropolis.houses.signs.HouseSign;
import com.hektropolis.houses.signs.InfoSign;
import com.hektropolis.houses.signs.RentSign;
import com.hektropolis.houses.signs.SignType;

public class DatabaseSynchronizer {

	private Houses plugin;

	public DatabaseSynchronizer(Houses plugin) {
		this.plugin = plugin;
	}

	public void syncRanks(int[] classes) {
		for (int houseClass : classes) {
			List<String> players = new ArrayList<String>();
			try {
				ResultSet playersRs = Houses.sqlite.query("SELECT player FROM houses WHERE class='" + houseClass + "'");
				while (playersRs.next())
					players.add(playersRs.getString("player"));
				playersRs.close();
				for (String name : players) {
					String player = plugin.getServer().getOfflinePlayer(name).getName();
					ResultSet classesRs = Houses.sqlite.query("SELECT class FROM houses WHERE player='" + player + "' ORDER BY class");
					classesRs.next();
					int bestClass = classesRs.getInt("class");
					classesRs.close();
					String[] groups = Houses.permission.getPlayerGroups((World) null, player);
					String rank = plugin.getConfig().getString("classes." + bestClass + ".rank");
					if (rank != null && rank != "") {
						for (String group : groups) {
							if (!group.equalsIgnoreCase(rank)) {
								Houses.permission.playerRemoveGroup((World) null, player, group);
								groups = Houses.permission.getPlayerGroups((World) null, player);
							}
						}
						if (groups.length == 0)
							Houses.permission.playerAddGroup((World) null, player, rank);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	public void syncRanks(int classNumber) {
		int[] classes = new int[1];
		classes[0] = classNumber;
		syncRanks(classes);
	}
	public void syncPrices(int classNumber) {
		sync(false, true, classNumber);
	}
	public void syncPrices(int[] classes) {
		for (int houseClass : classes) {
			sync(false, true, houseClass);
		}
	}
	public void syncSigns(int[] classes) {
		for (int houseClass : classes) {
			sync(true, true, houseClass);
		}
	}
	public void syncSigns(int classNumber) {
		int[] classes = {classNumber};
		syncSigns(classes);
	}
	private void sync(boolean syncClasses, boolean syncPrices, int houseClass) {
		ResultSet rs;
		try {
			rs = Houses.sqlite.query("SELECT * FROM signs WHERE class='" + houseClass + "'");
			World w;
			//System.out.println("Class " + houseClass);
			while(rs.next()) {
				//System.out.println("Getting result");
				w = plugin.getServer().getWorld(rs.getString("world"));
				if (w.getBlockAt(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")).getType().equals(Material.WALL_SIGN)) {
					Sign sign = (Sign) w.getBlockAt(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")).getState();
					HouseSign houseSign = new HouseSign(sign);
					if (rs.getString("type").equalsIgnoreCase("buy") || rs.getString("type").equalsIgnoreCase("sell")) {
						//System.out.println("It's a buy or sell");
						BuySellSign bsSign = new BuySellSign(sign);
						if (syncClasses)
							bsSign.setClass(houseClass);
						if (syncPrices)
							bsSign.setPrice(rs.getInt("price"));
						bsSign.setType(SignType.valueOf(rs.getString("type").toUpperCase()));
						bsSign.setNumber(rs.getInt("number"));
					}
					if (rs.getString("type").equalsIgnoreCase("rent")) {
						if (syncPrices)
							new RentSign(sign).setPrice(rs.getInt("price"));
					}
					if (rs.getString("type").equalsIgnoreCase("info")) {
						InfoSign infoSign = new InfoSign(sign);
						if (syncClasses)
							infoSign.setClass(houseClass);
						infoSign.setNumber(rs.getInt("number"));
					}
					houseSign.setType(SignType.valueOf(rs.getString("type").toUpperCase()));
					sign.update();
				}
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
