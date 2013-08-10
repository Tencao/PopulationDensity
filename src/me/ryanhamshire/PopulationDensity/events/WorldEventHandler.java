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

package me.ryanhamshire.PopulationDensity.events;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.tasks.AddRegionPostTask;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class WorldEventHandler implements Listener {
	//when a chunk loads, generate a region post in that chunk if necessary
	@EventHandler(ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent chunkLoadEvent) {		
		Chunk chunk = chunkLoadEvent.getChunk();
		
		//nothing to do in worlds other than the managed world
		if(chunk.getWorld() != PopulationDensity.managedWorld)
			return;
		
		//find the boundaries of the chunk
		Location lesserCorner = chunk.getBlock(0, 0, 0).getLocation();
		Location greaterCorner = chunk.getBlock(15, 0, 15).getLocation();
		
		//find the center of this chunk's region
		RegionCoordinates region = RegionCoordinates.fromLocation(lesserCorner);		
		Location regionCenter = PopulationDensity.getRegionCenter(region);
		
		//if the chunk contains the region center
		if(hasCenter(regionCenter, lesserCorner, greaterCorner)) {
			//create a task to build the post after 10 seconds
			AddRegionPostTask task = new AddRegionPostTask(region, false);
			
			//run it in a separate thread		
			PopulationDensity.instance.getServer().getScheduler().scheduleSyncDelayedTask(PopulationDensity.instance, task, 20L * 10);			
		}
	}
	
	private boolean hasCenter(Location regionCenter, Location lesserCorner, Location greaterCorner) {
		return (
			regionCenter.getBlockX() >= lesserCorner.getBlockX() &&
			regionCenter.getBlockX() <= greaterCorner.getBlockX() &&
			regionCenter.getBlockZ() >= lesserCorner.getBlockZ() &&
			regionCenter.getBlockZ() <= greaterCorner.getBlockZ()
		);
	}
}