package net.alex9849.arm.regions;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.ArmSettings;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.limitgroups.LimitGroup;
import net.alex9849.arm.entitylimit.EntityLimitGroup;
import net.alex9849.arm.events.BuyRegionEvent;
import net.alex9849.arm.regionkind.RegionKind;
import net.alex9849.exceptions.InputException;
import net.alex9849.arm.minifeatures.teleporter.Teleporter;
import net.alex9849.arm.regions.price.Price;
import net.alex9849.inter.WGRegion;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class SellRegion extends Region {


    public SellRegion(WGRegion region, World regionworld, List<Sign> sellsign, Price price, Boolean sold, Boolean autoreset, Boolean allowOnlyNewBlocks,
                      Boolean doBlockReset, RegionKind regionKind, Location teleportLoc, long lastreset, boolean isUserResettable, List<Region> subregions,
                      int allowedSubregions, EntityLimitGroup entityLimitGroup, HashMap<EntityType, Integer> extraEntitys, int boughtExtraTotalEntitys) {
        super(region, regionworld, sellsign, price, sold, autoreset,allowOnlyNewBlocks, doBlockReset, regionKind, teleportLoc, lastreset, isUserResettable,
                subregions, allowedSubregions, entityLimitGroup, extraEntitys, boughtExtraTotalEntitys);

        this.updateSigns();
    }

    @Override
    public void displayExtraInfo(CommandSender sender) {
        return;
    }

    @Override
    public void updateRegion() {
        this.updateSigns();
    }

    @Override
    protected void updateSignText(Sign mysign){
        if((mysign.getWorld() == null) || (!mysign.getWorld().isChunkLoaded(mysign.getLocation().getBlockX() / 16, mysign.getLocation().getBlockZ() / 16))) {
            return;
        }

        if(this.sold){

            String line1 = this.getConvertedMessage(Messages.SOLD_SIGN1);

            String line2 = this.getConvertedMessage(Messages.SOLD_SIGN2);

            String line3 = this.getConvertedMessage(Messages.SOLD_SIGN3);

            String line4 = this.getConvertedMessage(Messages.SOLD_SIGN4);

            mysign.setLine(0, line1);
            mysign.setLine(1, line2);
            mysign.setLine(2, line3);
            mysign.setLine(3, line4);
            mysign.update(false, false);

        } else {

            String line1 = this.getConvertedMessage(Messages.SELL_SIGN1);

            String line2 = this.getConvertedMessage(Messages.SELL_SIGN2);

            String line3 = this.getConvertedMessage(Messages.SELL_SIGN3);

            String line4 = this.getConvertedMessage(Messages.SELL_SIGN4);

            mysign.setLine(0, line1);
            mysign.setLine(1, line2);
            mysign.setLine(2, line3);
            mysign.setLine(3, line4);
            mysign.update(false, false);

        }

    }

    @Override
    public void buy(Player player) throws InputException {

        if(!Permission.hasAnyBuyPermission(player)) {
            throw new InputException(player, Messages.NO_PERMISSION);
        }
        if(this.sold) {
            throw new InputException(player, Messages.REGION_ALREADY_SOLD);
        }
        if (this.regionKind != RegionKind.DEFAULT){
            if(!RegionKind.hasPermission(player, regionKind)){
                throw new InputException(player, this.getConvertedMessage(Messages.NO_PERMISSIONS_TO_BUY_THIS_KIND_OF_REGION));
            }
        }

        if(!LimitGroup.isCanBuyAnother(player, this)){
            throw new InputException(player, LimitGroup.getRegionBuyOutOfLimitMessage(player, this.regionKind));
        }

        if(AdvancedRegionMarket.getEcon().getBalance(player) < this.getPrice()) {
            throw new InputException(player, Messages.NOT_ENOUGHT_MONEY);
        }
        BuyRegionEvent buyRegionEvent = new BuyRegionEvent(this, player);
        Bukkit.getServer().getPluginManager().callEvent(buyRegionEvent);
        if(buyRegionEvent.isCancelled()) {
            return;
        }

        AdvancedRegionMarket.getEcon().withdrawPlayer(player, this.getPrice());
        this.giveParentRegionOwnerMoney(this.getPrice());
        this.setSold(player);
        this.resetBuiltBlocks();
        if(ArmSettings.isTeleportAfterSellRegionBought()){
            Teleporter.teleport(player, this, "", AdvancedRegionMarket.getARM().getConfig().getBoolean("Other.TeleportAfterRegionBoughtCountdown"));
        }
        player.sendMessage(Messages.PREFIX + Messages.REGION_BUYMESSAGE);
    }

    @Override
    public void setSold(OfflinePlayer player){
        this.sold = true;
        this.getRegion().deleteMembers();
        this.getRegion().setOwner(player);

        this.updateSigns();

        this.queueSave();
    }

    @Override
    public void userSell(Player player){
        List<UUID> defdomain = this.getRegion().getOwners();
        double amount = this.getPaybackMoney();

        if(amount > 0){
            for(int i = 0; i < defdomain.size(); i++) {
                AdvancedRegionMarket.getEcon().depositPlayer(Bukkit.getOfflinePlayer(defdomain.get(i)), amount);
            }
        }

        this.automaticResetRegion(player);
    }

    @Override
    public double getPaybackMoney() {
        double money = (this.getPrice() * this.getRegionKind().getPaybackPercentage())/100;
        if (money > 0) {
            return money;
        } else {
            return 0;
        }
    }

    @Override
    public double getPricePerM2PerWeek() {
        return this.getPricePerM2();
    }

    public void setPrice(Price price) {
        this.price = price;
        this.updateSigns();
        this.queueSave();
    }

    @Override
    public double getPricePerM3PerWeek() {
        return this.getPricePerM2();
    }

    public SellType getSellType() {
        return SellType.SELL;
    }
}
