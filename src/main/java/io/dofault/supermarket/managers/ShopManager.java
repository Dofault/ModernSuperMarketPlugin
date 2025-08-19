package io.dofault.supermarket.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.dofault.supermarket.main.Main;
import io.dofault.supermarket.model.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShopManager {

    private Main plugin;
    private BlockPos entryPosSetting;
    private BlockPos exitPosSetting;
    private BlockPos entryPos;
    private BlockPos exitPos;
    private PlayerListManager playerListManager;

    public ShopManager(Main plugin, PlayerListManager playerlist) {
        this.plugin = plugin;
        this.playerListManager = playerlist;
        loadShop();
    }

    public void setEntryPosSetting(Location loc) {
        entryPosSetting = new BlockPos(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void setExitPosSetting(Location loc) {
        exitPosSetting = new BlockPos(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean saveShop() {
        if (entryPosSetting == null || exitPosSetting == null) return false;

        plugin.getConfig().set("shop.entry.world", entryPosSetting.getWorld());
        plugin.getConfig().set("shop.entry.x", entryPosSetting.getX());
        plugin.getConfig().set("shop.entry.y", entryPosSetting.getY());
        plugin.getConfig().set("shop.entry.z", entryPosSetting.getZ());

        plugin.getConfig().set("shop.exit.world", exitPosSetting.getWorld());
        plugin.getConfig().set("shop.exit.x", exitPosSetting.getX());
        plugin.getConfig().set("shop.exit.y", exitPosSetting.getY());
        plugin.getConfig().set("shop.exit.z", exitPosSetting.getZ());

        plugin.saveConfig();
        loadShop();
        return true;
    }

    public void loadShop() {
        if (plugin.getConfig().contains("shop.entry") && plugin.getConfig().contains("shop.exit")) {
            entryPos = new BlockPos(
                    plugin.getConfig().getString("shop.entry.world"),
                    plugin.getConfig().getInt("shop.entry.x"),
                    plugin.getConfig().getInt("shop.entry.y"),
                    plugin.getConfig().getInt("shop.entry.z")
            );
            exitPos = new BlockPos(
                    plugin.getConfig().getString("shop.exit.world"),
                    plugin.getConfig().getInt("shop.exit.x"),
                    plugin.getConfig().getInt("shop.exit.y"),
                    plugin.getConfig().getInt("shop.exit.z")
            );
        }
    }

    // --- Dans ShopManager ---
    public Map<Material, Integer> getInventoryDifference(Player player) {
        ItemStack[] savedInventory = playerListManager.getSavedInventory(player);
        ItemStack[] currentInventory = player.getInventory().getContents();

        Map<Material, Integer> savedMap = new HashMap<>();
        Map<Material, Integer> currentMap = new HashMap<>();

        for (ItemStack item : savedInventory) {
            if (item != null && item.getType() != Material.AIR) {
                savedMap.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        for (ItemStack item : currentInventory) {
            if (item != null && item.getType() != Material.AIR) {
                currentMap.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }

        Set<Material> allMats = new HashSet<>(savedMap.keySet());
        allMats.addAll(currentMap.keySet());

        Map<Material, Integer> diff = new HashMap<>();
        for (Material mat : allMats) {
            int savedAmount = savedMap.getOrDefault(mat, 0);
            int currentAmount = currentMap.getOrDefault(mat, 0);
            int delta = currentAmount - savedAmount;
            if (delta != 0) diff.put(mat, delta);
        }

        return diff;
    }


    public boolean isEntry(Block block) { return entryPos != null && entryPos.matches(block); }
    public boolean isExit(Block block) { return exitPos != null && exitPos.matches(block); }

    public Location getEntryLocation(Player player) {
        return new Location(Bukkit.getWorld(entryPos.getWorld()), entryPos.getX() + 0.5, entryPos.getY(), entryPos.getZ() + 0.5,
                player.getLocation().getYaw(), player.getLocation().getPitch());
    }

    public Location getExitLocation(Player player) {
        return new Location(Bukkit.getWorld(exitPos.getWorld()), exitPos.getX() + 0.5, exitPos.getY(), exitPos.getZ() + 0.5,
                player.getLocation().getYaw(), player.getLocation().getPitch());
    }
}
