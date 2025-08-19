package io.dofault.supermarket.main;



import io.dofault.supermarket.listener.ShopRestrictionListener;
import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.PlayerListManager;
import io.dofault.supermarket.managers.PriceManager;
import io.dofault.supermarket.managers.ShopManager;
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
import org.bukkit.plugin.java.JavaPlugin;

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



    @Override
    public void onEnable() {

        saveResource("config.yml", /* replace */ false);

        saveDefaultConfig();


        getServer().getPluginManager().registerEvents(this, this);

        playerlist = new PlayerListManager(this);

        shopManager = new ShopManager(this, playerlist);
        priceManager = new PriceManager(this);
        chestManager= new ChestManager(this, priceManager);

        getServer().getPluginManager().registerEvents(new ShopRestrictionListener(playerlist, chestManager, shopManager), this);


        getLogger().info("Supermarket plugin activé !!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("supermarket")) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("entry", "exit", "save", "setprice", "additemchest");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("setprice") || args[0].equalsIgnoreCase("additemchest"))) {
            String current = args[1].toLowerCase();
            return Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .map(Material::name)               // juste le nom brut
                    .map(String::toLowerCase)          // en minuscule pour tabcomplete
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
                String itemID = args[1].toUpperCase();
                try {
                    double price = Double.parseDouble(args[2]);
                    priceManager.setPrice(itemID, price);
                    player.sendMessage(ChatColor.GREEN + "Prix de " + itemID + " défini à " + price);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Prix invalide !");
                }
                return true;

            case "difference":
                Map<Material, Integer> diff = shopManager.getInventoryDifference(player);

                if (diff.isEmpty()) {
                    player.sendMessage(ChatColor.GREEN + "Aucune différence.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Différences avec l'inventaire sauvegardé :");
                    for (Map.Entry<Material, Integer> entry : diff.entrySet()) {
                        Material mat = entry.getKey();
                        int delta = entry.getValue();
                        player.sendMessage(
                                (delta > 0 ? ChatColor.GREEN : ChatColor.RED) +
                                        mat.name() + " (" + (delta > 0 ? "+" : "") + delta + ")"
                        );
                    }
                }
                return true;




            case "addchest":
                chestManager.addChest(player);
                return true;

            case "additemchest":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /supermarket additemchest <item> <amount>");
                    return false;
                }

                Material material = Material.matchMaterial(args[1]);
                if (material == null) {
                    player.sendMessage("§cItem inconnu ! Assurez-vous que le nom est correct.");
                    return false;
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
                chestManager.addItemToChest(player, material, amount);

                return true;


            default:
                player.sendMessage(ChatColor.YELLOW + "Usage: /supermarket <entry|exit|save|setprice>");
                return true;
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return; // clic droit seulement
        if (event.getItem() != null) return; // ignore si il a un item dans la main

        Player player = event.getPlayer();
        Block blockBelow = player.getLocation().getBlock();

        if (shopManager.isEntry(blockBelow)) {
            Location exitLoc = shopManager.getExitLocation(player);
            playerlist.enterShop(player);
            player.teleport(exitLoc);
            player.sendMessage(ChatColor.GREEN + "Vous êtes entré dans le shop !");
        }

        if (shopManager.isExit(blockBelow)) {
            Location entryLoc = shopManager.getEntryLocation(player);
            playerlist.leaveShop(player);
            player.teleport(entryLoc);
            player.sendMessage(ChatColor.GREEN + "Vous êtes sorti du shop !");
        }
    }
}