package net.alex9849.arm.regionkind;

import net.alex9849.arm.regions.Region;
import net.alex9849.arm.util.YamlFileManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegionKindManager extends YamlFileManager<RegionKind> {

    public RegionKindManager(File savepath) {
        super(savepath);
    }

    @Override
    public List<RegionKind> loadSavedObjects(YamlConfiguration yamlConfiguration) {
        List<RegionKind> regionKindList = new ArrayList<>();
        boolean fileupdated = false;

        yamlConfiguration.options().copyDefaults(true);

        if(yamlConfiguration.get("DefaultRegionKind") != null) {
            ConfigurationSection defaultRkConfig = yamlConfiguration.getConfigurationSection("DefaultRegionKind");
            fileupdated |= updateDefaults(defaultRkConfig);
            RegionKind.DEFAULT = RegionKind.parse(defaultRkConfig, "Default");
        }

        if(yamlConfiguration.get("SubregionRegionKind") != null) {
            ConfigurationSection subregionRkConfig = yamlConfiguration.getConfigurationSection("SubregionRegionKind");
            fileupdated |= updateDefaults(subregionRkConfig);
            RegionKind.SUBREGION = RegionKind.parse(subregionRkConfig, "Subregion");
        }

        if(yamlConfiguration.get("RegionKinds") != null) {
            ConfigurationSection regionKindsSection = yamlConfiguration.getConfigurationSection("RegionKinds");
            List<String> regionKinds = new ArrayList<>(regionKindsSection.getKeys(false));
            if(regionKinds != null) {
                for(String regionKindID : regionKinds) {
                    if(regionKindsSection.get(regionKindID) != null) {
                        ConfigurationSection rkConfSection = regionKindsSection.getConfigurationSection(regionKindID);
                        if(rkConfSection != null) {
                            fileupdated |=  updateDefaults(rkConfSection);
                            regionKindList.add(RegionKind.parse(rkConfSection, regionKindID));
                        }
                    }
                }
            }
        }

        if(fileupdated) {
            this.saveFile();
        }
        yamlConfiguration.options().copyDefaults(false);
        return regionKindList;
    }

    @Override
    public boolean staticSaveQuenued() {
        return RegionKind.DEFAULT.needsSave() || RegionKind.SUBREGION.needsSave();
    }

    @Override
    public void saveObjectToYamlObject(RegionKind regionKind, YamlConfiguration yamlConfiguration) {
        yamlConfiguration.set("RegionKinds." + regionKind.getName(), regionKind.toConfigureationSection());
    }

    @Override
    public void writeStaticSettings(YamlConfiguration yamlConfiguration) {
        yamlConfiguration.set("DefaultRegionKind", RegionKind.DEFAULT.toConfigureationSection());
        RegionKind.DEFAULT.setSaved();
        yamlConfiguration.set("SubregionRegionKind", RegionKind.SUBREGION.toConfigureationSection());
        RegionKind.SUBREGION.setSaved();
    }

    public List<String> completeTabRegionKinds(String arg, String returnPrefix) {
        List<String> returnme = new ArrayList<>();

        List<RegionKind> regionKinds = this.getObjectListCopy();

        for (RegionKind regionkind : regionKinds) {
            if ((returnPrefix + regionkind.getName()).toLowerCase().startsWith(arg)) {
                returnme.add(returnPrefix + regionkind.getName());
            }
        }
        if ((returnPrefix + "default").startsWith(arg)) {
            returnme.add(returnPrefix + "default");
        }
        if ((returnPrefix + "subregion").startsWith(arg)) {
            returnme.add(returnPrefix + "subregion");
        }

        return returnme;
    }

    public boolean kindExists(String kind){

        List<RegionKind> regionKinds = this.getObjectListCopy();
        for(RegionKind regionKind : regionKinds) {
            if(regionKind.getName().equalsIgnoreCase(kind)){
                return true;
            }
        }

        if(kind.equalsIgnoreCase("default")) {
            return true;
        }
        if(kind.equalsIgnoreCase(RegionKind.DEFAULT.getDisplayName())){
            return true;
        }
        if(kind.equalsIgnoreCase("subregion")) {
            return true;
        }
        if(kind.equalsIgnoreCase(RegionKind.SUBREGION.getDisplayName())){
            return true;
        }
        return false;
    }

    public RegionKind getRegionKind(String name){

        List<RegionKind> regionKinds = this.getObjectListCopy();
        for(RegionKind regionKind : regionKinds) {
            if(regionKind.getName().equalsIgnoreCase(name)){
                return regionKind;
            }
        }

        if(name.equalsIgnoreCase("default") || name.equalsIgnoreCase(RegionKind.DEFAULT.getDisplayName())){
            return RegionKind.DEFAULT;
        }
        if(name.equalsIgnoreCase("subregion") || name.equalsIgnoreCase(RegionKind.SUBREGION.getDisplayName())){
            return RegionKind.SUBREGION;
        }
        return null;
    }

    private boolean updateDefaults(ConfigurationSection section) {
        boolean fileupdated = false;
        fileupdated |= this.addDefault(section, "item", "RED_BED");
        fileupdated |= this.addDefault(section, "displayName", "Default Displayname");
        fileupdated |= this.addDefault(section, "displayName", "Default Displayname");
        fileupdated |= this.addDefault(section, "displayInLimits", true);
        fileupdated |= this.addDefault(section, "displayInGUI", true);
        fileupdated |= this.addDefault(section, "paypackPercentage", 0d);
        fileupdated |= this.addDefault(section, "lore", new ArrayList<String>(Arrays.asList("Default lore")));
        return fileupdated;
    }

    public List<String> tabCompleteRegionKind(String arg) {
        List<RegionKind> regionKinds = this.getObjectListCopy();
        List<String> returnme = new ArrayList<>();

        for(RegionKind regionKind : regionKinds) {
            if(regionKind.getName().equalsIgnoreCase(arg)) {
                returnme.add(regionKind.getName());
            }
        }
        return returnme;
    }
}
