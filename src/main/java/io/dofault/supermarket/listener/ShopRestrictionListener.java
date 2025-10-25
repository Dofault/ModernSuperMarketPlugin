package io.dofault.supermarket.listener;

import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.LangManager;
import io.dofault.supermarket.managers.PlayerListManager;
import io.dofault.supermarket.managers.SellManager;
import io.dofault.supermarket.managers.ShopManager;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ShopRestrictionListener implements Listener {

    private final PlayerListManager playerListManager;
    private ChestManager chestManager;
    private ShopManager shopManager;
    private LangManager lang;
    private final SellManager sellManager;
    private final JavaPlugin plugin;

    private final Material buyBlockMaterial;
    private final Material sellBlockMaterial;

    public ShopRestrictionListener(JavaPlugin plugin, LangManager lang, PlayerListManager playerListManager,
            ChestManager chestManager, ShopManager shopManager, SellManager sellManager) {
        this.plugin = plugin;
        this.sellManager = sellManager;
        this.lang = lang;
        this.playerListManager = playerListManager;
        this.chestManager = chestManager;
        this.shopManager = shopManager;

        // Récupère les blocs depuis config.yml
        String buyBlockName = plugin.getConfig().getString("buy_block", "OBSIDIAN");
        String sellBlockName = plugin.getConfig().getString("sell_block", "IRON_BLOCK");

        buyBlockMaterial = Material.matchMaterial(buyBlockName.toUpperCase());
        sellBlockMaterial = Material.matchMaterial(sellBlockName.toUpperCase());

        if (buyBlockMaterial == null) {
            plugin.getLogger().warning("Invalid material for buy_block in config.yml: " + buyBlockName);
        }
        if (sellBlockMaterial == null) {
            plugin.getLogger().warning("Invalid material for sell_block in config.yml: " + sellBlockName);
        }
    }

    // ... le reste du listener

    // Empêche toute interaction avec les blocs
    @EventHandler
    public void onPlayerClick(PlayerInteractEvent e) {

        if (e.getHand() != EquipmentSlot.HAND)
            return; // ignore OFF_HAND

        Player player = e.getPlayer();
        Block block = e.getClickedBlock();

        if (playerListManager.isInShop(player.getUniqueId())) {

            // --- INTERACTION AVEC UN BLOCK (coffres, distributeurs, etc.) ---
            if (block != null) {

                Location loc = block.getLocation();

                // Vérifie si c'est un coffre du shop
                boolean isShopChest = false;
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {

                    for (String chestId : chestManager.getChestsIds()) {
                        Location chestLoc = chestManager.getChestLocation(chestId);
                        if (chestLoc.equals(loc)) {
                            isShopChest = true;
                            break;
                        }
                    }

                }
                // Si c'est un coffre du shop et que le joueur est dans le shop → ouvre
                // l'inventaire
                if (isShopChest) {
                    if (block.getState() instanceof Container container) {

                        player.openInventory(container.getInventory());
                        e.setCancelled(true);
                        return;
                    }
                }

                // --- OBSIDIAN CHECKS (panier / paiement) ---
                if (block.getType() == buyBlockMaterial) {
                    if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                        shopManager.showInventoryDifference(player);
                        e.setCancelled(true);
                        return;
                    }
                    if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        shopManager.payUser(player);
                        e.setCancelled(true);
                        return;
                    }
                }

                if (block.getType() == sellBlockMaterial) {
                    if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        sellManager.tryOpenSellBlock(player, block);
                        e.setCancelled(true);
                        return;
                    }
                }

                // EXIT
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK && block.getType().name().contains("GLASS")) {
                    Block blockBelow = player.getLocation().getBlock();

                    if (shopManager.isExit(blockBelow) && playerListManager.isInShop(player.getUniqueId())) {
                        if (shopManager.canPlayerExit(player)) {
                            Location entryLoc = shopManager.getEntryLocation(player);
                            playerListManager.leaveShop(player);
                            player.teleport(entryLoc);
                            player.sendMessage(lang.get("shop-exit"));

                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.9f); // sonore,
                                                                                                             // cloche
                                                                                                             // douce
                        } else {
                            player.sendMessage(lang.get("shop-must-pay")); // aigu

                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 2.0f, 0.5f); // plus aigu
                                                                                                          // et plus
                                                                                                          // fort

                        }
                    }
                }
            }

            // Bloque tout le reste si le joueur est dans le shop
            e.setCancelled(true);
            return;

        } else { // joueur pas dans le shop

            if (block != null) {
                // Si le joueur clique sur un container
                if (block.getState() instanceof Container container) {
                    Location loc = block.getLocation();

                    boolean isShopChest = false;
                    for (String chestId : chestManager.getChestsIds()) {
                        Location chestLoc = chestManager.getChestLocation(chestId);
                        if (chestLoc.equals(loc)) {
                            isShopChest = true;
                            break;
                        }
                    }

                    // Si ce n'est pas un coffre du shop → laisse interagir normalement
                    if (isShopChest) {
                        e.setCancelled(true);
                        player.sendMessage(lang.get("shop-cannot-open-outside"));
                        return;
                    }
                }

                // ENTRY pour le shop sur block de verre
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK && block.getType().name().contains("GLASS")) {
                    Block blockBelow = player.getLocation().getBlock();

                    if (shopManager.isEntry(blockBelow) && !playerListManager.isInShop(player.getUniqueId())) {
                        if (playerListManager.enterShop(player)) {
                            chestManager.reloadChests();
                            Location exitLoc = shopManager.getExitLocation(player);
                            playerListManager.enterShop(player);
                            player.teleport(exitLoc);

                            player.sendMessage(lang.get("shop-welcome-line1"));
                            player.sendMessage(lang.get("shop-welcome-instruction"));
                            player.sendMessage(lang.get("shop-welcome-leftclick"));
                            player.sendMessage(lang.get("shop-welcome-rightclick"));

                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.9f);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (!playerListManager.isInShop(player.getUniqueId())) {
                                        this.cancel();
                                        return;
                                    }
                                    double balance = shopManager.getBalance(player);
                                    player.sendActionBar(
                                            lang.get("shop-cart-actionbar",
                                                    Map.of("balance", String.valueOf(balance))));
                                }
                            }.runTaskTimer(plugin, 0L, 20L);
                        }
                        return;
                    }
                }
            }
        }
    }

    // Empêche de jeter des items
    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        if (playerListManager.isInShop(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (playerListManager.isInShop(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // Empêche d'interagir avec les entités
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (playerListManager.isInShop(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent e) {
        Player player = e.getPlayer();
        if (!playerListManager.isInShop(player.getUniqueId()))
            return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        if (playerListManager.isInShop(player.getUniqueId())) {
            event.setCancelled(true); // annule tous les dégâts
        }
    }

    // Empêche d'exécuter des commandes
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (playerListManager.isInShop(e.getPlayer().getUniqueId())) {
            String message = e.getMessage().toLowerCase();
            if (!message.startsWith("/supermarket")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cVous ne pouvez utiliser que les commandes /supermarket dans le shop !");
            }
        }
    }
}
