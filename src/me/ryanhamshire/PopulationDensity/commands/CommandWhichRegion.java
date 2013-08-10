package me.ryanhamshire.PopulationDensity.commands;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandWhichRegion extends PDCmd {
	private PopulationDensity instance;
	
	public CommandWhichRegion(PopulationDensity inst) {
		super("Display the name of the current region", "whichregion");
		instance = inst;
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
		
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			if(PopulationDensity.managedWorld == null) {
				Messages.send(player, Message.NO_WORLD);
				return true;
			}
		}
		
		// fail if player is null (not online player)
		if (player == null) {
			Messages.send(player, Message.NOT_ONLINE);
			return true;
		}
		
		RegionCoordinates currentRegion = RegionCoordinates.fromLocation(player.getLocation());
		if(currentRegion == null) {
			Messages.send(player, Message.NO_REGION);
			return true;
		}
		
		String regionName = instance.dataStore.getRegionName(currentRegion);
		if(regionName == null)
			player.sendMessage(Colors.NORM + "You're in the wilderness!  This region doesn't have a name.");
		else
			player.sendMessage(Colors.NORM + "You're in the " + Colors.HEAD + PopulationDensity.capitalize(regionName) + Colors.NORM + " region.");				
		
		return true;
	}
}
