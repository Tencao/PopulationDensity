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

public class CommandInviteToRegion extends PDCmd {
	private PopulationDensity instance;
	
	public CommandInviteToRegion(PopulationDensity inst) {
		super("Invite a player to your region", "invitetoregion");
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
		
		if(args.length < 1)
			return false;
		
		//figure out the player's home region
		RegionCoordinates homeRegion = playerData.homeRegion;
		
		//send a notification to the invitee, if he's available
		Player invitee = instance.getServer().getPlayer(args[0]);
		if(invitee != null) {
			if (invitee.equals(player)) {
				player.sendMessage(Colors.ERR + "You can't invite yourself!");
				return true;
			}
			
			if (!instance.perms.has(invitee, "populationdensity.acceptinvite")) {
				player.sendMessage(Colors.ERR + "This player doesn't have permission to accept invites");
				return true;
			}
			
			playerData = instance.dataStore.getPlayerData(invitee);
			playerData.regionInvitation = homeRegion;
			player.sendMessage(Colors.NORM + "Invitation sent.  " + Colors.HEAD + invitee.getName() + Colors.NORM + " must use a region post to teleport to your region.");
			
			invitee.sendMessage(Colors.HEAD + player.getName() + Colors.NORM + " has invited you to visit his or her home region!");
			invitee.sendMessage(Colors.NORM + "Stand near a region post and use " + Colors.HEAD + "/AcceptRegionInvite" + Colors.NORM + " to accept.");
		} else {
			player.sendMessage(Colors.ERR + "There's no player named \"" + Colors.HEAD + args[0] + Colors.NORM + "\" online right now.");
		}
		
		return true;
	}
}
