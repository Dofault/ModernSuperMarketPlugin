package io.dofault.supermarket.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChestManager {

    private final JavaPlugin plugin;
    private final File chestFile;
    private final FileConfiguration chestConfig;
    private PriceManager priceManager;
    private int chestCounter = 0;

    public ChestManager(JavaPlugin plugin, PriceManager priceManager) {
        this.plugin = plugin;
        this.priceManager = priceManager;
        chestFile = new File(plugin.getDataFolder(), "chest.yml");
        if (!chestFile.exists()) {
            chestFile.getParentFile().mkdirs();
            try {
                chestFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        chestConfig = YamlConfiguration.loadConfiguration(chestFile);

        if (chestConfig.contains("chests")) {
            chestCounter = chestConfig.getConfigurationSection("chests").getKeys(false).size();
        }
    }

    public void saveConfig() {
        try {
            chestConfig.save(chestFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addChest(Player player) {
        Location loc = player.getLocation().getBlock().getLocation(); // Position exacte du bloc
        Block block = loc.getBlock();

        if (!(block.getState() instanceof Container)) {
            player.sendMessage("§cIl n'y a pas de coffre à ta position !");
            return;
        }

        if (chestConfig.contains("chests")) {
            for (String chestId : chestConfig.getConfigurationSection("chests").getKeys(false)) {
                String worldName = chestConfig.getString("chests." + chestId + ".world");
                int x = chestConfig.getInt("chests." + chestId + ".x");
                int y = chestConfig.getInt("chests." + chestId + ".y");
                int z = chestConfig.getInt("chests." + chestId + ".z");

                if (worldName.equals(loc.getWorld().getName())
                        && x == loc.getBlockX()
                        && y == loc.getBlockY()
                        && z == loc.getBlockZ()) {
                    player.sendMessage("§cCe coffre est déjà enregistré !");
                    return;
                }
            }
        }

        chestCounter++;
        String chestId = "chest" + chestCounter;

        chestConfig.set("chests." + chestId + ".world", loc.getWorld().getName());
        chestConfig.set("chests." + chestId + ".x", loc.getBlockX());
        chestConfig.set("chests." + chestId + ".y", loc.getBlockY());
        chestConfig.set("chests." + chestId + ".z", loc.getBlockZ());
        chestConfig.set("chests." + chestId + ".items", new ArrayList<String>());

        saveConfig();
        player.sendMessage("§aCoffre ajouté ! (ID: " + chestId + ")");
    }

    public void addItemToChest(Player player, Material material, int amount) {
        Location loc = player.getLocation().getBlock().getLocation();

        if (!chestConfig.contains("chests")) {
            player.sendMessage("§cAucun coffre enregistré !");
            return;
        }

        if (!priceManager.hasPrice(material.name())) {
            player.sendMessage("§cCe materiau n'a pas de prix defini !");
            return;
        }

        String targetChestId = null;

        for (String chestId : chestConfig.getConfigurationSection("chests").getKeys(false)) {
            String worldName = chestConfig.getString("chests." + chestId + ".world");
            int x = chestConfig.getInt("chests." + chestId + ".x");
            int y = chestConfig.getInt("chests." + chestId + ".y");
            int z = chestConfig.getInt("chests." + chestId + ".z");

            if (worldName.equals(loc.getWorld().getName())
                    && x == loc.getBlockX()
                    && y == loc.getBlockY()
                    && z == loc.getBlockZ()) {
                targetChestId = chestId;
                break;
            }
        }

        if (targetChestId == null) {
            player.sendMessage("§cTu n'es pas sur un coffre enregistré !");
            return;
        }

        List<String> items = chestConfig.getStringList("chests." + targetChestId + ".items");
        items.add(material.name() + ":" + amount);
        chestConfig.set("chests." + targetChestId + ".items", items);
        saveConfig();

        reloadChests();

        player.sendMessage("§aAjouté " + amount + " " + material.name() + " au coffre !");
    }


    // Retourne true si une location correspond à un coffre enregistré
    public boolean isChest(Location loc) {
        for (String chestId : getChestsIds()) {
            if (getChestLocation(chestId).equals(loc)) return true;
        }
        return false;
    }

    // Retourne l'inventaire du coffre à une location, ou null si ce n'est pas un coffre
    public Inventory getChestInventory(Location loc) {
        for (String chestId : getChestsIds()) {
            if (getChestLocation(chestId).equals(loc)) {
                Block block = loc.getBlock();
                if (block.getState() instanceof Container container) {
                    return container.getInventory();
                }
            }
        }
        return null;
    }


    public Set<String> getChestsIds() {
        return chestConfig.getConfigurationSection("chests").getKeys(false);
    }

    public Location getChestLocation(String chestId) {
        String worldName = chestConfig.getString("chests." + chestId + ".world");
        int x = chestConfig.getInt("chests." + chestId + ".x");
        int y = chestConfig.getInt("chests." + chestId + ".y");
        int z = chestConfig.getInt("chests." + chestId + ".z");
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public void reloadChests() {
        if (!chestConfig.contains("chests")) return;

        for (String chestId : chestConfig.getConfigurationSection("chests").getKeys(false)) {
            reloadChest(chestId);
        }
    }

    public void reloadChest(String chestId) {
        if (!chestConfig.contains("chests." + chestId)) return;

        String worldName = chestConfig.getString("chests." + chestId + ".world");
        int x = chestConfig.getInt("chests." + chestId + ".x");
        int y = chestConfig.getInt("chests." + chestId + ".y");
        int z = chestConfig.getInt("chests." + chestId + ".z");

        if (Bukkit.getWorld(worldName) == null) return;

        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
        Block block = loc.getBlock();

        if (!(block.getState() instanceof Container container)) return;

        // On vide l'inventaire avant de remettre les items
        container.getInventory().clear();

        List<String> items = chestConfig.getStringList("chests." + chestId + ".items");
        for (String itemStr : items) {
            String[] parts = itemStr.split(":");
            if (parts.length != 2) continue;

            Material mat = Material.matchMaterial(parts[0]);
            if (mat == null) continue;

            int amount;
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            container.getInventory().addItem(new ItemStack(mat, amount));
        }
    }


    public int getChestCounter() {
        return chestCounter;
    }
}
