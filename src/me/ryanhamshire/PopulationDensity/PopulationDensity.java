/*
    PopulationDensity Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.PopulationDensity;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import me.ryanhamshire.PopulationDensity.commands.CommandAcceptRegionInvite;
import me.ryanhamshire.PopulationDensity.commands.CommandAddRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandAddRegionPost;
import me.ryanhamshire.PopulationDensity.commands.CommandCityRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandHomeRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandInviteToRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandListRegions;
import me.ryanhamshire.PopulationDensity.commands.CommandLoginPriority;
import me.ryanhamshire.PopulationDensity.commands.CommandMoveIn;
import me.ryanhamshire.PopulationDensity.commands.CommandNameRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandNewestRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandPopulationDensity;
import me.ryanhamshire.PopulationDensity.commands.CommandRandomRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandScanRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandVisitRegion;
import me.ryanhamshire.PopulationDensity.commands.CommandWhichRegion;
import me.ryanhamshire.PopulationDensity.events.BlockEventHandler;
import me.ryanhamshire.PopulationDensity.events.EntityEventHandler;
import me.ryanhamshire.PopulationDensity.events.PlayerEventHandler;
import me.ryanhamshire.PopulationDensity.events.WorldEventHandler;
import me.ryanhamshire.PopulationDensity.tasks.AfkCheckTask;
import me.ryanhamshire.PopulationDensity.tasks.EntityScanTask;
import me.ryanhamshire.PopulationDensity.tasks.ScanOpenRegionTask;
import me.ryanhamshire.PopulationDensity.tasks.ScanRegionTask;
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Log;
import me.ryanhamshire.PopulationDensity.utils.Log.Level;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.NameData;
import me.ryanhamshire.PopulationDensity.utils.PDCmd;
import me.ryanhamshire.PopulationDensity.utils.PlayerData;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PopulationDensity extends JavaPlugin {
	// for convenience, a reference to the instance of this plugin
	public static PopulationDensity instance;
	
	// Custom logger that allows for colored logging ;)
	public Log log = new Log(this);
		
	// developer configuration, not modifiable by users
	public static final int REGION_SIZE = 400;
	
	// the world managed by this plugin
	public static World managedWorld;
	
	// the default world, not managed by this plugin
	// (may be null in some configurations)
	public static World CityWorld;
	
	// Vault Permission object
	public Permission perms;
	
	// this handles data storage, like player and region data
	public DataStore dataStore;
	
	// list of commands that are registered
	public HashMap<PluginCommand, PDCmd> commands = new HashMap<PluginCommand, PDCmd>();
	
	// initializes well...   everything
	public void onEnable() {	
		
		instance = this;
		
		// Setup Vault
		if (!setupPermissions()) {
			log.log("Disabled due to no Vault dependency found!", Level.SEVERE);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		// load the config if it exists
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
		new ConfigData(config); // Static method to fetch config variables
		
		// get a reference to the managed world
		if(ConfigData.managedWorld == null) {
			log.log("An invalid managed world was found in config! Please update your config.yml", Log.Level.SEVERE);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		managedWorld = ConfigData.managedWorld;
		
		new NameData(new File(DataStore.namesFilePath)); // Names list, loaded externally
		
		// when datastore initializes, it loads player and region data, and posts some stats to the log
		this.dataStore = new DataStore();
		
		// cache command list
		// This list is used in the /populationdensity command to print out help
		// See PDCmd class. Stores description and perm for each command extending PDCmd
		commands.put(getServer().getPluginCommand("visitregion"),new CommandVisitRegion(this));
		commands.put(getServer().getPluginCommand("newestregion"),new CommandNewestRegion(this));
		commands.put(getServer().getPluginCommand("whichregion"),new CommandWhichRegion(this));
		commands.put(getServer().getPluginCommand("nameregion"),new CommandNameRegion(this));
		commands.put(getServer().getPluginCommand("addregionpost"),new CommandAddRegionPost(this));
		commands.put(getServer().getPluginCommand("homeregion"),new CommandHomeRegion(this));
		commands.put(getServer().getPluginCommand("cityregion"),new CommandCityRegion(this));
		commands.put(getServer().getPluginCommand("randomregion"),new CommandRandomRegion(this));
		commands.put(getServer().getPluginCommand("invitetoregion"),new CommandInviteToRegion(this));
		commands.put(getServer().getPluginCommand("acceptregioninvite"),new CommandAcceptRegionInvite(this));
		commands.put(getServer().getPluginCommand("movein"),new CommandMoveIn(this));
		commands.put(getServer().getPluginCommand("addregion"),new CommandAddRegion(this));
		commands.put(getServer().getPluginCommand("scanregion"),new CommandScanRegion(this));
		commands.put(getServer().getPluginCommand("listregions"),new CommandListRegions(this));
		commands.put(getServer().getPluginCommand("loginpriority"),new CommandLoginPriority(this));
		
		//register commands
		for (PluginCommand cmd :commands.keySet())
			cmd.setExecutor(commands.get(cmd));
		
		// Register /pd seperately so it doesn't echo out in itself
		getServer().getPluginCommand("populationdensity").setExecutor(new CommandPopulationDensity(this));
		
		//register for events
		PluginManager pluginManager = this.getServer().getPluginManager();
		
		//player events, to control spawn, respawn, disconnect, and region-based notifications as players walk around
		PlayerEventHandler playerEventHandler = new PlayerEventHandler(this.dataStore, this);
		pluginManager.registerEvents(playerEventHandler, this);
				
		//block events, to limit building around region posts and in some other cases (config dependent)
		BlockEventHandler blockEventHandler = new BlockEventHandler();
		pluginManager.registerEvents(blockEventHandler, this);
		
		//entity events, to protect region posts from explosions
		EntityEventHandler entityEventHandler = new EntityEventHandler();
		pluginManager.registerEvents(entityEventHandler, this);
		
		//world events, to generate region posts when chunks load
		WorldEventHandler worldEventHandler = new WorldEventHandler();
		pluginManager.registerEvents(worldEventHandler, this);
		
		//make a note of the spawn world.  may be NULL if the configured city world name doesn't match an existing world
		CityWorld = ConfigData.cityWorld;
		if(CityWorld == null)
			log.log("Invalid City World specified in config. The City World feature will be disabled.");
		
		log.log("Enabled", Log.Level.INFO);
		
		//scan the open region for resources and open a new one as necessary
		//may open and close several regions before finally leaving an "acceptable" region open
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new ScanOpenRegionTask(), 5L, ConfigData.regionScanHours * 60 * 60 * 20L);
		
		// Scan all loaded chunks for entities and compare them to the
		// specified limites in the config.yml. Remove excessive entities
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new EntityScanTask(), 20L, ConfigData.entityScanHours * 60 * 60 * 20L);
	}
	
	public String [] initializeSignContentConfig(FileConfiguration config, String configurationNode, String [] defaultLines) {
		//read what's in the file
		List<String> linesFromConfig = config.getStringList(configurationNode);
		
		//if nothing, replace with default
		int i = 0;
		if(linesFromConfig == null || linesFromConfig.size() == 0)
			for(; i < defaultLines.length && i < 4; i++)
				linesFromConfig.add(defaultLines[i]);
		
		//fill any blanks
		for(i = linesFromConfig.size(); i < 4; i++)
			linesFromConfig.add("");
		
		//write it back to the config file
		config.set(configurationNode, linesFromConfig);
		
		//would the sign be empty?
		boolean emptySign = true;
		for(i = 0; i < 4; i++) {
			if(linesFromConfig.get(i).length() > 0) {
				emptySign = false;
				break;
			}
		}
		
		//return end result
		if(emptySign) {
			return null;
		} else {
			String [] returnArray = new String [4];
			for(i = 0; i < 4 && i < linesFromConfig.size(); i++)
				returnArray[i] = linesFromConfig.get(i);
			return returnArray;		
		}
	}
	
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		log.log("Disabled");
	}
	
	//examines configuration, player permissions, and player location to determine whether or not to allow a teleport
	public boolean playerCanTeleport(Player player, boolean isHomeOrCityTeleport) {
		//if the player has the permission for teleportation, always allow it
		if(perms.has(player, "populationdensity.teleportanywhere"))
			return true;
		
		//if teleportation from anywhere is enabled, always allow it
		if(ConfigData.teleportFromAnywhere)
			return true;
		
		//avoid teleporting from other worlds
		if(!player.getWorld().equals(managedWorld)) {
			player.sendMessage(Colors.ERR + "You can't teleport from here!");
			return false;
		}
		
		//when teleportation isn't allowed, the only exceptions are city to home, and home to city
		if(!ConfigData.allowTeleportation) {
			if(!isHomeOrCityTeleport) {
				player.sendMessage(Colors.ERR + "You're limited to /HomeRegion and /CityRegion here.");
				return false;
			}
			
			//if close to home post, go for it
			PlayerData playerData = this.dataStore.getPlayerData(player);
			Location homeCenter = getRegionCenter(playerData.homeRegion);
			if(homeCenter.distanceSquared(player.getLocation()) < 100)
				return true;
			
			//if city is defined and close to city post, go for it
			if(nearCityPost(player))
				return true;
			
			player.sendMessage(Colors.ERR + "You can't teleport from here!");
			return false;
		} else { //otherwise, any post is acceptable to teleport from or to
			RegionCoordinates currentRegion = RegionCoordinates.fromLocation(player.getLocation());
			Location currentCenter = getRegionCenter(currentRegion);
			if(currentCenter.distanceSquared(player.getLocation()) < 100)
				return true;
			
			if(nearCityPost(player))
				return true;
			
			player.sendMessage(Colors.ERR + "You're not close enough to a region post to teleport.");
			player.sendMessage(Colors.ERR + "On the surface, look for a glowing yellow post on a stone platform.");
			return false;
		}
	}
	
	private boolean nearCityPost(Player player) {
		if(CityWorld != null && player.getWorld().equals(CityWorld)) {
			//max distance == 0 indicates no distance maximum
			return (ConfigData.postTeleportRadius < 1 ||	player.getLocation().distance(CityWorld.getSpawnLocation()) < ConfigData.postTeleportRadius);
		}
		return false;
	}

	/**
	 * Teleports a player to a specific region of the managed world, notifying players of arrival/departure as necessary
	 * @param player The Player to teleport
	 * @param region The RegionCoordinates object which cooresponds to the region to be teleported to
	 * @param silent True will send the player a message that they have been teleported
	 * @see RegionCoordinates
	 */
	//players always land at the region's region post, which is placed on the surface at the center of the region
	public void TeleportPlayer(Player player, RegionCoordinates region, boolean silent) {
		//where specifically to send the player?
		Location teleportDestination = getRegionCenter(region);
		double x = teleportDestination.getBlockX()+0.5;
		double z = teleportDestination.getBlockZ()+2.5;
		
		//make sure the chunk is loaded
		GuaranteeChunkLoaded((int)x, (int)z);
		
		//send him the chunk so his client knows about his destination
		teleportDestination.getWorld().refreshChunk((int)x, (int)z);
		
		//find a safe height, a couple of blocks above the surface		
		Block highestBlock = managedWorld.getHighestBlockAt((int)x, (int)z);
		teleportDestination = new Location(managedWorld, x, highestBlock.getY(), z, -180, 0);		
		
		String regName = Colors.HEAD + "the wilderness";
		if (dataStore.getRegionName(region) != null)
			regName = Colors.HEAD + capitalize(dataStore.getRegionName(region));
		
		//notify him
		if (!silent)
			player.sendMessage(Colors.NORM + "Teleporting you to " + regName);
		
		//send him
		player.teleport(teleportDestination);
	}
	
	//scans the open region for resources and may close the region (and open a new one) if accessible resources are low
	//may repeat itself if the regions it opens are also not acceptably rich in resources
	public void scanRegion(RegionCoordinates region, boolean openNewRegions) {
		log.log("Examining available resources in region \"" + region.toString() + "\"...");
		
		Location regionCenter = getRegionCenter(region);
		int min_x = regionCenter.getBlockX() - REGION_SIZE / 2;
		int max_x = regionCenter.getBlockX() + REGION_SIZE / 2;
		int min_z = regionCenter.getBlockZ() - REGION_SIZE / 2;
		int max_z = regionCenter.getBlockZ() + REGION_SIZE / 2;
		
		Chunk lesserBoundaryChunk = managedWorld.getChunkAt(new Location(managedWorld, min_x, 1, min_z));
		Chunk greaterBoundaryChunk = managedWorld.getChunkAt(new Location(managedWorld, max_x, 1, max_z));
				
		ChunkSnapshot [][] snapshots = new ChunkSnapshot[greaterBoundaryChunk.getX() - lesserBoundaryChunk.getX() + 1][greaterBoundaryChunk.getZ() - lesserBoundaryChunk.getZ() + 1];
		boolean snapshotIncomplete;
		do {
			snapshotIncomplete = false;
		
			for(int x = 0; x < snapshots.length; x++) {
				for(int z = 0; z < snapshots[0].length; z++) {
					//get the chunk, load it, generate it if necessary
					Chunk chunk = managedWorld.getChunkAt(x + lesserBoundaryChunk.getX(), z + lesserBoundaryChunk.getZ());
					while(!chunk.load(true));
					
					//take a snapshot
					ChunkSnapshot snapshot = chunk.getChunkSnapshot();
					
					//verify the snapshot by finding something that's not air
					boolean foundNonAir = false;
					for(int y = 0; y < managedWorld.getMaxHeight(); y++) {
						//if we find something, save the snapshot to the snapshot array
						if(snapshot.getBlockTypeId(0, y, 0) != Material.AIR.getId()) {
							foundNonAir = true;
							snapshots[x][z] = snapshot;
							break;
						}
					}
					
					//otherwise, plan to repeat this process again after sleeping a bit
					if(!foundNonAir)
						snapshotIncomplete = true;
				}
			}
			
			//if at least one snapshot was all air, sleep a second to let the chunk loader/generator
			//catch up, and then try again
			if(snapshotIncomplete) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) { } 				
			}
			
		} while(snapshotIncomplete);
		
		//try to unload any chunks which don't have players nearby
		Chunk [] loadedChunks = managedWorld.getLoadedChunks();
		for(int i = 0; i < loadedChunks.length; i++)
			loadedChunks[i].unload(true, true);  //save = true, safe = true
		
		//collect garbage
		System.gc();
		
		//create a new task with this information, which will more completely scan the content of all the snapshots
		ScanRegionTask task = new ScanRegionTask(snapshots, openNewRegions);
		
		//run it in a separate thread		
		this.getServer().getScheduler().runTaskLaterAsynchronously(this, task, 5L);		
	}
	
	//ensures a piece of the managed world is loaded into server memory
	//(generates the chunk if necessary)
	//these coordinate params are BLOCK coordinates, not CHUNK coordinates
	public static void GuaranteeChunkLoaded(int x, int z) {
		Location location = new Location(managedWorld, x, 5, z);
		Chunk chunk = managedWorld.getChunkAt(location);
		while(!chunk.isLoaded() || !chunk.load(true));
	}
	
	//determines the center of a region (as a Location) given its region coordinates
	//keeping all regions the same size and aligning them in a grid keeps this calculation simple and fast
	public static Location getRegionCenter(RegionCoordinates region) {
		World w = managedWorld;
		int x, z;
		if(region.x >= 0)
			x = region.x * REGION_SIZE + REGION_SIZE / 2;
		else
			x = region.x * REGION_SIZE + REGION_SIZE / 2;
		
		if(region.z >= 0)
			z = region.z * REGION_SIZE + REGION_SIZE / 2;
		else
			z = region.z * REGION_SIZE + REGION_SIZE / 2;
		
		Location center = new Location(w, x, 1, z);
				
		//PopulationDensity.GuaranteeChunkLoaded(ManagedWorld.getChunkAt(center).getX(), ManagedWorld.getChunkAt(center).getZ());		
		center = w.getHighestBlockAt(center).getLocation();
		
		return center;
	}
	
	//capitalizes a string, used to make region names pretty
	public static String capitalize(String string) {
		if(string == null || string.length() == 0) 
			return string;
		
		if(string.length() == 1)
			return string.toUpperCase();
		
		return string.substring(0, 1).toUpperCase() + string.substring(1);    
	}

	public void resetIdleTimer(Player player) {
		//if idle kick is disabled, don't do anything here
		if(ConfigData.maxIdleMinutes < 1)
			return;
		
		PlayerData playerData = this.dataStore.getPlayerData(player);
		
		//if there's a task already in the queue for this player, cancel it
		if(playerData.afkCheckTaskID >= 0)
			PopulationDensity.instance.getServer().getScheduler().cancelTask(playerData.afkCheckTaskID);
		
		//queue a new task for later
		//note: 20L ~ 1 second
		playerData.afkCheckTaskID = PopulationDensity.instance.getServer().getScheduler().scheduleSyncDelayedTask(PopulationDensity.instance, new AfkCheckTask(player, playerData), 20L * 60 * ConfigData.maxIdleMinutes);
	}
	
	public OfflinePlayer resolvePlayer(String name) {
		Player player = this.getServer().getPlayer(name);
		if(player != null) return player;
		
		OfflinePlayer [] offlinePlayers = this.getServer().getOfflinePlayers();
		for(int i = 0; i < offlinePlayers.length; i++)
			if(offlinePlayers[i].getName().equalsIgnoreCase(name))
				return offlinePlayers[i];
		
		return null;
	}
	
	public static void sendMessage(Player player, String message) {
		if(player != null)
			player.sendMessage(message);
		else
			PopulationDensity.instance.log.log(message);
	}
	
	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> pp = getServer().getServicesManager().getRegistration(Permission.class);
		if (pp != null)
			perms = pp.getProvider();
		return perms != null;
	}
}