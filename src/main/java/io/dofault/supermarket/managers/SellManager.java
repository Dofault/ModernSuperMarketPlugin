package io.dofault.supermarket.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SellManager {

    private final JavaPlugin plugin;
    private final LangManager langManager;
    private final PlayerListManager playerListManager;
    private final ShopManager shopManager;
    private final PriceManager priceManager;
    private final Economy econ;

    private final File sellFile;
    private final FileConfiguration sellConfig;
    private int sellCounter = 0;

    public SellManager(JavaPlugin plugin, LangManager langManager,
            ShopManager shopManager, PriceManager priceManager, PlayerListManager playerListManager, Economy econ) {
        this.plugin = plugin;
        this.priceManager = priceManager;
        this.econ = econ;
        this.langManager = langManager;
        this.playerListManager = playerListManager;
        this.shopManager = shopManager;

        sellFile = new File(plugin.getDataFolder(), "sellblocks.yml");
        if (!sellFile.exists()) {
            sellFile.getParentFile().mkdirs();
            try {
                sellFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sellConfig = YamlConfiguration.loadConfiguration(sellFile);

        if (sellConfig.contains("sellBlocks")) {
            sellCounter = sellConfig.getConfigurationSection("sellBlocks").getKeys(false).size();
        }
    }

    public void saveConfig() {
        try {
            sellConfig.save(sellFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSellBlock(Player player) {
        Block block = player.getLocation().getBlock().getRelative(0, -1, 0); // bloc en dessous du joueur
        Location loc = block.getLocation();

        if (sellConfig.contains("sellBlocks")) {
            for (String id : sellConfig.getConfigurationSection("sellBlocks").getKeys(false)) {
                String worldName = sellConfig.getString("sellBlocks." + id + ".world");
                int x = sellConfig.getInt("sellBlocks." + id + ".x");
                int y = sellConfig.getInt("sellBlocks." + id + ".y");
                int z = sellConfig.getInt("sellBlocks." + id + ".z");

                if (worldName.equals(loc.getWorld().getName())
                        && x == loc.getBlockX()
                        && y == loc.getBlockY()
                        && z == loc.getBlockZ()) {
                    player.sendMessage(langManager.get("shop-sellblock-already-registered"));
                    return;
                }
            }
        }

        sellCounter++;
        String sellId = "sell" + sellCounter;

        sellConfig.set("sellBlocks." + sellId + ".world", loc.getWorld().getName());
        sellConfig.set("sellBlocks." + sellId + ".x", loc.getBlockX());
        sellConfig.set("sellBlocks." + sellId + ".y", loc.getBlockY());
        sellConfig.set("sellBlocks." + sellId + ".z", loc.getBlockZ());

        saveConfig();
        player.sendMessage(langManager.get("shop-sellblock-added", Map.of("id", sellId)));
    }

    public void removeSellBlock(Player player) {
        Block block = player.getLocation().getBlock().getRelative(0, -1, 0);
        Location loc = block.getLocation();

        if (!sellConfig.contains("sellBlocks")) {
            player.sendMessage(langManager.get("shop-no-sellblock"));
            return;
        }

        String targetId = null;
        for (String id : sellConfig.getConfigurationSection("sellBlocks").getKeys(false)) {
            String worldName = sellConfig.getString("sellBlocks." + id + ".world");
            int x = sellConfig.getInt("sellBlocks." + id + ".x");
            int y = sellConfig.getInt("sellBlocks." + id + ".y");
            int z = sellConfig.getInt("sellBlocks." + id + ".z");

            if (worldName.equals(loc.getWorld().getName())
                    && x == loc.getBlockX()
                    && y == loc.getBlockY()
                    && z == loc.getBlockZ()) {
                targetId = id;
                break;
            }
        }

        if (targetId == null) {
            player.sendMessage(langManager.get("shop-not-registered-sellblock"));
            return;
        }

        sellConfig.set("sellBlocks." + targetId, null);

        // Réindexe les sellBlocks
        List<String> ids = new ArrayList<>(sellConfig.getConfigurationSection("sellBlocks").getKeys(false));
        Collections.sort(ids, Comparator.comparingInt(id -> Integer.parseInt(id.replace("sell", ""))));
        int counter = 1;
        for (String id : ids) {
            if (!id.equals("sell" + counter)) {
                sellConfig.set("sellBlocks.sell" + counter,
                        sellConfig.getConfigurationSection("sellBlocks." + id).getValues(true));
                sellConfig.set("sellBlocks." + id, null);
            }
            counter++;
        }
        sellCounter = counter - 1;

        saveConfig();
        player.sendMessage(langManager.get("shop-sellblock-removed"));
    }

    public void tryOpenSellBlock(Player player, Block targetBlock) {
        if (!playerListManager.isInShop(player.getUniqueId())) {
            player.sendMessage(langManager.get("shop-not-in-shop"));
            return;
        }

        if (!shopManager.canPlayerExit(player)) {
            player.sendMessage(langManager.get("shop-sell-unpaid-items"));
            return;
        }

        boolean isSellBlock = false;

        if (sellConfig.contains("sellBlocks")) {
            for (String id : sellConfig.getConfigurationSection("sellBlocks").getKeys(false)) {
                String world = sellConfig.getString("sellBlocks." + id + ".world");
                int x = sellConfig.getInt("sellBlocks." + id + ".x");
                int y = sellConfig.getInt("sellBlocks." + id + ".y");
                int z = sellConfig.getInt("sellBlocks." + id + ".z");

                if (world.equals(targetBlock.getWorld().getName())
                        && x == targetBlock.getX()
                        && y == targetBlock.getY()
                        && z == targetBlock.getZ()) {
                    isSellBlock = true;
                    break;
                }
            }
        }

        if (!isSellBlock) {
            player.sendMessage(langManager.get("shop-no-sellblock"));
            return;
        }

        // Crée un inventaire temporaire pour vendre
        Inventory sellInv = Bukkit.createInventory(null, InventoryType.CHEST, "§6Sell Items");
        player.openInventory(sellInv);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onClose(InventoryCloseEvent event) {
                if (!event.getPlayer().equals(player))
                    return;
                if (!event.getInventory().equals(sellInv))
                    return;

                double total = 0.0;
                ItemStack[] toRestore = new ItemStack[sellInv.getSize()];
                int idx = 0;

                for (ItemStack item : sellInv.getContents()) {
                    if (item == null || item.getType() == Material.AIR)
                        continue;

                    if (priceManager.hasPrice(item.getType().name())) {
                        double price = priceManager.getPrice(item.getType().name());
                        total += price * item.getAmount() * 0.8;
                    } else {
                        toRestore[idx++] = item.clone();
                    }
                }

                if (total > 0) {
                    econ.depositPlayer(player, total);
                    player.sendMessage(langManager.get("shop-sell-success", Map.of("amount", String.valueOf(total))));
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);

                    // Sauvegarde l'inventaire du joueur en base après la vente

                    playerListManager.saveInventory(player, false);
                } else {
                    player.sendMessage(langManager.get("shop-sell-nothing"));
                }

                sellInv.clear();

                int restoredCount = 0;
                for (ItemStack item : toRestore) {
                    if (item != null) {
                        player.getInventory().addItem(item);
                        restoredCount += item.getAmount();
                    }
                }

                if (restoredCount > 0) {
                    player.sendMessage(langManager.get("shop-unsellable-restored",
                            Map.of("amount", String.valueOf(restoredCount))));
                }

                org.bukkit.event.HandlerList.unregisterAll(this);
            }
        }, plugin);
    }
}
