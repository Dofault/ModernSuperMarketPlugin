package io.dofault.supermarket.listener;

import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.PlayerListManager;
import io.dofault.supermarket.managers.ShopManager;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

public class ShopRestrictionListener implements Listener {

    private final PlayerListManager playerListManager;
    private ChestManager chestManager;
    private ShopManager shopManager;

    public ShopRestrictionListener(PlayerListManager playerListManager, ChestManager chestManager, ShopManager  shopManager) {
        this.playerListManager = playerListManager;
        this.chestManager = chestManager;
        this.shopManager = shopManager;
    }

//    // Empêche toute interaction avec les blocs
//    @EventHandler
//    public void onBlockInteract(PlayerInteractEvent e) {
//        Player player = e.getPlayer();
//        Block block = e.getClickedBlock();
//
//        if (block == null) {
//            //System.out.println("[DEBUG] Bloc cliqué est null pour " + player.getName());
//            return;
//        }
//
//        Location loc = block.getLocation();
//        //System.out.println("[DEBUG] " + player.getName() + " a cliqué sur le bloc en " + loc);
//
//        // Vérifie si le bloc cliqué est un coffre enregistré dans ChestManager
//        for (String chestId : chestManager.getChestsIds()) {
//            Location chestLoc = chestManager.getChestLocation(chestId);
//            if (chestLoc.equals(loc)) {
//                //System.out.println("[DEBUG] Le bloc correspond au coffre enregistré: " + chestId);
//
//                if (!playerListManager.isInShop(player.getUniqueId())) {
//                    e.setCancelled(true);
//                    return;
//                } else {
//                    //System.out.println("[DEBUG] " + player.getName() + " est dans le shop, interaction autorisée");
//                }
//
//                // Force l'ouverture du coffre
//                if (block.getState() instanceof Container container) {
//                    //System.out.println("[DEBUG] Ouverture forcée de l'inventaire pour " + player.getName());
//                    player.openInventory(container.getInventory());
//                } else {
//                    //System.out.println("[DEBUG] Le bloc n'est pas un container valide");
//                }
//
//                return; // stop ici pour éviter d'annuler
//            }
//        }
//
//        if(playerListManager.isInShop(player.getUniqueId())) {
//            e.setCancelled(true);
//
//        }
//
//    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (playerListManager.isInShop(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
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
    public void onBoatDestroy(VehicleDestroyEvent event) {
        if (event.getVehicle() instanceof Boat && event.getAttacker() instanceof Player player) {
            if (playerListManager.isInShop(player.getUniqueId())) {
                event.setCancelled(true);
            }
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

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (playerListManager.isInShop(player.getUniqueId())) {
            event.setCancelled(true); // empêche tout effet de potion d'être appliqué ou modifié
            player.getActivePotionEffects().forEach(effect -> {
                player.removePotionEffect(effect.getType());
            });
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
