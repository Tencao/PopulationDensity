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

public class CommandRandomRegion extends PDCmd {
	private PopulationDensity instance;
	
	public CommandRandomRegion(PopulationDensity inst) {
		super("Teleport to a random region", "randomteleport");
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
		
		if(!instance.playerCanTeleport(player, false))
			return true;
		
		RegionCoordinates randomRegion = instance.dataStore.getRandomRegion(RegionCoordinates.fromLocation(player.getLocation()));
		
		if(randomRegion == null)
			player.sendMessage(Colors.ERR + "Sorry, you're in the only region so far.  Over time, more regions will open.");
		else
			instance.TeleportPlayer(player, randomRegion, false);
		
		return true;
	}
}
