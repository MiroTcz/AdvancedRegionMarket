package net.alex9849.arm.regions;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.ArmSettings;
import net.alex9849.arm.Messages;
import net.alex9849.arm.entitylimit.EntityLimitGroup;
import net.alex9849.arm.events.AddRegionEvent;
import net.alex9849.arm.events.RemoveRegionEvent;
import net.alex9849.arm.minifeatures.PlayerRegionRelationship;
import net.alex9849.arm.minifeatures.teleporter.Teleporter;
import net.alex9849.arm.regionkind.RegionKind;
import net.alex9849.arm.regions.price.Autoprice.AutoPrice;
import net.alex9849.arm.regions.price.ContractPrice;
import net.alex9849.arm.regions.price.Price;
import net.alex9849.arm.regions.price.RentPrice;
import net.alex9849.arm.util.MaterialFinder;
import net.alex9849.arm.util.YamlFileManager;
import net.alex9849.exceptions.InputException;
import net.alex9849.exceptions.SchematicNotFoundException;
import net.alex9849.inter.WGRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class RegionManager extends YamlFileManager<Region> {

    public RegionManager(File savepath) {
        super(savepath);
    }

    @Override
    public void add(Region region) {
        AddRegionEvent addRegionEvent = new AddRegionEvent(region);
        Bukkit.getServer().getPluginManager().callEvent(addRegionEvent);
        if(addRegionEvent.isCancelled()) {
            return;
        }
        super.add(region);
    }

    @Override
    public void remove(Region region) {
        RemoveRegionEvent removeRegionEvent = new RemoveRegionEvent(region);
        Bukkit.getServer().getPluginManager().callEvent(removeRegionEvent);
        if(removeRegionEvent.isCancelled()) {
            return;
        }
        super.remove(region);
    }

    @Override
    public List<Region> loadSavedObjects(YamlConfiguration yamlConfiguration) {
        List<Region> loadedRegions = new ArrayList<>();
        boolean fileupdated = false;
        yamlConfiguration.options().copyDefaults(true);

        if(yamlConfiguration.get("Regions") != null) {
            ConfigurationSection mainSection = yamlConfiguration.getConfigurationSection("Regions");
            List<String> worlds = new ArrayList<String>(mainSection.getKeys(false));
            if(worlds != null) {
                for(String worldString : worlds) {
                    World regionWorld = Bukkit.getWorld(worldString);
                    if(regionWorld != null) {
                        if(mainSection.get(worldString) != null) {
                            ConfigurationSection worldSection = mainSection.getConfigurationSection(worldString);
                            List<String> regions = new ArrayList<String>(worldSection.getKeys(false));
                            if(regions != null) {
                                for(String regionname : regions){
                                    ConfigurationSection regionSection = worldSection.getConfigurationSection(regionname);
                                    WGRegion wgRegion = AdvancedRegionMarket.getWorldGuardInterface().getRegion(regionWorld, AdvancedRegionMarket.getWorldGuard(), regionname);

                                    if(wgRegion != null) {
                                        fileupdated |= updateDefaults(regionSection);
                                        Region armRegion = parseRegion(regionSection, regionWorld, wgRegion);
                                        loadedRegions.add(armRegion);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(fileupdated) {
            this.saveFile();
        }

        yamlConfiguration.options().copyDefaults(false);

        return loadedRegions;
    }

    @Override
    public boolean staticSaveQuenued() {
        return false;
    }

    private static Region parseRegion(ConfigurationSection regionSection, World regionWorld, WGRegion wgRegion) {
        boolean sold = regionSection.getBoolean("sold");
        String kind = regionSection.getString("kind");
        String autoPriceString = regionSection.getString("autoprice");
        boolean autoreset = regionSection.getBoolean("autoreset");
        String regiontype = regionSection.getString("regiontype");
        String entityLimitGroupString = regionSection.getString("entityLimitGroup");
        boolean allowonlynewblocks = regionSection.getBoolean("isHotel");
        boolean doBlockReset = regionSection.getBoolean("doBlockReset");
        long lastreset = regionSection.getLong("lastreset");
        String teleportLocString = regionSection.getString("teleportLoc");
        int allowedSubregions = regionSection.getInt("allowedSubregions");
        int boughtExtraTotalEntitys = regionSection.getInt("boughtExtraTotalEntitys");
        List<String> boughtExtraEntitys = regionSection.getStringList("boughtExtraEntitys");
        boolean isUserResettable = regionSection.getBoolean("isUserResettable");
        Location teleportLoc = parseTpLocation(teleportLocString);
        RegionKind regionKind = AdvancedRegionMarket.getRegionKindManager().getRegionKind(kind);
        if(regionKind == null) {
            regionKind = RegionKind.DEFAULT;
        }
        EntityLimitGroup entityLimitGroup = AdvancedRegionMarket.getEntityLimitGroupManager().getEntityLimitGroup(entityLimitGroupString);
        if(entityLimitGroup == null) {
            entityLimitGroup = EntityLimitGroup.DEFAULT;
        }
        HashMap<EntityType, Integer> extraEntitysMap = parseBoughtExtraEntitys(boughtExtraEntitys);
        List<Sign> regionsigns = parseRegionsSigns(regionSection);

        List<Region> subregions = new ArrayList<>();
        if (regionSection.getConfigurationSection("subregions") != null) {
            List<String> subregionsection = new ArrayList<>(regionSection.getConfigurationSection("subregions").getKeys(false));
            if (subregionsection != null) {
                for (String subregionName : subregionsection) {
                    WGRegion subWGRegion = AdvancedRegionMarket.getWorldGuardInterface().getRegion(regionWorld, AdvancedRegionMarket.getWorldGuard(), subregionName);
                    if(subWGRegion != null) {
                        Region armSubRegion = parseSubRegion(regionSection.getConfigurationSection("subregions." + subregionName), regionWorld, subWGRegion, wgRegion);
                        subregions.add(armSubRegion);
                    }
                }
            }
        }

        if (regiontype.equalsIgnoreCase("rentregion")) {
            RentPrice rentPrice;
            if (autoPriceString != null) {
                if (AutoPrice.getAutoprice(autoPriceString) != null) {
                    rentPrice = new RentPrice(AutoPrice.getAutoprice(autoPriceString));
                } else {
                    rentPrice = new RentPrice(AutoPrice.DEFAULT);
                }
            } else {
                double price = regionSection.getDouble("price");
                long maxRentTime = regionSection.getLong("maxRentTime");
                long rentExtendPerClick = regionSection.getLong("rentExtendPerClick");
                rentPrice = new RentPrice(price, rentExtendPerClick, maxRentTime);
            }
            long payedtill = regionSection.getLong("payedTill");
            return new RentRegion(wgRegion, regionWorld, regionsigns, rentPrice, sold, autoreset, allowonlynewblocks, doBlockReset, regionKind, teleportLoc,
                    lastreset, isUserResettable, payedtill, subregions, allowedSubregions, entityLimitGroup, extraEntitysMap, boughtExtraTotalEntitys);


        }  else if (regiontype.equalsIgnoreCase("contractregion")) {

            ContractPrice contractPrice;
            if (autoPriceString != null) {
                if (AutoPrice.getAutoprice(autoPriceString) != null) {
                    contractPrice = new ContractPrice(AutoPrice.getAutoprice(autoPriceString));
                } else {
                    contractPrice = new ContractPrice(AutoPrice.DEFAULT);
                }
            } else {
                double price = regionSection.getDouble("price");
                long extendTime = regionSection.getLong("extendTime");
                contractPrice = new ContractPrice(price, extendTime);
            }
            long payedtill = regionSection.getLong("payedTill");
            Boolean terminated = regionSection.getBoolean("terminated");
            return new ContractRegion(wgRegion, regionWorld, regionsigns, contractPrice, sold, autoreset, allowonlynewblocks, doBlockReset, regionKind, teleportLoc,
                    lastreset, isUserResettable, payedtill, terminated, subregions, allowedSubregions, entityLimitGroup, extraEntitysMap, boughtExtraTotalEntitys);
        } else {
            Price sellPrice;
            if (autoPriceString != null) {
                if (AutoPrice.getAutoprice(autoPriceString) != null) {
                    sellPrice = new Price(AutoPrice.getAutoprice(autoPriceString));
                } else {
                    sellPrice = new Price(AutoPrice.DEFAULT);
                }
            } else {
                double price = regionSection.getDouble("price");
                sellPrice = new Price(price);
            }
            return new SellRegion(wgRegion, regionWorld, regionsigns, sellPrice, sold, autoreset, allowonlynewblocks, doBlockReset, regionKind, teleportLoc, lastreset,
                    isUserResettable, subregions, allowedSubregions, entityLimitGroup, extraEntitysMap, boughtExtraTotalEntitys);

        }
    }

    private static Region parseSubRegion(ConfigurationSection section, World regionWorld, WGRegion subregion, WGRegion parentRegion) {
        double subregPrice = section.getDouble("price");
        boolean subregIsSold = section.getBoolean("sold");
        boolean subregIsHotel = section.getBoolean("isHotel");
        String subregionRegiontype = section.getString("regiontype");
        long sublastreset = section.getLong("lastreset");
        List<Sign> subregionsigns = parseRegionsSigns(section);

        if (ArmSettings.isAllowParentRegionOwnersBuildOnSubregions()) {
            if (subregion.getParent() == null) {
                subregion.setParent(parentRegion);
            } else {
                if (!subregion.getParent().equals(parentRegion)) {
                    subregion.setParent(parentRegion);
                }
            }
        } else {
            if ((parentRegion.getPriority() + 1) != subregion.getPriority())
                subregion.setPriority(parentRegion.getPriority() + 1);
        }

        if (subregionRegiontype.equalsIgnoreCase("rentregion")) {
            long subregpayedtill = section.getLong("payedTill");
            long subregmaxRentTime = section.getLong("maxRentTime");
            long subregrentExtendPerClick = section.getLong("rentExtendPerClick");
            RentPrice subPrice = new RentPrice(subregPrice, subregrentExtendPerClick, subregmaxRentTime);
            return new RentRegion(subregion, regionWorld, subregionsigns, subPrice, subregIsSold, ArmSettings.isSubregionAutoReset(), subregIsHotel, ArmSettings.isSubregionBlockReset(), RegionKind.SUBREGION, null,
                    sublastreset, ArmSettings.isAllowSubRegionUserReset(), subregpayedtill, new ArrayList<Region>(), 0, EntityLimitGroup.SUBREGION, new HashMap<>(), 0);

        }  else if (subregionRegiontype.equalsIgnoreCase("contractregion")) {
            long subregpayedtill = section.getLong("payedTill");
            long subregextendTime = section.getLong("extendTime");
            Boolean subregterminated = section.getBoolean("terminated");
            ContractPrice subPrice = new ContractPrice(subregPrice, subregextendTime);
            return new ContractRegion(subregion, regionWorld, subregionsigns, subPrice, subregIsSold, ArmSettings.isSubregionAutoReset(), subregIsHotel, ArmSettings.isSubregionBlockReset(), RegionKind.SUBREGION, null,
                    sublastreset, ArmSettings.isAllowSubRegionUserReset(), subregpayedtill, subregterminated, new ArrayList<Region>(), 0, EntityLimitGroup.SUBREGION, new HashMap<>(), 0);

        } else {
            Price subPrice = new Price(subregPrice);
            return new SellRegion(subregion, regionWorld, subregionsigns, subPrice, subregIsSold, ArmSettings.isSubregionAutoReset(), subregIsHotel, ArmSettings.isSubregionBlockReset(), RegionKind.SUBREGION, null,
                    sublastreset, ArmSettings.isAllowSubRegionUserReset(), new ArrayList<Region>(), 0, EntityLimitGroup.SUBREGION, new HashMap<>(), 0);
        }
    }

    private static List<Sign> parseRegionsSigns(ConfigurationSection section) {
        List<String> regionsignsloc = section.getStringList("signs");
        List<Sign> regionsigns = new ArrayList<>();
        for(int j = 0; j < regionsignsloc.size(); j++) {
            String[] locsplit = regionsignsloc.get(j).split(";");
            World world = Bukkit.getWorld(locsplit[0]);

            if(world != null) {
                Double x = Double.parseDouble(locsplit[1]);
                Double yy = Double.parseDouble(locsplit[2]);
                Double z = Double.parseDouble(locsplit[3]);
                Location loc = new Location(world, x, yy, z);
                Location locminone = new Location(world, x, yy - 1, z);

                boolean isWallSign = false;
                if(locsplit[4].equalsIgnoreCase("WALL")) {
                    isWallSign = true;
                }

                if (!MaterialFinder.getSignMaterials().contains(loc.getBlock().getType())){
                    if(!isWallSign){
                        if(locminone.getBlock().getType() == Material.AIR || locminone.getBlock().getType() == Material.LAVA || locminone.getBlock().getType() == Material.WATER
                                || locminone.getBlock().getType() == Material.LAVA || locminone.getBlock().getType() == Material.WATER) {
                            locminone.getBlock().setType(Material.STONE);
                        }
                    }

                    if(isWallSign) {
                        loc.getBlock().setType(MaterialFinder.getWallSign(), false);
                    } else {
                        loc.getBlock().setType(MaterialFinder.getSign(), false);
                    }
                }

                regionsigns.add((Sign) loc.getBlock().getState());
            }
        }
        return regionsigns;
    }

    private static HashMap<EntityType, Integer> parseBoughtExtraEntitys(List<String> stringList) {
        HashMap<EntityType, Integer> boughtExtraEntitys = new HashMap<>();
        for(String element : stringList) {
            if(element.matches("[^;\n ]+: [0-9]+")) {
                String[] extraparts = element.split(": ");
                int extraAmount = Integer.parseInt(extraparts[1]);
                try {
                    EntityType entityType = EntityType.valueOf(extraparts[0]);
                    boughtExtraEntitys.put(entityType, extraAmount);
                } catch (IllegalArgumentException e) {
                    Bukkit.getServer().getLogger().log(Level.INFO, "Could not parse EntitysType " + extraparts[0] + " at boughtExtraEntitys. Ignoring it...");
                }

            }
        }
        return boughtExtraEntitys;
    }

    private static Location parseTpLocation(String teleportLocString) {
        Location teleportLoc = null;
        if (teleportLocString != null) {
            String[] teleportLocarr = teleportLocString.split(";");
            World teleportLocWorld = Bukkit.getWorld(teleportLocarr[0]);
            int teleportLocBlockX = Integer.parseInt(teleportLocarr[1]);
            int teleportLocBlockY = Integer.parseInt(teleportLocarr[2]);
            int teleportLocBlockZ = Integer.parseInt(teleportLocarr[3]);
            float teleportLocPitch = Float.parseFloat(teleportLocarr[4]);
            float teleportLocYaw = Float.parseFloat(teleportLocarr[5]);
            teleportLoc = new Location(teleportLocWorld, teleportLocBlockX, teleportLocBlockY, teleportLocBlockZ);
            teleportLoc.setYaw(teleportLocYaw);
            teleportLoc.setPitch(teleportLocPitch);
        }
        return teleportLoc;
    }

    private boolean updateDefaults(ConfigurationSection section) {
        boolean fileupdated = false;

        fileupdated |= this.addDefault(section,"sold", false);
        fileupdated |= this.addDefault(section,"kind", "default");
        fileupdated |= this.addDefault(section,"autoreset", true);
        fileupdated |= this.addDefault(section,"lastreset", 1);
        fileupdated |= this.addDefault(section,"isHotel", false);
        fileupdated |= this.addDefault(section,"entityLimitGroup", "default");
        fileupdated |= this.addDefault(section,"doBlockReset", true);
        fileupdated |= this.addDefault(section,"allowedSubregions", 0);
        fileupdated |= this.addDefault(section,"isUserResettable", true);
        fileupdated |= this.addDefault(section,"boughtExtraTotalEntitys", 0);
        fileupdated |= this.addDefault(section,"boughtExtraEntitys", new ArrayList<String>());
        fileupdated |= this.addDefault(section,"regiontype", "sellregion");
        if (section.getString("regiontype").equalsIgnoreCase("rentregion")) {
            fileupdated |= this.addDefault(section,"payedTill", 1);
        }
        if (section.getString("regiontype").equalsIgnoreCase("contractregion")) {
            fileupdated |= this.addDefault(section,"payedTill", 1);
            fileupdated |= this.addDefault(section,"terminated", false);
        }
        if(section.get("subregions") != null) {
            List<String> subregions = new ArrayList<String>(section.getConfigurationSection("subregions").getKeys(false));
            if(subregions != null) {
                for (String subregionID : subregions) {
                    fileupdated |= this.addDefault(section,"subregions." + subregionID + ".price", 0);
                    fileupdated |= this.addDefault(section,"subregions." + subregionID + ".sold", false);
                    fileupdated |= this.addDefault(section,"subregions." + subregionID + ".isHotel", false);
                    fileupdated |= this.addDefault(section,"subregions." + subregionID + ".lastreset", 1);
                    fileupdated |= this.addDefault(section,"subregions." + subregionID + ".regiontype", "sellregion");
                    if (section.getString("subregions." + subregionID + ".regiontype").equalsIgnoreCase("contractregion")) {
                        fileupdated |= this.addDefault(section,"subregions." + subregionID + ".payedTill", 0);
                        fileupdated |= this.addDefault(section,"subregions." + subregionID + ".extendTime", 0);
                        fileupdated |= this.addDefault(section,"subregions." + subregionID + ".terminated", false);
                    }
                    if (section.getString("subregions." + subregionID + ".regiontype").equalsIgnoreCase("rentregion")) {
                        fileupdated |= this.addDefault(section,"subregions." + subregionID + ".payedTill", 0);
                        fileupdated |= this.addDefault(section,"subregions." + subregionID + ".maxRentTime", 0);
                        fileupdated |= this.addDefault(section,"subregions." + subregionID + ".rentExtendPerClick", 0);
                    }
                }
            }
        }

        return fileupdated;
    }

    @Override
    public void saveObjectToYamlObject(Region region, YamlConfiguration yamlConfiguration) {
        yamlConfiguration.set("Regions." + region.getRegionworld().getName() + "." + region.getRegion().getId(), region.toConfigureationSection());
    }

    @Override
    public void writeStaticSettings(YamlConfiguration yamlConfiguration) {

    }

    public List<Region> getRegionsByMember(UUID uuid) {
        List<Region> returnme = new ArrayList<>();
        List<Region> regions = this.getObjectListCopy();
        for (Region region : regions){
            for(Region subregion : region.getSubregions()) {
                if(subregion.getRegion().hasMember(uuid)){
                    returnme.add(subregion);
                }
            }
            if(region.getRegion().hasMember(uuid)) {
                returnme.add(region);
            }
        }
        return returnme;
    }

    public List<Region> getRegionsByOwner(UUID uuid) {
        List<Region> returnme = new ArrayList<>();
        List<Region> regions = this.getObjectListCopy();
        for (Region region : regions){
            for(Region subregion : region.getSubregions()) {
                if(subregion.getRegion().hasOwner(uuid)){
                    returnme.add(subregion);
                }
            }
            if(region.getRegion().hasOwner(uuid)) {
                returnme.add(region);
            }
        }
        return returnme;
    }

    public List<Region> getRegionsByOwnerOrMember(UUID uuid) {
        List<Region> returnme = new ArrayList<>();
        List<Region> regions = this.getObjectListCopy();
        for (Region region : regions){
            for(Region subregion : region.getSubregions()) {
                if(subregion.getRegion().hasOwner(uuid) || subregion.getRegion().hasMember(uuid)){
                    returnme.add(subregion);
                }
            }
            if(region.getRegion().hasOwner(uuid) || region.getRegion().hasMember(uuid)) {
                returnme.add(region);
            }
        }
        return returnme;
    }

    public boolean autoResetRegionsFromOwner(UUID uuid){
        List<Region> regions = this.getRegionsByOwner(uuid);
        for(Region region : regions){
            if(region.getAutoreset()){
                region.unsell();
                if(region.isDoBlockReset()) {
                    try {
                        region.resetBlocks();
                    } catch (SchematicNotFoundException e) {
                        Bukkit.getLogger().log(Level.WARNING, "Could not find schematic file for region " + region.getRegion().getId() + "in world " + region.getRegionworld().getName());
                    }
                }
            }
        }
        return true;
    }

    public void teleportToFreeRegion(RegionKind type, Player player) throws InputException {
        List<Region> regions = this.getObjectListCopy();
        for (Region region : regions){

            if ((region.isSold() == false) && (region.getRegionKind() == type)){
                WGRegion regionTP = region.getRegion();
                String message = region.getConvertedMessage(Messages.REGION_TELEPORT_MESSAGE);
                Teleporter.teleport(player, region, Messages.PREFIX + message, true);
                return;
            }
        }
        throw new InputException(player, Messages.NO_FREE_REGION_WITH_THIS_KIND);
    }

    public boolean checkIfSignExists(Sign sign) {
        List<Region> regions = this.getObjectListCopy();
        for(Region region : regions){
            if(region.hasSign(sign)){
                return true;
            }
            for(Region subregion : region.getSubregions()) {
                if(subregion.hasSign(sign)){
                    return true;
                }
            }
        }
        return false;
    }

    public Region getRegion(Sign sign) {
        List<Region> regions = this.getObjectListCopy();
        for(Region region : regions) {
            if(region.hasSign(sign)) {
                return region;
            }
            for(Region subregion : region.getSubregions()) {
                if(subregion.hasSign(sign)) {
                    return subregion;
                }
            }
        }
        return null;
    }

    public Region getRegion(WGRegion wgRegion) {
        List<Region> regions = this.getObjectListCopy();
        for(Region region : regions) {
            if(region.getRegion().equals(wgRegion)) {
                return region;
            }
            for(Region subregion : region.getSubregions()) {
                if(subregion.getRegion().equals(wgRegion)) {
                    return subregion;
                }
            }
        }
        return null;
    }

    public Region getRegionByNameAndWorld(String name, String world){
        List<Region> regions = this.getObjectListCopy();
        for(Region region : regions) {
            if(region.getRegionworld().getName().equalsIgnoreCase(world)) {
                if(region.getRegion().getId().equalsIgnoreCase(name)) {
                    return region;
                }
                for(Region subregion : region.getSubregions()) {
                    if(subregion.getRegion().getId().equalsIgnoreCase(name)) {
                        return subregion;
                    }
                }
            }
        }
        return null;
    }

    public Region getRegionbyNameAndWorldCommands(String name, String world) {
        List<Region> regions = this.getObjectListCopy();
        Region mayReturn = null;
        for(Region region : regions) {
            if(region.getRegionworld().getName().equalsIgnoreCase(world)) {
                if(region.getRegion().getId().equalsIgnoreCase(name)) {
                    return region;
                }
                for(Region subregion : region.getSubregions()) {
                    if(subregion.getRegion().getId().equalsIgnoreCase(name)) {
                        return subregion;
                    }
                }
            } else {
                if(region.getRegion().getId().equalsIgnoreCase(name)) {
                    mayReturn = region;
                }
                for(Region subregion : region.getSubregions()) {
                    if(subregion.getRegion().getId().equalsIgnoreCase(name)) {
                        mayReturn = subregion;
                    }
                }
            }
        }
        return mayReturn;
    }

    public List<Region> getRegionsByLocation(Location location) {
        List<Region> regionList = this.getObjectListCopy();
        List<Region> regions = new ArrayList<>();

        for(Region region : regionList) {
            if(region.getRegion().contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                if(region.getRegionworld().getName().equals(location.getWorld().getName())) {
                    regions.add(region);
                }
                for(Region subregion : region.getSubregions()) {
                    if(subregion.getRegion().contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                        regions.add(subregion);
                    }
                }
            }
        }
        return regions;
    }

    public List<Region> getRegionsByRegionKind(RegionKind regionKind) {
        List<Region> regions = new ArrayList<>();
        List<Region> regionList = this.getObjectListCopy();

        for(Region region : regionList) {
            if(region.getRegionKind() == regionKind) {
                regions.add(region);
                for(Region subregion : region.getSubregions()) {
                    if(subregion.getRegionKind() == regionKind) {
                        regions.add(subregion);
                    }
                }
            }
        }
        return regions;
    }

    public List<Region> getRegionsBySelltype(SellType sellType) {
        List<Region> regionList = this.getObjectListCopy();
        List<Region> regions = new ArrayList<>();
        for(Region region : regionList) {
            if(region.getSellType() == sellType) {
                regions.add(region);
                for(Region subregion : region.getSubregions()) {
                    if(subregion.getSellType() == sellType) {
                        regions.add(subregion);
                    }
                }
            }
        }
        return regions;
    }

    public  List<Region> getFreeRegions(RegionKind regionKind) {
        List<Region> regions = new ArrayList<>();
        List<Region> regionList = this.getObjectListCopy();

        for(Region region : regionList) {
            if(region.getRegionKind() == regionKind) {
                if(!region.isSold()) {
                    regions.add(region);
                }
            }
            for(Region subregion : region.getSubregions()) {
                if(subregion.getRegionKind() == regionKind) {
                    if(!subregion.isSold()) {
                        regions.add(subregion);
                    }
                }
            }
        }
        return regions;
    }

    public boolean containsRegion(Region region) {
        return this.getObjectListCopy().contains(region);
    }

    public List<String> completeTabRegions(Player player, String arg, PlayerRegionRelationship playerRegionRelationship, boolean inculdeNormalRegions, boolean includeSubregions) {
        List<String> returnme = new ArrayList<>();

        if(Region.completeTabRegions) {
            List<Region> allRegions = this.getObjectListCopy();

            for(Region region : allRegions) {
                if(inculdeNormalRegions) {
                    if(region.getRegion().getId().toLowerCase().startsWith(arg)) {
                        if(playerRegionRelationship == PlayerRegionRelationship.OWNER) {
                            if(region.getRegion().hasOwner(player.getUniqueId())) {
                                returnme.add(region.getRegion().getId());
                            }
                        } else if (playerRegionRelationship == PlayerRegionRelationship.MEMBER) {
                            if(region.getRegion().hasMember(player.getUniqueId())) {
                                returnme.add(region.getRegion().getId());
                            }
                        } else if (playerRegionRelationship == PlayerRegionRelationship.MEMBER_OR_OWNER) {
                            if(region.getRegion().hasMember(player.getUniqueId()) || region.getRegion().hasOwner(player.getUniqueId())) {
                                returnme.add(region.getRegion().getId());
                            }
                        } else if (playerRegionRelationship == PlayerRegionRelationship.ALL) {
                            returnme.add(region.getRegion().getId());
                        } else if (playerRegionRelationship == PlayerRegionRelationship.AVAILABLE) {
                            if(!region.isSold()) {
                                returnme.add(region.getRegion().getId());
                            }
                        }
                    }
                }
                if(includeSubregions) {
                    for(Region subregion : region.getSubregions()) {
                        if(subregion.getRegion().getId().toLowerCase().startsWith(arg)) {
                            if(playerRegionRelationship == PlayerRegionRelationship.OWNER) {
                                if(subregion.getRegion().hasOwner(player.getUniqueId())) {
                                    returnme.add(subregion.getRegion().getId());
                                }
                            } else if (playerRegionRelationship == PlayerRegionRelationship.MEMBER) {
                                if(subregion.getRegion().hasMember(player.getUniqueId())) {
                                    returnme.add(subregion.getRegion().getId());
                                }
                            } else if (playerRegionRelationship == PlayerRegionRelationship.MEMBER_OR_OWNER) {
                                if(subregion.getRegion().hasMember(player.getUniqueId()) || subregion.getRegion().hasOwner(player.getUniqueId())) {
                                    returnme.add(subregion.getRegion().getId());
                                }
                            } else if (playerRegionRelationship == PlayerRegionRelationship.ALL) {
                                returnme.add(subregion.getRegion().getId());
                            } else if (playerRegionRelationship == PlayerRegionRelationship.AVAILABLE) {
                                if(!subregion.isSold()) {
                                    returnme.add(subregion.getRegion().getId());
                                }
                            } else if (playerRegionRelationship == PlayerRegionRelationship.PARENTREGION_OWNER) {
                                if(subregion.getParentRegion().getRegion().hasOwner(player.getUniqueId())) {
                                    returnme.add(subregion.getRegion().getId());
                                }
                            }
                        }
                    }
                }
            }
        }

        return returnme;
    }

    public void updateRegions(){
        List<Region> regionList = this.getObjectListCopy();
        for(Region region : regionList) {
            region.updateRegion();
            for(Region subregion : region.getSubregions()) {
                subregion.updateRegion();
            }
        }
    }


}
