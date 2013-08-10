package me.ryanhamshire.PopulationDensity.utils;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Log.Level;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * @author OffLuffy
 */
public class Lib {
	private PopulationDensity p;
	private Log log;
	
	public Lib(PopulationDensity plugin) {
		p = plugin;
	}
	
	/**
	 * @param files String array of files
	 * @return True when files are loaded, false if failed
	 **/
	public boolean initFiles(String ... files) {
		try {
			for (String file : files) {
				File curFile = new File(p.getDataFolder().getAbsolutePath() + "Data", file);
				if (!curFile.exists()) {
					curFile.getParentFile().mkdirs();
					copy(p.getResource(file), curFile);
				}
			}
			return true;
		} catch (Exception e) {
			p.log.log("There was an error creating a file!", Level.WARN);
			return false;
		}
	} // End initFiles
	
	public final MemoryConfiguration loadResource(String file) {
		initFiles(file);
		FileConfiguration fc = loadFile(file);
		MemoryConfiguration mc = new MemoryConfiguration(fc);
		fc = null;
		deleteFile(file);
		return mc;
	}
	
	/**
	 * @param file File to load
	 * @return The FileConfiguration that was loaded
	 */
	public FileConfiguration loadFile(String file) {
		File f = new File(p.getDataFolder().getAbsolutePath() + "Data", file);
		FileConfiguration fc = new YamlConfiguration();
		try {
			fc.load(f);
			fc.save(f);
		} catch (Exception e) {
			p.log.log("There was an error loading file: " + f, Level.WARN);
		}
		return fc;
	} // End loadFile
	
	private void deleteFile(String file) {
		File f = new File(p.getDataFolder(), file);
		if (f.exists())
			f.delete();
	}
	
	/**
	 * @param fc The data to save
	 * @param file The file to save the data to
	 */
	public void saveFile(FileConfiguration fc, String file) {
		File f = new File(p.getDataFolder(), file);
		try {
			fc.save(f);
		} catch (Exception e) {
			p.log.log("There was an error saving file: " + f, Level.WARN);
		}
	} // End saveFile
	
	/**
	 * @param in The input/source stream being copied
	 * @param file The destination that the source is being copied to
	 * @throws IOException 
	 */
	private void copy(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int len;
		while((len=in.read(buf))>0)
			out.write(buf,0,len);
		out.close();
		in.close();
	} // End copy
	
	/**
	 * @param msg The message to be broadcasted
	 * @param perm The permission to check for
	 */
	public void permBroadcast(String msg, String perm) {
		log.log(msg);
		Player[] online = p.getServer().getOnlinePlayers();
		for (Player p : online)
			if (p.hasPermission(perm))
				p.sendMessage(msg);
	}
}
