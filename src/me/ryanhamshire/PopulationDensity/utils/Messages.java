package me.ryanhamshire.PopulationDensity.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


// This class stores messages that are used repetatively throughout the plugin as well
// as some colors the help unify the color scheme of messages, and just
// to make this class a little more useful, a shorter method to send players a message
public class Messages {
	public static enum Colors {
		// Can have multiple chars. Colors come before formattings!
		TITLE('6','l'), HEAD('b'), NORM('a'), NOTE('7','o'), ERR('c'), WARN('6');
		private String c;
		Colors(char ... colorChar) {
			c = "";
			for (char ch : colorChar)
				c += "\u00A7" + ch;
			//           ^-- Section symbol
		}
		@Override
		public String toString() { return c; }
		
		// get a custom color from char array
		public String cc(char ... colors) {
			String s = "";
			for (char ch : colors)
				s += "\u00A7" + ch;
			return s;
		}
		
		public String cc(String colors) {
			return cc(colors.toCharArray());
		}
	}
	
	/**
	 * Fetches a no permission message
	 * Use Message.NO_PERM to not display the permission
	 * @param perm The permission to display in the message
	 * @return The String message
	 */
	public static String noPerm(String perm) {
		return (Colors.ERR + String.format("You lack the required permission: %s", perm));
	}
	
	public static enum Message {
		NO_PERM(Colors.ERR + "You do not have permission to do that"),
		NO_WORLD(Colors.ERR + "PopulationDensity has not been properly configured. Please update your config.yml and specify a world to manage."),
		NOT_ONLINE(Colors.ERR + "You must be in-game to use that"),
		NO_REGION(Colors.ERR + "You're not in a region!"),
		NO_CITY(Colors.ERR + "There is no City world to teleport to");
		private String msg;
		Message(String msg) {
			this.msg = msg;
		}
		public String msg() {
			return msg;
		}
	}
	
	/**
	 * Sends a message to a player from the Message list
	 * @param sender The CommandSender to send the message
	 * @param msg The Message enum to send
	 * @see Message
	 */
	public static void send(CommandSender sender, Message msg) {
		sender.sendMessage(msg.msg());
	}
	
	/**
	 * Sends a message to a player from the Message list
	 * @param player The Player to send the message
	 * @param msg The Message enum to send
	 * @see Message
	 */
	public static void send(Player player, Message msg) {
		player.sendMessage(msg.msg());
	}
}
