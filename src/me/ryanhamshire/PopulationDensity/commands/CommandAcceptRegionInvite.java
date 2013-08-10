package me.ryanhamshire.PopulationDensity.commands;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.PlayerData;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandAcceptRegionInvite extends PDCmd {
	private PopulationDensity instance;
	
	public CommandAcceptRegionInvite(PopulationDensity inst) {
		super("Accept a user's region invite", "acceptinvite");
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
		
		//if he doesn't have an invitation, tell him so
		if(playerData.regionInvitation == null) {
			player.sendMessage(Colors.ERR + "You haven't been invited to visit any regions. Someone must invite you with /InviteToRegion");
			return true;
		} else if(instance.playerCanTeleport(player, false)) {
			instance.TeleportPlayer(player, playerData.regionInvitation, false);
		}
					
		return true;
	}
}
