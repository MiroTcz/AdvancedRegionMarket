package net.alex9849.adapters;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.alex9849.inter.WGRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WG7RegionBeta01 extends WGRegion {

    private ProtectedRegion region;

    WG7RegionBeta01(ProtectedRegion region) {
        this.region = region;
    }

    public Vector getMaxPoint()  {
        BlockVector wgVector = this.region.getMaximumPoint();
        return new Vector(wgVector.getBlockX(), wgVector.getBlockY(), wgVector.getBlockZ());
    }

    public Vector getMinPoint()  {
        BlockVector wgVector = this.region.getMinimumPoint();
        return new Vector(wgVector.getBlockX(), wgVector.getBlockY(), wgVector.getBlockZ());
    }

    public String getId() {
        return this.region.getId();
    }

    public boolean hasMember(UUID uuid) {
        return this.region.getMembers().contains(uuid);
    }

    public void addMember(UUID uuid) {
        this.region.getMembers().addPlayer(uuid);
    }

    public void deleteMembers() {
        DefaultDomain defaultDomain = new DefaultDomain();
        this.region.setMembers(defaultDomain);
    }

    public void removeMember(UUID uuid) {
        this.region.getMembers().removePlayer(uuid);
    }

    public ArrayList<UUID> getMembers() {
        return new ArrayList<UUID>(this.region.getMembers().getUniqueIds());
    }

    public void setOwner(OfflinePlayer player) {
        DefaultDomain newOwner = new DefaultDomain();
        newOwner.addPlayer(player.getUniqueId());
        this.region.setOwners(newOwner);
    }

    public boolean hasOwner(UUID uuid) {
        return this.region.getOwners().contains(uuid);
    }

    public void deleteOwners() {
        DefaultDomain defaultDomain = new DefaultDomain();
        this.region.setOwners(defaultDomain);
    }

    public void removeOwner(UUID uuid) {
        this.region.getOwners().removePlayer(uuid);
    }

    public ArrayList<UUID> getOwners() {
        return new ArrayList<UUID>(this.region.getOwners().getUniqueIds());
    }

    public boolean contains(int x, int y, int z) {
        return this.region.contains(x, y, z);
    }

    protected ProtectedRegion getRegion() {
        return this.region;
    }

    @Override
    public boolean equals(WGRegion wgRegion) {
        WG7RegionBeta01 wg7Region = (WG7RegionBeta01) wgRegion;
        return wg7Region.getRegion() == this.region;
    }

    @Override
    public WGRegion getParent() {
        if(this.getRegion().getParent() != null) {
            return new WG7RegionBeta01(this.getRegion().getParent());
        } else {
            return null;
        }

    }

    @Override
    public void setParent(WGRegion wgRegion) {
        WG7RegionBeta01 wg7Region = (WG7RegionBeta01) wgRegion;
        this.getRegion().setPriority(wg7Region.getPriority());
        try {
            this.getRegion().setParent(wg7Region.getRegion());
        } catch (ProtectedRegion.CircularInheritanceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getPriority() {
        return this.getRegion().getPriority();
    }

    @Override
    public void setPriority(int priority) {
        this.getRegion().clearParent();
        this.getRegion().setPriority(priority);
    }

    @Override
    public int getVolume() {
        return this.region.volume();
    }

    @Override
    public List<Vector> getPoints() {
        List<Vector> points = new ArrayList<Vector>();
        List<BlockVector2D> points2D = this.region.getPoints();
        int minY = this.region.getMinimumPoint().getBlockY();

        for(BlockVector2D vector2D : points2D) {
            points.add(new Vector(vector2D.getBlockX(), minY, vector2D.getBlockZ()));
        }
        return points;
    }
}
