package me.ryanhamshire.PopulationDensity.tasks;

import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Log.Level;

import org.bukkit.Chunk;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Villager;

public class EntityScanTask implements Runnable {
	public void run() {
		List<String> logEntries = new ArrayList<String>();
		int gac = 0, gmc = 0, gvc = 0, gdc = 0; // Global entity counts
		int gar = 0, gmr = 0, gvr = 0, gdr = 0; // Count how many have been removed
		
		//scan loaded chunks for chunks with too many monsters or items, and remove the superfluous
		if(ConfigData.limitEntities) {
			if (ConfigData.managedWorld == null) {
				PopulationDensity.instance.log.log("Managed world is null", Level.SEVERE);
				return;
			}
			Chunk[] chunks = ConfigData.managedWorld.getLoadedChunks();
			
			if (chunks.length > 0)
				PopulationDensity.instance.log.log("Scanning for excess entities...");
			
			for(int i = 0; i < chunks.length; i++) {
				Chunk chunk = chunks[i];
				
				Entity [] entities = chunk.getEntities();
				
				// Per chunk entity counts
				int monsterCount = 0;
				int itemCount = 0;		
				int animalCount = 0;
				int villagerCount = 0;
				
				for(int j = 0; j < entities.length; j++) {
					Entity entity = entities[j];
					if(entity instanceof Animals) {
						animalCount++;
						gac++;
						if(animalCount > ConfigData.maxAnimals) {
							entity.remove();
							gar++;
						}
					} else if(entity instanceof Villager) {
						villagerCount++;
						gvc++;
						if(villagerCount > ConfigData.maxVillagers) {
							entity.remove();
							gvr++;
						}
					} else if(entity instanceof Creature) {
						monsterCount++;
						gmc++;
						if(monsterCount > ConfigData.maxMonsters) {
							entity.remove();
							gmr++;
						}
					} else if(entity instanceof Item) {
						itemCount++;
						gdc++;
						if(itemCount > ConfigData.maxDrops) {
							entity.remove();
							gdr++;
						}
					}
				}
			}
			//deliver report	
			if (ConfigData.printEntityResults) {
				logEntries.add("/+============[ Entity Scan Result ]============");
				logEntries.add("|| Animals ..... " + gac + " / " + ConfigData.maxAnimals + " (Removed " + gar + ")");
				logEntries.add("|| Monsters .... " + gmc + " / " + ConfigData.maxMonsters + " (Removed " + gmr + ")");
				logEntries.add("|| Villagers ... " + gvc + " / " + ConfigData.maxVillagers + " (Removed " + gvr + ")");
				logEntries.add("|| Drops ....... " + gdc + " / " + ConfigData.maxDrops + " (Removed " + gdr + ")");
				logEntries.add("\\+==============================================");
			}
			
			
			//now that we're done, notify the main thread
			ScanResultsTask resultsTask = new ScanResultsTask(logEntries, false);
			PopulationDensity.instance.getServer().getScheduler().scheduleSyncDelayedTask(PopulationDensity.instance, resultsTask, 5L);
		}
	}

}
