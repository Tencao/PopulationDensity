package me.ryanhamshire.PopulationDensity.commands;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.PlayerData;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHomeRegion extends PDCmd {
	private PopulationDensity instance;
	
	public CommandHomeRegion(PopulationDensity inst) {
		super("Return to your home region", "hometeleport");
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
			if(ConfigData.managedWorld == null) {
				Messages.send(player, Message.NO_WORLD);
				return true;
			}
			playerData = instance.dataStore.getPlayerData(player);
		}
		
		// fail if player is null (not online player)
		if (player == null) {
			Messages.send(player, Message.NOT_ONLINE);
			return true;
		}
		
		//check to ensure the player isn't already home
		RegionCoordinates homeRegion = playerData.homeRegion;
		if(!player.hasPermission("populationdensity.teleportanywhere") && !ConfigData.teleportFromAnywhere && homeRegion.equals(RegionCoordinates.fromLocation(player.getLocation()))) {
			player.sendMessage(Colors.ERR + "You're already in your home region.");
			return true;
		}
		
		//consider config, player location, player permissions
		if(instance.playerCanTeleport(player, true)) {
			instance.TeleportPlayer(player, homeRegion, false);
			return true;
		}
		
		return true;
	}
}