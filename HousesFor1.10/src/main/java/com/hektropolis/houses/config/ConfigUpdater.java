package com.hektropolis.houses.config;

import java.io.File;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import com.hektropolis.houses.Houses;

public class ConfigUpdater {

	private Config config;

	public ConfigUpdater(Config config) {
		this.config = config;
	}

	public void updateDefaults() {
		FileConfiguration c = config.getConfig();
		if (!c.isSet("use-class-ranks"))
			this.config.set("use-class-rankings", true, "This will make players get the specified rank (permission group)", "for the class he/she buys");

		if (!c.isSet("use-class-prices"))
			this.config.set("use-class-prices", true, "Enabling this will let you leave the price row blank on signs ", "and let the plugin automatically set the right price");

		if (!c.isSet("autoclose-door-delay"))
			this.config.set("autoclose-door-delay", 2.5, "It is recommended to have autoclosing doors to avoid griefing");

		if (!c.isSet("home-teleport-cooldown"))
			this.config.set("home-teleport-cooldown", 3, "Cooldown for '/house home' command in seconds. Use -1 for no cooldown");

		if (!c.isSet("maximum-houses"))
			this.config.set("maximum-houses", -1, "Put -1 to allowed unlimited houses");

		if (!c.isSet("clean-database-on-start"))
			this.config.set("clean-database-on-start", true, "It is recommended to clean the database on start because ", "a corrupt database will cause trouble when doing database syncronization");

		if (!c.isSet("house-builder-profit"))
			this.config.set("house-builder-profit", false, "Enabling this will allow the house builder to receive", "the money from the buyer instead of money disappearing");

		if (!c.isSet("homeless"))
			this.config.set("homeless", "default", "This is the rank (permissions group) the players ", "will get if they become homeless");

		if (!c.isSet("sell-percentage"))
			this.config.set("sell-percentage", 75, "If using class prices, Houses will use this ", "percentage to calculate sell prices on signs");

		if (!c.isSet("worlds.display-worlds"))
			this.config.set("worlds.display-worlds", true, "Choose if the name/alias of the world should be displayed", "when doing all/player/class/number etc");
	}

	public void updateDeprecated() {
		if (config.getConfig().isSet("classes.homeless")) {
			System.out.println("Moving homeless in config");
			this.config.set("homeless", config.getConfig().get("classes.homeless"));
			this.config.set("classes.homeless", null);
		}
		if (config.getConfig().isSet("classes.sell-percentage")) {
			System.out.println("Moving sell-percentage in config");
			this.config.set("sell-percentage", config.getConfig().get("classes.sell-percentage"), "If using class prices, Houses will use ", "this percentage to calculate sell prices on signs");
			this.config.set("classes.sell-percentage", null);
		}
		if (config.getConfig().isSet("use-class-rankings")) {
			System.out.println("Moving use-class-rankings in config");
			this.config.set("use-class-ranks", config.getConfig().getBoolean("use-class-rankings"), "This will make players get the specified", "rank (permission group) for the class he/she buys");
			this.config.set("use-class-rankings", null);
		}
	}

	public void updateWorlds() {
		FileConfiguration c = config.getConfig();
		for (World world : Bukkit.getWorlds()) {
			String name = world.getName();
			if (!c.isSet("worlds." + name + ".display-name"))
				this.config.set("worlds." + name + ".display-name", name);
		}
		config.save();
	}

	public boolean hasOld() {
		if (config.getConfig().saveToString().contains("In here, you can set what ranking the player"))
			return true;
		else
			return false;
	}

	public void copyFromOld(Houses plugin) {
		FileConfiguration c = config.getConfig();
		File oldFile = new File(plugin.getDataFolder(), "config.yml");
		if (oldFile.renameTo(new File(plugin.getDataFolder(), "config.yml.old"))) {
			Set<String> keys = c.getKeys(true);
			plugin.setupConfig();
			Config newConfig = plugin.getHousesConfig();
			for (String path : keys) {
				if (!path.startsWith("Houses_COMMENT"))
					newConfig.set(path, c.get(path));
			}
		}
	}
}
