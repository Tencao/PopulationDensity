/*
    PopulationDensity Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Log.Level;
import me.ryanhamshire.PopulationDensity.utils.NameData;
import me.ryanhamshire.PopulationDensity.utils.PlayerData;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class DataStore {
	
	//used in the spiraling code below (see findNextRegion())
	private enum Direction {
		LEFT, RIGHT, UP, DOWN;
	}
	
	//in-memory cache for player home region, because it's needed very frequently
	//writes here also write immediately to file
	private HashMap<String, PlayerData> playerNameToPlayerDataMap = new HashMap<String, PlayerData>();
	
	//randomly ordered number map to mix up the region name selection a bit.
	//pretty much so that the names can still detect when it needs to append numbers
	private List<Integer> randNumList = new ArrayList<Integer>();
	
	//path information, for where stuff stored on disk is well...  stored
	private final static String dataLayerFolderPath = "plugins" + File.separator + "PopulationDensityData";
	private final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
	private final static String regionDataFolderPath = dataLayerFolderPath + File.separator + "RegionData";
	public final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
	public final static String namesFilePath = dataLayerFolderPath + File.separator + "regionNames.yml";
	
	//currently open region
	private RegionCoordinates openRegionCoordinates;
	
	//coordinates of the next region which will be opened, if one needs to be opened
	private RegionCoordinates nextRegionCoordinates;
	
	//total number of regions
	private int regionCount;
	
	//initialization!
	/**
	 * Initializes the DataStore object.
	 * This object includes the GriefPrevention API and cached data
	 */
	public DataStore() {
		//ensure data folders exist
		new File(playerDataFolderPath).mkdirs();
		new File(regionDataFolderPath).mkdirs();
		
		//fetch new set of random numbers for randNumList
		int len = NameData.regionNames.size();
		for (int i = 0; i < len; i++)
			randNumList.add(new Integer(i));
		Collections.shuffle(randNumList);
		
		//study region data and initialize both this.openRegionCoordinates and this.nextRegionCoordinates
		this.regionCount = this.findNextRegion();
		
		//if no regions were loaded, create the first one
		if(regionCount == 0) {
			PopulationDensity.instance.log.log("Please be patient while I search for a good new player starting point!", Level.INFO);
			PopulationDensity.instance.log.log("This initial scan could take a while, especially for worlds where players have already been building.", Level.INFO);
			this.addRegion();			
		}
		
		PopulationDensity.instance.log.log("Open region: \"" + this.getRegionName(this.getOpenRegion()) + "\" at " + this.getOpenRegion().toString() + ".", Level.INFO);
	}	
	
	//sets private variables for openRegion and nextRegion when it's done
	//this may look like black magic, but seriously, it produces a tight spiral on a grid
	//coding this made me reminisce about seemingly pointless computer science exercises in college
	/**
	 * Starting at region 0,0, spirals outward until an uninitialized region is found
	 * @return The number of regions iterated through, including the new region (starts from 0)
	 */
	public int findNextRegion() {
		//spiral out from region coordinates 0, 0 until we find coordinates for an uninitialized region
		int x = 0; int z = 0;
		
		//keep count of the regions encountered
		int regionCount = 0;

		//initialization
		Direction direction = Direction.DOWN;   //direction to search
		int sideLength = 1;  					//maximum number of regions to move in this direction before changing directions
		int side = 0;        					//increments each time we change directions.  this tells us when to add length to each side
		this.openRegionCoordinates = null;
		this.nextRegionCoordinates = new RegionCoordinates(0, 0);

		//while the next region coordinates are taken, walk the spiral
		while ((this.getRegionName(this.nextRegionCoordinates)) != null) {
			//loop for one side of the spiral
			for (int i = 0; i < sideLength && this.getRegionName(this.nextRegionCoordinates) != null; i++) {
				regionCount++;
				
				//converts a direction to a change in X or Z
				if (direction == Direction.DOWN)
					z++;
				else if (direction == Direction.LEFT)
					x--;
				else if (direction == Direction.UP)
					z--;
				else
					x++;
				
				this.openRegionCoordinates = this.nextRegionCoordinates;
				this.nextRegionCoordinates = new RegionCoordinates(x, z);
			}
		
			//after finishing a side, change directions
			if (direction == Direction.DOWN)
				direction = Direction.LEFT;
			else if (direction == Direction.LEFT)
				direction = Direction.UP;
			else if (direction == Direction.UP)
				direction = Direction.RIGHT;
			else
				direction = Direction.DOWN;
			
			//keep count of the completed sides
			side++;

			//on even-numbered sides starting with side == 2, increase the length of each side
			if (side % 2 == 0) sideLength++;
		}
		//return total number of regions seen
		return regionCount;
	}
	
	/**
	 * Picks a region at random (sort of)
	 * @param regionToAvoid Regions to exlude from potential region returns
	 * @return A random region
	 * @see RegionCoordinates
	 */
	public RegionCoordinates getRandomRegion(RegionCoordinates regionToAvoid) {
		if(this.regionCount < 2)
			return null;
		
		//initialize random number generator with a seed based the current time
		Random randomGenerator = new Random();
		
		//get a list of all the files in the region data folder
		//some of them are named after region names, others region coordinates
		File regionDataFolder = new File(regionDataFolderPath);
		File [] files = regionDataFolder.listFiles();			
		ArrayList<RegionCoordinates> regions = new ArrayList<RegionCoordinates>();
		
		for(int i = 0; i < files.length; i++) {				
			if(files[i].isFile()) { //avoid any folders
				try {
					//if the filename converts to region coordinates, add that region to the list of defined regions
					//(this constructor throws an exception if it can't do the conversion)
					RegionCoordinates regionCoordinates = new RegionCoordinates(files[i].getName());
					if(!regionCoordinates.equals(regionToAvoid))
						regions.add(regionCoordinates);
				} catch(Exception e) { /*catch for files named after region names*/ }					
			}
		}
		
		//pick one of those regions at random
		int randomRegion = randomGenerator.nextInt(regions.size());			
		return regions.get(randomRegion);			
	}
	
	/**
	 * Writes the player's PlayerData object to file
	 * @param player The player whose data to save
	 * @param data The PlayerData object to write to file
	 * @see PlayerData
	 */
	public void savePlayerData(OfflinePlayer player, PlayerData data) {
		//save that data in memory
		this.playerNameToPlayerDataMap.put(player.getName(), data);
		
		BufferedWriter outStream = null;
		try {
			//open the player's file
			File playerFile = new File(playerDataFolderPath + File.separator + player.getName());
			playerFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(playerFile));
			
			//first line is home region coordinates
			outStream.write(data.homeRegion.toString());
			outStream.newLine();
			
			//second line is last disconnection date,
			//note use of the ROOT locale to avoid problems related to regional settings on the server being updated
			DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ROOT);			
			outStream.write(dateFormat.format(data.lastDisconnect));
			outStream.newLine();
			
			//third line is login priority
			outStream.write(String.valueOf(data.loginPriority));
			outStream.newLine();
		} catch(Exception e) {
			//if any problem, log it
			PopulationDensity.instance.log.log("Unexpected exception saving data for player \"" + player.getName() + "\": " + e.getMessage(), Level.WARN);
		}		
		
		try {
			//close the file
			if(outStream != null)
				outStream.close();
		} catch(IOException exception) {}
	}
	
	/**
	 * Fetches the PlayerData object cached for a given user.
	 * This method will attempt to load the player's data if not already cached.
	 * @param player The player whose data to fetch from the data cache
	 * @return The PlayerData object which corresponds to the provided player, null if not found
	 * @see PlayerData
	 */
	public PlayerData getPlayerData(OfflinePlayer player) {
		//first, check the in-memory cache
		PlayerData data = this.playerNameToPlayerDataMap.get(player.getName());
		
		if(data != null)
			return data;
		
		//if not there, try to load the player from file		
		loadPlayerDataFromFile(player.getName());
		
		//check again
		data = this.playerNameToPlayerDataMap.get(player.getName());
		
		if(data != null) return data;
		
		return new PlayerData();
	}
	
	/**
	 * Loads a players data from file, stores it in a PlayerData object and adds it to the cache data list
	 * @param playerName The player whose data is to be loaded
	 * @see PlayerData
	 */
	private void loadPlayerDataFromFile(String playerName) {
		//load player data into memory		
		File playerFile = new File(playerDataFolderPath + File.separator + playerName);
		
		BufferedReader inStream = null;
		try {					
			PlayerData playerData = new PlayerData();
			inStream = new BufferedReader(new FileReader(playerFile.getAbsolutePath()));
						
			//first line is home region coordinates
			String homeRegionCoordinatesString = inStream.readLine();
			
			//second line is date of last disconnection
			String lastDisconnectedString = inStream.readLine();
			
			//third line is login priority
			String rankString = inStream.readLine(); 
			
			//convert string representation of home coordinates to a proper object
			RegionCoordinates homeRegionCoordinates = new RegionCoordinates(homeRegionCoordinatesString);
			playerData.homeRegion = homeRegionCoordinates;
			  
			//parse the last disconnect date string
			try {
				DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ROOT);
				Date lastDisconnect = dateFormat.parse(lastDisconnectedString);
				playerData.lastDisconnect = lastDisconnect;
			} catch(Exception e) {
				playerData.lastDisconnect = Calendar.getInstance().getTime();
			}
			
			//parse priority string
			if(rankString == null || rankString.isEmpty())
				playerData.loginPriority = 0;
			else {
				try {
					playerData.loginPriority = Integer.parseInt(rankString);
				} catch(Exception e) {
					playerData.loginPriority = 0;
				}			
			}
			  
			//shove into memory for quick access
			this.playerNameToPlayerDataMap.put(playerName, playerData);
		} catch(FileNotFoundException e) {
			//if the file isn't found, just don't do anything (probably a new-to-server player)
			return;
		} catch(Exception e) {
			//if there's any problem with the file's content, log an error message and skip it
			 PopulationDensity.instance.log.log("Unable to load data for player \"" + playerName + "\": " + e.getMessage(), Level.WARN);			 
		}
		
		try {
			if(inStream != null) inStream.close();
		} catch(IOException exception) {}		
	}
	
	/**
	 * adds a new region, assigning it a name and updating local variables accordingly
	 * @return RegionCoordinates object associated with the new region
	 * @see RegionCoordinates
	 */
	public RegionCoordinates addRegion() {
		//first, find a unique name for the new region
		String newRegionName; 
		
		//select a name from the list of region names		
		//strategy: use names from the list in rotation, appending a number when a name is already used
		//(redstone, mountain, valley, redstone1, mountain1, valley1, ...)
		
		int newRegionNumber = this.regionCount++ - 1;
		
		//as long as the generated name is already in use, move up one name on the list
		//just added a shuffled number list to act as a randomizer of sorts
		do {
			newRegionNumber++;
			int randIndex = newRegionNumber % NameData.regionNames.size();
			int nameSuffix = newRegionNumber / NameData.regionNames.size();
			// instead of fetching the next name, it gets the next random number
			// in randNumList, which semi-randomly gets a new name from names list
			// randNumList is randomized on each instance of DataStore, now the next
			// name will be a surprise! :D
			newRegionName = NameData.regionNames.get(randNumList.get(randIndex));
			if(nameSuffix > 0)
				newRegionName += nameSuffix;
		} while (this.getRegionCoordinates(newRegionName) != null);
		
		/*do {
			newRegionNumber++;
			int nameBodyIndex = newRegionNumber % this.regionNamesList.length;
			int nameSuffix = newRegionNumber / this.regionNamesList.length;
			newRegionName = this.regionNamesList[nameBodyIndex];
			if(nameSuffix > 0)
				newRegionName += nameSuffix;
		}while(this.getRegionCoordinates(newRegionName) != null);*/
		
		this.nameRegion(this.nextRegionCoordinates, newRegionName);		
		
		//find the next region in the spiral (updates this.openRegionCoordinates and this.nextRegionCoordinates)
		this.findNextRegion();
		
		return this.openRegionCoordinates;
	}
	
	/**
	 * Names a region. Also deletes and re-writes region data to disk.
	 * @param coords The RegionCoordinates object that corresponds to the desired region
	 * @param name The name of the region
	 * @see RegionCoordinates
	 */
	public void nameRegion(RegionCoordinates coords, String name) {
		//region names are always lowercase
		name = name.toLowerCase();
		
		//delete any existing data for the region at these coordinates
		String oldRegionName = this.getRegionName(coords);
		if(oldRegionName != null) {
			File oldRegionCoordinatesFile = new File(regionDataFolderPath + File.separator + coords.toString());
			oldRegionCoordinatesFile.delete();
			
			File oldRegionNameFile = new File(regionDataFolderPath + File.separator + oldRegionName);
			oldRegionNameFile.delete();
		}

		//"create" the region by saving necessary data to disk
		//(region names to coordinates mappings aren't kept in memory because they're less often needed, and this way we keep it simple) 
		BufferedWriter outStream = null;
		try {
			//coordinates file contains the region's name
			File regionNameFile = new File(regionDataFolderPath + File.separator + name);
			regionNameFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(regionNameFile));
			outStream.write(coords.toString());
			outStream.close();
			
			//name file contains the coordinates
			File regionCoordinatesFile = new File(regionDataFolderPath + File.separator + coords.toString());
			regionCoordinatesFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(regionCoordinatesFile));
			outStream.write(name);
			outStream.close();			
		} catch(Exception e) {
			//in case of any problem, log the details
			PopulationDensity.instance.log.log("Unexpected Exception: " + e.getMessage(), Level.WARN);
		}
		
		try {
			if(outStream != null)
				outStream.close();		
		} catch(IOException exception) {}
	}

	/**
	 * Retrieves the open region's coordinates
	 * @return RegionCoordinates object associated with the current region
	 * @see RegionCoordinates
	 */
	public RegionCoordinates getOpenRegion() {
		return this.openRegionCoordinates;
	}
	
	/**
	 * Goes to disk to get the name of a region, given its coordinates
	 * @param coordinates Coordinates of the region to search for
	 * @return The name of the region which exists at the given coordinates
	 */
	public String getRegionName(RegionCoordinates coordinates) {
		File regionCoordinatesFile;
		
		BufferedReader inStream = null;
		String regionName = null;
		try {
			regionCoordinatesFile = new File(regionDataFolderPath + File.separator + coordinates.toString());			
			inStream = new BufferedReader(new FileReader(regionCoordinatesFile));
			
			//only one line in the file, the region name
			regionName = inStream.readLine();
		} catch(FileNotFoundException e) {
			//if the file doesn't exist, the region hasn't been named yet, so return null
			return null;
		} catch(Exception e) {
			//if any other problems, log the details
			PopulationDensity.instance.log.log("Unable to read region data: " + e.getMessage(), Level.WARN);
			return null;
		}
		
		try {
			if(inStream != null) inStream.close();
		} catch(IOException exception) {}
		
		return regionName;
	}
	
	/**
	 * Goes to disk to get the coordinates that go with a region name
	 * @param regionName The name of the region to search for
	 * @return The RegionCoordinates object associated with the found region, null if not found
	 * @see RegionCoordinates
	 */
	// TODO Condense region files into a YML file
	
	public RegionCoordinates getRegionCoordinates(String regionName) {
		File regionNameFile = new File(regionDataFolderPath + File.separator + regionName.toLowerCase());
		
		BufferedReader inStream = null;
		RegionCoordinates coordinates = null;
		try {
			inStream = new BufferedReader(new FileReader(regionNameFile));
			
			//only one line in the file, the coordinates
			String coordinatesString = inStream.readLine();
			
			inStream.close();			
			coordinates = new RegionCoordinates(coordinatesString);
		} catch(FileNotFoundException e) {
			//file not found means there's no region with a matching name, so return null
		} catch(Exception e) {
			//if any other problems, log the details and then return null
			PopulationDensity.instance.log.log("Unable to read region data at " + regionNameFile.getAbsolutePath() + ": " + e.getMessage(), Level.WARN);			
		}
		
		try {
			if(inStream != null)
				inStream.close();
		} catch(IOException exception) {}
		
		return coordinates;
	}
	
	/**
	 * Edits the world to create a region post at the center of the specified region	
	 * @param region The RegionCoordinates object associated with the region to create the post for
	 * @param updateNeighboringRegions Whether or not to update neighboring regions
	 * @see RegionCoordinates
	 */
	public void AddRegionPost(RegionCoordinates region, boolean updateNeighboringRegions) {
		int minX = 0, minZ = 0, maxX = 0, maxZ = 0;
		//if region post building is disabled, don't do anything
		if(!ConfigData.buildRegionPosts)
			return;
		
		//find the center
		Location regionCenter = PopulationDensity.getRegionCenter(region);
		int x = regionCenter.getBlockX();
		int z = regionCenter.getBlockZ();
		int y;
		
		minX = x-ConfigData.postProtectionRadius;
		maxX = x+ConfigData.postProtectionRadius;
		minZ = z-ConfigData.postProtectionRadius;
		maxZ = z+ConfigData.postProtectionRadius;
		
		boolean claimable = true;
		boolean guardable = true;

		if (ConfigData.getGPData() != null && ConfigData.claimPosts) {
			for (int ix = minX; claimable && ix < maxX; ix++) {
				for (int iz = minZ; claimable && iz < maxZ; iz++) {
					Location loc = new Location(PopulationDensity.managedWorld, ix, 64, iz);
					Claim claim = ConfigData.getGPData().getClaimAt(loc, true, null);
					if (claim != null)
						claimable = false;
				}
			}
		} else {
			claimable = false;
		}
		
		if (!claimable && ConfigData.getWGData() != null && ConfigData.guardPosts) {
			RegionManager rm = ConfigData.getWGData().getRegionManager(PopulationDensity.managedWorld);
			for (int ix = minX; guardable && ix < maxX; ix++) {
				for (int iy = 0; guardable && iy < 255; iy++) {
					for (int iz = minZ; guardable && iz < maxZ; iz++) {
						Location loc = new Location(PopulationDensity.managedWorld, ix, iy, iz);
						ApplicableRegionSet regions = rm.getApplicableRegions(loc);
						if (regions.size() > 0) {
							guardable = false;
						}
					}
				}
			}
		} else {
			guardable = false;
		}

		//make sure data is loaded for that area, because we're about to request data about specific blocks there
		PopulationDensity.GuaranteeChunkLoaded(x, z);
		
		//sink lower until we find something solid
		//also ignore glowstone, in case there's already a post here!
		//race condition issue: chunks say they're loaded when they're not.  if it looks like the chunk isn't loaded, try again (up to five times)
		int retriesLeft = 5;
		boolean tryAgain;
		do {
			tryAgain = false;
			
			//find the highest block.  could be the surface, a tree, some grass...
			y = PopulationDensity.managedWorld.getHighestBlockYAt(x, z) + 1;
			
			//posts fall through trees, snow, and any existing post looking for the ground
			Material blockType;
			do {
				blockType = PopulationDensity.managedWorld.getBlockAt(x, --y, z).getType();
			} while(y > 2 && canFall(blockType));
			
			//if final y value is extremely small, it's probably wrong
			if(y < 5 && retriesLeft-- > 0) {
				tryAgain = true;
				try {
					Thread.sleep(500); //sleep half a second before restarting the loop
				} catch(InterruptedException e) {}
			}
		} while(tryAgain);
				
		//if y value is under sea level, correct it to sea level (no posts should be that difficult to find)
		if(y < ConfigData.minimumRegionPostY)
			y = ConfigData.minimumRegionPostY;
		
		//clear signs from the area, this ensures signs don't drop as items 
		//when the blocks they're attached to are destroyed in the next step
		for(int x1 = x - 2; x1 <= x + 2; x1++) {
			for(int z1 = z - 2; z1 <= z + 2; z1++) {
				for(int y1 = y + 1; y1 <= y + 15; y1++) {
					Block block = PopulationDensity.managedWorld.getBlockAt(x1, y1, z1);
					if(block.getType() == Material.SIGN_POST || block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN)
						block.setType(Material.AIR);					
				}
			}
		}
		
		//clear above it - sometimes this shears trees in half (doh!)
		for(int x1 = x - 2; x1 <= x + 2; x1++)
			for(int z1 = z - 2; z1 <= z + 2; z1++)
				for(int y1 = y + 1; y1 < PopulationDensity.managedWorld.getMaxHeight(); y1++)
					PopulationDensity.managedWorld.getBlockAt(x1, y1, z1).setType(Material.AIR);
		
		
		//build a glowpost in the center
		for(int y1 = y; y1 <= y + 3; y1++)
			PopulationDensity.managedWorld.getBlockAt(x, y1, z).setType(Material.GLOWSTONE);
		
		//build a stone platform
		for(int x1 = x - 2; x1 <= x + 2; x1++)
			for(int z1 = z - 2; z1 <= z + 2; z1++)
				PopulationDensity.managedWorld.getBlockAt(x1, y, z1).setType(Material.SMOOTH_BRICK);
		
		//if the region has a name, build a sign on top
		String regionName = this.getRegionName(region);
		if(regionName != null) {		
			regionName = PopulationDensity.capitalize(regionName);
			Block block = PopulationDensity.managedWorld.getBlockAt(x, y + 4, z);
			block.setType(Material.SIGN_POST);
			
			org.bukkit.block.Sign sign = (org.bukkit.block.Sign)block.getState();
			sign.setLine(1, "You are in:");
			sign.setLine(2, "\u00A71" + PopulationDensity.capitalize(regionName));
			sign.update();
		}
		
		//add a sign for the region to the south
		regionName = this.getRegionName(new RegionCoordinates(region.x + 1, region.z));
		if(regionName == null) regionName = "\u00A74Wilderness";
		regionName = "\u00A71" + PopulationDensity.capitalize(regionName);
		
		Block block = PopulationDensity.managedWorld.getBlockAt(x, y + 2, z - 1);
		
		org.bukkit.material.Sign signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
		signData.setFacingDirection(BlockFace.NORTH);
		
		block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
		
		org.bukkit.block.Sign sign = (org.bukkit.block.Sign)block.getState();
		
		sign.setLine(0, "E");
		sign.setLine(1, "<--");
		sign.setLine(2, regionName);
		
		sign.update();
		
		//if a city world is defined, also add a /cityregion sign on the east side of the post
		if(PopulationDensity.CityWorld != null) {
			block = PopulationDensity.managedWorld.getBlockAt(x, y + 3, z - 1);
			
			signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
			signData.setFacingDirection(BlockFace.NORTH);
			
			block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
			
			sign = (org.bukkit.block.Sign)block.getState();
			
			sign.setLine(0, "Visit the City:");
			sign.setLine(1, "/CityRegion");
			sign.setLine(2, "Return Home:");
			sign.setLine(3, "/HomeRegion");
			
			sign.update();
		}
		
		//add a sign for the region to the east
		regionName = this.getRegionName(new RegionCoordinates(region.x, region.z - 1));
		if(regionName == null) regionName = "\u00A74Wilderness";
		regionName = "\u00A71" + PopulationDensity.capitalize(regionName);
		
		block = PopulationDensity.managedWorld.getBlockAt(x - 1, y + 2, z);
		
		signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
		signData.setFacingDirection(BlockFace.WEST);
		
		block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
		
		sign = (org.bukkit.block.Sign)block.getState();
		
		sign.setLine(0, "N");
		sign.setLine(1, "<--");
		sign.setLine(2, regionName);
		
		sign.update();
		
		//if teleportation is enabled, also add a sign facing north for /visitregion and /invitetoregion
		if(ConfigData.allowTeleportation) {
			block = PopulationDensity.managedWorld.getBlockAt(x - 1, y + 3, z);
			
			signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
			signData.setFacingDirection(BlockFace.WEST);
			
			block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
			
			sign = (org.bukkit.block.Sign)block.getState();
			
			sign.setLine(0, "Visit Friends:");
			sign.setLine(1, "/VisitRegion");
			sign.setLine(2, "Invite Friends:");
			sign.setLine(3, "/InviteToRegion");
			
			sign.update();
		}
		
		//add a sign for the region to the south
		regionName = this.getRegionName(new RegionCoordinates(region.x, region.z + 1));
		if(regionName == null) regionName = "\u00A74Wilderness";
		regionName = "\u00A71" + PopulationDensity.capitalize(regionName);
		
		block = PopulationDensity.managedWorld.getBlockAt(x + 1, y + 2, z);
		
		signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
		signData.setFacingDirection(BlockFace.EAST);
		
		block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
		
		sign = (org.bukkit.block.Sign)block.getState();
		
		sign.setLine(0, "S");
		sign.setLine(1, "<--");
		sign.setLine(2, regionName);
		
		sign.update();
		
		//if teleportation is enabled, also add a sign facing south for /homeregion
		if(ConfigData.allowTeleportation) {
			block = PopulationDensity.managedWorld.getBlockAt(x + 1, y + 3, z);
			signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
			signData.setFacingDirection(BlockFace.EAST);
			
			block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
			
			sign = (org.bukkit.block.Sign)block.getState();
			
			sign.setLine(0, "Set Your Home:");
			sign.setLine(1, "/MoveIn");
			sign.setLine(2, "Return Home:");
			sign.setLine(3, "/HomeRegion");
			
			sign.update();
		}
		
		//add a sign for the region to the north
		regionName = this.getRegionName(new RegionCoordinates(region.x - 1, region.z));
		if(regionName == null) regionName = "\u00A74Wilderness";
		regionName = "\u00A71" + PopulationDensity.capitalize(regionName);
		
		block = PopulationDensity.managedWorld.getBlockAt(x, y + 2, z + 1);
		
		signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
		signData.setFacingDirection(BlockFace.SOUTH);
		
		block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
		
		sign = (org.bukkit.block.Sign)block.getState();
		
		sign.setLine(0, "W");
		sign.setLine(1, "<--");
		sign.setLine(2, regionName);
		
		sign.update();
		
		//if teleportation is enabled, also add a sign facing west for /newestregion and /randomregion
		if(ConfigData.allowTeleportation) {
			block = PopulationDensity.managedWorld.getBlockAt(x, y + 3, z + 1);

			signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
			signData.setFacingDirection(BlockFace.SOUTH);
			
			block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);

			sign = (org.bukkit.block.Sign)block.getState();
			
			sign.setLine(0, "Adventure!");
			sign.setLine(2, "/RandomRegion");
			sign.setLine(3, "/NewestRegion");
			
			sign.update();
		}
		
		//custom signs
		
		if(ConfigData.mainCustomSignContent != null) {
			block = PopulationDensity.managedWorld.getBlockAt(x, y + 3, z - 1);

			signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
			signData.setFacingDirection(BlockFace.NORTH);
			
			block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
			
			sign = (org.bukkit.block.Sign)block.getState();
			
			for(int i = 0; i < 4; i++)
				sign.setLine(i, ConfigData.mainCustomSignContent[i]);
			
			sign.update();
		}
		
		// 62 lines widdled down to 36 lines!
		HashMap<BlockFace, String[]> content = new HashMap<BlockFace, String[]>();
		content.put(BlockFace.WEST, ConfigData.northCustomSignContent);
		content.put(BlockFace.EAST, ConfigData.southCustomSignContent);
		content.put(BlockFace.NORTH, ConfigData.eastCustomSignContent);
		content.put(BlockFace.SOUTH, ConfigData.westCustomSignContent);
		
		for (BlockFace face : content.keySet()) {
			if (content.get(face) != null) {
				switch (face) {
					case WEST:
						block = PopulationDensity.managedWorld.getBlockAt(x - 1, y + 1, z);
						break;
					case EAST:
						block = PopulationDensity.managedWorld.getBlockAt(x + 1, y + 1, z);
						break;
					case NORTH:
						block = PopulationDensity.managedWorld.getBlockAt(x, y + 1, z - 1);
						break;
					case SOUTH:
						block = PopulationDensity.managedWorld.getBlockAt(x, y + 1, z + 1);
						break;
					default:
						break;
				}
				signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
				signData.setFacingDirection(face);
				
				block.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);
				
				sign = (org.bukkit.block.Sign)block.getState();
				
				for(int i = 0; i < 4; i++)
					sign.setLine(i, content.get(face)[i]);
				
				sign.update();
			}
		}
		
		// TODO Auto claim support for GriefPrevention and WorldGuard
		if (claimable) {
			me.ryanhamshire.GriefPrevention.DataStore gp = ConfigData.getGPData();
			Long id = 0L;
			Long i = 0L;
			boolean loop = true;
			List<Long> idList = Arrays.asList(gp.getClaimIds());
			while (loop && i <= idList.size()) {
				if (!idList.contains(id)) {
					id = i;
					loop = false;;
				} else
					i++;
			}
			gp.createClaim(ConfigData.managedWorld, minX, maxX, 0, 255, minZ, maxZ, "", null, id, true);
		}
		
		if(updateNeighboringRegions) {
			this.AddRegionPost(new RegionCoordinates(region.x - 1, region.z), false);
			this.AddRegionPost(new RegionCoordinates(region.x + 1, region.z), false);
			this.AddRegionPost(new RegionCoordinates(region.x, region.z - 1), false);
			this.AddRegionPost(new RegionCoordinates(region.x, region.z + 1), false);
		}
	}
	
	private boolean canFall(Material mat) {
		return (
				!mat.isSolid() ||
				mat.equals(Material.SIGN) ||
				mat.equals(Material.SIGN_POST) ||
				mat.equals(Material.WALL_SIGN) ||
				mat.equals(Material.LEAVES) || 
				mat.equals(Material.LOG) ||
				mat.equals(Material.GLOWSTONE)
		);
	}
	
	/**
	 * Removes cached data from the player data list
	 * @param player The player whose data is to be cleared
	 */
	public void clearCachedPlayerData(Player player) {
		this.playerNameToPlayerDataMap.remove(player.getName());		
	}
}
