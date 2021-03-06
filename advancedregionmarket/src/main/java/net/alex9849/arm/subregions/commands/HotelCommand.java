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

public class HotelCommand extends BasicArmCommand {

    private final String rootCommand = "hotel";
    private final String regex = "(?i)hotel [^;\n ]+ (false|true)";
    private final List<String> usage = new ArrayList<>(Arrays.asList("hotel [REGION] [true/false]"));

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
        if(!(sender.hasPermission(Permission.SUBREGION_CHANGE_IS_HOTEL))){
            throw new InputException(sender, Messages.NO_PERMISSION);
        }
        if(!(sender instanceof Player)) {
            throw new InputException(sender, Messages.COMMAND_ONLY_INGAME);
        }
        Player player = (Player) sender;
        Region region = AdvancedRegionMarket.getRegionManager().getRegionbyNameAndWorldCommands(args[1], player.getWorld().getName());
        if(region == null) {
            throw new InputException(sender, Messages.REGION_DOES_NOT_EXIST);
        }
        if(!region.isSubregion()) {
            throw new InputException(player, Messages.REGION_NOT_A_SUBREGION);
        }
        if(!region.getParentRegion().getRegion().hasOwner(player.getUniqueId())) {
            throw new InputException(player, Messages.PARENT_REGION_NOT_OWN);
        }
        region.setHotel(Boolean.parseBoolean(args[2]));
        String state = "disabled";
        if(Boolean.parseBoolean(args[2])){
            state = "enabled";
        }
        player.sendMessage(Messages.PREFIX + "isHotel " + state + " for " + region.getRegion().getId());
        return true;
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        List<String> returnme = new ArrayList<>();

        if(args.length >= 1) {
            if (this.rootCommand.startsWith(args[0])) {
                if(player.hasPermission(Permission.SUBREGION_CHANGE_IS_HOTEL)) {
                    if(args.length == 1) {
                        returnme.add(this.rootCommand);
                    } else if(args.length == 2 && (args[0].equalsIgnoreCase(this.rootCommand))) {
                        returnme.addAll(AdvancedRegionMarket.getRegionManager().completeTabRegions(player, args[1], PlayerRegionRelationship.PARENTREGION_OWNER,false, true));
                    } else if(args.length == 3 && (args[0].equalsIgnoreCase(this.rootCommand))) {
                        if("true".startsWith(args[2])) {
                            returnme.add("true");
                        }
                        if("false".startsWith(args[2])) {
                            returnme.add("false");
                        }
                    }
                }
            }
        }
        return returnme;
    }
}
