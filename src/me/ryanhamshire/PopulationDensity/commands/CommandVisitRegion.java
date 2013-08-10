package me.ryanhamshire.PopulationDensity.commands;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandVisitRegion extends PDCmd {
	private PopulationDensity instance;
	
	public CommandVisitRegion(PopulationDensity inst) {
		super("Visit the specified region", "visitregion");
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
			if(ConfigData.managedWorld == null) {
				Messages.send(player, Message.NO_WORLD);
				return true;
			}
		}
		
		// fail if player is null (not online player)
		if (player == null) {
			Messages.send(player, Message.NOT_ONLINE);
			return true;
		}
		
		if(args.length < 1)
			return false;
		
		//find the specified region, and send an error message if it's not found
		RegionCoordinates region = instance.dataStore.getRegionCoordinates(args[0].toLowerCase());									
		if(region == null) {
			player.sendMessage(Colors.ERR + "There's no region named \"" + args[0] + "\".  Unable to teleport.");
			return true;
		}
		
		if(!instance.playerCanTeleport(player, false))
			return true;
		
		//otherwise, teleport the user to the specified region					
		instance.TeleportPlayer(player, region, false);
		
		return true;
	}
}
