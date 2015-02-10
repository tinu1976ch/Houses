package com.hektropolis.houses;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.FileUtil;

import com.hektropolis.houses.commands.Commands;
import com.hektropolis.houses.config.Config;
import com.hektropolis.houses.config.ConfigManager;
import com.hektropolis.houses.config.ConfigUpdater;
import com.hektropolis.houses.database.SQLite;
import com.hektropolis.houses.signs.SignListener;

public class Houses extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");
	public static Houses plugin;
	public static SQLite sqlite;
	public static Economy econ;
	public static Permission permission;
	public static String prefix;

	public void onEnable() {
		prefix = "["  +  getDescription().getName()  +  "] ";
		getConfig().options().copyDefaults(true);
		saveConfig();
		setupBackups();
		setupDatabase();
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			log.info(prefix + "- Disabled due to no Vault dependency found!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (setupEconomy()) {
			log.info(prefix + "Hooked into " + econ.getName());
		} else {
			log.info(prefix + "- Disabled due to no economy plugin found!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (setupPermissions()) {
			log.info(prefix + "Hooked into " + permission.getName());
		} else {
			log.info(prefix + "No permissions plugin found. It is highly recommended to use permissions with Houses");
		}
		if (config.getConfig().getBoolean("clean-database-on-start"))
			new Utils().cleanUpDatabase(getServer().getWorld(getServer().getWorlds().get(0).getName()));
		PluginManager manager = this.getServer().getPluginManager();
		manager.registerEvents(new SignListener(this), this);
		manager.registerEvents(new GeneralEventListener(this), this);
		getCommand("house").setExecutor(new Commands(this));
		new RentScanner(this);
	}
	
	public void onDisable() {
		if(sqlite != null)
			sqlite.close();
	}

	private void setupDatabase() {
		Connection con;
		try {
			//log.info(prefix + " Database not found, creating database: houses.db");
			if (!new File(getDataFolder().getAbsolutePath() + File.separator + "Databases").isDirectory()) {
				new File(getDataFolder().getAbsolutePath() + File.separator + "Databases").mkdir();
			}
			//sqlite = new SQLite(log, prefix, getDataFolder().getAbsolutePath() + File.separator + "Databases", "houses");
			sqlite = new SQLite(this, "houses.db");
			/*if (!sqlite.isTable("houses")) {
				log.info(prefix + "No houses table found, creating table: houses");*/
			sqlite.query("CREATE TABLE IF NOT EXISTS houses(" +
					"id INTEGER PRIMARY KEY ASC, " +
					"player VARCHAR(50) COLLATE NOCASE, " +
					"class INT, " +
					"number INT," +
					"world VARCHAR(50))" +
					"");
			/*}
			if (!sqlite.isTable("signs")) {
				log.info(prefix + "No signs table found, creating table: signs");*/
			sqlite.query("CREATE TABLE IF NOT EXISTS signs(" +
					"id INTEGER PRIMARY KEY ASC, " +
					"type VARCHAR(50) COLLATE NOCASE, " +
					"class INT, " +
					"number INT, " +
					"price INT, " +
					"x INT, " +
					"y INT, " +
					"z INT, " +
					"world VARCHAR(50)," +
					"builder VARCHAR(40) COLLATE NOCASE" +
					")");
			/*}
			if(!sqlite.isTable("rentals")) {
				log.info(prefix + "No rentals table found, creating table: rentals");*/
			sqlite.query("CREATE TABLE IF NOT EXISTS rentals (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"player VARCHAR(50) COLLATE NOCASE, " +
					"class INT, " +
					"number INT," +
					"world VARCHAR(50)," +
					"expires INT)");
			/*}
			if(!sqlite.isTable("guests")) {
				log.info(prefix + "No guests table found, creating table: guests");*/
			sqlite.query("CREATE TABLE IF NOT EXISTS guests (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"house_id INT," +
					"type VARCHAR(10)," +
					"player VARCHAR(50))");
			/*}
			if(!sqlite.isTable("pending")) {
				log.info(prefix + "No pending table found, creating table: pending");*/
			sqlite.query("CREATE TABLE IF NOT EXISTS pending(" +
					"id INTEGER PRIMARY KEY ASC, " +
					"player VARCHAR(50) COLLATE NOCASE, " +
					"class INT, " +
					"number INT, " +
					"price INT, " +
					"type VARCHAR(20), " +
					"days INT, " +
					"hours INT, " +
					"world VARCHAR(50))" +
					"");
			//}
			ResultSet rsHouse = sqlite.query("SELECT COUNT(*) AS totalH FROM houses");
			log.info(prefix + "Loaded " + rsHouse.getInt("totalH") + " owned houses");
			rsHouse.close();
			ResultSet rsRent = sqlite.query("SELECT COUNT(*) AS totalR FROM rentals");
			log.info(prefix + "Loaded " + rsRent.getInt("totalR") + " rentals");
			rsRent.close();
			ResultSet rsSign = sqlite.query("SELECT COUNT(*) AS totalS FROM signs");
			log.info(prefix + "Loaded " + rsSign.getInt("totalS") + " signs");
			rsSign.close();
		} catch(Exception e) {e.printStackTrace();}
		try {
			sqlite.query("ALTER TABLE signs ADD COLUMN builder VARCHAR(40) COLLATE NOCASE");
			sqlite.query("UPDATE signs SET builder='server' WHERE builder=NULL OR builder=''");
		} catch (SQLException e) {}
	}
	/*private void addMultiWorldSupport() {
		try {
			sqlite.query("ALTER TABLE signs ADD COLUMN world VARCHAR(255)");
		} catch (SQLException e) {}
		try {
			sqlite.query("ALTER TABLE houses ADD COLUMN world VARCHAR(255)");
		} catch (SQLException e) {}
		try {
			sqlite.query("ALTER TABLE rentals ADD COLUMN world VARCHAR(255)");
		} catch (SQLException e) {}
		String[] tables = {"signs", "houses", "rentals"};
		for (String table : tables) {
			try {
				sqlite.query("UPDATE " + table + " SET world='" + getServer().getWorlds().get(0).getName() + "' WHERE world IS NULL");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}*/
	private void setupBackups() {
		File backupFiles;
		File oldDatabaseFile;
		try {
			backupFiles = new File(getDataFolder().getAbsolutePath() + File.separator + "Databases" + File.separator + "Backups" + File.separator);
			if(!backupFiles.exists()){
				backupFiles.mkdirs();
			}
			oldDatabaseFile = new File(getDataFolder().getAbsolutePath()  + File.separator + "houses.db");
			//System.out.println(oldDatabaseFile.toURI().getPath());
			if(oldDatabaseFile.exists()) {
				FileUtil.copy(oldDatabaseFile, new File(getDataFolder().getAbsolutePath() + File.separator + "Databases" + File.separator + "houses.db"));
				//System.out.print("Trying to copy to " + new File(getDataFolder().getAbsolutePath() + File.separator + "Databases" + File.separator + "houses.db"));
				oldDatabaseFile.delete();
			}
		} catch(SecurityException e) {
			log.severe("Security problems when copying database");
			return;
		}
	}
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	private boolean setupPermissions()
	{
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

}
