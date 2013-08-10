package me.ryanhamshire.PopulationDensity.commands;

import java.util.HashMap;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

public class CommandPopulationDensity extends PDCmd {
	private PopulationDensity p;
	public CommandPopulationDensity(PopulationDensity plugin) {
		super("Help command", "help");
		p = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!perm(sender, getPermission())) {
			if (sender.isOp())
				sender.sendMessage(Messages.noPerm(getPermission()));
			else
				Messages.send(sender, Message.NO_PERM);
			return true;
		}

		if (args.length == 0) {
			sender.sendMessage(Colors.TITLE + "Population Density" + Colors.NOTE + " (v" + p.getDescription().getVersion() + ")");
			sender.sendMessage(Colors.NORM + "Original Author: " + Colors.NORM + "bigscary  " + Colors.HEAD + "Current: OffLuffy");
			sender.sendMessage(Colors.NORM + "For more help, type " + Colors.HEAD + "/pd help");
		} else {
			if (args[0].equalsIgnoreCase("help")) {
				HashMap<PluginCommand, PDCmd> cmds = PopulationDensity.instance.commands;
				for (PluginCommand pc : cmds.keySet())
					if (perm(sender, pc.getPermission()))
						sender.sendMessage(Colors.HEAD + "/" + pc.getLabel() + ChatColor.RESET + ChatColor.BOLD + " > " + Colors.NORM + cmds.get(pc).getDescription());
			}
		}
		return true;
	}
}
