package me.ryanhamshire.PopulationDensity.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import me.ryanhamshire.PopulationDensity.PopulationDensity;
import me.ryanhamshire.PopulationDensity.utils.Log.Level;

public class NameData {
	public static List<String> regionNames = new ArrayList<String>();
	public NameData(File names) {
		Scanner in = null;
		PrintWriter out = null;
		boolean lengthWarning = false;
		boolean spaceWarning = false;
		try {
			if (!names.exists()) {
				PopulationDensity.instance.log.log("Names file doesn't exist, creating it", Level.WARN);
				names.getParentFile().mkdirs();
				names.createNewFile();
				out = new PrintWriter(names);
				for (String s : regionNamesList)
					out.println(s);
				out.close();
			}
			in = new Scanner(names);
			while (in.hasNextLine()) {
				String name = in.nextLine();
				if (!name.equals("")) {
					if (name.contains(" ")) {
						spaceWarning = true;
						name = name.replace(" ", "_").toLowerCase();
					}
					if (name.length() > 10) {
						lengthWarning = true;
						name = name.substring(0,10);
					}
					regionNames.add(name);
				}
			}
			if (regionNames.size() == 0) {
				PopulationDensity.instance.log.log("No names in names list. Using default list", Level.WARN);
				regionNames = Arrays.asList(regionNamesList);
			}
		} catch (Exception e) {
			PopulationDensity.instance.log.log("There was an problem loading the names list, using default list", Level.WARN);
			regionNames = Arrays.asList(regionNamesList);
			e.printStackTrace();
		} finally {
			if (lengthWarning)
				PopulationDensity.instance.log.log("One or more names in name list was more than 10 characters long. Trimming them.", Level.WARN);
			if (spaceWarning)
				PopulationDensity.instance.log.log("One or more names in name list spaces. Replacing them with underscores", Level.WARN);
			PopulationDensity.instance.log.log("Names list loaded (" + regionNames.size() + " names)", Level.INFO);
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch(Exception e) {}
		}
	}
	
	//list of region names to use
	// XXX May remove this into an external file for configurability
	private static final String [] regionNamesList = new String [] {
		// A
		"alderton", "abidon", "afton", "akure", "anvik",
		"auburn", "arista", "anaco", "ampar", "aleppo",
		// B
		"brightview", "bluespring", "bagan", "bialla", "birano",
		"blackdell", "barrowmill", "bellmont", "baron", "bulbury",
		// C
		"chasm", "coal", "copper", "cragstone", "coldoak",
		"cadin", "cicia", "corpus", "culion", "catania",
		// D
		"darktide", "dust", "dawn", "dalian", "dubont",
		"duke", "deepcourt", "drakefield", "denbrook", "durrie",
		// E
		"edentide", "evangelion", "eldern", "estelon",
		"ethel", "ellis", "edmond", "elyot", "emmits",
		// F
		"fjord", "firesteel", "falabay", "forlin", "fuma",
		"foalton", "feris", "fulir", "fiton", "faylake",
		// G
		"glendel", "glenwood", "galere", "gusten", "gabal",
		"geneva", "gomel", "greeley", "gullin", "gailbrook",
		// H
		"hazehaven", "hailar", "hanover", "halls", "hatten",
		"holdale", "huelva", "hyder", "herat", "hesan",
		// I
		"idlewood", "icewilde", "ilford", "imonda", "indagen",
		"ioma", "ishurdi", "izumo", "ivalo", "istres",
		// J
		"jesten", "jaffa", "jales", "jena", "joplin",
		"jundah", "junin", "jesolo", "jinan", "jasper",
		// K
		"kairn", "kelsen", "kaben", "kaint", "keromo",
		"kilwit", "kitale", "komai", "kratin", "kulgera",
		// L
		"larkfield", "lapon", "lanish", "laval", "lessat",
		"linden", "libson", "lonrein", "lucca", "luzhao",
		// M
		"morlden", "marnthaw", "mornstar", "meldor", "madera",
		"makou", "manja", "mizan", "monto", "mutare",
		// N
		"newfield", "nelcoven", "nobton", "nakina", "nasik",
		"nimba", "nomane", "norsen", "nulato", "nengan",
		// O
		"oldloch", "ottenhedge", "ocana", "olbia", "omura",
		"olechester", "odensburg", "orluge", "opulus", "oran",
		// P
		"palepeak", "penport", "pagin", "pala", "pleven",
		"ponce", "porto", "porvent", "prail", "pygon",
		// Q
		"quelton", "qeimo", "quetta", "quincy", "quopont",
		"queens", "quibido", "quont", "quipsy", "quang",
		// R
		"rivel", "rosten", "reydawn", "rupal", "rover",
		"rivera", "rockwood", "ropos", "rotunda", "ruston",
		// S
		"stagpost", "summit", "sunrise", "sunset", "sabath",
		"sandsear", "sanrem", "siena", "simara", "suva",
		// T
		"tailson", "tonbel", "tabou", "tormes", "tambor",
		"tiga", "toyama", "trieste", "tulcan", "tupelo",
		// U
		"ulden", "udine", "ulanholt", "upland", "urgen",
		"uvalde", "uzgoro", "utopa", "union", "ursula",
		// V
		"vale", "valley", "vincent", "valpond", "valdez",
		"vigant", "virule", "vorten", "virac", "vigo",
		// W
		"waterfall", "wintersebb", "worsten", "westhedge", "wagny",
		"wenton", "wersond", "whitewick", "windoak", "wrotham",
		// X
		//"", "", "", "", "",
		//"", "", "", "", "",
		// Y
		//"", "", "", "", "",
		//"", "", "", "", "",
		// Z
		//"", "", "", "", "",
		//"", "", "", "", ""
	};
}
