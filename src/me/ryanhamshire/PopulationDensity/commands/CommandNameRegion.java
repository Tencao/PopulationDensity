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

public class CommandNameRegion extends PDCmd {
	private PopulationDensity instance;
	
	public CommandNameRegion(PopulationDensity inst) {
		super("Name or rename the current region", "nameregion");
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
		
		// silently fail if player is null (not online player)
		if (player == null) {
			Messages.send(player, Message.NOT_ONLINE);
			return true;
		}
		
		RegionCoordinates currentRegion = RegionCoordinates.fromLocation(player.getLocation());
		if(currentRegion == null) {
			Messages.send(player, Message.NO_REGION);
			return true;
		}
		
		//validate argument
		if(args.length < 1)
			return false;
		if(args.length > 1) {
			player.sendMessage(Colors.ERR + "Region names may not include spaces.");
			return true;
		}
		
		String name = args[0];
		
		if(name.length() > 10) {
			player.sendMessage(Colors.ERR + "Region names can only be up to 10 letters long.");
			return true;
		}
		
		for(int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if(!Character.isLetter(c)) {
				player.sendMessage(Colors.ERR + "Region names may only include letters.");
				return true;
			}					
		}
		
		if(instance.dataStore.getRegionCoordinates(name) != null) {
			player.sendMessage(Colors.ERR + "There's already a region by that name.");
			return true;
		}
		
		//name region
		instance.dataStore.nameRegion(currentRegion, name);
		
		//update post
		instance.dataStore.AddRegionPost(currentRegion, true);	
		
		return true;
	}
}
