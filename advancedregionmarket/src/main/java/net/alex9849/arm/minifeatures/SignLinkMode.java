package net.alex9849.arm.minifeatures;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.presets.presets.Preset;
import net.alex9849.arm.util.MaterialFinder;
import net.alex9849.exceptions.InputException;
import net.alex9849.arm.regions.Region;
import net.alex9849.inter.WGRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public class SignLinkMode implements Listener {
    private static List<SignLinkMode> signLinkModeList = new ArrayList<>();
    private Player player;
    private Preset preset;
    private Sign sign;
    private WGRegion wgRegion;
    private World world;

    public SignLinkMode(Player player, Preset preset) throws InputException {
        if(preset == null) {
            throw new InputException(player, Messages.SIGN_LINK_MODE_NO_PRESET_SELECTED);
        }
        this.player = player;
        this.preset = preset;
    }

    @EventHandler
    private void playerLeave(PlayerQuitEvent event) {
        if(event.getPlayer().getUniqueId() == this.player.getUniqueId()) {
            this.unregister();
        }
    }

    @EventHandler
    private void playerClick(PlayerInteractEvent event) {
        if(event.getPlayer().getUniqueId() != this.player.getUniqueId()) {
            return;
        }
        try {
            if(event.getHand() != EquipmentSlot.HAND) {
                return;
            }
            if((!(event.getAction() == Action.LEFT_CLICK_BLOCK)) && (!(event.getAction() == Action.RIGHT_CLICK_BLOCK))) {
                return;
            }
            if((event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getPlayer().getInventory().getItemInMainHand() != null) {
                if(MaterialFinder.getSignMaterials().contains(event.getPlayer().getInventory().getItemInMainHand().getType())) {
                    return;
                }
            }
            if((event.getAction() == Action.LEFT_CLICK_BLOCK) && event.getClickedBlock() != null) {
                if((MaterialFinder.getSignMaterials().contains(event.getClickedBlock().getType()))) {
                    return;
                }
            }
            event.setCancelled(true);
            if(MaterialFinder.getSignMaterials().contains(event.getClickedBlock().getType())) {
                Sign sign  = (Sign) event.getClickedBlock().getState();
                if(AdvancedRegionMarket.getRegionManager().getRegion(sign) != null) {
                    throw new InputException(event.getPlayer(), Messages.SIGN_LINK_MODE_SIGN_BELONGS_TO_ANOTHER_REGION);
                }
                this.sign = sign;
                player.sendMessage(Messages.SIGN_LINK_MODE_SIGN_SELECTED);
            } else {
                Location clicklocation = event.getClickedBlock().getLocation();
                if(clicklocation == null) {
                    return;
                }
                List<WGRegion> regions = AdvancedRegionMarket.getWorldGuardInterface().getApplicableRegions(clicklocation.getWorld(), clicklocation, AdvancedRegionMarket.getWorldGuard());
                if(regions.size() > 1) {
                    throw new InputException(event.getPlayer(), Messages.SIGN_LINK_MODE_COULD_NOT_SELECT_REGION_MULTIPLE_WG_REGIONS);
                }
                if(regions.size() < 1) {
                    throw new InputException(event.getPlayer(), Messages.SIGN_LINK_MODE_COULD_NOT_SELECT_REGION_NO_WG_REGION);
                }
                this.wgRegion = regions.get(0);
                this.world = clicklocation.getWorld();
                this.player.sendMessage(Messages.PREFIX + Messages.SIGN_LINK_MODE_REGION_SELECTED.replace("%regionid%", this.wgRegion.getId()));
            }
            if((this.sign != null) && (this.wgRegion != null) && (this.world != null)) {
                this.registerRegion();
            }
        } catch (InputException e) {
            e.sendMessages(Messages.PREFIX);
        }


    }

    private void registerRegion() throws InputException {
        if(this.sign == null) {
            throw new InputException(player, Messages.SIGN_LINK_MODE_NO_SIGN_SELECTED);
        }
        if(this.wgRegion == null) {
            throw new InputException(player, Messages.SIGN_LINK_MODE_NO_WG_REGION_SELECTED);
        }
        if(this.world == null) {
            throw new InputException(player, Messages.SIGN_LINK_MODE_COULD_NOT_IDENTIFY_WORLD);
        }
        if(AdvancedRegionMarket.getRegionManager().getRegion(sign) != null) {
            throw new InputException(this.player, Messages.SIGN_LINK_MODE_SIGN_BELONGS_TO_ANOTHER_REGION);
        }
        List<Sign> signs = new ArrayList<>();
        signs.add(this.sign);
        Region existingRegion = AdvancedRegionMarket.getRegionManager().getRegion(wgRegion);
        if(existingRegion != null) {
            existingRegion.addSign(this.sign.getLocation());
            existingRegion.queueSave();
            this.player.sendMessage(Messages.PREFIX + Messages.SIGN_ADDED_TO_REGION);
        } else {
            Region newRegion = this.preset.generateRegion(this.wgRegion, this.world, this.player, signs);
            newRegion.createSchematic();
            AdvancedRegionMarket.getRegionManager().add(newRegion);
            this.player.sendMessage(Messages.PREFIX + Messages.REGION_ADDED_TO_ARM);
        }
        this.sign = null;
    }

    public void unregister() {
        SignLinkMode.signLinkModeList.remove(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        PlayerInteractEvent.getHandlerList().unregister(this);
    }

    public static void reset() {
        SignLinkMode.signLinkModeList = new ArrayList<>();
    }

    public Player getPlayer() {
        return this.player;
    }

    public static SignLinkMode getSignLinkMode(Player player) {
        for(SignLinkMode slm : signLinkModeList) {
            if(slm.getPlayer().getUniqueId() == player.getUniqueId()) {
                return slm;
            }
        }
        return null;
    }

    public static boolean hasSignLinkMode(Player player) {
        return getSignLinkMode(player) != null;
    }

    private void register() {
        Bukkit.getServer().getPluginManager().registerEvents(this, AdvancedRegionMarket.getARM());
    }

    public static void register(SignLinkMode slm) {
        if(SignLinkMode.hasSignLinkMode(slm.getPlayer())) {
            SignLinkMode.getSignLinkMode(slm.getPlayer()).unregister();
        }
        signLinkModeList.add(slm);
        slm.register();
    }
}
