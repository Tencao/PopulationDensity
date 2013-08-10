package me.ryanhamshire.PopulationDensity.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Messages;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.Messages.Message;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandListRegions extends PDCmd {
	PopulationDensity p;
	public CommandListRegions(PopulationDensity plugin) {
		super("List all the current regions", "listregions");
		p = plugin;
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
		
		String regions = "plugins" + File.separator + "PopulationDensityData" + File.separator + "RegionData";
		File regFiles = new File(regions);
		
		List<String> regNames = new ArrayList<String>();
		
		for (File child : regFiles.listFiles())
			if (child.getName().matches("^[a-z]{1,15}$"))
				if (!regNames.contains(child.getName()))
					regNames.add(child.getName());
		
		if (!regNames.isEmpty()) {
			sender.sendMessage(Colors.TITLE + "PopuationDensity Regions:");
			for (String r : regNames) {
				RegionCoordinates n = PopulationDensity.instance.dataStore.getRegionCoordinates(r);
				int xPos = (400 * n.getX())+200;
				int yPos = (400 * n.getZ())+200;
				sender.sendMessage(Colors.HEAD + "> " + r + Colors.NOTE + " (" + xPos + ", " + yPos + ")");
			}
		}
		return true;
	}
}
