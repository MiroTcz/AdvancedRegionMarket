package net.alex9849.arm.subregions.commands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.commands.BasicArmCommand;
import net.alex9849.exceptions.InputException;
import net.alex9849.arm.minifeatures.PlayerRegionRelationship;
import net.alex9849.arm.regions.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnsellCommand extends BasicArmCommand {
    private final String rootCommand = "unsell";
    private final String regex = "(?i)unsell [^;\n ]+";
    private final List<String> usage = new ArrayList<>(Arrays.asList("unsell [REGION]"));

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
        if (!sender.hasPermission(Permission.SUBREGION_TP)) {
            throw new InputException(sender, Messages.NO_PERMISSION);
        }
        if (!(sender instanceof Player)) {
            throw new InputException(sender, Messages.COMMAND_ONLY_INGAME);
        }
        Player player = (Player) sender;
        Region region = AdvancedRegionMarket.getRegionManager().getRegionbyNameAndWorldCommands(args[1], player.getWorld().getName());

        if (!region.isSubregion()) {
            throw new InputException(sender, Messages.REGION_NOT_A_SUBREGION);
        }

        if(!region.getParentRegion().getRegion().hasOwner(player.getUniqueId())) {
            throw new InputException(sender, Messages.PARENT_REGION_NOT_OWN);
        }

        region.unsell();
        player.sendMessage(Messages.PREFIX + Messages.REGION_NOW_AVIABLE);
        return true;
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        List<String> returnme = new ArrayList<>();

        if(args.length >= 1) {
            if (this.rootCommand.startsWith(args[0])) {
                if(player.hasPermission(Permission.SUBREGION_UNSELL)) {
                    if(args.length == 1) {
                        returnme.add(this.rootCommand);
                    } else if(args.length == 2 && (args[0].equalsIgnoreCase(this.rootCommand))) {
                        returnme.addAll(AdvancedRegionMarket.getRegionManager().completeTabRegions(player, args[1], PlayerRegionRelationship.PARENTREGION_OWNER,false, true));
                    }
                }
            }
        }
        return returnme;
    }
}
