package com.hektropolis.houses.signs;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Door;
import com.hektropolis.houses.Errors;
import com.hektropolis.houses.Houses;
import com.hektropolis.houses.Permissions;
import com.hektropolis.houses.Ranks;
import com.hektropolis.houses.Utils;
import com.hektropolis.houses.database.DatabaseQuery;

public class SignListener implements Listener{

	private Houses plugin;
	private HouseSign houseSign;
	private Ranks ranks;

	public SignListener(Houses plugin) {
		this.plugin = plugin;
		this.ranks = new Ranks(plugin);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clicked = event.getClickedBlock();
		Player player = event.getPlayer();
		Errors error = new Errors(player);
		//final BlockFace[] doorFaces = {BlockFace.SELF, BlockFace.DOWN, BlockFace.UP,
		//BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST};
		if (clicked == null) {
			return;
		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (clicked.getType().equals(Material.WALL_SIGN)) {
				houseSign = new HouseSign((Sign) clicked.getState()).getType();
				int houseClass = 0;
				int houseNumber = 0;
				DatabaseQuery.deletePend(player.getName());
				if(houseSign instanceof BuySellSign) {
					houseClass = houseSign.getHouseClass();
					houseNumber = houseSign.getHouseNumber();
					DatabaseQuery dbQuery = new DatabaseQuery(houseSign.getWorld().getName(), houseClass, houseNumber);
					if(houseSign.isBuy()) {
						if(Permissions.hasPerm(player, "sign.use.buy") ) {
							if(!dbQuery.playerHasHouse(player.getName())) {
								if(!dbQuery.playerHasRental(player.getName())) {
									if(dbQuery.anyoneHasHouse()) {
										error.notify("This house is already owned by " + ChatColor.DARK_RED + dbQuery.getHouseOwner());
										return;
									}
									else if(dbQuery.anyoneHasRental()) {
										error.notify("This house is already rented by " + ChatColor.DARK_RED + dbQuery.getRentalOwner());
										return;
									}
									if (dbQuery.hasTooMany(plugin, player.getName())) {
										error.notify("You own too many houses");
										return;
									}
									int balance = (int) Houses.econ.getBalance(player);
									if(balance >= houseSign.getPrice()) {
										dbQuery.insertPend(player.getName(), houseSign.getPrice(), "buy");
										String builder = new DatabaseQuery(houseSign.getWorld().getName(), houseClass, houseNumber).getBuilder();
										String from = "";
										if (builder != null && !builder.isEmpty() && plugin.getConfig().getBoolean("house-builder-profit"))
											from = ChatColor.DARK_GREEN + " from " + ChatColor.GOLD + builder;
										player.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.GOLD + "/house confirm " + ChatColor.DARK_GREEN + "to buy class " + ChatColor.GOLD + houseClass + ChatColor.DARK_GREEN + " number " + ChatColor.GOLD + houseNumber + from);
									} else
										player.sendMessage(ChatColor.translateAlternateColorCodes('&', "You can't afford this house"));
								} else
									error.notify("You already rented this house");
							} else
								error.notify("You already own this house");
						} else
							error.notify("You are not allowed to buy houses");
					}
					else if(houseSign.isSell()) {
						if(Permissions.hasPerm(player, "sign.use.sell") ) {
							if(dbQuery.playerHasHouse(player.getName())) {
								dbQuery.insertPend(player.getName(), houseSign.getPrice(), "sell");
								player.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.GOLD + "/house confirm " + ChatColor.DARK_GREEN + "to sell class " + ChatColor.GOLD + houseClass + ChatColor.DARK_GREEN + " number " + ChatColor.GOLD + houseNumber + "");
								Block doorBlock = Utils.getDoorFromSign((Sign) clicked.getState());
								if(doorBlock != null) {
									BlockState state = doorBlock.getState();
									Door door = (Door) state.getData();
									door.setOpen(false);
									state.update();
								}
								return;
							} else
								error.notify("You don't own this house");
						} else
							error.warning("You are not allowed to sell houses");
					}
				}
				else if (houseSign instanceof RentSign) {
					houseClass = houseSign.getHouseClass();
					houseNumber = houseSign.getHouseNumber();
					DatabaseQuery dbQuery = new DatabaseQuery(houseSign.getWorld().getName(), houseClass, houseNumber);
					RentSign rentSign = (RentSign) houseSign;
					if (Permissions.hasPerm(player, "sign.use.rent")) {
						if(!dbQuery.playerHasHouse(player.getName())) {
							if(!dbQuery.playerHasRental(player.getName())) {
								if(dbQuery.anyoneHasHouse()) {
									error.notify("This house is already owned by " + ChatColor.DARK_RED + dbQuery.getHouseOwner());
									return;
								}
								else if(dbQuery.anyoneHasRental()) {
									error.notify("This house is already rented by " + ChatColor.DARK_RED + dbQuery.getRentalOwner());
									return;
								}
								int balance = (int) Houses.econ.getBalance(player);
								if(balance >= houseSign.getPrice()) {
									dbQuery.insertPend(player.getName(), houseSign.getPrice(), "rent", rentSign.getDays(), rentSign.getHours());
									player.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.GOLD + "/house confirm " + ChatColor.DARK_GREEN + "to rent class " + ChatColor.GOLD + houseClass + ChatColor.DARK_GREEN + " number " + ChatColor.GOLD + houseNumber + "");
								} else
									error.notify("You can't afford this house");
							} else
								error.notify("You already rented this house");
						} else
							error.notify("You already own this house");
					} else
						error.warning("You are not allowed to rent houses");
				}
				else if (houseSign instanceof LeaveSign) {
					houseClass = houseSign.getHouseClass();
					houseNumber = houseSign.getHouseNumber();
					DatabaseQuery dbQuery = new DatabaseQuery(houseSign.getWorld().getName(), houseClass, houseNumber);
					if (Permissions.hasPerm(player, "sign.use.leave")) {
						if(!dbQuery.playerHasHouse(player.getName()) && !dbQuery.playerHasRental(player.getName())) {
							if (dbQuery.anyoneHasHouse()) {
								error.notify("This house is already owned by " + ChatColor.DARK_RED + dbQuery.getHouseOwner());
								return;
							}
							else if(dbQuery.anyoneHasRental()) {
								error.notify("This house is already rented by " + ChatColor.DARK_RED + dbQuery.getRentalOwner());
								return;
							}
							else {
								error.notify("Nobody lives here");
							}
						} else {
							dbQuery.insertPend(player.getName(), "leave");
							player.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.GOLD + "/house confirm " + ChatColor.DARK_GREEN + "to abandon you house at class " + ChatColor.GOLD + houseClass + ChatColor.DARK_GREEN + " number " + ChatColor.GOLD + houseNumber + "");
						}
					} else {
						error.notify("You are not allowed to leave houses");
					}
				}
			}
			else if (clicked.getType().equals(Material.IRON_DOOR_BLOCK)) {
				BlockState state = clicked.getState();
				Door door = (Door) state.getData();
				if(!door.isTopHalf())
					clicked = clicked.getRelative(BlockFace.UP);
				if(Utils.getSignsFromDoor(clicked).length > 0) {
					boolean hasBuySell = false;
					boolean hasRent = false;
					boolean openDoor = false;
					boolean hasStaff = false;
					DatabaseQuery dbQuery = null;
					for (HouseSign sign : Utils.getSignsFromDoor(clicked)) {
						if (sign.getHouseClass() > 0 && sign.getHouseNumber() > 0)
							dbQuery = new DatabaseQuery(sign.getWorld().getName(), sign.getHouseClass(), sign.getHouseNumber());
						if (sign.getType() instanceof BuySellSign)
							hasBuySell = true;
						else if (sign.getType() instanceof RentSign)
							hasRent = true;
						else if (sign.getType() instanceof StaffSign)
							hasStaff = true;
					}
					String builder = dbQuery.getBuilder();
					String from = "";
					if (builder != null && !builder.isEmpty() && plugin.getConfig().getBoolean("house-builder-profit"))
						from = "from " + ChatColor.DARK_GREEN + builder + ChatColor.GOLD + " ";
					if (hasStaff) {
						error.notify("This house is owned by staff member " + ChatColor.DARK_RED + Utils.getSignsFromDoor(clicked)[0].getLine(3));
					}
					else if (hasBuySell && !hasRent) {
						if(dbQuery.anyoneHasHouse()) {
							if (dbQuery.playerHasHouse(player.getName()))
								openDoor = true;
							else if (dbQuery.isGuest(player.getName(), "owned")) {
								openDoor = true;
								player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Welcome to &3" + dbQuery.getHouseOwner() + "&2's house"));
							}
							else
								error.notify("This house is already owned by " + ChatColor.DARK_RED + dbQuery.getHouseOwner());
						} else {
							player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNobody lives here. &6Buy this house " + from + "if you want to go inside"));
							return;
						}
					}
					else if (hasRent && !hasBuySell) {
						if(dbQuery.anyoneHasRental()) {
							if(dbQuery.playerHasRental(player.getName())) {
								if(dbQuery.rentIsValid(player.getName())) {
									openDoor = true;
								} else {
									error.warning("Your rental has expired. You no longer have access to this house");
									dbQuery.deleteRental(player.getName());
									ranks.setRank(player.getName(), Integer.toString(houseSign.getHouseClass()), false);
								}
							}
							else if (dbQuery.isGuest(player.getName(), "rented")) {
								openDoor = true;
								player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Welcome to &3" + dbQuery.getHouseOwner() + "&2's rented house"));
							}
							else
								error.notify("This house is already rented by " + ChatColor.DARK_RED + dbQuery.getRentalOwner());
						} else {
							player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNobody lives here. &Rent this house if you want to go inside."));
							return;
						}
					}
					else if (hasRent && hasBuySell) {
						if (dbQuery.anyoneHasRental() || dbQuery.anyoneHasHouse()) {
							if (dbQuery.playerHasHouse(player.getName()))
								openDoor = true;
							else if (dbQuery.isGuest(player.getName(), "owned")) {
								openDoor = true;
								player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Welcome to &3" + dbQuery.getHouseOwner() + "&2's house"));
							}
							else if(dbQuery.anyoneHasHouse() && !dbQuery.anyoneHasRental())
								error.notify("This house is already owned by " + ChatColor.DARK_RED + dbQuery.getHouseOwner());
							if (dbQuery.playerHasRental(player.getName())) {
								if (dbQuery.rentIsValid(player.getName())) {
									openDoor = true;
								} else {
									error.warning("Your rental has expired. You no longer have access to this house");
									dbQuery.deleteRental(player.getName());
									ranks.setRank(player.getName(), Integer.toString(houseSign.getHouseClass()), false);
								}
							}
							else if (dbQuery.isGuest(player.getName(), "rented")) {
								openDoor = true;
								player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Welcome to &3" + dbQuery.getHouseOwner() + "&2's rented house"));
							}
							else if(!dbQuery.anyoneHasHouse() && dbQuery.anyoneHasRental())
								error.notify("This house is already rented by " + ChatColor.DARK_RED + dbQuery.getRentalOwner());
						} else {
							player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNobody lives here. &6Buy or rent this house " + from + "if you want to go inside"));
							return;
						}
					}
					if(openDoor) {
						clicked = clicked.getRelative(BlockFace.DOWN);
						state = clicked.getState();
						door = (Door) state.getData();
						door.setOpen(!door.isOpen());
						state.update();
						if(plugin.getConfig().getDouble("autoclose-door-delay") > 0) {
							int delay = (int) plugin.getConfig().getDouble("autoclose-door-delay") * 20;
							final Door closingDoor = door;
							final BlockState fState = state;
							Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
								@Override
								public void run() {
									closingDoor.setOpen(false);
									fState.update();
								}
							}, delay);
						}
					}
				}
			}
			return;
		}
	}
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if(event.getBlock().getType().equals(Material.WALL_SIGN)) {
			Sign wallSign = (Sign) event.getBlock().getState();
			HouseSign houseSign = new HouseSign(wallSign);
			if (houseSign.isValid()) {
				boolean hasRent = false;
				int hasOther = 0;
				if(!houseSign.isRent()) {
					HouseSign[] signs = Utils.getSignsFromDoor(Utils.getDoorFromSign(wallSign));
					if (signs != null) {
						for (HouseSign sign : signs) {
							if (sign instanceof RentSign) {
								hasRent = true;
							} else {
								hasOther++;
							}
						}
					}
				}
				if(hasRent && hasOther == 1) {
					new Errors(event.getPlayer()).severe("You must break the rent sign BEFORE breaking this sign");
					event.setCancelled(true);
					return;
				}
				try {
					ResultSet rs = Houses.sqlite.query("SELECT * FROM signs WHERE type='" + houseSign.getTypeString() + "' AND class='" + houseSign.getHouseClass() + "' AND number='" + houseSign.getHouseNumber() + "'");
					if(rs.next()) {
						rs.close();
						Houses.sqlite.query("DELETE FROM signs WHERE type='" + houseSign.getTypeString() + "' AND class='" + houseSign.getHouseClass() + "' AND number='" + houseSign.getHouseNumber() + "'");
					}
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		final Player player = event.getPlayer();
		final Errors error = new Errors(player);
		String[] line = event.getLines();
		if (line[0].equalsIgnoreCase("[Buy House]") ||
				line[0].equalsIgnoreCase("[Sell House]") ||
				line[0].equalsIgnoreCase("[Rent House]") ||
				line[0].equalsIgnoreCase("[House Info]") ||
				line[0].equalsIgnoreCase("[Leave House]") ||
				line[0].equalsIgnoreCase("[Staff House]")) {
			if (line[0].equalsIgnoreCase("[Buy House]") && Permissions.hasPerm(player, "sign.create.house") ||
					line[0].equalsIgnoreCase("[Sell House]") && Permissions.hasPerm(player, "sign.create.sell") ||
					line[0].equalsIgnoreCase("[Rent House]") && Permissions.hasPerm(player, "sign.create.rent") ||
					line[0].equalsIgnoreCase("[House Info]") && Permissions.hasPerm(player, "sign.create.info") ||
					line[0].equalsIgnoreCase("[Leave House]") && Permissions.hasPerm(player, "sign.create.leave") ||
					line[0].equalsIgnoreCase("[Staff House]") && Permissions.hasPerm(player, "sign.create.staff")) {
				SignProcessor sProcessor = new SignProcessor(plugin, event, error);
				if (sProcessor.parseSign()) {
					sProcessor.setSign();
				} else {
					return;
				}
				final Location loc = event.getBlock().getLocation();
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					@Override
					public void run() {
						new HouseSign((Sign) loc.getBlock().getState()).registerSignAt(error, loc, player, true, plugin.getConfig().getBoolean("house-builder-profit"));
						}
					}, 2);
				} else {
				error.notify("You are not allowed to create this type of sign for houses");
				return;
			}
		}
	}
}
