package com.hektropolis.houses.signs;

import org.bukkit.block.Sign;

public class StaffSign extends HouseSign {

	public StaffSign(Sign s) {
		super(s);
	}

	@Override
	public int getHouseClass() {
		if (s.getLine(1).endsWith("Owner"))
			return 1;
		else if (s.getLine(1).endsWith("Admin"))
			return 2;
		else if (s.getLine(1).endsWith("Moderator"))
			return 3;
		else
			return 0;
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
