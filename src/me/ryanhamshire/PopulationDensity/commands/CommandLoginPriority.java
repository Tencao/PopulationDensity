package me.ryanhamshire.PopulationDensity.commands;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.PlayerData;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandLoginPriority extends PDCmd {
	private PopulationDensity instance;
	
	public CommandLoginPriority(PopulationDensity inst) {
		super("Set the specified user's login priority", "setloginpriority");
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
		
		//requires exactly two parameters, the other player's name and the priority
		if(args.length != 2 && args.length != 1)
			return false;
		
		PlayerData targetPlayerData = null;
		OfflinePlayer targetPlayer = null;
		if (args.length > 0) {
			//find the specified player
			targetPlayer = instance.resolvePlayer(args[0]);
			if(targetPlayer == null) {
				PopulationDensity.sendMessage(player, Colors.ERR + "Player \"" + args[0] + "\" not found.");
				return true;
			}
			
			targetPlayerData = instance.dataStore.getPlayerData(targetPlayer);
			
			PopulationDensity.sendMessage(player, Colors.HEAD + targetPlayer.getName() + "'s " + Colors.NORM + "login priority: " + Colors.HEAD + targetPlayerData.loginPriority);
			
			if(args.length < 2)
				return false;  //usage displayed
		
			//parse the adjustment amount
			int priority;			
			try {
				priority = Integer.parseInt(args[1]);
			} catch(NumberFormatException numberFormatException) {
				return false;  //causes usage to be displayed
			}
			
			//set priority			
			if(priority > 100)
				priority = 100;
			else if(priority < 0)
				priority = 0;
			
			targetPlayerData.loginPriority = priority;
			instance.dataStore.savePlayerData(targetPlayer, targetPlayerData);
			
			//confirmation message
			PopulationDensity.sendMessage(player, Colors.ERR + "Set " + targetPlayer.getName() + "'s priority to " + priority + ".");
			
			return true;
		}
	return false;
	}
}
