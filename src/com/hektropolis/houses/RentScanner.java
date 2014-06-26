package com.hektropolis.houses;

import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import com.hektropolis.houses.database.DatabaseQuery;

public class RentScanner extends BukkitRunnable {

	private Houses plugin;

	public RentScanner(Houses plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		try {
			if (DatabaseQuery.rsHasOutput(Houses.sqlite.query("SELECT COUNT(*) FROM rentals"))) {
				for (int houseClass : DatabaseQuery.getClasses("rentals")) {
					for (int houseNumber : DatabaseQuery.getNumbers("rentals", houseClass)) {
						for (String world : DatabaseQuery.getWorldNames("rentals")) {
							DatabaseQuery dbQuery2 = new DatabaseQuery(world, houseClass, houseNumber);
							if (!dbQuery2.rentIsValid(dbQuery2.getRentalOwner())) {
								plugin.getServer().broadcastMessage("§3" + dbQuery2.getRentalOwner() + "§2's house at class §3" +houseClass + "§2 number §3" + houseNumber + "§2 has expired");
								new Ranks(plugin).setRank(dbQuery2.getRentalOwner(), Integer.toString(houseClass), false);
								dbQuery2.deleteRental(dbQuery2.getRentalOwner());
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
