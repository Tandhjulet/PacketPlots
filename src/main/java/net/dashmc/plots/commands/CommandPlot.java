package net.dashmc.plots.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.dashmc.plots.plot.VirtualEnvironment;

public class CommandPlot implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.isOp()) {
			sender.sendMessage("Permission denied.");
			return true;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can execute this command!");
			return true;
		}

		Player player = (Player) sender;
		if (args.length <= 1 || !args[0].equals("virtualize")) {
			player.sendMessage("/plot virtualize <on/off>");
			return true;
		}
		if (args[1].equals("on")) {
			VirtualEnvironment.get(player).startVirtualization();
			player.sendMessage("Toggled plot virtualization on!");
		} else if (args[1].equals("off")) {
			VirtualEnvironment.get(player).stopVirtualization();
			player.sendMessage("Toggled plot virtualization off!");
		} else {
			player.sendMessage("Use either on/off to toggle");
		}

		return true;
	}
}
