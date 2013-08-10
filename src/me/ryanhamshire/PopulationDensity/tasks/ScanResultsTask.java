package me.ryanhamshire.PopulationDensity.tasks;

import java.util.List;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.ChatColor;

public class ScanResultsTask implements Runnable {
	private List<String> logEntries;
	private boolean openNewRegion;
	
	public ScanResultsTask(List<String> logEntries, boolean openNewRegion) {
		this.logEntries = logEntries;
		this.openNewRegion = openNewRegion;
	}
	
	@Override
	public void run() {
		//collect garbage
		System.gc();
		
		for (String s : logEntries) {
			PopulationDensity.instance.log.log(s, ChatColor.WHITE);
		}
		
		if(this.openNewRegion) {
			RegionCoordinates newRegion = PopulationDensity.instance.dataStore.addRegion();
			PopulationDensity.instance.scanRegion(newRegion, true);
		}		
	}
}
