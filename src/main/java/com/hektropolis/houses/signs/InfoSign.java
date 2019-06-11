package com.hektropolis.houses.signs;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

public class InfoSign extends HouseSign {

	public InfoSign(Sign s) {
		super(s);
	}
	public void setClass(int houseClass) {
		s.setLine(1, ChatColor.ITALIC + "Class " + ChatColor.BLACK + houseClass);
	}
	public void setNumber(int houseNumber) {
		s.setLine(2, ChatColor.ITALIC + "Number " + ChatColor.BLACK + houseNumber);
	}
	@Override
	public int getHouseClass() {
		return Integer.parseInt(line[1].substring(10));
	}
	@Override
	public int getHouseNumber() {
		return Integer.parseInt(line[2].substring(11));
	}
	@Override
	public int getPrice() {
		return 0;
	}
}
