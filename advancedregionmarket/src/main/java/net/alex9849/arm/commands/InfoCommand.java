package net.alex9849.arm.commands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.exceptions.InputException;
import net.alex9849.arm.minifeatures.PlayerRegionRelationship;
import net.alex9849.arm.regions.Region;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InfoCommand extends BasicArmCommand {
    private final String rootCommand = "info";
    private final String regex = "(?i)info";
    private final String regex_with_args = "(?i)info [^;\n ]+";
    private final List<String> usage = new ArrayList<>(Arrays.asList("info [REGION]", "info"));

    @Override
    public boolean matchesRegex(String command) {
        if(command.matches(this.regex)){
            return true;
        } else {
            return command.matches(this.regex_with_args);
        }
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
        if (allargs.matches(this.regex)) {
            return regionInfoCommand(sender);
        } else {
            return regionInfoCommand(sender, args[1]);
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        List<String> returnme = new ArrayList<>();

        if(args.length >= 1) {
            if(this.rootCommand.startsWith(args[0])) {
                if(player.hasPermission(Permission.ADMIN_INFO) || player.hasPermission(Permission.MEMBER_INFO)) {
                    if(args.length == 1) {
                        returnme.add(this.rootCommand);
                    } else if(args.length == 2 && (args[0].equalsIgnoreCase(this.rootCommand))) {
                        returnme.addAll(AdvancedRegionMarket.getRegionManager().completeTabRegions(player, args[1], PlayerRegionRelationship.ALL,true, true));
                    }
                }
            }
        }

        return returnme;
    }

    private boolean regionInfoCommand(CommandSender sender) throws InputException {
        if (!(sender instanceof Player)) {
            throw new InputException(sender, Messages.COMMAND_ONLY_INGAME);
        }

        Player player = (Player) sender;

        if(!player.hasPermission(Permission.MEMBER_INFO) && !player.hasPermission(Permission.ADMIN_INFO)) {
            throw new InputException(player, Messages.NO_PERMISSION);
        }

        Location loc = (player).getLocation();

        List<Region> regionList = AdvancedRegionMarket.getRegionManager().getRegionsByLocation(loc);

        if(regionList.size() == 1) {
            regionList.get(0).regionInfo(player);
            return true;
        } else if (regionList.size() > 1) {
            String regions = "";
            for(Region region : regionList) {
                regions = regions + region.getRegion().getId() + " ";
            }
            throw new InputException(player, Messages.REGION_INFO_MULTIPLE_REGIONS + regions);
        }

        throw new InputException(player, Messages.HAVE_TO_STAND_ON_REGION_TO_SHOW_INFO);
    }

    private boolean regionInfoCommand(CommandSender sender, String regionname) throws InputException {
        if (!(sender instanceof Player)) {
            throw new InputException(sender, Messages.COMMAND_ONLY_INGAME);
        }

        Player player = (Player) sender;

        if(!player.hasPermission(Permission.MEMBER_INFO) && !player.hasPermission(Permission.ADMIN_INFO)) {
            throw new InputException(player, Messages.NO_PERMISSION);
        }

        Region region = AdvancedRegionMarket.getRegionManager().getRegionbyNameAndWorldCommands(regionname, (player).getWorld().getName());

        if(region == null){
            throw new InputException(player, Messages.REGION_DOES_NOT_EXIST);
        }

        region.regionInfo(player);
        return true;
    }
}
