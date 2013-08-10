package me.ryanhamshire.PopulationDensity.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.PopulationDensity.DataStore;
import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Log.Level;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class ConfigData {
	public static World cityWorld;
	public static World managedWorld;
	public static String queueMessage;
	
	public static String [] mainCustomSignContent;
	public static String [] northCustomSignContent;
	public static String [] southCustomSignContent;
	public static String [] eastCustomSignContent;
	public static String [] westCustomSignContent;
	
	public static boolean allowTeleportation;
	public static boolean teleportFromAnywhere;
	public static boolean newPlayersSpawnInHomeRegion;
	public static boolean respawnInHomeRegion;
	public static boolean enableLoginQueue;
	public static boolean buildRegionPosts;
	public static boolean regrowGrass;
	public static boolean respawnAnimals;
	public static boolean regrowTrees;
	public static boolean limitEntities;
	public static boolean claimPosts;
	public static boolean guardPosts;
	public static boolean printResourceResults;
	public static boolean printEntityResults;
	public static boolean telePostSigns;
	public static boolean teleSigns;
	
	public static double densityRatio;
	
	public static int postTeleportRadius;
	public static int maxIdleMinutes;
	public static int minimumPlayersOnlineForIdleBoot;
	public static int reservedSlotsForAdmins;
	public static int maxAnimals;
	public static int maxMonsters;
	public static int maxVillagers;
	public static int maxDrops;
	public static int minimumRegionPostY;
	public static int postProtectionRadius;
	public static int regionScanHours;
	public static int entityScanHours;
	
	public ConfigData(FileConfiguration config) {
		
		config = ConfigUpdater.update(config);
		
		//prepare default setting for managed world...
		List<String> defaultManagedWorldNames = new ArrayList<String>();
		
		//build a list of normal environment worlds 
		List<World> worlds = PopulationDensity.instance.getServer().getWorlds();
		ArrayList<World> normalWorlds = new ArrayList<World>();
		for(int i = 0; i < worlds.size(); i++)
			if(worlds.get(i).getEnvironment() == Environment.NORMAL)
				normalWorlds.add(worlds.get(i));
		
		//if there's only one, make it the default
		if(normalWorlds.size() == 1)
			defaultManagedWorldNames.add(normalWorlds.get(0).getName());
		
		if (!(config.getString("Worlds.ManagedWorld") == null) && PopulationDensity.instance.getServer().getWorld(config.getString("Worlds.ManagedWorld")) != null)
			managedWorld = PopulationDensity.instance.getServer().getWorld(config.getString("Worlds.ManagedWorld"));
		else {
			PopulationDensity.instance.log.log("Cannot start, invalid world in config.yml", Level.SEVERE);
			PopulationDensity.instance.getServer().getPluginManager().disablePlugin(PopulationDensity.instance);
			return;
		}
		
		// XXX Wolrd Config Options
		if (!(config.getString("Worlds.CityWorld") == null) && PopulationDensity.instance.getServer().getWorld(config.getString("Worlds.CityWorld")) != null)
			cityWorld = PopulationDensity.instance.getServer().getWorld(config.getString("Worlds.CityWorld"));
		else
			cityWorld = null;
		
		// READ Spawning Config Options
		newPlayersSpawnInHomeRegion = config.getBoolean("Spawning.NewPlayersSpawnInHomeRegion", true);
		respawnInHomeRegion = config.getBoolean("Spawning.RespawnInHomeRegion", true);
		
		// READ Teleporting Config Options
		allowTeleportation = config.getBoolean("Teleporting.AllowTeleportation", true);
		teleportFromAnywhere = config.getBoolean("Teleporting.TeleportFromAnywhere", false);
		postTeleportRadius = config.getInt("Teleporting.PostTeleportRadius", 25);
		
		// READ Scanning Config Options
		densityRatio = config.getDouble("Scanning.DensityRatio", 1.0);
		regionScanHours = config.getInt("Scanning.HoursBetweenScans", 6);
		entityScanHours = config.getInt("Scanning.HoursBetweenEntityScans", 1);
		limitEntities = config.getBoolean("Scanning.LimitEntities", true);
		
		// READ ChunkLimits Config Options
		maxAnimals = config.getInt("ChunkLimits.MaximumAnimalsPerChunk", 12);
		maxMonsters = config.getInt("ChunkLimits.MaximumMonstersPerChunk", 8);
		maxVillagers = config.getInt("ChunkLimits.MaximumVillagersPerChunk", 5);
		maxDrops = config.getInt("ChunkLimits.MaximumDropsPerChunk", 25);
		
		// READ Queueing Config Options
		enableLoginQueue = config.getBoolean("Queueing.LoginQueueEnabled", true);
		minimumPlayersOnlineForIdleBoot = config.getInt("Queueing.MinimumPlayersOnlineForIdleBoot", PopulationDensity.instance.getServer().getMaxPlayers() / 2);

		queueMessage = config.getString("Queueing.LoginQueueMessage", "%queuePosition% of %queueLength% in queue.  Reconnect within 3 minutes to keep your place.  :)");
		reservedSlotsForAdmins = config.getInt("Queueing.ReservedSlotsForAdministrators", 1);
		if(reservedSlotsForAdmins < 0)
			reservedSlotsForAdmins = 0;
		
		// READ Misc Config Options
		maxIdleMinutes = config.getInt("Misc.MaxIdleMinutes", 10);
		regrowGrass = config.getBoolean("Misc.GrassRegrows", true);
		respawnAnimals = config.getBoolean("Misc.AnimalsRespawn", true);
		regrowTrees = config.getBoolean("Misc.TreesRegrow", true);
		printResourceResults = config.getBoolean("Misc.PrintResourceScanResults", true);
		printEntityResults = config.getBoolean("Misc.PrintEntityScanResults", false);
		
		// READ Regions Config Options
		buildRegionPosts = config.getBoolean("Regions.BuildRegionPosts", false);
		minimumRegionPostY = config.getInt("Regions.MinimumRegionPostY", 62);
		postProtectionRadius = config.getInt("Regions.PostProtectionRadius", 10);
		
		// READ Plugin Compatibility Options
		claimPosts = config.getBoolean("GriefPreventionOptions.ClaimRegionPosts", false);
		guardPosts = config.getBoolean("WorldGuardOptions.ClaimRegionPosts", false);
		
		//and write those values back and save. this ensures the config file is available on disk for editing
		
		// WRITE Worlds Config Options
		config.set("Worlds.CityWorldName", ((cityWorld == null)?"":cityWorld.getName()));
		config.set("Worlds.ManagedWorld", ((managedWorld == null)?"":managedWorld.getName()));
		
		// WRITE Spawning Config Options
		config.set("Spawning.NewPlayersSpawnInHomeRegion", newPlayersSpawnInHomeRegion);
		config.set("Spawning.RespawnInHomeRegion", respawnInHomeRegion);
		
		// WRITE Teleporting Config Options
		config.set("Teleporting.AllowTeleportation", allowTeleportation);
		config.set("Teleporting.TeleportFromAnywhere", teleportFromAnywhere);
		config.set("Teleporting.PostTeleportRadius", postTeleportRadius);
		config.set("Teleporting.TeleportViaPostSigns", false);
		config.set("Teleporting.TeleportViaOtherSigns", false);
		
		// WRITE Scanning Config Options
		config.set("Scanning.DensityRatio", densityRatio);
		config.set("Scanning.HoursBetweenScans", regionScanHours);
		config.set("Scanning.HoursBetweenEntityScans", entityScanHours);
		config.set("Scanning.LimitEntities", limitEntities);
		
		// WRITE ChunkLimits Config Options
		config.set("ChunkLimits.MaximumAnimalsPerChunk", maxAnimals);
		config.set("ChunkLimits.MaximumMonstersPerChunk", maxMonsters);
		config.set("ChunkLimits.MaximumVillagersPerChunk", maxVillagers);
		config.set("ChunkLimits.MaximumDropsPerChunk", maxDrops);
		
		// WRITE Queueing Config Options
		config.set("Queueing.MaxIdleMinutes", maxIdleMinutes);
		config.set("Queueing.LoginQueueEnabled", enableLoginQueue);
		config.set("Queueing.MinimumPlayersOnlineForIdleBoot", minimumPlayersOnlineForIdleBoot);
		config.set("Queueing.ReservedSlotsForAdministrators", reservedSlotsForAdmins);
		config.set("Queueing.LoginQueueMessage", queueMessage);
		
		// WRITE Misc Config Options
		config.set("Misc.GrassRegrows", regrowGrass);
		config.set("Misc.AnimalsRespawn", respawnAnimals);
		config.set("Misc.TreesRegrow", regrowTrees);
		config.set("Misc.PrintResourceScanResults", printResourceResults);
		config.set("Misc.PrintEntityScanResults", printEntityResults);
		
		// WRITE Regions Config Options
		config.set("Regions.PostProtectionRadius", postProtectionRadius);
		config.set("Regions.BuildRegionPosts", buildRegionPosts);
		config.set("Regions.MinimumRegionPostY", minimumRegionPostY);
		
		// WRITE Plugin Compatibility Options
		config.set("GriefPreventionClaims.ClaimRegionPosts", claimPosts);
		config.set("WorldGuardOptions.ClaimRegionPosts", guardPosts);
		
		// this is a combination load/preprocess/save for custom signs on the region posts
		mainCustomSignContent = PopulationDensity.instance.initializeSignContentConfig(config, "Regions.CustomSigns.Main", new String [] {"", "Population", "Density", ""});
		northCustomSignContent = PopulationDensity.instance.initializeSignContentConfig(config, "Regions.CustomSigns.North", new String [] {"", "", "", ""});
		southCustomSignContent = PopulationDensity.instance.initializeSignContentConfig(config, "Regions.CustomSigns.South", new String [] {"", "", "", ""});
		eastCustomSignContent = PopulationDensity.instance.initializeSignContentConfig(config, "Regions.CustomSigns.East", new String [] {"", "", "", ""});
		westCustomSignContent = PopulationDensity.instance.initializeSignContentConfig(config, "Regions.CustomSigns.West", new String [] {"", "", "", ""});
		
		// No more PopulationDensity nodes, remove them all
		config.set("PopulationDensity", null);
		
		try {
			config.save(DataStore.configFilePath);
		} catch(IOException exception) {
			PopulationDensity.instance.log.log("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"", Log.Level.SEVERE);
		}
	}
	
	public static me.ryanhamshire.GriefPrevention.DataStore getGPData() {
		if (PopulationDensity.instance.getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
			return GriefPrevention.instance.dataStore;
		} else
			return null;
	}
	
	public static WorldGuardPlugin getWGData() {
		if (PopulationDensity.instance.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			return WGBukkit.getPlugin();
		} else
			return null;
	}
}
