package com.hektropolis.houses.signs;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

public class BuySellSign extends HouseSign {

	public BuySellSign(Sign s) {
		super(s);
	}
	
	public void setClass(int houseClass) {
		s.setLine(1, ChatColor.ITALIC + "Class " + ChatColor.BLACK + houseClass);
	}
	public void setNumber(int houseNumber) {
		s.setLine(2, ChatColor.ITALIC + "Number " + ChatColor.BLACK + houseNumber);
	}
	public void setPrice(int price) {
		s.setLine(3, ChatColor.DARK_GREEN + "$" + price);
	}
	public String getBuyOrSell() {
		if(this.isBuy())
			return "buy";
		else if(this.isSell())
			return "sell";
		else return null;
	}
	public int getHouseClass() {
		return Integer.parseInt(line[1].substring(10));
	}
	public int getHouseNumber() {
		return Integer.parseInt(line[2].substring(11));
	}
	public int getPrice() {
		return Integer.parseInt(line[3].substring(3));
	}
}
