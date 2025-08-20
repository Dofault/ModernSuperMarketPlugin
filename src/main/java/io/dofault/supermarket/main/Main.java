package io.dofault.supermarket.main;



import io.dofault.supermarket.listener.ShopRestrictionListener;
import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.PlayerListManager;
import io.dofault.supermarket.managers.PriceManager;
import io.dofault.supermarket.managers.ShopManager;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    private ShopManager shopManager;
    private PriceManager priceManager;
    private PlayerListManager playerlist;
    private ChestManager chestManager;

    private Economy econ;


    @Override
    public void onEnable() {

        saveResource("config.yml", /* replace */ false);

        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Vault not found or economy plugin missing!");
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(this, this);

        playerlist = new PlayerListManager(this);

        priceManager = new PriceManager(this);
        
        shopManager = new ShopManager(this, playerlist, priceManager);
        chestManager= new ChestManager(this, priceManager);

        getServer().getPluginManager().registerEvents(new ShopRestrictionListener(playerlist, chestManager, shopManager), this);


        getLogger().info("Supermarket plugin activé !!");
    }

    public boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }





    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("supermarket")) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList(
                    "entry", 
                    "exit", 
                    "save", 
                    "pay",
                    "setprice", 
                    "difference", 
                    "addchest", 
                    "removechest", 
                    "additemchest"
            );
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("setprice") || args[0].equalsIgnoreCase("additemchest"))) {
            String current = args[1].toLowerCase();
            return Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .map(Material::name)
                    .map(String::toLowerCase)
                    .filter(name -> name.startsWith(current))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est uniquement pour les joueurs.");
            return true;
        }

        if (!command.getName().equalsIgnoreCase("supermarket")) return false;
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /supermarket <entry|exit|save|setprice>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "entry":
                chestManager.reloadChests();
                shopManager.setEntryPosSetting(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Position d'entrée du shop enregistrée !");
                return true;

            case "exit":
                shopManager.setExitPosSetting(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Position de sortie du shop enregistrée !");
                return true;

            case "save":
                if (shopManager.saveShop()) {
                    player.sendMessage(ChatColor.GREEN + "Shop sauvegardé !");
                } else {
                    player.sendMessage(ChatColor.RED + "Vous devez d'abord définir l'entrée et la sortie !");
                }
                return true;

            case "setprice":
                if (args.length != 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /supermarket setprice <item> <prix>");
                    return true;
                }

                Material material = Material.matchMaterial(args[1].toUpperCase());
                if (material == null) {
                    player.sendMessage(ChatColor.RED + "Item invalide !");
                    return true;
                }

                double price;
                try {
                    price = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Prix invalide !");
                    return true;
                }

                priceManager.setPrice(material.name(), price);
                player.sendMessage(ChatColor.GREEN + "Prix de " + material.name() + " défini à " + price);
                return true;

            case "difference":
                Map<ItemStack, Integer> diff = shopManager.getInventoryDifference(player);

                if (diff.isEmpty()) {
                    player.sendMessage(ChatColor.GREEN + "Aucune différence.");
                } else {
                    double totalPrice = 0.0;
                    player.sendMessage(ChatColor.YELLOW + "Différences avec l'inventaire sauvegardé :");

                    for (Map.Entry<ItemStack, Integer> entry : diff.entrySet()) {
                        ItemStack item = entry.getKey();
                        int delta = entry.getValue();

                        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                                ? item.getItemMeta().getDisplayName()
                                : item.getType().name();

                        if (delta > 0) {
                            double itemPrice = priceManager.hasPrice(item.getType().name())
                                    ? priceManager.getPrice(item.getType().name())
                                    : 0.0;
                            totalPrice += itemPrice * delta;
                            player.sendMessage(ChatColor.GREEN + itemName + " (+" + delta + ") -> " + itemPrice + "€ chacun");
                        } else {
                            player.sendMessage(ChatColor.RED + itemName + " (" + delta + ")");
                        }

                        // Log dans la console
                        Bukkit.getLogger().info("Différence pour " + player.getName() + ": " + itemName + " -> " + delta);
                    }

                    player.sendMessage(ChatColor.GOLD + "Total des dépenses : " + totalPrice + "€");
                }
                return true;


            case "pay":
                shopManager.payUser(player, econ);
                return true;


            case "addchest":
                chestManager.addChest(player);
                return true;

            case "removechest":
                chestManager.removeChest(player);

            case "additemchest":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /supermarket additemchest <item> <amount>");
                    return false;
                }

                Material materia = Material.matchMaterial(args[1].toUpperCase());
                if (materia == null) {
                    player.sendMessage(ChatColor.RED + "Item invalide !");
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cNombre invalide !");
                    return false;
                }

                // récupère le dernier coffre ajouté
                String lastChestId = "chest" + chestManager.getChestCounter(); // méthode getChestCounter à créer dans ChestManager
                chestManager.addItemToChest(player, materia, amount);

                return true;


            default:
                player.sendMessage(ChatColor.YELLOW + "Usage: /supermarket <entry|exit|save|setprice>");
                return true;
        }
    }


    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block blockClicked = event.getClickedBlock();
        if (blockClicked == null) return;
        if (!blockClicked.getType().name().contains("GLASS")) return;

        Player player = event.getPlayer();
        Block blockBelow = player.getLocation().getBlock();

        if (shopManager.isEntry(blockBelow) && !playerlist.isInShop(player.getUniqueId())) {
            chestManager.reloadChests();
            Location exitLoc = shopManager.getExitLocation(player);
            playerlist.enterShop(player);
            player.teleport(exitLoc);
            player.sendMessage(ChatColor.GREEN + "Vous êtes entré dans le shop !");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!playerlist.isInShop(player.getUniqueId())) {
                        this.cancel(); // auto-détruit la tâche
                        return;
                    }

                    double balance = shopManager.getBalance(player);
                    player.sendActionBar(
                        ChatColor.GOLD + "Balance: " + 
                        ChatColor.AQUA + "$" + balance + // balance en surbrillance
                        ChatColor.GOLD + " | /supermarket <pay/difference>"
                    );
                }
            }.runTaskTimer(this, 0L, 20L);
        }

        if (shopManager.isExit(blockBelow) && playerlist.isInShop(player.getUniqueId())) {
            if (shopManager.canPlayerExit(player)) {
                Location entryLoc = shopManager.getEntryLocation(player);
                playerlist.leaveShop(player);
                player.teleport(entryLoc);
                
                player.sendMessage(ChatColor.GREEN + "Vous êtes sorti du shop !");
            } else {
                player.sendMessage(ChatColor.RED + "Vous devez payer pour pouvoir sortir !");
            }
        }
    }


    
}