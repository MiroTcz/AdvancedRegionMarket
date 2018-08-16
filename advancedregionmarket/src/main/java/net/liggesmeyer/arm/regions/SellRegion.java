package net.liggesmeyer.arm.regions;

import net.liggesmeyer.arm.Main;
import net.liggesmeyer.arm.Messages;
import net.liggesmeyer.arm.Permission;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.liggesmeyer.arm.Group.LimitGroup;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.util.*;

public class SellRegion extends Region {


    public SellRegion(ProtectedRegion region, String regionworld, List<Sign> sellsign, double price, Boolean sold, Boolean autoreset, Boolean allowOnlyNewBlocks, Boolean doBlockReset, RegionKind regionKind, Location teleportLoc, long lastreset, Boolean newreg) {
        super(region, regionworld, sellsign, price, sold, autoreset,allowOnlyNewBlocks, doBlockReset, regionKind, teleportLoc, lastreset, newreg);

        this.updateSignText(sellsign.get(0));
        Bukkit.getServer().getWorld(regionworld).save();

    }

    @Override
    protected void updateSignText(Sign mysign){
        if(this.sold){

            LinkedList<UUID> ownerlist = new LinkedList<>(this.getRegion().getOwners().getUniqueIds());
            String ownername;
            if(ownerlist.size() == 0){
                ownername = "Unknown";
            } else {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerlist.get(0));
                ownername = owner.getName();
            }

            String line1 = Messages.SOLD_SIGN1.replace("%regionid%", this.getRegion().getId());
            line1 = line1.replace("%price%", this.price + "");
            line1 = line1.replace("%currency%", Messages.CURRENCY);
            line1 = line1.replace("%dimensions%", this.getDimensions());
            line1 = line1.replace("%owner%", ownername);

            String line2 = Messages.SOLD_SIGN2.replace("%regionid%", this.getRegion().getId());
            line2 = line2.replace("%price%", this.price + "");
            line2 = line2.replace("%currency%", Messages.CURRENCY);
            line2 = line2.replace("%dimensions%", this.getDimensions());
            line2 = line2.replace("%owner%", ownername);

            String line3 = Messages.SOLD_SIGN3.replace("%regionid%", this.getRegion().getId());
            line3 = line3.replace("%price%", this.price + "");
            line3 = line3.replace("%currency%", Messages.CURRENCY);
            line3 = line3.replace("%dimensions%", this.getDimensions());
            line3 = line3.replace("%owner%", ownername);

            String line4 = Messages.SOLD_SIGN4.replace("%regionid%", this.getRegion().getId());
            line4 = line4.replace("%price%", this.price + "");
            line4 = line4.replace("%currency%", Messages.CURRENCY);
            line4 = line4.replace("%dimensions%", this.getDimensions());
            line4 = line4.replace("%owner%", ownername);

            mysign.setLine(0, line1);
            mysign.setLine(1, line2);
            mysign.setLine(2, line3);
            mysign.setLine(3, line4);
            mysign.update();

        } else {

            String line1 = Messages.SELL_SIGN1.replace("%regionid%", this.getRegion().getId());
            line1 = line1.replace("%price%", this.price + "");
            line1 = line1.replace("%currency%", Messages.CURRENCY);
            line1 = line1.replace("%dimensions%", this.getDimensions());

            String line2 = Messages.SELL_SIGN2.replace("%regionid%", this.getRegion().getId());
            line2 = line2.replace("%price%", this.price + "");
            line2 = line2.replace("%currency%", Messages.CURRENCY);
            line2 = line2.replace("%dimensions%", this.getDimensions());

            String line3 = Messages.SELL_SIGN3.replace("%regionid%", this.getRegion().getId());
            line3 = line3.replace("%price%", this.price + "");
            line3 = line3.replace("%currency%", Messages.CURRENCY);
            line3 = line3.replace("%dimensions%", this.getDimensions());

            String line4 = Messages.SELL_SIGN4.replace("%regionid%", this.getRegion().getId());
            line4 = line4.replace("%price%", this.price + "");
            line4 = line4.replace("%currency%", Messages.CURRENCY);
            line4 = line4.replace("%dimensions%", this.getDimensions());

            mysign.setLine(0, line1);
            mysign.setLine(1, line2);
            mysign.setLine(2, line3);
            mysign.setLine(3, line4);
            mysign.update();

        }

    }

    @Override
    public void buy(Player player){

        if(!player.hasPermission(Permission.ARM_BUY_SELLREGION)) {
            player.sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            return;
        }
        if(this.sold) {
            player.sendMessage(Messages.PREFIX + Messages.REGION_ALREADY_SOLD);
            return;
        }
        if (this.regionKind != RegionKind.DEFAULT){
            if(!player.hasPermission(Permission.ARM_BUYKIND + this.regionKind.getName())){
                player.sendMessage(Messages.PREFIX + Messages.NO_PERMISSIONS_TO_BUY_THIS_KIND_OF_REGION);
                return;
            }
        }

        if(!LimitGroup.isCanBuyAnother(player, this)){
            int limittotal = LimitGroup.getLimit(player);
            int limitkind = LimitGroup.getLimit(player, this.regionKind);
            String limittotalS = "" + limittotal;
            String limitkindS = "" + limitkind;

            if(limitkind == Integer.MAX_VALUE) {
                limitkindS = Messages.UNLIMITED;
            }
            if(limittotal == Integer.MAX_VALUE) {
                limittotalS = Messages.UNLIMITED;
            }
            String message = Messages.REGION_BUY_OUT_OF_LIMIT;
            message = message.replace("%playerownedkind%", LimitGroup.getOwnedRegions(player, this.regionKind) + "");
            message = message.replace("%limitkind%", limitkindS);
            message = message.replace("%regionkind%", this.regionKind.getName());
            message = message.replace("%playerownedtotal%", LimitGroup.getOwnedRegions(player) + "");
            message = message.replace("%limittotal%", limittotalS);

            player.sendMessage(Messages.PREFIX + message);
            return;
        }

        if(Main.getEcon().getBalance(player) < this.price) {
            player.sendMessage(Messages.PREFIX + Messages.NOT_ENOUGHT_MONEY);
            return;
        }
        Main.getEcon().withdrawPlayer(player, price);

        this.setSold(player);
        if(Main.isTeleportAfterSellRegionBought()){
            this.teleportToRegion(player);
        }
        player.sendMessage(Messages.PREFIX + Messages.REGION_BUYMESSAGE);
    }

    @Override
    protected void setSold(OfflinePlayer player){
        this.sold = true;
        Main.getWorldGuardInterface().deleteMembers(this.getRegion());
        Main.getWorldGuardInterface().setOwner(player, this.getRegion());

        for (Sign aSellsign : this.sellsign) {
            this.updateSignText(aSellsign);
        }

        YamlConfiguration config = Region.getRegionsConf();
        config.set("Regions." + this.regionworld + "." + this.region.getId() + ".sold", true);
        Region.saveRegionsConf(config);
    }

    @Override
    public void userSell(Player player){
        List<UUID> defdomain = Main.getWorldGuardInterface().getOwners(this.region);
        double amount = this.getPaybackMoney();

        if(amount > 0){
            for(int i = 0; i < defdomain.size(); i++) {
                Main.getEcon().depositPlayer(Bukkit.getOfflinePlayer(defdomain.get(i)), amount);
            }
        }

        this.resetRegion(player);
    }

    @Override
    public double getPaybackMoney() {
        return (this.getPrice() * Region.paybackPercentage)/100;
    }

}