package me.ryanhamshire.PopulationDensity.utils;

import me.ryanhamshire.PopulationDensity.PopulationDensity;

import org.bukkit.ChatColor;

public class Log {
	public enum Level {
		INFO('a'), WARN('6'), SEVERE('4');
		private char c;
		Level(char color) {
			c = color;
		}
		public String getColor() {
			return "\u00A7" + c;
		}
	}
	
	private PopulationDensity p;
	
	public Log(PopulationDensity plugin) {
		p = plugin;
	}
	
	/**
	 * Log a message with log level: INFO
	 * @param msg The message to log
	 * @see Level
	 */
	public void log(String msg) {
		p.getServer().getConsoleSender().sendMessage("[" + p.getDescription().getName() + "] " + msg);
	}
	
	/**
	 * Log a message with a specefied log level
	 * @param msg The message to log
	 * @param level The Level to log the message
	 * @see Level
	 */
	public void log(String msg, Level level) {
		p.getServer().getConsoleSender().sendMessage(level.getColor() + "[" + p.getDescription().getName() + "] " + msg);
	}
	
	/**
	 * Log a message with a specified color
	 * @param msg The message to log
	 * @param color The ChatColor to use when logging the message
	 * @see ChatColor
	 */
	public void log(String msg, ChatColor color) {
		p.getServer().getConsoleSender().sendMessage(color + "[" + p.getDescription().getName() + "] " + msg);
	}
}
