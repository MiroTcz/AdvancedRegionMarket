package net.alex9849.arm.commands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.exceptions.SchematicNotFoundException;
import net.alex9849.exceptions.InputException;
import net.alex9849.arm.minifeatures.PlayerRegionRelationship;
import net.alex9849.arm.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class ResetCommand extends BasicArmCommand {

    private final String rootCommand = "reset";
    private final String regex = "(?i)reset [^;\n ]+";
    private final List<String> usage = new ArrayList<>(Arrays.asList("reset [REGION]"));

    @Override
    public boolean matchesRegex(String command) {
        return command.matches(this.regex);
    }

    @Override
    public String getRootCommand() {
        return this.rootCommand;
    }

    @Override
    public List<String> getUsage() {
        return this.usage;
    }

    @Override
    public boolean runCommand(CommandSender sender, Command cmd, String commandsLabel, String[] args, String allargs) throws InputException {
        if (!(sender instanceof Player)) {
            throw new InputException(sender, Messages.COMMAND_ONLY_INGAME);
        }

        if(!sender.hasPermission(Permission.ADMIN_RESETREGION)){
            throw new InputException(sender, Messages.NO_PERMISSION);
        }

        Region resregion = AdvancedRegionMarket.getRegionManager().getRegionbyNameAndWorldCommands(args[1], ((Player) sender).getPlayer().getWorld().getName());
        if(resregion == null) {
            throw new InputException(sender, Messages.REGION_DOES_NOT_EXIST);
        } else {
            resregion.unsell();

            try {
                resregion.resetBlocks();
            } catch (SchematicNotFoundException e) {
                sender.sendMessage(Messages.PREFIX + Messages.SCHEMATIC_NOT_FOUND_ERROR_ADMIN.replace("%regionid%", e.getRegion().getId()));
                Bukkit.getLogger().log(Level.WARNING, "Could not find schematic file for region " + resregion.getRegion().getId() + "in world " + resregion.getRegionworld().getName());
            }
            sender.sendMessage(Messages.PREFIX + Messages.REGION_NOW_AVIABLE);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        List<String> returnme = new ArrayList<>();

        if(args.length >= 1) {
            if(this.rootCommand.startsWith(args[0])) {
                if(player.hasPermission(Permission.ADMIN_RESETREGION)) {
                    if(args.length == 1) {
                        returnme.add(this.rootCommand);
                    } else if(args.length == 2 && (args[0].equalsIgnoreCase(this.rootCommand))) {
                        returnme.addAll(AdvancedRegionMarket.getRegionManager().completeTabRegions(player, args[1], PlayerRegionRelationship.ALL, true,true));
                    }
                }
            }
        }
        return returnme;
    }
}
