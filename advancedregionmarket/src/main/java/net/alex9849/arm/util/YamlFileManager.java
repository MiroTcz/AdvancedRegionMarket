package net.alex9849.arm.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class YamlFileManager<ManagedObject extends Saveable> {
    private List<ManagedObject> objectList;
    private YamlConfiguration yamlConfiguration;
    private File savepath;
    private boolean completeSaveQueuned;


    public YamlFileManager(File savepath) {
        this.objectList = new ArrayList<>();
        this.savepath = savepath;
        this.completeSaveQueuned = false;
        this.yamlConfiguration = YamlConfiguration.loadConfiguration(this.savepath);
        this.objectList.addAll(loadSavedObjects(this.yamlConfiguration));
    }

    public void add(ManagedObject managedObject) {
        if(!this.objectList.contains(managedObject)) {
            this.objectList.add(managedObject);
            managedObject.queueSave();
            updateFile();
        }
    }

    public void remove(ManagedObject managedObject) {
        if(this.objectList.remove(managedObject)) {
            this.queueSaveCompleteSave();
            this.updateFile();
        }
    }

    public void saveFile() {
        try {
            this.yamlConfiguration.save(this.savepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateFile() {
        boolean savedSomething = false;

        if(this.completeSaveQueuned) {
            this.yamlConfiguration = new YamlConfiguration();
            savedSomething = true;
        }

        if(this.completeSaveQueuned || this.staticSaveQuenued()) {
            this.writeStaticSettings(this.yamlConfiguration);
            savedSomething = true;
        }

        for(ManagedObject managedObject : this.objectList) {
            if(managedObject.needsSave() || this.completeSaveQueuned) {
                saveObjectToYamlObject(managedObject, this.yamlConfiguration);
                managedObject.setSaved();
                savedSomething = true;
            }
        }

        this.completeSaveQueuned = false;

        if(savedSomething) {
            saveFile();
        }
    }

    public abstract List<ManagedObject> loadSavedObjects(YamlConfiguration yamlConfiguration);

    public abstract boolean staticSaveQuenued();

    public abstract void saveObjectToYamlObject(ManagedObject object, YamlConfiguration yamlConfiguration);

    public abstract void writeStaticSettings(YamlConfiguration yamlConfiguration);

    public void queueSaveCompleteSave() {
        this.completeSaveQueuned = true;
    }

    public List<ManagedObject> getObjectListCopy() {
        return new ArrayList<>(this.objectList);
    }

    public static void writeResourceToDisc(File savepath, InputStream resourceStream) {
        if(savepath.exists()) {
            return;
        }
        try {
            byte[] buffer = new byte[resourceStream.available()];
            resourceStream.read(buffer);
            OutputStream output = new FileOutputStream(savepath);
            output.write(buffer);
            output.close();
            resourceStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean addDefault(ConfigurationSection section, String path, Object obj) {
        if(section.get(path) == null) {
            section.addDefault(path, obj);
            return true;
        }
        return false;
    }
}
