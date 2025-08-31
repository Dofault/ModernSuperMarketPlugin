package io.dofault.supermarket.listener;

import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.PlayerListManager;
import io.dofault.supermarket.managers.ShopManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
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
    private final JavaPlugin plugin;


    public ShopRestrictionListener(JavaPlugin plugin, PlayerListManager playerListManager, ChestManager chestManager, ShopManager  shopManager) {
        this.plugin = plugin;
        this.playerListManager = playerListManager;
        this.chestManager = chestManager;
        this.shopManager = shopManager;
    }

   // Empêche toute interaction avec les blocs
   @EventHandler
   public void onBlockInteract(PlayerInteractEvent e) {

       if (e.getHand() != EquipmentSlot.HAND) return; // ignore OFF_HAND
       if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

       Player player = e.getPlayer();


       Block block = e.getClickedBlock();

       if (block == null || !(block.getState() instanceof Container)) {
           return;
       }

       Location loc = block.getLocation();
       //System.out.println("[DEBUG] " + player.getName() + " a cliqué sur le bloc en " + loc);

       // Vérifie si le bloc cliqué est un coffre enregistré dans ChestManager
       for (String chestId : chestManager.getChestsIds()) {
           Location chestLoc = chestManager.getChestLocation(chestId);
           if (chestLoc.equals(loc)) {
               //System.out.println("[DEBUG] Le bloc correspond au coffre enregistré: " + chestId);

               if (!playerListManager.isInShop(player.getUniqueId())) {
                   e.setCancelled(true);
                   return;
               } else {
                   //System.out.println("[DEBUG] " + player.getName() + " est dans le shop, interaction autorisée");
               }

               // Force l'ouverture du coffre
               if (block.getState() instanceof Container container) {
                   //System.out.println("[DEBUG] Ouverture forcée de l'inventaire pour " + player.getName());
                   player.openInventory(container.getInventory());
                                      e.setCancelled(true);

               } 

               return; // stop ici pour éviter d'annuler
           }
       }

        if (playerListManager.isInShop(player.getUniqueId())) {
            plugin.getLogger().warning(
                "[EXPLOIT_ATTEMPT] Player " + player.getName() + " is in shop but tried to open a Container outside!"
            );

            e.setCancelled(true);
        }

   }

   @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (playerListManager.isInShop(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }


   
@EventHandler
public void onPlayerClick(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) return; // ignore OFF_HAND

    Player player = event.getPlayer();
    Block blockClicked = event.getClickedBlock();
    if (blockClicked == null) return;

    // --- OBSIDIAN CHECKS ---
    if ((blockClicked.getType() == Material.IRON_BLOCK) && playerListManager.isInShop(player.getUniqueId())) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            shopManager.showInventoryDifference(player);
            event.setCancelled(true);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            shopManager.payUser(player);
            event.setCancelled(true);
            return;
        }
    }

    // --- SHOP ENTRY/EXIT LOGIC ---
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (!blockClicked.getType().name().contains("GLASS")) return;

    Block blockBelow = player.getLocation().getBlock();

    // ENTRY
    if (shopManager.isEntry(blockBelow) && !playerListManager.isInShop(player.getUniqueId())) {
        if (playerListManager.enterShop(player)) {
            chestManager.reloadChests();
            Location exitLoc = shopManager.getExitLocation(player);
            playerListManager.enterShop(player);
            player.teleport(exitLoc);

            player.sendMessage(ChatColor.GOLD + "Bienvenue dans le shop !");
            player.sendMessage(ChatColor.DARK_GRAY + "---------------------------------");

            player.sendMessage(ChatColor.YELLOW + "Sur le block d'OBSIDIENNE :");
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + "Click gauche " + ChatColor.WHITE + "pour voir votre panier.");
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + "Click droit " + ChatColor.WHITE + "pour payer vos articles.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!playerListManager.isInShop(player.getUniqueId())) {
                        this.cancel();
                        return;
                    }

                    double balance = shopManager.getBalance(player);
                    player.sendActionBar(
                        ChatColor.GOLD + "Balance: " +
                        ChatColor.AQUA + "$" + balance
                    );
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
        return;
    }

    // EXIT
    if (shopManager.isExit(blockBelow) && playerListManager.isInShop(player.getUniqueId())) {
        if (shopManager.canPlayerExit(player)) {
            Location entryLoc = shopManager.getEntryLocation(player);
            playerListManager.leaveShop(player);
            player.teleport(entryLoc);
            player.sendMessage(ChatColor.GREEN + "Vous êtes sorti du shop !");
        } else {
            player.sendMessage(ChatColor.RED + "Vous devez payer pour pouvoir sortir !");
        }
    }
}




    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (playerListManager.isInShop(e.getPlayer().getUniqueId())) {
            e.setCancelled(true); 

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

    // Empêche de manger
    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.Player player) {
            if (playerListManager.isInShop(player.getUniqueId())) {
                e.setCancelled(true);
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
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

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
