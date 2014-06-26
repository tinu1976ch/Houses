package com.hektropolis.houses.signs;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.hektropolis.houses.Errors;
import com.hektropolis.houses.Houses;
import com.hektropolis.houses.Permissions;

public class HouseSign {

	public Sign s;
	public String[] line;

	public HouseSign(Sign s) {
		this.s = s;
		line = s.getLines();
	}

	public boolean isBuy() {
		if(line[0].equals(SignType.BUY.getHeader()))
			return true;
		else
			return false;
	}
	public boolean isSell() {
		if(line[0].equals(SignType.SELL.getHeader()))
			return true;
		else
			return false;
	}
	public boolean isRent() {
		if(line[0].equals(SignType.RENT.getHeader()))
			return true;
		else
			return false;
	}
	public boolean isInfo() {
		if(line[0].equals(SignType.INFO.getHeader()))
			return true;
		else
			return false;
	}
	public boolean isStaff() {
		if (line[0].equals(SignType.STAFF.getHeader()))
			return true;
		else
			return false;
	}
	public boolean isLeave() {
		if (line[0].equals(SignType.LEAVE.getHeader()))
			return true;
		else
			return false;
	}
	public boolean isValid() {
		if(this.isBuy() || this.isSell() || this.isRent() || this.isInfo() || this.isLeave() || this.isStaff())
			return true;
		else
			return false;
	}
	public String getTypeString() {
		if(this.isBuy())
			return "buy";
		else if(this.isSell())
			return "sell";
		else if(this.isRent())
			return "rent";
		else if(this.isInfo())
			return "info";
		else if(this.isLeave())
			return "leave";
		else if(this.isStaff())
			return "staff";
		else
			return null;
	}

	public int getHouseClass() {
		return this.getType().getHouseClass();
	}
	public int getHouseNumber() {
		return this.getType().getHouseNumber();
	}
	public int getPrice() {
		return this.getType().getPrice();
	}
	public World getWorld() {
		return s.getWorld();
	}
	public HouseSign getType() {
		if(this.isBuy() || this.isSell())
			return new BuySellSign(s);
		else if (this.isRent())
			return new RentSign(s);
		else if (this.isInfo())
			return new InfoSign(s);
		else if (this.isLeave())
			return new LeaveSign(s);
		else if (this.isStaff())
			return new StaffSign(s);
		else
			return null;
	}
	public void setType(SignType type) {
		s.setLine(0, type.getHeader());
	}

	public boolean registerSignAt(Errors error, Location loc, Player player, boolean errorIfExists, boolean registerBuilder) {
		try {
			String builder = "";
			if (!Permissions.hasPerm(player, "admin") && registerBuilder == true) {
				builder = player.getName();
			}
			ResultSet rs = Houses.sqlite.query("SELECT * FROM signs WHERE type='" + this.getTypeString() + "' AND class='" + this.getHouseClass() + "' AND number='" + this.getHouseNumber() + "'");
			if(!rs.next()) {
				rs.close();
				Houses.sqlite.query("INSERT INTO signs(type, class, number, price, x, y, z, world, builder) VALUES(" +
						"'" +  this.getTypeString() +
						"', '" + this.getHouseClass() +
						"', '" + this.getHouseNumber() +
						"', '" + this.getPrice() +
						"', '" + loc.getBlockX() +
						"', '" + loc.getBlockY() +
						"', '" + loc.getBlockZ() +
						"', '" + loc.getWorld().getName() +
						"', '" + builder +
						"')");
				return true;
			} else {
				if(errorIfExists)
					error.warning("This sign already exists at " + rs.getInt("x") + ", " + rs.getInt("y") + ", " + rs.getInt("z")  +  ". Sign and builder was not registered");
				rs.close();
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	@Deprecated
	public void updateSignAt(Errors error, Location loc) {
		try {
			Houses.sqlite.query("UPDATE signs SET type='" +  this.getTypeString() +
					"' class='" + this.getHouseClass() +
					"' number='" + this.getHouseNumber() +
					"' price='" + this.getPrice() +
					"' x='" + loc.getBlockX() +
					"' y='" + loc.getBlockY() +
					"' z='" + loc.getBlockZ() +
					"' world='" + loc.getWorld().getName() +
					"')");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String getLine(int i) {
		return s.getLine(i);
	}

}
