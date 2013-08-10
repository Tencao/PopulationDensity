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
import me.ryanhamshire.PopulationDensity.utils.RegionCoordinates;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Spider;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

public class EntityEventHandler implements Listener {
	//when an entity (includes both dynamite and creepers) explodes...
	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent explodeEvent) {		
		Location location = explodeEvent.getLocation();
		
		//if it's NOT in the managed world, let it splode (or let other plugins worry about it)
		RegionCoordinates region = RegionCoordinates.fromLocation(location);
		if(region == null)
			return;
		
		//otherwise if it's close to a region post
		Location regionCenter = PopulationDensity.getRegionCenter(region);
		regionCenter.setY(ConfigData.managedWorld.getHighestBlockYAt(regionCenter));		
		if(regionCenter.distanceSquared(location) < 225)  //225 = 15 * 15
			explodeEvent.blockList().clear(); //All the noise and terror, none of the destruction (whew!).
		
		//NOTE!  Why not distance?  Because distance squared is cheaper and will be good enough for this.
	}
	
	//when an item despawns
	//FEATURE: in the newest region only, regrow trees from fallen saplings
	@EventHandler(ignoreCancelled = true)
	public void onItemDespawn (ItemDespawnEvent event) {
		//respect config option
		if(!ConfigData.regrowTrees)
			return;
		
		//only care about dropped items
		Entity entity = event.getEntity();
		if(entity.getType() != EntityType.DROPPED_ITEM)
			return;
		
		if(!(entity instanceof Item))
			return;
		
		//get info about the dropped item
		ItemStack item = ((Item)entity).getItemStack();
		
		//only care about saplings
		if(item.getType() != Material.SAPLING)
			return;
		
		//only care about the newest region
		if(!PopulationDensity.instance.dataStore.getOpenRegion().equals(RegionCoordinates.fromLocation(entity.getLocation())))
			return;
		
		//only replace these blocks with saplings
		Block block = entity.getLocation().getBlock();
		if(block.getType() != Material.AIR && block.getType() != Material.LONG_GRASS && block.getType() != Material.SNOW)
			return;
		
		//don't plant saplings next to other saplings or logs
		Block [] neighbors = new Block [] { 				
				block.getRelative(BlockFace.EAST), 
				block.getRelative(BlockFace.WEST), 
				block.getRelative(BlockFace.NORTH), 
				block.getRelative(BlockFace.SOUTH), 
				block.getRelative(BlockFace.NORTH_EAST), 
				block.getRelative(BlockFace.SOUTH_EAST), 
				block.getRelative(BlockFace.SOUTH_WEST), 
				block.getRelative(BlockFace.NORTH_WEST) };
		
		for(Block b : neighbors) {
			if(b.getType() == Material.SAPLING || b.getType() == Material.LOG)
				return;
		}
		
		//only plant trees in grass or dirt
		Block underBlock = block.getRelative(BlockFace.DOWN);
		if(underBlock.getType() == Material.GRASS || underBlock.getType() == Material.DIRT)
			block.setTypeIdAndData(item.getTypeId(), item.getData().getData(), false);
	}	
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage (EntityDamageEvent event) {
		if(!(event instanceof EntityDamageByEntityEvent))
			return;
		
		EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
		
		Player attacker = null;
		Entity damageSource = subEvent.getDamager();
		if(damageSource instanceof Player) {
			attacker = (Player)damageSource;
		} else if (damageSource instanceof Arrow || damageSource instanceof ThrownPotion) {
			Projectile proj = (Projectile)damageSource;
			if(proj.getShooter() instanceof Player)
				attacker = (Player)proj.getShooter();
		}
		
		if(attacker != null)
			PopulationDensity.instance.resetIdleTimer(attacker);
	}
	
	private int respawnAnimalCounter = 1;
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntitySpawn(CreatureSpawnEvent event) {
		//do nothing for non-natural spawns
		if(event.getSpawnReason() != SpawnReason.NATURAL)
			return;
		
		if(ConfigData.managedWorld == null || event.getLocation().getWorld() != ConfigData.managedWorld)
			return;
		
		//when an animal naturally spawns, grow grass around it
		Entity entity = event.getEntity();
		if(entity instanceof Animals && ConfigData.regrowGrass)
			this.regrow(entity.getLocation().getBlock(), 4);
		
		//when a monster spawns, sometimes spawn animals too
		if(entity instanceof Monster && ConfigData.respawnAnimals) {
			//only do this if the spawn is in the newest region
			if(!PopulationDensity.instance.dataStore.getOpenRegion().equals(RegionCoordinates.fromLocation(entity.getLocation())))
				return;				
			
			//if it's on grass, there's a 1/100 chance it will also spawn a group of animals
			Block underBlock = event.getLocation().getBlock().getRelative(BlockFace.DOWN);
			if(underBlock.getType() == Material.GRASS && --this.respawnAnimalCounter == 0) {
				this.respawnAnimalCounter = 100;
				
				//check the chunk for other animals
				Chunk chunk = entity.getLocation().getChunk();
				Entity [] entities = chunk.getEntities();
				for(int i = 0; i < entities.length; i++) {
					if(entity instanceof Animals)
						return;
				}
				
				EntityType animalType = null;
				
				//decide what to spawn based on the type of monster
				if(entity instanceof Creeper)
					animalType = EntityType.COW;
				else if(entity instanceof Zombie)
					animalType = EntityType.CHICKEN;
				else if(entity instanceof Spider)
					animalType = EntityType.PIG;
				else if(entity instanceof Enderman)
					animalType = EntityType.SHEEP;
				
				//spawn an animal at the entity's location and regrow some grass
				if(animalType != null) {
					entity.getWorld().spawnEntity(entity.getLocation(), animalType);
					entity.getWorld().spawnEntity(entity.getLocation(), animalType);
					this.regrow(entity.getLocation().getBlock(), 4);
				}
			}
		}
	}
	
	private void regrow(Block center, int radius) {
        Block toHandle;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                toHandle = center.getWorld().getBlockAt(center.getX() + x, center.getY() + 2, center.getZ() + z);
                while(toHandle.getType() == Material.AIR && toHandle.getY() > center.getY() - 4)
                	toHandle = toHandle.getRelative(BlockFace.DOWN);
                if (toHandle.getType() == Material.GRASS) { // Block is grass
                    Block aboveBlock = toHandle.getRelative(BlockFace.UP);
                	aboveBlock.setType(Material.LONG_GRASS);
                    aboveBlock.setData((byte) 1);  //data == 1 means live grass instead of dead shrub
                    continue;
                }
            }
        }
    }
}
