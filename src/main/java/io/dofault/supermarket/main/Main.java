package io.dofault.supermarket.main;

import io.dofault.supermarket.commands.*;
import io.dofault.supermarket.listener.ShopRestrictionListener;
import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.PlayerListManager;
import io.dofault.supermarket.managers.PriceManager;
import io.dofault.supermarket.managers.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    private ShopManager shopManager;
    private PriceManager priceManager;
    private PlayerListManager playerList;
    private ChestManager chestManager;
    private Economy econ;

    private CommandManager commandManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault not found or economy plugin missing!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        playerList = new PlayerListManager(this);
        priceManager = new PriceManager(this);
        shopManager = new ShopManager(this, econ, playerList, priceManager);
        chestManager = new ChestManager(this, priceManager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(
                new ShopRestrictionListener(this, playerList, chestManager, shopManager), this
        );

        // Initialisation du CommandManager
        commandManager = new CommandManager();
        commandManager.register(new EntryCommand(shopManager, chestManager));
        commandManager.register(new ExitCommand(shopManager));
        commandManager.register(new SaveCommand(shopManager));
        commandManager.register(new SetPriceCommand(priceManager));
        commandManager.register(new DifferenceCommand(shopManager, playerList));
        commandManager.register(new PayCommand(shopManager, playerList));
        commandManager.register(new AddChestCommand(chestManager));
        commandManager.register(new RemoveChestCommand(chestManager));
        commandManager.register(new AddItemChestCommand(chestManager));

        getCommand("supermarket").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Uniquement pour les joueurs.");
                return true;
            }
            if (!commandManager.execute(player, args)) {
                player.sendMessage("§eUsage: /supermarket <entry|exit|save|pay|setprice|difference|addchest|removechest|additemchest>");
            }
            return true;
        });


        getCommand("supermarket").setTabCompleter((sender, command, alias, args) -> {
            if (!(sender instanceof Player)) return Collections.emptyList();

            if (args.length == 1) {
                return commandManager.getCommandNames(); // noms dynamiques
            }

            if (args.length == 2) {
                String cmdName = args[0].toLowerCase();
                if (cmdName.equals("setprice") || cmdName.equals("additemchest")) {
                    String current = args[1].toLowerCase();
                    return Arrays.stream(Material.values())
                            .filter(Material::isItem)
                            .map(Material::name)
                            .map(String::toLowerCase)
                            .filter(name -> name.startsWith(current))
                            .toList();
                }
            }

            return Collections.emptyList();
        });

        getLogger().info("Supermarket plugin activé !");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (playerList.isInShop(player.getUniqueId())) {
            player.teleport(shopManager.getEntryLocation(player));
            playerList.leaveShop(player);
        }
    }
}
