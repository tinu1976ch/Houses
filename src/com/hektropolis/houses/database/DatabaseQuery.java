package com.hektropolis.houses.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

import com.hektropolis.houses.Houses;

public class DatabaseQuery {
	protected int houseClass;
	protected int houseNumber;
	protected String world;

	public DatabaseQuery() {

	}

	public DatabaseQuery(String world, int houseClass, int houseNumber) {
		this.world = world;
		this.houseClass = houseClass;
		this.houseNumber = houseNumber;
	}

	public boolean anyoneHasRental() {
		ResultSet rs = null;
		try {
			rs = Houses.sqlite.query("SELECT * FROM rentals WHERE class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rsHasOutput(rs);
	}
	public String getRentalOwner() {
		try {
			ResultSet rs = Houses.sqlite.query("SELECT * FROM rentals WHERE class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
			if(rs.next()) {
				String owner = rs.getString("player");
				rs.close();
				return owner;
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	public int getHouseId() {
		int id = 0;
		try {
			ResultSet idOwnedRs = Houses.sqlite.query("SELECT id FROM houses WHERE class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
			if(idOwnedRs.next())
				id = idOwnedRs.getInt("id");
			idOwnedRs.close();
			ResultSet idRentedRs = Houses.sqlite.query("SELECT id FROM houses WHERE class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
			if(idRentedRs.next())
				id = idRentedRs.getInt("id");
			idRentedRs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}
	public boolean rentIsValid(String player) {
		try {
			ResultSet rs = Houses.sqlite.query("SELECT * FROM rentals WHERE player='" + player +  "' AND class='" + houseClass + "' AND number='" + houseNumber + "' AND world='" + world + "'");
			int expires = rs.getInt("expires");
			long unixTime = System.currentTimeMillis() / 1000L;
			if (rs.next()) {
				rs.close();
				if(unixTime >= expires) return false;
				else if(unixTime <= expires) return true;
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	public void deleteRental(String player) {
		try {
			Houses.sqlite.query("DELETE FROM guests WHERE house_id=" + getHouseId());
			Houses.sqlite.query("DELETE FROM rentals WHERE player='" + player + "' AND class='" + houseClass + "' AND number='" + houseNumber + "' AND world='" + world + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void insertRental(String player, int days, int hours) {
		try {
			int expires = ((int) (System.currentTimeMillis() / 1000L)) + (days*86400) + (hours*3600);
			Houses.sqlite.query("INSERT INTO rentals(player, class, number, world, expires) VALUES('" + player + "', '" + houseClass + "', '" + houseNumber + "', '" + world + "', '" + expires + "')");
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	public void deleteOwner(String player) {
		try {
			Houses.sqlite.query("DELETE FROM guests WHERE house_id=" + getHouseId());
			Houses.sqlite.query("DELETE FROM houses WHERE player='" + player + "' AND class='" + houseClass + "' AND number='" + houseNumber + "' AND world='" + world + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void deleteGuest(String player) {
		try {
			Houses.sqlite.query("DELETE FROM guests WHERE player='" + player + "' AND house_id=" + getHouseId());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public boolean playerHasHouse(String player) {
		ResultSet rs = null;
		try {
			rs = Houses.sqlite.query("SELECT * FROM houses WHERE player='" + player + "' AND class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rsHasOutput(rs);
	}
	public boolean anyoneHasHouse() {
		ResultSet rs = null;
		try {
			rs = Houses.sqlite.query("SELECT * FROM houses WHERE class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rsHasOutput(rs);
	}
	public String getHouseOwner() {
		try {
			ResultSet rs = Houses.sqlite.query("SELECT * FROM houses WHERE class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
			if(rs.next()) {
				String owner = rs.getString("player");
				rs.close();
				return owner;
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	public boolean playerHasRental(String player) {
		ResultSet rs = null;
		try {
			rs = Houses.sqlite.query("SELECT * FROM rentals WHERE player='" + player + "' AND class='"  +  houseClass  +  "' AND number='"  +  houseNumber  +  "' AND world='" + world + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rsHasOutput(rs);
	}
	public static boolean hasTooMany(Houses plugin, String player) {
		int totalHouses = 0;
		int totalRentals = 0;
		int allowedHouses = plugin.getConfig().getInt("maximum-houses");
		if (allowedHouses < 0)
			return false;
		try {
			ResultSet rsHouses = Houses.sqlite.query("SELECT COUNT(*) AS nHouses FROM houses WHERE player='" + player + "'");
			if (rsHouses.next()) {
				totalHouses = rsHouses.getInt("nHouses");
			}
			rsHouses.close();
			ResultSet rsRentals = Houses.sqlite.query("SELECT COUNT(*) AS nHouses FROM houses WHERE player='" + player + "'");
			if (rsRentals.next()) {
				totalRentals = rsRentals.getInt("nHouses");
			}
			rsRentals.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if ((totalHouses + totalRentals) >= allowedHouses)
			return true;
		else
			return false;
	}
	public static boolean rsHasOutput(ResultSet rs) {
		try {
			if (rs.next()) {
				rs.close();
				return true;
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static int[] getNumbers(String tableName, int houseClass) {
		ResultSet rs;
		int[] numbers = null;
		try {
			rs = Houses.sqlite.query("SELECT DISTINCT number FROM " + tableName + " WHERE class='" + houseClass + "'");
			List<Integer> list = new ArrayList<Integer>();
			while (rs.next())
				list.add(rs.getInt("number"));
			rs.close();
			numbers = new int[list.size()];
			for(int i=0, len = list.size(); i < len; i++)
				numbers[i] = list.get(i);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return numbers;
	}
	public static int[] getClasses(String tableName) {
		ResultSet rs;
		int[] classes = null;
		try {
			rs = Houses.sqlite.query("SELECT DISTINCT class FROM " + tableName);
			List<Integer> list = new ArrayList<Integer>();
			while (rs.next())
				list.add(rs.getInt("class"));
			rs.close();
			classes = new int[list.size()];
			for(int i=0, len = list.size(); i < len; i++)
				classes[i] = list.get(i);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return classes;
	}
	public String[] getPlayerNames(String tableName) {
		ResultSet rs;
		String[] players = null;
		try {
			rs = Houses.sqlite.query("SELECT DISTINCT player FROM " + tableName);
			List<String> list = new ArrayList<String>();
			while (rs.next())
				list.add(rs.getString("player"));
			rs.close();
			players = list.toArray(new String[list.size()]);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return players;
	}
	public static String[] getWorldNames(String tableName) {
		ResultSet rs;
		String[] worlds = null;
		try {
			rs = Houses.sqlite.query("SELECT DISTINCT world FROM " + tableName);
			List<String> list = new ArrayList<String>();
			while (rs.next())
				list.add(rs.getString("world"));
			rs.close();
			worlds = list.toArray(new String[list.size()]);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return worlds;
	}
	@Deprecated
	public boolean signMatchesDatabase(Location loc) {
		try {
			ResultSet rs = Houses.sqlite.query("SELECT * FROM signs WHERE x='" + loc.getX() + "' AND y='" + loc.getY() + "' AND z='" + loc.getZ() + "'");
			if (rs.next()) {
				rs.close();
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isGuest(String player, String type) {
		try {
			ResultSet rs = Houses.sqlite.query("SELECT player FROM guests WHERE house_id=" + this.getHouseId() + " AND type='" + type + "'");
			while (rs.next()) {
				if (rs.getString("player").equalsIgnoreCase(player)) {
					rs.close();
					return true;
				}
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void deletePend(String player) {
		try {
			Houses.sqlite.query("DELETE FROM pending WHERE player='" + player + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean hasPending(String player) {
		ResultSet rs = null;
		try {
			rs = Houses.sqlite.query("SELECT * FROM pending WHERE player='" + player + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rsHasOutput(rs);
	}

	public void insertPend(String player, int price, String type, int days, int hours) {
		try {
			Houses.sqlite.query("INSERT INTO pending(player, class, number, type, price, world, days, hours) VALUES('" +
					player + "', '" + houseClass + "','" + houseNumber + "', '" + type + "', '" + price + "','" + world + "', " + days + ", " + hours + ")");
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	public void insertPend(String player, int price, String type) {
		insertPend(player, price, type, 0, 0);
	}

	public void insertPend(String player, String type) {
		insertPend(player, 0, type);
	}

	public String getBuilder() {
		try {
			String builder = "";
			ResultSet rs = Houses.sqlite.query("SELECT builder FROM signs WHERE class=" + houseClass + " AND number=" + houseNumber + " AND world='" + world + "'");
			if (rs.next())
				builder = rs.getString("builder");
			rs.close();
			if (!builder.isEmpty())
				return builder;
			else
				return "";
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String[] getGuests() {
		List<String> guests = new ArrayList<String>();
		try {
			ResultSet rs = Houses.sqlite.query("SELECT player FROM guests WHERE house_id=" + getHouseId());
			while (rs.next())
				guests.add(rs.getString("player"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return guests.toArray(new String[guests.size()]);
	}
}
