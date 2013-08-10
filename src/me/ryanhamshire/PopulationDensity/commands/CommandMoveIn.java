package me.ryanhamshire.PopulationDensity.commands;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.PlayerData;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandMoveIn extends PDCmd {
	private PopulationDensity instance;
	
	public CommandMoveIn(PopulationDensity inst) {
		super("Move in to your current region", "movein");
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
		PlayerData playerData = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			if(PopulationDensity.managedWorld == null) {
				Messages.send(player, Message.NO_WORLD);
				return true;
			}
			playerData = instance.dataStore.getPlayerData(player);
		}
		
		// silently fail if player is null (not online player)
		if (player == null) {
			Messages.send(player, Message.NOT_ONLINE);
			return true;
		}
		
		//if not in the managed world, /movein doesn't make sense
		if(!player.getWorld().equals(PopulationDensity.managedWorld)) {
			player.sendMessage(Colors.ERR + "Sorry, no one can move in here.");
			return true;
		}
					
		RegionCoordinates currentRegion = RegionCoordinates.fromLocation(player.getLocation());
		
		if(currentRegion.equals(playerData.homeRegion)) {
			player.sendMessage(Colors.ERR + "This region is already your home!");
			return true;
		}
		
		playerData.homeRegion = RegionCoordinates.fromLocation(player.getLocation());
		instance.dataStore.savePlayerData(player, playerData);
		player.sendMessage(Colors.NORM + "Welcome to your new home!");
		player.sendMessage(Colors.HEAD + "/HomeRegion " + Colors.NORM + "Return here");
		player.sendMessage(Colors.HEAD + "/InviteToRegion " + Colors.NORM + "Invite other players here.");

		return true;
	}
}