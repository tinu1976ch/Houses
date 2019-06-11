package com.hektropolis.houses.config;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;

import com.hektropolis.houses.Houses;

public class Config {

	private Houses plugin;
	private File configFile;
	private YamlConfiguration config;
	private ConfigManager manager;

	public int sellPercentage;
	public boolean useClassPrices;
	public boolean useClassRankings;
	private int comments;

	public Config(InputStream configStream, File configFile, int comments, Houses plugin) {
		this.comments = comments;
		this.manager = new ConfigManager(plugin);

		this.configFile = configFile;
		//this.config = YamlConfiguration.loadConfiguration(configStream);
		this.config = YamlConfiguration.loadConfiguration(configFile);
	}

	private void loadValues() {
		this.sellPercentage = config.getInt("classes.sell-percentage");
		this.useClassPrices = config.getBoolean("use-class-prices");
		this.useClassRankings = config.getBoolean("use-class-rankings");
	}

	public void printValues() {
		System.out.println(sellPercentage);
		System.out.println(useClassPrices);
		System.out.println(useClassRankings);
	}

	public YamlConfiguration getConfig() {
		if (config == null) {
			this.reload();
		}
		return config;
	}

	public void set(String path, Object value, String... comment) {
		for(String comm : comment) {
			if(!this.config.contains(path)) {
				this.config.set(manager.getPluginName() + "_COMMENT_" + comments, " " + comm);
				comments++;
			}

		}
		this.config.set(path, value);
	}

	public void setHeader(String[] header) {
		manager.setHeader(this.configFile, header);
		this.comments = header.length + 2;
		this.reload();
	}

	public void reload() {
		//this.config = YamlConfiguration.loadConfiguration(manager.getConfigContent(configFile));
		this.config = YamlConfiguration.loadConfiguration(configFile);
	}

	public void save() {
		String config = this.config.saveToString();
		manager.saveConfig(config, this.configFile);
	}

	public Set<String> getKeys() {
		return this.config.getKeys(false);
	}

}
