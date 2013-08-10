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
import me.ryanhamshire.PopulationDensity.utils.ConfigData;
import me.ryanhamshire.PopulationDensity.utils.Messages.Colors;
import me.ryanhamshire.PopulationDensity.utils.PlayerData;
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

public class BlockEventHandler implements Listener  {
	//when a player breaks a block...
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		Player player = e.getPlayer();
		
		PopulationDensity.instance.resetIdleTimer(player);
		
		Block block = e.getBlock();

		if (!eventHandle(block, player))
			e.setCancelled(true);
	}
	
	//COPY PASTE!  this is practically the same as the above block break handler
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		
		PopulationDensity.instance.resetIdleTimer(player);
		
		Block block = e.getBlock();

		if (!eventHandle(block, player))
			e.setCancelled(true);
	}
	
	//when a sign is created...
	@EventHandler(ignoreCancelled=true)
	public void onSignChange(SignChangeEvent e) {
		if (PopulationDensity.instance.perms.has(e.getPlayer(), "populationdensity.createteleportsign")) {
			String[] lines = e.getLines();
			if (lines[0].equalsIgnoreCase("[region]") || lines[0].equalsIgnoreCase("[ region ]")) {
				if (lines[1] != "" && lines[1] != null) {
					RegionCoordinates region = PopulationDensity.instance.dataStore.getRegionCoordinates(lines[1]);
					if (region != null) {
						String regName = PopulationDensity.capitalize(PopulationDensity.instance.dataStore.getRegionName(region));
						e.getPlayer().sendMessage(Colors.NOTE + "Sign linked to " + Colors.HEAD + regName);
						// Last two lines are just a password of sorts to somewhat
						// prevent players with colored sign perms to create a
						// teleport sign without permission. (I don't want to register
						// signs in a flat file or anything is all. Also the bold
						// at the end of the [ Regoin ]  \/
						e.setLine(0, "\u00A71[ Region ]\u00A7l");
						e.setLine(1, "\u00A74" + regName);
						//               C     6     E     A     B     1     5      :D
						e.setLine(2, "\u00A7c\u00A76\u00A7e\u00A7a\u00A7b\u00A71\u00A75");
						
						// Less likely to have format permissions, so lets use those too
						//               L     R     O    L      N     M     K
						e.setLine(3, "\u00A7l\u00A7r\u00A7o\u00A7l\u00A7n\u00A7m\u00A7k");
					} else {
						e.getPlayer().sendMessage(Colors.ERR + "Region not found");
						e.setLine(0, "\u00A74[ Region ]");
						e.setLine(1, "");
						e.setLine(2, "");
						e.setLine(3, "");
					}
				}
			}
		}
	}
	
	//moved copy and paste blocks from events to single method
	private boolean eventHandle(Block block, Player player) {
		//if not in managed world, do nothing
		if(PopulationDensity.managedWorld == null || !player.getWorld().equals(PopulationDensity.managedWorld))
			return true;
		
		Location blockLocation = block.getLocation();
		
		//region posts are at sea level at the lowest, so no need to check build permissions under that
		if(blockLocation.getBlockY() < ConfigData.minimumRegionPostY)
			return true;
		
		RegionCoordinates blockRegion = RegionCoordinates.fromLocation(blockLocation); 
		
		//if too close to (or above) region post, send an error message
		if(!PopulationDensity.instance.perms.has(player, "populationdensity.buildbreakanywhere") && this.nearRegionPost(blockLocation, blockRegion)) {
			if(ConfigData.buildRegionPosts)
				player.sendMessage(Colors.ERR + "You can't build this close to the region post.");
			else
				player.sendMessage(Colors.ERR + "You can't build this close to a player spawn point.");
			return false;
		}
		
		//if bed or chest and player has not been reminded about /movein this play session
		if(block.getType() == Material.BED || block.getType() == Material.CHEST) {
			PlayerData playerData = PopulationDensity.instance.dataStore.getPlayerData(player);
			if(playerData.advertisedMoveInThisSession)
				return true;
			
			if(!playerData.homeRegion.equals(blockRegion)) {
				if (!PopulationDensity.instance.perms.has(player, "populationdensity.movein"))
					player.sendMessage(Colors.WARN + "You're building outside of your home region.  If you'd like to make this region your new home to help you return here later, use /MoveIn.");
				playerData.advertisedMoveInThisSession = true;
			}
		}
		return true;
	}
	
	//determines whether or not you're "near" a region post
	//has to be pretty restrictive to make grief via lava difficult to pull off
	private boolean nearRegionPost(Location location, RegionCoordinates region) {
		Location postLocation = PopulationDensity.getRegionCenter(region);
		
		//NOTE!  Why not use distance?  Because I want a box to the sky, not a sphere.
		//Why not round?  Below calculation is cheaper than distance (needed for a cylinder or sphere).
		//Why to the sky?  Because if somebody builds a platform above the post, folks will teleport onto that platform by mistake.
		//Also...  lava from above would be bad.
		//Why not below?  Because I can't imagine mining beneath a post as an avenue for griefing. 
		int r = ConfigData.postProtectionRadius;
		return (
			location.getBlockX() > postLocation.getBlockX() - r &&
			location.getBlockX() < postLocation.getBlockX() + r &&
			location.getBlockZ() > postLocation.getBlockZ() - r &&
			location.getBlockZ() < postLocation.getBlockZ() + r &&
			location.getBlockY() > PopulationDensity.managedWorld.getHighestBlockYAt(postLocation) - 5
		);
	}
}
