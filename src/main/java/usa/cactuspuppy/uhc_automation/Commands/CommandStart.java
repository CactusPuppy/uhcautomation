package usa.cactuspuppy.uhc_automation.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import usa.cactuspuppy.uhc_automation.Main;
import usa.cactuspuppy.uhc_automation.Tasks.GenerateChunksHelper;
import usa.cactuspuppy.uhc_automation.Tasks.PreGameCountdown;
import usa.cactuspuppy.uhc_automation.UHCUtils;

public class CommandStart {
    public static void onCommand(CommandSender sender, String[] args) {
        if (!Main.getInstance().getGameInstance().validate(sender)) {
            UHCUtils.broadcastMessage(Main.getInstance().getGameInstance(), ChatColor.RED + "Game aborted.");
            return;
        } else if (Main.getInstance().getGameInstance().isActive()) {
            sender.sendMessage(ChatColor.RED + "A game is already in progress! Use " + ChatColor.RESET + "/uhcreset " + ChatColor.RED + "to stop the game first!");
            return;
        }
        if (GenerateChunksHelper.getInstance() != null) {
            sender.sendMessage(ChatColor.RED + "Chunk pre-generation is currently in progress! Please wait for it to complete or cancel it with " + ChatColor.RESET + "/uhc prep cancel");
            return;
        }
        if (PreGameCountdown.instanced) {
            if (args.length == 0) {
                PreGameCountdown.getInstance().instaStart();
            } else if (args.length == 1) {
                Bukkit.getScheduler().cancelTask(PreGameCountdown.getInstance().getID());
                try {
                    (new PreGameCountdown(Integer.valueOf(args[0]), sender)).schedule();
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Usage: /uhc start [secs]");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /uhc start [secs]");
            }
            return;
        }
        if (args.length == 0) {
            Main.getInstance().getGameInstance().start(sender);
            return;
        } else if (args.length == 1) {
            try {
                int cdSecs = Integer.valueOf(args[0]);
                (new PreGameCountdown(cdSecs, sender)).schedule();
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Usage: /uhc start [secs]");
            }
        }
        sender.sendMessage(ChatColor.RED + "Usage: /uhc start [secs]");
    }
}
