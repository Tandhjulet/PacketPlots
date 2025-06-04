package net.dashmc.plots.utils;

import java.util.logging.Logger;

import org.bukkit.Bukkit;

public class Debug {
	public static boolean log = true;
	private static Logger logger = Bukkit.getLogger();

	public static void log(String obj) {
		if (!log)
			return;

		logger.info(obj);
	}

}
