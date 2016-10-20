package com.hektropolis.houses.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hektropolis.houses.Houses;


/** Database connection class & utilities **/

public class SQLite {

	private int timeout = 20;
	private Connection conn;
	private Statement statement;
	private Houses plugin;
	private Logger l;

	/* quick and dirty constructor to test the database passing the DriverManager name and the fully loaded url to handle */
	/* NB this will typically be available if you make this class concrete and not abstract */
	public SQLite(Houses plugin, String database) {
		this.plugin = plugin;
		l = plugin.getLogger();
		String path = plugin.getDataFolder().getAbsolutePath() + File.separator + "Databases" + File.separator;
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:" + path + database);
			statement = conn.createStatement();
			statement.setQueryTimeout(timeout); // set timeout to 30 sec.
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			l.log(Level.SEVERE, "Couldn't load the sqlite-JDBC driver.");
		} catch (SQLException e) {
			e.printStackTrace();
			l.log(Level.SEVERE, "Couldn't connect to database.");
		}
	}

	public Connection getConnection() {
		return conn;
	}

	public Statement getStatement() {
		return statement;
	}

	public void execute(String instruction) throws SQLException {
		statement.executeUpdate(instruction);
	}

	// processes an array of instructions e.g. a set of SQL command strings passed from a file
	//NB you should ensure you either handle empty lines in files by either removing them or parsing them out
	// since they will generate spurious SQLExceptions when they are encountered during the iteration....
	public void execute(String[] instructionSet) throws SQLException {
		for (int i = 0; i < instructionSet.length; i++) {
			execute(instructionSet[i]);
		}
	}

	public ResultSet query(String instruction) throws SQLException {
		if (instruction.contains("SELECT"))
			return statement.executeQuery(instruction);
		else
			execute(instruction);
		return null;
	}

	public void close() {
		try { conn.close(); } catch (Exception ignore) {}
	}

}