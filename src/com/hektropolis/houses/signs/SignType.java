package com.hektropolis.houses.signs;

import org.bukkit.ChatColor;

public enum SignType {

	BUY (ChatColor.DARK_BLUE + "[Buy House]"),
	SELL (ChatColor.DARK_BLUE + "[Sell House]"),
	RENT (ChatColor.DARK_BLUE + "[Rent House]"),
	INFO (ChatColor.DARK_BLUE + "[House Info]"),
	STAFF (ChatColor.DARK_BLUE + "[Staff House]"),
	LEAVE (ChatColor.DARK_BLUE + "[Leave House]");

	private final String line1;

	SignType(String line1) {
		this.line1 = line1;
	}

	public String getHeader() {
		return line1;
	}

}
