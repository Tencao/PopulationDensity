package me.ryanhamshire.PopulationDensity.utils;

import me.ryanhamshire.PopulationDensity.PopulationDensity;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// A few methods and what-not to make processing commands and arguments a bit easier
public abstract class PDCmd implements CommandExecutor {
	private static final String PRE = "populationdensity.";
	protected String desc, node;
	
	public PDCmd(String description, String permNode) {
		desc = description;
		node = permNode;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public String getPermission() {
		return PRE + "node";
	}
	
	// Just a shorter perm check, ie, perm(p, permNode), rather
	// than PopulationDensity.instance.perms.had(p, perm)
	// Doesn't take full permission node, only the part
	// after the "PopulationDensity." for sake of unity
	/**
	 * Checks if the Player has the permission node
	 * @param p The Player to check
	 * @param perm The permission node after "PopulationDensity." ...
	 * @return true if the player has the permission, otherwise false
	 */
	public static boolean perm(Player p, String perm) {
		return (PopulationDensity.instance.perms.has(p, PRE + perm));
	}
	
	// Same as perm(Player, String), but for checking CommandSenders
	/**
	 * Checks if the CommandSender has the permission node
	 * @param sender The CommandSender to check
	 * @param perm The permission node after "PopulationDensity." ...
	 * @return true if the player has the permission, otherwise false
	 */
	public static boolean perm(CommandSender sender, String perm) {
		return (PopulationDensity.instance.perms.has(sender, PRE + perm));
	}
	
	// Method to check if a provided String (arg) matches any
	// Strings in a String array (args). I use this a lot when
	// I check for command argument aliases. ie:
	// eq(arg, "check", "status"); // true if arg equals check OR status
	// Also helps prevent length OR if statements
	/**
	 * Compares a String (arg) against several Strings (args)
	 * @param arg The String to compare against args
	 * @param args List of Strings to compare against arg
	 * @return true if any word in args matches arg (not case-sensitive)
	 */
	public static boolean eq(String arg, String ... args) {
		for (String a : args)
			if (a.equalsIgnoreCase(arg))
				return true;
		return false;
	}
}
