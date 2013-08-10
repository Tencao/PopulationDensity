package me.ryanhamshire.PopulationDensity.commands;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandCityRegion extends PDCmd {
	private PopulationDensity instance;
	
	public CommandCityRegion(PopulationDensity inst) {
		super("Teleport to the City region", "cityteleport");
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
		
		//if city world isn't defined, this command isn't available
		if(PopulationDensity.CityWorld == null) {
			Messages.send(player, Message.NO_CITY);
			return true;
		}
		
		//otherwise teleportation is enabled, so consider config, player location, player permissions					
		if(instance.playerCanTeleport(player, true)) {
			Location spawn = PopulationDensity.CityWorld.getSpawnLocation();
			
			Block block = spawn.getBlock();
			while(block.getType() != Material.AIR)
				block = block.getRelative(BlockFace.UP);					
			
			player.teleport(block.getLocation());
		}
		
		return true;
	}
}