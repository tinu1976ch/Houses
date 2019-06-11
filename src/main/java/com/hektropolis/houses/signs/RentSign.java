package com.hektropolis.houses.signs;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

import com.hektropolis.houses.Utils;

public class RentSign extends HouseSign {

	public RentSign(Sign s) {
		super(s);
	}

	public int getDays() {
		return Integer.parseInt(Character.toString(line[1].charAt(0)));
	}
	public int getHours() {
		return Integer.parseInt(Character.toString(line[2].charAt(0)));
	}
	@Override
	public int getPrice() {
		return Integer.parseInt(line[3].substring(3));
	}
	@Override
	public int getHouseClass() {
		if(this.getHelperSign() != null)
			return this.getHelperSign().getHouseClass();
		else return 0;
	}
	@Override
	public int getHouseNumber() {
		if(this.getHelperSign() != null)
			return this.getHelperSign().getHouseNumber();
		else return 0;
	}
	public void setPrice(int price) {
		s.setLine(3, ChatColor.DARK_GREEN + "$" + price);
		s.update();
	}
	public int calcPrice(int perDayCost) {
		return this.getDays() * perDayCost + (this.getHours() * perDayCost)/24;
	}
	public HouseSign getHelperSign() {
		HouseSign[] signs = Utils.getSignsFromDoor(Utils.getDoorFromSign(s));
		if (signs == null)
			return null;
		if (signs.length > 0) {
			for (HouseSign searchedSign : signs) {
				if (searchedSign instanceof BuySellSign || searchedSign instanceof InfoSign || searchedSign instanceof LeaveSign) {
					return searchedSign;
				}
			}
		}
		return null;
	}
}
